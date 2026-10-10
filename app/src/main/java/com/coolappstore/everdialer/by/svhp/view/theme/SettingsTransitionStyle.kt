package com.coolappstore.everdialer.by.svhp.view.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import org.koin.compose.koinInject

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import org.koin.core.context.GlobalContext

/**
 * Ultra-smooth expressive easing curves for slow, luxurious zoom motion.
 */
val SettingsSmoothEase = FastOutSlowInEasing
val SettingsSmoothEaseIn = FastOutLinearInEasing
val SettingsSmoothEaseOut = LinearOutSlowInEasing

const val SETTINGS_ANIM_DURATION_ENTER = 400
const val SETTINGS_ANIM_DURATION_EXIT = 400

const val CHAT_ANIM_DURATION_ENTER = 600
const val CHAT_ANIM_DURATION_EXIT = 550
val ChatSmoothEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val ChatSmoothEaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val ChatSmoothEaseIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

const val SETTINGS_SCALE_ENTER_FROM = 0.92f
const val SETTINGS_SCALE_EXIT_TO = 0.96f

fun isSmsChatRoute(route: String?): Boolean {
    if (route == null) return false
    return route.contains("sms_chat", ignoreCase = true)
}

/**
 * Windows Phone Metro-style turnstile page transition specifications,
 * modeled after the Windows Phone 7/8 animation physics and MangoTile's implementation.
 */
val MetroSlideInEasing = CubicBezierEasing(0.22f, 0f, 0.05f, 1f)
const val SETTINGS_WP_ANIM_DURATION_ENTER = 750
const val SETTINGS_WP_ANIM_DURATION_EXIT = 650

fun isWindowsPhoneAnimation(): Boolean {
    return try {
        val prefs = GlobalContext.getOrNull()?.getOrNull<PreferenceManager>()
        prefs?.getString(PreferenceManager.KEY_ANIMATION_STYLE, PreferenceManager.ANIMATION_STYLE_ZOOM) == PreferenceManager.ANIMATION_STYLE_WINDOWS_PHONE
    } catch (_: Throwable) {
        false
    }
}

/**
 * Tracks the last tap/click location so zoom-in originates directly from where the user clicked.
 */
object SettingsClickTracker {
    var lastOrigin: TransformOrigin = TransformOrigin.Center
        private set

    fun recordTap(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        lastOrigin = TransformOrigin.Center
    }

    fun recordNormalized(pivotX: Float, pivotY: Float) {
        lastOrigin = TransformOrigin.Center
    }
}

/**
 * Set of known settings route identifiers for transition matching.
 */
val settingsRoutes = setOf(
    "settings_screen",
    "contact_details_screen",
    "call_log_detail_screen",
    "search_screen",
    "contact_edit_screen",
    "hidden_contacts_screen",
    "interface_screen",
    "app_settings_screen",
    "sound_vibration_screen",
    "biometric_screen",
    "call_settings_screen",
    "caller_ui_screen",
    "caller_u_i_screen",
    "incoming_call_ui_screen",
    "incoming_call_u_i_screen",
    "rain_mode_screen",
    "fake_call_screen",
    "contacts_hider_screen",
    "custom_background_picker_screen",
    "contact_pfp_customization_screen",
    "liquid_glass_elements_screen",
    "blur_effects_elements_screen",
    "app_icon_screen",
    "call_accounts_screen",
    "default_message_app_screen",
    "raise_to_answer_screen",
    "updates_screen",
    "about_app_screen",
    "contributors_screen",
    "more_apps_web_view_screen",
    "ratings_web_view_screen",
    "sms_chat_screen",
    "new_message_screen"
)

fun isSettingsRoute(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/")
    return base in settingsRoutes || base.contains("settings", ignoreCase = true) || base.contains("about", ignoreCase = true) || base.contains("contact_details", ignoreCase = true) || base.contains("call_log_detail", ignoreCase = true) || base.contains("search", ignoreCase = true) || base.contains("contact_edit", ignoreCase = true) || base.contains("sms_chat", ignoreCase = true) || base.contains("new_message", ignoreCase = true)
}

/**
 * Destination style providing dynamic transition styles:
 * - Windows Phone: Classic WP7/WP8 turnstile slide-in with scale and subtle fade
 * - Zoom (in/out): Slow, smooth zoom-in when opening and zoom-out when closing/popping
 * Fully compatible with Predictive Back gestures in Android.
 */
object SettingsTransitionStyle : NavHostAnimatedDestinationStyle() {

    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val isChat = isSmsChatRoute(targetState.destination.route)
        val duration = if (isChat) CHAT_ANIM_DURATION_ENTER else SETTINGS_ANIM_DURATION_ENTER
        val easing = if (isChat) ChatSmoothEase else SettingsSmoothEase
        val easingOut = if (isChat) ChatSmoothEaseOut else SettingsSmoothEaseOut

