package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.FakeCallEntry
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

private val DAY_LABELS = listOf("S", "M", "T", "W", "T", "F", "S")
// Calendar.DAY_OF_WEEK: 1=Sun … 7=Sat
private val DAY_VALUES = listOf(1, 2, 3, 4, 5, 6, 7)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun FakeCallScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var entries by remember { mutableStateOf(FakeCallManager.loadEntries(prefs)) }

    // Refresh list periodically to reflect updated triggerAt times
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            entries = FakeCallManager.loadEntries(prefs)
        }
    }

    // FAB mini-menu visibility
    var fabExpanded by remember { mutableStateOf(false) }

    // Top-bar overflow menu ("Fake call in Context menu" toggle)
    var showOverflowMenu by remember { mutableStateOf(false) }
    val settingsVer by prefs.settingsChanged.collectAsState()
    val fakeCallInContextMenu = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
    }

    // Add sheet state
    var showAddSheet by remember { mutableStateOf(false) }
    var addMode by remember { mutableStateOf<AddMode?>(null) } // contact or number

    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }
    val screenAlpha by animateFloatAsState(
        targetValue = if (screenVisible) 1f else 0f,
        animationSpec = tween(320),
        label = "fcAlpha"
    )

    if (showAddSheet && addMode != null) {
        FakeCallAddSheet(
            mode = addMode!!,
            onDismiss = { showAddSheet = false; addMode = null },
            onSave = { entry, exactTriggerOverride ->
                FakeCallManager.addEntry(context, prefs, entry, exactTriggerOverride)
                entries = FakeCallManager.loadEntries(prefs)
                showAddSheet = false
                addMode = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fake Call", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        RivoDropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            FakeCallMenuCheckboxItem(
                                text = "Fake call in Context menu",
                                checked = fakeCallInContextMenu,
                                onCheckedChange = { checked ->
                                    prefs.setBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, checked)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mini menu
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + scaleIn(initialScale = 0.7f, transformOrigin = TransformOrigin(1f, 1f)),
                    exit = fadeOut() + scaleOut(targetScale = 0.7f, transformOrigin = TransformOrigin(1f, 1f))
                ) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FabMiniItem(label = "Choose Contact", icon = Icons.Outlined.Person) {
                            fabExpanded = false
                            addMode = AddMode.Contact
                            showAddSheet = true
                        }
                        FabMiniItem(label = "Enter Number", icon = Icons.Outlined.Dialpad) {
                            fabExpanded = false
                            addMode = AddMode.Number
                            showAddSheet = true
                        }
                    }
                }

                // Main FAB
                val fabRotation by animateFloatAsState(
                    targetValue = if (fabExpanded) 45f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "fabRotation"
                )
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add, "Add",
                        modifier = Modifier.graphicsLayer { rotationZ = fabRotation }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val emptyPulse by rememberInfiniteTransition(label = "ep").animateFloat(
                        0.92f, 1.06f,
                        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "ep"
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(emptyPulse)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PhoneCallback,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("No Fake Calls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap + to schedule a fake incoming call",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(screenAlpha),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        FakeCallCard(
                            entry = entry,
                            onToggle = { enabled ->
                                FakeCallManager.setEnabled(context, prefs, entry.id, enabled)
                                entries = FakeCallManager.loadEntries(prefs)
                            },
                            onDelete = {
                                FakeCallManager.removeEntry(context, prefs, entry.id)
                                entries = FakeCallManager.loadEntries(prefs)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // Tap-outside catcher for the fab menu — no dimming, just lets you tap away to close it
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { fabExpanded = false }
                )
            }
        }
    }
}

// ─── Overflow Menu Checkbox Item ───────────────────────────────────────────────

@Composable
private fun FakeCallMenuCheckboxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// ─── FAB Mini Item ─────────────────────────────────────────────────────────────

@Composable
private fun FabMiniItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    // The parent AnimatedVisibility scale/fade-in transform clips elevation shadows while it's
    // mid-animation, so the pill's shadow is ramped in afterwards — smooth and clearly visible.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180)
        settled = true
    }
    val labelShadow by animateDpAsState(
        targetValue = if (settled) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "fabMiniLabelShadow"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = labelShadow,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
        }
    }
}

// ─── Pick Time mode tab (Set Clock / Set Timer) ───────────────────────────────

@Composable
private fun TimeModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "tabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent"
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}

// ─── Timer unit toggle (Sec / Min) ─────────────────────────────────────────────

@Composable
private fun TimerUnitButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "unitBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "unitContent"
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

