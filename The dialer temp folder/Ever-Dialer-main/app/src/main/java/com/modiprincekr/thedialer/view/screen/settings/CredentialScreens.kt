package com.modiprincekr.thedialer.view.screen.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

// ─── Utility Shake Animation ──────────────────────────────────────────────────

@Composable
fun rememberCredentialShake(): Pair<Float, () -> Unit> {
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val shakeAnim = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-14f) at 50 using FastOutSlowInEasing
                    14f at 110 using FastOutSlowInEasing
                    (-10f) at 170 using FastOutSlowInEasing
                    10f at 230 using FastOutSlowInEasing
                    (-5f) at 290 using FastOutSlowInEasing
                    0f at 400
                }
            )
        }
    }

    return Pair(shakeAnim.value) { shakeTrigger++ }
}

fun triggerHapticError(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, 180))
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(VibrationEffect.createOneShot(80, 180))
        }
    } catch (_: Exception) {}
}

fun triggerHapticSuccess(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(40, 120))
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                ?.vibrate(VibrationEffect.createOneShot(40, 120))
        }
    } catch (_: Exception) {}
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. PATTERN LOCK CANVAS & SCREEN
// ─────────────────────────────────────────────────────────────────────────────

private fun getIntermediateDot(dot1: Int, dot2: Int): Int? {
    val r1 = dot1 / 3
    val c1 = dot1 % 3
    val r2 = dot2 / 3
    val c2 = dot2 % 3
    if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
        val midR = (r1 + r2) / 2
        val midC = (c1 + c2) / 2
        return midR * 3 + midC
    }
    return null
}

