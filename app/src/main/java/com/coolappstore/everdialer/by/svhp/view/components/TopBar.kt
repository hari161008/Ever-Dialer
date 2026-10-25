package com.coolappstore.everdialer.by.svhp.view.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import com.coolappstore.everdialer.by.svhp.controller.util.VoiceSearchHelper
import com.coolappstore.everdialer.by.svhp.controller.util.rememberVoiceSearchLauncher
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import com.coolappstore.everdialer.by.svhp.view.theme.wpTurnstileHeader
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker

/** The pill-shaped, non-editable "Search in Ever Dialer" bar — tapping it opens the single
 *  unified [SearchScreenDestination] (contacts, non-contacts, contact notes, recording notes).
 *  Shown at the top of the main tabs (via [TopBar]) as well as Settings, Notes, and Recordings,
 *  so every entry point into search looks and behaves identically. */
@Composable
fun SearchBarPill(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = SettingsClickTracker.isSearchOpening
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    var pillCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val recordSearchClick: () -> Unit = {
        pillCoordinates?.let { coords ->
            val center = coords.boundsInRoot().center
            val rootSize = coords.findRootCoordinates().size
            SettingsClickTracker.recordTap(center.x, center.y, rootSize.width.toFloat(), rootSize.height.toFloat())
        } ?: SettingsClickTracker.recordNormalized(0.50f, 0.05f)
    }

    val voiceSearchLauncher = rememberVoiceSearchLauncher { spokenText ->
        SettingsClickTracker.isSearchOpening = true
        SettingsClickTracker.triggerSearchBounce()
        recordSearchClick()
        navigator.navigate(SearchScreenDestination(initialQuery = spokenText))
    }

    val searchBounceX = remember { Animatable(0f) }
    val searchBounceY = remember { Animatable(0f) }
    val searchBounceScale = remember { Animatable(1f) }
    val searchBounceTrigger = SettingsClickTracker.searchBounceTrigger

    LaunchedEffect(searchBounceTrigger) {
        if (searchBounceTrigger == 0) return@LaunchedEffect
        launch {
            searchBounceX.snapTo(0f)
            searchBounceX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 820
                    12f at 260 using FastOutSlowInEasing   // glide right once
                    2f at 560 using FastOutSlowInEasing    // float gently back
                    0f at 820 using FastOutSlowInEasing    // settle center
                }
            )
        }
        launch {
            searchBounceY.snapTo(0f)
            searchBounceY.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 820
                    (-8f) at 260 using FastOutSlowInEasing // float up once
                    (-1f) at 560 using FastOutSlowInEasing // float gently down
                    0f at 820 using FastOutSlowInEasing    // settle center
                }
            )
        }
        launch {
            searchBounceScale.snapTo(1f)
            searchBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 820
                    1.03f at 260 using FastOutSlowInEasing
                    1.005f at 560 using FastOutSlowInEasing
                    1f at 820 using FastOutSlowInEasing
                }
            )
        }
    }

    AppSearchBar(
        modifier = modifier.graphicsLayer {
            translationX = searchBounceX.value
            translationY = searchBounceY.value
            scaleX = searchBounceScale.value
            scaleY = searchBounceScale.value
        },
        isEditable = false,
        onSearchBarClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
            }
            SettingsClickTracker.isSearchOpening = true
            SettingsClickTracker.triggerSearchBounce()
            recordSearchClick()
            navigator.navigate(SearchScreenDestination())
        },
        onVoiceClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
            }
            VoiceSearchHelper.launchVoiceSearch(context, voiceSearchLauncher)
        },
        onGloballyPositioned = { coords -> pillCoordinates = coords }
    )
}

