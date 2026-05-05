package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
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
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.os.Build
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
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
    NotesScreenDestination.route
)

@Composable
fun BottomBar(navController: NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) return
    val prefs         = koinInject<PreferenceManager>()
    val context       = LocalContext.current
    @Suppress("UNUSED_VARIABLE")
    val settingsState by prefs.settingsChanged.collectAsState()

    val pillNav      = prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true)
    val iconOnly     = prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)
    val notesEnabled = prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)
    val liquidGlass  = prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false)
    val lgBottomNav  = prefs.getBoolean(PreferenceManager.KEY_LG_BOTTOM_NAV, false)
    val labelStyle: TextStyle = MaterialTheme.typography.labelMedium

    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute       = currentDestination?.route ?: ""

    // ── isNavigating debounce ─────────────────────────────────────────────────
    // Freeze selection on the tapped item while the back-stack is in transition
    // so neither the old nor the new route can momentarily appear selected.
    var isNavigating by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf<String?>(null) }

    fun routeSelected(dest: String): Boolean {
        if (isNavigating && pendingRoute != null) return pendingRoute == dest
        return currentDestination?.hierarchy?.any { it.route == dest } == true
    }

    val isFavoritesSelected = routeSelected(FavoritesScreenDestination.route)
    val isRecentsSelected   = routeSelected(RecentScreenDestination.route)
    val isContactsSelected  = routeSelected(ContactScreenDestination.route)
    val isNotesSelected     = routeSelected(NotesScreenDestination.route)

    // Clear flag once back-stack settles on the pending destination
    LaunchedEffect(currentRoute) {
        if (isNavigating && pendingRoute != null &&
            currentRoute.contains(pendingRoute!!, ignoreCase = true)
        ) {
            delay(80)
            isNavigating = false
            pendingRoute = null
        }
    }

    // Only render pill when a tab screen is active
    val isOnTabScreen = TAB_ROUTES.any { currentRoute.contains(it, ignoreCase = true) }

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
        if (isNavigating) return          // drop rapid double-taps to different tabs
        isNavigating = true
        pendingRoute = route
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    if (pillNav) {
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
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                val globalBackdrop = LocalLiquidGlassBackdrop.current
                val pillShape = RoundedCornerShape(32.dp)

                val useLgBottomNav = liquidGlass && lgBottomNav && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
                Surface(
                    shape           = pillShape,
                    color           = if (useLgBottomNav)
                                          MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
                                      else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = if (useLgBottomNav) 0.dp else 8.dp,
                    tonalElevation  = if (useLgBottomNav) 0.dp else 4.dp,
                    modifier = if (useLgBottomNav) {
                        Modifier.drawBackdrop(
                            backdrop = globalBackdrop!!,
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
                    } else Modifier
                ) {
                    Row(
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = spring(
                                    stiffness    = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioLowBouncy
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
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
        if (!isOnTabScreen) return
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
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
                        animationSpec = tween(durationMillis = 420, easing = EaseOutQuint),
                        expandFrom    = Alignment.Start
                    ) + fadeIn(tween(350)),
                    exit = shrinkHorizontally(
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Start
                    ) + fadeOut(tween(220))
                ) {
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
