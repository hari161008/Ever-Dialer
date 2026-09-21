package com.coolappstore.evercallrecorder.by.svhp.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Model describing an action revealed by swiping an item left or right.
 */
data class SwipeActionItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val contentColor: Color = Color.White,
    val isDestructive: Boolean = false
)

/**
 * A swipeable item container that translates horizontally as the user drags their finger
 * (moving the foreground container right or left), revealing the action behind it,
 * and executes the action when released past the threshold.
 */
@Composable
fun SwipeableItemContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    leftAction: SwipeActionItem? = null,
    rightAction: SwipeActionItem? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    // Reduced sensitivity: threshold is farther (125.dp) so user must swipe more deliberately
    val threshold = with(density) { 125.dp.toPx() }
    val maxDrag = with(density) { 200.dp.toPx() }

    val swipeModifier = if (enabled && ((leftAction != null && leftAction.key != "none") || (rightAction != null && rightAction.key != "none"))) {
        Modifier.pointerInput(enabled, leftAction, rightAction, threshold, maxDrag) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var totalDx = 0f
                var isDragging = false
                var hapticFired = false
                var canDragRight = rightAction != null && rightAction.key != "none"
                var canDragLeft = leftAction != null && leftAction.key != "none"

                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.pressed) {
                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y
                        totalDx += dx

                        if (!isDragging) {
                            if (abs(totalDx) > touchSlop && abs(totalDx) > abs(change.position.y - down.position.y) * 1.2f) {
                                val allowed = (totalDx > 0 && canDragRight) || (totalDx < 0 && canDragLeft)
                                if (allowed) {
                                    isDragging = true
                                    change.consume()
                                }
                            }
                        } else {
                            change.consume()
                            val current = offsetX.value
                            val target = if (totalDx > 0 && canDragRight) {
                                (current + dx).coerceIn(0f, maxDrag)
                            } else if (totalDx < 0 && canDragLeft) {
                                (current + dx).coerceIn(-maxDrag, 0f)
                            } else {
                                current
                            }

                            coroutineScope.launch { offsetX.snapTo(target) }

                            if (abs(target) >= threshold && !hapticFired) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hapticFired = true
                            } else if (abs(target) < threshold) {
                                hapticFired = false
                            }
                        }
                    } else {
                        break
                    }
                } while (event.changes.any { it.pressed })

                if (isDragging) {
                    val finalX = offsetX.value
                    if (finalX >= threshold && canDragRight) {
                        onSwipeRight?.invoke()
                    } else if (finalX <= -threshold && canDragLeft) {
                        onSwipeLeft?.invoke()
                    }

                    coroutineScope.launch {
                        offsetX.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        val currentX = offsetX.value

        // Background action reveal
        if (currentX > 0 && rightAction != null && rightAction.key != "none") {
            val isTriggered = currentX >= threshold
            val progress = (currentX / threshold).coerceIn(0f, 1f)
            val slowVisibility = (progress * progress).coerceIn(0f, 1f)

            // Slowly visible background tint that smoothly activates to full dynamic color when swiped more far
            val targetBgColor = if (isTriggered) {
                rightAction.backgroundColor
            } else {
                rightAction.backgroundColor.copy(alpha = slowVisibility * 0.4f)
            }
            val animatedBg by animateColorAsState(
                targetValue = targetBgColor,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "rightBgColor"
            )

            val targetContentColor = if (isTriggered) {
                rightAction.contentColor
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = (slowVisibility * 0.85f).coerceIn(0.15f, 0.85f))
            }
            val animatedContentColor by animateColorAsState(
                targetValue = targetContentColor,
                animationSpec = tween(180),
                label = "rightContentColor"
            )

            val targetScale = if (isTriggered) 1.18f else (0.75f + 0.25f * progress)
            val animatedScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "rightScale"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(animatedBg)
                    .padding(start = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = if (isTriggered) 1f else slowVisibility
                    }
                ) {
                    Icon(
                        imageVector = rightAction.icon,
                        contentDescription = rightAction.label,
                        tint = animatedContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = rightAction.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isTriggered) FontWeight.Bold else FontWeight.Medium,
                        color = animatedContentColor
                    )
                }
            }
        } else if (currentX < 0 && leftAction != null && leftAction.key != "none") {
            val isTriggered = abs(currentX) >= threshold
            val progress = (abs(currentX) / threshold).coerceIn(0f, 1f)
            val slowVisibility = (progress * progress).coerceIn(0f, 1f)

            val targetBgColor = if (isTriggered) {
                leftAction.backgroundColor
            } else {
                leftAction.backgroundColor.copy(alpha = slowVisibility * 0.4f)
            }
            val animatedBg by animateColorAsState(
                targetValue = targetBgColor,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "leftBgColor"
            )

            val targetContentColor = if (isTriggered) {
                leftAction.contentColor
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = (slowVisibility * 0.85f).coerceIn(0.15f, 0.85f))
            }
            val animatedContentColor by animateColorAsState(
                targetValue = targetContentColor,
                animationSpec = tween(180),
                label = "leftContentColor"
            )

            val targetScale = if (isTriggered) 1.18f else (0.75f + 0.25f * progress)
            val animatedScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "leftScale"
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(animatedBg)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = if (isTriggered) 1f else slowVisibility
                    }
                ) {
                    Text(
                        text = leftAction.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isTriggered) FontWeight.Bold else FontWeight.Medium,
                        color = animatedContentColor
                    )
                    Icon(
                        imageVector = leftAction.icon,
                        contentDescription = leftAction.label,
                        tint = animatedContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Foreground content container (physically moves with swipe)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .then(swipeModifier)
        ) {
            content()
        }
    }
}
