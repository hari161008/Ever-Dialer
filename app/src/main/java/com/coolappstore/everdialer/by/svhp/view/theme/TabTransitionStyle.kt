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
private val EaseOutExpo  = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

internal var isLandscapeMode: Boolean = false

object TabTransitionStyle : NavHostAnimatedDestinationStyle() {

    private fun routeOrder(route: String?): Int {
        if (route == null) return -1
        val base = route.substringBefore("?").substringBefore("/")
        return TAB_ROUTES.indexOfFirst { base.contains(it, ignoreCase = true) }
    }

    private fun isTabRoute(route: String?): Boolean = routeOrder(route) >= 0

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromTab = isTabRoute(initialState.destination.route)
        val toTab   = isTabRoute(targetState.destination.route)
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)

        when {
            fromTab && toTab && !isLandscapeMode -> {
                val goRight = toIdx > fromIdx
                slideInHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    initialOffsetX = { if (goRight) (it * 0.25f).toInt() else -(it * 0.25f).toInt() }
                ) + fadeIn(tween(400, easing = EaseOutQuart))
            }
            !toTab && !isLandscapeMode -> {
                // Pushing into a detail/settings screen: slide in from right
                slideInHorizontally(
                    animationSpec = tween(600, easing = EaseOutExpo),
                    initialOffsetX = { (it * 0.35f).toInt() }
                ) + fadeIn(tween(500, easing = EaseOutExpo))
            }
            else -> fadeIn(tween(450, easing = EaseOutQuart))
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromTab = isTabRoute(initialState.destination.route)
        val toTab   = isTabRoute(targetState.destination.route)
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)

        when {
            fromTab && toTab && !isLandscapeMode -> {
                val goRight = toIdx > fromIdx
                slideOutHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    targetOffsetX = { if (goRight) -(it * 0.25f).toInt() else (it * 0.25f).toInt() }
                ) + fadeOut(tween(350, easing = EaseOutQuart))
            }
            !toTab && !isLandscapeMode -> {
                // Pushing to detail: current screen slides slightly left and fades
                slideOutHorizontally(
                    animationSpec = tween(600, easing = EaseOutExpo),
                    targetOffsetX = { -(it * 0.12f).toInt() }
                ) + fadeOut(tween(400, easing = EaseOutExpo))
            }
            else -> fadeOut(tween(380, easing = EaseOutQuart))
        }
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val toTab = isTabRoute(targetState.destination.route)
        if (!isLandscapeMode) {
            if (toTab) {
                slideInHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    initialOffsetX = { -(it * 0.25f).toInt() }
                ) + fadeIn(tween(400, easing = EaseOutQuart))
            } else {
                // Popping back to a detail screen: slide back in from left
                slideInHorizontally(
                    animationSpec = tween(600, easing = EaseOutExpo),
                    initialOffsetX = { -(it * 0.12f).toInt() }
                ) + fadeIn(tween(450, easing = EaseOutExpo))
            }
        } else {
            fadeIn(tween(450, easing = EaseOutQuart))
        }
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (!isLandscapeMode) {
            slideOutHorizontally(
                animationSpec = tween(600, easing = EaseOutExpo),
                targetOffsetX = { (it * 0.35f).toInt() }
            ) + fadeOut(tween(450, easing = EaseOutExpo))
        } else {
            fadeOut(tween(380, easing = EaseOutQuart))
        }
    }
}