@Composable
fun PatternLockCanvas(
    modifier: Modifier = Modifier,
    isPatternVisible: Boolean = true,
    isError: Boolean = false,
    enabled: Boolean = true,
    onPatternComplete: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedDots by remember { mutableStateOf(listOf<Int>()) }
    var currentDragOffset by remember { mutableStateOf<Offset?>(null) }
    var dotCenters by remember { mutableStateOf(List(9) { Offset.Zero }) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val dotInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val ringColor = if (isError) errorColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.25f)
    val activePathColor = if (isError) errorColor else primaryColor

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(enabled, isError) {
                if (!enabled || isError) return@pointerInput
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val hitIndex = dotCenters.indexOfFirst { center ->
                            hypot(center.x - startOffset.x, center.y - startOffset.y) < 48.dp.toPx()
                        }
                        if (hitIndex != -1) {
                            selectedDots = listOf(hitIndex)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            selectedDots = emptyList()
                        }
                        currentDragOffset = startOffset
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        currentDragOffset = pos

                        val hitIndex = dotCenters.indexOfFirst { center ->
                            hypot(center.x - pos.x, center.y - pos.y) < 40.dp.toPx()
                        }
                        if (hitIndex != -1 && !selectedDots.contains(hitIndex)) {
                            val newDots = selectedDots.toMutableList()
                            if (newDots.isNotEmpty()) {
                                val last = newDots.last()
                                val intermediate = getIntermediateDot(last, hitIndex)
                                if (intermediate != null && !newDots.contains(intermediate)) {
                                    newDots.add(intermediate)
                                }
                            }
                            newDots.add(hitIndex)
                            selectedDots = newDots
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDragEnd = {
                        currentDragOffset = null
                        if (selectedDots.isNotEmpty()) {
                            val patternString = selectedDots.joinToString("")
                            onPatternComplete(patternString)
                        }
                    },
                    onDragCancel = {
                        currentDragOffset = null
                        selectedDots = emptyList()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val padding = minDim * 0.16f
            val spacing = (minDim - 2 * padding) / 2f

            // Calculate dot centers
            val calculated = (0 until 9).map { idx ->
                val row = idx / 3
                val col = idx % 3
                Offset(padding + col * spacing, padding + row * spacing)
            }
            if (dotCenters != calculated) {
                dotCenters = calculated
            }

            // Draw connecting lines if visible
            if (isPatternVisible && selectedDots.size > 1) {
                val path = Path()
                path.moveTo(dotCenters[selectedDots[0]].x, dotCenters[selectedDots[0]].y)
                for (i in 1 until selectedDots.size) {
                    val p = dotCenters[selectedDots[i]]
                    path.lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = activePathColor,
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw line to current finger drag position if visible
            if (isPatternVisible && selectedDots.isNotEmpty() && currentDragOffset != null) {
                val lastCenter = dotCenters[selectedDots.last()]
                drawLine(
                    color = activePathColor.copy(alpha = 0.7f),
                    start = lastCenter,
                    end = currentDragOffset!!,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw dots
            dotCenters.forEachIndexed { index, center ->
                val isSelected = selectedDots.contains(index)
                if (isSelected) {
                    if (isPatternVisible) {
                        // Outer halo/ring
                        drawCircle(
                            color = ringColor,
                            radius = 24.dp.toPx(),
                            center = center
                        )
                        // Inner circle
                        drawCircle(
                            color = activePathColor,
                            radius = 9.dp.toPx(),
                            center = center
                        )
                    } else {
                        // When hidden: draw active dot softly without line trails
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.7f),
                            radius = 8.dp.toPx(),
                            center = center
                        )
                    }
                } else {
                    // Resting unselected dot
                    drawCircle(
                        color = dotInactiveColor,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}

@Composable
fun PatternCredentialView(
    modifier: Modifier = Modifier,
    isVerify: Boolean = false,
    expectedPattern: String = "",
    isPatternVisible: Boolean = true,
    title: String = if (isVerify) "Draw Pattern" else "Set Pattern",
    showCloseButton: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableIntStateOf(if (isVerify) 2 else 0) }
    var firstPattern by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var statusText by remember {
        mutableStateOf(
            if (isVerify) "Draw your pattern to unlock"
            else "Connect at least 4 dots"
        )
    }

    val (shakeOffset, triggerShake) = rememberCredentialShake()

    fun handlePattern(pattern: String) {
        if (pattern.length < 4) {
            isError = true
            statusText = "Connect at least 4 dots"
            triggerShake()
            triggerHapticError(context)
            scope.launch {
                delay(600)
                isError = false
                statusText = if (phase == 0) "Connect at least 4 dots" else "Draw pattern again to confirm"
            }
            return
        }

        when {
            isVerify -> {
                if (pattern == expectedPattern) {
                    triggerHapticSuccess(context)
                    onConfirm(pattern)
                } else {
                    isError = true
                    statusText = "Incorrect pattern. Try again."
                    triggerShake()
                    triggerHapticError(context)
                    scope.launch {
                        delay(650)
                        isError = false
                        statusText = "Draw your pattern to unlock"
                    }
                }
            }
            phase == 0 -> {
                firstPattern = pattern
                phase = 1
                statusText = "Draw pattern again to confirm"
                triggerHapticSuccess(context)
            }
            phase == 1 -> {
                if (pattern == firstPattern) {
                    triggerHapticSuccess(context)
                    onConfirm(pattern)
                } else {
                    isError = true
                    statusText = "Patterns did not match. Try again."
                    triggerShake()
                    triggerHapticError(context)
                    scope.launch {
                        delay(700)
                        isError = false
                        phase = 0
                        firstPattern = ""
                        statusText = "Connect at least 4 dots"
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showCloseButton) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Pattern,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = when {
                    isVerify -> title
                    phase == 0 -> "Draw an Unlock Pattern"
                    else -> "Confirm Pattern"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // 3x3 Canvas Grid
            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .offset(x = shakeOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                PatternLockCanvas(
                    modifier = Modifier.fillMaxSize(),
                    isPatternVisible = isPatternVisible,
                    isError = isError,
                    enabled = !isError,
                    onPatternComplete = ::handlePattern
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            if (!isVerify && phase == 1) {
                TextButton(
                    onClick = {
                        phase = 0
                        firstPattern = ""
                        statusText = "Connect at least 4 dots"
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset Pattern", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. PIN NUMPAD & SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PinDotsRow(
    pinLength: Int,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalDots = pinLength.coerceAtLeast(4).coerceAtMost(8)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val filled = index < pinLength
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0.42f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dotScale$index"
            )
            val color by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = tween(140),
                label = "dotColor$index"
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun PinNumpad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("⌫" to "", "0" to "+", "✓" to "")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { (key, subText) ->
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.86f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "keyScale$key"
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "⌫" -> Surface(
                                onClick = onBackspace,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            "✓" -> Surface(
                                onClick = onSubmit,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(scale),
                                interactionSource = interaction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Confirm",
                                        modifier = Modifier.size(26.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            else -> Surface(
                                onClick = { onDigit(key) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(scale),
                                interactionSource = interaction
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    if (subText.isNotEmpty()) {
                                        Text(
                                            text = subText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
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
}

@Composable
fun PinCredentialView(
    modifier: Modifier = Modifier,
    isVerify: Boolean = false,
    expectedPin: String = "",
    title: String = if (isVerify) "Enter PIN" else "Set PIN",
    showCloseButton: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phase by remember { mutableIntStateOf(if (isVerify) 2 else 0) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var subtitleText by remember {
        mutableStateOf(
            if (isVerify) "Enter your PIN to verify"
            else "At least 4 digits"
        )
    }

    val (shakeOffset, triggerShake) = rememberCredentialShake()

    fun onDigit(d: String) {
        if (pin.length < 12) {
            pin += d
            isError = false
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            isError = false
        }
    }

    fun onSubmit() {
        when {
            pin.length < 4 -> {
                isError = true
                subtitleText = "PIN must be at least 4 digits"
                triggerShake()
                triggerHapticError(context)
            }
            isVerify -> {
                if (pin == expectedPin) {
                    triggerHapticSuccess(context)
                    onConfirm(pin)
                } else {
                    pin = ""
                    isError = true
                    subtitleText = "Incorrect PIN. Try again."
                    triggerShake()
                    triggerHapticError(context)
                }
            }
            phase == 0 -> {
                firstPin = pin
                pin = ""
                phase = 1
                subtitleText = "Re-enter your PIN to confirm"
                triggerHapticSuccess(context)
            }
            phase == 1 -> {
                if (pin == firstPin) {
                    triggerHapticSuccess(context)
                    onConfirm(pin)
                } else {
                    pin = ""
                    firstPin = ""
                    phase = 0
                    isError = true
                    subtitleText = "PINs did not match. Try again."
                    triggerShake()
                    triggerHapticError(context)
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showCloseButton) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Pin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = when {
                    isVerify -> title
                    phase == 0 -> "Set PIN"
                    else -> "Confirm PIN"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // PIN Dots Indicator
            Box(
                modifier = Modifier.offset(x = shakeOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                PinDotsRow(pinLength = pin.length, isError = isError)
            }

            Spacer(Modifier.height(24.dp))

            // Numpad
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PinNumpad(
                    onDigit = ::onDigit,
                    onBackspace = ::onBackspace,
                    onSubmit = ::onSubmit
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. PASSWORD ENTRY & SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PasswordCredentialView(
    modifier: Modifier = Modifier,
    isVerify: Boolean = false,
    expectedPassword: String = "",
    title: String = if (isVerify) "Enter Password" else "Set Password",
    showCloseButton: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val (shakeOffset, triggerShake) = rememberCredentialShake()

    fun validateAndSubmit() {
        when {
            isVerify -> {
                if (password == expectedPassword) {
                    triggerHapticSuccess(context)
                    onConfirm(password)
                } else {
                    errorText = "Incorrect password"
                    triggerShake()
                    triggerHapticError(context)
                }
            }
            password.length < 4 -> {
                errorText = "Password must be at least 4 characters"
                triggerShake()
                triggerHapticError(context)
            }
            password != confirmPassword -> {
                errorText = "Passwords do not match"
                triggerShake()
                triggerHapticError(context)
            }
            else -> {
                triggerHapticSuccess(context)
                onConfirm(password)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showCloseButton) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (isVerify) title else "Set Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isVerify) "Enter your alphanumeric password" else "Letters, numbers & symbols",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shakeOffset.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = "" },
                    label = { Text(if (isVerify) "Password" else "New Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isVerify) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onDone = { validateAndSubmit() }),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp),
                    isError = errorText.isNotEmpty()
                )

                if (!isVerify) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorText = "" },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { validateAndSubmit() }),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = errorText.isNotEmpty()
                    )
                }

                AnimatedVisibility(visible = errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCloseButton) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                    Button(
                        onClick = ::validateAndSubmit,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isVerify) "Unlock" else "Confirm", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FULL-SCREEN MATERIAL 3 CREDENTIAL SETUP DIALOG WITH BLURRED BACKGROUND
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CredentialSetupDialog(
    type: String, // "pin" | "password" | "pattern"
    isPatternVisible: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            // Blurred background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                when (type) {
                    "pattern" -> PatternCredentialView(
                        isVerify = false,
                        isPatternVisible = isPatternVisible,
                        showCloseButton = true,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss
                    )
                    "pin" -> PinCredentialView(
                        isVerify = false,
                        showCloseButton = true,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss
                    )
                    "password" -> PasswordCredentialView(
                        isVerify = false,
                        showCloseButton = true,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

// Deprecated alias for backwards compatibility
@Composable
fun CredentialSetupBottomSheet(
    type: String,
    isPatternVisible: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CredentialSetupDialog(
        type = type,
        isPatternVisible = isPatternVisible,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