// ─── Fake Call Card ───────────────────────────────────────────────────────────

@Composable
private fun FakeCallCard(
    entry: FakeCallEntry,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val nextLabel = remember(entry.triggerAt) {
        if (entry.triggerAt <= 0L || !entry.enabled) ""
        else "Rings ${timeFmt.format(Date(entry.triggerAt))}"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val itemAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "cardAlpha"
    )
    val itemScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = itemAlpha; scaleX = itemScale; scaleY = itemScale }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (entry.displayName.firstOrNull()?.uppercaseChar() ?: '#').toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", entry.hour, entry.minute)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                    Text(
                        timeStr + if (entry.days.isNotEmpty()) " · ${entry.days.map { DAY_LABELS[it - 1] }.joinToString("")}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (nextLabel.isNotEmpty()) {
                    Text(nextLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Switch(
                    checked = entry.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.85f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─── Add Sheet ────────────────────────────────────────────────────────────────

enum class AddMode { Contact, Number }
private enum class TimePickMode { Clock, Timer }
private enum class TimerUnit { Seconds, Minutes }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeCallAddSheet(
    mode: AddMode,
    onDismiss: () -> Unit,
    onSave: (FakeCallEntry, Long?) -> Unit,
    initialNumber: String = "",
    initialDisplayName: String = ""
) {
    val context = LocalContext.current

    // Contact/number state
    var displayName by remember {
        mutableStateOf(initialDisplayName.ifBlank { if (mode == AddMode.Number) initialNumber else "" })
    }
    var phoneNumber by remember { mutableStateOf(initialNumber) }
    var numberInput by remember { mutableStateOf(TextFieldValue(initialNumber)) }

    // Time state
    val cal = remember { Calendar.getInstance() }
    var hour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }

    // Pick Time mode: an absolute clock time, or a relative countdown timer
    var timePickMode by remember { mutableStateOf(TimePickMode.Clock) }
    var timerAmountText by remember { mutableStateOf("30") }
    var timerUnit by remember { mutableStateOf(TimerUnit.Seconds) }

    // Day state
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }

    // Contact picker launcher
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uri = data.data ?: return@rememberLauncherForActivityResult
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val name = if (nameIdx >= 0) c.getString(nameIdx) else ""
                    val contactId = if (idIdx >= 0) c.getString(idIdx) else null
                    displayName = name

                    // Fetch phone
                    if (contactId != null) {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId), null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                phoneNumber = if (numIdx >= 0) pc.getString(numIdx) else ""
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Launch contact picker immediately for Contact mode
    LaunchedEffect(mode) {
        if (mode == AddMode.Contact) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactLauncher.launch(intent)
        }
    }

    // Time picker dialog
    val timePicker = remember {
        TimePickerDialog(context, { _, h, m ->
            hour = h
            minute = m
        }, hour, minute, false)
    }

    val timeStr = remember(hour, minute) {
        val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute) }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(c.time)
    }

    val timerAmount = timerAmountText.toIntOrNull() ?: 0
    val timerDescription = remember(timerAmount, timerUnit) {
        val unitLabel = when {
            timerUnit == TimerUnit.Seconds && timerAmount == 1 -> "second"
            timerUnit == TimerUnit.Seconds -> "seconds"
            timerAmount == 1 -> "minute"
            else -> "minutes"
        }
        "$timerAmount $unitLabel"
    }

    val canSave = displayName.isNotBlank() &&
        (phoneNumber.isNotBlank() || numberInput.text.isNotBlank()) &&
        (timePickMode == TimePickMode.Clock || timerAmount > 0)

    var scaleIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { scaleIn = true }
    val dialogScale by animateFloatAsState(
        targetValue = if (scaleIn) 1f else 0.82f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sheetScale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue = if (scaleIn) 1f else 0f,
        animationSpec = tween(220),
        label = "sheetAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = dialogAlpha; scaleX = dialogScale; scaleY = dialogScale }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (mode == AddMode.Contact) Icons.Outlined.Person else Icons.Outlined.Dialpad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            if (mode == AddMode.Contact) "Choose Contact" else "Enter Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Schedule a realistic fake call",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Contact display or number input
                if (mode == AddMode.Contact) {
                    if (displayName.isNotBlank()) {
                        // Show selected contact pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        displayName.first().uppercaseChar().toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (phoneNumber.isNotBlank()) {
                                        Text(phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                        contactLauncher.launch(intent)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                contactLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pick a Contact")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = {
                            numberInput = it
                            phoneNumber = it.text
                            if (displayName.isBlank()) displayName = it.text
                        },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Outlined.Dialpad, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    if (numberInput.text.isNotBlank()) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name (optional)") },
                            leadingIcon = { Icon(Icons.Outlined.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Pick Time ──
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pick Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

                    // Set Clock / Set Timer toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TimeModeTab(
                                label = "Set Clock",
                                icon = Icons.Outlined.AccessTime,
                                selected = timePickMode == TimePickMode.Clock,
                                onClick = { timePickMode = TimePickMode.Clock },
                                modifier = Modifier.weight(1f)
                            )
                            TimeModeTab(
                                label = "Set Timer",
                                icon = Icons.Outlined.Timer,
                                selected = timePickMode == TimePickMode.Timer,
                                onClick = { timePickMode = TimePickMode.Timer },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    AnimatedContent(targetState = timePickMode, label = "timeModeContent") { mode ->
                        when (mode) {
                            TimePickMode.Clock -> {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().clickable { timePicker.show() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        Text(
                                            timeStr,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            TimePickMode.Timer -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                            OutlinedTextField(
                                                value = timerAmountText,
                                                onValueChange = { newVal -> timerAmountText = newVal.filter { it.isDigit() }.take(3) },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.width(78.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                                TimerUnitButton(
                                                    label = "Sec",
                                                    selected = timerUnit == TimerUnit.Seconds,
                                                    onClick = { timerUnit = TimerUnit.Seconds },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TimerUnitButton(
                                                    label = "Min",
                                                    selected = timerUnit == TimerUnit.Minutes,
                                                    onClick = { timerUnit = TimerUnit.Minutes },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                        Text(
                                            "Rings once, in $timerDescription",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Day Selector (Set Clock mode only — a one-off timer never repeats) ──
                if (timePickMode == TimePickMode.Clock) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Repeat Days", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "Optional",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DAY_VALUES.forEachIndexed { i, dayVal ->
                                val label = DAY_LABELS[i]
                                val selected = dayVal in selectedDays
                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.08f else 1f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "dayScale$i"
                                )
                                // The outer 40dp slot reserves fixed, un-scaled layout space; only
                                // the inner 34dp circle scales on selection, so the bounce effect
                                // never bleeds into a neighboring day button.
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .scale(scale)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = if (selected) 0.dp else 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedDays = if (selected) selectedDays - dayVal else selectedDays + dayVal
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        // Weekday/weekend quick selects
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = setOf(2, 3, 4, 5, 6).all { it in selectedDays },
                                onClick = {
                                    val weekdays = setOf(2, 3, 4, 5, 6)
                                    selectedDays = if (weekdays.all { it in selectedDays })
                                        selectedDays - weekdays else selectedDays + weekdays
                                },
                                label = { Text("Weekdays", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(30.dp)
                            )
                            FilterChip(
                                selected = setOf(1, 7).all { it in selectedDays },
                                onClick = {
                                    val weekend = setOf(1, 7)
                                    selectedDays = if (weekend.all { it in selectedDays })
                                        selectedDays - weekend else selectedDays + weekend
                                },
                                label = { Text("Weekend", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(30.dp)
                            )
                            if (selectedDays.isNotEmpty()) {
                                FilterChip(
                                    selected = false,
                                    onClick = { selectedDays = emptySet() },
                                    label = { Text("Clear", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }

                // ── Save button ──
                Button(
                    onClick = {
                        val finalName = displayName.ifBlank { phoneNumber.ifBlank { numberInput.text } }
                        val finalNumber = phoneNumber.ifBlank { numberInput.text }

                        if (timePickMode == TimePickMode.Timer) {
                            val delayMillis = if (timerUnit == TimerUnit.Seconds) timerAmount * 1_000L else timerAmount * 60_000L
                            val exactTriggerAt = System.currentTimeMillis() + delayMillis
                            val targetCal = Calendar.getInstance().apply { timeInMillis = exactTriggerAt }
                            val entry = FakeCallEntry(
                                id = UUID.randomUUID().toString(),
                                displayName = finalName,
                                phoneNumber = finalNumber,
                                hour = targetCal.get(Calendar.HOUR_OF_DAY),
                                minute = targetCal.get(Calendar.MINUTE),
                                days = emptySet(),
                                enabled = true
                            )
                            onSave(entry, exactTriggerAt)
                        } else {
                            val entry = FakeCallEntry(
                                id = UUID.randomUUID().toString(),
                                displayName = finalName,
                                phoneNumber = finalNumber,
                                hour = hour,
                                minute = minute,
                                days = selectedDays,
                                enabled = true
                            )
                            onSave(entry, null)
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Fake Call", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}


