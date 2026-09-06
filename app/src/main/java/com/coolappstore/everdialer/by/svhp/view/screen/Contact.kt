package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.view.components.*
import android.os.Build
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.lens
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.colorControls
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import com.coolappstore.everdialer.by.svhp.view.components.enterNotesTab

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun ContactScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )
    LaunchedEffect(Unit) { visible = true }

    val prefs_ui = koinInject<PreferenceManager>()
    val pillNav = remember { prefs_ui.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectionMode by remember { mutableStateOf(false) }
    var selectedContacts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val contactsVM2: ContactsViewModel = koinActivityViewModel()
    val displayedContacts2 by contactsVM2.displayedContacts.collectAsState()

    BackHandler(enabled = selectionMode) { selectionMode = false; selectedContacts = emptySet() }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedContacts.size} contact${if (selectedContacts.size != 1) "s" else ""}?") },
            text  = { Text("This will permanently delete the selected contacts.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    selectedContacts.forEach { contactsVM2.deleteContact(it) }
                    selectedContacts = emptySet(); selectionMode = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

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
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            val elapsed = System.currentTimeMillis() - startTime
                            if (!triggered && elapsed >= 150L && !change.isConsumed && kotlin.math.abs(dx) > 700f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 5.5f) {
                                triggered = true
                                if (dx > 0) {
                                    scope.launch {
                                        navController.navigate(RecentScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    }
                                } else {
                                    // swipe left from Contacts → Notes (wrap around)
                                    scope.launch {
                                        navController.enterNotesTab()
                                    }
                                }
                            }
                            if (!change.pressed) break
                        }
                    }
                }
            },
        topBar = { TopBar(navController, navigator) },
        floatingActionButton = {
            val globalBackdrop = LocalLiquidGlassBackdrop.current
            val settingsVer by prefs_ui.settingsChanged.collectAsState()
            val liquidGlass = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }
            val lgContactsFab = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_LG_CONTACTS_FAB, false) }
            val blurEffects = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
            val blurContactsFab = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_BLUR_CONTACTS_FAB, false) }
            val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
            val isSaturatedActive = remember(settingsVer, isDark) { prefs_ui.isSaturatedForTheme(isDark) }
            val fabBg = if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            val fabFg = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            val fabShape = RoundedCornerShape(17.dp)
            val useLiquidGlass = liquidGlass && lgContactsFab && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
            val useBlur = blurEffects && blurContactsFab && !useLiquidGlass
            val baseModifier = Modifier
                .scale(fabScale)
                .then(if (pillNav) Modifier.navigationBarsPadding().padding(bottom = 92.dp) else Modifier)
                .then(if (isLandscape) Modifier.navigationBarsPadding().padding(bottom = 8.dp) else Modifier)
            val fabOnClick: () -> Unit = {
                val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                context.startActivity(intent)
            }
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
                        onClick = fabOnClick,
                        containerColor = fabBg.copy(alpha = 0.0f),
                        contentColor = fabFg,
                        shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp,
                        focusedElevation = 6.dp,
                        hoveredElevation = 6.dp
                    ),
                    ) { Icon(Icons.Default.PersonAdd, "Add Contact") }
                }
            } else {
                FloatingActionButton(
                    onClick = fabOnClick,
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
                ) { Icon(Icons.Default.PersonAdd, "Add Contact") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ContactContent(
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState,
                selectionMode = selectionMode,
                selectedContacts = selectedContacts,
                onSelectionModeChange = { selectionMode = it },
                onSelectedContactsChange = { selectedContacts = it },
                isLandscape = isLandscape
            )
        }
    }
    // Selection bar — screen-root overlay (same style as Recents)
    Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).zIndex(10f)) {
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(280)),
            exit  = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(420, easing = FastOutLinearInEasing)) + fadeOut(tween(380))
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp) {
                Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectionMode = false; selectedContacts = emptySet() }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("${selectedContacts.size} selected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showSelectionMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        DropdownMenu(expanded = showSelectionMenu, onDismissRequest = { showSelectionMenu = false }) {
                            DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showSelectionMenu = false; if (selectedContacts.isNotEmpty()) showDeleteConfirm = true })
                            DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showSelectionMenu = false
                                    val text = displayedContacts2.filter { selectedContacts.contains(it.id) }.joinToString("\n") { "${it.name}: ${it.phoneNumbers.firstOrNull() ?: ""}" }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share contacts"))
                                })
                            DropdownMenuItem(text = { Text("Select All") }, leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                                onClick = { showSelectionMenu = false; selectedContacts = displayedContacts2.map { it.id }.toSet() })
                            DropdownMenuItem(text = { Text("Deselect All") }, leadingIcon = { Icon(Icons.Default.Close, null) },
                                onClick = { showSelectionMenu = false; selectedContacts = emptySet() })
                        }
                    }
                }
            }
        }
    }
    } // end wrapper Box
}

