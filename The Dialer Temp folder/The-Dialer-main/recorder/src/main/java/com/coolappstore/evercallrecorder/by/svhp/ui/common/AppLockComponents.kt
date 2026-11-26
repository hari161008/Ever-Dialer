/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import kotlin.math.sqrt

/**
 * AppLockComponents.kt holds the small, reusable building blocks shared by the App Lock setup
 * flow ([AppLockSetupDialog]), the re-authentication flow ([AppLockVerifyDialog]) and the actual
 * lock-screen gate ([com.coolappstore.evercallrecorder.by.svhp.ui.screens.AppLockScreen]), so all
 * three feel like one cohesive feature.
 */

/** Smallest PIN length accepted. The user can choose anything from here up to [APP_LOCK_PIN_MAX_LENGTH]. */
const val APP_LOCK_PIN_MIN_LENGTH = 4

/** Longest PIN length accepted, after which digit entry stops automatically. */
const val APP_LOCK_PIN_MAX_LENGTH = 8

/** Shortest password length accepted. */
const val APP_LOCK_PASSWORD_MIN_LENGTH = 4

/**
 * Draws a small vector glyph representing [method] (a dot row for PIN, a key for password, a
 * fingerprint-style swirl for biometrics) entirely with [Canvas] primitives, so it never depends
 * on a specific icon being present in the Material icon font.
 */
@Composable
fun AppLockMethodIcon(
    method: AppPreferences.AppLockMethod,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        when (method) {
            AppPreferences.AppLockMethod.PIN       -> drawPinGlyph(tint)
            AppPreferences.AppLockMethod.PASSWORD  -> drawKeyGlyph(tint)
            AppPreferences.AppLockMethod.BIOMETRIC -> drawFingerprintGlyph(tint)
            AppPreferences.AppLockMethod.NONE      -> drawKeyGlyph(tint)
        }
    }
}

private fun DrawScope.drawPinGlyph(color: Color) {
    val radius = size.minDimension * 0.09f
    val spacing = size.width * 0.26f
    val startX = center.x - spacing * 1.5f
    for (i in 0 until 4) {
        drawCircle(color = color, radius = radius, center = Offset(startX + i * spacing, center.y))
    }
}

private fun DrawScope.drawKeyGlyph(color: Color) {
    val stroke = size.minDimension * 0.10f
    val headRadius = size.minDimension * 0.20f
    val headCenter = Offset(size.width * 0.32f, size.height * 0.32f)
    drawCircle(color = color, radius = headRadius, center = headCenter, style = Stroke(width = stroke))

    val shaftEnd = Offset(size.width * 0.80f, size.height * 0.80f)
    val dirX = shaftEnd.x - headCenter.x
    val dirY = shaftEnd.y - headCenter.y
    val dirLen = sqrt(dirX * dirX + dirY * dirY).takeIf { it > 0f } ?: 1f
    val ux = dirX / dirLen
    val uy = dirY / dirLen
    val shaftStart = Offset(headCenter.x + ux * headRadius * 1.3f, headCenter.y + uy * headRadius * 1.3f)
    drawLine(color = color, start = shaftStart, end = shaftEnd, strokeWidth = stroke * 0.55f, cap = StrokeCap.Round)

    val perpX = -uy
    val perpY = ux
    val toothBase = Offset(shaftEnd.x - ux * size.minDimension * 0.14f, shaftEnd.y - uy * size.minDimension * 0.14f)
    drawLine(
        color = color,
        start = toothBase,
        end = Offset(toothBase.x + perpX * size.minDimension * 0.16f, toothBase.y + perpY * size.minDimension * 0.16f),
        strokeWidth = stroke * 0.55f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawFingerprintGlyph(color: Color) {
    val maxRadius = size.minDimension * 0.42f
    val baseStroke = size.minDimension * 0.085f
    for (i in 0 until 4) {
        val radius = maxRadius * (1f - i * 0.24f)
        drawArc(
            color = color,
            startAngle = 200f - i * 4f,
            sweepAngle = 220f + i * 6f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = baseStroke * (1f - i * 0.12f), cap = StrokeCap.Round)
        )
    }
}

/** Maps an [AppPreferences.AppLockMethod] to its short, human-readable display name. */
fun appLockMethodLabel(method: AppPreferences.AppLockMethod): String = when (method) {
    AppPreferences.AppLockMethod.PIN       -> "PIN"
    AppPreferences.AppLockMethod.PASSWORD  -> "Password"
    AppPreferences.AppLockMethod.BIOMETRIC -> "Biometrics"
    AppPreferences.AppLockMethod.NONE      -> "Off"
}

/**
 * Remembers an [Animatable] that plays a short side-to-side shake whenever [trigger] changes to
 * a new, positive value. Apply its [Animatable.value] (in dp) with `Modifier.offset(x = ...)` on
 * whatever should visibly "shake" (a PIN dots row, a password field) to signal a wrong attempt.
 */
@Composable
fun rememberShakeAnimatable(trigger: Int): Animatable<Float, AnimationVector1D> {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 420
                0f at 0
                -16f at 50 using FastOutSlowInEasing
                16f at 110 using FastOutSlowInEasing
                -12f at 180 using FastOutSlowInEasing
                12f at 250 using FastOutSlowInEasing
                -6f at 320 using FastOutSlowInEasing
                0f at 420
            }
        )
    }
    return shake
}

