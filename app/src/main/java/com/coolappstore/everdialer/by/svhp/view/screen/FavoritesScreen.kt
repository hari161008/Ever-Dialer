package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallWithSimPreference
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoScrollAnimatedItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollHapticsGridEffect
import com.coolappstore.everdialer.by.svhp.view.components.SimPickerDialog
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import com.coolappstore.everdialer.by.svhp.view.screen.settings.AddMode
import com.coolappstore.everdialer.by.svhp.view.screen.settings.FakeCallAddSheet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.Checkbox
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.abs

@Destination<RootGraph>(style = TabTransitionStyle::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(navController: NavController, navigator: DestinationsNavigator) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val favorites = remember(allContacts) { allContacts.filter { it.isFavorite } }
    val scope = rememberCoroutineScope()
    val prefs = koinInject<PreferenceManager>()

    // Drag-to-reorder state — declared first so LaunchedEffect can reference draggedContactId
    var draggedContactId       by remember { mutableStateOf<String?>(null) }
    var dragOffset             by remember { mutableStateOf(Offset.Zero) }
    val itemBoundsMap          = remember { mutableStateMapOf<String, Rect>() }
    var lastSwapTargetId       by remember { mutableStateOf<String?>(null) }
    var fingerAbsPos           by remember { mutableStateOf(Offset.Zero) }
    var expectedDraggedCenter  by remember { mutableStateOf(Offset.Zero) }

    // Ordered favorites — persists custom drag-to-reorder order
    val orderedFavorites = remember { mutableStateListOf<Contact>() }
    LaunchedEffect(favorites) {
        // Don't touch the list while the user is actively dragging
        if (draggedContactId != null) return@LaunchedEffect
        val savedIds = prefs.getString(PreferenceManager.KEY_FAVORITES_ORDER, null)
            ?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        val favMap   = favorites.associateBy { it.id }
        val ordered  = savedIds.mapNotNull { favMap[it] }
        val newList  = ordered + favorites.filter { it.id !in savedIds.toSet() }
        val toRemove = orderedFavorites.filter { o -> newList.none { it.id == o.id } }
        toRemove.forEach { orderedFavorites.remove(it) }
        newList.filter { n -> orderedFavorites.none { it.id == n.id } }
               .forEach { orderedFavorites.add(it) }
    }

    val dragNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (draggedContactId != null) available else Offset.Zero
        }
    }

    fun saveFavoritesOrder() {
        prefs.setString(PreferenceManager.KEY_FAVORITES_ORDER, orderedFavorites.joinToString(",") { it.id })
    }
    val notesEnabled = prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)
    val context = LocalContext.current

    var showSimPicker by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    val simPref = remember { prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault()) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedFavorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFavSelectionMenu by remember { mutableStateOf(false) }
    var showFavDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedFavorites = emptySet()
    }

    if (showFavDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showFavDeleteConfirm = false },
            title = { Text("Remove ${selectedFavorites.size} from Favourites?") },
            confirmButton = {
                Button(
                    onClick = {
                        showFavDeleteConfirm = false
                        allContacts.filter { selectedFavorites.contains(it.id) }.forEach { contactsVM.toggleFavorite(it) }
                        selectedFavorites = emptySet(); selectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showFavDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true) {
            pendingCallNumber?.let { num ->
                placeCallWithSimPreference(context, num, simPref) {
                    showSimPicker = true
                }
            }
        }
    }

    if (showSimPicker && pendingCallNumber != null) {
        val telecomManager = remember { context.getSystemService(android.content.Context.TELECOM_SERVICE) as TelecomManager }
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, pendingCallNumber!!, handle)
                showSimPicker = false
            }
        )
    }

    val pillNav = remember { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull() ?: continue
                        if (!down.pressed) continue
                        val startX = down.position.x
                        val startY = down.position.y
                        val startTime = System.currentTimeMillis()
                        var triggered = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break
                            // Never swipe tabs while a drag-to-reorder is in progress
                            if (draggedContactId != null) { if (!change.pressed) break; continue }
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            val elapsed = System.currentTimeMillis() - startTime
                            if (!triggered && elapsed >= 150L &&
                                abs(dx) > 700f &&
                                abs(dx) > abs(dy) * 5.5f
                            ) {
                                triggered = true
                                if (dx < 0) {
                                    scope.launch {
                                        navController.navigate(RecentScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    }
                                } else {
                                    if (notesEnabled) {
                                        scope.launch {
                                            navController.navigate(NotesScreenDestination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                        }
                                    }
                                }
                            }
                            if (!change.pressed) break
                        }
                    }
                }
            },
        topBar = { TopBar(navController, navigator) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (favorites.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Favorites Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Star a contact to add them here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                val gridState = rememberLazyGridState()
                ScrollHapticsGridEffect(gridState = gridState)
                LazyVerticalGrid(
                    // Fixed(3) always used exactly 3 columns even in landscape, where the
                    // much wider screen made each of those 3 cells — and therefore each
                    // favorite tile — stretch to a huge size. Adaptive sizes each column to
                    // a sensible minimum width and lets the column count grow with the
                    // available width instead, so tiles look right in both orientations.
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(dragNestedScroll),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orderedFavorites, key = { it.id }) { contact ->
                        RivoScrollAnimatedItem {
                            FavoriteContactCard(
                                contact = contact,
                                contactsVM = contactsVM,
                                navigator = navigator,
                                context = context,
                                isSelected = selectedFavorites.contains(contact.id),
                                selectionMode = selectionMode,
                                isDragging = draggedContactId == contact.id,
                                dragOffset = if (draggedContactId == contact.id) dragOffset else null,
                                onBoundsChanged = { bounds -> itemBoundsMap[contact.id] = bounds },
                                onDragStart = { _ ->
                                    draggedContactId = contact.id
                                    dragOffset = Offset.Zero
                                    val center = itemBoundsMap[contact.id]?.center ?: Offset.Zero
                                    fingerAbsPos = center
                                    expectedDraggedCenter = center
                                    lastSwapTargetId = null
                                },
                                onDrag = { amt ->
                                    if (draggedContactId == contact.id) {
                                        fingerAbsPos += amt
                                        // dragOffset = finger position minus expected card center
                                        // expectedDraggedCenter is updated synchronously on swap,
                                        // eliminating the 1-frame position jump that causes flicker
                                        dragOffset = fingerAbsPos - expectedDraggedCenter

                                        val targetId = itemBoundsMap.entries
                                            .filter { it.key != draggedContactId }
                                            .firstOrNull { (_, b) -> b.contains(fingerAbsPos) }
                                            ?.key

                                        if (targetId != null && targetId != lastSwapTargetId) {
                                            // Update expectedDraggedCenter to target's current bounds BEFORE swap
                                            // so dragOffset compensates immediately in this same frame
                                            val targetCenter = itemBoundsMap[targetId]?.center
                                            if (targetCenter != null) {
                                                expectedDraggedCenter = targetCenter
                                                dragOffset = fingerAbsPos - expectedDraggedCenter
                                            }
                                            lastSwapTargetId = targetId
                                            val fromIdx = orderedFavorites.indexOfFirst { it.id == draggedContactId }
                                            val toIdx   = orderedFavorites.indexOfFirst { it.id == targetId }
                                            if (fromIdx != -1 && toIdx != -1) {
                                                orderedFavorites.add(toIdx, orderedFavorites.removeAt(fromIdx))
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedContactId = null
                                    dragOffset = Offset.Zero
                                    fingerAbsPos = Offset.Zero
                                    expectedDraggedCenter = Offset.Zero
                                    lastSwapTargetId = null
                                    saveFavoritesOrder()
                                },
                                onDragCancel = {
                                    draggedContactId = null
                                    dragOffset = Offset.Zero
                                    fingerAbsPos = Offset.Zero
                                    expectedDraggedCenter = Offset.Zero
                                    lastSwapTargetId = null
                                },
                                onSelectToggle = {
                                    val id = contact.id
                                    selectedFavorites = if (selectedFavorites.contains(id)) selectedFavorites - id else selectedFavorites + id
                                },
                                onSelectMode = {
                                    selectionMode = true
                                    selectedFavorites = setOf(contact.id)
                                },
                                onClick = {
                                    val directCall = prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)
                                    val phoneNumber = contact.phoneNumbers.firstOrNull()
                                    if (directCall && phoneNumber != null) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                            placeCallWithSimPreference(context, phoneNumber, simPref) {
                                                pendingCallNumber = phoneNumber
                                                showSimPicker = true
                                            }
                                        } else {
                                            pendingCallNumber = phoneNumber
                                            callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
                                        }
                                    } else {
                                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                                    }
                                }
                            )
                        }
                    }
                }
            }
                } // end Column
            } // end inner Box
        }
    // Selection bar at screen root level (outside Scaffold)
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
                            IconButton(onClick = { selectionMode = false; selectedFavorites = emptySet() }) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(
                                "${selectedFavorites.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(onClick = { showFavSelectionMenu = true }) {
                                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                DropdownMenu(expanded = showFavSelectionMenu, onDismissRequest = { showFavSelectionMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Remove from Favourites") },
                                        leadingIcon = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { showFavSelectionMenu = false; if (selectedFavorites.isNotEmpty()) showFavDeleteConfirm = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Select All") },
                                        leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                                        onClick = { showFavSelectionMenu = false; selectedFavorites = favorites.map { it.id }.toSet() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteContactCard(
    contact: Contact,
    contactsVM: ContactsViewModel,
    navigator: DestinationsNavigator,
    context: android.content.Context,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: Offset? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onSelectToggle: (() -> Unit)? = null,
    onSelectMode: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "cardAlpha")
    LaunchedEffect(Unit) { visible = true }

    var isPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isDraggingLocally by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val fakeCallInContextMenu = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
    }
    var showFakeCallSheet by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.08f
            isPressed || showMenu -> 0.93f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )
    // Selection highlight
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(200),
        label = "cardBg"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBgColor,
        tonalElevation = if (isDragging) 8.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                if (isDragging && dragOffset != null) {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                    scaleX = 1.08f; scaleY = 1.08f; shadowElevation = 24f
                }
            }
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                onBoundsChanged?.invoke(bounds)
            }
            .pointerInput(selectionMode, contact.id) {
                val pis = this
                coroutineScope {
                    val cs = this
                    pis.awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            val startPos = down.position
                            val touchSlop = viewConfiguration.touchSlop
                            var longPressTriggered = false
                            var dragStarted = false

                            val longPressJob = cs.launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                longPressTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isPressed = false
                                if (selectionMode) onSelectToggle?.invoke() else showMenu = true
                            }

                            loop@ while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }

                                if (change == null || !change.pressed) {
                                    longPressJob.cancel()
                                    isPressed = false
                                    when {
                                        dragStarted -> { isDraggingLocally = false; onDragEnd?.invoke() }
                                        !longPressTriggered -> { if (selectionMode) onSelectToggle?.invoke() else onClick() }
                                        // long press without drag: menu already shown in job
                                    }
                                    break@loop
                                }

                                val totalDist = (change.position - startPos).getDistance()
                                val delta = change.position - change.previousPosition

                                if (!dragStarted && totalDist > touchSlop) {
                                    if (longPressTriggered && !selectionMode) {
                                        showMenu = false
                                        dragStarted = true
                                        isDraggingLocally = true
                                        isPressed = false
                                        longPressJob.cancel()
                                        onDragStart?.invoke(startPos)
                                    } else if (!longPressTriggered) {
                                        longPressJob.cancel()
                                        isPressed = false
                                        break@loop
                                    }
                                }

                                if (dragStarted) {
                                    change.consume()
                                    onDrag?.invoke(delta)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                RivoAvatar(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            Text(
                text = contact.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        // Checkbox overlay — animated entry/exit
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.6f),
            exit  = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle?.invoke() },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        }
    }

    // Respect Settings → Appearance → "Context Menu Elements" customization (show/hide + order)
    val phoneNumber = contact.phoneNumbers.firstOrNull()
    val favContextMenuKeys = remember(settingsVer, phoneNumber, fakeCallInContextMenu) {
        com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.resolvedKeys(
            prefs,
            com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.SECTION_FAVORITES,
            listOf("select", "call", "send_sms", "view_details", "fake_call", "remove_favorite")
        ).filter { key ->
            when (key) {
                "send_sms" -> !phoneNumber.isNullOrEmpty()
                "fake_call" -> fakeCallInContextMenu
                else -> true
            }
        }
    }

    RivoDropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        favContextMenuKeys.forEachIndexed { index, key ->
            // A divider separates the destructive "Remove from Favourites" action from
            // everything above it.
            if (key == "remove_favorite" && index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            when (key) {
                "select" -> RivoDropdownMenuItem(
                    text = "Select",
                    icon = Icons.Default.CheckBox,
                    iconTint = Color(0xFF9C27B0),
                    onClick = {
                        showMenu = false
                        onSelectMode?.invoke()
                    }
                )
                "call" -> RivoDropdownMenuItem(
                    text = "Call",
                    icon = Icons.Default.Call,
                    iconTint = Color(0xFF4CAF50),
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                "send_sms" -> RivoDropdownMenuItem(
                    text = "Send SMS",
                    icon = Icons.Default.Message,
                    iconTint = Color(0xFF009688),
                    onClick = {
                        showMenu = false
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("sms:$phoneNumber")
                        }
                        context.startActivity(intent)
                    }
                )
                "view_details" -> RivoDropdownMenuItem(
                    text = "View Details",
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFF2196F3),
                    onClick = {
                        showMenu = false
                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                    }
                )
                "fake_call" -> RivoDropdownMenuItem(
                    text = "Fake Call",
                    icon = Icons.Outlined.PhoneCallback,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        showMenu = false
                        showFakeCallSheet = true
                    }
                )
                "remove_favorite" -> RivoDropdownMenuItem(
                    text = "Remove from Favourites",
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFF44336),
                    isDestructive = true,
                    onClick = {
                        showMenu = false
                        contactsVM.toggleFavorite(contact)
                    }
                )
            }
            if (key == "select" && index < favContextMenuKeys.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showFakeCallSheet) {
        FakeCallAddSheet(
            mode = AddMode.Number,
            initialNumber = contact.phoneNumbers.firstOrNull() ?: "",
            initialDisplayName = contact.name,
            onDismiss = { showFakeCallSheet = false },
            onSave = { entry, exactTriggerOverride ->
                FakeCallManager.addEntry(context, prefs, entry, exactTriggerOverride)
                showFakeCallSheet = false
            }
        )
    }
}
