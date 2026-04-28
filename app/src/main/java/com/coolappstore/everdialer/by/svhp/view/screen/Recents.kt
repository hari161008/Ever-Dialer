package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogFilter
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Locale

private val ColorBlue   = Color(0xFF2196F3)
private val ColorRed    = Color(0xFFE91E63)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun RecentScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CALL_LOG)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
    val prefs = koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager>()

    var showDialpad by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(false) }
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

    if (showDialpad) {
        ModalBottomSheet(
            onDismissRequest = { showDialpad = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(width = 36.dp, height = 4.dp)) {}
                }
            }
        ) {
            DialPadContent(navigator = navigator, onDismiss = { showDialpad = false })
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(navController, navigator) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialpad = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                modifier = Modifier.scale(fabScale)
            ) { Icon(Icons.Default.Dialpad, "Dialpad") }
        },
        bottomBar = { BottomBar(navController, navigator) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            CallLogFullContent(
                navController = navController,
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState
            )
            ScrollToTopButton(visible = showButton, onClick = { scope.launch { listState.animateScrollToItem(0) } })
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

@Composable
fun CallLogFullContent(
    navController: NavController,
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val prefs = koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()

    if (isGranted) {
        val viewModel: CallLogViewModel = koinActivityViewModel()
        val logs by viewModel.allCallLogs.collectAsState()
        val selectedFilter by viewModel.selectedFilter.collectAsState()
        val context = LocalContext.current
        val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

        var showSimPicker by remember { mutableStateOf(false) }
        var pendingNumber by remember { mutableStateOf<String?>(null) }

        // Track previous filter index for slide direction
        val filterEntries = CallLogFilter.entries
        var previousFilterIndex by remember { mutableIntStateOf(filterEntries.indexOf(selectedFilter)) }
        val currentFilterIndex = filterEntries.indexOf(selectedFilter)
        val slideForward = currentFilterIndex >= previousFilterIndex

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
            RivoLoadingIndicatorView()
        } else {
            val missedCount = remember(logs) { logs.count { it.type == CallLog.Calls.MISSED_TYPE } }
            val todayStart = remember { System.currentTimeMillis() - 86_400_000L }
            val totalToday = remember(logs) { logs.count { it.date >= todayStart } }
            val totalDurationToday = remember(logs) {
                logs.filter { it.date >= todayStart && it.duration > 0 }.sumOf { it.duration }
            }

            // Swipe drag state
            var dragAccumulator by remember { mutableFloatStateOf(0f) }
            val swipeThreshold = 80f

            Column(modifier = Modifier.fillMaxSize()) {
                // Stat cards – visibility controlled by Call UI settings
                val showToday    = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_TODAY, true) }
                val showMissed   = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_MISSED, true) }
                val showOutgoing = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true) }
                val showCallTime = remember(settingsVersion) { prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true) }
                if (showToday || showMissed || showOutgoing || (showCallTime && totalDurationToday > 0)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showToday) item { AnimatedStatCard(0L, "Today", totalToday.toString(), Icons.AutoMirrored.Filled.CallReceived, ColorBlue, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.All) } }
                        if (showMissed) item { AnimatedStatCard(60L, "Missed", missedCount.toString(), Icons.AutoMirrored.Filled.CallMissed, ColorRed, Modifier.width(110.dp),
                            if (missedCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow
                        ) { viewModel.setFilter(CallLogFilter.Missed) } }
                        if (showOutgoing) item { AnimatedStatCard(120L, "Outgoing", logs.count { it.type == CallLog.Calls.OUTGOING_TYPE }.toString(), Icons.AutoMirrored.Filled.CallMade, ColorGreen, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.Outgoing) } }
                        if (showCallTime && totalDurationToday > 0) {
                            item { AnimatedStatCard(180L, "Call Time", formatDuration(totalDurationToday), Icons.Default.Timer, ColorOrange, Modifier.width(110.dp)) { viewModel.setFilter(CallLogFilter.Incoming) } }
                        }
                    }
                }

                // ── Filter pills (fixed, do not move) ──────────────────────────
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CallLogFilter.entries) { filter ->
                        val isSelected = selectedFilter == filter
                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                          else MaterialTheme.colorScheme.surfaceContainerLow,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chipColor"
                        )
                        val labelColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
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

                // ── Animated content: slides left/right on filter change ──────
                AnimatedContent(
                    targetState = Pair(selectedFilter, groupedLogs),
                    transitionSpec = {
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
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulator = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccumulator += dragAmount
                                },
                                onDragEnd = {
                                    if (dragAccumulator < -swipeThreshold) {
                                        navController.navigate(ContactScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else if (dragAccumulator > swipeThreshold) {
                                        navController.navigate(FavoritesScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    dragAccumulator = 0f
                                },
                                onDragCancel = { dragAccumulator = 0f }
                            )
                        },
                    label = "filterSlide"
                ) { (_, currentGroupedLogs) ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentGroupedLogs.forEach { (header, logsInGroup) ->
                            item {
                                RivoSectionHeader(title = header)
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    RivoExpressiveCard {
                                        logsInGroup.forEachIndexed { index, lg ->
                                            CallLogTile(
                                                log = lg,
                                                onTileClick = { log ->
                                                    navigator.navigate(ContactDetailsScreenDestination(contactId = log.contactId ?: "null", phoneNumber = log.number))
                                                },
                                                onButtonClick = { log ->
                                                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                                                    if (hasPermission) {
                                                        val accounts = telecomManager.callCapablePhoneAccounts
                                                        if (accounts.size > 1) { pendingNumber = log.number; showSimPicker = true }
                                                        else makeCall(context, log.number)
                                                    } else makeCall(context, log.number)
                                                },
                                                onDelete = { viewModel.refreshLogs() }
                                            )
                                            if (index < logsInGroup.size - 1) {
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
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
        Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = containerColor, modifier = modifier) {
            RivoStatCard(label = label, value = value, icon = icon, iconTint = iconTint, containerColor = Color.Transparent, modifier = Modifier.fillMaxWidth())
        }
    }
}
