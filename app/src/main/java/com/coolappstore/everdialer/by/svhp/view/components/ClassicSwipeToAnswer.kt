package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * LineageOS / Google Classic style swipe up to answer or swipe down to decline gesture.
 */
@Composable
fun ClassicSwipeToAnswer(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit = {},
    onMute: () -> Unit = {},
    showMuteButton: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f),
    bgColor: Color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f),
    isPocketBlocked: () -> Boolean = { false },
    isDark: Boolean = isSystemInDarkTheme(),
    handleColor: Color? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val resolvedHandleColor = handleColor ?: (if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface)

    val maxDragY = with(density) { 130.dp.toPx() }

    // Flash-fill progress for confirmed answer / decline
    val answerFlash = remember { Animatable(0f) }
    val declineFlash = remember { Animatable(0f) }

    // Idle bounce animation for the "Swipe up" hint
    val infiniteTransition = rememberInfiniteTransition(label = "classicHintBounce")
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceY"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Centered Quick Action Pills (Message, and optional Mute ringer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Message quick-reply pill
            Surface(
                onClick = onMessage,
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.height(45.dp).width(if (showMuteButton) 130.dp else 140.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ChatBubble, null, tint = labelColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Message", color = labelColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            if (showMuteButton) {
                Spacer(Modifier.width(16.dp))
                // Mute ringer pill
                Surface(
                    onClick = onMute,
                    shape = CircleShape,
                    color = bgColor,
                    modifier = Modifier.height(45.dp).width(130.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.VolumeOff, null, tint = labelColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mute", color = labelColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Vertical Swipe Gesture Area
        val dragFraction = if (maxDragY > 0f) (offsetY.value / maxDragY).coerceIn(-1f, 1f) else 0f

        // Icon rotation:
        // Swiping up (dragFraction < 0): rotates slightly upright (-15°)
        // Swiping down (dragFraction > 0): rotates up to 135° (hang-up orientation)
        val iconRotation = if (dragFraction <= 0f) {
            dragFraction * 15f
        } else {
            dragFraction * 135f
        }

        // Icon tint:
        // Default / drag up: Green
        // Drag down: blends to Red
        val iconTint = if (dragFraction > 0.2f) {
            lerp(Color(0xFF4CAF50), Color(0xFFF44336), ((dragFraction - 0.2f) / 0.8f).coerceIn(0f, 1f))
        } else {
            Color(0xFF4CAF50)
        }

        val handleAlpha = (1f - maxOf(answerFlash.value, declineFlash.value) * 2f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Flash fills on answer / decline
            if (answerFlash.value > 0f) {
                Box(
                    modifier = Modifier
                        .size((72 + answerFlash.value * 200).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF43A047).copy(alpha = (1f - answerFlash.value * 0.5f).coerceIn(0f, 1f)))
                )
            }
            if (declineFlash.value > 0f) {
                Box(
                    modifier = Modifier
                        .size((72 + declineFlash.value * 200).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F).copy(alpha = (1f - declineFlash.value * 0.5f).coerceIn(0f, 1f)))
                )
            }

            // Top hint: Swipe up to answer
            val upHintAlpha = ((1f - dragFraction.coerceAtLeast(0f) * 2f) * (1f - abs(dragFraction) * 0.6f)).coerceIn(0.2f, 1f)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .graphicsLayer { translationY = bounceY }
                    .alpha(upHintAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Swipe up to answer",
                    color = labelColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                )
            }

            // Bottom hint: Swipe down to decline
            val downHintAlpha = ((1f - (-dragFraction).coerceAtLeast(0f) * 2f) * (1f - abs(dragFraction) * 0.6f)).coerceIn(0.2f, 1f)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .alpha(downHintAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Swipe down to decline",
                    color = labelColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Center Puck (Draggable button)
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .size(74.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(resolvedHandleColor)
                    .alpha(handleAlpha)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        offsetY.value <= -maxDragY * 0.78f -> {
                                            // Swiped UP -> Answer
                                            launch { answerFlash.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                            kotlinx.coroutines.delay(180)
                                            onAnswer()
                                        }
                                        offsetY.value >= maxDragY * 0.78f -> {
                                            // Swiped DOWN -> Decline
                                            launch { declineFlash.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                            kotlinx.coroutines.delay(180)
                                            onDecline()
                                        }
                                        else -> offsetY.animateTo(
                                            0f,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (!isPocketBlocked()) {
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetY.snapTo((offsetY.value + dragAmount).coerceIn(-maxDragY, maxDragY))
                                    }
                                } else {
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }
        }
    }
}
