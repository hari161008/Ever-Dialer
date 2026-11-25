package com.modiprincekr.thedialer.view.screen

import android.Manifest
import android.content.res.Configuration
import com.modiprincekr.thedialer.view.theme.TabTransitionStyle
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.filled.*
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.modiprincekr.thedialer.controller.CallLogViewModel
import com.modiprincekr.thedialer.modal.data.CallLogEntry
import com.modiprincekr.thedialer.controller.util.formatDateHeader
import com.modiprincekr.thedialer.controller.util.makeCall
import com.modiprincekr.thedialer.controller.util.placeCallHonoringContactSim
import com.modiprincekr.thedialer.controller.util.numbersLikelyMatch
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.modal.data.CallLogFilter
import com.modiprincekr.thedialer.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupsScreenDestination
import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import com.modiprincekr.thedialer.controller.ContactsViewModel
import android.os.Build
import com.modiprincekr.thedialer.liquidglass.drawBackdrop
import com.modiprincekr.thedialer.liquidglass.effects.lens
import com.modiprincekr.thedialer.liquidglass.effects.blur
import com.modiprincekr.thedialer.liquidglass.effects.colorControls
import com.modiprincekr.thedialer.liquidglass.highlight.Highlight
import com.modiprincekr.thedialer.liquidglass.backdrops.rememberLayerBackdrop
import com.modiprincekr.thedialer.liquidglass.backdrops.layerBackdrop
import com.modiprincekr.thedialer.liquidglass.LocalLiquidGlassBackdrop
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Calendar
import java.util.Locale

