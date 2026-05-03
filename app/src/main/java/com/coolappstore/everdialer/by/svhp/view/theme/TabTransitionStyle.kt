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

// Smooth expressive decelerate easing
private val EaseOutExpressive = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

// Shared mutable flag updated from MainActivity / screens
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
        when {
            isLandscapeMode -> fadeIn(tween(300, easing = EaseOutExpressive))
            fromIdx >= 0 && toIdx >= 0 -> {
                val goRight = toIdx > fromIdx
                slideInHorizontally(
                    animationSpec = tween(500, easing = EaseOutExpressive),
                    initialOffsetX = { if (goRight) (it * 0.35f).toInt() else -(it * 0.35f).toInt() }
                ) + fadeIn(tween(380, easing = EaseOutExpressive))
            }
            else -> slideInHorizontally(
                animationSpec = tween(500, easing = EaseOutExpressive),
                initialOffsetX = { (it * 0.35f).toInt() }
            ) + fadeIn(tween(380))
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        when {
            isLandscapeMode -> fadeOut(tween(250, easing = EaseOutExpressive))
            fromIdx >= 0 && toIdx >= 0 -> {
                val goRight = toIdx > fromIdx
                slideOutHorizontally(
                    animationSpec = tween(500, easing = EaseOutExpressive),
                    targetOffsetX = { if (goRight) -(it * 0.35f).toInt() else (it * 0.35f).toInt() }
                ) + fadeOut(tween(320, easing = EaseOutExpressive))
            }
            else -> slideOutHorizontally(
                animationSpec = tween(500, easing = EaseOutExpressive),
                targetOffsetX = { -(it * 0.35f).toInt() }
            ) + fadeOut(tween(320))
        }
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isLandscapeMode) fadeIn(tween(300, easing = EaseOutExpressive))
        else slideInHorizontally(
            animationSpec = tween(500, easing = EaseOutExpressive),
            initialOffsetX = { -(it * 0.35f).toInt() }
        ) + fadeIn(tween(380))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isLandscapeMode) fadeOut(tween(250, easing = EaseOutExpressive))
        else slideOutHorizontally(
            animationSpec = tween(500, easing = EaseOutExpressive),
            targetOffsetX = { (it * 0.35f).toInt() }
        ) + fadeOut(tween(320))
    }
}
