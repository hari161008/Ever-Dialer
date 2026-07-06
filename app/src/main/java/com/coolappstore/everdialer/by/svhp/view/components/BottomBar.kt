package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
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
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import kotlinx.coroutines.delay
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
    RecordingsScreenDestination.route,
    NotesScreenDestination.route
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

/** Parses the user-configured tab order preference into an ordered list of tab keys. */
private fun parseTabOrder(raw: String?): List<String> {
    val fallback = PreferenceManager.DEFAULT_TAB_ORDER.split(",")
    if (raw.isNullOrBlank()) return fallback
    val parsed = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    // Ensure any tab keys missing from a stale/older saved order are still appended,
    // so newly-added tabs (like Recordings) always show up even for existing users.
    val merged = parsed.toMutableList()
    fallback.forEach { key -> if (key !in merged) merged.add(key) }
    return merged.filter { it in fallback }
}

@Composable
fun BottomBar(navController: NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) return
    val prefs         = koinInject<PreferenceManager>()
    val context       = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val settingsState by prefs.settingsChanged.collectAsState()

    val pillNav      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
    val iconOnly     = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false) }
    val liquidGlass  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }
    val lgBottomNav  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_LG_BOTTOM_NAV, false) }
    val blurEffects  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
    val blurBottomNav = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_BLUR_BOTTOM_NAV, false) }
    val showFavoritesTab  = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true) }
    val showCallsTab      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true) }
    val showContactsTab   = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true) }
    val showRecordingsTab = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true) }
    val showNotesTab      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true) }
    val tabOrder          = remember(settingsState) { parseTabOrder(prefs.getString(PreferenceManager.KEY_TAB_ORDER, null)) }
    val labelStyle: TextStyle = MaterialTheme.typography.labelMedium

    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute       = currentDestination?.route ?: ""

    val isFavoritesSelected  = currentDestination?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true
    val isRecentsSelected    = currentDestination?.hierarchy?.any { it.route == RecentScreenDestination.route } == true
    val isContactsSelected   = currentDestination?.hierarchy?.any { it.route == ContactScreenDestination.route } == true
    val isRecordingsSelected = currentDestination?.hierarchy?.any { it.route == RecordingsScreenDestination.route } == true
    val isNotesSelected      = currentDestination?.hierarchy?.any { it.route == NotesScreenDestination.route } == true

    // Build visible tab routes dynamically based on prefs
    val visibleTabRoutes = remember(showFavoritesTab, showCallsTab, showContactsTab, showRecordingsTab, showNotesTab) {
        buildSet {
            if (showFavoritesTab)  add(FavoritesScreenDestination.route)
            if (showCallsTab)      add(RecentScreenDestination.route)
            if (showContactsTab)   add(ContactScreenDestination.route)
            if (showRecordingsTab) add(RecordingsScreenDestination.route)
            if (showNotesTab)      add(NotesScreenDestination.route)
        }
    }

    // Only render pill when a visible tab screen is active, and not while a tab screen
    // (e.g. Recordings) is showing its own full-screen onboarding content.
    val isOnTabScreen = visibleTabRoutes.any { currentRoute.contains(it, ignoreCase = true) } &&
        !NavBarVisibilityState.hideForOnboarding

    // If current tab is now hidden, redirect to first visible tab. This must only fire for
    // tabs the user actually disabled in Settings > Tab Sections — not for a visible tab that's
    // just temporarily hiding the nav bar for its own onboarding content (e.g. Recordings'
    // disclaimer/permissions gate), otherwise tapping that tab would immediately get redirected
    // away again in a loop.
    val isOnHiddenTab = TAB_ROUTES.any { currentRoute.contains(it, ignoreCase = true) } &&
        visibleTabRoutes.none { currentRoute.contains(it, ignoreCase = true) }
    fun routeForTabKey(key: String): String? = when (key) {
        "favorites"  -> FavoritesScreenDestination.route
        "calls"      -> RecentScreenDestination.route
        "contacts"   -> ContactScreenDestination.route
        "recordings" -> RecordingsScreenDestination.route
        "notes"      -> NotesScreenDestination.route
        else         -> null
    }

    LaunchedEffect(isOnHiddenTab) {
        if (isOnHiddenTab) {
            val firstVisible = tabOrder
                .asSequence()
                .mapNotNull { routeForTabKey(it) }
                .firstOrNull { it in visibleTabRoutes }
                ?: RecentScreenDestination.route
            navController.navigate(firstVisible) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                launchSingleTop = true
            }
        }
    }

    // ── Slide-in animation — slower, re-triggers every time pill re-enters ───
    var pillVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isOnTabScreen) {
        if (isOnTabScreen) {
            pillVisible = false
            delay(16) // one frame — lets Compose commit the hidden state
            pillVisible = true
        } else {
            pillVisible = false
        }
    }
    val pillOffsetY by animateFloatAsState(
        targetValue   = if (pillVisible) 0f else 220f,
        animationSpec = tween(durationMillis = 750, easing = EaseOutQuint),
        label         = "pillSlideIn"
    )
    val pillAlpha by animateFloatAsState(
        targetValue   = if (pillVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label         = "pillFadeIn"
    )

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
        // If already on this route, do nothing (prevents double-tap freeze)
        if (currentDestination?.hierarchy?.any { it.route == route } == true) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    val orderedTabs: List<TabSpec> = remember(
        tabOrder, showFavoritesTab, showCallsTab, showContactsTab, showRecordingsTab, showNotesTab,
        isFavoritesSelected, isRecentsSelected, isContactsSelected, isRecordingsSelected, isNotesSelected
    ) {
        tabOrder.mapNotNull { key ->
            when (key) {
                "favorites" -> if (showFavoritesTab) TabSpec(
                    key = key, route = FavoritesScreenDestination.route, label = "Favourites",
                    selectedIcon = Icons.Filled.Favorite, unselectedIcon = Icons.Outlined.FavoriteBorder,
                    selected = isFavoritesSelected,
                    onClick = { doHaptic(); navigate(FavoritesScreenDestination.route) }
                ) else null
                "calls" -> if (showCallsTab) TabSpec(
                    key = key, route = RecentScreenDestination.route, label = "Calls",
                    selectedIcon = Icons.Filled.History, unselectedIcon = Icons.Outlined.History,
                    selected = isRecentsSelected,
                    onClick = { doHaptic(); navigate(RecentScreenDestination.route) }
                ) else null
                "contacts" -> if (showContactsTab) TabSpec(
                    key = key, route = ContactScreenDestination.route, label = "Contacts",
                    selectedIcon = Icons.Filled.Person, unselectedIcon = Icons.Outlined.Person,
                    selected = isContactsSelected,
                    onClick = { doHaptic(); navigate(ContactScreenDestination.route) }
                ) else null
                "recordings" -> if (showRecordingsTab) TabSpec(
                    key = key, route = RecordingsScreenDestination.route, label = "Recordings",
                    selectedIcon = Icons.Filled.FiberManualRecord, unselectedIcon = Icons.Outlined.FiberManualRecord,
                    selected = isRecordingsSelected,
                    onClick = { doHaptic(); navigate(RecordingsScreenDestination.route) }
                ) else null
                "notes" -> if (showNotesTab) TabSpec(
                    key = key, route = NotesScreenDestination.route, label = "Notes",
                    selectedIcon = Icons.Filled.Note, unselectedIcon = Icons.Outlined.Note,
                    selected = isNotesSelected,
                    onClick = { doHaptic(); navigate(NotesScreenDestination.route) }
                ) else null
                else -> null
            }
        }
    }

    if (pillNav) {
        if (!isOnTabScreen && !pillVisible) return

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .offset { IntOffset(0, pillOffsetY.toInt()) }
                .graphicsLayer { alpha = pillAlpha },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                val globalBackdrop = LocalLiquidGlassBackdrop.current
                val pillShape = RoundedCornerShape(32.dp)

                val useLgBottomNav = liquidGlass && lgBottomNav && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
                val useBlurBottomNav = blurEffects && blurBottomNav && !useLgBottomNav

                val pillContent: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        orderedTabs.forEach { tab ->
                            PillNavItem(
                                selected       = tab.selected,
                                selectedIcon   = tab.selectedIcon,
                                unselectedIcon = tab.unselectedIcon,
                                label          = tab.label,
                                iconOnly       = iconOnly,
                                onClick        = tab.onClick
                            )
                        }
                    }
                }

                if (useLgBottomNav && globalBackdrop != null) {
                    Surface(
                        shape           = pillShape,
                        color           = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                        shadowElevation = 0.dp,
                        tonalElevation  = 0.dp,
                        modifier = Modifier.drawBackdrop(
                            backdrop = globalBackdrop,
                            shape = { pillShape },
                            effects = {
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
                        modifier        = Modifier.drawPlainBackdrop(
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
                    ) { pillContent() }
                }
            }
        }
    } else {
        val navBarAlpha by animateFloatAsState(
            targetValue   = if (isOnTabScreen) 1f else 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label         = "navBarAlpha"
        )
        if (!isOnTabScreen && navBarAlpha == 0f) return
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .graphicsLayer { alpha = navBarAlpha }
        ) {
            orderedTabs.forEach { tab ->
                AnimatedNavBarItem(
                    selected       = tab.selected,
                    selectedIcon   = tab.selectedIcon,
                    unselectedIcon = tab.unselectedIcon,
                    label          = tab.label,
                    iconOnly       = iconOnly,
                    labelStyle     = labelStyle,
                    onClick        = tab.onClick
                )
            }
        }
    }
}

// ── Animated standard nav bar item ────────────────────────────────────────────

@Composable
private fun RowScope.AnimatedNavBarItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    iconOnly: Boolean,
    labelStyle: TextStyle,
    onClick: () -> Unit
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

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = indicatorAlpha))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Crossfade(
                        targetState   = selected,
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label         = "${label}IconCrossfade"
                    ) { sel ->
                        Icon(
                            imageVector        = if (sel) selectedIcon else unselectedIcon,
                            contentDescription = label,
                            modifier           = Modifier.size(iconSize)
                        )
                    }
                }
            },
            label           = if (iconOnly) null else ({ Text(label, style = labelStyle) }),
            alwaysShowLabel = !iconOnly,
            selected        = selected,
            interactionSource = interactionSource,
            colors          = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                indicatorColor    = Color.Transparent
            ),
            onClick = onClick
        )
    }
}

// ── Pill nav item ─────────────────────────────────────────────────────────────

@Composable
private fun PillNavItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    iconOnly: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label         = "${label}BgAlpha"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
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
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
