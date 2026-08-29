package com.coolappstore.everdialer.by.svhp.controller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager

class VolumeDndAccessibilityService : AccessibilityService() {

    private val sequenceBuffer = StringBuilder()
    private var lastPressTimestamp = 0L
    private var mediaSession: MediaSession? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info

        initMediaSession()
        registerScreenStateReceiver()
    }

    private fun initMediaSession() {
        try {
            if (mediaSession == null) {
                mediaSession = MediaSession(this, "EverDialerVolumeDnd").apply {
                    setFlags(
                        MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                    )
                    setPlaybackState(
                        PlaybackState.Builder()
                            .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                            .setActions(
                                PlaybackState.ACTION_PLAY_PAUSE or
                                        PlaybackState.ACTION_SKIP_TO_NEXT or
                                        PlaybackState.ACTION_SKIP_TO_PREVIOUS
                            )
                            .build()
                    )
                    setCallback(object : MediaSession.Callback() {
                        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                            @Suppress("DEPRECATION")
                            val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                                val kc = keyEvent.keyCode
                                if (kc == KeyEvent.KEYCODE_VOLUME_UP || kc == KeyEvent.KEYCODE_MEDIA_NEXT) {
                                    handleKeyInput('U')
                                    return true
                                } else if (kc == KeyEvent.KEYCODE_VOLUME_DOWN || kc == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                                    handleKeyInput('D')
                                    return true
                                }
                            }
                            return super.onMediaButtonEvent(mediaButtonIntent)
                        }
                    })
                    isActive = true
                }
            }
        } catch (_: Exception) {}
    }

    private fun registerScreenStateReceiver() {
        try {
            if (screenReceiver == null) {
                screenReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            Intent.ACTION_SCREEN_OFF -> {
                                mediaSession?.isActive = true
                            }
                            Intent.ACTION_SCREEN_ON -> {
                                mediaSession?.isActive = true
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
                registerReceiver(screenReceiver, filter)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaSession?.release()
            mediaSession = null
        } catch (_: Exception) {}
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver)
                screenReceiver = null
            }
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: service is primarily for volume key event interception
    }

    override fun onInterrupt() {
        sequenceBuffer.clear()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || event.action != KeyEvent.ACTION_DOWN) {
            return super.onKeyEvent(event)
        }

        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        val inputChar = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 'U' else 'D'
        val handled = handleKeyInput(inputChar)

        return if (handled) false else super.onKeyEvent(event)
    }

    private fun handleKeyInput(inputChar: Char): Boolean {
        val prefs = PreferenceManager(this)
        val isEnabled = prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, false)
        if (!isEnabled) {
            return false
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOff = powerManager?.isInteractive == false
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked == true

        val lockScreenOnly = prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, false)
        if (lockScreenOnly) {
            // Must be locked or screen off
            if (!isLocked && !isScreenOff) {
                sequenceBuffer.clear()
                return false
            }
        }

        val timeoutMs = prefs.getInt(
            PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS,
            PreferenceManager.DEFAULT_VOLUME_DND_TIMEOUT_MS
        ).toLong().coerceIn(100L, 5000L)

        val currentTime = System.currentTimeMillis()

        // If delay between key presses exceeds timeout, start again from the first
        if (currentTime - lastPressTimestamp > timeoutMs) {
            sequenceBuffer.clear()
        }
        lastPressTimestamp = currentTime

        val targetSequence = (prefs.getString(
            PreferenceManager.KEY_VOLUME_DND_SEQUENCE,
            PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
        ) ?: PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE)
            .uppercase()
            .filter { it == 'U' || it == 'D' }

        if (targetSequence.isEmpty()) {
            return false
        }

        sequenceBuffer.append(inputChar)

        if (targetSequence.startsWith(sequenceBuffer.toString())) {
            if (sequenceBuffer.toString() == targetSequence) {
                // Success: entire combination matched!
                sequenceBuffer.clear()

                // Acquire brief partial wake lock for smooth execution if screen is off
                try {
                    val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EverDialer:VolumeDndWakeLock")
                    wakeLock?.acquire(3000L)
                } catch (_: Exception) {}

                toggleDnd(this)
            }
        } else {
            // Mismatch: restart combination
            sequenceBuffer.clear()
            if (targetSequence.startsWith(inputChar.toString())) {
                sequenceBuffer.append(inputChar)
            }
        }

        return true
    }

    companion object {
        /**
         * Checks whether this app's AccessibilityService is actively enabled in System Settings.
         * Accurate with zero false readings.
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedComponentName = ComponentName(context, VolumeDndAccessibilityService::class.java)

            // Primary check: Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (!enabledServices.isNullOrBlank()) {
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                while (colonSplitter.hasNext()) {
                    val componentNameString = colonSplitter.next()
                    val enabledComponent = ComponentName.unflattenFromString(componentNameString)
                    if (enabledComponent != null && enabledComponent == expectedComponentName) {
                        return true
                    }
                }
            }

            // Secondary check: AccessibilityManager getEnabledAccessibilityServiceList
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val list = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            if (list != null) {
                for (info in list) {
                    val sInfo = info.resolveInfo?.serviceInfo
                    if (sInfo != null && sInfo.packageName == context.packageName &&
                        sInfo.name == VolumeDndAccessibilityService::class.java.name
                    ) {
                        return true
                    }
                }
            }

            return false
        }

        /**
         * Checks if Notification Policy Access (Do Not Disturb access) is granted.
         */
        fun isDndAccessGranted(context: Context): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            return nm?.isNotificationPolicyAccessGranted ?: false
        }

        /**
         * Opens System Accessibility Settings.
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        /**
         * Opens System Do Not Disturb / Notification Policy Access Settings.
         */
        fun openDndAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        /**
         * Toggles System Do Not Disturb mode on or off, providing vibration feedback.
         */
        fun toggleDnd(context: Context): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
            if (!nm.isNotificationPolicyAccessGranted) {
                // Vibrate error buzz (3 short ticks)
                performVibration(context, longArrayOf(0, 80, 80, 80, 80, 80))
                return false
            }

            try {
                val currentFilter = nm.currentInterruptionFilter
                val newFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }

                nm.setInterruptionFilter(newFilter)

                if (newFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                    // DND ON: 2 distinct pulses
                    performVibration(context, longArrayOf(0, 100, 100, 150))
                } else {
                    // DND OFF: 1 long smooth pulse
                    performVibration(context, longArrayOf(0, 200))
                }
                return true
            } catch (e: Exception) {
                return false
            }
        }

        private fun performVibration(context: Context, pattern: LongArray) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v?.vibrate(pattern, -1)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