private val ColorBlue   = Color(0xFF2196F3)
private val ColorRed    = Color(0xFFE91E63)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)
private val ColorPurple = Color(0xFF9C27B0)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true, style = TabTransitionStyle::class)
@Composable
fun RecentScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val prefs = koinInject<com.modiprincekr.thedialer.controller.util.PreferenceManager>()
    val pillNav = remember { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_PILL_NAV, true) }

    var showDialpad by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Hoisted selection state
    val context = LocalContext.current
    var selectionMode by remember { mutableStateOf(false) }
    var selectedLogs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSelectionDeleteConfirm by remember { mutableStateOf(false) }
    var showSelectionMenuOuter by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedLogs = emptySet()
    }

    var fabCenter by remember { mutableStateOf<Offset?>(null) }
    var dialpadClosing by remember { mutableStateOf(false) }
    val dialpadProgress = remember { Animatable(0f) }

    LaunchedEffect(showDialpad) {
        NavBarVisibilityState.hideForDialpad = showDialpad
        if (showDialpad) {
            dialpadClosing = false
            dialpadProgress.snapTo(0f)
            dialpadProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 680, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
            )
        } else {
            dialpadProgress.snapTo(0f)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            NavBarVisibilityState.hideForDialpad = false
        }
    }

    val closeDialpad: ((() -> Unit)?) -> Unit = { onClosed ->
        if (showDialpad && !dialpadClosing) {
            dialpadClosing = true
            NavBarVisibilityState.hideForDialpad = false
            scope.launch {
                dialpadProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 520, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                )
                showDialpad = false
                dialpadClosing = false
                onClosed?.invoke()
            }
        }
    }

    val predictiveBackEnabled = remember(prefs.settingsChanged.collectAsState().value) {
        prefs.getBoolean(PreferenceManager.KEY_PREDICTIVE_BACK_GESTURE, true)
    }

    if (predictiveBackEnabled) {
        PredictiveBackHandler(enabled = showDialpad && !dialpadClosing) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    // Interactive scrub: scale down smoothly with gesture progress
                    val p = 1f - (backEvent.progress * 0.45f)
                    dialpadProgress.snapTo(p)
                }
                // When back gesture commits, collapse back into FAB
                dialpadClosing = true
                NavBarVisibilityState.hideForDialpad = false
                dialpadProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 520, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                )
                showDialpad = false
                dialpadClosing = false
            } catch (e: CancellationException) {
                // If user cancels back gesture, snap/animate back to full open state
                dialpadClosing = false
                dialpadProgress.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    } else {
        BackHandler(enabled = showDialpad && !dialpadClosing) {
            closeDialpad(null)
        }
    }

    val isDialpadOpen = showDialpad && !dialpadClosing
    val dialpadExpandScale by animateFloatAsState(
        targetValue = if (isDialpadOpen) 2.6f else 1f,
        animationSpec = tween(
            durationMillis = if (isDialpadOpen) 680 else 520,
            easing = if (isDialpadOpen) CubicBezierEasing(0.16f, 1f, 0.3f, 1f) else CubicBezierEasing(0.2f, 0f, 0f, 1f)
        ),
        label = "dialpadExpandScale"
    )
    val dialpadFabElevation by animateDpAsState(
        targetValue = if (isDialpadOpen) 12.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dialpadFabElevation"
    )

    val fabScale by animateFloatAsState(
        targetValue = if (fabVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )
    LaunchedEffect(Unit) {
        fabVisible = true
        if (prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) {
            showDialpad = true
        }
    }

    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val allCallLogs by callLogViewModel.allCallLogs.collectAsState()
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(permState.status) {
        if (permState.status == PermissionStatus.Granted) {
            callLogViewModel.refreshLogs()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val isNewContacts = prefs.getBoolean(PreferenceManager.KEY_NEW_CONTACTS_LOGIC, false)
                if (isNewContacts) {
                    if (contactsViewModel.allContacts.value.isEmpty()) {
                        contactsViewModel.fetchContacts()
                    }
                } else {
                    contactsViewModel.fetchContacts()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                callLogViewModel.refreshLogs()
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    val isNewContacts = prefs.getBoolean(PreferenceManager.KEY_NEW_CONTACTS_LOGIC, false)
                    if (isNewContacts) {
                        if (contactsViewModel.allContacts.value.isEmpty()) {
                            contactsViewModel.fetchContacts()
                        }
                    } else {
                        contactsViewModel.fetchContacts()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var childHScrolling by remember { mutableStateOf(false) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A child LazyRow is consuming horizontal scroll – block our swipe nav
                if (kotlin.math.abs(available.x) > kotlin.math.abs(available.y)) {
                    childHScrolling = true
                }
                return Offset.Zero
            }
        }
    }

    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val recentsBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(isRtl) {
                // Use PointerEventPass.Final so children (LazyColumn) get events first.
                // Only trigger navigation when the horizontal movement clearly dominates
                // vertical movement, preventing accidental swipes during scrolling.
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull() ?: continue
                        if (!down.pressed) continue
                        val startX = down.position.x
                        val startY = down.position.y
                        val startTime = System.currentTimeMillis()
                        var triggered = false
                        childHScrolling = false // reset at start of each gesture
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            val elapsed = System.currentTimeMillis() - startTime
                            if (!triggered &&
                                !change.isConsumed &&
                                !childHScrolling &&
                                elapsed >= 150L &&
                                kotlin.math.abs(dx) > 700f &&
                                kotlin.math.abs(dx) > kotlin.math.abs(dy) * 5.5f
                            ) {
                                triggered = true
                                scope.launch {
                                    TabNavigationHelper.navigateAdjacentTab(
                                        navController = navController,
                                        currentRoute = RecentScreenDestination.route,
                                        goForward = dx < 0,
                                        prefs = prefs,
                                        isRtl = isRtl
                                    )
                                }
                            }
                            if (!change.pressed) {
                                childHScrolling = false
                                break
                            }
                        }
                    }
                }
            },
        topBar = { TopBar(navController, navigator) },
        floatingActionButton = {
            val settingsVer by prefs.settingsChanged.collectAsState()
            val liquidGlass = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }
            val lgRecentsFab = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_LG_RECENTS_FAB, true) }
            val blurEffects = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
            val blurRecentsFab = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_BLUR_RECENTS_FAB, true) }
            val lgRecentsConfig = remember(settingsVer) { prefs.getLiquidGlassConfig(PreferenceManager.KEY_LG_RECENTS_FAB) }
            val blurRecentsConfig = remember(settingsVer) { prefs.getBlurConfig(PreferenceManager.KEY_BLUR_RECENTS_FAB) }
            val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
            val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
            val isDynamic = remember(settingsVer) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_DYNAMIC_COLORS, true) }
            val usePrimary = isSaturatedActive || !isDynamic
            val fabBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            val fabFg = if (isSaturatedActive && isDark) Color.Black else if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            val fabShape = RoundedCornerShape(17.dp)
            val useLiquidGlass = liquidGlass && lgRecentsFab && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val useBlur = blurEffects && blurRecentsFab && !useLiquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val onOpenDialpad: () -> Unit = {
                try {
                    performAppHaptic(
                        context,
                        prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                        prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                    )
                } catch (_: Throwable) {}
                dialpadClosing = false
                showDialpad = true
            }
            val baseModifier = Modifier
                .onGloballyPositioned { coordinates ->
                    fabCenter = coordinates.boundsInRoot().center
                }
                .scale(fabScale * dialpadExpandScale)
                .then(
                    if (isLandscape) {
                        Modifier.navigationBarsPadding().padding(bottom = 12.dp, end = 8.dp)
                    } else if (pillNav) {
                        Modifier.navigationBarsPadding().padding(bottom = 92.dp)
                    } else {
                        Modifier.navigationBarsPadding().padding(bottom = 80.dp)
                    }
                )
            if (useLiquidGlass) {
                FloatingActionButton(
                    onClick = onOpenDialpad,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    modifier = baseModifier.drawBackdrop(
                        backdrop = recentsBackdrop,
                        shape = { fabShape },
                        effects = {
                            val d = density
                            colorControls(brightness = lgRecentsConfig.brightness, saturation = lgRecentsConfig.saturation)
                            if (lgRecentsConfig.blurRadiusDp > 0f) blur(lgRecentsConfig.blurRadiusDp * d)
                            lens(
                                refractionHeight = lgRecentsConfig.refractionHeightDp * d,
                                refractionAmount = lgRecentsConfig.refractionAmountDp * d,
                                chromaticAberration = lgRecentsConfig.chromaticAberration,
                                depthEffect = lgRecentsConfig.depthEffect
                            )
                        },
                        highlight = { Highlight.Default }
                    )
                ) { Icon(Icons.Default.Dialpad, "Dialpad") }
            } else if (useBlur) {
                Box(
                    modifier = baseModifier.drawBackdrop(
                        backdrop = recentsBackdrop,
                        shape = { fabShape },
                        effects = {
                            val d = density
                            colorControls(saturation = blurRecentsConfig.saturation)
                            blur(blurRecentsConfig.blurRadiusDp * d)
                        },
                        highlight = { Highlight.Default }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(fabBg.copy(alpha = 0.35f), fabShape)
                    )
                    FloatingActionButton(
                        onClick = onOpenDialpad,
                        containerColor = Color.Transparent,
                        contentColor = fabFg,
                        shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                    ) { Icon(Icons.Default.Dialpad, "Dialpad") }
                }
            } else {
                FloatingActionButton(
                    onClick = onOpenDialpad,
                    containerColor = fabBg,
                    contentColor = fabFg,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = dialpadFabElevation,
                        pressedElevation = dialpadFabElevation,
                        focusedElevation = dialpadFabElevation,
                        hoveredElevation = dialpadFabElevation
                    ),
                    modifier = baseModifier
                ) { Icon(Icons.Default.Dialpad, "Dialpad") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        val settingsVer by prefs.settingsChanged.collectAsState()
        val needBackdropContent = remember(settingsVer) {
            val lg = prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) &&
                     prefs.getBoolean(PreferenceManager.KEY_LG_RECENTS_FAB, true)
            val bl = prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) &&
                     prefs.getBoolean(PreferenceManager.KEY_BLUR_RECENTS_FAB, true)
            (lg || bl) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .then(if (needBackdropContent) Modifier.layerBackdrop(recentsBackdrop) else Modifier)
        ) {
            CallLogFullContent(
                navController = navController,
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState,
                selectionMode = selectionMode,
                selectedLogs = selectedLogs,
                onSelectionModeChange = { selectionMode = it },
                onSelectedLogsChange = { selectedLogs = it },
                showSelectionDeleteConfirm = showSelectionDeleteConfirm,
                onShowSelectionDeleteConfirmChange = { showSelectionDeleteConfirm = it }
            )
        }
    }
    // Selection bar overlays at screen root level (outside Scaffold)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart)
            .zIndex(10f)
    ) {
                AnimatedVisibility(
                    visible = selectionMode,
                    enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)),
                    exit  = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(420, easing = FastOutLinearInEasing)) + fadeOut(animationSpec = tween(380, easing = FastOutLinearInEasing))
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectionMode = false; selectedLogs = emptySet() }) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(
                                "${selectedLogs.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(onClick = { showSelectionMenuOuter = true }) {
                                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                DropdownMenu(expanded = showSelectionMenuOuter, onDismissRequest = { showSelectionMenuOuter = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Select All") },
                                        leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                                        onClick = {
                                            showSelectionMenuOuter = false
                                            selectedLogs = allCallLogs.map { "${it.number}|${it.date}" }.toSet()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { showSelectionMenuOuter = false; if (selectedLogs.isNotEmpty()) showSelectionDeleteConfirm = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        leadingIcon = { Icon(Icons.Default.Share, null) },
                                        onClick = {
                                            showSelectionMenuOuter = false
                                            val text = selectedLogs.joinToString("\n") { it.split("|").firstOrNull() ?: it }
                                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                                            context.startActivity(Intent.createChooser(intent, "Share call logs"))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Deselect All") },
                                        leadingIcon = { Icon(Icons.Default.Close, null) },
                                        onClick = { showSelectionMenuOuter = false; selectedLogs = emptySet() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val p = dialpadProgress.value
        if (p > 0.001f || showDialpad) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .background(Color.Black.copy(alpha = 0.45f * p))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeDialpad(null) }
            )

            // Expanding Dialpad Container originating at the FAB
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(21f)
            ) {
                val density = LocalDensity.current
                val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                val pivotX = ((fabCenter?.x ?: (screenWidthPx * 0.88f)) / screenWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val pivotY = ((fabCenter?.y ?: (screenHeightPx * 0.92f)) / screenHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = lerp(0.10f, 1f, p)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(pivotX, pivotY)
                            this.alpha = ((p - 0.03f) / 0.97f).coerceIn(0f, 1f)
                        },
                    shape = RoundedCornerShape(
                        topStart = if (p < 0.96f) 28.dp else 0.dp,
                        topEnd = if (p < 0.96f) 28.dp else 0.dp,
                        bottomStart = if (p < 0.96f) 28.dp else 0.dp,
                        bottomEnd = if (p < 0.96f) 28.dp else 0.dp
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount > 15f) {
                                            closeDialpad(null)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(width = 36.dp, height = 4.dp)
                            ) {}
                        }
                        com.modiprincekr.thedialer.view.theme.ProvideScaledDensity(prefs = prefs) {
                            DialPadContent(
                                navigator = navigator,
                                onDismiss = { closeDialpad(null) },
                                closing = dialpadClosing,
                                onNavigateToContact = { contactId, phoneNumber ->
                                    closeDialpad {
                                        navigator.navigate(
                                            ContactDetailsScreenDestination(
                                                contactId = contactId,
                                                phoneNumber = phoneNumber
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun todayStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavouritesSection(
    favorites: List<com.modiprincekr.thedialer.modal.data.Contact>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onContactClick: (com.modiprincekr.thedialer.modal.data.Contact) -> Unit,
    onContactLongClick: (com.modiprincekr.thedialer.modal.data.Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    if (favorites.isEmpty()) return

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "favsChevronRotation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = com.modiprincekr.thedialer.controller.util.tr("Favourites"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse Favourites" else "Expand Favourites",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isExpanded) chevronRotation else chevronRotation - 180f),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(favorites, key = { it.id }) { contact ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { onContactClick(contact) },
                                onLongClick = { onContactLongClick(contact) }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ExpressiveAvatar(
                                name = contact.name,
                                photoUri = contact.photoUri,
                                size = 56.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogFullContent(
    navController: NavController,
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectionMode: Boolean = false,
    selectedLogs: Set<String> = emptySet(),
    onSelectionModeChange: (Boolean) -> Unit = {},
    onSelectedLogsChange: (Set<String>) -> Unit = {},
    showSelectionDeleteConfirm: Boolean = false,
    onShowSelectionDeleteConfirmChange: (Boolean) -> Unit = {}
) {
    val prefs = koinInject<com.modiprincekr.thedialer.controller.util.PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isGranted) {
        val viewModel: CallLogViewModel = koinActivityViewModel()
        val contactsViewModel: com.modiprincekr.thedialer.controller.ContactsViewModel = koinActivityViewModel()
        val logs by viewModel.allCallLogs.collectAsState()
        val isFullHistoryLoaded by viewModel.isFullHistoryLoaded.collectAsState()
        val isLoadingMore by viewModel.isLoadingMore.collectAsState()
        val contacts by contactsViewModel.allContacts.collectAsState()
        val selectedFilter by viewModel.selectedFilter.collectAsState()
        val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
        val allCategories = remember(settingsVersion) { prefs.getAllCallCategories() }
        val hiddenCategories = remember(settingsVersion) { prefs.getHiddenCallCategories() }
        val visibleCategories = remember(allCategories, hiddenCategories) {
            allCategories.filterNot { hiddenCategories.contains(it.id) }
        }
        val customCategories = remember(settingsVersion) { prefs.getCustomCallCategories() }

        LaunchedEffect(visibleCategories) {
            val def = prefs.getDefaultCallCategory()
            if (visibleCategories.isNotEmpty() && visibleCategories.none { it.id == selectedCategoryId }) {
                val fallback = if (visibleCategories.any { it.id == def }) def else visibleCategories.first().id
                viewModel.setCategoryId(fallback)
            }
        }

        val context = LocalContext.current
        val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }

        var showSimPicker by remember { mutableStateOf(false) }
        var pendingNumber by remember { mutableStateOf<String?>(null) }
        var showAllGroupMenuPortrait by remember { mutableStateOf(false) }
        var showAllGroupMenuLandscape by remember { mutableStateOf(false) }

        // Selection mode state - hoisted to parent

        // Track previous filter index for slide direction
        val filterEntries = CallLogFilter.entries
        var previousFilterIndex by remember { mutableIntStateOf(0) }

        val hiddenIds = remember(settingsVersion) {
            val raw = prefs.getString(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
            if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
        }

        val favoriteContactIds = remember(contacts) { contacts.filter { it.isFavorite }.map { it.id }.toSet() }
        val favoritePhoneNumbers = remember(contacts) { contacts.filter { it.isFavorite }.flatMap { it.phoneNumbers }.toSet() }

        val favoriteContacts = remember(contacts, hiddenIds) {
            contacts.filter { it.isFavorite && (hiddenIds.isEmpty() || it.id !in hiddenIds) }
        }
        val savedFavoritesOrder = remember(settingsVersion) {
            prefs.getString(PreferenceManager.KEY_FAVORITES_ORDER, null)
                ?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        }
        val orderedFavorites = remember(favoriteContacts, savedFavoritesOrder) {
            if (savedFavoritesOrder.isEmpty()) {
                favoriteContacts
            } else {
                val favMap = favoriteContacts.associateBy { it.id }
                val ordered = savedFavoritesOrder.mapNotNull { favMap[it] }.toMutableList()
                val remaining = favoriteContacts.filter { it.id !in savedFavoritesOrder }
                ordered.addAll(remaining)
                ordered
            }
        }
        var isExpanded by rememberSaveable { mutableStateOf(true) }

        val onFavoriteContactClick: (com.modiprincekr.thedialer.modal.data.Contact) -> Unit = { contact ->
            val phoneNumber = contact.phoneNumbers.firstOrNull()
            if (!phoneNumber.isNullOrBlank()) {
                placeCallHonoringContactSim(context, prefs, contact.id, phoneNumber) {
                    pendingNumber = phoneNumber
                    showSimPicker = true
                }
            } else {
                navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
            }
        }

        val onFavoriteContactLongClick: (com.modiprincekr.thedialer.modal.data.Contact) -> Unit = { contact ->
            val phoneNumber = contact.phoneNumbers.firstOrNull() ?: ""
            navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id, phoneNumber = phoneNumber))
        }

        val filteredLogs = remember(logs, selectedCategoryId, hiddenIds, favoriteContactIds, favoritePhoneNumbers, customCategories) {
            val base = if (hiddenIds.isEmpty()) logs else logs.filter { it.contactId == null || it.contactId !in hiddenIds }
            val custom = customCategories.firstOrNull { it.id == selectedCategoryId }
            if (custom != null) {
                base.filter { entry ->
                    custom.contactPhones.any { numbersLikelyMatch(entry.number, it) }
                }
            } else {
                when (selectedCategoryId) {
                    "all" -> base
                    "contacts" -> base.filter { it.name != null && it.name != it.number && !it.isCallerIdName }
                    "favourites" -> {
                        base.filter { log ->
                            (log.contactId != null && log.contactId in favoriteContactIds) ||
                            log.number in favoritePhoneNumbers
                        }
                    }
                    "unknown" -> base.filter { it.contactId.isNullOrBlank() || it.name.isNullOrEmpty() || it.name == it.number || it.isCallerIdName }
                    "missed" -> base.filter { it.type == CallLog.Calls.MISSED_TYPE || it.type == 5 /* REJECTED */ || it.types.any { t -> t == CallLog.Calls.MISSED_TYPE || t == 5 } }
                    "incoming" -> base.filter { it.type == CallLog.Calls.INCOMING_TYPE || it.type == 7 /* ANSWERED_EXTERNALLY */ || it.types.any { t -> t == CallLog.Calls.INCOMING_TYPE || t == 7 } }
                    "outgoing" -> base.filter { it.type == CallLog.Calls.OUTGOING_TYPE || it.types.any { t -> t == CallLog.Calls.OUTGOING_TYPE } }
                    "whatsapp" -> base.filter { it.isWhatsApp }
                    "alternate" -> base.filter { it.isAlternate || com.modiprincekr.thedialer.controller.util.AlternateNumberRepository.findByNumber(context, it.number) != null }
                    else -> base
                }
            }
        }
        val totalCallsMap = remember(logs) {
            val map = mutableMapOf<String, Int>()
            for (entry in logs) {
                val key = entry.contactId?.takeIf { it.isNotBlank() } ?: entry.number.filter { it.isDigit() }.ifEmpty { entry.number }
                map[key] = (map[key] ?: 0) + entry.count
            }
            map
        }
        val groupCallsByLatest = remember(settingsVersion) {
            prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)
        }
        val groupedLogs: Map<String, List<CallLogEntry>> = remember(filteredLogs, groupCallsByLatest) {
            if (groupCallsByLatest) {
                val groupedByNumber = LinkedHashMap<String, MutableList<CallLogEntry>>()
                for (entry in filteredLogs) {
                    val isMissed = entry.type == CallLog.Calls.MISSED_TYPE || entry.type == 5 || entry.types.any { it == CallLog.Calls.MISSED_TYPE || it == 5 }
                    val typeGroup = if (isMissed) "missed" else "normal"
                    val baseKey = entry.contactId?.takeIf { it.isNotBlank() }
                        ?: entry.number.filter { it.isDigit() }.ifEmpty { null }
                        ?: "unknown_${entry.date}_${entry.number}"
                    val key = "${baseKey}_$typeGroup"
                    groupedByNumber.getOrPut(key) { mutableListOf() }.add(entry)
                }
                val mergedList = groupedByNumber.values.map { list ->
                    val first = list.first()
                    if (list.size == 1) {
                        first
                    } else {
                        first.copy(
                            types = list.flatMap { it.types },
                            callIds = list.flatMap { it.callIds },
                            dates = list.flatMap { it.dates }
                        )
                    }
                }
                mapOf("" to mergedList)
            } else {
                filteredLogs.groupBy { formatDateHeader(it.date) }
            }
        }

        val contactsById = remember(contacts) {
            contacts.associateBy { it.id }
        }
        val contactsByDigits = remember(contacts) {
            val map = HashMap<String, com.modiprincekr.thedialer.modal.data.Contact>(contacts.size * 2)
            for (c in contacts) {
                for (n in c.phoneNumbers) {
                    val d = com.modiprincekr.thedialer.controller.util.normalizeNumberDigits(n)
                    if (d.isNotEmpty()) {
                        map[d] = c
                        if (d.length >= 7) {
                            map[d.takeLast(7)] = c
                        }
                    }
                }
            }
            map
        }

        val avatarConfig = com.modiprincekr.thedialer.view.components.rememberAvatarDisplayConfig(prefs, settingsVersion)
        val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
        val isSaturatedActive = remember(settingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
        val colorScheme = MaterialTheme.colorScheme
        val leftSwipeKey = remember(settingsVersion) { prefs.getSwipeAction("call_logs", "left") }
        val rightSwipeKey = remember(settingsVersion) { prefs.getSwipeAction("call_logs", "right") }
        val leftSwipeAction = remember(leftSwipeKey, colorScheme) { com.modiprincekr.thedialer.controller.util.SwipeActionHelper.resolveActionItem(leftSwipeKey, colorScheme) }
        val rightSwipeAction = remember(rightSwipeKey, colorScheme) { com.modiprincekr.thedialer.controller.util.SwipeActionHelper.resolveActionItem(rightSwipeKey, colorScheme) }

        val callLogDisplayConfig = remember(settingsVersion, groupCallsByLatest, hiddenIds, isSaturatedActive, leftSwipeKey, rightSwipeKey, leftSwipeAction, rightSwipeAction) {
            val fakeCallInContextMenu = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
            val use24HourTime = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false)
            val showTalkTime = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, false)
            val showTotalCallsMade = groupCallsByLatest || prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
            val hideNames = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false)
            val nameNonContactsAsUnknown = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, true)
            val showSimsSetting = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault())
            val sim1Color = Color(prefs.getInt(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SIM1_COLOR, com.modiprincekr.thedialer.controller.util.PreferenceManager.DEFAULT_SIM1_COLOR))
            val sim2Color = Color(prefs.getInt(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SIM2_COLOR, com.modiprincekr.thedialer.controller.util.PreferenceManager.DEFAULT_SIM2_COLOR))
            val isScrollAnimEnabled = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SCROLL_ANIMATION, true)
            val showAvatarsInCalls = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SHOW_AVATARS_CALLS, true)
            val showNumbersInCallLogs = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, true)
            val showMissedCallDuration = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_SHOW_MISSED_CALL_DURATION, false)

            com.modiprincekr.thedialer.view.components.CallLogDisplayConfig(
                use24HourTime = use24HourTime,
                showTalkTime = showTalkTime,
                showMissedCallDuration = showMissedCallDuration,
                groupCallsByLatest = groupCallsByLatest,
                showTotalCallsMade = showTotalCallsMade,
                showSims = showSimsSetting,
                hideNames = hideNames,
                hiddenIds = hiddenIds,
                nameNonContactsAsUnknown = nameNonContactsAsUnknown,
                fakeCallInContextMenu = fakeCallInContextMenu,
                showAvatars = showAvatarsInCalls,
                sim1Color = sim1Color,
                sim2Color = sim2Color,
                isScrollAnimEnabled = isScrollAnimEnabled,
                showNumbersInCallLogs = showNumbersInCallLogs,
                isSaturatedActive = isSaturatedActive,
                leftSwipeKey = leftSwipeKey,
                rightSwipeKey = rightSwipeKey,
                leftSwipeAction = leftSwipeAction,
                rightSwipeAction = rightSwipeAction
            )
        }

        if (showSelectionDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { onShowSelectionDeleteConfirmChange(false) },
                title = { Text(com.modiprincekr.thedialer.controller.util.tr("Delete entries?")) },
                text = { Text(com.modiprincekr.thedialer.controller.util.tr("This will permanently delete the selected call log entries.")) },
                confirmButton = {
                    Button(
                        onClick = {
                            onShowSelectionDeleteConfirmChange(false)
                            val entriesToDelete = groupedLogs.values.flatten().filter { "${it.number}|${it.date}" in selectedLogs }
                            if (entriesToDelete.isNotEmpty()) {
                                viewModel.deleteCallLogs(entriesToDelete)
                            } else {
                                viewModel.deleteCallLogsByKeys(selectedLogs)
                            }
                            onSelectedLogsChange(emptySet())
                            onSelectionModeChange(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(com.modiprincekr.thedialer.controller.util.tr("Delete")) }
                },
                dismissButton = { TextButton(onClick = { onShowSelectionDeleteConfirmChange(false) }) { Text(com.modiprincekr.thedialer.controller.util.tr("Cancel")) } }
            )
        }

        // Bug fix: a new call (e.g. the first call of a new day, when the last entry in the list
        // was from yesterday) gets correctly prepended to the top of `logs` the moment it's
        // detected, but the LazyColumn's scroll position doesn't move on its own — if the user
        // was sitting at/near the top already, the new "Today" section is inserted *above* the
        // current viewport and never becomes visible until they manually scroll up, which reads
        // as "the call log isn't updating". Detect exactly that case (the very top entry's
        // identity changed) and, only when the user was already near the top of the list, scroll
        // them to it automatically. If they've scrolled down into older history, leave their
        // position alone rather than yanking them back up.
        val topEntryKey = remember(groupedLogs) {
            groupedLogs.entries.firstOrNull()?.value?.firstOrNull()?.let { "${it.number}_${it.date}" }
        }
        var lastKnownTopEntryKey by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(topEntryKey) {
            val previous = lastKnownTopEntryKey
            lastKnownTopEntryKey = topEntryKey
            if (previous != null && topEntryKey != null && topEntryKey != previous &&
                listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0 &&
                !listState.isScrollInProgress
            ) {
                listState.animateScrollToItem(0)
            }
        }


        if (showSimPicker && pendingNumber != null) {
            SimPickerDialog(
                onDismissRequest = { showSimPicker = false },
                number = pendingNumber,
                onSimSelected = { handle ->
                    makeCall(context, pendingNumber!!, handle)
                    showSimPicker = false
                }
            )
        }

        if (logs.isEmpty()) {
            // Only show a spinner on the very first launch when no disk cache exists.
            // On subsequent opens the disk cache fills instantly so this won't be seen.
            var showSpinner by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                // Give the disk cache ~200ms to arrive; only show spinner if still empty
                kotlinx.coroutines.delay(200)
                showSpinner = true
            }
            if (isLandscape) {
                com.modiprincekr.thedialer.view.components.SearchBarPill(
                    navigator = navigator,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (showSpinner) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                }
            }
        } else {
            // Use start-of-day (midnight) for "today" so all four stat cards
            // consistently reflect the current calendar day.
            val todayStart = remember(logs) { todayStartMillis() }
            val todayLogs  = remember(logs, todayStart) { logs.filter { it.date >= todayStart } }
            val missedResetTime = remember(settingsVersion) {
                prefs.getLong(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_MISSED_CALL_RESET_TIME, 0L)
            }

            val totalToday = remember(todayLogs) { todayLogs.sumOf { it.count } }
            val incomingToday = remember(todayLogs) {
                todayLogs.sumOf { entry ->
                    if (entry.types.isNotEmpty()) {
                        entry.types.count { it == CallLog.Calls.INCOMING_TYPE || it == 7 /* ANSWERED_EXTERNALLY */ }
                    } else {
                        if (entry.type == CallLog.Calls.INCOMING_TYPE || entry.type == 7) 1 else 0
                    }
                }
            }
            val missedToday = remember(todayLogs, missedResetTime) {
                todayLogs.sumOf { entry ->
                    if (entry.types.isNotEmpty()) {
                        entry.types.count { t -> (t == CallLog.Calls.MISSED_TYPE || t == 5 /* REJECTED */) && entry.date > missedResetTime }
                    } else {
                        if ((entry.type == CallLog.Calls.MISSED_TYPE || entry.type == 5) && entry.date > missedResetTime) 1 else 0
                    }
                }
            }
            val outgoingToday = remember(todayLogs) {
                todayLogs.sumOf { entry ->
                    if (entry.types.isNotEmpty()) {
                        entry.types.count { it == CallLog.Calls.OUTGOING_TYPE }
                    } else {
                        if (entry.type == CallLog.Calls.OUTGOING_TYPE) 1 else 0
                    }
                }
            }
            val totalDurationToday = remember(todayLogs) {
                todayLogs.filter { it.duration > 0 }.sumOf { it.duration }
            }

            Column(modifier = Modifier.fillMaxSize()) {

                // Stat cards – visibility and order controlled by Call UI settings
                val showToday    = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_TODAY, true) }
                val showIncoming = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_INCOMING, true) }
                val showMissed   = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_MISSED, true) }
                val showOutgoing = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true) }
                val showCallTime = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true) }
                val showContacts = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_CONTACTS, false) }
                val showGroups   = remember(settingsVersion) { prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_GROUPS, false) }
                val callUIOrder  = remember(settingsVersion) {
                    com.modiprincekr.thedialer.controller.util.PreferenceManager.parseCallUIOrder(
                        prefs.getString(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_CALL_UI_ORDER, null)
                    )
                }
                val contactsVM: ContactsViewModel = koinActivityViewModel()
                val contactsList by contactsVM.allContacts.collectAsState()
                val contactsCount = contactsList.size
                val contactGroups by contactsVM.contactGroups.collectAsState()
                val hiddenGroupIds = remember(settingsVersion) { prefs.getHiddenContactGroupIds() }
                val enabledAccountKeys by contactsVM.enabledAccountKeys.collectAsState()
                val visibleContactGroups = remember(contactGroups, hiddenGroupIds, enabledAccountKeys) {
                    contactGroups.filter { it.id !in hiddenGroupIds }.filter { group ->
                        isGroupMatchingAccountFilter(group, enabledAccountKeys)
                    }
                }
                val groupsCount = visibleContactGroups.size

                val visibleStatCards = remember(callUIOrder, showToday, showIncoming, showMissed, showOutgoing, showCallTime, showContacts, showGroups) {
                    callUIOrder.filter { key ->
                        when (key) {
                            "today"     -> showToday
                            "incoming"  -> showIncoming
                            "missed"    -> showMissed
                            "outgoing"  -> showOutgoing
                            "call_time" -> showCallTime
                            "contacts"  -> showContacts
                            "groups"    -> showGroups
                            else        -> false
                        }
                    }
                }

                @Composable
                fun StatCardItem(cardKey: String, delayMs: Long) {
                    when (cardKey) {
                        "today" -> AnimatedStatCard(delayMs, com.modiprincekr.thedialer.controller.util.tr("Today"), totalToday.toString(), Icons.Rounded.Call, ColorBlue, Modifier.size(110.dp)) { viewModel.setFilter(CallLogFilter.All) }
                        "incoming" -> AnimatedStatCard(delayMs, com.modiprincekr.thedialer.controller.util.tr("Incoming"), incomingToday.toString(), Icons.Rounded.CallReceived, Color(0xFF2196F3), Modifier.size(110.dp)) { viewModel.setFilter(CallLogFilter.Incoming) }
                        "missed" -> AnimatedStatCard(
                            delayMs = delayMs,
                            label = com.modiprincekr.thedialer.controller.util.tr("Missed"),
                            value = missedToday.toString(),
                            icon = Icons.AutoMirrored.Filled.CallMissed,
                            iconTint = ColorRed,
                            modifier = Modifier.size(110.dp),
                            containerColor = if (missedToday > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            onClick = { viewModel.setFilter(CallLogFilter.Missed) },
                            onLongClick = {
                                if (prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_APP_HAPTICS, true)) {
                                    performAppHaptic(context, prefs.getString(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                }
                                prefs.setLong(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_MISSED_CALL_RESET_TIME, System.currentTimeMillis())
                                com.modiprincekr.thedialer.controller.util.MissedCallBadgeManager.markMissedCallsAsRead(context)
                            }
                        )
                        "outgoing" -> AnimatedStatCard(delayMs, com.modiprincekr.thedialer.controller.util.tr("Outgoing"), outgoingToday.toString(), Icons.AutoMirrored.Filled.CallMade, ColorGreen, Modifier.size(110.dp)) { viewModel.setFilter(CallLogFilter.Outgoing) }
                        "call_time" -> AnimatedStatCard(delayMs, com.modiprincekr.thedialer.controller.util.tr("Call Time"), if (totalDurationToday > 0) formatDuration(totalDurationToday) else "0s", Icons.Default.Timer, ColorOrange, Modifier.size(110.dp)) { viewModel.setFilter(CallLogFilter.Incoming) }
                        "contacts" -> AnimatedStatCard(delayMs, com.modiprincekr.thedialer.controller.util.tr("Contacts"), if (contactsCount > 0) contactsCount.toString() else "Open", Icons.Default.People, ColorPurple, Modifier.size(110.dp)) {
                            navController.navigate(ContactScreenDestination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "groups" -> AnimatedStatCard(delayMs, com.modiprincekr.thedialer.controller.util.tr("Groups"), if (groupsCount > 0) groupsCount.toString() else "Open", Icons.Default.Group, Color(0xFF00897B), Modifier.size(110.dp)) {
                            navController.navigate(GroupsScreenDestination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }

                // In portrait, render stat cards and pills above the list (sticky)
                // In landscape, they go inside the LazyColumn so they scroll with content
                if (!isLandscape) {
                    if (visibleStatCards.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(visibleStatCards, key = { it }) { cardKey ->
                                val idx = visibleStatCards.indexOf(cardKey)
                                StatCardItem(cardKey, (idx * 60).toLong())
                            }
                        }
                    }

                    if (orderedFavorites.isNotEmpty()) {
                        FavouritesSection(
                            favorites = orderedFavorites,
                            isExpanded = isExpanded,
                            onToggleExpand = { isExpanded = !isExpanded },
                            onContactClick = onFavoriteContactClick,
                            onContactLongClick = onFavoriteContactLongClick
                        )
                    }

                    // ── Filter pills ──────────────────────────────────────────────
                    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                    val isSaturatedActive = remember(settingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
                    val isDynamic = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                    val usePrimary = isSaturatedActive || !isDynamic
                    val solidIcons = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false) }
                    val solidStyle = remember(settingsVersion, isDark) { prefs.getSolidIconsStyle(isDark) }
                    val isSaturatedSolidBrightDark = isDark && isSaturatedActive && solidIcons && (solidStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT)
                    val activeChipBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    val activeChipFg = if (isSaturatedSolidBrightDark) Color.Black else if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleCategories, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) activeChipBg
                                              else MaterialTheme.colorScheme.surfaceContainerLow,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "chipColor"
                            )
                            val labelColor by animateColorAsState(
                                targetValue = if (isSelected) activeChipFg
                                              else MaterialTheme.colorScheme.onSurface,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "chipLabelColor"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.08f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "chipScale"
                            )
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = containerColor,
                                    modifier = Modifier
                                        .scale(scale)
                                        .clip(RoundedCornerShape(50.dp))
                                        .combinedClickable(
                                            onClick = {
                                                previousFilterIndex = visibleCategories.indexOfFirst { it.id == selectedCategoryId }
                                                viewModel.setCategoryId(cat.id)
                                            },
                                            onLongClick = if (cat.id == "all") {
                                                {
                                                    if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                        performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                                    }
                                                    showAllGroupMenuPortrait = true
                                                }
                                            } else null
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            com.modiprincekr.thedialer.controller.util.tr(cat.name),
                                            color = labelColor,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                if (cat.id == "all") {
                                    DropdownMenu(
                                        expanded = showAllGroupMenuPortrait,
                                        onDismissRequest = { showAllGroupMenuPortrait = false },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Grouped based on date") },
                                            onClick = {
                                                if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                    performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                                }
                                                prefs.setBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)
                                                prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
                                                showAllGroupMenuPortrait = false
                                            },
                                            leadingIcon = {
                                                if (!groupCallsByLatest) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                } else {
                                                    Spacer(modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Grouped based on number") },
                                            onClick = {
                                                if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                    performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                                }
                                                prefs.setBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, true)
                                                prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, true)
                                                showAllGroupMenuPortrait = false
                                            },
                                            leadingIcon = {
                                                if (groupCallsByLatest) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                } else {
                                                    Spacer(modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Animated content: slides left/right on filter change ──────
                // On the very first data load (startup) we use a slow fade-in so the
                // list appears gracefully instead of jumping. A data-only refresh (e.g. a
                // call log entry appearing right after a call ends) applies instantly with
                // no animation — it should never feel like something is "loading". Only an
                // actual filter change (All/Missed/etc.) gets the slide transition.
                var hasLoadedOnce by remember { mutableStateOf(false) }
                AnimatedContent(
                    targetState = selectedCategoryId,
                    transitionSpec = {
                        val filterChanged = initialState != targetState
                        if (!hasLoadedOnce) {
                            // Startup: slow gentle fade, no slide
                            fadeIn(animationSpec = tween(600, easing = LinearOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(0))
                        } else if (!filterChanged) {
                            // Data-only refresh: instant, no animation whatsoever
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val currentIdx = visibleCategories.indexOfFirst { it.id == targetState }
                            val prevIdx = visibleCategories.indexOfFirst { it.id == initialState }
                            val goingRight = currentIdx > prevIdx
                            if (goingRight) {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) togetherWith slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            } else {
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) togetherWith slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "filterSlide"
                ) { _ ->
                    LaunchedEffect(Unit) { hasLoadedOnce = true }
                    ScrollHapticsEffect(listState = listState)
                    CompositionLocalProvider(com.modiprincekr.thedialer.view.components.LocalAvatarDisplayConfig provides avatarConfig) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // In landscape, stat cards and filter pills scroll with the list
                        if (isLandscape) {
                            item(key = "search_bar_pill", contentType = "searchBar") {
                                com.modiprincekr.thedialer.view.components.SearchBarPill(
                                    navigator = navigator,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            if (visibleStatCards.isNotEmpty()) {
                                item(key = "stat_cards", contentType = "statCards") {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(visibleStatCards, key = { it }) { cardKey ->
                                            val idx = visibleStatCards.indexOf(cardKey)
                                            StatCardItem(cardKey, (idx * 60).toLong())
                                        }
                                    }
                                }
                            }
                            if (orderedFavorites.isNotEmpty()) {
                                item(key = "favourites_section", contentType = "favouritesSection") {
                                    FavouritesSection(
                                        favorites = orderedFavorites,
                                        isExpanded = isExpanded,
                                        onToggleExpand = { isExpanded = !isExpanded },
                                        onContactClick = onFavoriteContactClick,
                                        onContactLongClick = onFavoriteContactLongClick
                                    )
                                }
                            }
                            item(key = "filter_pills", contentType = "filterPills") {
                                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                                val isSaturatedActive = remember(settingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
                                val isDynamic = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                                val usePrimary = isSaturatedActive || !isDynamic
                                val solidIcons = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false) }
                                val solidStyle = remember(settingsVersion, isDark) { prefs.getSolidIconsStyle(isDark) }
                                val isSaturatedSolidBrightDark = isDark && isSaturatedActive && solidIcons && (solidStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT)
                                val activeChipBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                                val activeChipFg = if (isSaturatedSolidBrightDark) Color.Black else if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(visibleCategories, key = { it.id }) { cat ->
                                        val isSelected = selectedCategoryId == cat.id
                                        val containerColor by animateColorAsState(
                                            targetValue = if (isSelected) activeChipBg
                                                          else MaterialTheme.colorScheme.surfaceContainerLow,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "chipColor"
                                        )
                                        val labelColor by animateColorAsState(
                                            targetValue = if (isSelected) activeChipFg
                                                          else MaterialTheme.colorScheme.onSurface,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "chipLabelColor"
                                        )
                                        val scale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.08f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                                            label = "chipScale"
                                        )
                                        Box {
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = containerColor,
                                                modifier = Modifier
                                                    .scale(scale)
                                                    .clip(RoundedCornerShape(50.dp))
                                                    .combinedClickable(
                                                        onClick = {
                                                            previousFilterIndex = visibleCategories.indexOfFirst { it.id == selectedCategoryId }
                                                            viewModel.setCategoryId(cat.id)
                                                        },
                                                        onLongClick = if (cat.id == "all") {
                                                            {
                                                                if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                                    performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                                                }
                                                                showAllGroupMenuLandscape = true
                                                            }
                                                        } else null
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .padding(horizontal = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        com.modiprincekr.thedialer.controller.util.tr(cat.name),
                                                        color = labelColor,
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }

                                            if (cat.id == "all") {
                                                DropdownMenu(
                                                    expanded = showAllGroupMenuLandscape,
                                                    onDismissRequest = { showAllGroupMenuLandscape = false },
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Grouped based on date") },
                                                        onClick = {
                                                            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                                performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                                            }
                                                            prefs.setBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)
                                                            prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
                                                            showAllGroupMenuLandscape = false
                                                        },
                                                        leadingIcon = {
                                                            if (!groupCallsByLatest) {
                                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                            } else {
                                                                Spacer(modifier = Modifier.size(24.dp))
                                                            }
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Grouped based on number") },
                                                        onClick = {
                                                            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                                performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                                                            }
                                                            prefs.setBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, true)
                                                            prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, true)
                                                            showAllGroupMenuLandscape = false
                                                        },
                                                        leadingIcon = {
                                                            if (groupCallsByLatest) {
                                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                            } else {
                                                                Spacer(modifier = Modifier.size(24.dp))
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val directCall = prefs.getBoolean(com.modiprincekr.thedialer.controller.util.PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)
                        groupedLogs.forEach { (header, logsInGroup) ->
                            // Section header as its own item
                            if (header.isNotBlank()) {
                                item(key = "header_$header", contentType = "sectionHeader") {
                                    RivoScrollAnimatedItem(enabled = callLogDisplayConfig.isScrollAnimEnabled) {
                                        RivoSectionHeader(title = header)
                                    }
                                }
                            } else {
                                item(key = "header_top_spacer") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                            // Individual items per log entry with per-item rounded corners
                            itemsIndexed(
                                items = logsInGroup,
                                key = { _, lg -> lg.callIds.firstOrNull()?.toString() ?: "${lg.number}_${lg.date}" },
                                contentType = { _, _ -> "callLogEntry" }
                            ) { index, lg ->
                                val isFirst = index == 0
                                val isLast = index == logsInGroup.size - 1
                                val cornerRadius = 28.dp
                                val topStart = if (isFirst) cornerRadius else 0.dp
                                val topEnd = if (isFirst) cornerRadius else 0.dp
                                val bottomStart = if (isLast) cornerRadius else 0.dp
                                val bottomEnd = if (isLast) cornerRadius else 0.dp
                                RivoScrollAnimatedItem(delayMs = 0L, enabled = callLogDisplayConfig.isScrollAnimEnabled) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = topStart,
                                                topEnd = topEnd,
                                                bottomStart = bottomStart,
                                                bottomEnd = bottomEnd
                                            ),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                if (!isFirst) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                        thickness = 0.5.dp
                                                    )
                                                }
                                                val contactKey = lg.contactId?.takeIf { it.isNotBlank() } ?: lg.number.filter { it.isDigit() }.ifEmpty { lg.number }
                                                val matchedContact = if (!lg.contactId.isNullOrBlank() && lg.contactId != "0" && lg.contactId != "-1" && lg.contactId != "null") {
                                                    contactsById[lg.contactId]
                                                } else if (lg.number.isNotBlank()) {
                                                    val digits = com.modiprincekr.thedialer.controller.util.normalizeNumberDigits(lg.number)
                                                    contactsByDigits[digits] ?: if (digits.length >= 7) contactsByDigits[digits.takeLast(7)] else null
                                                } else null
                                                CallLogTile(
                                                    log = lg,
                                                    config = callLogDisplayConfig,
                                                    totalCallsCount = totalCallsMap[contactKey] ?: lg.count,
                                                    isSelected = selectedLogs.contains("${lg.number}|${lg.date}"),
                                                    selectionMode = selectionMode,
                                                    navigator = navigator,
                                                    matchedContact = matchedContact,
                                                    onSelectToggle = { log ->
                                                        val key = "${log.number}|${log.date}"
                                                        onSelectedLogsChange(if (selectedLogs.contains(key)) selectedLogs - key else selectedLogs + key)
                                                    },
                                                    onSelectMode = { log ->
                                                        onSelectionModeChange(true)
                                                        val key = "${log.number}|${log.date}"
                                                        onSelectedLogsChange(setOf(key))
                                                    },
                                                    onTileClick = { log ->
                                                        if (selectionMode) {
                                                            val key = "${log.number}|${log.date}"
                                                            onSelectedLogsChange(if (selectedLogs.contains(key)) selectedLogs - key else selectedLogs + key)
                                                        } else if (directCall) {
                                                            if (log.isWhatsApp) {
                                                                if (log.isVideo) {
                                                                    com.modiprincekr.thedialer.controller.util.launchDirectWhatsAppVideoCall(context, log.number)
                                                                } else {
                                                                    com.modiprincekr.thedialer.controller.util.startWhatsAppVoiceCall(context, log.number)
                                                                }
                                                            } else {
                                                                val recentSlot = if (log.simSlot in 0..1) log.simSlot else null
                                                                placeCallHonoringContactSim(context, prefs, log.contactId ?: log.number, log.number, recentSlot) {
                                                                    pendingNumber = log.number; showSimPicker = true
                                                                }
                                                            }
                                                        } else {
                                                            navigator.navigate(ContactDetailsScreenDestination(contactId = log.contactId ?: "null", phoneNumber = log.number))
                                                        }
                                                    },
                                                    onAvatarClick = { log ->
                                                        navigator.navigate(ContactDetailsScreenDestination(contactId = log.contactId ?: "null", phoneNumber = log.number))
                                                    },
                                                    onButtonClick = { log ->
                                                        if (log.isWhatsApp) {
                                                            if (log.isVideo) {
                                                                com.modiprincekr.thedialer.controller.util.launchDirectWhatsAppVideoCall(context, log.number)
                                                            } else {
                                                                com.modiprincekr.thedialer.controller.util.startWhatsAppVoiceCall(context, log.number)
                                                            }
                                                        } else {
                                                            val recentSlot = if (log.simSlot in 0..1) log.simSlot else null
                                                            placeCallHonoringContactSim(context, prefs, log.contactId ?: log.number, log.number, recentSlot) {
                                                                pendingNumber = log.number; showSimPicker = true
                                                            }
                                                        }
                                                    },
                                                    onDelete = { viewModel.refreshLogs() }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isLast) Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    }
                }
            }
        }
    } else {
        PermissionDeniedView(
            icon = Icons.Default.Call,
            title = "Call History",
            description = "The Dialer needs access to your call logs to show your recent activity and missed calls.",
            onGrantClick = onRequestPermission
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AnimatedStatCard(
    delayMs: Long,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs); visible = true }
    val cardAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "statAlpha")
    val cardOffset by animateDpAsState(if (visible) 0.dp else 16.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "statOffset")
    Box(modifier = Modifier.alpha(cardAlpha).offset(y = cardOffset)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            RivoStatCard(label = label, value = value, icon = icon, iconTint = iconTint, containerColor = containerColor, modifier = Modifier.fillMaxSize())
        }
    }
}