@Composable
fun TopBar(navController: NavController, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val prefs = koinInject<PreferenceManager>()
    // In landscape, Settings is reachable from the NavigationRail, and each tab screen hosts
    // its own search bar inline within its scrollable content (so it scrolls away with the
    // rest of the list instead of staying pinned) — so the shared top bar renders nothing here.
    if (isLandscape) {
        // Reserve the same top inset the fixed bar used to occupy so page content lines up
        // the same as before, without pinning a non-scrolling search bar on screen.
        Spacer(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars))
        return
    }
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else (-16).dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarOffset"
    )
    LaunchedEffect(Unit) { visible = true }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val isSettingsRunning = currentRoute.contains("settings", ignoreCase = true) || SettingsClickTracker.isSettingsOpening
    val isSearchRunning = currentRoute.contains("search", ignoreCase = true) || SettingsClickTracker.isSearchOpening

    var prevRoute by remember { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        if (prevRoute.contains("settings", ignoreCase = true) && !currentRoute.contains("settings", ignoreCase = true)) {
            SettingsClickTracker.triggerSettingsCloseBounce()
        }
        prevRoute = currentRoute
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isSettingsRunning) 1000f else 1f)
            .windowInsetsPadding(WindowInsets.statusBars)
            .alpha(alpha)
            .offset(y = offsetY),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(if (isSettingsRunning) 1000f else 1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search bar
                SearchBarPill(navigator = navigator, isExpanded = isSearchRunning, modifier = Modifier.weight(1f))

                // Spacer placeholder preserving identical SearchBarPill width and alignment
                Spacer(modifier = Modifier.size(52.dp))
            }

            // Settings button rendered in floating top layer above row and search bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(if (isSettingsRunning) 1000f else 2f)
            ) {
                SettingsIconButton(navigator = navigator, isExpanded = isSettingsRunning)
            }
        }
    }
}

