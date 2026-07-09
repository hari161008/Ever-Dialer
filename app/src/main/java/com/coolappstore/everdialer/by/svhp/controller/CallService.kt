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
import com.coolappstore.everdialer.by.svhp.controller.UssdRepository
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

    // Bug fix: the ongoing-call notification's elapsed-time display was driven off whatever
    // System.currentTimeMillis() happened to be at the moment updateNotification() rebuilt the
    // notification (setWhen() was never set, so NotificationCompat.Builder defaulted it to
    // "now"). updateNotification() gets called for lots of things besides the call actually
    // connecting — mute toggle, speaker toggle, held-call updates — so every one of those
    // silently reset the notification's start time and made the displayed call duration drift
    // away from the real one. This map remembers the real moment each call became ACTIVE once,
    // so every later rebuild reuses that same timestamp instead of a fresh "now".
    private val callConnectTimes = mutableMapOf<Call, Long>()

    

    

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val CHANNEL_INCOMING_ID = "call_incoming_channel"
        private const val NOTIFICATION_ID = 101

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
        override fun onConnectionEvent(call: Call, event: String, extras: android.os.Bundle?) {
            super.onConnectionEvent(call, event, extras)
            val number = call.details?.handle?.schemeSpecificPart?.let { android.net.Uri.decode(it) } ?: ""
            if (isUssdNumber(number)) {
                val resp = extras?.let { b ->
                    b.getString("ussdResult") ?: b.getString("android.telecom.extra.ussd_message")
                    ?: b.getString("android.telephony.extra.USSD_RESPONSE")
                    ?: b.getString("response") ?: b.getString("result") ?: b.getString("data") ?: b.getString("message")
                }
                if (!resp.isNullOrBlank()) UssdRepository.post(number, resp)
            }
        }

        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)

            RaiseToAnswerManager.onCallStateChanged(this@CallService, call)

            // "Add to call" flow — watch the outgoing 3rd-party call
            if (isAddingToCall && _currentCallSession.value?.call == call) {
                when (state) {
                    Call.STATE_ACTIVE -> {
                        // 3rd person answered — update current state and auto-merge
                        isAddingToCall = false
                        callConnectTimes.getOrPut(call) { System.currentTimeMillis() }
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

            // Call reached its "answered/connected" state through this dialer (outgoing: call timer started;
            // incoming: accepted here). Lets the recorder module start recording when the
            // "Record Only When Call Answered" preference is enabled. Safe to call multiple times.
            if (state == Call.STATE_ACTIVE) {
                callConnectTimes.getOrPut(call) { System.currentTimeMillis() }
                com.coolappstore.evercallrecorder.by.svhp.services.call.CallSessionManager.getInstance(this@CallService)
                    .notifyCallAnsweredInDialer()
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
            RaiseToAnswerManager.onCallStateChanged(this@CallService, call)
            if (state == Call.STATE_ACTIVE) callConnectTimes.getOrPut(call) { System.currentTimeMillis() }
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
        RaiseToAnswerManager.stop(this)
        call.unregisterCallback(callCallback)
        call.unregisterCallback(heldCallCallback)
        callConnectTimes.remove(call)

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

    /** Returns true for any MMI / USSD code like *124# *#06# ##002# *21*N# */
    private fun isUssdNumber(number: String): Boolean {
        if (number.isBlank()) return false
        val n = android.net.Uri.decode(number).trim()
        return (n.startsWith("*") || n.startsWith("#")) && n.endsWith("#")
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

        val number = call.details.handle?.schemeSpecificPart
            ?.let { android.net.Uri.decode(it) } ?: ""

        // ── USSD / MMI outgoing calls ────────────────────────────────────────
        // Do NOT launch CallActivity for codes like *124# *#06# ##002# *21*N#.
        // com.android.phone owns MMI/USSD processing at the RIL level and shows
        // its own system dialog — just return and let it handle everything.
        val isUssd = call.state != Call.STATE_RINGING && isUssdNumber(number)
        if (isUssd) return
        // ────────────────────────────────────────────────────────────────────

        if (prefs.getBoolean(PreferenceManager.KEY_SILENCE_UNKNOWN, false) && number.isBlank() && call.state == Call.STATE_RINGING) {
            call.disconnect(); return
        }
        if (number.isNotBlank() && call.state == Call.STATE_RINGING && isNumberBlocked(number)) {
            call.disconnect(); return
        }

        if (call.state == Call.STATE_RINGING) {
            RaiseToAnswerManager.onCallStateChanged(this, call)
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
        val hideNames = prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false)
        val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
        val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet() else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
        val isHiddenContact = contact != null && contact.id in hiddenIds
        val contactName = when {
            isHiddenContact && hideNames -> number.ifEmpty { "Unknown Number" }
            else -> contact?.name ?: number.ifEmpty { "Unknown Number" }
        }

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

        // Use the call's real, authoritative connect time from Telecom (the same source
        // CallActivity's on-screen timer already trusts) — not "now" — so the notification's
        // elapsed-time chronometer stays correct across every later rebuild (mute/speaker
        // toggles, held-call updates, etc.) instead of restarting from zero each time.
        val connectTime = call.details?.connectTimeMillis?.takeIf { it > 0L }
            ?: callConnectTimes.getOrPut(call) { System.currentTimeMillis() }

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
            .setWhen(connectTime)
            .setShowWhen(!isRinging)
            .setUsesChronometer(!isRinging)
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
