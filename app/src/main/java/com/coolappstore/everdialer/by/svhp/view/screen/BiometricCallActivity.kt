package com.coolappstore.everdialer.by.svhp.view.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.VideoProfile
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.coolappstore.everdialer.by.svhp.controller.CallService
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
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
                com.coolappstore.everdialer.by.svhp.view.screen.auth.AuthLockScreen(
                    title = "Ever Dialer",
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
                            com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic(this, strength, customIntensity, fromDirectTouch = true)
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
                                com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic(this, strength, customIntensity, fromDirectTouch = true)
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
