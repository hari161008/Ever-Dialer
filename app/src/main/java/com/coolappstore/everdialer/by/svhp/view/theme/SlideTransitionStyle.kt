package com.coolappstore.everdialer.by.svhp.view.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object SlideTransitionStyle : NavHostAnimatedDestinationStyle() {

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280))
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280))
    }
}
