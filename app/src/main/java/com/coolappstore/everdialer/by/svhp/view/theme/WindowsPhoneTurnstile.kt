package com.coolappstore.everdialer.by.svhp.view.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Authentic Metro / Windows Phone cubic easing curve:
 * Rapid initial acceleration that authoritatively decelerates with zero overshoot.
 */
val MetroTurnstileEasing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f)

/**
 * Tracks navigation direction (forward push vs pop back) and navigation generation
 * so that element animations know whether to flip in from the right (forward)
 * or reverse in from the opposite direction (back).
 */
object TurnstileNavigationTracker {
    @Volatile
    var lastNavigationIsBack: Boolean = false
        private set

    @Volatile
    var navigationGeneration: Long = 0L
        private set

    private val routeBackMap = mutableMapOf<String, Boolean>()

    fun recordEnter(isBack: Boolean, route: String?) {
        lastNavigationIsBack = isBack
        navigationGeneration++
        if (route != null) {
            val base = route.substringBefore("?").substringBefore("/")
            routeBackMap[base] = isBack
        }
    }

    fun isBackForRoute(route: String?): Boolean {
        if (route == null) return lastNavigationIsBack
        val base = route.substringBefore("?").substringBefore("/")
        return routeBackMap[base] ?: lastNavigationIsBack
    }
}

/**
 * Categorizes UI elements to assign realistic cascading delays and 3D rotation angles
 * modeled on the Windows Phone 7/8 Turnstile transitions.
 */
sealed class TurnstileRole {
    data object Header : TurnstileRole()
    data object Control : TurnstileRole()
    data class Card(val order: Int = 0) : TurnstileRole()
    data class ListItem(val index: Int = 0) : TurnstileRole()
}

/**
 * Reusable modifier providing an authentic Windows Phone 3D Turnstile transition to individual
 * UI elements (headers, cards, controls, list items).
 *
 * Automatically no-ops when the user has selected any other animation style (e.g. Zoom).
 *
 * @param role The role of this element (Header, Control, Card, ListItem) which dictates its delay and angles.
 * @param customDelayMs Optional delay override in milliseconds.
 * @param durationMs Animation duration in milliseconds.
 */
@Composable
fun Modifier.wpTurnstile(
    role: TurnstileRole = TurnstileRole.Card(),
    customDelayMs: Int? = null,
    durationMs: Int = SETTINGS_WP_ANIM_DURATION_ENTER
): Modifier {
    if (!isWindowsPhoneAnimation()) return this

    val isBack = TurnstileNavigationTracker.lastNavigationIsBack
    val generation = TurnstileNavigationTracker.navigationGeneration

    val delayMs = customDelayMs ?: when (role) {
        is TurnstileRole.Header -> 0
        is TurnstileRole.Control -> 60
        is TurnstileRole.Card -> 110 + (role.order * 45).coerceAtMost(260)
        is TurnstileRole.ListItem -> (80 + role.index * 45).coerceAtMost(360)
    }

    val initialRotationY = if (!isBack) {
        when (role) {
            is TurnstileRole.Header -> -22f
            is TurnstileRole.Control -> -26f
            is TurnstileRole.Card -> -30f
            is TurnstileRole.ListItem -> -32f
        }
    } else {
        when (role) {
            is TurnstileRole.Header -> 16f
            is TurnstileRole.Control -> 18f
            is TurnstileRole.Card -> 20f
            is TurnstileRole.ListItem -> 22f
        }
    }

    val initialTranslationXDp = if (!isBack) {
        when (role) {
            is TurnstileRole.Header -> 40f
            is TurnstileRole.Control -> 60f
            is TurnstileRole.Card -> 75f
            is TurnstileRole.ListItem -> 80f
        }
    } else {
        when (role) {
            is TurnstileRole.Header -> -35f
            is TurnstileRole.Control -> -50f
            is TurnstileRole.Card -> -60f
            is TurnstileRole.ListItem -> -65f
        }
    }

    val progress = remember(generation) { Animatable(0f) }

    LaunchedEffect(generation) {
        if (delayMs > 0) {
            delay(delayMs.toLong())
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = MetroTurnstileEasing
            )
        )
    }

    val p = progress.value
    if (p >= 1f) {
        // Once settled, avoid setting any rotation or translation so scroll performance is 100% native
        return this
    }

    return this.graphicsLayer {
        val currentProgress = progress.value
        val remaining = 1f - currentProgress
        rotationY = remaining * initialRotationY
        translationX = remaining * initialTranslationXDp.dp.toPx()
        alpha = currentProgress.coerceIn(0f, 1f)
        transformOrigin = TransformOrigin(0f, 0.5f) // Hinged on the left edge
        cameraDistance = 16f * density
    }
}

/** Convenience modifier for headers and top app bars. */
@Composable
fun Modifier.wpTurnstileHeader(delayMs: Int? = null): Modifier =
    this.wpTurnstile(role = TurnstileRole.Header, customDelayMs = delayMs)

/** Convenience modifier for tabs, search pills, buttons, and control chips. */
@Composable
fun Modifier.wpTurnstileControl(delayMs: Int? = null): Modifier =
    this.wpTurnstile(role = TurnstileRole.Control, customDelayMs = delayMs)

/** Convenience modifier for section cards and containers. */
@Composable
fun Modifier.wpTurnstileCard(order: Int = 0, delayMs: Int? = null): Modifier =
    this.wpTurnstile(role = TurnstileRole.Card(order), customDelayMs = delayMs)

/** Convenience modifier for staggered list items in LazyColumns. */
@Composable
fun Modifier.wpTurnstileItem(index: Int, delayMs: Int? = null): Modifier =
    this.wpTurnstile(role = TurnstileRole.ListItem(index), customDelayMs = delayMs)
