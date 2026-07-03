/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.ui.common.APP_LOCK_PIN_MAX_LENGTH
import com.coolappstore.evercallrecorder.by.svhp.ui.common.APP_LOCK_PIN_MIN_LENGTH
import com.coolappstore.evercallrecorder.by.svhp.ui.common.AppLockMethodIcon
import com.coolappstore.evercallrecorder.by.svhp.ui.common.AppLockPasswordField
import com.coolappstore.evercallrecorder.by.svhp.ui.common.NumericKeypad
import com.coolappstore.evercallrecorder.by.svhp.ui.common.PinDotsRow
import com.coolappstore.evercallrecorder.by.svhp.ui.common.rememberShakeAnimatable
import com.coolappstore.evercallrecorder.by.svhp.system.rememberBiometricPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen gate shown instead of the rest of the app whenever App Lock is enabled and the
 * current session hasn't been authenticated yet (see [com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.AppLockViewModel]).
 *
 * @param method         The configured unlock method - decides which input UI is shown.
 * @param onVerifySecret For [AppPreferences.AppLockMethod.PIN]/[AppPreferences.AppLockMethod.PASSWORD],
 *                        called with the typed-in secret; should return whether it was correct.
 * @param onUnlocked     Called once the user has successfully authenticated.
 */
@Composable
fun AppLockScreen(
    method: AppPreferences.AppLockMethod,
    onVerifySecret: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(true) }

    LaunchedEffect(unlocked) {
        if (unlocked) {
            visibleState.targetState = false
        }
    }

    LaunchedEffect(visibleState.currentState) {
        if (!visibleState.currentState && !visibleState.targetState) {
            onUnlocked()
        }
    }

    val handleUnlocked: () -> Unit = { unlocked = true }

    androidx.compose.animation.AnimatedVisibility(
        visibleState = visibleState,
        exit = fadeOut(animationSpec = tween(350)) + scaleOut(
            targetScale = 1.08f,
            animationSpec = tween(350)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Text("Ever Call Recorder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                when (method) {
                    AppPreferences.AppLockMethod.PIN       -> PinUnlockContent(onVerifySecret, handleUnlocked)
                    AppPreferences.AppLockMethod.PASSWORD  -> PasswordUnlockContent(onVerifySecret, handleUnlocked)
                    AppPreferences.AppLockMethod.BIOMETRIC -> BiometricUnlockContent(handleUnlocked)
                    AppPreferences.AppLockMethod.NONE      -> {}
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.PinUnlockContent(onVerifySecret: (String) -> Boolean, onUnlocked: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shake = rememberShakeAnimatable(shakeTrigger)

    fun attemptVerify() {
        if (onVerifySecret(pinInput)) {
            onUnlocked()
        } else {
            isError = true; locked = true; shakeTrigger++
            scope.launch { delay(550); pinInput = ""; isError = false; locked = false }
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val keypad: @Composable () -> Unit = {
        NumericKeypad(
            enabled = !locked,
            onDigit = { digit ->
                if (!locked && pinInput.length < APP_LOCK_PIN_MAX_LENGTH) {
                    pinInput += digit
                    if (pinInput.length == APP_LOCK_PIN_MAX_LENGTH) attemptVerify()
                }
            },
            onBackspace = { if (!locked && pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
            showConfirm = pinInput.length in APP_LOCK_PIN_MIN_LENGTH until APP_LOCK_PIN_MAX_LENGTH && !locked,
            onConfirm = { attemptVerify() }
        )
    }

    val pinInfo: @Composable () -> Unit = {
        Text("Enter your PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isError) "Incorrect PIN, try again." else "Enter your PIN to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        PinDotsRow(
            length = pinInput.length.coerceAtLeast(1),
            filledCount = pinInput.length,
            isError = isError,
            modifier = Modifier.offset(x = shake.value.dp)
        )
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { pinInfo() }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { keypad() }
        }
    } else {
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) { pinInfo() }
        Spacer(modifier = Modifier.height(36.dp))
        keypad()
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ColumnScope.PasswordUnlockContent(onVerifySecret: (String) -> Boolean, onUnlocked: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val shake = rememberShakeAnimatable(shakeTrigger)
    val scrollState = rememberScrollState()

    fun attemptVerify() {
        if (onVerifySecret(password)) onUnlocked()
        else { isError = true; shakeTrigger++ }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Enter your Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isError) "Incorrect password, try again." else "Enter your password to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            AppLockPasswordField(
                value = password,
                onValueChange = { password = it; isError = false },
                label = "Password",
                isError = isError,
                imeAction = ImeAction.Done,
                onImeAction = { attemptVerify() },
                modifier = Modifier.offset(x = shake.value.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { attemptVerify() },
            enabled = password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Unlock", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun ColumnScope.BiometricUnlockContent(onUnlocked: () -> Unit) {
    var errorText by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    // Survives rotation: once the prompt has been shown once, don't auto-show it again on
    // recomposition (e.g. after a device rotation). The user can still retry via the button.
    var hasShownInitialPrompt by rememberSaveable { mutableStateOf(false) }

    val showPrompt = rememberBiometricPrompt(
        onSuccess = onUnlocked,
        onError = { message -> errorText = message }
    )

    LaunchedEffect(attempt) {
        if (attempt == 0 && hasShownInitialPrompt) return@LaunchedEffect
        errorText = null
        hasShownInitialPrompt = true
        showPrompt("Unlock Ever Call Recorder", null)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lockScreenBiometricPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "lockScreenBiometricPulseScale"
    )

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val biometricIcon: @Composable () -> Unit = {
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

    val biometricText: @Composable () -> Unit = {
        Text("Unlock with Biometrics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorText ?: "Use your fingerprint or face to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { attempt++ }, shape = CircleShape) {
            Text("Unlock with Biometrics")
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            biometricIcon()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { biometricText() }
        }
    } else {
        Spacer(modifier = Modifier.weight(1f))
        biometricIcon()
        Spacer(modifier = Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) { biometricText() }
        Spacer(modifier = Modifier.weight(1f))
    }
}
