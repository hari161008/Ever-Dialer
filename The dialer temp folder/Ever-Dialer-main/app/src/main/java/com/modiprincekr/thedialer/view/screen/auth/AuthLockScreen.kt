package com.modiprincekr.thedialer.view.screen.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.screen.settings.PasswordCredentialView
import com.modiprincekr.thedialer.view.screen.settings.PatternCredentialView
import com.modiprincekr.thedialer.view.screen.settings.PinCredentialView
import org.koin.compose.koinInject

/**
 * Modern Material 3 Lock Screen featuring Smart Biometric Fallback.
 *
 * When System Biometrics is configured, it initiates the system prompt. If the prompt fails
 * or is cancelled by the user, the screen transitions to an elegant fallback state with a
 * modern TextButton at the bottom-left ("Use PIN", "Use Pattern", or "Use Password")
 * allowing instant fallback to the configured secondary credential.
 */
@Composable
fun AuthLockScreen(
    title: String = "The Dialer",
    subtitle: String = "Verify your identity to continue",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    canDismiss: Boolean = true,
    modifier: Modifier = Modifier
) {
    val prefs: PreferenceManager = koinInject()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val biometricType = remember { prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: "" }
    val backupType = remember { prefs.getBiometricsBackupType() }
    val expectedPin = remember { prefs.getString(PreferenceManager.KEY_BIOMETRICS_PIN, "") ?: "" }
    val expectedPassword = remember { prefs.getString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "") ?: "" }
    val expectedPattern = remember { prefs.getString(PreferenceManager.KEY_BIOMETRICS_PATTERN, "") ?: "" }
    val isPatternVisible = remember { prefs.isPatternVisible() }

    // Active credential state: null means showing biometric prompt / fallback view;
    // "pin", "pattern", "password" means displaying the respective credential UI.
    var activeCredential by remember {
        mutableStateOf(
            when (biometricType) {
                "pin" -> "pin"
                "pattern" -> "pattern"
                "password" -> "password"
                else -> null
            }
        )
    }

    var biometricPromptTrigger by remember { mutableIntStateOf(0) }
    var biometricFailedOrCancelled by remember { mutableStateOf(false) }

    // Automatic trigger for System Biometrics
    if (biometricType == "system" && activeCredential == null) {
        LaunchedEffect(biometricPromptTrigger) {
            val act = activity ?: return@LaunchedEffect
            val executor = ContextCompat.getMainExecutor(act)
            val prompt = BiometricPrompt(
                act,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        biometricFailedOrCancelled = false
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        biometricFailedOrCancelled = true
                    }

                    override fun onAuthenticationFailed() {
                        biometricFailedOrCancelled = true
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            prompt.authenticate(promptInfo)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        // Blurred translucent background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Color.Black.copy(alpha = 0.72f))
        )

        AnimatedContent(
            targetState = activeCredential,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.02f, animationSpec = tween(180)))
            },
            label = "authScreenState",
            modifier = Modifier.fillMaxSize()
        ) { credential ->
            when (credential) {
                "pin" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        PinCredentialView(
                            isVerify = true,
                            expectedPin = expectedPin,
                            title = "Enter PIN",
                            showCloseButton = canDismiss,
                            onConfirm = { onSuccess() },
                            onDismiss = {
                                if (biometricType == "system") {
                                    activeCredential = null
                                } else {
                                    onDismiss()
                                }
                            }
                        )

                        if (biometricType == "system") {
                            TextButton(
                                onClick = {
                                    activeCredential = null
                                    biometricPromptTrigger++
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 20.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Use Biometrics", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                "pattern" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternCredentialView(
                            isVerify = true,
                            expectedPattern = expectedPattern,
                            isPatternVisible = isPatternVisible,
                            title = "Draw Pattern",
                            showCloseButton = canDismiss,
                            onConfirm = { onSuccess() },
                            onDismiss = {
                                if (biometricType == "system") {
                                    activeCredential = null
                                } else {
                                    onDismiss()
                                }
                            }
                        )

                        if (biometricType == "system") {
                            TextButton(
                                onClick = {
                                    activeCredential = null
                                    biometricPromptTrigger++
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 20.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Use Biometrics", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                "password" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        PasswordCredentialView(
                            isVerify = true,
                            expectedPassword = expectedPassword,
                            title = "Enter Password",
                            showCloseButton = canDismiss,
                            onConfirm = { onSuccess() },
                            onDismiss = {
                                if (biometricType == "system") {
                                    activeCredential = null
                                } else {
                                    onDismiss()
                                }
                            }
                        )

                        if (biometricType == "system") {
                            TextButton(
                                onClick = {
                                    activeCredential = null
                                    biometricPromptTrigger++
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 20.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Use Biometrics", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                else -> {
                    // Biometric Waiting / Smart Fallback Screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Pulsing Biometric/Lock Icon
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(if (!biometricFailedOrCancelled) pulseScale else 1f)
                                    .clickable { biometricPromptTrigger++ }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Fingerprint Sensor",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (biometricFailedOrCancelled) {
                                    "Authentication cancelled or not recognized.\nTap the sensor above to retry, or use backup below."
                                } else {
                                    subtitle
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (biometricFailedOrCancelled) {
                                FilledTonalButton(
                                    onClick = { biometricPromptTrigger++ },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry Biometrics")
                                }
                            }
                        }

                        // ── SMART BIOMETRIC FALLBACK: Bottom-Left Button ────────
                        // Always shown when biometric is configured or fails
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Smart Fallback Button at Bottom-Left
                            val fallbackLabel = when (backupType) {
                                "pattern" -> "Use Pattern"
                                "password" -> "Use Password"
                                else -> "Use PIN"
                            }
                            val fallbackIcon = when (backupType) {
                                "pattern" -> Icons.Default.Pattern
                                "password" -> Icons.Default.Key
                                else -> Icons.Default.Pin
                            }

                            TextButton(
                                onClick = { activeCredential = backupType },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = fallbackIcon,
                                    contentDescription = fallbackLabel,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = fallbackLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            // Dismiss / Cancel at Bottom-Right
                            if (canDismiss) {
                                TextButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
