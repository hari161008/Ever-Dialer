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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Ultra-smooth expressive easing curves for slow, luxurious zoom motion.
 */
val SettingsSmoothEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val SettingsSmoothEaseIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
val SettingsSmoothEaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)

const val SETTINGS_ANIM_DURATION_ENTER = 420
const val SETTINGS_ANIM_DURATION_EXIT = 800

const val CHAT_ANIM_DURATION_ENTER = 600
const val CHAT_ANIM_DURATION_EXIT = 550
val ChatSmoothEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val ChatSmoothEaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val ChatSmoothEaseIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

const val CONTACT_DETAILS_ANIM_DURATION_ENTER = 800
const val CONTACT_DETAILS_ANIM_DURATION_EXIT = 800
val ContactDetailsSmoothEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val ContactDetailsSmoothEaseExit = CubicBezierEasing(0.2f, 0f, 0f, 1f)

const val SETTINGS_SCALE_ENTER_FROM = 0.88f
const val SETTINGS_SCALE_EXIT_TO = 0.96f

fun isSmsChatRoute(route: String?): Boolean {
    if (route == null) return false
    return route.contains("sms_chat", ignoreCase = true)
}

fun isContactDetailsRoute(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/")
    return base.contains("contact_details", ignoreCase = true) || base.contains("call_log_detail", ignoreCase = true)
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

    // Constant origin reserved specifically for the Ever Dialer Settings button in TopBar
    var settingsIconOrigin: TransformOrigin = TransformOrigin(0.92f, 0.05f)

    var isSettingsOpening by mutableStateOf(false)
    var isSearchOpening by mutableStateOf(false)
    var isNewMessageOpening by mutableStateOf(false)
    var isNewMessageClosing by mutableStateOf(false)
    var isNewMessageOpen by mutableStateOf(false)

    var settingsBounceTrigger by mutableIntStateOf(0)
    var searchBounceTrigger by mutableIntStateOf(0)
    var isClosingBounce by mutableStateOf(false)
        private set

    private var lastSettingsBounceTime = 0L
    private var lastSearchBounceTime = 0L
    private var hasFiredSettingsCloseBounce = false

    fun onSettingsScreenEntered() {
        hasFiredSettingsCloseBounce = false
    }

    fun triggerSettingsOpenBounce() {
        val now = System.currentTimeMillis()
        if (now - lastSettingsBounceTime > 250L) {
            lastSettingsBounceTime = now
            hasFiredSettingsCloseBounce = false
            isClosingBounce = false
            settingsBounceTrigger++
        }
    }

    fun triggerSettingsCloseBounce() {
        if (!hasFiredSettingsCloseBounce) {
            hasFiredSettingsCloseBounce = true
            isClosingBounce = true
            lastSettingsBounceTime = System.currentTimeMillis()
            settingsBounceTrigger++
        }
    }

    fun triggerSettingsBounce() {
        triggerSettingsCloseBounce()
    }

    fun triggerSearchBounce() {
        val now = System.currentTimeMillis()
        if (now - lastSearchBounceTime > 250L) {
            lastSearchBounceTime = now
            searchBounceTrigger++
        }
    }

    private val routeOrigins = mutableMapOf<String, TransformOrigin>()

    fun setOriginForRoute(route: String?, origin: TransformOrigin) {
        if (route == null) return
        val base = route.substringBefore("?").substringBefore("/")
        routeOrigins[base] = origin
        routeOrigins[route] = origin
    }

    fun getOriginForRoute(route: String?): TransformOrigin {
        if (route == null) return lastOrigin
        val base = route.substringBefore("?").substringBefore("/")
        return routeOrigins[base] ?: routeOrigins[route] ?: lastOrigin
    }

    fun recordTap(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        if (screenWidth > 0f && screenHeight > 0f) {
            val px = (x / screenWidth).coerceIn(0f, 1f)
            val py = (y / screenHeight).coerceIn(0f, 1f)
            lastOrigin = TransformOrigin(px, py)
        }
    }

    fun recordNormalized(pivotX: Float, pivotY: Float) {
        lastOrigin = TransformOrigin(pivotX.coerceIn(0f, 1f), pivotY.coerceIn(0f, 1f))
    }

    fun notifyClosing() {
        isSettingsOpening = false
        isSearchOpening = false
        isNewMessageOpening = false
        isNewMessageClosing = true
        isNewMessageOpen = false
    }
}

/**
 * Wrapper for root settings, search, contact info, and new message destinations.
 */
