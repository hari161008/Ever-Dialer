package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.MissedCallDurationStore
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorBlue   = Color(0xFF2196F3)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorPurple = Color(0xFF9C27B0)
private val ColorTeal   = Color(0xFF009688)
private val ColorRed    = Color(0xFFE53935)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorOrange = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CallLogsSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val keyboardController = LocalSoftwareKeyboardController.current
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var groupCallsByLatest by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)) }
    var showSimsInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault())) }
    var showTotalCallsMade by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)) }
    var nameNonContactsAsUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, true)) }
    var showNumbersInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, true)) }
    var autoDeleteUnknownEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED, false)) }
    var autoDeleteUnknownValue by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, 1).toString()) }
    var autoDeleteUnknownUnit by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "days") ?: "days") }
    var callTimeFormat24h by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false)) }
    var showTalkTimeInCallLogs by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, false)) }
    var showMissedCallDuration by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_MISSED_CALL_DURATION, false)) }
    var autoResetMissedCallDurationEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_ENABLED, false)) }
    var autoResetMissedCallDurationValue by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_VALUE, 24).toString()) }
    var autoResetMissedCallDurationUnit by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_UNIT, "hours") ?: "hours") }

    // Call UI section checkboxes dialog
    var showCallUIDialog   by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showDefaultCategoryDialog by remember { mutableStateOf(false) }
    var defaultCallCategory by remember { mutableStateOf(prefs.getDefaultCallCategory()) }
    var callUIShowToday    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY, true)) }
    var callUIShowMissed   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED, true)) }
    var callUIShowOutgoing by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true)) }
    var callUIShowCallTime by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true)) }
    var callUIShowContacts by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CONTACTS, false)) }
    var callUIShowGroups   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_GROUPS, false)) }

    data class CallUIOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val callUIOptions = listOf(
        CallUIOption("today",     "Today",     Icons.AutoMirrored.Filled.CallReceived),
        CallUIOption("missed",    "Missed",    Icons.AutoMirrored.Filled.CallMissed),
        CallUIOption("outgoing",  "Outgoing",  Icons.AutoMirrored.Filled.CallMade),
        CallUIOption("call_time", "Call Time", Icons.Default.Timer),
        CallUIOption("contacts",  "Contacts",  Icons.Default.People),
        CallUIOption("groups",    "Groups",    Icons.Default.Group)
    )

    val callUIOrder = remember {
        mutableStateListOf<String>().apply {
            val saved = prefs.getString(PreferenceManager.KEY_CALL_UI_ORDER, null)
            val savedKeys = PreferenceManager.parseCallUIOrder(saved)
            addAll(savedKeys)
        }
    }
    fun persistCallUIOrder() {
        prefs.setString(PreferenceManager.KEY_CALL_UI_ORDER, callUIOrder.joinToString(","))
    }
    fun resetCallUIToDefault() {
        val defaults = PreferenceManager.DEFAULT_CALL_UI_ORDER.split(",").map { it.trim() }.filter { it.isNotBlank() }
        callUIOrder.clear()
        callUIOrder.addAll(defaults)
        persistCallUIOrder()
        callUIShowToday    = true;  prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY,    true)
        callUIShowMissed   = true;  prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED,   true)
        callUIShowOutgoing = true;  prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true)
        callUIShowCallTime = true;  prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true)
        callUIShowContacts = false; prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CONTACTS,  false)
        callUIShowGroups   = false; prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_GROUPS,    false)
    }

    if (showCallUIDialog) {
        val density = LocalDensity.current
        val haptic = LocalHapticFeedback.current
        val rowHeightDp = 52.dp
        val rowHeightPx = with(density) { rowHeightDp.toPx() }
        var draggedKey by remember { mutableStateOf<String?>(null) }
        var dragOffsetY by remember { mutableStateOf(0f) }

        fun callUIChecked(key: String): Boolean = when (key) {
            "today"     -> callUIShowToday
            "missed"    -> callUIShowMissed
            "outgoing"  -> callUIShowOutgoing
            "call_time" -> callUIShowCallTime
            "contacts"  -> callUIShowContacts
            "groups"    -> callUIShowGroups
            else        -> true
        }
        fun setCallUIChecked(key: String, value: Boolean) {
            when (key) {
                "today"     -> { callUIShowToday = value;    prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY,    value) }
                "missed"    -> { callUIShowMissed = value;   prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED,   value) }
                "outgoing"  -> { callUIShowOutgoing = value; prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, value) }
                "call_time" -> { callUIShowCallTime = value; prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, value) }
                "contacts"  -> { callUIShowContacts = value; prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CONTACTS,  value) }
                "groups"    -> { callUIShowGroups = value;   prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_GROUPS,    value) }
            }
        }

        AlertDialog(
            onDismissRequest = { showCallUIDialog = false },
            icon = { Icon(Icons.Default.Dashboard, null, tint = ColorOrange) },
            title = { Text("Calls Section Elements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Toggle which stat cards appear in the Calls home screen, and drag the handle to reorder them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    val maxListHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.55f).coerceAtLeast(240.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxListHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        callUIOrder.forEach { itemKey ->
                            val option = callUIOptions.firstOrNull { it.key == itemKey } ?: return@forEach
                            val isDragging = draggedKey == itemKey
                            key(itemKey) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = if (isDragging) 6.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .zIndex(if (isDragging) 2f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(rowHeightDp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = callUIChecked(itemKey),
                                            onCheckedChange = { setCallUIChecked(itemKey, it) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "Reorder ${option.label}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .pointerInput(itemKey) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggedKey = itemKey
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                            persistCallUIOrder()
                                                        },
                                                        onDragCancel = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val currentIdx = callUIOrder.indexOf(itemKey)
                                                            if (currentIdx != -1) {
                                                                val threshold = rowHeightPx * 0.6f
                                                                if (dragOffsetY > threshold && currentIdx < callUIOrder.lastIndex) {
                                                                    val item = callUIOrder.removeAt(currentIdx)
                                                                    callUIOrder.add(currentIdx + 1, item)
                                                                    dragOffsetY -= rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                                    val item = callUIOrder.removeAt(currentIdx)
                                                                    callUIOrder.add(currentIdx - 1, item)
                                                                    dragOffsetY += rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCallUIDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { resetCallUIToDefault() }) { Text("Default") }
            }
        )
    }

    if (showCategoriesDialog) {
        com.coolappstore.everdialer.by.svhp.view.components.CallCategoriesDialog(
            onDismissRequest = { showCategoriesDialog = false },
            onAddClick = {
                showCategoriesDialog = false
                navigator.navigate(com.ramcosta.composedestinations.generated.destinations.CreateCallCategoryScreenDestination())
            },
            onEditClick = { categoryId ->
                showCategoriesDialog = false
                navigator.navigate(com.ramcosta.composedestinations.generated.destinations.CreateCallCategoryScreenDestination(categoryId = categoryId))
            }
        )
    }

    if (showDefaultCategoryDialog) {
        val visibleCategories = remember(showDefaultCategoryDialog) {
            val all = prefs.getAllCallCategories()
            val hidden = prefs.getHiddenCallCategories()
            all.filterNot { hidden.contains(it.id) }
        }
        AlertDialog(
            onDismissRequest = { showDefaultCategoryDialog = false },
            icon = { Icon(Icons.Default.Tab, null, tint = ColorPurple) },
            title = { Text("Default Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose which category opens when the call logs screen starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    visibleCategories.forEach { option ->
                        val isSelected = defaultCallCategory == option.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        defaultCallCategory = option.id
                                        prefs.setDefaultCallCategory(option.id)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (option.isCustom) Icons.Default.People else Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    option.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        defaultCallCategory = option.id
                                        prefs.setDefaultCallCategory(option.id)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultCategoryDialog = false }) { Text("Done") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Call Logs",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Calls Screen Elements",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Calls Section Elements",
                            supporting = "Toggle and drag to reorder Today, Missed, Outgoing, Call Time, Contacts, Groups cards",
                            leadingIcon = Icons.Default.Dashboard,
                            iconContainerColor = ColorOrange,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("calls_section_elements", highlightedKey) { highlightedKey = null },
                            onClick = { showCallUIDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        RivoListItem(
                            headline = "Category",
                            supporting = "Customize pills, reorder, visibility & custom categories",
                            leadingIcon = Icons.Default.Category,
                            iconContainerColor = ColorPurple,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("call_category", highlightedKey) { highlightedKey = null },
                            onClick = { showCategoriesDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        val allCallCategories = remember(showCategoriesDialog, showDefaultCategoryDialog) {
                            prefs.getAllCallCategories()
                        }
                        val currentCallCategoryLabel = remember(defaultCallCategory, allCallCategories) {
                            allCallCategories.firstOrNull { it.id == defaultCallCategory }?.name ?: "All"
                        }
                        RivoListItem(
                            headline = "Default Category",
                            supporting = "Choose which category opens when the call logs screen starts (currently: $currentCallCategoryLabel)",
                            leadingIcon = Icons.Default.Tab,
                            iconContainerColor = ColorPurple,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("call_default_category", highlightedKey) { highlightedKey = null },
                            onClick = { showDefaultCategoryDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 40L) {
                Column {
                    Text(
                        "Call Log Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Group numbers in call logs based on latest calls",
                            supporting = "Sort call logs by latest calls made and group numbers, showing total call count",
                            leadingIcon = Icons.Outlined.Layers,
                            iconContainerColor = ColorBlue,
                            checked = groupCallsByLatest,
                            modifier = Modifier.settingsSearchHighlight("group_calls_by_latest", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                groupCallsByLatest = it
                                prefs.setBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, it)
                                if (it) {
                                    showTotalCallsMade = true
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, true)
                                } else {
                                    showTotalCallsMade = false
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
                                }
                            }
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "In call logs, long pressing the all button will show a menu to choose grouped based on number or by date.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show Sims In Call Logs",
                            supporting = "Show a SIM icon with its number on calls in Call Logs",
                            leadingIcon = Icons.Outlined.SimCard,
                            iconContainerColor = ColorGreen,
                            checked = showSimsInCallLogs,
                            modifier = Modifier.settingsSearchHighlight("show_sims_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showSimsInCallLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show total number of calls made",
                            supporting = "Show the total call count for each contact or number in Call Logs",
                            leadingIcon = Icons.Outlined.Call,
                            iconContainerColor = ColorPurple,
                            checked = showTotalCallsMade,
                            modifier = Modifier.settingsSearchHighlight("show_total_calls_made", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showTotalCallsMade = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Name non contacts as Unknown",
                            supporting = if (nameNonContactsAsUnknown) "Show \"Unknown\" as the name for numbers not in your contacts" else "Directly show the phone number instead of \"Unknown\"",
                            leadingIcon = Icons.Outlined.PersonOff,
                            iconContainerColor = ColorTeal,
                            checked = nameNonContactsAsUnknown,
                            modifier = Modifier.settingsSearchHighlight("name_non_contacts_as_unknown", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                nameNonContactsAsUnknown = it
                                prefs.setBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show numbers in call logs",
                            supporting = "Show phone numbers below contact names in call logs. When disabled, numbers are hidden for saved contacts",
                            leadingIcon = Icons.Outlined.Numbers,
                            iconContainerColor = ColorBlue,
                            checked = showNumbersInCallLogs,
                            modifier = Modifier.settingsSearchHighlight("show_numbers_in_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showNumbersInCallLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Call Time Format in call logs",
                            supporting = if (callTimeFormat24h) "Showing call times in 24-hour format" else "Showing call times in 12-hour format",
                            leadingIcon = Icons.Outlined.Schedule,
                            iconContainerColor = ColorAmber,
                            checked = callTimeFormat24h,
                            modifier = Modifier.settingsSearchHighlight("call_time_format", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                callTimeFormat24h = it
                                prefs.setBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Talk time in call logs",
                            supporting = if (showTalkTimeInCallLogs) "Showing call duration for answered calls below the time" else "Call duration hidden in call logs",
                            leadingIcon = Icons.Outlined.Timer,
                            iconContainerColor = ColorGreen,
                            checked = showTalkTimeInCallLogs,
                            modifier = Modifier.settingsSearchHighlight("talk_time_in_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showTalkTimeInCallLogs = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Missed Call Duration",
                            supporting = if (showMissedCallDuration) "Showing ringing duration for missed calls below the time" else "Missed call duration hidden in call logs",
                            leadingIcon = Icons.AutoMirrored.Filled.CallMissed,
                            iconContainerColor = ColorRed,
                            checked = showMissedCallDuration,
                            modifier = Modifier.settingsSearchHighlight("missed_call_duration", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showMissedCallDuration = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_MISSED_CALL_DURATION, it)
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoResetMissedCallDurationEnabled = !autoResetMissedCallDurationEnabled
                                    prefs.setBoolean(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_ENABLED, autoResetMissedCallDurationEnabled)
                                    if (autoResetMissedCallDurationEnabled) {
                                        val n = autoResetMissedCallDurationValue.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        autoResetMissedCallDurationValue = n.toString()
                                        prefs.setInt(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_VALUE, n)
                                        prefs.setString(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_UNIT, autoResetMissedCallDurationUnit)
                                    }
                                }
                                .padding(start = 24.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                        ) {
                            Checkbox(
                                checked = autoResetMissedCallDurationEnabled,
                                onCheckedChange = { checked ->
                                    autoResetMissedCallDurationEnabled = checked
                                    prefs.setBoolean(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_ENABLED, checked)
                                    if (checked) {
                                        val n = autoResetMissedCallDurationValue.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        autoResetMissedCallDurationValue = n.toString()
                                        prefs.setInt(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_VALUE, n)
                                        prefs.setString(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_UNIT, autoResetMissedCallDurationUnit)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Reset missed call duration every",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 68.dp, end = 16.dp, bottom = 8.dp, top = 2.dp)
                        ) {
                            OutlinedTextField(
                                value = autoResetMissedCallDurationValue,
                                onValueChange = { raw ->
                                    val digitsOnly = raw.filter { it.isDigit() }.take(4)
                                    autoResetMissedCallDurationValue = digitsOnly
                                    val n = digitsOnly.toIntOrNull()
                                    if (n != null && n > 0) {
                                        prefs.setInt(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_VALUE, n)
                                    }
                                },
                                enabled = autoResetMissedCallDurationEnabled,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(90.dp)
                            )
                            FilterChip(
                                selected = autoResetMissedCallDurationUnit == "hours",
                                onClick = {
                                    autoResetMissedCallDurationUnit = "hours"
                                    prefs.setString(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_UNIT, "hours")
                                },
                                enabled = autoResetMissedCallDurationEnabled,
                                label = { Text("Hours") }
                            )
                            FilterChip(
                                selected = autoResetMissedCallDurationUnit == "days",
                                onClick = {
                                    autoResetMissedCallDurationUnit = "days"
                                    prefs.setString(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_UNIT, "days")
                                },
                                enabled = autoResetMissedCallDurationEnabled,
                                label = { Text("Days") }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 68.dp, end = 16.dp, bottom = 14.dp, top = 4.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    MissedCallDurationStore.clear(context)
                                    prefs.setLong(PreferenceManager.KEY_AUTO_RESET_MISSED_CALL_DURATION_LAST_RESET, System.currentTimeMillis())
                                    prefs.notifyChanged()
                                    Toast.makeText(context, "Missed call duration reset", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Reset right now")
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Auto Delete Unknown No in call log",
                            supporting = if (autoDeleteUnknownEnabled)
                                "Deletes call log entries from unsaved numbers older than $autoDeleteUnknownValue ${if (autoDeleteUnknownUnit == "hours") "hour(s)" else "day(s)"}"
                            else "Set a duration below, then turn this on to remove old call log entries from numbers not in your contacts",
                            leadingIcon = Icons.Outlined.AutoDelete,
                            iconContainerColor = ColorRed,
                            checked = autoDeleteUnknownEnabled,
                            modifier = Modifier.settingsSearchHighlight("auto_delete_unknown_calllog", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { enabled ->
                                autoDeleteUnknownEnabled = enabled
                                prefs.setBoolean(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED, enabled)
                                if (enabled) {
                                    val n = autoDeleteUnknownValue.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    autoDeleteUnknownValue = n.toString()
                                    prefs.setInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, n)
                                    prefs.setString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, autoDeleteUnknownUnit)
                                    prefs.setLong(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED_AT, System.currentTimeMillis())
                                }
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(start = 68.dp, end = 16.dp, bottom = 14.dp, top = 2.dp)
                        ) {
                            OutlinedTextField(
                                value = autoDeleteUnknownValue,
                                onValueChange = { raw ->
                                    val digitsOnly = raw.filter { it.isDigit() }.take(4)
                                    autoDeleteUnknownValue = digitsOnly
                                    val n = digitsOnly.toIntOrNull()
                                    if (n != null && n > 0) {
                                        prefs.setInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, n)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(90.dp)
                            )
                            FilterChip(
                                selected = autoDeleteUnknownUnit == "days",
                                onClick = {
                                    autoDeleteUnknownUnit = "days"
                                    prefs.setString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "days")
                                },
                                label = { Text("Days") }
                            )
                            FilterChip(
                                selected = autoDeleteUnknownUnit == "hours",
                                onClick = {
                                    autoDeleteUnknownUnit = "hours"
                                    prefs.setString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "hours")
                                },
                                label = { Text("Hours") }
                            )
                        }
                    }
                }
            }
        }
    }
}
