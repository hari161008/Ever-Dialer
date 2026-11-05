package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import com.coolappstore.everdialer.by.svhp.feature.AppFeatures
import com.ramcosta.composedestinations.generated.destinations.SmsScreenDestination
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Chat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.os.Build
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.drawPlainBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.blur
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.lens
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.colorControls
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop

// Tab routes — only show the bar when one of these is active
private val TAB_ROUTES = setOf(
    FavoritesScreenDestination.route,
    RecentScreenDestination.route,
    ContactScreenDestination.route,
    SmsScreenDestination.route,
    GroupsScreenDestination.route,
    RecordingsScreenDestination.route,
    NotesScreenDestination.route,
    DialPadScreenDestination.route
)

/** Describes a single bottom-navigation tab, driving both the pill-style and standard nav bars. */
private data class TabSpec(
    val key: String,
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

/** Parses the user-configured tab order preference into an ordered list of tab keys.
 *  Delegates to [PreferenceManager.parseTabOrder] so this stays in sync with the tab
 *  order used everywhere else (e.g. the page-switching transition animation). */
private fun parseTabOrder(raw: String?): List<String> = PreferenceManager.parseTabOrder(raw)

@Composable
fun BottomBar(navController: NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) return
    val prefs         = koinInject<PreferenceManager>()
    val context       = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val settingsState by prefs.settingsChanged.collectAsState()

    val pillNav          = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
    val scrollIndication = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SCROLL_INDICATION, true) }
    val iconOnly         = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false) }
    val liquidGlass  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }
    val lgBottomNav  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_LG_BOTTOM_NAV, true) }
    val blurEffects  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
    val blurBottomNav = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_BLUR_BOTTOM_NAV, true) }
    val lgBottomNavConfig = remember(settingsState) { prefs.getLiquidGlassConfig(PreferenceManager.KEY_LG_BOTTOM_NAV) }
    val blurBottomNavConfig = remember(settingsState) { prefs.getBlurConfig(PreferenceManager.KEY_BLUR_BOTTOM_NAV) }
    val slideToOpen      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SLIDE_TO_OPEN, true) }
    val globalBackdrop   = LocalLiquidGlassBackdrop.current
    val useLgBottomNav   = liquidGlass && lgBottomNav && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
    val useBlurBottomNav = blurEffects && blurBottomNav && !useLgBottomNav && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
    val showFavoritesTab  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true) }
    val showCallsTab      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true) }
    val showContactsTab   = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true) }
    val showSmsTab        = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true) }
    val showGroupsTab     = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false) }
    val showRecordingsTab = remember(settingsState) {
        AppFeatures.isCallRecorderAvailable && prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)
    }
    val showNotesTab      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true) }
    val showDialpadTab    = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false) }
    val tabOrder          = remember(settingsState) { parseTabOrder(prefs.getString(PreferenceManager.KEY_TAB_ORDER, null)) }
    val labelStyle: TextStyle = MaterialTheme.typography.labelMedium

    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute       = currentDestination?.route ?: ""

    val isFavoritesSelected  = currentDestination?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true
    val isRecentsSelected    = currentDestination?.hierarchy?.any { it.route == RecentScreenDestination.route } == true
    val isContactsSelected   = currentDestination?.hierarchy?.any { it.route == ContactScreenDestination.route } == true
    val isSmsSelected        = currentDestination?.hierarchy?.any { it.route == SmsScreenDestination.route } == true
    val isGroupsSelected     = currentDestination?.hierarchy?.any { it.route == GroupsScreenDestination.route } == true
    val isRecordingsSelected = currentDestination?.hierarchy?.any { it.route == RecordingsScreenDestination.route } == true
    val isNotesSelected      = currentDestination?.hierarchy?.any { it.route == NotesScreenDestination.route } == true
    val isDialpadSelected    = currentDestination?.hierarchy?.any { it.route == DialPadScreenDestination.route } == true

    // Build visible tab routes dynamically based on prefs
    val visibleTabRoutes = remember(showFavoritesTab, showCallsTab, showContactsTab, showSmsTab, showGroupsTab, showRecordingsTab, showNotesTab, showDialpadTab) {
        buildSet {
            if (showFavoritesTab)  add(FavoritesScreenDestination.route)
            if (showCallsTab)      add(RecentScreenDestination.route)
            if (showContactsTab)   add(ContactScreenDestination.route)
            if (showSmsTab)        add(SmsScreenDestination.route)
            if (showGroupsTab)     add(GroupsScreenDestination.route)
            if (showRecordingsTab) add(RecordingsScreenDestination.route)
            if (showNotesTab)      add(NotesScreenDestination.route)
            if (showDialpadTab)    add(DialPadScreenDestination.route)
        }
    }

    val isRecordingsFromSettings = currentRoute.contains("recordings_screen", ignoreCase = true) &&
        (navBackStackEntry?.arguments?.getBoolean("openedFromSettings", false) == true ||
         navBackStackEntry?.arguments?.getString("openedFromSettings") == "true" ||
         NavBarVisibilityState.hideForSettingsEntry)

    // Only render pill when a visible tab screen is active, and not while a tab screen
    // (e.g. Recordings) is showing its own full-screen onboarding content or opened from settings.
    val isOnTabScreen = visibleTabRoutes.any { currentRoute.contains(it, ignoreCase = true) } &&
        !isRecordingsFromSettings &&
        !NavBarVisibilityState.hideForOnboarding &&
        !NavBarVisibilityState.hideForSelectionMode &&
        !NavBarVisibilityState.hideForSettingsEntry &&
        !NavBarVisibilityState.hideForSearchResult &&
        !NavBarVisibilityState.hideForDialpad

    // If current tab is now hidden, redirect to first visible tab. This must only fire for
    // tabs the user actually disabled in Settings > Tab Sections — not for a visible tab that's
    // just temporarily hiding the nav bar for its own onboarding content (e.g. Recordings'
    // disclaimer/permissions gate), otherwise tapping that tab would immediately get redirected
    // away again in a loop.
    val isOnHiddenTab = TAB_ROUTES.any { currentRoute.contains(it, ignoreCase = true) } &&
        visibleTabRoutes.none { currentRoute.contains(it, ignoreCase = true) } &&
        !isRecordingsFromSettings &&
        !NavBarVisibilityState.hideForSettingsEntry &&
        !NavBarVisibilityState.hideForSearchResult &&
        !currentRoute.contains(GroupsScreenDestination.route, ignoreCase = true) &&
        !currentRoute.contains(DialPadScreenDestination.baseRoute, ignoreCase = true)

    LaunchedEffect(isOnHiddenTab) {
        if (isOnHiddenTab) {
            val firstVisible = tabOrder
                .asSequence()
                .mapNotNull { TabNavigationHelper.routeForTabKey(it) }
                .firstOrNull { it in visibleTabRoutes }
                ?: RecentScreenDestination.route
            TabNavigationHelper.navigateToTab(navController, firstVisible)
        }
    }

    // ── Slide-in animation — re-triggers smoothly when pill re-enters ───
    var pillVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isOnTabScreen) {
        pillVisible = isOnTabScreen
    }
    val localDensity = LocalDensity.current
    val pillExitOffsetY = with(localDensity) { 180.dp.toPx() }
    val pillOffsetY by animateFloatAsState(
        targetValue   = if (pillVisible) 0f else pillExitOffsetY,
        animationSpec = tween(
            durationMillis = if (pillVisible) 600 else 700,
            easing         = if (pillVisible) CubicBezierEasing(0.2f, 0f, 0f, 1f) else CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
        ),
        label         = "pillSlideIn"
    )
    val pillAlpha by animateFloatAsState(
        targetValue   = if (pillVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pillVisible) 550 else 650,
            easing         = if (pillVisible) CubicBezierEasing(0.2f, 0f, 0f, 1f) else CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        ),
        label         = "pillFadeIn"
    )

    var showTabSectionsDialog by remember { mutableStateOf(false) }

    fun doHaptic() {
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            performAppHaptic(
                context,
                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
            )
        }
    }

    fun navigate(route: String) {
        TabNavigationHelper.navigateToTab(navController, route)
    }

    val appLang = com.coolappstore.everdialer.by.svhp.controller.util.LocalAppLanguage.current
    val orderedTabs: List<TabSpec> = remember(
        tabOrder, showFavoritesTab, showCallsTab, showContactsTab, showSmsTab, showGroupsTab, showRecordingsTab, showNotesTab, showDialpadTab,
        isFavoritesSelected, isRecentsSelected, isContactsSelected, isSmsSelected, isGroupsSelected, isRecordingsSelected, isNotesSelected, isDialpadSelected,
        appLang
    ) {
        tabOrder.mapNotNull { key ->
            when (key) {
                "favorites" -> if (showFavoritesTab) TabSpec(
                    key = key, route = FavoritesScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Favourites", appLang),
                    selectedIcon = Icons.Filled.Favorite, unselectedIcon = Icons.Outlined.FavoriteBorder,
                    selected = isFavoritesSelected,
                    onClick = { doHaptic(); navigate(FavoritesScreenDestination.route) }
                ) else null
                "calls" -> if (showCallsTab) TabSpec(
                    key = key, route = RecentScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Calls", appLang),
                    selectedIcon = Icons.Filled.History, unselectedIcon = Icons.Outlined.History,
                    selected = isRecentsSelected,
                    onClick = { doHaptic(); navigate(RecentScreenDestination.route) }
                ) else null
                "contacts" -> if (showContactsTab) TabSpec(
                    key = key, route = ContactScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Contacts", appLang),
                    selectedIcon = Icons.Filled.Person, unselectedIcon = Icons.Outlined.Person,
                    selected = isContactsSelected,
                    onClick = { doHaptic(); navigate(ContactScreenDestination.route) }
                ) else null
                "sms" -> if (showSmsTab) TabSpec(
                    key = key, route = SmsScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("SMS", appLang),
                    selectedIcon = Icons.Filled.Chat, unselectedIcon = Icons.Outlined.Chat,
                    selected = isSmsSelected,
                    onClick = { doHaptic(); navigate(SmsScreenDestination().route) }
                ) else null
                "groups" -> if (showGroupsTab) TabSpec(
                    key = key, route = GroupsScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Groups", appLang),
                    selectedIcon = Icons.Filled.Group, unselectedIcon = Icons.Outlined.Group,
                    selected = isGroupsSelected,
                    onClick = { doHaptic(); navigate(GroupsScreenDestination.route) }
                ) else null
                "recordings" -> if (showRecordingsTab) TabSpec(
                    key = key, route = RecordingsScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Recordings", appLang),
                    selectedIcon = Icons.Filled.FiberManualRecord, unselectedIcon = Icons.Outlined.FiberManualRecord,
                    selected = isRecordingsSelected,
                    onClick = { doHaptic(); navigate(RecordingsScreenDestination.route) }
                ) else null
                "notes" -> if (showNotesTab) TabSpec(
                    key = key, route = NotesScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Notes", appLang),
                    selectedIcon = Icons.Filled.Note, unselectedIcon = Icons.Outlined.Note,
                    selected = isNotesSelected,
                    onClick = { doHaptic(); navigate(NotesScreenDestination.route) }
                ) else null
                "dialpad" -> if (showDialpadTab) TabSpec(
                    key = key, route = DialPadScreenDestination.route, label = com.coolappstore.everdialer.by.svhp.controller.util.AppStrings.get("Dialpad", appLang),
                    selectedIcon = Icons.Filled.Dialpad, unselectedIcon = Icons.Outlined.Dialpad,
                    selected = isDialpadSelected,
                    onClick = {
                        doHaptic()
                        navigate(DialPadScreenDestination().route)
                    }
                ) else null
                else -> null
            }
        }
    }

    if (pillNav) {
        if (!isOnTabScreen && pillAlpha <= 0.005f && pillOffsetY >= pillExitOffsetY * 0.95f) return

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .offset { IntOffset(0, pillOffsetY.toInt()) }
                .graphicsLayer { alpha = pillAlpha },
            contentAlignment = Alignment.Center
        ) {
            val pillShape = RoundedCornerShape(percent = 50)

            val screenWidth = configuration.screenWidthDp.dp
            val horizontalGap = 24.dp
            val maxPillWidth = (screenWidth - (horizontalGap * 2)).coerceAtLeast(100.dp)

            val scrollState = rememberScrollState()

            // Pre-calculate likely scrollable state to eliminate 1-frame startup flicker
            val estimatedTabWidth = if (iconOnly) 56.dp else 72.dp
            val estimatedTotalWidth = (orderedTabs.size * estimatedTabWidth.value).dp + 24.dp + ((orderedTabs.size - 1).coerceAtLeast(0) * 4).dp
            val likelyCanScroll = estimatedTotalWidth > maxPillWidth

            var scrollStateMeasured by remember { mutableStateOf(false) }
            LaunchedEffect(scrollState.maxValue) {
                if (scrollState.maxValue > 0) {
                    scrollStateMeasured = true
                }
            }
            val canScroll = if (scrollStateMeasured) scrollState.maxValue > 0 else (likelyCanScroll || scrollState.maxValue > 0)

            val indicatorProgress by animateFloatAsState(
                targetValue   = if (pillVisible && canScroll && scrollIndication) 1f else 0f,
                animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                label         = "pillIndicatorProgress"
            )

            // When indicator shows: pill smoothly moves slightly UP by 10.dp (18.dp -> 28.dp above nav bar)
            // When indicator hides: pill smoothly settles slightly DOWN by 10.dp (back to 18.dp above nav bar)
            val currentBottomPadding = 18.dp - (8.dp * indicatorProgress)
            val indicatorSlotHeight = 18.dp * indicatorProgress

            val coroutineScope = rememberCoroutineScope()
            val pillWidthScale = remember { Animatable(1f) }
            var pillAnimJob by remember { mutableStateOf<Job?>(null) }

            fun triggerPillWidthAnimation() {
                pillAnimJob?.cancel()
                pillAnimJob = coroutineScope.launch {
                    pillWidthScale.animateTo(
                        targetValue = 1.07f,
                        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                    )
                    pillWidthScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = horizontalGap)
                    .padding(bottom = currentBottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                val childCoordinates = remember { mutableMapOf<String, LayoutCoordinates>() }
                val tabBounds = remember { mutableStateMapOf<String, Rect>() }
                var isSliding by remember { mutableStateOf(false) }
                var slideDragOffset by remember { mutableFloatStateOf(0f) }
                var dragStartViewportX by remember { mutableFloatStateOf(0f) }
                var hoveredTabKey by remember { mutableStateOf<String?>(null) }

                fun updateTabBounds(rowCoords: LayoutCoordinates?) {
                    val rc = rowCoords ?: return
                    if (!rc.isAttached) return
                    childCoordinates.forEach { (key, coords) ->
                        if (coords.isAttached) {
                            val pos = rc.localPositionOf(coords, Offset.Zero)
                            tabBounds[key] = Rect(
                                pos.x,
                                pos.y,
                                pos.x + coords.size.width,
                                pos.y + coords.size.height
                            )
                        }
                    }
                }

                LaunchedEffect(scrollState.value, rowCoordinates) {
                    updateTabBounds(rowCoordinates)
                }

                var lastSelectedKey by remember { mutableStateOf<String?>(null) }
                val currentSelectedTab = orderedTabs.firstOrNull { it.selected }
                LaunchedEffect(currentSelectedTab?.key) {
                    if (currentSelectedTab != null) {
                        lastSelectedKey = currentSelectedTab.key
                    }
                }
                val effectiveSelectedTab = currentSelectedTab ?: orderedTabs.firstOrNull { it.key == lastSelectedKey }
                val selectedTab = effectiveSelectedTab
                val selectedRect = selectedTab?.let { tabBounds[it.key] }
                val boundsReady = selectedRect != null

                val highlightPillProgress by animateFloatAsState(
                    targetValue = if (pillVisible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = if (pillVisible) 400 else 450,
                        easing = if (pillVisible) CubicBezierEasing(0.2f, 0f, 0f, 1f) else CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                    ),
                    label = "highlightPillProgress"
                )

                val sortedTabsWithBounds = remember(orderedTabs, tabBounds.toMap(), scrollState.value) {
                    orderedTabs.mapNotNull { tab ->
                        tabBounds[tab.key]?.let { rect ->
                            val contentRect = Rect(
                                rect.left + scrollState.value,
                                rect.top,
                                rect.right + scrollState.value,
                                rect.bottom
                            )
                            Pair(tab, contentRect)
                        }
                    }.sortedBy { it.second.left }
                }

                val viewportFingerX = dragStartViewportX + slideDragOffset
                val currentContentCenterX = viewportFingerX + scrollState.value
                val targetWidth = selectedRect?.width ?: 0f
                val targetTop = selectedRect?.top ?: 0f
                val targetHeight = selectedRect?.height ?: 0f

                var calcWidth = targetWidth
                var calcHeight = targetHeight
                var calcTop = targetTop
                var closestKey = selectedTab?.key

                if (sortedTabsWithBounds.isNotEmpty()) {
                    val first = sortedTabsWithBounds.first()
                    val last = sortedTabsWithBounds.last()

                    if (currentContentCenterX <= first.second.center.x) {
                        val overdrag = first.second.center.x - currentContentCenterX
                        calcWidth = first.second.width
                        calcHeight = first.second.height
                        calcTop = first.second.top
                        closestKey = first.first.key
                    } else if (currentContentCenterX >= last.second.center.x) {
                        val overdrag = currentContentCenterX - last.second.center.x
                        calcWidth = last.second.width
                        calcHeight = last.second.height
                        calcTop = last.second.top
                        closestKey = last.first.key
                    } else {
                        for (i in 0 until sortedTabsWithBounds.size - 1) {
                            val leftItem = sortedTabsWithBounds[i]
                            val rightItem = sortedTabsWithBounds[i + 1]
                            val cLeft = leftItem.second.center.x
                            val cRight = rightItem.second.center.x
                            if (currentContentCenterX in cLeft..cRight) {
                                val span = cRight - cLeft
                                val fraction = if (span > 0f) ((currentContentCenterX - cLeft) / span).coerceIn(0f, 1f) else 0f
                                calcWidth = leftItem.second.width + fraction * (rightItem.second.width - leftItem.second.width)
                                calcHeight = leftItem.second.height + fraction * (rightItem.second.height - leftItem.second.height)
                                calcTop = leftItem.second.top + fraction * (rightItem.second.top - leftItem.second.top)
                                closestKey = if (fraction < 0.5f) leftItem.first.key else rightItem.first.key
                                break
                            }
                        }
                    }
                }

                LaunchedEffect(closestKey, isSliding) {
                    if (isSliding && closestKey != null && closestKey != hoveredTabKey) {
                        hoveredTabKey = closestKey
                        doHaptic()
                    }
                }

                val localDensity = LocalDensity.current
                val currentFingerViewportX by rememberUpdatedState(dragStartViewportX + slideDragOffset)
                val currentCalcWidth by rememberUpdatedState(calcWidth)

                LaunchedEffect(isSliding) {
                    if (!isSliding) return@LaunchedEffect
                    val edgeThresholdPx = with(localDensity) { 64.dp.toPx() }
                    val edgeMarginPx = with(localDensity) { 24.dp.toPx() }
                    val baseScrollSpeed = with(localDensity) { 5.dp.toPx() }
                    val maxScrollSpeed = with(localDensity) { 7.5.dp.toPx() }

                    while (isActive && isSliding) {
                        val rowWidth = rowCoordinates?.size?.width?.toFloat() ?: 0f
                        if (rowWidth > 0f && scrollState.maxValue > 0) {
                            val fingerX = currentFingerViewportX
                            val pillW = currentCalcWidth
                            val pillLeft = fingerX - (pillW / 2f)
                            val pillRight = fingerX + (pillW / 2f)

                            val scrollDelta: Float = when {
                                (fingerX > rowWidth - edgeThresholdPx || pillRight > rowWidth - edgeMarginPx) -> {
                                    val overDist = (fingerX - (rowWidth - edgeThresholdPx)).coerceAtLeast(0f)
                                    val factor = (overDist / edgeThresholdPx).coerceIn(0f, 1f)
                                    val speed = baseScrollSpeed + factor * (maxScrollSpeed - baseScrollSpeed)
                                    val canScroll = (scrollState.maxValue - scrollState.value).toFloat()
                                    if (canScroll > 0f) speed.coerceAtMost(canScroll) else 0f
                                }
                                (fingerX < edgeThresholdPx || pillLeft < edgeMarginPx) -> {
                                    val overDist = (edgeThresholdPx - fingerX).coerceAtLeast(0f)
                                    val factor = (overDist / edgeThresholdPx).coerceIn(0f, 1f)
                                    val speed = baseScrollSpeed + factor * (maxScrollSpeed - baseScrollSpeed)
                                    val canScroll = scrollState.value.toFloat()
                                    if (canScroll > 0f) -speed.coerceAtMost(canScroll) else 0f
                                }
                                else -> 0f
                            }

                            if (scrollDelta != 0f) {
                                scrollState.scrollBy(scrollDelta)
                                updateTabBounds(rowCoordinates)
                            }
                        }
                        delay(16)
                    }
                }

                val animPillWidth by animateFloatAsState(
                    targetValue = if (isSliding) calcWidth else targetWidth,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    ),
                    label = "slidingPillWidth"
                )

                val animPillLeft by animateFloatAsState(
                    targetValue = if (isSliding) (viewportFingerX - (calcWidth / 2f)) else (selectedRect?.left ?: 0f),
                    animationSpec = if (isSliding) {
                        spring(stiffness = Spring.StiffnessHigh)
                    } else {
                        spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
                    },
                    label = "slidingPillLeft"
                )

                val isExpressive = com.coolappstore.everdialer.by.svhp.view.theme.isMaterialExpressive()
                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                val isSaturatedActive = remember(settingsState, isDark) { prefs.isSaturatedForTheme(isDark) }
                val isDynamic = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                val usePrimary = isSaturatedActive || !isDynamic
                val activeNavBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

                val pillContent: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { rowCoords ->
                                rowCoordinates = rowCoords
                                updateTabBounds(rowCoords)
                            }
                            .drawBehind {
                                if (slideToOpen && boundsReady && selectedRect != null && animPillWidth > 0f && highlightPillProgress > 0.005f) {
                                    val cornerRadiusPx = if (isExpressive) 18.dp.toPx() else (calcHeight / 2f)
                                    val scale = 0.85f + 0.15f * highlightPillProgress
                                    val scaledW = animPillWidth * scale
                                    val currentH = if (isSliding) calcHeight else targetHeight
                                    val scaledH = currentH * scale
                                    val currentTop = if (isSliding) calcTop else targetTop
                                    val leftOffset = animPillLeft + (animPillWidth - scaledW) / 2f
                                    val topOffset = currentTop + (currentH - scaledH) / 2f
                                    drawRoundRect(
                                        color = activeNavBg.copy(alpha = activeNavBg.alpha * highlightPillProgress),
                                        topLeft = Offset(leftOffset, topOffset),
                                        size = Size(scaledW, scaledH),
                                        cornerRadius = CornerRadius(cornerRadiusPx * scale, cornerRadiusPx * scale)
                                    )
                                }
                            }
                            .horizontalScroll(scrollState, enabled = !isSliding)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        orderedTabs.forEach { tab ->
                            key(tab.key) {
                                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                                LaunchedEffect(tab.selected) {
                                    if (tab.selected && !isSliding) {
                                        try {
                                            bringIntoViewRequester.bringIntoView()
                                        } catch (_: Throwable) {}
                                    }
                                }
                                PillNavItem(
                                    modifier       = Modifier
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .onGloballyPositioned { coords ->
                                            childCoordinates[tab.key] = coords
                                            rowCoordinates?.let { parentCoords ->
                                                if (coords.isAttached && parentCoords.isAttached) {
                                                    val pos = parentCoords.localPositionOf(coords, Offset.Zero)
                                                    tabBounds[tab.key] = Rect(
                                                        pos.x,
                                                        pos.y,
                                                        pos.x + coords.size.width,
                                                        pos.y + coords.size.height
                                                    )
                                                }
                                            }
                                        },
                                    selected       = tab.selected,
                                    selectedIcon   = tab.selectedIcon,
                                    unselectedIcon = tab.unselectedIcon,
                                    label          = tab.label,
                                    iconOnly       = iconOnly,
                                    isHovered      = isSliding && (hoveredTabKey == tab.key),
                                    isSliding      = isSliding,
                                    slideToOpen    = slideToOpen,
                                    boundsReady    = boundsReady,
                                    onSlideDragStart = {
                                        isSliding = true
                                        slideDragOffset = 0f
                                        dragStartViewportX = selectedRect?.center?.x ?: 0f
                                        hoveredTabKey = tab.key
                                    },
                                    onSlideDrag    = { delta ->
                                        slideDragOffset += delta
                                    },
                                    onSlideDragEnd = {
                                        if (isSliding) {
                                            val targetKey = hoveredTabKey ?: closestKey
                                            val targetTab = orderedTabs.firstOrNull { it.key == targetKey }
                                            if (targetTab != null && !targetTab.selected) {
                                                triggerPillWidthAnimation()
                                                targetTab.onClick()
                                            }
                                        }
                                        isSliding = false
                                        slideDragOffset = 0f
                                        hoveredTabKey = null
                                    },
                                    onSlideDragCancel = {
                                        isSliding = false
                                        slideDragOffset = 0f
                                        hoveredTabKey = null
                                    },
                                    onClick        = {
                                        triggerPillWidthAnimation()
                                        tab.onClick()
                                    },
                                    onLongClick    = {
                                        doHaptic()
                                        showTabSectionsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                val mainPillModifier = Modifier
                    .graphicsLayer {
                        scaleX = pillWidthScale.value
                    }
                    .animateContentSize(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                    .widthIn(max = maxPillWidth)

                if (useLgBottomNav && globalBackdrop != null) {
                    Surface(
                        shape           = pillShape,
                        color           = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                        shadowElevation = 0.dp,
                        tonalElevation  = 0.dp,
                        modifier        = mainPillModifier.drawBackdrop(
                            backdrop = globalBackdrop,
                            shape    = { pillShape },
                            effects  = {
                                val d = density
                                colorControls(brightness = lgBottomNavConfig.brightness, saturation = lgBottomNavConfig.saturation)
                                if (lgBottomNavConfig.blurRadiusDp > 0f) blur(lgBottomNavConfig.blurRadiusDp * d)
                                lens(
                                    refractionHeight = lgBottomNavConfig.refractionHeightDp * d,
                                    refractionAmount = lgBottomNavConfig.refractionAmountDp * d,
                                    chromaticAberration = lgBottomNavConfig.chromaticAberration,
                                    depthEffect = lgBottomNavConfig.depthEffect
                                )
                            },
                            highlight = { Highlight.Default },
                            shadow    = null
                        )
                    ) { pillContent() }
                } else if (useBlurBottomNav && globalBackdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Box(
                        modifier = mainPillModifier.drawBackdrop(
                            backdrop = globalBackdrop,
                            shape = { pillShape },
                            effects = {
                                val d = density
                                colorControls(saturation = blurBottomNavConfig.saturation)
                                blur(blurBottomNavConfig.blurRadiusDp * d)
                            },
                            highlight = { Highlight.Default },
                            shadow = null
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.25f), pillShape)
                        )
                        pillContent()
                    }
                } else {
                    Surface(
                        shape           = pillShape,
                        color           = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 0.dp,
                        tonalElevation  = 2.dp,
                        modifier        = mainPillModifier
                    ) { pillContent() }
                }

                Box(
                    modifier = Modifier
                        .height(indicatorSlotHeight)
                        .graphicsLayer {
                            alpha = indicatorProgress.coerceIn(0f, 1f)
                            scaleX = 0.85f + (0.15f * indicatorProgress)
                            scaleY = 0.85f + (0.15f * indicatorProgress)
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (indicatorProgress > 0.01f) {
                        ScrollIndicatorPill(
                            scrollState = scrollState,
                            useLg       = useLgBottomNav,
                            useBlur     = useBlurBottomNav
                        )
                    }
                }
            }
        }
    } else {
        val localDensity = LocalDensity.current
        var barHeightPx by remember { mutableFloatStateOf(0f) }
        val fallbackOffsetPx = with(localDensity) { 160.dp.toPx() }
        val maxOffsetPx = if (barHeightPx > 0f) barHeightPx else fallbackOffsetPx

        val navBarOffsetY by animateFloatAsState(
            targetValue   = if (isOnTabScreen) 0f else maxOffsetPx,
            animationSpec = tween(
                durationMillis = if (isOnTabScreen) 420 else 700,
                easing         = if (isOnTabScreen) CubicBezierEasing(0.2f, 0f, 0f, 1f) else CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
            ),
            label         = "navBarOffsetY"
        )
        val navBarAlpha by animateFloatAsState(
            targetValue   = if (isOnTabScreen) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isOnTabScreen) 350 else 650,
                easing         = if (isOnTabScreen) LinearEasing else CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            ),
            label         = "navBarAlpha"
        )
        if (!isOnTabScreen && navBarAlpha <= 0.005f && navBarOffsetY >= maxOffsetPx * 0.95f) return

        val standardScrollState = rememberScrollState()
        val screenWidth = configuration.screenWidthDp.dp
        val minItemWidth = if (iconOnly) 64.dp else 80.dp
        val naturalItemWidth = screenWidth / orderedTabs.size.coerceAtLeast(1)
        val likelyCanStandardScroll = (orderedTabs.size * minItemWidth.value).dp > screenWidth
        var standardScrollMeasured by remember { mutableStateOf(false) }
        LaunchedEffect(standardScrollState.maxValue) {
            if (standardScrollState.maxValue > 0) {
                standardScrollMeasured = true
            }
        }
        val canStandardScroll = if (standardScrollMeasured) standardScrollState.maxValue > 0 else (likelyCanStandardScroll || standardScrollState.maxValue > 0)
        val itemWidth = if (canStandardScroll) minItemWidth else naturalItemWidth.coerceAtLeast(minItemWidth)

        val standardIndicatorProgress by animateFloatAsState(
            targetValue   = if (isOnTabScreen && canStandardScroll && scrollIndication) 1f else 0f,
            animationSpec = tween(durationMillis = 320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            label         = "standardIndicatorProgress"
        )

        val standardDensity = LocalDensity.current
        var standardRowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val standardChildCoordinates = remember { mutableMapOf<String, LayoutCoordinates>() }
        val standardTabBounds = remember { mutableStateMapOf<String, Rect>() }
        var standardIsSliding by remember { mutableStateOf(false) }
        var standardSlideDragOffset by remember { mutableFloatStateOf(0f) }
        var standardDragStartViewportX by remember { mutableFloatStateOf(0f) }
        var standardHoveredTabKey by remember { mutableStateOf<String?>(null) }

        fun updateStandardTabBounds(rowCoords: LayoutCoordinates?) {
            val rc = rowCoords ?: return
            if (!rc.isAttached) return
            standardChildCoordinates.forEach { (key, coords) ->
                if (coords.isAttached) {
                    val pos = rc.localPositionOf(coords, Offset.Zero)
                    standardTabBounds[key] = Rect(
                        pos.x,
                        pos.y,
                        pos.x + coords.size.width,
                        pos.y + coords.size.height
                    )
                }
            }
        }

        LaunchedEffect(standardScrollState.value, standardRowCoordinates) {
            updateStandardTabBounds(standardRowCoordinates)
        }

        var standardLastSelectedKey by remember { mutableStateOf<String?>(null) }
        val standardCurrentSelectedTab = orderedTabs.firstOrNull { it.selected }
        LaunchedEffect(standardCurrentSelectedTab?.key) {
            if (standardCurrentSelectedTab != null) {
                standardLastSelectedKey = standardCurrentSelectedTab.key
            }
        }
        val standardEffectiveSelectedTab = standardCurrentSelectedTab ?: orderedTabs.firstOrNull { it.key == standardLastSelectedKey }
        val standardSelectedTab = standardEffectiveSelectedTab
        val standardSelectedRect = standardSelectedTab?.let { standardTabBounds[it.key] }
        val standardBoundsReady = standardSelectedRect != null

        val standardHighlightPillProgress by animateFloatAsState(
            targetValue = if (isOnTabScreen) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isOnTabScreen) 400 else 450,
                easing = if (isOnTabScreen) CubicBezierEasing(0.2f, 0f, 0f, 1f) else CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            ),
            label = "standardHighlightPillProgress"
        )

        val standardSortedTabs = remember(orderedTabs, standardTabBounds.toMap(), standardScrollState.value) {
            orderedTabs.mapNotNull { tab ->
                standardTabBounds[tab.key]?.let { rect ->
                    val contentRect = Rect(
                        rect.left + standardScrollState.value,
                        rect.top,
                        rect.right + standardScrollState.value,
                        rect.bottom
                    )
                    Pair(tab, contentRect)
                }
            }.sortedBy { it.second.left }
        }

        val standardViewportFingerX = standardDragStartViewportX + standardSlideDragOffset
        val standardContentCenterX = standardViewportFingerX + standardScrollState.value
        val standardTargetWidth = standardSelectedRect?.width ?: 0f
        val standardTargetTop = standardSelectedRect?.top ?: 0f
        val standardTargetHeight = standardSelectedRect?.height ?: 0f

        var standardCalcWidth = standardTargetWidth
        var standardCalcHeight = standardTargetHeight
        var standardCalcTop = standardTargetTop
        var standardClosestKey = standardSelectedTab?.key

        if (standardSortedTabs.isNotEmpty()) {
            val first = standardSortedTabs.first()
            val last = standardSortedTabs.last()

            if (standardContentCenterX <= first.second.center.x) {
                val overdrag = first.second.center.x - standardContentCenterX
                standardCalcWidth = first.second.width
                standardCalcHeight = first.second.height
                standardCalcTop = first.second.top
                standardClosestKey = first.first.key
            } else if (standardContentCenterX >= last.second.center.x) {
                val overdrag = standardContentCenterX - last.second.center.x
                standardCalcWidth = last.second.width
                standardCalcHeight = last.second.height
                standardCalcTop = last.second.top
                standardClosestKey = last.first.key
            } else {
                for (i in 0 until standardSortedTabs.size - 1) {
                    val leftItem = standardSortedTabs[i]
                    val rightItem = standardSortedTabs[i + 1]
                    val cLeft = leftItem.second.center.x
                    val cRight = rightItem.second.center.x
                    if (standardContentCenterX in cLeft..cRight) {
                        val span = cRight - cLeft
                        val fraction = if (span > 0f) ((standardContentCenterX - cLeft) / span).coerceIn(0f, 1f) else 0f
                        standardCalcWidth = leftItem.second.width + fraction * (rightItem.second.width - leftItem.second.width)
                        standardCalcHeight = leftItem.second.height + fraction * (rightItem.second.height - leftItem.second.height)
                        standardCalcTop = leftItem.second.top + fraction * (rightItem.second.top - leftItem.second.top)
                        standardClosestKey = if (fraction < 0.5f) leftItem.first.key else rightItem.first.key
                        break
                    }
                }
            }
        }

        LaunchedEffect(standardClosestKey, standardIsSliding) {
            if (standardIsSliding && standardClosestKey != null && standardClosestKey != standardHoveredTabKey) {
                standardHoveredTabKey = standardClosestKey
                doHaptic()
            }
        }

        val standardLiveFingerViewportX by rememberUpdatedState(standardDragStartViewportX + standardSlideDragOffset)
        val standardLiveCalcWidth by rememberUpdatedState(standardCalcWidth)

        LaunchedEffect(standardIsSliding) {
            if (!standardIsSliding) return@LaunchedEffect
            val edgeThresholdPx = with(standardDensity) { 64.dp.toPx() }
            val edgeMarginPx = with(standardDensity) { 24.dp.toPx() }
            val baseScrollSpeed = with(standardDensity) { 5.dp.toPx() }
            val maxScrollSpeed = with(standardDensity) { 7.5.dp.toPx() }

            while (isActive && standardIsSliding) {
                val rowWidth = standardRowCoordinates?.size?.width?.toFloat() ?: 0f
                if (rowWidth > 0f && standardScrollState.maxValue > 0) {
                    val fingerX = standardLiveFingerViewportX
                    val pillW = standardLiveCalcWidth
                    val pillLeft = fingerX - (pillW / 2f)
                    val pillRight = fingerX + (pillW / 2f)

                    val scrollDelta: Float = when {
                        (fingerX > rowWidth - edgeThresholdPx || pillRight > rowWidth - edgeMarginPx) -> {
                            val overDist = (fingerX - (rowWidth - edgeThresholdPx)).coerceAtLeast(0f)
                            val factor = (overDist / edgeThresholdPx).coerceIn(0f, 1f)
                            val speed = baseScrollSpeed + factor * (maxScrollSpeed - baseScrollSpeed)
                            val canScroll = (standardScrollState.maxValue - standardScrollState.value).toFloat()
                            if (canScroll > 0f) speed.coerceAtMost(canScroll) else 0f
                        }
                        (fingerX < edgeThresholdPx || pillLeft < edgeMarginPx) -> {
                            val overDist = (edgeThresholdPx - fingerX).coerceAtLeast(0f)
                            val factor = (overDist / edgeThresholdPx).coerceIn(0f, 1f)
                            val speed = baseScrollSpeed + factor * (maxScrollSpeed - baseScrollSpeed)
                            val canScroll = standardScrollState.value.toFloat()
                            if (canScroll > 0f) -speed.coerceAtMost(canScroll) else 0f
                        }
                        else -> 0f
                    }

                    if (scrollDelta != 0f) {
                        standardScrollState.scrollBy(scrollDelta)
                        updateStandardTabBounds(standardRowCoordinates)
                    }
                }
                delay(16)
            }
        }

        val standardAnimPillWidth by animateFloatAsState(
            targetValue = if (standardIsSliding) standardCalcWidth else standardTargetWidth,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            label = "standardSlidingPillWidth"
        )

        val standardAnimPillLeft by animateFloatAsState(
            targetValue = if (standardIsSliding) (standardViewportFingerX - (standardCalcWidth / 2f)) else (standardSelectedRect?.left ?: 0f),
            animationSpec = if (standardIsSliding) {
                spring(stiffness = Spring.StiffnessHigh)
            } else {
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
            },
            label = "standardSlidingPillLeft"
        )

        val isStandardExpressive = com.coolappstore.everdialer.by.svhp.view.theme.isMaterialExpressive()
        val isStandardDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
        val isStandardSaturatedActive = remember(settingsState, isStandardDark) { prefs.isSaturatedForTheme(isStandardDark) }
        val isStandardDynamic = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
        val useStandardPrimary = isStandardSaturatedActive || !isStandardDynamic
        val standardActiveNavBg = if (useStandardPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .offset { IntOffset(0, navBarOffsetY.toInt()) }
                .graphicsLayer { alpha = navBarAlpha },
            contentAlignment = Alignment.BottomCenter
        ) {
            val standardBarModifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .onSizeChanged { size ->
                    if (size.height > 0) {
                        barHeightPx = size.height.toFloat()
                    }
                }

            val standardBarContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { rowCoords ->
                                standardRowCoordinates = rowCoords
                                updateStandardTabBounds(rowCoords)
                            }
                            .drawBehind {
                                if (slideToOpen && standardBoundsReady && standardSelectedRect != null && standardAnimPillWidth > 0f && standardHighlightPillProgress > 0.005f) {
                                    val cornerRadiusPx = if (isStandardExpressive) 16.dp.toPx() else (standardCalcHeight / 2f)
                                    val scale = 0.85f + 0.15f * standardHighlightPillProgress
                                    val scaledW = standardAnimPillWidth * scale
                                    val currentH = if (standardIsSliding) standardCalcHeight else standardTargetHeight
                                    val scaledH = currentH * scale
                                    val currentTop = if (standardIsSliding) standardCalcTop else standardTargetTop
                                    val leftOffset = standardAnimPillLeft + (standardAnimPillWidth - scaledW) / 2f
                                    val topOffset = currentTop + (currentH - scaledH) / 2f
                                    drawRoundRect(
                                        color = standardActiveNavBg.copy(alpha = standardActiveNavBg.alpha * standardHighlightPillProgress),
                                        topLeft = Offset(leftOffset, topOffset),
                                        size = Size(scaledW, scaledH),
                                        cornerRadius = CornerRadius(cornerRadiusPx * scale, cornerRadiusPx * scale)
                                    )
                                }
                            }
                            .horizontalScroll(standardScrollState, enabled = !standardIsSliding)
                            .padding(horizontal = if (canStandardScroll) 12.dp else 0.dp),
                        horizontalArrangement = if (canStandardScroll) Arrangement.spacedBy(4.dp) else Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        orderedTabs.forEach { tab ->
                            key(tab.key) {
                                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                                LaunchedEffect(tab.selected) {
                                    if (tab.selected && !standardIsSliding) {
                                        try {
                                            bringIntoViewRequester.bringIntoView()
                                        } catch (_: Throwable) {}
                                    }
                                }
                                AnimatedNavBarItem(
                                    selected       = tab.selected,
                                    selectedIcon   = tab.selectedIcon,
                                    unselectedIcon = tab.unselectedIcon,
                                    label          = tab.label,
                                    iconOnly       = iconOnly,
                                    labelStyle     = labelStyle,
                                    modifier       = Modifier
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .width(itemWidth),
                                    isHovered      = standardIsSliding && (standardHoveredTabKey == tab.key),
                                    isSliding      = standardIsSliding,
                                    slideToOpen    = slideToOpen,
                                    boundsReady    = standardBoundsReady,
                                    onSlideDragStart = {
                                        standardIsSliding = true
                                        standardSlideDragOffset = 0f
                                        standardDragStartViewportX = standardSelectedRect?.center?.x ?: 0f
                                        standardHoveredTabKey = tab.key
                                    },
                                    onSlideDrag    = { delta ->
                                        standardSlideDragOffset += delta
                                    },
                                    onSlideDragEnd = {
                                        if (standardIsSliding) {
                                            val targetKey = standardHoveredTabKey ?: standardClosestKey
                                            val targetTab = orderedTabs.firstOrNull { it.key == targetKey }
                                            if (targetTab != null && !targetTab.selected) {
                                                targetTab.onClick()
                                            }
                                        }
                                        standardIsSliding = false
                                        standardSlideDragOffset = 0f
                                        standardHoveredTabKey = null
                                    },
                                    onSlideDragCancel = {
                                        standardIsSliding = false
                                        standardSlideDragOffset = 0f
                                        standardHoveredTabKey = null
                                    },
                                    onIndicatorPositioned = { coords ->
                                        standardChildCoordinates[tab.key] = coords
                                        standardRowCoordinates?.let { parentCoords ->
                                            if (coords.isAttached && parentCoords.isAttached) {
                                                val pos = parentCoords.localPositionOf(coords, Offset.Zero)
                                                standardTabBounds[tab.key] = Rect(
                                                    pos.x,
                                                    pos.y,
                                                    pos.x + coords.size.width,
                                                    pos.y + coords.size.height
                                                )
                                            }
                                        }
                                    },
                                    onClick        = tab.onClick,
                                    onLongClick    = {
                                        doHaptic()
                                        showTabSectionsDialog = true
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp * standardIndicatorProgress)
                            .graphicsLayer {
                                alpha = standardIndicatorProgress.coerceIn(0f, 1f)
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (standardIndicatorProgress > 0.01f) {
                            ScrollIndicatorPill(
                                scrollState = standardScrollState,
                                useLg       = useLgBottomNav,
                                useBlur     = useBlurBottomNav
                            )
                        }
                    }
                }
            }

            val standardBarShape = RoundedCornerShape(0.dp)

            if (useLgBottomNav && globalBackdrop != null) {
                Surface(
                    shape           = standardBarShape,
                    color           = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                    shadowElevation = 0.dp,
                    tonalElevation  = 0.dp,
                    modifier        = standardBarModifier.drawBackdrop(
                        backdrop = globalBackdrop,
                        shape    = { standardBarShape },
                        effects  = {
                            val d = density
                            colorControls(brightness = lgBottomNavConfig.brightness, saturation = lgBottomNavConfig.saturation)
                            if (lgBottomNavConfig.blurRadiusDp > 0f) blur(lgBottomNavConfig.blurRadiusDp * d)
                            lens(
                                refractionHeight = lgBottomNavConfig.refractionHeightDp * d,
                                refractionAmount = lgBottomNavConfig.refractionAmountDp * d,
                                chromaticAberration = lgBottomNavConfig.chromaticAberration,
                                depthEffect = lgBottomNavConfig.depthEffect
                            )
                        },
                        highlight = { Highlight.Default },
                        shadow    = null
                    )
                ) { standardBarContent() }
            } else if (useBlurBottomNav && globalBackdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = standardBarModifier.drawBackdrop(
                        backdrop = globalBackdrop,
                        shape    = { standardBarShape },
                        effects  = {
                            val d = density
                            colorControls(saturation = blurBottomNavConfig.saturation)
                            blur(blurBottomNavConfig.blurRadiusDp * d)
                        },
                        highlight = { Highlight.Default },
                        shadow    = null
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f))
                    )
                    standardBarContent()
                }
            } else {
                Surface(
                    color          = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier       = standardBarModifier
                ) {
                    standardBarContent()
                }
            }
        }
    }

    if (showTabSectionsDialog) {
        TabSectionsDialog(onDismissRequest = { showTabSectionsDialog = false })
    }
}

