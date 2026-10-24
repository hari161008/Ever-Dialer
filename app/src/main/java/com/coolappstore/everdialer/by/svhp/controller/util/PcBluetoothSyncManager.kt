package com.coolappstore.everdialer.by.svhp.controller.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.telephony.TelephonyManager
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.modal.data.getPhoneTypeLabel
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

enum class PcSyncStatus {
    DISABLED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    DISCONNECTED,
    ERROR
}

class PcBluetoothSyncManager(
    private val context: Context,
    private val prefs: PreferenceManager,
    private val contactsRepository: IContactsRepository,
    private val callLogRepository: ICallLogRepository,
    private val smsRepository: ISmsRepository
) {
    companion object {
        private const val TAG = "PcBluetoothSyncManager"
        val EVER_DIALER_SYNC_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val RFCOMM_CHANNEL = 4
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    private val _status = MutableStateFlow(PcSyncStatus.DISABLED)
    val status: StateFlow<PcSyncStatus> = _status.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    @Volatile
    private var activeSocket: BluetoothSocket? = null
    @Volatile
    private var activeWriter: BufferedWriter? = null

    init {
        NoteManager.onNotesChanged = {
            triggerImmediateSync()
        }
        val isEnabled = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false)
        if (isEnabled) {
            start()
        }
    }

    fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun onBluetoothSyncToggled(enabled: Boolean) {
        prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, enabled)
        if (enabled) {
            com.coolappstore.everdialer.by.svhp.controller.PcSyncService.start(context)
            start()
        } else {
            com.coolappstore.everdialer.by.svhp.controller.PcSyncService.stop(context)
            stop()
        }
    }

    @Synchronized
    fun start() {
        stop()
        _status.value = PcSyncStatus.CONNECTING
        syncJob = scope.launch {
            runSyncLoop()
        }
    }

    @Synchronized
    fun stop() {
        syncJob?.cancel()
        syncJob = null
        closeActiveConnection()
        _status.value = PcSyncStatus.DISABLED
        _connectedDeviceName.value = null
    }

    private fun closeActiveConnection() {
        try {
            activeWriter?.close()
        } catch (_: Exception) {}
        activeWriter = null

        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
    }

    fun triggerSyncNow() {
        scope.launch {
            val writer = activeWriter
            val socket = activeSocket
            if (writer != null && socket != null && socket.isConnected) {
                Log.d(TAG, "triggerSyncNow: socket already connected, sending data directly")
                sendSyncData(writer)
            } else {
                Log.d(TAG, "triggerSyncNow: socket not connected, triggering immediate connect")
                start()
            }
        }
    }

    fun triggerImmediateSync() {
        val writer = activeWriter
        val socket = activeSocket
        if (writer != null && socket != null && socket.isConnected) {
            scope.launch(Dispatchers.IO) {
                try {
                    Log.i(TAG, "triggerImmediateSync: pushing fresh data to PC...")
                    sendSyncData(writer)
                } catch (e: Exception) {
                    Log.w(TAG, "triggerImmediateSync error: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runSyncLoop() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth adapter unavailable or disabled")
            _status.value = PcSyncStatus.ERROR
            return
        }

        while (scope.isActive) {
            try {
                if (!hasBluetoothPermission()) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not yet granted. Waiting...")
                    _status.value = PcSyncStatus.ERROR
                    delay(3000)
                    continue
                }

                _status.value = PcSyncStatus.CONNECTING

                // 1. Search bonded devices for a paired PC or computer
                val bonded = try {
                    adapter.bondedDevices?.toList() ?: emptyList()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth permissions: ${e.message}")
                    _status.value = PcSyncStatus.ERROR
                    delay(3000)
                    continue
                }

                // Filter out audio headsets and watches
                val pcCandidates = bonded.filter { dev ->
                    val major = dev.bluetoothClass?.majorDeviceClass
                    val name = dev.name?.lowercase() ?: ""
                    val isAudioOrWatch = major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                            major == BluetoothClass.Device.Major.WEARABLE ||
                            name.contains("fireboltt") || name.contains("buds") ||
                            name.contains("headset") || name.contains("watch") ||
                            name.contains("airpods")
                    !isAudioOrWatch
                }.sortedByDescending { dev ->
                    val name = dev.name?.lowercase() ?: ""
                    val isConnected = try {
                        val m = dev.javaClass.getMethod("isConnected")
                        (m.invoke(dev) as? Boolean) == true
                    } catch (_: Exception) {
                        false
                    }

                    var score = 0
                    if (name.contains("fedora")) score += 50
                    if (isConnected) score += 30
                    if (dev.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER) score += 20
                    if (name.contains("pc") || name.contains("laptop") || name.contains("desktop") || name.contains("linux") || name.contains("ubuntu")) score += 15
                    score
                }

                var connected = false
                for (device in pcCandidates) {
                    if (!scope.isActive) break
                    connected = attemptClientConnect(device)
                    if (connected) break
                }

                if (!connected && scope.isActive) {
                    // 2. Also listen as RFCOMM server in case PC connects to mobile
                    listenAsServer(adapter)
                }

                if (scope.isActive) {
                    delay(3000)
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sync loop: ${e.message}", e)
                _status.value = PcSyncStatus.DISCONNECTED
                delay(3000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun attemptClientConnect(device: BluetoothDevice): Boolean {
        val devName = device.name ?: device.address
        Log.d(TAG, "Attempting client connect to $devName (${device.address})")

        // Strategy A: Direct RFCOMM channel 4 (connects immediately without SDP lookup)
        try {
            val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
            val socket = method.invoke(device, RFCOMM_CHANNEL) as BluetoothSocket
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Successfully connected to $devName on direct RFCOMM channel $RFCOMM_CHANNEL")
                handleConnection(socket, devName)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct channel $RFCOMM_CHANNEL connect to $devName failed: ${e.message}")
        }

        // Strategy B: SDP Ever Dialer UUID
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(EVER_DIALER_SYNC_UUID)
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Successfully connected to $devName with UUID $EVER_DIALER_SYNC_UUID")
                handleConnection(socket, devName)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "SDP connect to $devName via $EVER_DIALER_SYNC_UUID failed: ${e.message}")
        }

        // Strategy C: SDP SPP UUID
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Successfully connected to $devName with UUID $SPP_UUID")
                handleConnection(socket, devName)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "SDP connect to $devName via $SPP_UUID failed: ${e.message}")
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private suspend fun listenAsServer(adapter: BluetoothAdapter) {
        var serverSocket: BluetoothServerSocket? = null
        try {
            Log.d(TAG, "Opening RFCOMM server socket with UUID $EVER_DIALER_SYNC_UUID")
            serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(
                "EverDialerSync",
                EVER_DIALER_SYNC_UUID
            )

            val timeoutJob = scope.launch {
                delay(3000)
                try { serverSocket?.close() } catch (_: Exception) {}
            }

            val clientSocket = try {
                withContext(Dispatchers.IO) {
                    serverSocket.accept()
                }
            } catch (e: Exception) {
                null
            } finally {
                timeoutJob.cancel()
            }

            if (clientSocket != null) {
                val clientDevName = clientSocket.remoteDevice?.name ?: "Ever Dialer PC"
                Log.i(TAG, "Accepted incoming connection from $clientDevName")
                handleConnection(clientSocket, clientDevName)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Server socket listen note: ${e.message}")
        } finally {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun computeStateFingerprint(): String {
        val sb = StringBuilder()
        // 1. Call log
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.DATE),
                null, null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    sb.append("calls:").append(c.getLong(0)).append("-").append(c.getLong(1)).append(";")
                }
            }
        } catch (_: Exception) {}

        // 2. Contacts
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.STARRED,
                    ContactsContract.Contacts.PHOTO_ID,
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                ),
                null, null,
                "${ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    val sIdx = c.getColumnIndex(ContactsContract.Contacts.STARRED)
                    val pIdx = c.getColumnIndex(ContactsContract.Contacts.PHOTO_ID)
                    val tIdx = c.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
                    if (sIdx >= 0) sb.append(c.getInt(sIdx)).append(",")
                    if (pIdx >= 0) sb.append(c.getLong(pIdx)).append(",")
                    if (tIdx >= 0) sb.append(c.getLong(tIdx)).append(";")
                }
            }
        } catch (_: Exception) {}

        // Total contacts count
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null
            )?.use { c ->
                sb.append("cnt:").append(c.count).append(";")
            }
        } catch (_: Exception) {}

        // Starred contacts count
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                "${ContactsContract.Contacts.STARRED} = 1",
                null, null
            )?.use { c ->
                sb.append("fav:").append(c.count).append(";")
            }
        } catch (_: Exception) {}

        // 3. SMS (query by highest _ID and DATE, and conversation snippet/date)
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.DATE, Telephony.Sms.READ),
                null, null,
                "${Telephony.Sms._ID} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    sb.append("sms:").append(c.getLong(0)).append("-").append(c.getLong(1)).append("-").append(c.getInt(2)).append(";")
                }
            }
        } catch (_: Exception) {}

        try {
            context.contentResolver.query(
                Uri.parse("content://mms-sms/conversations"),
                arrayOf("date", "snippet"),
                null, null,
                "date DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    val d = c.getLong(0)
                    val s = c.getString(1)?.hashCode() ?: 0
                    sb.append("mms_conv:").append(d).append("-").append(s).append(";")
                }
            }
        } catch (_: Exception) {}

        // 4. Notes
        try {
            val notes = NoteManager.getNotesDir(context).listFiles() ?: emptyArray()
            val maxMod = notes.maxOfOrNull { it.lastModified() } ?: 0L
            sb.append("notes:").append(notes.size).append("-").append(maxMod)
        } catch (_: Exception) {}

        // 5. Settings that affect PC
        sb.append(";confirm_call:").append(prefs.getBoolean(PreferenceManager.KEY_CONFIRM_PLACING_CALL, false))
        sb.append(";pin_msg:").append(prefs.getString(PreferenceManager.KEY_PINNED_SMS_MESSAGES, ""))
        sb.append(";star_msg:").append(prefs.getString(PreferenceManager.KEY_STARRED_SMS_MESSAGES, ""))
        sb.append(";pin_thr:").append(prefs.getString(PreferenceManager.KEY_PINNED_SMS_THREADS, ""))

        return sb.toString()
    }

    private suspend fun handleConnection(socket: BluetoothSocket, deviceName: String) {
        activeSocket = socket
        _connectedDeviceName.value = deviceName
        _status.value = PcSyncStatus.CONNECTED

        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))
        activeWriter = writer

        val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

        var lastFingerprint = computeStateFingerprint()

        // 1. Send full sync data immediately upon connection
        sendSyncData(writer)

        // 2. Set up ContentObserver for real-time changes (calls, contacts, SMS)
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            private var lastTrigger = 0L
            override fun onChange(selfChange: Boolean) {
                val now = System.currentTimeMillis()
                if (now - lastTrigger > 50) {
                    lastTrigger = now
                    scope.launch(Dispatchers.IO) {
                        var newFp = computeStateFingerprint()
                        if (newFp == lastFingerprint) {
                            delay(150)
                            newFp = computeStateFingerprint()
                        }
                        if (newFp == lastFingerprint) {
                            delay(350)
                            newFp = computeStateFingerprint()
                        }
                        if (newFp != lastFingerprint) {
                            Log.i(TAG, "ContentObserver: data change detected on phone, syncing to PC...")
                            lastFingerprint = newFp
                            photoCache.clear()
                            sendSyncData(writer)
                        }
                    }
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(Telephony.Sms.Inbox.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(Uri.parse("content://mms-sms/conversations"), true, contentObserver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register content observers: ${e.message}")
        }

        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(pContext: Context?, intent: Intent?) {
                Log.i(TAG, "Dynamic SMS broadcast received (${intent?.action}), triggering sync...")
                triggerImmediateSync()
            }
        }
        try {
            val filter = IntentFilter().apply {
                addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION)
            }
            ContextCompat.registerReceiver(context, smsReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register dynamic SMS receiver: ${e.message}")
        }

        // 3. Periodic change-detector coroutine (runs every 800ms for notes or missed provider events + telemetry)
        var ticks = 0
        var lastTelemFp = ""
        val periodicSyncJob = scope.launch(Dispatchers.IO) {
            while (isActive && socket.isConnected) {
                delay(800)
                if (!socket.isConnected) break
                val newFp = computeStateFingerprint()
                if (newFp != lastFingerprint) {
                    Log.i(TAG, "Periodic check detected data change on phone: syncing to PC...")
                    lastFingerprint = newFp
                    photoCache.clear()
                    sendSyncData(writer)
                }
                ticks++
                if (ticks % 4 == 0) { // Every ~3.2 seconds check if telemetry (battery, wifi, signal) changed
                    val telem = getDeviceTelemetry()
                    val telemFp = telem.toString()
                    if (telemFp != lastTelemFp) {
                        lastTelemFp = telemFp
                        sendTelemetry(writer)
                    }
                }
            }
        }

        // 4. Read incoming commands from Ever Dialer PC
        try {
            while (scope.isActive && socket.isConnected) {
                val line = withContext(Dispatchers.IO) {
                    reader.readLine()
                } ?: break

                Log.d(TAG, "Received command from PC: $line")
                try {
                    val cmdObj = JSONObject(line)
                    val cmd = cmdObj.optString("cmd")
                    when (cmd) {
                        "request_sync" -> {
                            photoCache.clear()
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                        "request_telemetry" -> {
                            sendTelemetry(writer)
                        }
                        "make_call" -> {
                            val number = cmdObj.optString("number")
                            Log.i(TAG, "Executing make_call for $number via Ever Dialer")
                            if (number.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                                    val accounts = try { telecomManager?.callCapablePhoneAccounts ?: emptyList() } catch (_: Throwable) { emptyList() }
                                    val defaultSim = prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, 1)
                                    val handle = if (accounts.isNotEmpty()) {
                                        getPhoneAccountForSimSlot(context, accounts, if (defaultSim == 2) 1 else 0)
                                    } else null
                                    makeCall(context, number, accountHandle = handle, skipConfirm = true)
                                }
                            }
                        }
                        "send_sms" -> {
                            val phone = cmdObj.optString("phone").ifBlank { cmdObj.optString("number") }
                            val text = cmdObj.optString("text").ifBlank { cmdObj.optString("body") }
                            Log.i(TAG, "Executing send_sms via Ever Dialer to $phone: $text")
                            if (phone.isNotBlank() && text.isNotBlank()) {
                                val success = smsRepository.sendSms(phone, text)
                                Log.i(TAG, "send_sms result: $success")
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "toggle_favorite" -> {
                            val contactId = cmdObj.optString("contactId")
                            val isFav = cmdObj.optBoolean("isFavorite", false)
                            Log.i(TAG, "Executing toggle_favorite for contactId $contactId -> $isFav")
                            if (contactId.isNotBlank()) {
                                try {
                                    val values = ContentValues().apply {
                                        put(ContactsContract.Contacts.STARRED, if (isFav) 1 else 0)
                                    }
                                    context.contentResolver.update(
                                        ContactsContract.Contacts.CONTENT_URI,
                                        values,
                                        "${ContactsContract.Contacts._ID} = ?",
                                        arrayOf(contactId)
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error toggling favorite for $contactId: ${e.message}")
                                }
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "add_contact" -> {
                            val name = cmdObj.optString("name")
                            val phone = cmdObj.optString("phone").ifBlank { cmdObj.optString("number") }
                            Log.i(TAG, "Executing add_contact name=$name, phone=$phone")
                            if (name.isNotBlank() || phone.isNotBlank()) {
                                try {
                                    val ops = ArrayList<ContentProviderOperation>()
                                    val rawContactInsertIndex = ops.size
                                    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                        .build())
                                    if (name.isNotBlank()) {
                                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                                            .build())
                                    }
                                    if (phone.isNotBlank()) {
                                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                            .build())
                                    }
                                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error adding contact: ${e.message}")
                                }
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "delete_call_log" -> {
                            val id = cmdObj.optString("id")
                            val number = cmdObj.optString("number")
                            Log.i(TAG, "Executing delete_call_log id=$id, number=$number")
                            try {
                                if (id.isNotBlank() && id.toLongOrNull() != null) {
                                    context.contentResolver.delete(
                                        CallLog.Calls.CONTENT_URI,
                                        "${CallLog.Calls._ID} = ?",
                                        arrayOf(id)
                                    )
                                } else if (number.isNotBlank()) {
                                    context.contentResolver.delete(
                                        CallLog.Calls.CONTENT_URI,
                                        "${CallLog.Calls.NUMBER} = ?",
                                        arrayOf(number)
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting call log: ${e.message}")
                            }
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                        "delete_sms_thread" -> {
                            val threadIdStr = cmdObj.optString("threadId")
                            val threadId = threadIdStr.replace("thread_", "").toLongOrNull()
                            Log.i(TAG, "Executing delete_sms_thread threadId=$threadId")
                            if (threadId != null && threadId > 0) {
                                smsRepository.deleteThread(threadId)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "mark_sms_read" -> {
                            val threadIdStr = cmdObj.optString("threadId")
                            val threadId = threadIdStr.replace("thread_", "").toLongOrNull()
                            Log.i(TAG, "Executing mark_sms_read threadId=$threadId")
                            if (threadId != null && threadId > 0) {
                                smsRepository.markThreadAsRead(threadId)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "pin_sms_thread" -> {
                            val threadId = cmdObj.optLong("threadId", -1L).takeIf { it != -1L }
                                ?: cmdObj.optString("threadId").toLongOrNull()
                            val pinned = cmdObj.optBoolean("pinned", true)
                            Log.i(TAG, "Executing pin_sms_thread threadId=$threadId pinned=$pinned")
                            if (threadId != null) {
                                prefs.setSmsThreadPinned(threadId, pinned)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "pin_sms_message" -> {
                            val messageIdStr = cmdObj.optString("messageId").replace("msg_", "")
                            val messageId = messageIdStr.toLongOrNull()
                            val pinned = cmdObj.optBoolean("pinned", true)
                            Log.i(TAG, "Executing pin_sms_message messageId=$messageId pinned=$pinned")
                            if (messageId != null) {
                                prefs.setSmsMessagePinned(messageId, pinned)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "star_sms_message" -> {
                            val messageIdStr = cmdObj.optString("messageId").replace("msg_", "")
                            val messageId = messageIdStr.toLongOrNull()
                            val starred = cmdObj.optBoolean("starred", true)
                            Log.i(TAG, "Executing star_sms_message messageId=$messageId starred=$starred")
                            if (messageId != null) {
                                prefs.setSmsMessageStarred(messageId, starred)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "delete_sms_message" -> {
                            val messageIdStr = cmdObj.optString("messageId").replace("msg_", "")
                            val messageId = messageIdStr.toLongOrNull()
                            val isMms = cmdObj.optBoolean("isMms", false)
                            Log.i(TAG, "Executing delete_sms_message messageId=$messageId isMms=$isMms")
                            if (messageId != null) {
                                smsRepository.deleteMessage(messageId, isMms)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "block_number" -> {
                            val number = cmdObj.optString("number")
                            Log.i(TAG, "Executing block_number for $number")
                            if (number.isNotBlank()) {
                                BlockedNumbersManager.block(context, prefs, number)
                            }
                        }
                        "add_note" -> {
                            val title = cmdObj.optString("title", "Untitled Note")
                            val content = cmdObj.optString("content", "")
                            NoteManager.writeNote(context, title, "", content)
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                        "edit_note" -> {
                            val id = cmdObj.optString("id")
                            val title = cmdObj.optString("title", "Untitled Note")
                            val content = cmdObj.optString("content", "")
                            Log.i(TAG, "Executing edit_note id=$id title=$title")
                            if (id.isNotBlank()) {
                                val file = NoteManager.getNotesDir(context).listFiles()
                                    ?.firstOrNull { it.nameWithoutExtension == id }
                                if (file != null) {
                                    file.writeText(content)
                                } else {
                                    NoteManager.writeNote(context, title, "", content)
                                }
                            } else {
                                NoteManager.writeNote(context, title, "", content)
                            }
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                        "delete_note" -> {
                            val id = cmdObj.optString("id", "")
                            if (id.isNotBlank()) {
                                val file = NoteManager.getNotesDir(context).listFiles()
                                    ?.firstOrNull { it.nameWithoutExtension == id }
                                if (file != null) {
                                    NoteManager.deleteNoteFile(file)
                                }
                            }
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error handling command JSON: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Connection lost: ${e.message}")
        } finally {
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (_: Exception) {}
            periodicSyncJob.cancel()
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (_: Exception) {}
            closeActiveConnection()
            _status.value = PcSyncStatus.DISCONNECTED
            _connectedDeviceName.value = null
        }
    }

    private val photoCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun loadPhotoBase64(context: Context, photoUriString: String?): String? {
        if (photoUriString.isNullOrBlank()) return null
        photoCache[photoUriString]?.let { return it }

        return try {
            val uri = Uri.parse(photoUriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val originalBitmap = BitmapFactory.decodeStream(stream) ?: return null
                val maxDim = 96
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scaledBitmap = if (width > maxDim || height > maxDim) {
                    val ratio = width.toFloat() / height.toFloat()
                    val (newW, newH) = if (ratio > 1) {
                        Pair(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
                    } else {
                        Pair((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
                    }
                    Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
                } else {
                    originalBitmap
                }
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                photoCache[photoUriString] = b64
                b64
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun sendSyncData(writer: BufferedWriter) = withContext(Dispatchers.IO) {
        try {
            _status.value = PcSyncStatus.SYNCING

            val syncFavorites = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_FAVOURITES, true)
            val syncCallLogs = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CALL_LOGS, true)
            val syncContacts = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CONTACTS, true)
            val syncSms = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_SMS, true)
            val syncNotes = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_NOTES, true)
            val syncContactPfp = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CONTACT_PFP, true)

            // 1. Real Contacts (Only saved contacts from phone)
            val contactsArray = JSONArray()
            if (syncContacts || syncFavorites) {
                val realContacts = try {
                    contactsRepository.getContacts()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching contacts: ${e.message}")
                    emptyList()
                }

                val filtered = if (syncContacts) {
                    realContacts
                } else {
                    realContacts.filter { it.isFavorite }
                }

                for (c in filtered) {
                    val phone = c.phones.firstOrNull()?.number ?: c.phoneNumbers.firstOrNull() ?: ""
                    val pfpBase64 = if (syncContactPfp) loadPhotoBase64(context, c.photoUri) else null

                    val phonesJson = JSONArray()
                    if (c.phones.isNotEmpty()) {
                        for (p in c.phones) {
                            phonesJson.put(JSONObject().apply {
                                put("number", p.number)
                                put("type", p.type)
                                put("label", getPhoneTypeLabel(p.type, p.label))
                            })
                        }
                    } else {
                        for (num in c.phoneNumbers) {
                            phonesJson.put(JSONObject().apply {
                                put("number", num)
                                put("type", 2)
                                put("label", "Mobile")
                            })
                        }
                    }

                    val emailsJson = JSONArray()
                    for (e in c.emails) {
                        emailsJson.put(e)
                    }

                    val addressesJson = JSONArray()
                    for (a in c.addresses) {
                        addressesJson.put(a)
                    }

                    val accountsJson = JSONArray()
                    for (acc in c.sourceAccounts) {
                        accountsJson.put(acc)
                    }

                    val cObj = JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name.ifBlank { phone })
                        put("phone", phone)
                        put("isFavorite", c.isFavorite)
                        put("account", c.sourceAccounts.firstOrNull() ?: "Phone")
                        if (pfpBase64 != null) {
                            put("photoBase64", pfpBase64)
                        }
                        put("phones", phonesJson)
                        put("emails", emailsJson)
                        put("addresses", addressesJson)
                        put("accounts", accountsJson)
                        if (!c.note.isNullOrBlank()) {
                            put("note", c.note)
                        }
                    }
                    contactsArray.put(cObj)
                }
            }

            // 2. Real Call Logs
            val callLogsArray = JSONArray()
            if (syncCallLogs) {
                val realLogs = try {
                    callLogRepository.getCallLogs(limit = 100)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching call logs: ${e.message}")
                    emptyList()
                }

                for (log in realLogs) {
                    val lObj = JSONObject().apply {
                        put("id", "${log.number}_${log.date}")
                        put("name", log.name ?: log.number)
                        put("number", log.number)
                        put("type", when (log.type) {
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                            android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                            else -> "incoming"
                        })
                        put("timestamp", log.date)
                        put("duration", log.duration)
                        put("simSlot", log.simSlot)
                        put("count", log.count)
                    }
                    callLogsArray.put(lObj)
                }
            }

            // 3. Real SMS Threads
            val smsArray = JSONArray()
            if (syncSms) {
                val convs = try {
                    smsRepository.getConversations().take(30)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching SMS conversations: ${e.message}")
                    emptyList()
                }

                for (conv in convs) {
                    val msgs = try {
                        smsRepository.getMessagesForThread(conv.threadId).takeLast(25)
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val msgsArray = JSONArray()
                    for (m in msgs) {
                        val mObj = JSONObject().apply {
                            put("id", m.id.toString())
                            put("body", m.body)
                            put("timestamp", m.date)
                            put("isOutgoing", m.isOutgoing)
                            put("isPinned", prefs.isSmsMessagePinned(m.id))
                            put("isStarred", prefs.isSmsMessageStarred(m.id))
                        }
                        msgsArray.put(mObj)
                    }

                    val tObj = JSONObject().apply {
                        put("id", conv.threadId.toString())
                        put("contactName", conv.contactName ?: conv.address)
                        put("phone", conv.address)
                        put("snippet", conv.snippet)
                        put("timestamp", conv.date)
                        put("unreadCount", conv.unreadCount)
                        put("isPinned", prefs.getPinnedSmsThreads().contains(conv.threadId))
                        put("messages", msgsArray)
                    }
                    smsArray.put(tObj)
                }
            }

            // 4. Real Notes
            val notesArray = JSONArray()
            if (syncNotes) {
                val realNotes = try {
                    NoteManager.getAllNotes(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching notes: ${e.message}")
                    emptyList()
                }

                for (n in realNotes) {
                    val nObj = JSONObject().apply {
                        put("id", n.file.nameWithoutExtension)
                        put("title", if (n.contactName.isNotBlank() && n.contactName != "Unknown") n.contactName else "Note")
                        put("content", n.content)
                        put("timestamp", n.lastModified)
                    }
                    notesArray.put(nObj)
                }
            }

            val confirmPlacingCall = prefs.getBoolean(PreferenceManager.KEY_CONFIRM_PLACING_CALL, false)
            val enabledFeatures = JSONObject().apply {
                put("favourites", syncFavorites)
                put("callLogs", syncCallLogs)
                put("contacts", syncContacts)
                put("sms", syncSms)
                put("notes", syncNotes)
                put("contactPfp", syncContactPfp)
                put("confirmPlacingCall", confirmPlacingCall)
            }

            val starredMsgIds = JSONArray(prefs.getStarredSmsMessages().map { it.toString() })
            val pinnedMsgIds = JSONArray(prefs.getPinnedSmsMessages().map { it.toString() })
            val pinnedThreadIds = JSONArray(prefs.getPinnedSmsThreads().map { it.toString() })

            val categories = prefs.getAllSmsCategories()
            val hiddenCategories = prefs.getHiddenSmsCategories()
            val smsCategoriesArray = JSONArray()
            for (c in categories) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("isCustom", c.isCustom)
                obj.put("isVisible", !hiddenCategories.contains(c.id))
                val phones = JSONArray()
                for (p in c.contactPhones) {
                    phones.put(p)
                }
                obj.put("contactPhones", phones)
                smsCategoriesArray.put(obj)
            }

            val callCategories = prefs.getAllCallCategories()
            val hiddenCallCategories = prefs.getHiddenCallCategories()
            val callCategoriesArray = JSONArray()
            for (c in callCategories) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("isCustom", c.isCustom)
                obj.put("isVisible", !hiddenCallCategories.contains(c.id))
                val phones = JSONArray()
                for (p in c.contactPhones) {
                    phones.put(p)
                }
                obj.put("contactPhones", phones)
                callCategoriesArray.put(obj)
            }

            // Build wrapper payload
            val payload = JSONObject().apply {
                put("contacts", contactsArray)
                put("callLogs", callLogsArray)
                put("callCategories", callCategoriesArray)
                put("smsThreads", smsArray)
                put("smsCategories", smsCategoriesArray)
                put("notes", notesArray)
                put("starredSmsMessageIds", starredMsgIds)
                put("pinnedSmsMessageIds", pinnedMsgIds)
                put("pinnedSmsThreadIds", pinnedThreadIds)
                put("enabledFeatures", enabledFeatures)
                put("confirmPlacingCall", confirmPlacingCall)
                put("deviceName", Build.MODEL)
                put("telemetry", getDeviceTelemetry())
                put("timestamp", System.currentTimeMillis())
            }

            val root = JSONObject().apply {
                put("type", "sync_data")
                put("payload", payload)
            }

            writer.write(root.toString())
            writer.write("\n")
            writer.flush()

            _lastSyncTimestamp.value = System.currentTimeMillis()
            _status.value = PcSyncStatus.CONNECTED
            Log.i(TAG, "Sync payload sent: ${contactsArray.length()} contacts, ${callLogsArray.length()} calls, ${smsArray.length()} threads, ${notesArray.length()} notes")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending sync data: ${e.message}", e)
            _status.value = PcSyncStatus.ERROR
        }
    }

    private fun getDeviceTelemetry(): JSONObject {
        val obj = JSONObject()
        try {
            // 1. Battery
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, batteryFilter)
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            obj.put("batteryLevel", batteryPct)
            obj.put("isCharging", isCharging)

            // 2. Wi-Fi
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            val isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            if (isWifiConnected) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val rssi = wm?.connectionInfo?.rssi ?: -127
                val wifiLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    wm?.calculateSignalLevel(rssi) ?: 4
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.calculateSignalLevel(rssi, 5)
                }
                obj.put("wifiLevel", wifiLevel.coerceIn(0, 4))
                obj.put("isWifiConnected", true)
            } else {
                obj.put("wifiLevel", -1)
                obj.put("isWifiConnected", false)
            }

            // 3. Cellular Signal
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val cellLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    tm?.signalStrength?.level ?: -1
                } catch (_: Throwable) { -1 }
            } else {
                -1
            }
            obj.put("cellularLevel", cellLevel.coerceIn(-1, 4))
        } catch (e: Exception) {
            Log.w(TAG, "Error gathering telemetry: ${e.message}")
        }
        return obj
    }

    private fun sendTelemetry(writer: BufferedWriter) {
        try {
            val telem = getDeviceTelemetry()
            val root = JSONObject().apply {
                put("type", "telemetry")
                put("payload", telem)
            }
            writer.write(root.toString())
            writer.write("\n")
            writer.flush()
            Log.d(TAG, "Telemetry update sent: $telem")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending telemetry: ${e.message}")
        }
    }
}
