package com.coolappstore.everdialer.by.svhp.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.getSimSlotForAccountHandle
import com.coolappstore.everdialer.by.svhp.controller.UssdRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.view.screen.BiometricCallActivity
import com.coolappstore.everdialer.by.svhp.view.screen.CallActivity
import com.coolappstore.everdialer.by.svhp.view.screen.settings.KEY_SELECTED_APP_ICON
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
    private val callRingStartTimes = mutableMapOf<Call, Long>()
    private val callAnsweredSet = mutableSetOf<Call>()

    private fun recordMissedCallDurationIfNeeded(call: Call) {
        val ringStart = callRingStartTimes.remove(call)
        val wasAnswered = callAnsweredSet.remove(call)
        if (ringStart != null && !wasAnswered) {
            val ringDurationSec = ((System.currentTimeMillis() - ringStart) / 1000L).coerceAtLeast(1L)
            val number = call.details?.handle?.schemeSpecificPart?.let { android.net.Uri.decode(it) } ?: ""
            val callDate = call.details?.creationTimeMillis?.takeIf { it > 0 } ?: ringStart
            if (number.isNotBlank()) {
                com.coolappstore.everdialer.by.svhp.controller.util.MissedCallDurationStore.saveDuration(
                    this, number, callDate, ringDurationSec
                )
                com.coolappstore.everdialer.by.svhp.controller.util.MissedCallDurationStore.updateProviderDuration(
                    this, number, ringDurationSec
                )
            }
        }
    }

    // ── Proximity screen-off (plain + "Device Orientation with Proximity Sensor") ──────────
    // This used to live entirely inside CallActivity, acquiring/releasing a real
    // PROXIMITY_SCREEN_OFF_WAKE_LOCK from there. That wake lock is only reliably honored by the
    // system while the acquiring app has an actively foregrounded window — so the moment the user
    // switched away to the main Ever Dialer app during a call (leaving CallActivity backgrounded
    // behind it), the near-ear screen-off would silently stop taking effect, even though the
    // sensor readings and gate logic underneath kept ticking along just fine. Re-opening the
    // in-call UI put CallActivity back on top and "fixed" it again — but only by accident.
    // Owning it here instead, in the InCallService that already runs continuously for the whole
    // call as a foreground service regardless of which Activity (if any) is on top, removes that
    // dependency on window focus entirely, for both the plain and orientation-gated modes.
    private var proxSensorManager: SensorManager? = null
    private var proxProximitySensor: Sensor? = null
    private var proxAccelerometer: Sensor? = null
    private var proxWakeLock: PowerManager.WakeLock? = null
    private var mLastProximityNear: Boolean? = null
    private var mInclinationValue: Int? = null
    private var mIsSlanted: Boolean = false

    private fun acquireProxWakeLock() { if (proxWakeLock?.isHeld == false) proxWakeLock?.acquire() }
    private fun releaseProxWakeLock() { if (proxWakeLock?.isHeld == true) proxWakeLock?.release() }

    /** Re-evaluates from the latest cached sensor readings + current call/audio state and
     *  directly acquires/releases the real proximity wake lock. Called on every proximity and
     *  accelerometer update (so either sensor changing, or the phone leaving the ear, reacts
     *  immediately), and whenever call state or audio route changes. */
    private fun updateProximityScreenOffGate() {
        val session = _currentCallSession.value
        val callState = session?.state
        val isSpeakerOn = _audioState.value?.route == CallAudioState.ROUTE_SPEAKER
        val inCallForScreenOff = (callState == Call.STATE_ACTIVE || callState == Call.STATE_DIALING) && !isSpeakerOn

        if (!inCallForScreenOff) {
            releaseProxWakeLock()
            return
        }

        val orientationGateEnabled = prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_ORIENTATION_BG, false)
        if (orientationGateEnabled) {
            val isNear = mLastProximityNear == true
            val inclination = mInclinationValue
            val orientedToEar = inclination != null && inclination in -90..90 && mIsSlanted
            if (isNear && orientedToEar) acquireProxWakeLock() else releaseProxWakeLock()
            return
        }

        // Plain mode: proximity sensor alone, no orientation/tilt gating.
        val proximityBgEnabled = prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)
        if (proximityBgEnabled && mLastProximityNear == true) {
            acquireProxWakeLock()
        } else {
            releaseProxWakeLock()
        }
    }

    private val proxSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val az = event.values[2]
                    val normOfG = kotlin.math.sqrt(ax * ax + ay * ay + az * az)
                    if (normOfG > 0f) {
                        val nx = ax / normOfG
                        val ny = ay / normOfG
                        val nz = az / normOfG
                        mInclinationValue = Math.toDegrees(kotlin.math.atan2(nx, ny).toDouble()).roundToInt()
                        val angleFromFlatDeg = Math.toDegrees(kotlin.math.acos(nz.coerceIn(-1f, 1f).toDouble()))
                        val angleFromFlatOrBelow = kotlin.math.min(angleFromFlatDeg, 180.0 - angleFromFlatDeg)
                        val slantThreshold = prefs.getFloat(
                            PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD,
                            PreferenceManager.DEFAULT_PROXIMITY_ORIENTATION_SLANT_THRESHOLD
                        )
                        mIsSlanted = angleFromFlatOrBelow > slantThreshold.toDouble()
                    }
                    updateProximityScreenOffGate()
                }
                Sensor.TYPE_PROXIMITY -> {
                    val maxRange = event.sensor.maximumRange
                    mLastProximityNear = event.values[0] < maxRange * 0.5f
                    updateProximityScreenOffGate()
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun setupProximityScreenOff() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        proxWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Rivo::ServiceProx")
        proxSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proxProximitySensor = proxSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proxAccelerometer = proxSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proxProximitySensor?.let { proxSensorManager?.registerListener(proxSensorListener, it, SensorManager.SENSOR_DELAY_UI) }
        proxAccelerometer?.let { proxSensorManager?.registerListener(proxSensorListener, it, SensorManager.SENSOR_DELAY_UI) }

        // Also re-evaluate whenever the call/audio state or the relevant settings change —
        // covers the moments proxSensorListener alone wouldn't (e.g. the call just went ACTIVE,
        // or speakerphone was toggled, with no new sensor reading yet).
        proxScope.launch {
            combine(_currentCallSession, _audioState, prefs.settingsChanged) { _, _, _ -> Unit }
                .collect { updateProximityScreenOffGate() }
        }
    }

    private fun teardownProximityScreenOff() {
        releaseProxWakeLock()
        proxSensorManager?.unregisterListener(proxSensorListener)
        proxScopeJob.cancel()
    }

    private val proxScopeJob = SupervisorJob()
    private val proxScope = CoroutineScope(Dispatchers.Main + proxScopeJob)

    override fun onCreate() {
        super.onCreate()
        setupProximityScreenOff()
    }

    override fun onDestroy() {
        super.onDestroy()
        teardownProximityScreenOff()
    }

    /**
     * The ongoing/incoming-call notification's small icon should look like whichever app icon
     * the user currently has selected (Settings > App Icon), instead of a generic stock phone
     * glyph that doesn't match and can appear mirrored/"inverted" next to it. Each app icon
     * variant already ships a monochrome adaptive-icon layer designed for exactly this purpose.
     */
    private fun currentCallSmallIcon(): Int {
        return when (prefs.getString(KEY_SELECTED_APP_ICON, "default")) {
            "phone"        -> R.drawable.ic_notif_call_phone
            "custom_phone" -> R.drawable.ic_notif_call_custom_phone
            "google"       -> R.drawable.ic_notif_call_google
            "nothing"      -> R.drawable.ic_notif_call_nothing
            else           -> R.drawable.ic_notif_call_default
        }
    }

    /** Loads the contact's photo (if any) as a square bitmap suitable for a Person/large icon. */
    private fun loadContactPhotoBitmap(photoUri: String?): android.graphics.Bitmap? {
        if (photoUri.isNullOrEmpty()) return null
        return try {
            contentResolver.openInputStream(android.net.Uri.parse(photoUri))?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    

    

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val CHANNEL_INCOMING_ID = "call_incoming_channel"
        private const val NOTIFICATION_ID = 101

        private val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        // Third-party apps that register their own voice/video calls as
        // self-managed Telecom connections. Because CallService declares
        // INCLUDE_SELF_MANAGED_CALLS, Telecom hands these to us too — but
        // Ever Dialer must never touch them (no CallActivity, no notification,
        // no cellular call side-effects). Snapchat is one such app: without
        // this exclusion its self-managed call was being treated like a real
        // cellular call by this service.
        private val SELF_MANAGED_THIRD_PARTY_PACKAGES = WHATSAPP_PACKAGES + setOf(
            "com.snapchat.android"
        )

        /** True if [call] is a self-managed call placed/received by a third-party app (WhatsApp, Snapchat, etc.) that Ever Dialer must not touch. */
        private fun isWhatsAppCall(call: Call): Boolean {
            val pkg = call.details?.accountHandle?.componentName?.packageName ?: return false
            return pkg in SELF_MANAGED_THIRD_PARTY_PACKAGES
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
        fun answerCall() {
            val call = _currentCallSession.value?.call
                ?: _heldCallSession.value?.call
                ?: instance?.calls?.find { it.state == Call.STATE_RINGING }
                ?: instance?.calls?.firstOrNull()
            try {
                call?.answer(VideoProfile.STATE_AUDIO_ONLY)
            } catch (_: Exception) {}
            try {
                instance?.let { s ->
                    val showUi = PreferenceManager(s).getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true)
                    if (showUi) {
                        s.launchCallActivity(answeredFromNotification = true)
                    }
                }
            } catch (_: Exception) {}
        }
        fun declineCall() {
            val call = _currentCallSession.value?.call
                ?: _heldCallSession.value?.call
                ?: instance?.calls?.firstOrNull()
            try {
                call?.disconnect()
            } catch (_: Exception) {}
        }

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
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            // The call's handle/caller info can arrive or change AFTER onCallAdded (e.g. some
            // carriers/OEMs deliver the ringing call before its number is attached, or update
            // caller ID data slightly later). Since the notification and the incoming/ongoing
            // call UI both derive the displayed name from call.details.handle at the moment
            // they're built, missing this callback meant they never re-resolved the contact
            // once the number showed up — leaving the name blank even though the call log
            // (built later, once the call ends) resolved it fine. Refresh both here.
            if (_currentCallSession.value?.call == call) {
                _currentCallSession.value = CallSession(call, call.state)
                updateNotification(call)
            } else if (_heldCallSession.value?.call == call) {
                _heldCallSession.value = CallSession(call, call.state)
                updateNotification(call)
            }
        }

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

            android.util.Log.d(
                "EverDialerCall",
                "call state -> ${stateName(state)} number=${call.details?.handle} account=${call.details?.accountHandle}"
            )

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

            if (state == Call.STATE_RINGING) {
                callRingStartTimes.getOrPut(call) { System.currentTimeMillis() }
            } else if (state == Call.STATE_ACTIVE) {
                callAnsweredSet.add(call)
                callConnectTimes.getOrPut(call) { System.currentTimeMillis() }
                com.coolappstore.evercallrecorder.by.svhp.services.call.CallSessionManager.getInstance(this@CallService)
                    .notifyCallAnsweredInDialer()
            }

            if (state == Call.STATE_DISCONNECTED) {
                recordMissedCallDurationIfNeeded(call)
                val cause = call.details?.disconnectCause
                android.util.Log.w(
                    "EverDialerCall",
                    "call DISCONNECTED number=${call.details?.handle} code=${cause?.code} " +
                        "label=${cause?.label} description=${cause?.description} reason=${cause?.reason}"
                )
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
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            if (_heldCallSession.value?.call == call) {
                _heldCallSession.value = CallSession(call, call.state)
                updateNotification(call)
            }
        }

        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            RaiseToAnswerManager.onCallStateChanged(this@CallService, call)
            if (state == Call.STATE_RINGING) {
                callRingStartTimes.getOrPut(call) { System.currentTimeMillis() }
            } else if (state == Call.STATE_ACTIVE) {
                callAnsweredSet.add(call)
                callConnectTimes.getOrPut(call) { System.currentTimeMillis() }
            }
            _heldCallSession.value = CallSession(call, state)
            if (state == Call.STATE_DISCONNECTED) {
                recordMissedCallDurationIfNeeded(call)
                _heldCallSession.value = null
            } else {
                updateNotification(call)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        RaiseToAnswerManager.stop(this)
        recordMissedCallDurationIfNeeded(call)
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

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.updateBadge(this)
            } catch (_: Exception) {}
        }, 600)
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

    private fun isNumberBlocked(number: String): Boolean =
        com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.isBlocked(prefs, number)

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

    private fun stateName(state: Int): String = when (state) {
        Call.STATE_NEW -> "NEW"
        Call.STATE_RINGING -> "RINGING"
        Call.STATE_DIALING -> "DIALING"
        Call.STATE_ACTIVE -> "ACTIVE"
        Call.STATE_HOLDING -> "HOLDING"
        Call.STATE_DISCONNECTED -> "DISCONNECTED"
        Call.STATE_CONNECTING -> "CONNECTING"
        Call.STATE_DISCONNECTING -> "DISCONNECTING"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
        Call.STATE_PULLING_CALL -> "PULLING_CALL"
        Call.STATE_AUDIO_PROCESSING -> "AUDIO_PROCESSING"
        Call.STATE_SIMULATED_RINGING -> "SIMULATED_RINGING"
        else -> "UNKNOWN($state)"
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        instance = this
        val direction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) call.details?.callDirection else null
        android.util.Log.d(
            "EverDialerCall",
            "onCallAdded number=${call.details?.handle} state=${stateName(call.state)} " +
                "account=${call.details?.accountHandle} isOutgoing=$direction"
        )

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
            callRingStartTimes.getOrPut(call) { System.currentTimeMillis() }
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
            if (call.state != Call.STATE_RINGING) {
                launchCallActivity()
            } else if (prefs.getBoolean(PreferenceManager.KEY_SHOW_FULL_SCREEN_INCOMING_ON_ANY_APPS, false)) {
                launchCallActivity()
            }
            return
        }

        call.registerCallback(callCallback)
        _currentCallSession.value = CallSession(call, call.state)
        updateNotification(call)
        if (call.state != Call.STATE_RINGING) {
            launchCallActivity()
        } else if (prefs.getBoolean(PreferenceManager.KEY_SHOW_FULL_SCREEN_INCOMING_ON_ANY_APPS, false)) {
            launchCallActivity()
        }
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
                    answerCall()
                    if (prefs.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true)) {
                        launchCallActivity(answeredFromNotification = true)
                    }
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
            "SILENCE_CALL" -> {
                com.coolappstore.everdialer.by.svhp.controller.util.silenceRingingCall(this)
            }
            "MUTE_CALL"    -> setMuted(!(_audioState.value?.isMuted ?: false))
            "SPEAKER_CALL" -> {
                val isSpeaker = _audioState.value?.route == android.telecom.CallAudioState.ROUTE_SPEAKER
                setAudioRoute(if (isSpeaker) android.telecom.CallAudioState.ROUTE_EARPIECE else android.telecom.CallAudioState.ROUTE_SPEAKER)
            }
            "HOLD_CALL"    -> {
                val call = _currentCallSession.value?.call
                try {
                    if (call?.state == Call.STATE_HOLDING) call.unhold() else call?.hold()
                } catch (_: Exception) {}
            }
            "BLUETOOTH_CALL" -> {
                val isBluetooth = _audioState.value?.route == android.telecom.CallAudioState.ROUTE_BLUETOOTH
                setAudioRoute(if (isBluetooth) android.telecom.CallAudioState.ROUTE_SPEAKER else android.telecom.CallAudioState.ROUTE_BLUETOOTH)
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
        val holdPi = PendingIntent.getService(this, 6,
            Intent(this, CallService::class.java).apply { action = "HOLD_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val bluetoothPi = PendingIntent.getService(this, 7,
            Intent(this, CallService::class.java).apply { action = "BLUETOOTH_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val contactPhotoBitmap = loadContactPhotoBitmap(if (isHiddenContact && hideNames) null else contact?.photoUri)
        val personBuilder = Person.Builder().setName(contactName).setImportant(true)
        if (contactPhotoBitmap != null) {
            personBuilder.setIcon(IconCompat.createWithAdaptiveBitmap(contactPhotoBitmap))
        }
        val person = personBuilder.build()
        val isRinging = call.state == Call.STATE_RINGING
        val isMuted   = _audioState.value?.isMuted ?: false
        val isSpeaker = _audioState.value?.route == android.telecom.CallAudioState.ROUTE_SPEAKER
        val isOnHold  = call.state == Call.STATE_HOLDING
        val bluetoothAvailable = (_audioState.value?.supportedRouteMask ?: 0) and android.telecom.CallAudioState.ROUTE_BLUETOOTH != 0
        val isBluetooth = _audioState.value?.route == android.telecom.CallAudioState.ROUTE_BLUETOOTH
        val isFullScreenIncoming = isRinging && prefs.getBoolean(PreferenceManager.KEY_SHOW_FULL_SCREEN_INCOMING_ON_ANY_APPS, false)
        val channelId = if (isRinging && !isFullScreenIncoming) CHANNEL_INCOMING_ID else CHANNEL_ID

        // Use the call's real, authoritative connect time from Telecom (the same source
        // CallActivity's on-screen timer already trusts) — not "now" — so the notification's
        // elapsed-time chronometer stays correct across every later rebuild (mute/speaker
        // toggles, held-call updates, etc.) instead of restarting from zero each time.
        val connectTime = call.details?.connectTimeMillis?.takeIf { it > 0L }
            ?: callConnectTimes.getOrPut(call) { System.currentTimeMillis() }

        val silencePi = PendingIntent.getService(this, 8,
            Intent(this, CallService::class.java).apply { action = "SILENCE_CALL" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val isDualSim = prefs.getActiveSimCount() >= 2
        val simSlot = if (isDualSim) getSimSlotForAccountHandle(this, call.details?.accountHandle) else -1
        val contentText = buildCallNotificationContentText(this, isRinging, isDualSim, simSlot)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(currentCallSmallIcon())
            .setContentTitle(contactName)
            .setContentText(contentText)
            .apply {
                if (isDualSim && simSlot in 0..1) {
                    setSubText(buildSimBadgeSpan(this@CallService, simSlot))
                }
            }
            .setPriority(if (isFullScreenIncoming) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .apply {
                if (!isFullScreenIncoming) {
                    setFullScreenIntent(fsi, true)
                }
            }
            .setContentIntent(fsi)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSilent(!isRinging || isFullScreenIncoming)
            .setDefaults(if (isRinging && !isFullScreenIncoming) NotificationCompat.DEFAULT_ALL else 0)
            .setWhen(connectTime)
            .setShowWhen(!isRinging)
            .setUsesChronometer(!isRinging)
            .setStyle(
                if (isRinging) NotificationCompat.CallStyle.forIncomingCall(person, declinePi, answerPi)
                else           NotificationCompat.CallStyle.forOngoingCall(person, declinePi)
            )
            .setColorized(false)

        val showIncomingMute = prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_MUTE_BUTTON, false)
        if (isRinging && showIncomingMute) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notif_volume_off,
                    "Mute",
                    silencePi
                ).build()
            )
        }

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
            builder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notif_hold,
                    if (isOnHold) "Resume" else "Hold",
                    holdPi
                ).build()
            )
            if (bluetoothAvailable) {
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_notif_bluetooth,
                        if (isBluetooth) "Speaker" else "Bluetooth",
                        bluetoothPi
                    ).build()
                )
            }
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        else
            startForeground(NOTIFICATION_ID, notification)

        // Start/stop floating bubble based on preference
        if (call.state != android.telecom.Call.STATE_DISCONNECTED &&
            call.state != android.telecom.Call.STATE_DISCONNECTING) {
            maybeStartFloatingCall(contactName, number, if (isHiddenContact && hideNames) null else contact?.photoUri)
        }
    }

    private fun maybeStartFloatingCall(contactName: String, number: String, photoUri: String?) {
        if (!prefs.getBoolean(PreferenceManager.KEY_FLOATING_CALL, false)) return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        FloatingCallService.start(this, contactName, number, photoUri)
    }

    private fun cancelNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }

    private fun createSimBadgeBitmap(context: Context, slot: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (16 * density).toInt().coerceAtLeast(1)
        val heightPx = (19 * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (slot == 0) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#C62828")
            style = Paint.Style.FILL
        }
        val cornerRadius = 4 * density
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 10f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = textPaint.fontMetrics
        val textY = (heightPx - (fontMetrics.descent + fontMetrics.ascent)) / 2f
        canvas.drawText((slot + 1).toString(), widthPx / 2f, textY, textPaint)

        return bitmap
    }

    private fun buildSimBadgeSpan(context: Context, simSlot: Int): CharSequence {
        if (simSlot !in 0..1) return ""
        val simTag = "SIM ${simSlot + 1}"
        val ssb = SpannableStringBuilder(simTag)
        try {
            val bitmap = createSimBadgeBitmap(context, simSlot)
            val align = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ImageSpan.ALIGN_CENTER else ImageSpan.ALIGN_BOTTOM
            val imageSpan = ImageSpan(context, bitmap, align)
            ssb.setSpan(imageSpan, 0, simTag.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } catch (_: Exception) {}
        return ssb
    }

    private fun buildCallNotificationContentText(
        context: Context,
        isRinging: Boolean,
        isDualSim: Boolean,
        simSlot: Int
    ): CharSequence {
        val baseText = if (isRinging) "Incoming call" else "Active call"
        if (!isDualSim || simSlot !in 0..1) return baseText

        val simTag = "SIM ${simSlot + 1}"
        val fullText = "$simTag  $baseText"
        val ssb = SpannableStringBuilder(fullText)
        try {
            val bitmap = createSimBadgeBitmap(context, simSlot)
            val align = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ImageSpan.ALIGN_CENTER else ImageSpan.ALIGN_BOTTOM
            val imageSpan = ImageSpan(context, bitmap, align)
            ssb.setSpan(imageSpan, 0, simTag.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } catch (_: Exception) {}
        return ssb
    }
}
