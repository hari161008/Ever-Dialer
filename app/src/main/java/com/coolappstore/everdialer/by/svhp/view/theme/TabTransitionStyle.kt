package com.coolappstore.everdialer.by.svhp.view.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

// Keep route strings as plain constants so this file has zero dependency on
// generated Destinations classes (generated code runs AFTER this file compiles).
private val TAB_ROUTES = listOf(
    "favorites_screen",
    "recent_screen",
    "contact_screen",
    "notes_screen"
)

/**
 * Direction-aware horizontal slide animation for bottom-tab navigation.
 * Tabs slide left/right based on their logical order.
 * Any non-tab screen (settings, call details …) falls back to a standard push/pop.
 */
object TabTransitionStyle : NavHostAnimatedDestinationStyle() {

    private fun routeOrder(route: String?): Int {
        if (route == null) return -1
        // Strip query-param / argument suffix that compose-destinations may append
        val base = route.substringBefore("?").substringBefore("/")
        return TAB_ROUTES.indexOfFirst { base.contains(it, ignoreCase = true) }
    }

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        when {
            fromIdx >= 0 && toIdx >= 0 -> {
                val goRight = toIdx > fromIdx
                slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) {
                    if (goRight) it else -it
                } + fadeIn(tween(260))
            }
            else -> slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        when {
            fromIdx >= 0 && toIdx >= 0 -> {
                val goRight = toIdx > fromIdx
                slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) {
                    if (goRight) -it else it
                } + fadeOut(tween(260))
            }
            else -> slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280))
        }
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280))
    }
}