// ── Animated standard nav bar item ────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedNavBarItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    iconOnly: Boolean,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    isSliding: Boolean = false,
    slideToOpen: Boolean = false,
    boundsReady: Boolean = false,
    onSlideDragStart: () -> Unit = {},
    onSlideDrag: (Float) -> Unit = {},
    onSlideDragEnd: () -> Unit = {},
    onSlideDragCancel: () -> Unit = {},
    onIndicatorPositioned: (LayoutCoordinates) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnSlideDragStart by rememberUpdatedState(onSlideDragStart)
    val currentOnSlideDrag by rememberUpdatedState(onSlideDrag)
    val currentOnSlideDragEnd by rememberUpdatedState(onSlideDragEnd)
    val currentOnSlideDragCancel by rememberUpdatedState(onSlideDragCancel)
    val coroutineScope = rememberCoroutineScope()

    val iconSize by animateDpAsState(
        targetValue   = if (selected || (isSliding && isHovered)) 26.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "${label}Size"
    )
    val scale by animateFloatAsState(
        targetValue   = when {
            isPressed -> 0.85f
            selected  -> 1.05f
            else      -> 1f
        },
        animationSpec = if (isPressed)
            tween(durationMillis = 80, easing = FastOutSlowInEasing)
        else if (selected)
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        else
            tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label         = "${label}Scale"
    )

    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
    val usePrimary = isSaturatedActive || !isDynamic
    val activeNavBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val activeNavFg = if (isSaturatedActive && isDark) Color.Black else if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

    val hasCustomHighlight = slideToOpen && boundsReady
    val isCoveredByPill = if (slideToOpen) {
        if (isSliding) isHovered else selected
    } else {
        selected
    }

    val indicatorAlpha by animateFloatAsState(
        targetValue   = if (hasCustomHighlight) 0f else if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label         = "${label}IndicatorAlpha"
    )

    val iconTint by animateColorAsState(
        targetValue   = if (isCoveredByPill) activeNavFg else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label         = "${label}IconTint"
    )

    val isExpressive = com.coolappstore.everdialer.by.svhp.view.theme.isMaterialExpressive()
    val indicatorShape = if (isExpressive && (selected || isHovered)) {
        RoundedCornerShape(16.dp)
    } else {
        RoundedCornerShape(50.dp)
    }

    val gestureModifier = if (slideToOpen && selected) {
        Modifier.pointerInput(Unit) {
            val touchSlop = viewConfiguration.touchSlop
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var isDragging = false
                var longClickFired = false
                var pressInteraction: PressInteraction.Press? = null

                val pressJob = coroutineScope.launch {
                    val press = PressInteraction.Press(down.position)
                    pressInteraction = press
                    interactionSource.emit(press)
                }

                val longPressJob = coroutineScope.launch {
                    delay(longPressTimeout)
                    if (!isDragging) {
                        longClickFired = true
                        currentOnLongClick()
                    }
                }

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.changedToUp()) {
                            longPressJob.cancel()
                            if (isDragging) {
                                change.consume()
                                currentOnSlideDragEnd()
                            } else if (!longClickFired) {
                                change.consume()
                                currentOnClick()
                            }
                            break
                        }

                        if (!change.pressed) {
                            longPressJob.cancel()
                            if (isDragging) currentOnSlideDragCancel()
                            break
                        }

                        val totalDeltaX = change.position.x - down.position.x
                        val totalDeltaY = change.position.y - down.position.y

                        if (!isDragging) {
                            if (kotlin.math.abs(totalDeltaX) > touchSlop && kotlin.math.abs(totalDeltaX) > kotlin.math.abs(totalDeltaY)) {
                                isDragging = true
                                longPressJob.cancel()
                                coroutineScope.launch {
                                    pressInteraction?.let {
                                        interactionSource.emit(PressInteraction.Cancel(it))
                                    }
                                }
                                change.consume()
                                currentOnSlideDragStart()
                                currentOnSlideDrag(totalDeltaX)
                            } else if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                                longPressJob.cancel()
                            }
                        } else {
                            val delta = change.positionChange().x
                            change.consume()
                            currentOnSlideDrag(delta)
                        }
                    }
                } finally {
                    longPressJob.cancel()
                    coroutineScope.launch {
                        pressInteraction?.let {
                            interactionSource.emit(PressInteraction.Release(it))
                        }
                    }
                }
            }
        }
    } else {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .then(gestureModifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { onIndicatorPositioned(it) }
                .clip(indicatorShape)
                .background(activeNavBg.copy(alpha = indicatorAlpha))
                .padding(horizontal = 16.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState   = selected,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label         = "${label}IconCrossfade"
            ) { sel ->
                Icon(
                    imageVector        = if (sel) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    modifier           = Modifier.size(iconSize),
                    tint               = iconTint
                )
            }
        }
        if (!iconOnly) {
            Spacer(Modifier.height(3.dp))
            Text(
                text     = label,
                style    = labelStyle,
                color    = if (selected || (isSliding && isHovered)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ── Pill nav item ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PillNavItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    iconOnly: Boolean,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    isSliding: Boolean = false,
    slideToOpen: Boolean = false,
    boundsReady: Boolean = false,
    onSlideDragStart: () -> Unit = {},
    onSlideDrag: (Float) -> Unit = {},
    onSlideDragEnd: () -> Unit = {},
    onSlideDragCancel: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
    val usePrimary = isSaturatedActive || !isDynamic
    val activeNavBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val activeNavFg = if (isSaturatedActive && isDark) Color.Black else if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val hasCustomHighlight = slideToOpen && boundsReady
    val isCoveredByPill = if (slideToOpen) {
        if (isSliding) isHovered else selected
    } else {
        selected
    }

    val bgAlpha by animateFloatAsState(
        targetValue   = if (hasCustomHighlight) 0f else if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label         = "${label}BgAlpha"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (isCoveredByPill) activeNavFg
                        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label         = "${label}IconTint"
    )
    // Tap-press squish → spring back bounce
    val scale by animateFloatAsState(
        targetValue   = when {
            isPressed -> 0.82f
            isHovered -> 1.08f
            selected  -> 1.10f
            else      -> 1f
        },
        animationSpec = if (isPressed)
            tween(durationMillis = 80, easing = FastOutSlowInEasing)
        else if (selected || isHovered)
            spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        else
            tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "${label}Scale"
    )

    val isExpressive = com.coolappstore.everdialer.by.svhp.view.theme.isMaterialExpressive()
    val navShape = if (isExpressive && (selected || isHovered)) {
        RoundedCornerShape(18.dp)
    } else {
        RoundedCornerShape(50.dp)
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnSlideDragStart by rememberUpdatedState(onSlideDragStart)
    val currentOnSlideDrag by rememberUpdatedState(onSlideDrag)
    val currentOnSlideDragEnd by rememberUpdatedState(onSlideDragEnd)
    val currentOnSlideDragCancel by rememberUpdatedState(onSlideDragCancel)
    val coroutineScope = rememberCoroutineScope()

    val gestureModifier = if (slideToOpen && selected) {
        Modifier.pointerInput(Unit) {
            val touchSlop = viewConfiguration.touchSlop
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var isDragging = false
                var longClickFired = false
                var pressInteraction: PressInteraction.Press? = null

                val pressJob = coroutineScope.launch {
                    val press = PressInteraction.Press(down.position)
                    pressInteraction = press
                    interactionSource.emit(press)
                }

                val longPressJob = coroutineScope.launch {
                    delay(longPressTimeout)
                    if (!isDragging) {
                        longClickFired = true
                        currentOnLongClick()
                    }
                }

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.changedToUp()) {
                            longPressJob.cancel()
                            if (isDragging) {
                                change.consume()
                                currentOnSlideDragEnd()
                            } else if (!longClickFired) {
                                change.consume()
                                currentOnClick()
                            }
                            break
                        }

                        if (!change.pressed) {
                            longPressJob.cancel()
                            if (isDragging) currentOnSlideDragCancel()
                            break
                        }

                        val totalDeltaX = change.position.x - down.position.x
                        val totalDeltaY = change.position.y - down.position.y

                        if (!isDragging) {
                            if (kotlin.math.abs(totalDeltaX) > touchSlop && kotlin.math.abs(totalDeltaX) > kotlin.math.abs(totalDeltaY)) {
                                isDragging = true
                                longPressJob.cancel()
                                coroutineScope.launch {
                                    pressInteraction?.let {
                                        interactionSource.emit(PressInteraction.Cancel(it))
                                    }
                                }
                                change.consume()
                                currentOnSlideDragStart()
                                currentOnSlideDrag(totalDeltaX)
                            } else if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                                longPressJob.cancel()
                            }
                        } else {
                            val delta = change.positionChange().x
                            change.consume()
                            currentOnSlideDrag(delta)
                        }
                    }
                } finally {
                    longPressJob.cancel()
                    coroutineScope.launch {
                        pressInteraction?.let {
                            interactionSource.emit(PressInteraction.Release(it))
                        }
                    }
                }
            }
        }
    } else {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(navShape)
            .background(activeNavBg.copy(alpha = bgAlpha))
            .then(gestureModifier)
            .padding(horizontal = if (iconOnly) 16.dp else 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (iconOnly) {
            Crossfade(
                targetState   = selected,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label         = "${label}IconCrossfade"
            ) { sel ->
                Icon(
                    imageVector        = if (sel) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    modifier           = Modifier.size(24.dp),
                    tint               = iconTint
                )
            }
        } else {
            Row(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        stiffness    = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Crossfade(
                    targetState   = selected,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label         = "${label}IconCrossfade"
                ) { sel ->
                    Icon(
                        imageVector        = if (sel) selectedIcon else unselectedIcon,
                        contentDescription = label,
                        modifier           = Modifier.size(24.dp),
                        tint               = iconTint
                    )
                }
                AnimatedVisibility(
                    visible = selected,
                    enter = expandHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                        expandFrom = Alignment.Start
                    ) + fadeIn(tween(durationMillis = 350)),
                    exit = shrinkHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                        shrinkTowards = Alignment.Start
                    ) + fadeOut(tween(durationMillis = 250))
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCoveredByPill) activeNavFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ── Floating pill with horizontal line indicator ───────────────────────────────

@Composable
private fun ScrollIndicatorPill(
    scrollState: ScrollState,
    useLg: Boolean,
    useBlur: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val primaryColor = MaterialTheme.colorScheme.primary

    // Bright primary color in dark mode, dark primary color in light mode for high visibility
    val indicatorColor = remember(primaryColor, isDark) {
        val argb = primaryColor.toArgb()
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(argb, hsl)
        if (isDark) {
            hsl[2] = hsl[2].coerceAtLeast(0.72f)
        } else {
            hsl[2] = hsl[2].coerceAtMost(0.38f)
        }
        Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }

    val trackColor = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }

    val indicatorPillShape = RoundedCornerShape(percent = 50)
    val trackWidth = 44.dp
    val trackHeight = 3.5.dp

    val viewport = scrollState.viewportSize.toFloat()
    val total = (scrollState.maxValue + scrollState.viewportSize).toFloat()
    val visibleRatio = if (total > 0f) (viewport / total).coerceIn(0.2f, 0.8f) else 0.5f
    val thumbWidth = trackWidth * visibleRatio
    val maxTravel = trackWidth - thumbWidth
    val scrollFraction = if (scrollState.maxValue > 0) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val thumbOffset = maxTravel * scrollFraction
    val animatedOffset by animateDpAsState(
        targetValue   = thumbOffset,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "indicatorThumbOffset"
    )

    Surface(
        shape           = indicatorPillShape,
        color           = if (useLg) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                          else if (useBlur) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                          else MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = if (useLg || useBlur) 0.dp else 4.dp,
        tonalElevation  = 2.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track in pill style
            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .height(trackHeight)
                    .clip(indicatorPillShape)
                    .background(trackColor)
                    .pointerInput(scrollState.maxValue) {
                        detectTapGestures { tapOffset ->
                            if (scrollState.maxValue > 0) {
                                val targetFraction = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    scrollState.animateScrollTo((targetFraction * scrollState.maxValue).toInt())
                                }
                            }
                        }
                    }
            ) {
                // Horizontal line indicator in pill style
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(thumbWidth)
                        .fillMaxHeight()
                        .clip(indicatorPillShape)
                        .background(indicatorColor)
                )
            }
        }
    }
}