        if (isWindowsPhoneAnimation()) {
            TurnstileNavigationTracker.recordEnter(isBack = false, targetState.destination.route)
            fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
        } else {
            scaleIn(
                animationSpec = tween(duration, easing = easing),
                initialScale = SETTINGS_SCALE_ENTER_FROM,
                transformOrigin = TransformOrigin.Center
            ) + fadeIn(tween(duration, easing = easingOut))
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val isChat = isSmsChatRoute(initialState.destination.route)
        val duration = if (isChat) CHAT_ANIM_DURATION_EXIT else SETTINGS_ANIM_DURATION_EXIT
        val easing = if (isChat) ChatSmoothEase else SettingsSmoothEase
        val easingIn = if (isChat) ChatSmoothEaseIn else SettingsSmoothEaseIn

        if (isWindowsPhoneAnimation()) {
            fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
        } else {
            scaleOut(
                animationSpec = tween(duration, easing = easing),
                targetScale = SETTINGS_SCALE_EXIT_TO,
                transformOrigin = TransformOrigin.Center
            ) + fadeOut(tween(duration, easing = easingIn))
        }
    }

    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val isChat = isSmsChatRoute(targetState.destination.route)
        val duration = if (isChat) CHAT_ANIM_DURATION_ENTER else SETTINGS_ANIM_DURATION_ENTER
        val easing = if (isChat) ChatSmoothEase else SettingsSmoothEase
        val easingOut = if (isChat) ChatSmoothEaseOut else SettingsSmoothEaseOut

        if (isWindowsPhoneAnimation()) {
            TurnstileNavigationTracker.recordEnter(isBack = true, targetState.destination.route)
            fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
        } else {
            scaleIn(
                animationSpec = tween(duration, easing = easing),
                initialScale = SETTINGS_SCALE_EXIT_TO,
                transformOrigin = TransformOrigin.Center
            ) + fadeIn(tween(duration, easing = easingOut))
        }
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val isChat = isSmsChatRoute(initialState.destination.route)
        val duration = if (isChat) CHAT_ANIM_DURATION_EXIT else SETTINGS_ANIM_DURATION_EXIT
        val easing = if (isChat) ChatSmoothEase else SettingsSmoothEase
        val easingIn = if (isChat) ChatSmoothEaseIn else SettingsSmoothEaseIn

        if (isWindowsPhoneAnimation()) {
            slideOutHorizontally(
                animationSpec = tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing),
                targetOffsetX = { full -> full / 6 }
            ) + fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
        } else {
            scaleOut(
                animationSpec = tween(duration, easing = easing),
                targetScale = SETTINGS_SCALE_ENTER_FROM,
                transformOrigin = TransformOrigin.Center
            ) + fadeOut(tween(duration, easing = easingIn))
        }
    }
}

/**
 * Modifier that applies a subtle dynamic motion blur during the zoom transition when opening a page,
 * and attaches a NestedScrollConnection to provide scroll haptics across all settings screens.
 * Controlled by user toggles in Settings.
 */
@Composable
fun Modifier.settingsMotionBlur(maxBlurDp: Float = 10f): Modifier {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val motionBlurEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_MOTION_BLUR_ANIMATION, false)
    }
    val scrollHapticsEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, false)
    }
    val cmPerHaptic = remember(settingsVer) {
        prefs.getFloat(PreferenceManager.KEY_SCROLL_CM_PER_HAPTIC, 1.5f)
    }
    val hapticAmplitude = remember(settingsVer) {
        prefs.getInt(PreferenceManager.KEY_SCROLL_HAPTIC_STRENGTH, 60)
    }

    val pxPerCm = with(density) { (160f / 2.54f).dp.toPx() }
    val pxThreshold = (cmPerHaptic * pxPerCm).coerceAtLeast(8f)

    val nestedScrollConnection = remember(scrollHapticsEnabled, pxThreshold, hapticAmplitude) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            private var hapticBucket = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): Offset {
                if (scrollHapticsEnabled) {
                    val delta = kotlin.math.abs(consumed.y)
                    if (delta > 0f) {
                        hapticBucket += delta
                        if (hapticBucket >= pxThreshold) {
                            val count = (hapticBucket / pxThreshold).toInt()
                            hapticBucket -= count * pxThreshold
                            com.coolappstore.everdialer.by.svhp.view.components.performScrollHaptic(context, hapticAmplitude)
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    var mod: Modifier = this.nestedScroll(nestedScrollConnection)

    if (motionBlurEnabled) {
        var isSettled by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isSettled = true
        }
        val blurFactor by animateFloatAsState(
            targetValue = if (isSettled) 0f else 1f,
            animationSpec = tween(durationMillis = SETTINGS_ANIM_DURATION_ENTER, easing = SettingsSmoothEase),
            label = "settingsMotionBlur"
        )
        val blurRadius = (blurFactor * maxBlurDp).dp
        if (blurRadius > 0.1.dp) {
            mod = mod.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        }
    }

    return mod
}

