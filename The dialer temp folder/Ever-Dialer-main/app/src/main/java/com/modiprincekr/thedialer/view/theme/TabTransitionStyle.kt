package com.modiprincekr.thedialer.view.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination

import com.modiprincekr.thedialer.view.components.TabNavigationHelper
import org.koin.core.context.GlobalContext

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin

/** Tab route order, kept in sync with the user's Settings > Appearance > Tab Sections order —
 *  never hardcoded. Falls back to [PreferenceManager.DEFAULT_TAB_ORDER] until the first sync. */
private var tabRouteOrder: List<String> =
    PreferenceManager.parseTabOrder(null).mapNotNull { TabNavigationHelper.routeForTabKey(it) }

/** Call whenever settings may have changed (e.g. once per recomposition from the screen that
 *  hosts the main NavHost) so the page-switching slide direction always matches the tab order
 *  the user actually configured, however they've arranged it. */
internal fun syncTabTransitionOrder(prefs: PreferenceManager) {
    tabRouteOrder = prefs.getTabOrder().mapNotNull { TabNavigationHelper.routeForTabKey(it) }
}

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val EaseOutExpo  = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

internal var isLandscapeMode: Boolean = false

object TabTransitionStyle : NavHostAnimatedDestinationStyle() {

    private fun getLiveTabOrder(): List<String> {
        return try {
            val prefs = GlobalContext.get().get<PreferenceManager>()
            prefs.getTabOrder().mapNotNull { TabNavigationHelper.routeForTabKey(it) }
        } catch (_: Exception) {
            tabRouteOrder
        }
    }

    private fun routeOrder(route: String?): Int {
        if (route == null) return -1
        val base = route.substringBefore("?").substringBefore("/")
        val order = getLiveTabOrder()
        return order.indexOfFirst { base.contains(it, ignoreCase = true) || it.contains(base, ignoreCase = true) }
    }

    private fun isRecordingsFromSettings(route: String?): Boolean {
        return route != null && route.contains("recordings_screen", ignoreCase = true) && route.contains("openedFromSettings=true", ignoreCase = true)
    }

    private fun isRecordingsFromSettings(entry: NavBackStackEntry?): Boolean {
        if (entry == null) return false
        if (isRecordingsFromSettings(entry.destination.route)) return true
        val route = entry.destination.route ?: return false
        if (!route.contains("recordings_screen", ignoreCase = true)) return false
        return entry.arguments?.getBoolean("openedFromSettings", false) == true ||
               entry.arguments?.getString("openedFromSettings") == "true"
    }

