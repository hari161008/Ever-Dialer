/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.common

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.system.BiometricAvailability
import com.coolappstore.evercallrecorder.by.svhp.system.checkBiometricAvailability
import com.coolappstore.evercallrecorder.by.svhp.system.rememberBiometricPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppLockSetupStep { CHOOSE_METHOD, PIN_ENTER, PIN_CONFIRM, PASSWORD, BIOMETRIC, SUCCESS }

/**
 * Full-screen flow shown when the user turns App Lock on from Settings: pick a method
 * (PIN / password / biometrics), set it up, and confirm it before it takes effect.
 *
 * @param onSetPin       Called with the final PIN once it has been entered twice and matches.
 * @param onSetPassword  Called with the final password once it has been entered twice and matches.
 * @param onSetBiometric Called once the system biometric prompt succeeds.
 * @param onDismiss      Called when the user backs out without finishing setup.
 */
@Composable
fun AppLockSetupDialog(
    onSetPin: (String) -> Unit,
    onSetPassword: (String) -> Unit,
    onSetBiometric: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(AppLockSetupStep.CHOOSE_METHOD) }
    var confirmedMethod by remember { mutableStateOf(AppPreferences.AppLockMethod.NONE) }

    // PIN state
    var firstPin by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinShakeTrigger by remember { mutableIntStateOf(0) }
    var pinLocked by remember { mutableStateOf(false) }

    // Password state
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirmInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordShakeTrigger by remember { mutableIntStateOf(0) }

    // Biometric state
    var biometricError by remember { mutableStateOf<String?>(null) }
    var biometricAttempt by remember { mutableIntStateOf(0) }
    var hasShownBiometricPrompt by rememberSaveable { mutableStateOf(false) }
    val biometricAvailability = remember { context.checkBiometricAvailability() }

    val showBiometricPrompt = rememberBiometricPrompt(
        onSuccess = {
            confirmedMethod = AppPreferences.AppLockMethod.BIOMETRIC
            onSetBiometric()
            step = AppLockSetupStep.SUCCESS
        },
        onError = { message -> biometricError = message }
    )

    fun resetAllInput() {
        firstPin = ""; pinInput = ""; pinError = false; pinLocked = false
        passwordInput = ""; passwordConfirmInput = ""; passwordError = null
        biometricError = null
    }

    fun goBack() {
        when (step) {
            AppLockSetupStep.PIN_ENTER, AppLockSetupStep.PASSWORD, AppLockSetupStep.BIOMETRIC -> {
                resetAllInput(); step = AppLockSetupStep.CHOOSE_METHOD
            }
            AppLockSetupStep.PIN_CONFIRM -> { firstPin = ""; pinInput = ""; pinError = false; step = AppLockSetupStep.PIN_ENTER }
            else -> onDismiss()
        }
    }

    fun handlePinDigit(digit: Char) {
        if (pinLocked) return
        when (step) {
            AppLockSetupStep.PIN_ENTER -> {
                if (pinInput.length < APP_LOCK_PIN_MAX_LENGTH) {
                    pinInput += digit
                    if (pinInput.length == APP_LOCK_PIN_MAX_LENGTH) {
                        firstPin = pinInput; pinInput = ""; step = AppLockSetupStep.PIN_CONFIRM
                    }
                }
            }
            AppLockSetupStep.PIN_CONFIRM -> {
                if (pinInput.length < firstPin.length) {
                    pinInput += digit
                    if (pinInput.length == firstPin.length) {
                        if (pinInput == firstPin) {
                            confirmedMethod = AppPreferences.AppLockMethod.PIN
                            onSetPin(firstPin)
                            step = AppLockSetupStep.SUCCESS
                        } else {
                            pinError = true; pinLocked = true; pinShakeTrigger++
                            scope.launch { delay(550); pinInput = ""; pinError = false; pinLocked = false }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun handlePinBackspace() {
        if (pinLocked) return
        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
    }

    fun handlePinManualConfirm() {
        if (step == AppLockSetupStep.PIN_ENTER && pinInput.length >= APP_LOCK_PIN_MIN_LENGTH) {
            firstPin = pinInput; pinInput = ""; step = AppLockSetupStep.PIN_CONFIRM
        }
    }

    fun handlePasswordSubmit() {
        when {
            passwordInput.length < APP_LOCK_PASSWORD_MIN_LENGTH -> {
                passwordError = "Use at least $APP_LOCK_PASSWORD_MIN_LENGTH characters"
                passwordShakeTrigger++
            }
            passwordInput != passwordConfirmInput -> {
                passwordError = "Passwords don't match"
                passwordShakeTrigger++
                passwordConfirmInput = ""
            }
            else -> {
                confirmedMethod = AppPreferences.AppLockMethod.PASSWORD
                onSetPassword(passwordInput)
                step = AppLockSetupStep.SUCCESS
            }
        }
    }

    BackHandler { goBack() }

    LaunchedEffect(step, biometricAttempt) {
        if (step == AppLockSetupStep.BIOMETRIC) {
            if (biometricAttempt == 0 && hasShownBiometricPrompt) return@LaunchedEffect
            biometricError = null
            hasShownBiometricPrompt = true
            showBiometricPrompt("Confirm it's you", "Use your fingerprint or face to enable App Lock")
        }
    }

    LaunchedEffect(step) {
        if (step == AppLockSetupStep.SUCCESS) {
            delay(950)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = step != AppLockSetupStep.CHOOSE_METHOD && step != AppLockSetupStep.SUCCESS) {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (step != AppLockSetupStep.SUCCESS) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                        }
                    }
                }

                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val slideAmount = if (forward) 60 else -60
                        (slideInHorizontally(tween(320)) { slideAmount } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(220)) { -slideAmount / 3 } + fadeOut(tween(180)))
                    },
                    label = "appLockSetupStep"
                ) { targetStep ->
                    when (targetStep) {
                        AppLockSetupStep.CHOOSE_METHOD -> MethodChooserStep(
                            biometricAvailability = biometricAvailability,
                            onChoosePin = { resetAllInput(); step = AppLockSetupStep.PIN_ENTER },
                            onChoosePassword = { resetAllInput(); step = AppLockSetupStep.PASSWORD },
                            onChooseBiometric = { resetAllInput(); step = AppLockSetupStep.BIOMETRIC }
                        )
                        AppLockSetupStep.PIN_ENTER -> PinEntryStep(
                            title = "Create a PIN",
                            subtitle = "Choose $APP_LOCK_PIN_MIN_LENGTH to $APP_LOCK_PIN_MAX_LENGTH digits.",
                            pinValue = pinInput,
                            isError = false,
                            shakeTrigger = 0,
                            showManualConfirm = pinInput.length >= APP_LOCK_PIN_MIN_LENGTH,
                            onDigit = ::handlePinDigit,
                            onBackspace = ::handlePinBackspace,
                            onManualConfirm = ::handlePinManualConfirm
                        )
                        AppLockSetupStep.PIN_CONFIRM -> PinEntryStep(
                            title = "Confirm your PIN",
                            subtitle = if (pinError) "PINs didn't match - try again." else "Enter it once more to confirm.",
                            pinValue = pinInput,
                            pinSlotCount = firstPin.length,
                            isError = pinError,
                            shakeTrigger = pinShakeTrigger,
                            showManualConfirm = false,
                            onDigit = ::handlePinDigit,
                            onBackspace = ::handlePinBackspace,
                            onManualConfirm = {}
                        )
                        AppLockSetupStep.PASSWORD -> PasswordStep(
                            password = passwordInput,
                            confirmPassword = passwordConfirmInput,
                            onPasswordChange = { passwordInput = it; passwordError = null },
                            onConfirmChange = { passwordConfirmInput = it; passwordError = null },
                            errorText = passwordError,
                            shakeTrigger = passwordShakeTrigger,
                            onSubmit = ::handlePasswordSubmit
                        )
                        AppLockSetupStep.BIOMETRIC -> BiometricStep(
                            errorText = biometricError,
                            onRetry = { biometricAttempt++ },
                            onUseAnotherMethod = { resetAllInput(); step = AppLockSetupStep.CHOOSE_METHOD }
                        )
                        AppLockSetupStep.SUCCESS -> SuccessStep(method = confirmedMethod)
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodChooserStep(
    biometricAvailability: BiometricAvailability,
    onChoosePin: () -> Unit,
    onChoosePassword: () -> Unit,
    onChooseBiometric: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Set Up App Lock", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Choose how you'd like to confirm it's really you before Ever Call Recorder opens.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))
        MethodOptionCard(
            method = AppPreferences.AppLockMethod.PIN,
            title = "PIN",
            description = "A numeric code, $APP_LOCK_PIN_MIN_LENGTH digits or more",
            onClick = onChoosePin
        )
        Spacer(modifier = Modifier.height(12.dp))
        MethodOptionCard(
            method = AppPreferences.AppLockMethod.PASSWORD,
            title = "Password",
            description = "An alphanumeric password you choose",
            onClick = onChoosePassword
        )
        Spacer(modifier = Modifier.height(12.dp))
        MethodOptionCard(
            method = AppPreferences.AppLockMethod.BIOMETRIC,
            title = "Biometrics",
            description = when (biometricAvailability) {
                BiometricAvailability.READY         -> "Use your fingerprint or face unlock"
                BiometricAvailability.NONE_ENROLLED -> "No fingerprint or face set up on this device yet"
                BiometricAvailability.NO_HARDWARE   -> "This device has no biometric sensor"
                BiometricAvailability.UNAVAILABLE   -> "Temporarily unavailable on this device"
            },
            enabled = biometricAvailability == BiometricAvailability.READY,
            onClick = onChooseBiometric
        )
        if (biometricAvailability == BiometricAvailability.NONE_ENROLLED) {
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = {
                val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Settings.ACTION_BIOMETRIC_ENROLL
                             else Settings.ACTION_FINGERPRINT_ENROLL
                context.startActivity(Intent(action))
            }) {
                Text("Set up a fingerprint or face in system settings")
            }
        }
    }
}

@Composable
private fun MethodOptionCard(
    method: AppPreferences.AppLockMethod,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.45f, animationSpec = tween(220), label = "methodCardAlpha")
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                AppLockMethodIcon(
                    method = method,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            }
        }
    }
}

