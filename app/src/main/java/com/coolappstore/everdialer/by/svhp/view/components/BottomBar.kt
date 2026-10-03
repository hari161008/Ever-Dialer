package com.coolappstore.everdialer.by.svhp.view.components

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
    val lgBottomNav  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_LG_BOTTOM_NAV, false) }
    val blurEffects  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
    val blurBottomNav = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_BLUR_BOTTOM_NAV, false) }
    val showFavoritesTab  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true) }
    val showCallsTab      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true) }
    val showContactsTab   = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true) }
    val showSmsTab        = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true) }
    val showGroupsTab     = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false) }
    val showRecordingsTab = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true) }
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

    // Only render pill when a visible tab screen is active, and not while a tab screen
    // (e.g. Recordings) is showing its own full-screen onboarding content.
    val isOnTabScreen = visibleTabRoutes.any { currentRoute.contains(it, ignoreCase = true) } &&
        !NavBarVisibilityState.hideForOnboarding &&
        !NavBarVisibilityState.hideForSelectionMode &&
        !NavBarVisibilityState.hideForSettingsEntry &&
        !NavBarVisibilityState.hideForSearchResult

    // If current tab is now hidden, redirect to first visible tab. This must only fire for
    // tabs the user actually disabled in Settings > Tab Sections — not for a visible tab that's
    // just temporarily hiding the nav bar for its own onboarding content (e.g. Recordings'
    // disclaimer/permissions gate), otherwise tapping that tab would immediately get redirected
    // away again in a loop.
    val isOnHiddenTab = TAB_ROUTES.any { currentRoute.contains(it, ignoreCase = true) } &&
        visibleTabRoutes.none { currentRoute.contains(it, ignoreCase = true) } &&
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
    val pillOffsetY by animateFloatAsState(
        targetValue   = if (pillVisible) 0f else 220f,
        animationSpec = tween(durationMillis = 600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label         = "pillSlideIn"
    )
    val pillAlpha by animateFloatAsState(
        targetValue   = if (pillVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
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
        if (!isOnTabScreen && pillAlpha <= 0.005f && pillOffsetY >= 210f) return

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .offset { IntOffset(0, pillOffsetY.toInt()) }
                .graphicsLayer { alpha = pillAlpha },
            contentAlignment = Alignment.Center
        ) {
            val globalBackdrop = LocalLiquidGlassBackdrop.current
            val pillShape = RoundedCornerShape(32.dp)

            val useLgBottomNav = liquidGlass && lgBottomNav && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
            val useBlurBottomNav = blurEffects && blurBottomNav && !useLgBottomNav

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
                targetValue   = if (canScroll && scrollIndication) 1f else 0f,
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
                val pillContent: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        orderedTabs.forEach { tab ->
                            key(tab.key) {
                                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                                LaunchedEffect(tab.selected) {
                                    if (tab.selected) {
                                        try {
                                            bringIntoViewRequester.bringIntoView()
                                        } catch (_: Throwable) {}
                                    }
                                }
                                PillNavItem(
                                    modifier       = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                                    selected       = tab.selected,
                                    selectedIcon   = tab.selectedIcon,
                                    unselectedIcon = tab.unselectedIcon,
                                    label          = tab.label,
                                    iconOnly       = iconOnly,
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
                    .clip(pillShape)

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
                                colorControls(saturation = 1.4f)
                                blur(2f * d)
                                lens(
                                    refractionHeight = 23f * d,
                                    refractionAmount = 64f * d
                                )
                            },
                            highlight = { Highlight.Default }
                        )
                    ) { pillContent() }
                } else if (useBlurBottomNav && globalBackdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Surface(
                        shape           = pillShape,
                        color           = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                        shadowElevation = 0.dp,
                        tonalElevation  = 0.dp,
                        modifier        = mainPillModifier.drawPlainBackdrop(
                            backdrop = globalBackdrop,
                            shape    = { pillShape },
                            effects  = { blur(30f * density) }
                        )
                    ) { pillContent() }
                } else {
                    Surface(
                        shape           = pillShape,
                        color           = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 8.dp,
                        tonalElevation  = 4.dp,
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
        val navBarAlpha by animateFloatAsState(
            targetValue   = if (isOnTabScreen) 1f else 0f,
            animationSpec = tween(durationMillis = 550, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            label         = "navBarAlpha"
        )
        if (!isOnTabScreen && navBarAlpha <= 0.005f) return

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
            targetValue   = if (canStandardScroll && scrollIndication) 1f else 0f,
            animationSpec = tween(durationMillis = 320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            label         = "standardIndicatorProgress"
        )

        Surface(
            color          = if (isSmsSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            modifier       = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .graphicsLayer { alpha = navBarAlpha }
        ) {
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
                        .horizontalScroll(standardScrollState)
                        .padding(horizontal = if (canStandardScroll) 12.dp else 0.dp),
                    horizontalArrangement = if (canStandardScroll) Arrangement.spacedBy(4.dp) else Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    orderedTabs.forEach { tab ->
                        key(tab.key) {
                            val bringIntoViewRequester = remember { BringIntoViewRequester() }
                            LaunchedEffect(tab.selected) {
                                if (tab.selected) {
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
                            useLg       = false,
                            useBlur     = false
                        )
                    }
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconSize by animateDpAsState(
        targetValue   = if (selected) 26.dp else 22.dp,
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
    // Fade the selected-state highlight pill in/out smoothly instead of the
    // default indicator's abrupt show/hide.
    val indicatorAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "${label}IndicatorAlpha"
    )

    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
    val usePrimary = isSaturatedActive || !isDynamic
    val activeNavBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val activeNavFg = if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
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
                    tint               = if (sel) activeNavFg else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!iconOnly) {
            Spacer(Modifier.height(3.dp))
            Text(
                text     = label,
                style    = labelStyle,
                color    = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val activeNavFg = if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label         = "${label}BgAlpha"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (selected) activeNavFg
                        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label         = "${label}IconTint"
    )
    // Tap-press squish → spring back bounce
    val scale by animateFloatAsState(
        targetValue   = when {
            isPressed -> 0.82f
            selected  -> 1.10f
            else      -> 1f
        },
        animationSpec = if (isPressed)
            tween(durationMillis = 80, easing = FastOutSlowInEasing)
        else if (selected)
            spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        else
            tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "${label}Scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50.dp))
            .background(activeNavBg.copy(alpha = bgAlpha))
            .combinedClickable(interactionSource = interactionSource, indication = null, onClick = onClick, onLongClick = onLongClick)
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
                        color = activeNavFg,
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
