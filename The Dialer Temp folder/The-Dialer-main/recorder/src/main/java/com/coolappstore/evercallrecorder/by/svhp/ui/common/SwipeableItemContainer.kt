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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Reduced sensitivity: threshold is farther (125.dp) so user must swipe deliberately
    val threshold = with(density) { 125.dp.toPx() }
    val maxDrag = with(density) { 200.dp.toPx() }

    val swipeModifier = if (enabled && ((leftAction != null && leftAction.key != "none") || (rightAction != null && rightAction.key != "none"))) {
        Modifier.pointerInput(enabled, leftAction, rightAction, threshold, maxDrag, isRtl) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var isDragging = false
                var hapticFired = false
                val canDragRight = rightAction != null && rightAction.key != "none"
                val canDragLeft = leftAction != null && leftAction.key != "none"

                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.isConsumed && !isDragging) {
                        // Vertical scroll or parent component claimed the gesture
                        break
                    }

                    if (change.pressed) {
                        val totalDx = change.position.x - down.position.x
                        val totalDy = change.position.y - down.position.y
                        val dx = change.position.x - change.previousPosition.x

                        if (!isDragging) {
                            // If user is scrolling vertically up/down, cancel swipe gesture immediately
                            if (abs(totalDy) > touchSlop && abs(totalDy) >= abs(totalDx)) {
                                break
                            }

                            // Require deliberate horizontal movement:
                            // 1. Horizontal movement exceeds touchSlop * 1.25f
                            // 2. clear horizontal orientation (horizontal movement at least twice vertical)
                            // 3. User is moving in a direction with an active action configured
                            if (abs(totalDx) > touchSlop * 1.25f && abs(totalDx) > abs(totalDy) * 2f) {
                                val allowed = if (!isRtl) {
                                    (totalDx > 0 && canDragRight) || (totalDx < 0 && canDragLeft)
                                } else {
                                    (totalDx > 0 && canDragLeft) || (totalDx < 0 && canDragRight)
                                }
                                if (allowed) {
                                    isDragging = true
                                    change.consume()
                                }
                            }
                        } else {
                            change.consume()
                            val current = offsetX.value
                            val effectiveDx = if (isRtl) -dx else dx
                            val target = (current + effectiveDx).coerceIn(
                                if (canDragLeft) -maxDrag else 0f,
                                if (canDragRight) maxDrag else 0f
                            )

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

        // currentX > 0: user dragged rightwards -> reveals left side of container -> rightAction
        // currentX < 0: user dragged leftwards  -> reveals right side of container -> leftAction
        val isRevealingRightAction = currentX > 0
        val isRevealingLeftAction = currentX < 0

        // Background action reveal
        if (isRevealingRightAction && rightAction != null && rightAction.key != "none") {
            val currentDisplacement = abs(currentX)
            val isTriggered = currentDisplacement >= threshold
            val progress = (currentDisplacement / threshold).coerceIn(0f, 1f)
            val slowVisibility = (progress * progress).coerceIn(0f, 1f)

            // Slowly visible background tint that smoothly activates to full dynamic color when swiped farther
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

            // Revealed on the physical left of the container (Absolute Alignment)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(animatedBg)
                    .padding(start = if (!isRtl) 24.dp else 0.dp, end = if (isRtl) 24.dp else 0.dp),
                contentAlignment = if (!isRtl) Alignment.CenterStart else Alignment.CenterEnd
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
        } else if (isRevealingLeftAction && leftAction != null && leftAction.key != "none") {
            val currentDisplacement = abs(currentX)
            val isTriggered = currentDisplacement >= threshold
            val progress = (currentDisplacement / threshold).coerceIn(0f, 1f)
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

            // Revealed on the physical right of the container (Absolute Alignment)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(animatedBg)
                    .padding(end = if (!isRtl) 24.dp else 0.dp, start = if (isRtl) 24.dp else 0.dp),
                contentAlignment = if (!isRtl) Alignment.CenterEnd else Alignment.CenterStart
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
                .offset { IntOffset(if (isRtl) -offsetX.value.roundToInt() else offsetX.value.roundToInt(), 0) }
                .then(swipeModifier)
        ) {
            content()
        }
    }
}