@Composable
fun ContactContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectionMode: Boolean = false,
    selectedContacts: Set<String> = emptySet(),
    onSelectionModeChange: (Boolean) -> Unit = {},
    onSelectedContactsChange: (Set<String>) -> Unit = {},
    isLandscape: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "contentAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        if (isGranted) {
            val contactsVM: ContactsViewModel = koinActivityViewModel()
            val prefs = koinInject<PreferenceManager>()
            val settingsVersion by prefs.settingsChanged.collectAsState()

            LaunchedEffect(settingsVersion) {
                contactsVM.fetchContacts()
            }

            val contacts = contactsVM.displayedContacts.collectAsState().value
            val isLoadingContacts by contactsVM.isLoading.collectAsState()

            // ── Contact count / account-switcher pill ─────────────────────
            // This row (and the sheet it opens) must always stay visible whenever contacts
            // permission is granted — regardless of whether the current filter is still loading
            // or came back with zero contacts. Previously this whole block lived inside the
            // "else" branch below (only shown once loaded AND non-empty), which meant picking a
            // source with few/no contacts, or being mid-load, made the switcher disappear
            // entirely — looking like the app was stuck.
            var chipVisible by remember { mutableStateOf(false) }
            val chipAlpha by animateFloatAsState(
                targetValue = if (chipVisible) 1f else 0f,
                animationSpec = tween(500),
                label = "chipAlpha"
            )
            val chipScale by animateFloatAsState(
                targetValue = if (chipVisible) 1f else 0.8f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipScale"
            )
            LaunchedEffect(Unit) { chipVisible = true }

            val availableAccounts by contactsVM.availableAccounts.collectAsState()
            val contactGroups by contactsVM.contactGroups.collectAsState()
            val selectedAccountKey by contactsVM.selectedAccountKey.collectAsState()
            val selectedGroupId by contactsVM.selectedGroupId.collectAsState()
            var showAccountSheet by remember { mutableStateOf(false) }

            LaunchedEffect(settingsVersion) {
                contactsVM.fetchAvailableAccounts()
                contactsVM.fetchContactGroups()
            }

            val contactsCountText = when {
                isLoadingContacts -> "Loading…"
                selectedGroupId != null -> {
                    val grp = contactGroups.find { it.id == selectedGroupId }
                    "${grp?.name ?: "Group"} · ${contacts.size}"
                }
                selectedAccountKey != null -> {
                    val acc = availableAccounts.find { it.key == selectedAccountKey }
                    "${acc?.displayName ?: selectedAccountKey} · ${contacts.size}"
                }
                else -> "${contacts.size} contacts"
            }

            val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
            val isSaturatedActive = remember(settingsVersion, isDark) { prefs.isSaturatedForTheme(isDark) }
            val activePillBg = if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            val activePillFg = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer

            // ── 1. Contact Groups Section (ABOVE Contacts to Display) ─────────
            if (contactGroups.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .alpha(chipAlpha)
                        .scale(chipScale),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All" group chip
                    item(key = "group_all") {
                        FilterChip(
                            selected = selectedGroupId == null,
                            onClick = { contactsVM.clearFilters() },
                            label = { Text("All") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    // Contact groups in user-ordered sequence
                    items(contactGroups, key = { it.id }) { grp ->
                        val isSelected = selectedGroupId == grp.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) contactsVM.clearFilters()
                                else contactsVM.setGroupFilter(grp.id)
                            },
                            label = { Text(grp.name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // ── 2. Contacts to Display Button (BELOW Contact Groups) ─────────
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .alpha(chipAlpha)
                    .scale(chipScale),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showAccountSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedAccountKey != null)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        activePillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLoadingContacts) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = if (selectedAccountKey != null)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    activePillFg
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedAccountKey != null)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    activePillFg
                            )
                        }
                        Text(
                            text = contactsCountText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedAccountKey != null)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                activePillFg
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedAccountKey != null)
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            else
                                activePillFg.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (showAccountSheet) {
                ContactsToDisplaySheet(
                    accounts = availableAccounts,
                    groups = contactGroups,
                    selectedAccountKey = selectedAccountKey,
                    selectedGroupId = selectedGroupId,
                    totalCount = availableAccounts.sumOf { it.contactCount }.takeIf { it > 0 } ?: contacts.size,
                    onSelectAccount = { key ->
                        contactsVM.setAccountFilter(key)
                        showAccountSheet = false
                    },
                    onSelectGroup = { groupId ->
                        contactsVM.setGroupFilter(groupId)
                        showAccountSheet = false
                    },
                    onDismiss = { showAccountSheet = false },
                    contactsVM = contactsVM
                )
            }

            // ── Body: loading indicator, empty state, or the actual list ──
            if (isLoadingContacts) {
                if (isLandscape) {
                    com.coolappstore.everdialer.by.svhp.view.components.SearchBarPill(
                        navigator = navigator,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                RivoLoadingIndicatorView()
            } else if (contacts.isEmpty()) {
                if (isLandscape) {
                    com.coolappstore.everdialer.by.svhp.view.components.SearchBarPill(
                        navigator = navigator,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No contacts here",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ScrollHapticsEffect(listState = listState)
                AZListScroll(
                                contacts = contacts,
                                navigator = navigator,
                                listState = listState,
                                selectionMode = selectionMode,
                                selectedContacts = selectedContacts,
                                onSelectionModeChange = onSelectionModeChange,
                                onSelectedContactsChange = onSelectedContactsChange,
                                topContent = if (isLandscape) {
                                    {
                                        com.coolappstore.everdialer.by.svhp.view.components.SearchBarPill(
                                            navigator = navigator,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                        )
                                    }
                                } else null
                            )
            }
        } else {
            if (isLandscape) {
                com.coolappstore.everdialer.by.svhp.view.components.SearchBarPill(
                    navigator = navigator,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            PermissionDeniedView(
                icon = Icons.Default.Person,
                title = "Contacts",
                description = "Ever Dialer needs access to your contacts to show your contact list and identify incoming calls.",
                onGrantClick = onRequestPermission
            )
        }
    }
}


