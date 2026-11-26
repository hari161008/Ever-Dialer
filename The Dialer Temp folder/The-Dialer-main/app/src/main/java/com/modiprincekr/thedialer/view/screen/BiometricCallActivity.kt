package com.modiprincekr.thedialer.view.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.VideoProfile
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.modiprincekr.thedialer.controller.CallService
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.theme.Rivo4Theme
import org.koin.android.ext.android.inject

class BiometricCallActivity : FragmentActivity() {

    private val prefs: PreferenceManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val action = intent?.getStringExtra("NOTIFICATION_PENDING_ACTION") ?: run { finish(); return }
        val biometricType = prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""

        // Verify we should actually gate this specific call
        val callPhoneNumber = CallService.incomingCallSession.value?.call?.details?.handle?.schemeSpecificPart
            ?: CallService.currentCallSession.value?.call?.details?.handle?.schemeSpecificPart
        if (!prefs.shouldGateCallWithBiometric(callPhoneNumber) || biometricType.isEmpty()) {
            // Lock scope excludes this number — perform action directly
            when (action) {
                "ANSWER" -> CallService.answerCall()
                "DECLINE" -> CallService.declineCall()
            }
            finish()
            return
        }

        setContent {
            Rivo4Theme {
                com.modiprincekr.thedialer.view.screen.auth.AuthLockScreen(
                    title = "The Dialer",
                    subtitle = "Verify your identity to access this call",
                    onSuccess = {
                        when (action) {
                            "ANSWER" -> CallService.answerCall()
                            "DECLINE" -> CallService.declineCall()
                        }
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var pendingTouchHapticFired = false
    private val touchSlop by lazy { android.view.ViewConfiguration.get(this).scaledTouchSlop }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev != null) {
            val touchOnlyEnabled = prefs.getBoolean(PreferenceManager.KEY_HAPTIC_TOUCH_ONLY, false)
            val appHapticsEnabled = prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)

            if (appHapticsEnabled) {
                when (ev.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        touchDownX = ev.x
                        touchDownY = ev.y
                        pendingTouchHapticFired = false
                        if (touchOnlyEnabled) {
                            val strength = prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light"
                            val customIntensity = prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                            com.modiprincekr.thedialer.view.components.performAppHaptic(this, strength, customIntensity, fromDirectTouch = true)
                            pendingTouchHapticFired = true
                        }
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = ev.x - touchDownX
                        val dy = ev.y - touchDownY
                        if (dx * dx + dy * dy > touchSlop * touchSlop) {
                            pendingTouchHapticFired = true
                        }
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        if (!pendingTouchHapticFired) {
                            val dx = ev.x - touchDownX
                            val dy = ev.y - touchDownY
                            if (dx * dx + dy * dy <= touchSlop * touchSlop) {
                                val strength = prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light"
                                val customIntensity = prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                                com.modiprincekr.thedialer.view.components.performAppHaptic(this, strength, customIntensity, fromDirectTouch = true)
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        pendingTouchHapticFired = true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
