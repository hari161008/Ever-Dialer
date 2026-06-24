package com.coolappstore.everdialer.by.svhp.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.view.screen.BiometricCallActivity
import com.coolappstore.everdialer.by.svhp.view.screen.CallActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject

data class CallSession(
    val call: Call,
    val state: Int,
    val updateTime: Long = System.currentTimeMillis()
)

class CallService : InCallService() {

    private val contactsRepository: IContactsRepository by inject()
    private val prefs: PreferenceManager by inject()

    // ── Missed call notifications ───────────────────────────────────────────
    // Since Android 7.0 the default dialer is solely responsible for posting
    // missed-call notifications — Telecom no longer does this for us — so we
    // track each incoming call and notify if it's never answered.
    private data class IncomingCallInfo(var wasAnswered: Boolean = false)
    private val incomingCallTracking = mutableMapOf<Call, IncomingCallInfo>()

    private fun trackIncomingCallState(call: Call, state: Int) {
        val info = incomingCallTracking[call] ?: return
        when (state) {
            Call.STATE_ACTIVE -> info.wasAnswered = true
            Call.STATE_DISCONNECTED -> {
                incomingCallTracking.remove(call)
                if (!info.wasAnswered) {
                    val cause = try { call.details?.disconnectCause?.code } catch (_: Exception) { null }
                    if (cause != android.telecom.DisconnectCause.REJECTED) {
                        showMissedCallNotification(call)
                    }
                }
            }
        }
    }

    private fun showMissedCallNotification(call: Call) {
        try {
            val number = call.details?.handle?.schemeSpecificPart ?: ""
            val contact = if (number.isNotEmpty()) try { contactsRepository.getContactByNumber(number) } catch (_: Exception) { null } else null
            val displayName = contact?.name ?: number.ifEmpty { "Unknown" }

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(MISSED_CHANNEL_ID, "Missed Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        enableVibration(true)
                    }
                )
            }

