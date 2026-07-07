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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues

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
    val allContacts2 by contactsVM2.allContacts.collectAsState()

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
                                        navController.navigate(NotesScreenDestination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true; restoreState = true
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
        floatingActionButton = {
            val globalBackdrop = LocalLiquidGlassBackdrop.current
            val settingsVer by prefs_ui.settingsChanged.collectAsState()
            val liquidGlass = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }
            val lgContactsFab = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_LG_CONTACTS_FAB, false) }
            val blurEffects = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
            val blurContactsFab = remember(settingsVer) { prefs_ui.getBoolean(PreferenceManager.KEY_BLUR_CONTACTS_FAB, false) }
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.0f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    ) { Icon(Icons.Default.PersonAdd, "Add Contact") }
                }
            } else {
                FloatingActionButton(
                    onClick = fabOnClick,
                    containerColor = if (useBlur)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    else
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
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
                onSelectedContactsChange = { selectedContacts = it }
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
                                    val text = allContacts2.filter { selectedContacts.contains(it.id) }.joinToString("\n") { "${it.name}: ${it.phoneNumbers.firstOrNull() ?: ""}" }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share contacts"))
                                })
                            DropdownMenuItem(text = { Text("Select All") }, leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                                onClick = { showSelectionMenu = false; selectedContacts = allContacts2.map { it.id }.toSet() })
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
    onSelectedContactsChange: (Set<String>) -> Unit = {}
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

            val contacts = contactsVM.allContacts.collectAsState().value
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
            val selectedAccountKey by contactsVM.selectedAccountKey.collectAsState()
            var showAccountSheet by remember { mutableStateOf(false) }

            LaunchedEffect(settingsVersion) {
                contactsVM.fetchAvailableAccounts()
            }

            val contactsCountText = when {
                isLoadingContacts -> "Loading…"
                selectedAccountKey != null -> {
                    val acc = availableAccounts.find { it.key == selectedAccountKey }
                    "${acc?.displayName ?: selectedAccountKey} · ${contacts.size}"
                }
                else -> "${contacts.size} contacts"
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .alpha(chipAlpha)
                    .scale(chipScale),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Contacts count + account switcher pill ─────────────
                Surface(
                    onClick = { showAccountSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedAccountKey != null)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
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
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedAccountKey != null)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = contactsCountText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedAccountKey != null)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedAccountKey != null)
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // ── Sources pill (separate, non-interactive summary) ───
                if (selectedAccountKey == null && availableAccounts.isNotEmpty()) {
                    val sourceLabels = availableAccounts.joinToString(" · ") { acc ->
                        when {
                            acc.key.startsWith("google_") -> acc.accountName.substringBefore("@").take(10)
                            acc.key == "sim_1" -> "SIM 1"
                            acc.key == "sim_2" -> "SIM 2"
                            acc.key == "whatsapp" -> "WhatsApp"
                            else -> acc.displayName.take(10)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = sourceLabels,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (showAccountSheet) {
                AccountSwitcherSheet(
                    accounts = availableAccounts,
                    selectedKey = selectedAccountKey,
                    totalCount = availableAccounts.sumOf { it.contactCount }.takeIf { it > 0 } ?: contacts.size,
                    onSelect = { key ->
                        contactsVM.setAccountFilter(key)
                        showAccountSheet = false
                    },
                    onDismiss = { showAccountSheet = false }
                )
            }

            // ── Body: loading indicator, empty state, or the actual list ──
            if (isLoadingContacts) {
                RivoLoadingIndicatorView()
            } else if (contacts.isEmpty()) {
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
                                onSelectedContactsChange = onSelectedContactsChange
                            )
            }
        } else {
            PermissionDeniedView(
                icon = Icons.Default.Person,
                title = "Contacts",
                description = "Ever Dialer needs access to your contacts to show your contact list and identify incoming calls.",
                onGrantClick = onRequestPermission
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSwitcherSheet(
    accounts: List<ContactAccount>,
    selectedKey: String?,
    totalCount: Int,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = androidx.compose.ui.Modifier.size(width = 36.dp, height = 4.dp)
                ) {}
            }
        }
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Contact Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                // "All Contacts" row
                item {
                    AccountRow(
                        icon = Icons.Default.People,
                        name = "All Contacts",
                        subtitle = "$totalCount contacts",
                        isSelected = selectedKey == null,
                        onClick = { onSelect(null) }
                    )
                }

                if (accounts.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                    items(accounts, key = { it.key }) { account ->
                        val accountIcon = when {
                            account.accountType.contains("google", ignoreCase = true) -> Icons.Default.Email
                            account.key == "sim_1" || account.key == "sim_2" || account.key.startsWith("sim_") -> Icons.Default.SimCard
                            else -> Icons.Default.AccountCircle
                        }
                        AccountRow(
                            icon = accountIcon,
                            name = account.displayName,
                            subtitle = "${account.contactCount} contacts",
                            isSelected = selectedKey == account.key,
                            onClick = { onSelect(account.key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else androidx.compose.ui.graphics.Color.Transparent,
        spring(stiffness = Spring.StiffnessMediumLow), label = "rowBg"
    )
    Surface(
        onClick = onClick,
        color = bgColor,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = androidx.compose.ui.Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(22.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = androidx.compose.ui.Modifier.size(22.dp)
                )
            }
        }
    }
}
