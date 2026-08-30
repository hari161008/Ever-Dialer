package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.res.Configuration
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.formatDateHeader
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallHonoringContactSim
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogFilter
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import android.os.Build
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.lens
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.colorControls
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Calendar
import java.util.Locale

private val ColorBlue   = Color(0xFF2196F3)
private val ColorRed    = Color(0xFFE91E63)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true, style = TabTransitionStyle::class)
@Composable
fun RecentScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val prefs = koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager>()
    val pillNav = remember { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_PILL_NAV, true) }

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

    val fabScale by animateFloatAsState(
        targetValue = if (fabVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )
    LaunchedEffect(Unit) {
        fabVisible = true
        if (prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) {
            showDialpad = true
        }
    }

    // Bug fix: the call log list previously only ever updated via the system ContentObserver
    // (which can lag well behind the moment a call actually starts/ends) or by watching
    // CallService's session state end-to-end — nothing re-fetched just from the user coming
    // back to this screen/app, which is why re-opening the app right after a call, or bringing
    // it to the foreground mid-call, could show a stale list until something else (like a
    // scroll, which happens to trigger recomposition) forced it to catch up. Explicitly
    // re-fetch every time this screen becomes visible again so the list is always current the
    // instant it's looked at, with no scroll needed. Uses the same activity-scoped
    // CallLogViewModel instance that CallLogFullContent below reads from.
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                callLogViewModel.refreshLogs()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDialpad) {
        val dialpadSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val dialpadScope = rememberCoroutineScope()
        // See DialPadContent's `closing` param doc — flipping this (rather than clearing focus
        // from out here) is what actually reaches the search field's real focus manager, since
        // the sheet renders in its own separate Dialog window.
        var dialpadClosing by remember { mutableStateOf(false) }
        // Catch a swipe-down or scrim-tap dismiss the moment the drag/tap decides to close the
        // sheet (sheetState.targetValue flips to Hidden), not only once onDismissRequest fires
        // after the sheet's own hide animation has already finished playing — otherwise the
        // search field keeps its focus and the keyboard stays up for that whole animation before
        // finally hiding. See DialPadScreen's identical fix.
        //
        // sheetState.targetValue actually starts at Hidden too, for the single frame before the
        // sheet's own internal LaunchedEffect calls show() to open it — so naively treating
        // "target == Hidden" as "closing" misfired on that very first frame and permanently
        // blocked the search field from ever focusing (via DialPadContent's `closing`-gated
        // focusProperties) even on a fresh open. hasOpenedOnce guards against that: we only start
        // treating a Hidden target as a real close once the sheet has reached non-Hidden once.
        var hasOpenedOnce by remember { mutableStateOf(false) }
        LaunchedEffect(dialpadSheetState) {
            snapshotFlow { dialpadSheetState.targetValue }.collect { target ->
                if (target != SheetValue.Hidden) {
                    hasOpenedOnce = true
                } else if (hasOpenedOnce) {
                    dialpadClosing = true
                }
            }
        }
        // ModalBottomSheet.animateTo(targetValue, animationSpec) is internal, so we drive our own
        // slow, smooth slide + scrim fade over the content instead. Fading the scrim's alpha down
        // alongside the slide (not just translating the content) avoids the dim background
        // cutting out abruptly the instant the Dialog is torn down at the end — leaving the
        // system's own (quicker) swipe-to-dismiss animation untouched.
        val closeProgress = remember { Animatable(0f) }
        val slowSlideSpec = tween<Float>(durationMillis = 650, easing = FastOutSlowInEasing)
        // Fixed dp offsets don't clear the screen on taller devices, so the sheet gets torn down
        // mid-slide and appears to vanish abruptly. Basing it on the actual screen height (plus a
        // buffer for the status/nav bars) guarantees it's fully off-screen before removal.
        val dialpadConfiguration = LocalConfiguration.current
        val slideDistance = dialpadConfiguration.screenHeightDp.dp + 150.dp
        var didNavigateAway by remember { mutableStateOf(false) }

        fun finishDismissDialpad() {
            if (!didNavigateAway) showDialpad = false
        }

        // Swipe-down / scrim tap / predictive back — sheetState has already played its own
        // (quicker) hide animation by the time onDismissRequest fires, so just finish up.
        fun animateDismissDialpad() {
            dialpadClosing = true
            finishDismissDialpad()
        }

        // Used by the X button — plays our own slow, smooth slide-down.
        fun closeWithSlowAnimation() {
            if (didNavigateAway) return
            dialpadClosing = true
            dialpadScope.launch {
                closeProgress.animateTo(1f, slowSlideSpec)
            }.invokeOnCompletion {
                finishDismissDialpad()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { animateDismissDialpad() },
            sheetState = dialpadSheetState,
            // The default containerColor/dragHandle paint their own background surface behind
            // whatever we put in the content slot, so animating only our content left that
            // background fixed while just the elements slid down. Making the sheet transparent
            // and drawing our own background + handle inside the offset Surface (below) makes
            // the background move together with the content as one unit.
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            // No dim behind the sheet.
            scrimColor = Color.Transparent,
            // Stop the sheet from reserving its own system-bar inset padding around the content —
            // that reserved space stayed transparent once containerColor became Transparent,
            // showing as a gap between the sheet's visible bottom and the actual screen edge. We
            // apply the navigation-bar inset ourselves inside the Surface below instead, so the
            // background extends all the way to the screen edge and only the content is padded
            // above it.
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            dragHandle = null
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = slideDistance * closeProgress.value),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // System back should feel identical to tapping the X — the same slow, smooth
                // slide-down — instead of the sheet's own quicker default dismiss.
                BackHandler(enabled = true) { closeWithSlowAnimation() }
                Column(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(width = 36.dp, height = 4.dp)) {}
                    }
                    DialPadContent(
                        navigator = navigator,
                        onDismiss = { closeWithSlowAnimation() },
                        closing = dialpadClosing,
                        // Play our own slow, smooth slide-down before navigating, same as the main
                        // Dialpad screen does, so tapping a pfp in search results animates smoothly
                        // into Contact Info instead of popping in instantly.
                        onNavigateToContact = { contactId, phoneNumber ->
                            didNavigateAway = true
                            dialpadScope.launch {
                                closeProgress.animateTo(1f, slowSlideSpec)
                            }.invokeOnCompletion {
                                showDialpad = false
                                navigator.navigate(ContactDetailsScreenDestination(contactId = contactId, phoneNumber = phoneNumber))
                            }
                        }
                    )
                }
            }
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
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
                                !childHScrolling &&
                                elapsed >= 150L &&
                                kotlin.math.abs(dx) > 700f &&
                                kotlin.math.abs(dx) > kotlin.math.abs(dy) * 5.5f
                            ) {
                                triggered = true
                                if (dx < 0) {
                                    scope.launch {
                                        navController.navigate(ContactScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        navController.navigate(FavoritesScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
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
            val globalBackdrop = LocalLiquidGlassBackdrop.current
            val settingsVer by prefs.settingsChanged.collectAsState()
            val liquidGlass = remember(settingsVer) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_LIQUID_GLASS, false) }
            val lgRecentsFab = remember(settingsVer) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_LG_RECENTS_FAB, false) }
            val blurEffects = remember(settingsVer) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_BLUR_EFFECTS, false) }
            val blurRecentsFab = remember(settingsVer) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_BLUR_RECENTS_FAB, false) }
            val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
            val isSaturatedDark = remember(settingsVer, isDark) { isDark && prefs.isSaturatedForTheme(isDark) }
            val fabBg = if (isSaturatedDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            val fabFg = if (isSaturatedDark) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onPrimaryContainer
            val fabShape = RoundedCornerShape(17.dp)
            val useLiquidGlass = liquidGlass && lgRecentsFab && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
            val useBlur = blurEffects && blurRecentsFab && !useLiquidGlass
            val baseModifier = Modifier
                .scale(fabScale)
                .then(if (pillNav) Modifier.navigationBarsPadding().padding(bottom = 92.dp) else Modifier)
                .then(if (isLandscape) Modifier.navigationBarsPadding().padding(bottom = 8.dp) else Modifier)
            if (useLiquidGlass && globalBackdrop != null) {
                Box(
                    modifier = baseModifier.drawBackdrop(
                        backdrop = globalBackdrop,
                        shape = { fabShape },
                        effects = {
                            val d = density
                            colorControls(brightness = -0.15f)
                            lens(refractionHeight = 46f * d, refractionAmount = 64f * d)
                        },
                        highlight = { Highlight.Default }
                    )
                ) {
                    FloatingActionButton(
                        onClick = { showDialpad = true },
                        containerColor = fabBg.copy(alpha = 0.0f),
                        contentColor = fabFg,
                        shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp,
                        focusedElevation = 6.dp,
                        hoveredElevation = 6.dp
                    ),
                    ) { Icon(Icons.Default.Dialpad, "Dialpad") }
                }
            } else {
                FloatingActionButton(
                    onClick = { showDialpad = true },
                    containerColor = if (useBlur)
                        fabBg.copy(alpha = 0.75f)
                    else
                        fabBg,
                    contentColor = fabFg,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp,
                        focusedElevation = 6.dp,
                        hoveredElevation = 6.dp
                    ),
                    modifier = baseModifier
                ) { Icon(Icons.Default.Dialpad, "Dialpad") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
    val prefs = koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isGranted) {
        val viewModel: CallLogViewModel = koinActivityViewModel()
        val logs by viewModel.allCallLogs.collectAsState()
        val selectedFilter by viewModel.selectedFilter.collectAsState()
        val context = LocalContext.current
        val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

        var showSimPicker by remember { mutableStateOf(false) }
        var pendingNumber by remember { mutableStateOf<String?>(null) }

        // Selection mode state - hoisted to parent

        if (showSelectionDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { onShowSelectionDeleteConfirmChange(false) },
                title = { Text("Delete ${selectedLogs.size} entries?") },
                text = { Text("This will permanently delete the selected call log entries.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onShowSelectionDeleteConfirmChange(false)
                            selectedLogs.forEach { key ->
                                val parts = key.split("|", limit = 2)
                                if (parts.size == 2) {
                                    try {
                                        context.contentResolver.delete(
                                            android.provider.CallLog.Calls.CONTENT_URI,
                                            "${android.provider.CallLog.Calls.NUMBER} = ? AND ${android.provider.CallLog.Calls.DATE} = ?",
                                            arrayOf(parts[0], parts[1])
                                        )
                                    } catch (_: Exception) {}
                                }
                            }
                            onSelectedLogsChange(emptySet())
                            onSelectionModeChange(false)
                            viewModel.refreshLogs()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { onShowSelectionDeleteConfirmChange(false) }) { Text("Cancel") } }
            )
        }

        // Track previous filter index for slide direction
        val filterEntries = CallLogFilter.entries
        var previousFilterIndex by remember { mutableIntStateOf(filterEntries.indexOf(selectedFilter)) }

        val filteredLogs = remember(logs, selectedFilter) {
            when (selectedFilter) {
                CallLogFilter.All -> logs
                CallLogFilter.Missed -> logs.filter { it.type == CallLog.Calls.MISSED_TYPE }
                CallLogFilter.Incoming -> logs.filter { it.type == CallLog.Calls.INCOMING_TYPE }
                CallLogFilter.Outgoing -> logs.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
                CallLogFilter.Contacts -> logs.filter { it.name != null && it.name != it.number }
            }
        }
        val groupedLogs = remember(filteredLogs) { filteredLogs.groupBy { formatDateHeader(it.date) } }

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
                listState.firstVisibleItemIndex <= 1
            ) {
                listState.animateScrollToItem(0)
            }
        }

        if (showSimPicker && pendingNumber != null) {
            SimPickerDialog(
                onDismissRequest = { showSimPicker = false },
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
                com.coolappstore.everdialer.by.svhp.view.components.SearchBarPill(
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
            val todayStart = remember { todayStartMillis() }
            val todayLogs  = remember(logs) { logs.filter { it.date >= todayStart } }

            val totalToday        = remember(todayLogs) { todayLogs.size }
            val missedToday       = remember(todayLogs) { todayLogs.count { it.type == CallLog.Calls.MISSED_TYPE } }
            val outgoingToday     = remember(todayLogs) { todayLogs.count { it.type == CallLog.Calls.OUTGOING_TYPE } }
            val totalDurationToday = remember(todayLogs) {
                todayLogs.filter { it.duration > 0 }.sumOf { it.duration }
            }

            Column(modifier = Modifier.fillMaxSize()) {

                // Stat cards – visibility controlled by Call UI settings
                val showToday    = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_TODAY, true) }
                val showMissed   = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_MISSED, true) }
                val showOutgoing = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true) }
                val showCallTime = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true) }

                // In portrait, render stat cards and pills above the list (sticky)
                // In landscape, they go inside the LazyColumn so they scroll with content
                if (!isLandscape) {
                    if (showToday || showMissed || showOutgoing || showCallTime) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (showToday) item { AnimatedStatCard(0L, "Today", totalToday.toString(), Icons.AutoMirrored.Filled.CallReceived, ColorBlue, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.All) } }
                            if (showMissed) item { AnimatedStatCard(60L, "Missed", missedToday.toString(), Icons.AutoMirrored.Filled.CallMissed, ColorRed, Modifier.width(110.dp),
                                if (missedToday > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow
                            ) { viewModel.setFilter(CallLogFilter.Missed) } }
                            if (showOutgoing) item { AnimatedStatCard(120L, "Outgoing", outgoingToday.toString(), Icons.AutoMirrored.Filled.CallMade, ColorGreen, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.Outgoing) } }
                            if (showCallTime) {
                                item { AnimatedStatCard(180L, "Call Time", if (totalDurationToday > 0) formatDuration(totalDurationToday) else "0s", Icons.Default.Timer, ColorOrange, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.Incoming) } }
                            }
                        }
                    }

                    // ── Filter pills ──────────────────────────────────────────────
                    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                    val isSaturatedDark = remember(settingsVersion, isDark) { isDark && prefs.isSaturatedForTheme(isDark) }
                    val activeChipBg = if (isSaturatedDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    val activeChipFg = if (isSaturatedDark) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onPrimaryContainer

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CallLogFilter.entries) { filter ->
                            val isSelected = selectedFilter == filter
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
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    previousFilterIndex = filterEntries.indexOf(selectedFilter)
                                    viewModel.setFilter(filter)
                                },
                                label = {
                                    Text(
                                        filter.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                        color = labelColor
                                    )
                                },
                                shape = RoundedCornerShape(50.dp),
                                border = null,
                                modifier = Modifier.scale(scale),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = containerColor,
                                    selectedContainerColor = containerColor,
                                    labelColor = labelColor,
                                    selectedLabelColor = labelColor
                                )
                            )
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
                    targetState = Pair(selectedFilter, groupedLogs),
                    transitionSpec = {
                        val filterChanged = initialState.first != targetState.first
                        if (!hasLoadedOnce) {
                            // Startup: slow gentle fade, no slide
                            fadeIn(animationSpec = tween(600, easing = LinearOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(0))
                        } else if (!filterChanged) {
                            // Data-only refresh: instant, no animation whatsoever
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val currentIdx = filterEntries.indexOf(targetState.first)
                            val prevIdx = filterEntries.indexOf(initialState.first)
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
                ) { (_, currentGroupedLogs) ->
                    LaunchedEffect(Unit) { hasLoadedOnce = true }
                    ScrollHapticsEffect(listState = listState)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // In landscape, stat cards and filter pills scroll with the list
                        if (isLandscape) {
                            item(key = "search_bar_pill", contentType = "searchBar") {
                                com.coolappstore.everdialer.by.svhp.view.components.SearchBarPill(
                                    navigator = navigator,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            if (showToday || showMissed || showOutgoing || showCallTime) {
                                item(key = "stat_cards", contentType = "statCards") {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (showToday) item { AnimatedStatCard(0L, "Today", totalToday.toString(), Icons.AutoMirrored.Filled.CallReceived, ColorBlue, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.All) } }
                                        if (showMissed) item { AnimatedStatCard(60L, "Missed", missedToday.toString(), Icons.AutoMirrored.Filled.CallMissed, ColorRed, Modifier.width(110.dp),
                                            if (missedToday > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow
                                        ) { viewModel.setFilter(CallLogFilter.Missed) } }
                                        if (showOutgoing) item { AnimatedStatCard(120L, "Outgoing", outgoingToday.toString(), Icons.AutoMirrored.Filled.CallMade, ColorGreen, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.Outgoing) } }
                                        if (showCallTime) {
                                            item { AnimatedStatCard(180L, "Call Time", if (totalDurationToday > 0) formatDuration(totalDurationToday) else "0s", Icons.Default.Timer, ColorOrange, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.Incoming) } }
                                        }
                                    }
                                }
                            }
                            item(key = "filter_pills", contentType = "filterPills") {
                                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                                val isSaturatedDark = remember(settingsVersion, isDark) { isDark && prefs.isSaturatedForTheme(isDark) }
                                val activeChipBg = if (isSaturatedDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                                val activeChipFg = if (isSaturatedDark) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onPrimaryContainer

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(CallLogFilter.entries) { filter ->
                                        val isSelected = selectedFilter == filter
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
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                previousFilterIndex = filterEntries.indexOf(selectedFilter)
                                                viewModel.setFilter(filter)
                                            },
                                            label = {
                                                Text(
                                                    filter.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                                    color = labelColor
                                                )
                                            },
                                            shape = RoundedCornerShape(50.dp),
                                            border = null,
                                            modifier = Modifier.scale(scale),
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = containerColor,
                                                selectedContainerColor = containerColor,
                                                labelColor = labelColor,
                                                selectedLabelColor = labelColor
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val directCall = prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)
                        currentGroupedLogs.forEach { (header, logsInGroup) ->
                            // Section header as its own item
                            item(key = "header_$header", contentType = "sectionHeader") {
                                RivoScrollAnimatedItem {
                                    RivoSectionHeader(title = header)
                                }
                            }
                            // Individual items per log entry with per-item rounded corners
                            logsInGroup.forEachIndexed { index, lg ->
                                val isFirst = index == 0
                                val isLast = index == logsInGroup.size - 1
                                val cornerRadius = 28.dp
                                val topStart = if (isFirst) cornerRadius else 0.dp
                                val topEnd = if (isFirst) cornerRadius else 0.dp
                                val bottomStart = if (isLast) cornerRadius else 0.dp
                                val bottomEnd = if (isLast) cornerRadius else 0.dp
                                item(
                                    key = "log_${lg.number}_${lg.date}_${index}",
                                    contentType = "callLogEntry"
                                ) {
                                    RivoScrollAnimatedItem(delayMs = (index.coerceAtMost(5) * 30).toLong()) {
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
                                                    CallLogTile(
                                                        log = lg,
                                                        isSelected = selectedLogs.contains("${lg.number}|${lg.date}"),
                                                        selectionMode = selectionMode,
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
                                                                placeCallHonoringContactSim(context, prefs, log.contactId ?: log.number, log.number) {
                                                                    pendingNumber = log.number; showSimPicker = true
                                                                }
                                                            } else {
                                                                navigator.navigate(ContactDetailsScreenDestination(contactId = log.contactId ?: "null", phoneNumber = log.number))
                                                            }
                                                        },
                                                        onAvatarClick = { log ->
                                                            navigator.navigate(ContactDetailsScreenDestination(contactId = log.contactId ?: "null", phoneNumber = log.number))
                                                        },
                                                        onButtonClick = { log ->
                                                            placeCallHonoringContactSim(context, prefs, log.contactId ?: log.number, log.number) {
                                                                pendingNumber = log.number; showSimPicker = true
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
            description = "Ever Dialer needs access to your call logs to show your recent activity and missed calls.",
            onGrantClick = onRequestPermission
        )
    }
}

@Composable
private fun AnimatedStatCard(
    delayMs: Long,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs); visible = true }
    val cardAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "statAlpha")
    val cardOffset by animateDpAsState(if (visible) 0.dp else 16.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "statOffset")
    Box(modifier = Modifier.alpha(cardAlpha).offset(y = cardOffset)) {
        Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.Transparent, modifier = modifier) {
            RivoStatCard(label = label, value = value, icon = icon, iconTint = iconTint, containerColor = containerColor, modifier = Modifier.fillMaxWidth())
        }
    }
}
