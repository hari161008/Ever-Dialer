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
import com.coolappstore.everdialer.by.svhp.view.screen.settings.PasswordDialogContent
import com.coolappstore.everdialer.by.svhp.view.screen.settings.PinDialogContent
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

        setContent {
            Rivo4Theme {
                val activity = this
                BiometricFloatingUi(
                    biometricType  = biometricType,
                    activity       = activity,
                    expectedPin      = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PIN, "") ?: "",
                    expectedPassword = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "") ?: "",
                    onSuccess = {
                        val call = CallService.currentCallSession.value?.call
                        when (action) {
                            "ANSWER" -> {
                                try { call?.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {}
                                startActivity(Intent(activity, CallActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    putExtra("ANSWERED_FROM_NOTIFICATION", true)
                                })
                            }
                            "DECLINE" -> try { call?.disconnect() } catch (_: Exception) {}
                        }
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun BiometricFloatingUi(
    biometricType: String,
    activity: FragmentActivity,
    expectedPin: String,
    expectedPassword: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { if (biometricType != "system") onDismiss() }
    ) {
        when (biometricType) {
            "system" -> {
                LaunchedEffect(Unit) {
                    val executor = ContextCompat.getMainExecutor(activity)
                    val prompt = BiometricPrompt(
                        activity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onDismiss() }
                            override fun onAuthenticationFailed() {}
                        }
                    )
                    prompt.authenticate(
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Ever Dialer")
                            .setSubtitle("Verify your identity to access this call")
                            .setNegativeButtonText("Cancel")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                            .build()
                    )
                }
            }
            "pin", "password" -> {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp)
                        .wrapContentHeight()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    when (biometricType) {
                        "pin" -> PinDialogContent(
                            title          = "Enter PIN",
                            isVerify       = true,
                            expectedPin    = expectedPin,
                            showCloseButton = true,
                            onConfirm      = { onSuccess() },
                            onDismiss      = onDismiss
                        )
                        "password" -> PasswordDialogContent(
                            title            = "Enter Password",
                            isVerify         = true,
                            expectedPassword = expectedPassword,
                            showCloseButton  = true,
                            onConfirm        = { onSuccess() },
                            onDismiss        = onDismiss
                        )
                    }
                }
            }
        }
    }
}