@Composable
fun ExpandingScreenContainer(
    origin: TransformOrigin = SettingsClickTracker.lastOrigin,
    onClose: () -> Unit,
    content: @Composable (closeWithAnimation: () -> Unit) -> Unit
) {
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val handleClose = {
        SettingsClickTracker.notifyClosing()
        SettingsClickTracker.triggerSettingsCloseBounce()
        if (backDispatcher != null) {
            backDispatcher.onBackPressed()
        } else {
            onClose()
        }
    }
    content(handleClose)
}

/**
 * Set of known settings route identifiers for transition matching.
 */
val settingsRoutes = setOf(
    "settings_screen",
    "call_recording_settings_screen",
    "contact_details_screen",
    "call_log_detail_screen",
    "search_screen",
    "contact_edit_screen",
    "hidden_contacts_screen",
    "interface_screen",
    "app_settings_screen",
    "sound_vibration_screen",
    "volume_dnd_screen",
    "biometric_screen",
    "call_settings_screen",
    "caller_ui_screen",
    "caller_u_i_screen",
    "incoming_call_ui_screen",
    "incoming_call_u_i_screen",
    "call_logs_settings_screen",
    "call_accounts_screen",
    "sim_and_call_placement_screen",
    "answer_style_screen",
    "raise_to_answer_screen",
    "swipe_actions_screen",
    "rain_mode_screen",
    "fake_call_screen",
    "contacts_hider_screen",
    "custom_background_picker_screen",
    "contact_pfp_customization_screen",
    "liquid_glass_elements_screen",
    "blur_effects_elements_screen",
    "app_icon_screen",
    "sms_app_icon_screen",
    "custom_font_screen",
    "dialpad_settings_screen",
    "default_message_app_screen",
    "navigation_settings_screen",
    "pc_connect_screen",
    "pc_sync_settings_screen",
    "language_settings_screen",
    "favourites_settings_screen",
    "contact_info_settings_screen",
    "debug_settings_screen",
    "updates_screen",
    "about_app_screen",
    "about_screen",
    "contributors_screen",
    "more_apps_web_view_screen",
    "ratings_web_view_screen",
    "donate_web_view_screen",
    "sms_settings_screen",
    "sms_chat_screen",
    "new_message_screen",
    "blocked_numbers_screen",
    "blocked_sms_screen"
)

fun isRecordingsFromSettings(entry: NavBackStackEntry?): Boolean {
    if (entry == null) return false
    val route = entry.destination.route ?: return false
    if (!route.contains("recordings_screen", ignoreCase = true)) return false
    return entry.arguments?.getBoolean("openedFromSettings", false) == true ||
           entry.arguments?.getString("openedFromSettings") == "true" ||
           route.contains("openedFromSettings=true", ignoreCase = true)
}

fun isSettingsRoute(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/")
    if (route.contains("recordings_screen", ignoreCase = true) && route.contains("openedFromSettings=true", ignoreCase = true)) return true
    return base in settingsRoutes || base.contains("settings", ignoreCase = true) || base.contains("about", ignoreCase = true) || base.contains("contact_details", ignoreCase = true) || base.contains("call_log_detail", ignoreCase = true) || base.contains("search", ignoreCase = true) || base.contains("contact_edit", ignoreCase = true) || base.contains("sms_chat", ignoreCase = true) || base.contains("new_message", ignoreCase = true) || base.contains("blocked", ignoreCase = true)
}

fun isSettingsRoute(entry: NavBackStackEntry?): Boolean {
    if (entry == null) return false
    if (isRecordingsFromSettings(entry)) return true
    return isSettingsRoute(entry.destination.route)
}

fun isExpandingRoute(route: String?): Boolean {
    if (route == null) return false
    val base = route.substringBefore("?").substringBefore("/")
    if (isSmsChatRoute(route)) return false
    return isSettingsRoute(route) ||
        base == "contact_details_screen" ||
        base == "call_log_detail_screen" ||
        base == "new_message_screen" ||
        base == "search_screen"
}

