package com.coolappstore.everdialer.by.svhp.view.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

private val TAB_ROUTES = listOf(
    "favorites_screen",
    "recent_screen",
    "contact_screen",
    "notes_screen"
)

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

internal var isLandscapeMode: Boolean = false

object TabTransitionStyle : NavHostAnimatedDestinationStyle() {

    private fun routeOrder(route: String?): Int {
        if (route == null) return -1
        val base = route.substringBefore("?").substringBefore("/")
        return TAB_ROUTES.indexOfFirst { base.contains(it, ignoreCase = true) }
    }

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        if (fromIdx >= 0 && toIdx >= 0 && !isLandscapeMode) {
            val goRight = toIdx > fromIdx
            slideInHorizontally(
                animationSpec = tween(550, easing = EaseOutQuart),
                initialOffsetX = { if (goRight) (it * 0.25f).toInt() else -(it * 0.25f).toInt() }
            ) + fadeIn(tween(400, easing = EaseOutQuart))
        } else {
            fadeIn(tween(400, easing = EaseOutQuart))
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        if (fromIdx >= 0 && toIdx >= 0 && !isLandscapeMode) {
            val goRight = toIdx > fromIdx
            slideOutHorizontally(
                animationSpec = tween(550, easing = EaseOutQuart),
                targetOffsetX = { if (goRight) -(it * 0.25f).toInt() else (it * 0.25f).toInt() }
            ) + fadeOut(tween(350, easing = EaseOutQuart))
        } else {
            fadeOut(tween(350, easing = EaseOutQuart))
        }
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (!isLandscapeMode) {
            slideInHorizontally(
                animationSpec = tween(550, easing = EaseOutQuart),
                initialOffsetX = { -(it * 0.25f).toInt() }
            ) + fadeIn(tween(400, easing = EaseOutQuart))
        } else {
            fadeIn(tween(400, easing = EaseOutQuart))
        }
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (!isLandscapeMode) {
            slideOutHorizontally(
                animationSpec = tween(550, easing = EaseOutQuart),
                targetOffsetX = { (it * 0.25f).toInt() }
            ) + fadeOut(tween(350, easing = EaseOutQuart))
        } else {
            fadeOut(tween(350, easing = EaseOutQuart))
        }
    }
}
