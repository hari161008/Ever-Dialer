package com.coolappstore.everdialer.by.svhp.view.theme

import android.content.res.Configuration
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

private val TAB_ROUTES = listOf(
    "favorites_screen",
    "recent_screen",
    "contact_screen",
    "notes_screen"
)

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
            isLandscapeMode -> fadeIn(tween(220))
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
            isLandscapeMode -> fadeOut(tween(180))
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
        if (isLandscapeMode) fadeIn(tween(220))
        else slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isLandscapeMode) fadeOut(tween(180))
        else slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280))
    }
}
