package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import org.koin.compose.koinInject

// Tab routes — only show the bar when one of these is active
private val TAB_ROUTES = setOf(
    FavoritesScreenDestination.route,
    RecentScreenDestination.route,
    ContactScreenDestination.route,
    NotesScreenDestination.route
)

@Composable
fun BottomBar(navController: NavController) {
    val prefs         = koinInject<PreferenceManager>()
    val context       = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val settingsState by prefs.settingsChanged.collectAsState()

    val pillNav      = prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true)
    val iconOnly     = prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)
    val notesEnabled = prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)
    val labelStyle: TextStyle = MaterialTheme.typography.labelMedium

    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute       = currentDestination?.route ?: ""

    // Track the last-clicked route so selection is instant and never shows two items lit at once
    var pendingRoute by remember { mutableStateOf<String?>(null) }

    val isFavoritesSelected = (pendingRoute ?: currentRoute).let { r ->
        if (pendingRoute != null) r == FavoritesScreenDestination.route
        else currentDestination?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true
    }
    val isRecentsSelected = (pendingRoute ?: currentRoute).let { r ->
        if (pendingRoute != null) r == RecentScreenDestination.route
        else currentDestination?.hierarchy?.any { it.route == RecentScreenDestination.route } == true
    }
    val isContactsSelected = (pendingRoute ?: currentRoute).let { r ->
        if (pendingRoute != null) r == ContactScreenDestination.route
        else currentDestination?.hierarchy?.any { it.route == ContactScreenDestination.route } == true
    }
    val isNotesSelected = (pendingRoute ?: currentRoute).let { r ->
        if (pendingRoute != null) r == NotesScreenDestination.route
        else currentDestination?.hierarchy?.any { it.route == NotesScreenDestination.route } == true
    }

    // Clear pendingRoute once navigation settles on the target
    LaunchedEffect(currentRoute) {
        if (pendingRoute != null && currentRoute.contains(pendingRoute!!, ignoreCase = true)) {
            pendingRoute = null
        }
    }

    // Only render pill when a tab screen is active
    val isOnTabScreen = TAB_ROUTES.any { currentRoute.contains(it, ignoreCase = true) }

    // Slide-in animation for the pill — triggers on first appear AND every time
    // the pill re-enters (e.g. returning from Settings, ContactDetails, etc.)
    var pillVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isOnTabScreen) {
        if (isOnTabScreen) {
            pillVisible = false
            kotlinx.coroutines.delay(16) // one frame — lets Compose commit the hidden state
            pillVisible = true
        } else {
            pillVisible = false
        }
    }
    val pillOffsetY by animateFloatAsState(
        targetValue   = if (pillVisible) 0f else 200f,
        animationSpec = tween(durationMillis = 520, easing = EaseOutQuint),
        label         = "pillSlideIn"
    )
    val pillAlpha by animateFloatAsState(
        targetValue   = if (pillVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
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
        pendingRoute = route
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    if (pillNav) {
        // Hide entirely when not on a tab screen (e.g. Settings, ContactDetails, etc.)
        if (!isOnTabScreen) return

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
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape           = RoundedCornerShape(50.dp),
                    color           = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation  = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        PillNavItem(
                            selected       = isFavoritesSelected,
                            selectedIcon   = Icons.Filled.Favorite,
                            unselectedIcon = Icons.Outlined.FavoriteBorder,
                            label          = "Favourites",
                            iconOnly       = iconOnly,
                            onClick        = { doHaptic(); navigate(FavoritesScreenDestination.route) }
                        )
                        PillNavItem(
                            selected       = isRecentsSelected,
                            selectedIcon   = Icons.Filled.History,
                            unselectedIcon = Icons.Outlined.History,
                            label          = "Calls",
                            iconOnly       = iconOnly,
                            onClick        = { doHaptic(); navigate(RecentScreenDestination.route) }
                        )
                        PillNavItem(
                            selected       = isContactsSelected,
                            selectedIcon   = Icons.Filled.Person,
                            unselectedIcon = Icons.Outlined.Person,
                            label          = "Contacts",
                            iconOnly       = iconOnly,
                            onClick        = { doHaptic(); navigate(ContactScreenDestination.route) }
                        )
                        if (notesEnabled) {
                            PillNavItem(
                                selected       = isNotesSelected,
                                selectedIcon   = Icons.Filled.Note,
                                unselectedIcon = Icons.Outlined.Note,
                                label          = "Notes",
                                iconOnly       = iconOnly,
                                onClick        = { doHaptic(); navigate(NotesScreenDestination.route) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Standard bottom navigation bar — always visible, hides on non-tab via NavigationBarItem selection state
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            AnimatedNavBarItem(
                selected       = isFavoritesSelected,
                selectedIcon   = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder,
                label          = "Favourites",
                iconOnly       = iconOnly,
                labelStyle     = labelStyle,
                onClick        = { doHaptic(); navigate(FavoritesScreenDestination.route) }
            )
            AnimatedNavBarItem(
                selected       = isRecentsSelected,
                selectedIcon   = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                label          = "Calls",
                iconOnly       = iconOnly,
                labelStyle     = labelStyle,
                onClick        = { doHaptic(); navigate(RecentScreenDestination.route) }
            )
            AnimatedNavBarItem(
                selected       = isContactsSelected,
                selectedIcon   = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                label          = "Contacts",
                iconOnly       = iconOnly,
                labelStyle     = labelStyle,
                onClick        = { doHaptic(); navigate(ContactScreenDestination.route) }
            )
            if (notesEnabled) {
                AnimatedNavBarItem(
                    selected       = isNotesSelected,
                    selectedIcon   = Icons.Filled.Note,
                    unselectedIcon = Icons.Outlined.Note,
                    label          = "Notes",
                    iconOnly       = iconOnly,
                    labelStyle     = labelStyle,
                    onClick        = { doHaptic(); navigate(NotesScreenDestination.route) }
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
    val iconSize by animateDpAsState(
        targetValue   = if (selected) 27.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "${label}Size"
    )
    // Bounce only on selection, snap-back on deselection — prevents "phantom press" on the old tab
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.15f else 1f,
        animationSpec = if (selected)
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        else
            tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label         = "${label}Scale"
    )

    NavigationBarItem(
        icon = {
            Box(modifier = Modifier.scale(scale)) {
                Icon(
                    imageVector        = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    modifier           = Modifier.size(iconSize)
                )
            }
        },
        label           = if (iconOnly) null else ({ Text(label, style = labelStyle) }),
        alwaysShowLabel = !iconOnly,
        selected        = selected,
        colors          = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            indicatorColor    = MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = onClick
    )
}

// ── Pill nav item ─────────────────────────────────────────────────────────────
// Bounce ONLY when becoming selected. Deselection uses a fast linear tween so
// the previously-active item never overshoots back through 1f (which looked
// like a phantom press on the old tab).

@Composable
private fun PillNavItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    iconOnly: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label         = "${label}BgColor"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label         = "${label}IconTint"
    )
    // Select  → bouncy spring   (satisfying bounce into place)
    // Deselect → fast tween     (no overshoot — stops exactly at 1f)
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.12f else 1f,
        animationSpec = if (selected)
            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        else
            tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label         = "${label}Scale"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (iconOnly) 16.dp else 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (iconOnly) {
            Icon(
                imageVector        = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                modifier           = Modifier.size(24.dp).scale(scale),
                tint               = iconTint
            )
        } else {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector        = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    modifier           = Modifier.size(24.dp).scale(scale),
                    tint               = iconTint
                )
                if (selected) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