            val contentIntent = PendingIntent.getActivity(
                this,
                number.hashCode(),
                Intent(this, MainActivity::class.java).apply {
                    action = "com.coolappstore.everdialer.OPEN_RECENTS"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, MISSED_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_phone_call)
                .setContentTitle("Missed call")
                .setContentText(displayName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())

            if (number.isNotEmpty()) {
                val callBackIntent = PendingIntent.getActivity(
                    this,
                    number.hashCode() + 1,
                    Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$number")),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(android.R.drawable.sym_action_call, "Call back", callBackIntent).build()
                )
            }

            nm.notify(MISSED_CALL_BASE_ID + (number.hashCode() and 0xFFFF), builder.build())
        } catch (_: Exception) {}
    }

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val CHANNEL_INCOMING_ID = "call_incoming_channel"
        private const val NOTIFICATION_ID = 101
        private const val MISSED_CHANNEL_ID = "missed_call_channel"
        private const val MISSED_CALL_BASE_ID = 2000

        private val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        /** True if [call] is a self-managed call placed/received by WhatsApp. */
        private fun isWhatsAppCall(call: Call): Boolean {
            val pkg = call.details?.accountHandle?.componentName?.packageName ?: return false
            return pkg in WHATSAPP_PACKAGES
        }

        private val _currentCallSession = MutableStateFlow<CallSession?>(null)
        val currentCallSession = _currentCallSession.asStateFlow()

        private val _heldCallSession = MutableStateFlow<CallSession?>(null)
        val heldCallSession = _heldCallSession.asStateFlow()

        private val _audioState = MutableStateFlow<CallAudioState?>(null)
        val audioState = _audioState.asStateFlow()

        private var instance: CallService? = null

        @Volatile private var isMerging = false

        // Set to true when "Add to call" is triggered so CallService knows to
        // auto-merge the second call once it becomes active, or restore call 1
        // if it is rejected/disconnected before being answered.
        @Volatile var isAddingToCall = false

        fun setMuted(muted: Boolean) { instance?.setMuted(muted) }
        fun setAudioRoute(route: Int) { instance?.setAudioRoute(route) }
        fun answerCall() { _currentCallSession.value?.call?.answer(VideoProfile.STATE_AUDIO_ONLY) }
        fun declineCall() { _currentCallSession.value?.call?.disconnect() }

        fun mergeCalls() {
            val primary = _currentCallSession.value?.call ?: return
            val secondary = _heldCallSession.value?.call ?: return
            isMerging = true
            var mergeSucceeded = false
            try {
                primary.conference(secondary)
                mergeSucceeded = true
            } catch (_: Exception) {}
            if (!mergeSucceeded) {
                try {
                    secondary.conference(primary)
                    mergeSucceeded = true
                } catch (_: Exception) { isMerging = false }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isMerging) {
                    isMerging = false
                    if (_currentCallSession.value == null && _heldCallSession.value != null) {
                        _currentCallSession.value = _heldCallSession.value
                        _heldCallSession.value = null
                    }
                }
            }, 4000)
        }

        fun hasHeldCall(): Boolean = _heldCallSession.value != null
    }

    // Callback for the primary (active/dialing) call
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            trackIncomingCallState(call, state)

            // "Add to call" flow — watch the outgoing 3rd-party call
            if (isAddingToCall && _currentCallSession.value?.call == call) {
                when (state) {
                    Call.STATE_ACTIVE -> {
                        // 3rd person answered — update current state and auto-merge
                        isAddingToCall = false
                        _currentCallSession.value = CallSession(call, state)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            mergeCalls()
                        }, 1200)
                        return
                    }
                    Call.STATE_DISCONNECTING -> {
                        // 3rd party call ending, wait for DISCONNECTED
                        _currentCallSession.value = CallSession(call, state)
                        return
                    }
                    Call.STATE_DISCONNECTED -> {
                        // 3rd person rejected/was cancelled/hung up → restore held call (call 1/2)
                        isAddingToCall = false
                        val held = _heldCallSession.value
                        if (held != null) {
                            _heldCallSession.value = null
                            _currentCallSession.value = CallSession(held.call, held.call.state)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try { held.call.unhold() } catch (_: Exception) {}
                            }, 300)
                        } else {
                            _currentCallSession.value = null
                            removeForeground()
                            cancelNotification()
                        }
                        return
                    }
                    else -> {
                        // DIALING / CONNECTING — update state and keep waiting
                        _currentCallSession.value = CallSession(call, state)
                        return
                    }
                }
            }

            // Normal state update
            when {
                _currentCallSession.value?.call == call -> _currentCallSession.value = CallSession(call, state)
                _heldCallSession.value?.call == call   -> _heldCallSession.value   = CallSession(call, state)
            }

            if (state == Call.STATE_DISCONNECTED) {
                if (_currentCallSession.value?.call == call) {
                    _currentCallSession.value = null
                    _heldCallSession.value?.let { held ->
                        _heldCallSession.value = null
                        _currentCallSession.value = CallSession(held.call, held.call.state)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try { held.call.unhold() } catch (_: Exception) {}
                        }, 300)
                    }
                } else if (_heldCallSession.value?.call == call) {
                    _heldCallSession.value = null
                }
                if (_currentCallSession.value == null) { removeForeground(); cancelNotification() }
            } else {
                updateNotification(call)
            }
        }
    }

    private val heldCallCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            trackIncomingCallState(call, state)
            _heldCallSession.value = CallSession(call, state)
            if (state == Call.STATE_DISCONNECTED) {
                _heldCallSession.value = null
            } else {
                updateNotification(call)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        call.unregisterCallback(heldCallCallback)

        // Safety net: if Telecom removed this call without ever delivering a
        // STATE_DISCONNECTED callback to us, still check for a missed call.
        incomingCallTracking.remove(call)?.let { info ->
            if (!info.wasAnswered) {
                val cause = try { call.details?.disconnectCause?.code } catch (_: Exception) { null }
                if (cause != android.telecom.DisconnectCause.REJECTED) showMissedCallNotification(call)
            }
        }

        if (isMerging) {
            if (_currentCallSession.value?.call == call) _currentCallSession.value = null
            if (_heldCallSession.value?.call == call)   _heldCallSession.value   = null
            return
        }

        // If isAddingToCall was set, the DISCONNECTED branch in onStateChanged
        // already promoted the held call. Guard against double-promotion by
        // checking whether currentCallSession still points to this call.
        if (isAddingToCall && _currentCallSession.value?.call == call) {
            // onStateChanged DISCONNECTED branch didn't fire (race) — handle here
            isAddingToCall = false
            val held = _heldCallSession.value
            if (held != null) {
                _heldCallSession.value = null
                _currentCallSession.value = CallSession(held.call, held.call.state)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try { held.call.unhold() } catch (_: Exception) {}
                }, 300)
            } else {
                _currentCallSession.value = null
                instance = null
                removeForeground()
                cancelNotification()
            }
            return
        }

        // Normal removal — if the current call is removed, promote held call if any
        if (_currentCallSession.value?.call == call) {
            _currentCallSession.value = null
            _heldCallSession.value?.let { held ->
                _heldCallSession.value = null
                _currentCallSession.value = CallSession(held.call, held.call.state)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try { held.call.unhold() } catch (_: Exception) {}
                }, 300)
            }
        } else if (_heldCallSession.value?.call == call) {
            _heldCallSession.value = null
        }

        if (_currentCallSession.value == null) {
            instance = null
            removeForeground()
            cancelNotification()
        }
    }

    private fun removeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    private fun isNumberBlocked(number: String): Boolean {
        val blockedList = prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        return blockedList.any { blocked ->
            val cb = blocked.replace(" ", "").replace("-", "")
            val cn = number.replace(" ", "").replace("-", "")
            cn.endsWith(cb) || cb.endsWith(cn)
        }
    }

    private fun launchCallActivity(answeredFromNotification: Boolean = false) {
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (answeredFromNotification) putExtra("ANSWERED_FROM_NOTIFICATION", true)
        }
        startActivity(intent)
    }

    private fun launchBiometricCallActivity(action: String) {
        val intent = Intent(this, BiometricCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("NOTIFICATION_PENDING_ACTION", action)
        }
        startActivity(intent)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        instance = this

        // WhatsApp registers its voice/video calls as a self-managed Telecom
        // connection. Because this InCallService declares
        // INCLUDE_SELF_MANAGED_CALLS, Telecom hands those calls to us too —
        // but Ever Dialer must never touch them: no callback, no notification,
        // no CallActivity, no missed-call alert. Bail out immediately so
        // WhatsApp's own call UI is left completely alone.
        if (isWhatsAppCall(call)) return

        val number = call.details.handle?.schemeSpecificPart ?: ""

        if (prefs.getBoolean(PreferenceManager.KEY_SILENCE_UNKNOWN, false) && number.isBlank()) {
            call.disconnect(); return
        }
        if (number.isNotBlank() && isNumberBlocked(number)) {
            call.disconnect(); return
        }

        if (call.state == Call.STATE_RINGING) {
            incomingCallTracking[call] = IncomingCallInfo()
        }

        if (isMerging) {
            isMerging = false
            call.registerCallback(callCallback)
            _currentCallSession.value = CallSession(call, call.state)
            _heldCallSession.value = null
            updateNotification(call)
            return
        }

        if (_currentCallSession.value != null && _currentCallSession.value?.state != Call.STATE_DISCONNECTED) {
            if (call.state != Call.STATE_RINGING) {
                // Second outgoing call (from "Add to call" or user-initiated)
                val prev = _currentCallSession.value
                if (prev != null) {
                    // If isAddingToCall, the original call was already held by CallActivity
                    if (!isAddingToCall) {
                        try { if (prev.call.state != Call.STATE_HOLDING) prev.call.hold() } catch (_: Exception) {}
                    }
                    prev.call.unregisterCallback(callCallback)
                    prev.call.registerCallback(heldCallCallback)
                    _heldCallSession.value = CallSession(prev.call, Call.STATE_HOLDING)
                }
                call.registerCallback(callCallback)
                _currentCallSession.value = CallSession(call, call.state)
            } else {
                // Incoming second call
                call.registerCallback(heldCallCallback)
                _heldCallSession.value = CallSession(call, call.state)
            }
            updateNotification(call)
            if (call.state != Call.STATE_RINGING) launchCallActivity()
            return
        }

        call.registerCallback(callCallback)
        _currentCallSession.value = CallSession(call, call.state)
        updateNotification(call)
        if (call.state != Call.STATE_RINGING) launchCallActivity()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        _audioState.value = audioState
        // Rebuild notification so mute/speaker button labels stay in sync
        _currentCallSession.value?.call?.let { updateNotification(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ANSWER_CALL"  -> {
                val phoneNumber = _currentCallSession.value?.call?.details?.handle?.schemeSpecificPart
                if (prefs.shouldGateCallWithBiometric(phoneNumber)) {
                    launchBiometricCallActivity("ANSWER")
                } else {
                    answerCall(); launchCallActivity(answeredFromNotification = true)
                }
            }
            "DECLINE_CALL" -> {
                val phoneNumber = _currentCallSession.value?.call?.details?.handle?.schemeSpecificPart
                if (prefs.shouldGateCallWithBiometric(phoneNumber)) {
                    launchBiometricCallActivity("DECLINE")
                } else {
                    declineCall()
                }
            }
            "MUTE_CALL"    -> setMuted(!(_audioState.value?.isMuted ?: false))
            "SPEAKER_CALL" -> {
                val isSpeaker = _audioState.value?.route == android.telecom.CallAudioState.ROUTE_SPEAKER
                setAudioRoute(if (isSpeaker) android.telecom.CallAudioState.ROUTE_EARPIECE else android.telecom.CallAudioState.ROUTE_SPEAKER)
            }
            "NOTES_CALL"   -> {
                val name   = intent.getStringExtra("contact_name") ?: "Unknown"
                val number = intent.getStringExtra("phone_number") ?: ""
                if (android.provider.Settings.canDrawOverlays(this)) {
                    FloatingNotesService.start(this, name, number)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(call: Call) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_INCOMING_ID, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
            })
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Ongoing Calls", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            })
        }

        val number = call.details.handle?.schemeSpecificPart ?: ""
        val contact = if (number.isNotEmpty()) try { contactsRepository.getContactByNumber(number) } catch (_: Exception) { null } else null
        val contactName = contact?.name ?: number.ifEmpty { "Unknown Number" }

        val fsi = PendingIntent.getActivity(this, 0,
            Intent(this, CallActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val answerPi = PendingIntent.getService(this, 1,
            Intent(this, CallService::class.java).apply { action = "ANSWER_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val declinePi = PendingIntent.getService(this, 2,
            Intent(this, CallService::class.java).apply { action = "DECLINE_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notesPi = PendingIntent.getService(this, 3,
            Intent(this, CallService::class.java).apply {
                action = "NOTES_CALL"
                putExtra("contact_name", contactName)
                putExtra("phone_number", number)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val mutePi = PendingIntent.getService(this, 4,
            Intent(this, CallService::class.java).apply { action = "MUTE_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val speakerPi = PendingIntent.getService(this, 5,
            Intent(this, CallService::class.java).apply { action = "SPEAKER_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val person = androidx.core.app.Person.Builder().setName(contactName).setImportant(true).build()
        val isRinging = call.state == Call.STATE_RINGING
        val isMuted   = _audioState.value?.isMuted ?: false
        val isSpeaker = _audioState.value?.route == android.telecom.CallAudioState.ROUTE_SPEAKER
        val channelId = if (isRinging) CHANNEL_INCOMING_ID else CHANNEL_ID

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(contactName)
            .setContentText(if (isRinging) "Incoming call" else "Active call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fsi, true)
            .setContentIntent(fsi)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSilent(!isRinging)
            .setDefaults(if (isRinging) NotificationCompat.DEFAULT_ALL else 0)
            .setStyle(
                if (isRinging) NotificationCompat.CallStyle.forIncomingCall(person, declinePi, answerPi)
                else           NotificationCompat.CallStyle.forOngoingCall(person, declinePi)
            )
            .setColorized(false)

        // Add extra action buttons for ongoing calls
        if (!isRinging) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notif_note,
                    "Notes",
                    notesPi
                ).build()
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    if (isMuted) R.drawable.ic_notif_mic_on else R.drawable.ic_notif_mic_off,
                    if (isMuted) "Unmute" else "Mute",
                    mutePi
                ).build()
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notif_speaker,
                    if (isSpeaker) "Earpiece" else "Speaker",
                    speakerPi
                ).build()
            )
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        else
            startForeground(NOTIFICATION_ID, notification)

        // Start/stop floating bubble based on preference
        if (call.state != android.telecom.Call.STATE_DISCONNECTED &&
            call.state != android.telecom.Call.STATE_DISCONNECTING) {
            maybeStartFloatingCall(contactName, number)
        }
    }

    private fun maybeStartFloatingCall(contactName: String, number: String) {
        if (!prefs.getBoolean(PreferenceManager.KEY_FLOATING_CALL, false)) return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        FloatingCallService.start(this, contactName, number)
    }

    private fun cancelNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }
}
