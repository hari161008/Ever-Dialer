package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.*
import com.coolappstore.evercallrecorder.by.svhp.ui.common.FileSavePickerDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appVersion: String,
    onSettingsClick: () -> Unit,
    onRecordingClick: (com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem, String) -> Unit = { _, _ -> },
    onSelectionModeChanged: (Boolean) -> Unit = {},
    onGlobalSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vm: HomeViewModel = viewModel()
    val recordings     by vm.recordings.collectAsState()
    val isLoading      by vm.isLoading.collectAsState()
    val query          by vm.searchQuery.collectAsState()
    val filterTab      by vm.filterTab.collectAsState()
    val sortConfig     by vm.sortConfig.collectAsState()
    val selectedUris   by vm.selectedUris.collectAsState()
    val isSelectionMode = selectedUris.isNotEmpty()
    val context = LocalContext.current

    // Let the host (Ever Dialer's Recordings tab) know when the recording-selection pill is
    // showing, so it can smoothly animate its own bottom navigation pill out of the way instead
    // of the two pills overlapping.
    LaunchedEffect(isSelectionMode) { onSelectionModeChanged(isSelectionMode) }
    DisposableEffect(Unit) { onDispose { onSelectionModeChanged(false) } }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSelectionMode) { vm.clearSelection() }
    BackHandler(enabled = !isSelectionMode && query.isNotBlank()) {
        vm.searchQuery.value = ""
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    var recordingEnabledState by remember {
        mutableStateOf(com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences(context).isCallRecordingEnabled())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
                recordingEnabledState = com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences(context).isCallRecordingEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showFileSavePicker    by remember { mutableStateOf(false) }

    // Storage permission flow
    val writePermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showFileSavePicker = true
        else android.widget.Toast.makeText(context, "Storage permission required to save files", android.widget.Toast.LENGTH_SHORT).show()
    }
    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            showFileSavePicker = true
        } else {
            android.widget.Toast.makeText(context, "Storage access required to save files", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun requestStorageThenPick() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) showFileSavePicker = true
                else manageStorageLauncher.launch(
                    android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED -> {
                writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> showFileSavePicker = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ever Call Recorder", fontWeight = FontWeight.Bold) },
                actions = {
                    AnimatedVisibility(visible = !isSelectionMode, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        val grouped: Map<String, List<com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem>> = remember(recordings) {
            recordings.groupBy { groupLabel(it.date) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
            com.coolappstore.evercallrecorder.by.svhp.ui.common.ScrollHapticsEffect(listState = homeListState)
            LazyColumn(
                state = homeListState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (!recordingEnabledState) {
                    item {
                        CallRecordingDisabledBanner(
                            onClick = onSettingsClick,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                item {
                    GlobalSearchPill(
                        onClick = onGlobalSearchClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    FilterPillRow(
                        filterTab = filterTab,
                        sortConfig = sortConfig,
                        onFilterChange = { vm.filterTab.value = it },
                        onSortChange   = { vm.sortConfig.value = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                when {
                    isLoading -> item {
                        Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    recordings.isEmpty() -> item {
                        EmptyState(isFavourites = filterTab == FilterTab.FAVOURITES, hasQuery = query.isNotBlank())
                    }
                    else -> {
                        grouped.forEach { (dateLabel, items) ->
                            item(key = "header_$dateLabel") {
                                DateGroupHeader(label = dateLabel, modifier = Modifier.animateItem(fadeInSpec = tween(340), placementSpec = spring(stiffness = Spring.StiffnessLow), fadeOutSpec = tween(220)))
                            }
                            item(key = "group_$dateLabel") {
                                RecordingGroupCard(
                                    items = items,
                                    searchQuery = query,
                                    isSelectionMode = isSelectionMode,
                                    selectedUris = selectedUris,
                                    onFavouriteToggle = { vm.toggleFavourite(it) },
                                    onRecordingClick  = { recording -> onRecordingClick(recording, query) },
                                    onToggleSelect    = { vm.toggleSelection(it.uri) },
                                    modifier = Modifier.animateItem(fadeInSpec = tween(380, easing = FastOutSlowInEasing), placementSpec = spring(stiffness = Spring.StiffnessLow), fadeOutSpec = tween(240))
                                )
                            }
                        }
                    }
                }
            }

            // ── Floating selection bar ────────────────────────────────────────
            // Use graphicsLayer instead of AnimatedVisibility so the Surface shadow
            // is captured in the same render layer as the content — preventing the
            // one-frame shadow "blink" that AnimatedVisibility causes on Surface.
            val density = LocalDensity.current
            val offsetPx = with(density) { 110.dp.toPx() }
            val barAlpha by animateFloatAsState(
                targetValue   = if (isSelectionMode) 1f else 0f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "barAlpha"
            )
            val barTranslateY by animateFloatAsState(
                targetValue   = if (isSelectionMode) 0f else offsetPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                ),
                label = "barTranslate"
            )
            // Only keep in composition while visible to avoid wasted recompositions
            if (barAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 48.dp)
                        .graphicsLayer {
                            alpha        = barAlpha
                            translationY = barTranslateY
                        }
                ) {
                    SelectionBar(
                        count       = selectedUris.size,
                        total       = recordings.size,
                        onCancel    = { vm.clearSelection() },
                        onSelectAll = { vm.selectAll(recordings.map { it.uri }) },
                        onShare     = {
                            val uris = ArrayList(selectedUris.toList())
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "audio/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Recordings"))
                        },
                        onSave      = { requestStorageThenPick() },
                        onDelete    = { showBulkDeleteConfirm = true }
                    )
                }
            }
        }
    }

    if (showBulkDeleteConfirm) {
        val count = selectedUris.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete $count Recording${if (count != 1) "s" else ""}?") },
            text  = { Text("This will permanently delete the selected recording${if (count != 1) "s" else ""}. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { showBulkDeleteConfirm = false; vm.deleteSelected(context) },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showFileSavePicker) {
        FileSavePickerDialog(
            count     = selectedUris.size,
            onDismiss = { showFileSavePicker = false },
            onSave    = { directory ->
                showFileSavePicker = false
                vm.saveSelectedToDirectory(context, directory)
                vm.clearSelection()
            }
        )
    }
}

// ── Floating selection action bar ─────────────────────────────────────────────

@Composable
private fun SelectionBar(
    count: Int,
    total: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape          = RoundedCornerShape(28.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 0.dp,
        border         = androidx.compose.foundation.BorderStroke(
                             0.8.dp,
                             MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                         ),
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel selection", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text  = "$count selected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            if (count < total) {
                Surface(
                    onClick = onSelectAll,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Select All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            IconButton(onClick = onSave, enabled = count > 0) {
                Icon(Icons.Outlined.Download, contentDescription = "Save selected", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onShare, enabled = count > 0) {
                Icon(Icons.Outlined.Share, contentDescription = "Share selected", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDelete, enabled = count > 0) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

/** Same look/behaviour as the "Search in Ever Dialer" pill shown atop the main Calls / Contacts
 *  / Favourites tabs: a non-editable pill that opens the app's single unified search screen
 *  (which also searches call recordings and recording notes) rather than filtering only the
 *  recordings list in place. */
@Composable
fun GlobalSearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Search in Ever Dialer",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Filter + Sort pills ────────────────────────────────────────────────────────

@Composable
private fun FilterPillRow(
    filterTab: FilterTab,
    sortConfig: SortConfig,
    onFilterChange: (FilterTab) -> Unit,
    onSortChange: (SortConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill(label = "All",        selected = filterTab == FilterTab.ALL,        icon = Icons.Outlined.List,          onClick = { onFilterChange(FilterTab.ALL) })
        FilterPill(label = "Favourites", selected = filterTab == FilterTab.FAVOURITES, icon = Icons.Outlined.FavoriteBorder, onClick = { onFilterChange(FilterTab.FAVOURITES) })
        Spacer(Modifier.weight(1f))
        Box {
            val sortLabel = when (sortConfig.field) {
                SortField.TIME, SortField.DATE -> "Date"
                SortField.NAME -> "Name"
            }
            FilterPill(
                label = sortLabel,
                selected = false,
                icon = Icons.Outlined.Sort,
                trailingIcon = if (sortConfig.order == SortOrder.DESC) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                onClick = { showSortMenu = true }
            )
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, shape = RoundedCornerShape(16.dp)) {
                SortOption.entries.forEach { opt ->
                    val selected = sortConfig.field == opt.field && sortConfig.order == opt.order
                    DropdownMenuItem(
                        text = { Text(opt.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                        leadingIcon = { Icon(opt.icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = if (selected) {{ Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }} else null,
                        onClick = { onSortChange(SortConfig(opt.field, opt.order)); showSortMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, icon: ImageVector, trailingIcon: ImageVector? = null, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor   = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(onClick = onClick, shape = CircleShape, color = containerColor, contentColor = contentColor, tonalElevation = 0.dp) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Sort options ──────────────────────────────────────────────────────────────

private enum class SortOption(val label: String, val field: SortField, val order: SortOrder, val icon: ImageVector) {
    DATE_DESC("Newest first",  SortField.TIME, SortOrder.DESC, Icons.Rounded.ArrowDownward),
    DATE_ASC ("Oldest first",  SortField.TIME, SortOrder.ASC,  Icons.Rounded.ArrowUpward),
    NAME_ASC ("Name A → Z",    SortField.NAME, SortOrder.ASC,  Icons.Rounded.ArrowDownward),
    NAME_DESC("Name Z → A",    SortField.NAME, SortOrder.DESC, Icons.Rounded.ArrowUpward)
}

// ── Date group header ─────────────────────────────────────────────────────────

@Composable
private fun DateGroupHeader(label: String, modifier: Modifier = Modifier) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp))
}

// ── Recording group card ──────────────────────────────────────────────────────

@Composable
private fun RecordingGroupCard(
    items: List<com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem>,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedUris: Set<android.net.Uri>,
    onFavouriteToggle: (com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem) -> Unit,
    onRecordingClick:  (com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem) -> Unit,
    onToggleSelect:    (com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEachIndexed { index, item ->
                RecordingRow(
                    item            = item,
                    searchQuery     = searchQuery,
                    isSelectionMode = isSelectionMode,
                    isSelected      = item.uri in selectedUris,
                    onFavouriteToggle = { onFavouriteToggle(item) },
                    onClick         = {
                        if (isSelectionMode) onToggleSelect(item)
                        else onRecordingClick(item)
                    },
                    onEnterSelectionMode = { onToggleSelect(item) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ── Single recording row ──────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordingRow(
    item: com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem,
    searchQuery: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onFavouriteToggle: () -> Unit,
    onClick: () -> Unit,
    onEnterSelectionMode: () -> Unit
) {
    val vm: HomeViewModel = viewModel()
    val context = LocalContext.current
    val isIncoming = item.direction == "in"
    val accentColor = if (isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val directionIcon  = if (isIncoming) Icons.Rounded.CallReceived else Icons.Rounded.CallMade
    val directionLabel = if (isIncoming) "Incoming" else "Outgoing"
    val timeStr    = item.date?.let { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(it) } ?: ""
    val displayName = item.contactName ?: item.phoneNumber

    var showMenu        by remember { mutableStateOf(false) }
    var showInfoDialog  by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var photoBitmap by remember(item.phoneNumber) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.phoneNumber) { photoBitmap = vm.loadContactPhoto(context, item.phoneNumber) }

    val lowerQuery = searchQuery.trim().lowercase()
    val noteSnippet: String? = remember(item.noteText, lowerQuery) {
        if (lowerQuery.isNotBlank() && item.noteText.lowercase().contains(lowerQuery))
            buildNoteSnippet(item.noteText, lowerQuery) else null
    }

    // Animated selection background
    val rowBg by animateColorAsState(
        targetValue  = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = tween(220),
        label = "rowBg"
    )

    Box(modifier = Modifier.background(rowBg)) {
        ListItem(
            modifier = Modifier.combinedClickable(
                onLongClick = { if (isSelectionMode) showMenu = true else onEnterSelectionMode() },
                onClick     = onClick
            ),
            leadingContent = {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    // Avatar alpha — fades out when selection mode enters
                    val avatarAlpha by animateFloatAsState(
                        targetValue   = if (isSelectionMode) 0f else 1f,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "avatarAlpha"
                    )
                    // Checkbox layer scale + alpha — animates in with a bouncy spring
                    val checkLayerScale by animateFloatAsState(
                        targetValue   = if (isSelectionMode) 1f else 0.65f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "checkLayerScale"
                    )
                    val checkLayerAlpha by animateFloatAsState(
                        targetValue   = if (isSelectionMode) 1f else 0f,
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        label = "checkLayerAlpha"
                    )
                    // Inner scale pulses when selected/deselected
                    val innerBounce by animateFloatAsState(
                        targetValue   = if (isSelected) 1f else 0.88f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "innerBounce"
                    )
                    val tickAlpha by animateFloatAsState(
                        targetValue   = if (isSelected) 1f else 0f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        label = "tickAlpha"
                    )
                    val tickScale by animateFloatAsState(
                        targetValue   = if (isSelected) 1f else 0.4f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tickScale"
                    )

                    // Avatar layer (always in tree, fades behind checkbox)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { alpha = avatarAlpha }
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoBitmap != null) {
                            Image(bitmap = photoBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            val initial = item.contactName?.firstOrNull()?.uppercaseChar()?.toString()
                                ?: item.phoneNumber.firstOrNull { it.isDigit() }?.toString() ?: "?"
                            Text(initial, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }

                    // Checkbox layer (overlaid, fades/scales in on selection mode)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { alpha = checkLayerAlpha; scaleX = checkLayerScale; scaleY = checkLayerScale }
                            .scale(innerBounce)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .then(if (isSelected) Modifier.border(2.dp, accentColor, CircleShape) else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp).graphicsLayer { alpha = tickAlpha; scaleX = tickScale; scaleY = tickScale }
                        )
                    }
                }
            },
            headlineContent = {
                Text(text = displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = directionIcon, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                        Text(text = directionLabel, style = MaterialTheme.typography.labelSmall, color = accentColor)
                        if (timeStr.isNotBlank()) {
                            Text("\u00b7", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.sizeBytes > 0) {
                            Text("\u00b7", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatSize(item.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (noteSnippet != null) {
                        val highlightColor = MaterialTheme.colorScheme.primary
                        val annotated = buildAnnotatedString {
                            val lower = noteSnippet.lowercase()
                            var start = 0
                            while (true) {
                                val idx = lower.indexOf(lowerQuery, start)
                                if (idx == -1) { withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(noteSnippet.substring(start)) }; break }
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(noteSnippet.substring(start, idx)) }
                                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold, background = highlightColor.copy(alpha = 0.15f))) { append(noteSnippet.substring(idx, idx + lowerQuery.length)) }
                                start = idx + lowerQuery.length
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Text(annotated, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            trailingContent = {
                AnimatedContent(
                    targetState = isSelectionMode,
                    transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(140))) },
                    label = "trailing"
                ) { inSelMode ->
                    if (!inSelMode) {
                        IconButton(onClick = onFavouriteToggle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (item.isFavourite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (item.isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        // Context menu — only when NOT already in selection mode from long-press on this item
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Select") },
                leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                onClick = { showMenu = false; onEnterSelectionMode() }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    showMenu = false
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, item.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Recording"))
                }
            )
            DropdownMenuItem(
                text = { Text("View Info") },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = { showMenu = false; showInfoDialog = true }
            )
            DropdownMenuItem(
                text = { Text(if (item.isFavourite) "Remove Favourite" else "Add to Favourites") },
                leadingIcon = { Icon(if (item.isFavourite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null) },
                onClick = { showMenu = false; onFavouriteToggle() }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; showDeleteConfirm = true }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon  = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Recording?") },
            text  = { Text("This will permanently delete the recording of ${item.contactName ?: item.phoneNumber}. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; vm.deleteRecording(context, item) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showInfoDialog) {
        RecordingInfoDialog(item = item, onDismiss = { showInfoDialog = false })
    }
}

@Composable
private fun RecordingInfoDialog(item: com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem, onDismiss: () -> Unit) {
    val isIncoming  = item.direction == "in"
    val accentColor = if (isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon  = {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = if (isIncoming) Icons.Rounded.CallReceived else Icons.Rounded.CallMade, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text("Recording Info", fontWeight = FontWeight.SemiBold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                InfoRow("Contact",   item.contactName ?: "\u2014")
                InfoRow("Number",    item.phoneNumber)
                InfoRow("Direction", if (isIncoming) "Incoming" else "Outgoing")
                InfoRow("Date",      item.date?.let { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(it) } ?: "\u2014")
                InfoRow("Time",      item.date?.let { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(it) } ?: "\u2014")
                InfoRow("Duration",  if (item.durationMs > 0L) formatDurationMs(item.durationMs) else "\u2014")
                InfoRow("Size",      formatSize(item.sizeBytes))
                InfoRow("Format",    item.extension.uppercase().ifBlank { "\u2014" })
                InfoRow("Favourite", if (item.isFavourite) "Yes" else "No")
                if (item.noteText.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(item.noteText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Call recording disabled banner ─────────────────────────────────────────────

@Composable
private fun CallRecordingDisabledBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFB71C1C),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Call recording is turned off",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Calls aren't being monitored or recorded. Tap to turn it on in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(isFavourites: Boolean, hasQuery: Boolean) {
    val icon  = when { hasQuery -> Icons.Outlined.SearchOff; isFavourites -> Icons.Outlined.FavoriteBorder; else -> Icons.Outlined.MicNone }
    val title = when { hasQuery -> "No results found"; isFavourites -> "No favourites yet"; else -> "No recordings yet" }
    val body  = when {
        hasQuery     -> "Try a different search term."
        isFavourites -> "Tap the heart icon on any recording to save it here."
        else         -> "Recordings will appear here once calls are captured."
    }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            }
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = body,  style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildNoteSnippet(noteText: String, query: String): String {
    val idx   = noteText.lowercase().indexOf(query.lowercase())
    if (idx == -1) return noteText.take(80)
    val start  = (idx - 20).coerceAtLeast(0)
    val end    = (idx + query.length + 40).coerceAtMost(noteText.length)
    val prefix = if (start > 0) "\u2026" else ""
    val suffix = if (end < noteText.length) "\u2026" else ""
    return "$prefix${noteText.substring(start, end)}$suffix"
}

private fun groupLabel(date: Date?): String {
    if (date == null) return "Unknown date"
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }
    return when {
        isSameDay(now, cal)  -> "Today"
        isYesterday(now, cal)-> "Yesterday"
        isSameWeek(now, cal) -> SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
        isSameYear(now, cal) -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(date)
        else                 -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
private fun isYesterday(now: Calendar, b: Calendar): Boolean { val y = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }; return isSameDay(y, b) }
private fun isSameWeek(now: Calendar, b: Calendar) = now.get(Calendar.YEAR) == b.get(Calendar.YEAR) && now.get(Calendar.WEEK_OF_YEAR) == b.get(Calendar.WEEK_OF_YEAR)
private fun isSameYear(now: Calendar, b: Calendar) = now.get(Calendar.YEAR) == b.get(Calendar.YEAR)
private fun formatSize(bytes: Long): String = when { bytes < 1024 -> "${bytes}B"; bytes < 1024 * 1024 -> "${bytes / 1024}KB"; else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB" }
private fun formatDurationMs(ms: Long): String { val mins = ms / 60_000; val secs = (ms % 60_000) / 1_000; return "%d:%02d".format(mins, secs) }