fun isExpandingRoute(entry: NavBackStackEntry?): Boolean {
    if (entry == null) return false
    if (isRecordingsFromSettings(entry)) return true
    return isExpandingRoute(entry.destination.route)
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
        val isContact = isContactDetailsRoute(targetState.destination.route)
        val isExpanding = isExpandingRoute(targetState)
        val duration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_ENTER
            isChat -> CHAT_ANIM_DURATION_ENTER
            else -> 600
        }
        val easing = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val easingOut = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEaseOut
            else -> SettingsSmoothEaseOut
        }
        val isRootSettings = targetState.destination.route?.substringBefore("?")?.substringBefore("/") == "settings_screen"
        val origin = when {
            isRootSettings -> SettingsClickTracker.settingsIconOrigin
            isExpanding || isContact -> SettingsClickTracker.lastOrigin
            else -> TransformOrigin.Center
        }
        if (isExpanding || isContact) {
            SettingsClickTracker.setOriginForRoute(targetState.destination.route, origin)
        }
        val initialScale = when {
            isContact -> 0.04f
            isExpanding -> 0.04f
            else -> 0.92f
        }

        if (isWindowsPhoneAnimation()) {
            TurnstileNavigationTracker.recordEnter(isBack = false, targetState.destination.route)
            fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
        } else {
            scaleIn(
                animationSpec = tween(duration, easing = easing),
                initialScale = initialScale,
                transformOrigin = origin
            ) + fadeIn(tween(duration, easing = easingOut))
        }
    }

    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val isChat = isSmsChatRoute(initialState.destination.route)
        val isContact = isContactDetailsRoute(initialState.destination.route)
        val duration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_EXIT
            isChat -> CHAT_ANIM_DURATION_EXIT
            else -> SETTINGS_ANIM_DURATION_EXIT
        }
        val easing = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val easingIn = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEaseIn
            else -> SettingsSmoothEaseIn
        }

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
        val isContact = isContactDetailsRoute(targetState.destination.route)
        val duration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_ENTER
            isChat -> CHAT_ANIM_DURATION_ENTER
            else -> SETTINGS_ANIM_DURATION_EXIT
        }
        val easing = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val easingOut = when {
            isContact -> ContactDetailsSmoothEase
            isChat -> ChatSmoothEaseOut
            else -> SettingsSmoothEaseOut
        }

        val isFromRootSettings = initialState.destination.route?.substringBefore("?")?.substringBefore("/") == "settings_screen"
        if (isFromRootSettings || isRecordingsFromSettings(initialState)) {
            SettingsClickTracker.triggerSettingsCloseBounce()
        }

        if (isWindowsPhoneAnimation()) {
            TurnstileNavigationTracker.recordEnter(isBack = true, targetState.destination.route)
            fadeIn(tween(SETTINGS_WP_ANIM_DURATION_ENTER, easing = LinearOutSlowInEasing))
        } else {
            scaleIn(
                animationSpec = tween(duration, easing = easing),
                initialScale = SETTINGS_SCALE_EXIT_TO,
                transformOrigin = TransformOrigin.Center
            ) + fadeIn(
                animationSpec = tween(duration, easing = easingOut),
                initialAlpha = 0.95f
            )
        }
    }

    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val isChat = isSmsChatRoute(initialState.destination.route)
        val isContact = isContactDetailsRoute(initialState.destination.route)
        val isExpanding = isExpandingRoute(initialState)
        val duration = when {
            isContact -> CONTACT_DETAILS_ANIM_DURATION_EXIT
            isChat -> CHAT_ANIM_DURATION_EXIT
            else -> SETTINGS_ANIM_DURATION_EXIT
        }
        val easing = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEase
            else -> SettingsSmoothEase
        }
        val easingIn = when {
            isContact -> ContactDetailsSmoothEaseExit
            isChat -> ChatSmoothEaseIn
            else -> SettingsSmoothEaseIn
        }
        val isRootSettings = initialState.destination.route?.substringBefore("?")?.substringBefore("/") == "settings_screen"
        if (isRootSettings || isRecordingsFromSettings(initialState)) {
            SettingsClickTracker.triggerSettingsCloseBounce()
        }
        val origin = when {
            isRootSettings -> SettingsClickTracker.settingsIconOrigin
            isExpanding || isContact -> SettingsClickTracker.getOriginForRoute(initialState.destination.route)
            else -> TransformOrigin.Center
        }
        val targetScale = when {
            isContact -> 0.04f
            isExpanding -> 0.04f
            else -> 0.92f
        }

        if (isWindowsPhoneAnimation()) {
            slideOutHorizontally(
                animationSpec = tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing),
                targetOffsetX = { full -> full / 6 }
            ) + fadeOut(tween(SETTINGS_WP_ANIM_DURATION_EXIT, easing = FastOutLinearInEasing))
        } else {
            val fadeEasing = if (isContact || isExpanding) SettingsSmoothEase else easingIn
            scaleOut(
                animationSpec = tween(duration, easing = easing),
                targetScale = targetScale,
                transformOrigin = origin
            ) + fadeOut(tween(duration, easing = fadeEasing))
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
            mod = mod.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
        }
    }

    return mod
}