@Composable
fun SettingsIconButton(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = SettingsClickTracker.isSettingsOpening
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsSource = remember { MutableInteractionSource() }
    val settingsPressed by settingsSource.collectIsPressedAsState()
    val settingsScale by animateFloatAsState(
        targetValue = if (settingsPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "settingsScale"
    )

    val bounceScale = remember { Animatable(1f) }
    val settingsBounceTrigger = SettingsClickTracker.settingsBounceTrigger
    val isClosingBounce = SettingsClickTracker.isClosingBounce

    LaunchedEffect(settingsBounceTrigger) {
        if (settingsBounceTrigger == 0) return@LaunchedEffect
        val isClosing = isClosingBounce
        val duration = if (isClosing) 750 else 400
        val peakTime = if (isClosing) 320 else 160
        val peakScale = if (isClosing) 1.18f else 1.16f

        launch {
            bounceScale.snapTo(1f)
            bounceScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = duration
                    peakScale at peakTime using FastOutSlowInEasing
                    1.00f at duration using FastOutSlowInEasing
                }
            )
        }
    }

    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val solidIcons = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false) }
    val solidIconsDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS_DYNAMIC, false) }
    val circleIcons = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CIRCLE_ICONS, false) }
    val solidStyle = remember(settingsVer, isDark) { prefs.getSolidIconsStyle(isDark) }
    val isBackgroundTarget = remember(settingsVer) { prefs.getSaturatedTarget() == PreferenceManager.SATURATED_TARGET_BACKGROUND }

    val (settingsBgColor, settingsFgColor) = when {
        isSaturatedActive && isBackgroundTarget -> {
            val primary = MaterialTheme.colorScheme.primary
            val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(primary.toArgb()) > 0.45
            primary to (if (isBright) Color(0xFF1C1B1F) else Color.White)
        }
        isSaturatedActive || (solidIcons && solidIconsDynamic) -> {
            if (solidStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT) {
                val primary = MaterialTheme.colorScheme.primary
                if (isSaturatedActive && isDark) {
                    primary to Color.Black
                } else {
                    val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(primary.toArgb()) > 0.45
                    primary to (if (isBright) Color(0xFF1C1B1F) else Color.White)
                }
            } else {
                val container = if (!isDark && isSaturatedActive) {
                    androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, 0.30f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(container.toArgb()) > 0.45
                container to (if (isBright) Color(0xFF1C1B1F) else if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        solidIcons -> {
            val baseColor = MaterialTheme.colorScheme.primary
            val adjusted = adjustIconColorForTheme(baseColor, isDark)
            val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(adjusted.toArgb()) > 0.45
            adjusted to (if (isBright) Color(0xFF1C1B1F) else Color.White)
        }
        else -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }
    }

    val appRoundness = LocalCardCornerRadius.current.value.coerceAtLeast(0f)
    val settingsShape = if (circleIcons) {
        CircleShape
    } else {
        RoundedCornerShape((appRoundness * (16f / 28f)).dp)
    }

    var iconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Surface(
        onClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
            }
            SettingsClickTracker.isSettingsOpening = true
            SettingsClickTracker.triggerSettingsOpenBounce()
            iconCoordinates?.let { coords ->
                val center = coords.boundsInRoot().center
                val rootSize = coords.findRootCoordinates().size
                if (rootSize.width > 0 && rootSize.height > 0) {
                    val px = (center.x / rootSize.width.toFloat()).coerceIn(0f, 1f)
                    val py = (center.y / rootSize.height.toFloat()).coerceIn(0f, 1f)
                    SettingsClickTracker.settingsIconOrigin = TransformOrigin(px, py)
                }
                SettingsClickTracker.recordTap(center.x, center.y, rootSize.width.toFloat(), rootSize.height.toFloat())
            } ?: run {
                SettingsClickTracker.settingsIconOrigin = TransformOrigin(0.92f, 0.05f)
                SettingsClickTracker.recordNormalized(0.92f, 0.05f)
            }
            navigator.navigate(SettingsScreenDestination())
        },
        modifier = modifier
            .size(52.dp)
            .zIndex(if (isExpanded) 1000f else 2f)
            .graphicsLayer {
                val s = settingsScale * bounceScale.value
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin.Center
            }
            .onGloballyPositioned { coords ->
                iconCoordinates = coords
                val center = coords.boundsInRoot().center
                val rootSize = coords.findRootCoordinates().size
                if (rootSize.width > 0 && rootSize.height > 0) {
                    val px = (center.x / rootSize.width.toFloat()).coerceIn(0f, 1f)
                    val py = (center.y / rootSize.height.toFloat()).coerceIn(0f, 1f)
                    SettingsClickTracker.settingsIconOrigin = TransformOrigin(px, py)
                }
            },
        shape = settingsShape,
        color = settingsBgColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        interactionSource = settingsSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = settingsFgColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingsBackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isSaturatedContainer = remember(settingsVer) { prefs.getSaturatedTarget() == PreferenceManager.SATURATED_TARGET_CONTAINER }
    val containerBg = if (isSaturatedActive && isSaturatedContainer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentFg = if (isSaturatedActive && isSaturatedContainer) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    val context = LocalContext.current
    FilledIconButton(
        onClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                performAppHaptic(
                    context,
                    prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                    prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                )
            }
            onClick()
        },
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerBg,
            contentColor = contentFg,
            disabledContainerColor = containerBg,
            disabledContentColor = contentFg
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}

/**
 * Floating pill-styled top app bar header for Settings screens.
 * Features a floating capsule surface with subtle elevation, glass border, integrated back button,
 * and bold heading.
 */
@Composable
fun SettingsPillTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
    val isBackgroundTarget = remember(settingsVer) { prefs.getSaturatedTarget() == PreferenceManager.SATURATED_TARGET_BACKGROUND }
    val usePrimary = isSaturatedActive || !isDynamic

    val context = LocalContext.current
    val pillBackground = if (isSaturatedActive && isBackgroundTarget) {
        if (isDark) Color.Black else Color.White
    } else if (isSaturatedActive) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
    val appRoundness = LocalCardCornerRadius.current.value.coerceAtLeast(0f)
    val pillCornerRadius = (appRoundness * (36f / 28f)).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.wrapContentSize().wpTurnstileHeader(),
            shape = RoundedCornerShape(pillCornerRadius),
            color = pillBackground,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(min = 58.dp)
                    .padding(start = 7.dp, end = if (actions != null) 8.dp else 22.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
                    val isInteractive = lifecycleState.isAtLeast(Lifecycle.State.STARTED)

                    val backBtnBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    val isBackBtnBright = androidx.core.graphics.ColorUtils.calculateLuminance(backBtnBg.toArgb()) > 0.45
                    val backBtnFg = if (usePrimary) {
                        if (isBackBtnBright) Color(0xFF1C1B1F) else Color.White
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    FilledIconButton(
                        onClick = {
                            if (!isInteractive) return@FilledIconButton
                            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                performAppHaptic(
                                    context,
                                    prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                    prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                                )
                            }
                            onBackClick()
                        },
                        enabled = true,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = backBtnBg,
                            contentColor = backBtnFg,
                            disabledContainerColor = backBtnBg,
                            disabledContentColor = backBtnFg
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                } else {
                    Spacer(Modifier.width(16.dp))
                }

                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = rivoHeadingColor()
                        )
                    ) {
                        title()
                    }
                }

                if (actions != null) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                }
            }
        }

        if (endActions != null) {
            Surface(
                modifier = Modifier.wrapContentSize().align(Alignment.CenterEnd),
                shape = RoundedCornerShape(pillCornerRadius),
                color = pillBackground,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .heightIn(min = 58.dp)
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    endActions()
                }
            }
        }
    }
}

@Composable
fun SettingsPillTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null
) {
    SettingsPillTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = rivoHeadingColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        onBackClick = onBackClick,
        actions = actions,
        endActions = endActions
    )
}