/** A row of dots showing how many digits of a PIN have been typed so far, with a soft pop-in animation. */
@Composable
fun PinDotsRow(
    length: Int,
    filledCount: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(length) { index -> PinDot(filled = index < filledCount, isError = isError) }
    }
}

@Composable
private fun PinDot(filled: Boolean, isError: Boolean) {
    val targetColor = when {
        isError -> MaterialTheme.colorScheme.error
        filled  -> MaterialTheme.colorScheme.primary
        else    -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val color by animateColorAsState(targetColor, animationSpec = tween(180), label = "pinDotColor")
    val size by animateDpAsState(
        targetValue = if (filled) 18.dp else 11.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "pinDotSize"
    )
    val scale by animateFloatAsState(
        targetValue = if (filled) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pinDotScale"
    )
    Box(
        modifier = Modifier
            .size(20.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (!filled && !isError) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    else Modifier
                )
        )
    }
}

/** A single round key on [NumericKeypad]: either a digit, a backspace icon, or a blank. */
@Composable
private fun KeypadKey(
    modifier: Modifier = Modifier,
    label: String? = null,
    isBackspace: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "keyScale"
    )
    val haptics = LocalHapticFeedback.current
    val glyphColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    // Surface keeps full 64dp touch target; scale applied only to visual content inside
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = interactionSource,
        modifier = modifier.size(64.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center
        ) {
            when {
                isBackspace -> Icon(
                    imageVector = Icons.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = glyphColor,
                    modifier = Modifier.size(26.dp)
                )
                label != null -> Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )
            }
        }
    }
}

/**
 * A 3-column numeric keypad (1-9, then backspace / 0 / confirm) used for PIN entry.
 * Keys fade in with a fast stagger; touch targets are always full-size.
 */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showConfirm: Boolean = false,
    onConfirm: () -> Unit = {}
) {
    val total = 12
    val alphas = remember { List(total) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        alphas.forEachIndexed { i, anim ->
            launch {
                delay(i * 25L)
                anim.animateTo(1f, animationSpec = tween(durationMillis = 120))
            }
        }
    }

    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEachIndexed { colIdx, digit ->
                    val idx = rowIdx * 3 + colIdx
                    Box(modifier = Modifier.graphicsLayer { alpha = alphas[idx].value }) {
                        KeypadKey(label = digit.toString(), enabled = enabled, onClick = { onDigit(digit) })
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.graphicsLayer { alpha = alphas[9].value }) {
                KeypadKey(isBackspace = true, enabled = enabled, onClick = onBackspace)
            }
            Box(modifier = Modifier.graphicsLayer { alpha = alphas[10].value }) {
                KeypadKey(label = "0", enabled = enabled, onClick = { onDigit('0') })
            }
            Box(modifier = Modifier.graphicsLayer { alpha = alphas[11].value }) {
                if (showConfirm) {
                    Surface(
                        onClick = onConfirm,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Outlined.Check, contentDescription = "Confirm", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }
        }
    }
}

/**
 * A password text field with a show/hide toggle, styled like the rest of the settings screens.
 */
@Composable
fun AppLockPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        }
    )
}