@Composable
private fun PinEntryStep(
    title: String,
    subtitle: String,
    pinValue: String,
    isError: Boolean,
    shakeTrigger: Int,
    showManualConfirm: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onManualConfirm: () -> Unit,
    pinSlotCount: Int? = null
) {
    val shake = rememberShakeAnimatable(shakeTrigger)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val pinInfoContent: @Composable () -> Unit = {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(36.dp))
        PinDotsRow(
            length = pinSlotCount ?: pinValue.length.coerceAtLeast(1),
            filledCount = pinValue.length,
            isError = isError,
            modifier = Modifier.offset(x = shake.value.dp)
        )
    }

    val keypadContent: @Composable () -> Unit = {
        NumericKeypad(onDigit = onDigit, onBackspace = onBackspace, showConfirm = showManualConfirm, onConfirm = onManualConfirm)
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { pinInfoContent() }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { keypadContent() }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            pinInfoContent()
            Spacer(modifier = Modifier.height(40.dp))
            keypadContent()
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FilledKeyButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.Outlined.Check, contentDescription = "Confirm PIN", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun PasswordStep(
    password: String,
    confirmPassword: String,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    errorText: String?,
    shakeTrigger: Int,
    onSubmit: () -> Unit
) {
    val shake = rememberShakeAnimatable(shakeTrigger)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create a Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Use at least $APP_LOCK_PASSWORD_MIN_LENGTH characters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth().offset(x = shake.value.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppLockPasswordField(value = password, onValueChange = onPasswordChange, label = "Password", isError = errorText != null)
                AppLockPasswordField(
                    value = confirmPassword,
                    onValueChange = onConfirmChange,
                    label = "Confirm password",
                    isError = errorText != null,
                    imeAction = ImeAction.Done,
                    onImeAction = onSubmit
                )
            }
            AnimatedVisibility(visible = errorText != null) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSubmit,
            enabled = password.isNotEmpty() && confirmPassword.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Set Password", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BiometricStep(
    errorText: String?,
    onRetry: () -> Unit,
    onUseAnotherMethod: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometricPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "biometricPulseScale"
    )
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val iconContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AppLockMethodIcon(
                method = AppPreferences.AppLockMethod.BIOMETRIC,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp).graphicsLayer(scaleX = pulse, scaleY = pulse)
            )
        }
    }

    val textContent: @Composable () -> Unit = {
        Text("Confirm It's You", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorText ?: "Follow the system prompt to enable biometric unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        AnimatedVisibility(visible = errorText != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onRetry, shape = CircleShape) { Text("Try Again") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onUseAnotherMethod) { Text("Use PIN or Password instead") }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconContent()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { textContent() }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            iconContent()
            Spacer(modifier = Modifier.height(24.dp))
            textContent()
        }
    }
}

@Composable
private fun SuccessStep(method: AppPreferences.AppLockMethod) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "successScale"
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("App Lock Enabled", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Protected with ${appLockMethodLabel(method)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