    private fun isTabRoute(route: String?): Boolean = routeOrder(route) >= 0

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromTab = isTabRoute(initialState.destination.route)
        val toTab   = isTabRoute(targetState.destination.route)
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        val toSettings   = isSettingsRoute(targetState)
        val fromSettings = isSettingsRoute(initialState)
        val isChat = isSmsChatRoute(targetState.destination.route) || isSmsChatRoute(initialState.destination.route)
        val isContact = isContactDetailsRoute(targetState.destination.route) || isContactDetailsRoute(initialState.destination.route)
        val enterDuration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_ENTER
            isChat -> CHAT_ANIM_DURATION_ENTER
            else -> SETTINGS_ANIM_DURATION_ENTER
        }
        val enterEasing = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val enterEasingOut = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEaseOut
            else -> SettingsSmoothEaseOut
        }
        val initialScale = 0.04f
        val origin = if (isContact) SettingsClickTracker.lastOrigin else TransformOrigin.Center

        when {
            isRecordingsFromSettings(targetState) -> {
                if (isWindowsPhoneAnimation()) {
                    TurnstileNavigationTracker.recordEnter(isBack = false, targetState.destination.route)
                    fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
                } else {
                    scaleIn(
                        animationSpec = tween(enterDuration, easing = enterEasing),
                        initialScale = 0.04f,
                        transformOrigin = SettingsClickTracker.lastOrigin
                    ) + fadeIn(tween(enterDuration, easing = enterEasingOut))
                }
            }
            toSettings -> {
                if (isWindowsPhoneAnimation()) {
                    TurnstileNavigationTracker.recordEnter(isBack = false, targetState.destination.route)
                    fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
                } else {
                    scaleIn(
                        animationSpec = tween(enterDuration, easing = enterEasing),
                        initialScale = initialScale,
                        transformOrigin = origin
                    ) + fadeIn(tween(enterDuration, easing = enterEasingOut))
                }
            }
            fromSettings -> {
                if (isWindowsPhoneAnimation()) {
                    TurnstileNavigationTracker.recordEnter(isBack = true, targetState.destination.route)
                    fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
                } else {
                    scaleIn(
                        animationSpec = tween(enterDuration, easing = enterEasing),
                        initialScale = SETTINGS_SCALE_EXIT_TO,
                        transformOrigin = TransformOrigin.Center
                    ) + fadeIn(
                        animationSpec = tween(enterDuration, easing = enterEasingOut),
                        initialAlpha = 0.95f
                    )
                }
            }
            fromTab && toTab && !isLandscapeMode -> {
                val goRight = toIdx > fromIdx
                slideInHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    initialOffsetX = { if (goRight) (it * 0.25f).toInt() else -(it * 0.25f).toInt() }
                ) + fadeIn(tween(400, easing = EaseOutQuart))
            }
            !toTab && !isLandscapeMode -> {
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
        val fromSettings = isSettingsRoute(initialState)
        val toSettings   = isSettingsRoute(targetState)
        val isChat = isSmsChatRoute(targetState.destination.route) || isSmsChatRoute(initialState.destination.route)
        val isContact = isContactDetailsRoute(targetState.destination.route) || isContactDetailsRoute(initialState.destination.route)
        val exitDuration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_ENTER
            isChat -> CHAT_ANIM_DURATION_EXIT
            else -> SETTINGS_ANIM_DURATION_EXIT
        }
        val exitEasing = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val exitEasingIn = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEaseIn
            else -> SettingsSmoothEaseIn
        }

        when {
            isRecordingsFromSettings(initialState) && toSettings -> {
                if (isWindowsPhoneAnimation()) {
                    fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
                } else {
                    scaleOut(
                        animationSpec = tween(exitDuration, easing = exitEasing),
                        targetScale = SETTINGS_SCALE_EXIT_TO,
                        transformOrigin = TransformOrigin.Center
                    ) + fadeOut(tween(exitDuration, easing = exitEasingIn))
                }
            }
            fromSettings || toSettings -> {
                if (isWindowsPhoneAnimation()) {
                    fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
                } else {
                    scaleOut(
                        animationSpec = tween(exitDuration, easing = exitEasing),
                        targetScale = SETTINGS_SCALE_EXIT_TO,
                        transformOrigin = TransformOrigin.Center
                    ) + fadeOut(tween(exitDuration, easing = exitEasingIn))
                }
            }
            fromTab && toTab && !isLandscapeMode -> {
                val goRight = toIdx > fromIdx
                slideOutHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    targetOffsetX = { if (goRight) -(it * 0.25f).toInt() else (it * 0.25f).toInt() }
                ) + fadeOut(tween(350, easing = EaseOutQuart))
            }
            !toTab && !isLandscapeMode -> {
                slideOutHorizontally(
                    animationSpec = tween(600, easing = EaseOutExpo),
                    targetOffsetX = { -(it * 0.12f).toInt() }
                ) + fadeOut(tween(400, easing = EaseOutExpo))
            }
            else -> fadeOut(tween(380, easing = EaseOutQuart))
        }
    }

    // Navigation-Compose treats returning to the graph's start destination (the "Calls" tab is
    // marked start = true) as a *pop*, not a regular navigate — so tapping that tab plays this
    // pop transition instead of enterTransition/exitTransition above. It must resolve its slide
    // direction from tabRouteOrder exactly like the regular transitions do, or else it'll always
    // slide in from the same hardcoded side no matter where the user has actually placed that
    // tab in Settings > Appearance > Tab Sections.
    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val fromTab = isTabRoute(initialState.destination.route)
        val toTab   = isTabRoute(targetState.destination.route)
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        val fromSettings = isSettingsRoute(initialState)
        val toSettings   = isSettingsRoute(targetState)
        val isChat = isSmsChatRoute(targetState.destination.route) || isSmsChatRoute(initialState.destination.route)
        val isContact = isContactDetailsRoute(targetState.destination.route) || isContactDetailsRoute(initialState.destination.route)
        val enterDuration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_EXIT
            isChat -> CHAT_ANIM_DURATION_ENTER
            fromSettings -> SETTINGS_ANIM_DURATION_EXIT
            else -> SETTINGS_ANIM_DURATION_ENTER
        }
        val enterEasing = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val enterEasingOut = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEaseOut
            else -> SettingsSmoothEaseOut
        }

        when {
            isRecordingsFromSettings(targetState) && fromSettings -> {
                if (isWindowsPhoneAnimation()) {
                    TurnstileNavigationTracker.recordEnter(isBack = true, targetState.destination.route)
                    fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
                } else {
                    scaleIn(
                        animationSpec = tween(enterDuration, easing = enterEasing),
                        initialScale = SETTINGS_SCALE_EXIT_TO,
                        transformOrigin = TransformOrigin.Center
                    ) + fadeIn(
                        animationSpec = tween(enterDuration, easing = enterEasingOut),
                        initialAlpha = 0.95f
                    )
                }
            }
            fromSettings || toSettings -> {
                if (isWindowsPhoneAnimation()) {
                    TurnstileNavigationTracker.recordEnter(isBack = true, targetState.destination.route)
                    fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
                } else {
                    scaleIn(
                        animationSpec = tween(enterDuration, easing = enterEasing),
                        initialScale = SETTINGS_SCALE_EXIT_TO,
                        transformOrigin = TransformOrigin.Center
                    ) + fadeIn(
                        animationSpec = tween(enterDuration, easing = enterEasingOut),
                        initialAlpha = 0.95f
                    )
                }
            }
            fromTab && toTab && !isLandscapeMode -> {
                val goRight = toIdx > fromIdx
                slideInHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    initialOffsetX = { if (goRight) (it * 0.25f).toInt() else -(it * 0.25f).toInt() }
                ) + fadeIn(tween(400, easing = EaseOutQuart))
            }
            toTab && !isLandscapeMode -> {
                slideInHorizontally(
                    animationSpec = tween(600, easing = EaseOutExpo),
                    initialOffsetX = { -(it * 0.12f).toInt() }
                ) + fadeIn(tween(450, easing = EaseOutExpo))
            }
            else -> fadeIn(tween(450, easing = EaseOutQuart))
        }
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val fromTab = isTabRoute(initialState.destination.route)
        val toTab   = isTabRoute(targetState.destination.route)
        val fromIdx = routeOrder(initialState.destination.route)
        val toIdx   = routeOrder(targetState.destination.route)
        val fromSettings = isSettingsRoute(initialState)
        val toSettings   = isSettingsRoute(targetState)
        val isChat = isSmsChatRoute(targetState.destination.route) || isSmsChatRoute(initialState.destination.route)
        val isContact = isContactDetailsRoute(targetState.destination.route) || isContactDetailsRoute(initialState.destination.route)
        val exitDuration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_EXIT
            isChat -> CHAT_ANIM_DURATION_EXIT
            else -> SETTINGS_ANIM_DURATION_EXIT
        }
        val exitEasing = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val exitEasingIn = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEaseIn
            else -> SettingsSmoothEaseIn
        }

        when {
            isRecordingsFromSettings(initialState) -> {
                if (isWindowsPhoneAnimation()) {
                    slideOutHorizontally(
                        animationSpec = tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing),
                        targetOffsetX = { full -> full / 6 }
                    ) + fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
                } else {
                    val fadeEasing = CubicBezierEasing(0.15f, 0.15f, 0.35f, 1f)
                    scaleOut(
                        animationSpec = tween(exitDuration, easing = exitEasing),
                        targetScale = 0.04f,
                        transformOrigin = SettingsClickTracker.lastOrigin
                    ) + fadeOut(tween(exitDuration, easing = fadeEasing))
                }
            }
            fromSettings || toSettings -> {
                if (isWindowsPhoneAnimation()) {
                    slideOutHorizontally(
                        animationSpec = tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing),
                        targetOffsetX = { full -> full / 6 }
                    ) + fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
                } else {
                    scaleOut(
                        animationSpec = tween(exitDuration, easing = exitEasing),
                        targetScale = SETTINGS_SCALE_EXIT_TO,
                        transformOrigin = TransformOrigin.Center
                    ) + fadeOut(tween(exitDuration, easing = exitEasingIn))
                }
            }
            fromTab && toTab && !isLandscapeMode -> {
                val goRight = toIdx > fromIdx
                slideOutHorizontally(
                    animationSpec = tween(550, easing = EaseOutQuart),
                    targetOffsetX = { if (goRight) -(it * 0.25f).toInt() else (it * 0.25f).toInt() }
                ) + fadeOut(tween(350, easing = EaseOutQuart))
            }
            fromTab && !isLandscapeMode -> {
                slideOutHorizontally(
                    animationSpec = tween(600, easing = EaseOutExpo),
                    targetOffsetX = { (it * 0.35f).toInt() }
                ) + fadeOut(tween(450, easing = EaseOutExpo))
            }
            else -> fadeOut(tween(380, easing = EaseOutQuart))
        }
    }
}
