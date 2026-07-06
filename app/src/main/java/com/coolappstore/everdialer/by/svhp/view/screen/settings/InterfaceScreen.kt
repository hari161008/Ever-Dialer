package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import android.os.Build
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

private val ColorPurple = Color(0xFF9C27B0)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)
private val ColorIndigo = Color(0xFF3F51B5)

data class ThemeOption(val key: String, val label: String)

private val themeOptions = listOf(
    ThemeOption("auto",    "Auto"),
    ThemeOption("light",   "Light"),
    ThemeOption("dark",    "Dark"),
    ThemeOption("white",   "White"),
    ThemeOption("black",   "Black"),
    ThemeOption("auto_bw", "Auto B/W")
)

private fun triggerRestartPrompt(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context
) {
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = "Restart required to apply theme changes fully.",
            actionLabel = "Restart",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            (context as? Activity)?.recreate()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InterfaceScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var themeMode           by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto") }
    var dynamicColors       by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)) }
    var showFirstLetter     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)) }
    var colorfulAvatars     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)) }
    var showPicture         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)) }
    var iconOnlyNav         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)) }
    var pillNav             by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true)) }
    var showSimsInCallLogs  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, true)) }
    var customPrimaryColor  by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }
    var showIncomingCallUI  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_INCOMING_CALL_UI, true)) }
    var showCallerUI        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALLER_UI, true)) }
    var openDialpadDefault  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) }
    var scrollAnimation     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, true)) }
    var liquidGlass         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false)) }
    var blurEffects         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false)) }

    // Call UI section checkboxes dialog
    var showCallUIDialog   by remember { mutableStateOf(false) }
    var callUIShowToday    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY, true)) }
    var callUIShowMissed   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED, true)) }
    var callUIShowOutgoing by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true)) }
    var callUIShowCallTime by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true)) }

    // Default Tab dialog
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var defaultTab           by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_DEFAULT_TAB, "calls") ?: "calls") }

    var showTabSectionsDialog by remember { mutableStateOf(false) }
    var tabShowFavorites  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)) }
    var tabShowCalls      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)) }
    var tabShowContacts   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)) }
    var tabShowRecordings by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)) }
    var tabShowNotes      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)) }
    data class TabOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tabOptions = listOf(
        TabOption("favorites",  "Favourites", Icons.Outlined.FavoriteBorder),
        TabOption("calls",      "Calls",      Icons.Outlined.History),
        TabOption("contacts",   "Contacts",   Icons.Outlined.Person),
        TabOption("recordings", "Recordings", Icons.Outlined.FiberManualRecord),
        TabOption("notes",      "Note",       Icons.Outlined.Note)
    )

    // Custom order of tab keys, persisted as a comma-separated string. Any tab keys
    // missing from a previously-saved (older) order are appended so new tabs always show.
    val tabOrder = remember {
        mutableStateListOf<String>().apply {
            val saved = prefs.getString(PreferenceManager.KEY_TAB_ORDER, null)
            val savedKeys = saved?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val validKeys = tabOptions.map { it.key }
            addAll(savedKeys.filter { it in validKeys })
            validKeys.forEach { key -> if (key !in this) add(key) }
        }
    }
    fun persistTabOrder() {
        prefs.setString(PreferenceManager.KEY_TAB_ORDER, tabOrder.joinToString(","))
    }

    // ── Context Menu Elements ──────────────────────────────────────────────
    // Top level: 3 fixed sections (Favourites, Call Logs, Contacts) — these are just
    // navigation rows (no checkbox/drag here). Tapping one opens a sub-dialog listing
    // that section's actual long-press context menu entries, each with a checkbox to
    // show/hide it and a drag handle to reorder it (same pattern as Tab Sections).
    var showContextMenuDialog by remember { mutableStateOf(false) }
    var activeContextMenuSection by remember { mutableStateOf<String?>(null) }

    data class ContextMenuSection(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val contextMenuSections = listOf(
        ContextMenuSection("favorites", "Favourites", Icons.Outlined.FavoriteBorder),
        ContextMenuSection("call_logs", "Call Logs",  Icons.Outlined.History),
        ContextMenuSection("contacts",  "Contacts",   Icons.Outlined.Person)
    )

    data class ContextMenuItemOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val contextMenuItemsBySection = mapOf(
        "favorites" to listOf(
            ContextMenuItemOption("select",          "Select",                    Icons.Default.CheckBox),
            ContextMenuItemOption("call",             "Call",                      Icons.Default.Call),
            ContextMenuItemOption("send_sms",         "Send SMS",                  Icons.Default.Message),
            ContextMenuItemOption("view_details",     "View Details",              Icons.Default.Info),
            ContextMenuItemOption("fake_call",        "Fake Call",                 Icons.Outlined.PhoneCallback),
            ContextMenuItemOption("remove_favorite",  "Remove from Favourites",    Icons.Default.Favorite)
        ),
        "call_logs" to listOf(
            ContextMenuItemOption("select",           "Select",                    Icons.Default.CheckBox),
            ContextMenuItemOption("call_back",         "Call back",                 Icons.Default.Call),
            ContextMenuItemOption("copy_number",       "Copy number",               Icons.Default.ContentCopy),
            ContextMenuItemOption("add_to_contacts",   "Add to contacts",           Icons.Default.PersonAdd),
            ContextMenuItemOption("block_number",      "Block number",              Icons.Default.Block),
            ContextMenuItemOption("fake_call",         "Fake Call",                 Icons.Outlined.PhoneCallback),
            ContextMenuItemOption("delete_call_log",   "Delete from call log",      Icons.Default.Delete)
        ),
        "contacts" to listOf(
            ContextMenuItemOption("select",           "Select",                    Icons.Default.CheckBox),
            ContextMenuItemOption("view_contact",      "View contact",              Icons.Default.Person),
            ContextMenuItemOption("edit_contact",      "Edit contact",              Icons.Default.Edit),
            ContextMenuItemOption("copy_number",       "Copy number",               Icons.Default.ContentCopy),
            ContextMenuItemOption("share_contact",     "Share contact",             Icons.Default.Share),
            ContextMenuItemOption("move_contact",      "Move contact",              Icons.Default.DriveFileMove),
            ContextMenuItemOption("toggle_favorite",   "Add/Remove Favourites",     Icons.Default.Favorite),
            ContextMenuItemOption("fake_call",         "Fake Call",                 Icons.Outlined.PhoneCallback),
            ContextMenuItemOption("delete_contact",    "Delete contact",            Icons.Default.Delete)
        )
    )

    fun contextMenuShowKey(section: String, itemKey: String) = "context_menu_${section}_show_$itemKey"
    fun contextMenuOrderKey(section: String) = "context_menu_${section}_order"

    // Per-section ordered key list, persisted as a comma-separated string. Any item keys
    // missing from a previously-saved (older) order are appended so new entries always show.
    val contextMenuOrders = remember {
        mutableStateMapOf<String, MutableList<String>>().apply {
            contextMenuSections.forEach { section ->
                val validKeys = contextMenuItemsBySection[section.key].orEmpty().map { it.key }
                val saved = prefs.getString(contextMenuOrderKey(section.key), null)
                val savedKeys = saved?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                val list = mutableStateListOf<String>()
                list.addAll(savedKeys.filter { it in validKeys })
                validKeys.forEach { key -> if (key !in list) list.add(key) }
                this[section.key] = list
            }
        }
    }
    fun persistContextMenuOrder(section: String) {
        val order = contextMenuOrders[section] ?: return
        prefs.setString(contextMenuOrderKey(section), order.joinToString(","))
    }

    var hexInput by remember { mutableStateOf(String.format("%06X", 0xFFFFFF and customPrimaryColor)) }
    var hexError by remember { mutableStateOf(false) }

    // Font state
    val savedFontPath = prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
    var hasFontSet    by remember { mutableStateOf(savedFontPath != null) }
    var fontSizeScale by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)) }

    val fontPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fontFile = File(context.filesDir, "custom_font.ttf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        fontFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, fontFile.absolutePath)
                    hasFontSet = true
                    (context as? Activity)?.let { activity ->
                        val intent = activity.intent
                        activity.finish()
                        activity.startActivity(intent)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60), Color(0xFF436916),
        Color(0xFF984061), Color(0xFF006874), Color(0xFF705D00), Color(0xFFBF0031),
        Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B)
    )

    fun applyHexColor(hex: String) {
        val cleaned = hex.trimStart('#').uppercase()
        if (cleaned.length == 6) {
            try {
                val colorInt = android.graphics.Color.parseColor("#$cleaned")
                customPrimaryColor = colorInt
                prefs.setInt("custom_primary_color", colorInt)
                hexError = false
                triggerRestartPrompt(scope, snackbarHostState, context)
            } catch (_: Exception) { hexError = true }
        } else {
            hexError = true
        }
    }

    // ── Call UI Dialog ────────────────────────────────────────────────────────
    if (showCallUIDialog) {
        AlertDialog(
            onDismissRequest = { showCallUIDialog = false },
            icon = { Icon(Icons.Default.Dashboard, null, tint = ColorBlue) },
            title = { Text("Call UI Elements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Toggle which stat cards appear in the Calls home screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        Triple("Today", callUIShowToday) { v: Boolean ->
                            callUIShowToday = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY, v)
                        },
                        Triple("Missed", callUIShowMissed) { v: Boolean ->
                            callUIShowMissed = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED, v)
                        },
                        Triple("Outgoing", callUIShowOutgoing) { v: Boolean ->
                            callUIShowOutgoing = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, v)
                        },
                        Triple("Call Time", callUIShowCallTime) { v: Boolean ->
                            callUIShowCallTime = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, v)
                        }
                    ).forEach { (label, checked, onChange) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = onChange,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCallUIDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Default Tab Dialog ─────────────────────────────────────────────────
    if (showDefaultTabDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultTabDialog = false },
            icon = { Icon(Icons.Default.Tab, null, tint = ColorIndigo) },
            title = { Text("Default Tab Section") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose which tab opens when the app starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    tabOptions.forEach { option ->
                        val isSelected = defaultTab == option.key
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
                                        defaultTab = option.key
                                        prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, option.key)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        defaultTab = option.key
                                        prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, option.key)
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
                TextButton(onClick = { showDefaultTabDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Tab Sections Dialog ──────────────────────────────────────────────────
    if (showTabSectionsDialog) {
        val density = LocalDensity.current
        val rowHeightDp = 52.dp
        val rowHeightPx = with(density) { rowHeightDp.toPx() }
        var draggedIndex by remember { mutableStateOf(-1) }
        var dragOffsetY by remember { mutableStateOf(0f) }

        fun tabChecked(key: String): Boolean = when (key) {
            "favorites"  -> tabShowFavorites
            "calls"      -> tabShowCalls
            "contacts"   -> tabShowContacts
            "recordings" -> tabShowRecordings
            "notes"      -> tabShowNotes
            else         -> true
        }
        fun setTabChecked(key: String, value: Boolean) {
            when (key) {
                "favorites"  -> { tabShowFavorites = value;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  value) }
                "calls"      -> { tabShowCalls = value;      prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      value) }
                "contacts"   -> { tabShowContacts = value;   prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   value) }
                "recordings" -> { tabShowRecordings = value; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, value) }
                "notes"      -> { tabShowNotes = value;      prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      value) }
            }
        }

        AlertDialog(
            onDismissRequest = { showTabSectionsDialog = false },
            icon = { Icon(Icons.Default.ViewWeek, null, tint = ColorIndigo) },
            title = { Text("Tab Sections") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose which tabs are visible, and drag the handle to reorder them in the navigation bar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column {
                        tabOrder.forEachIndexed { index, tabKey ->
                            val option = tabOptions.firstOrNull { it.key == tabKey } ?: return@forEachIndexed
                            val isDragging = draggedIndex == index
                            key(tabKey) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (isDragging) 4.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .zIndex(if (isDragging) 1f else 0f)
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
                                        checked = tabChecked(tabKey),
                                        onCheckedChange = { setTabChecked(tabKey, it) },
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
                                            .pointerInput(tabKey) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedIndex = index
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedIndex = -1
                                                        dragOffsetY = 0f
                                                        persistTabOrder()
                                                    },
                                                    onDragCancel = {
                                                        draggedIndex = -1
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val moveBy = (dragOffsetY / rowHeightPx).roundToInt()
                                                        if (moveBy != 0 && draggedIndex >= 0) {
                                                            val newIndex = (draggedIndex + moveBy).coerceIn(0, tabOrder.lastIndex)
                                                            if (newIndex != draggedIndex) {
                                                                val moving = tabOrder.removeAt(draggedIndex)
                                                                tabOrder.add(newIndex, moving)
                                                                dragOffsetY -= moveBy * rowHeightPx
                                                                draggedIndex = newIndex
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
                TextButton(onClick = { showTabSectionsDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Context Menu Elements: top-level section list (no checkbox/drag here) ─
    if (showContextMenuDialog) {
        AlertDialog(
            onDismissRequest = { showContextMenuDialog = false },
            icon = { Icon(Icons.Default.ViewWeek, null, tint = ColorOrange) },
            title = { Text("Context Menu Elements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose a section to view and customize its long-press context menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Column {
                        contextMenuSections.forEach { section ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        showContextMenuDialog = false
                                        activeContextMenuSection = section.key
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(section.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenuDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Context Menu Elements: per-section entries (checkbox + drag to reorder) ─
    activeContextMenuSection?.let { sectionKey ->
        val section = contextMenuSections.firstOrNull { it.key == sectionKey }
        val sectionItems = contextMenuItemsBySection[sectionKey].orEmpty()
        val sectionOrder = contextMenuOrders[sectionKey]

        if (section != null && sectionOrder != null) {
            val density = LocalDensity.current
            val rowHeightDp = 52.dp
            val rowHeightPx = with(density) { rowHeightDp.toPx() }
            var draggedIndex by remember(sectionKey) { mutableStateOf(-1) }
            var dragOffsetY by remember(sectionKey) { mutableStateOf(0f) }

            fun itemChecked(itemKey: String) = prefs.getBoolean(contextMenuShowKey(sectionKey, itemKey), true)
            fun setItemChecked(itemKey: String, value: Boolean) {
                prefs.setBoolean(contextMenuShowKey(sectionKey, itemKey), value)
            }

            AlertDialog(
                onDismissRequest = { activeContextMenuSection = null },
                icon = { Icon(section.icon, null, tint = ColorOrange) },
                title = { Text("${section.label} Context Menu") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Choose which entries are visible, and drag the handle to reorder them in the context menu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Column {
                            sectionOrder.forEachIndexed { index, itemKey ->
                                val option = sectionItems.firstOrNull { it.key == itemKey } ?: return@forEachIndexed
                                val isDragging = draggedIndex == index
                                key(itemKey) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = if (isDragging) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .zIndex(if (isDragging) 1f else 0f)
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
                                            checked = itemChecked(itemKey),
                                            onCheckedChange = { setItemChecked(itemKey, it) },
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
                                                            draggedIndex = index
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggedIndex = -1
                                                            dragOffsetY = 0f
                                                            persistContextMenuOrder(sectionKey)
                                                        },
                                                        onDragCancel = {
                                                            draggedIndex = -1
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val moveBy = (dragOffsetY / rowHeightPx).roundToInt()
                                                            if (moveBy != 0 && draggedIndex >= 0) {
                                                                val newIndex = (draggedIndex + moveBy).coerceIn(0, sectionOrder.lastIndex)
                                                                if (newIndex != draggedIndex) {
                                                                    val moving = sectionOrder.removeAt(draggedIndex)
                                                                    sectionOrder.add(newIndex, moving)
                                                                    dragOffsetY -= moveBy * rowHeightPx
                                                                    draggedIndex = newIndex
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
                    TextButton(onClick = { activeContextMenuSection = null }) { Text("Done") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        activeContextMenuSection = null
                        showContextMenuDialog = true
                    }) { Text("Back") }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Interface", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── App Theme ────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        Column {
                            Text("App Theme", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Color Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(12.dp))
                                    themeOptions.chunked(3).forEach { rowItems ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            rowItems.forEach { option ->
                                                val selected = themeMode == option.key
                                                Surface(
                                                    onClick = {
                                                        themeMode = option.key
                                                        prefs.setString(PreferenceManager.KEY_THEME_MODE, option.key)
                                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                                    },
                                                    shape = RoundedCornerShape(50),
                                                    color = if (selected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.weight(1f).height(38.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(option.label, style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Theme Colors ──────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 60L) {
                        Column {
                            Text("Theme Colors", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Dynamic Colors",
                                    supporting = "Wallpaper based app color theming",
                                    leadingIcon = Icons.Outlined.Palette,
                                    iconContainerColor = ColorPurple,
                                    checked = dynamicColors,
                                    onCheckedChange = {
                                        dynamicColors = it
                                        prefs.setBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, it)
                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                    }
                                )
                                if (!dynamicColors) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Primary Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(12.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(presetColors) { color ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .border(
                                                            width = if (customPrimaryColor == color.toArgb()) 3.dp else 0.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            customPrimaryColor = color.toArgb()
                                                            prefs.setInt("custom_primary_color", color.toArgb())
                                                            hexInput = String.format("%06X", 0xFFFFFF and color.toArgb())
                                                            hexError = false
                                                            triggerRestartPrompt(scope, snackbarHostState, context)
                                                        }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                                    try { Color(android.graphics.Color.parseColor("#${hexInput.trimStart('#')}")) }
                                                    catch (_: Exception) { Color.Gray }
                                                )
                                            )
                                            OutlinedTextField(
                                                value = hexInput,
                                                onValueChange = { v ->
                                                    hexInput = v.trimStart('#').uppercase().take(6)
                                                    hexError = false
                                                },
                                                label = { Text("Hex Color") },
                                                prefix = { Text("#") },
                                                isError = hexError,
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = {
                                                    applyHexColor(hexInput)
                                                    keyboardController?.hide()
                                                }),
                                                shape = RoundedCornerShape(12.dp),
                                                supportingText = if (hexError) {{ Text("Enter a valid 6-digit hex code") }} else null
                                            )
                                            Button(onClick = {
                                                applyHexColor(hexInput)
                                                keyboardController?.hide()
                                            }, shape = RoundedCornerShape(12.dp)) {
                                                Text("Apply")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Custom Font ─────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 70L) {
                        Column {
                            Text("Custom Font", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                Column(modifier = Modifier
                                    .clickable { fontPickerLauncher.launch("font/ttf") }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Surface(shape = RoundedCornerShape(12.dp), color = ColorPurple.copy(alpha = 0.18f), modifier = Modifier.size(40.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.TextFormat, null, tint = ColorPurple, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Custom Font", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text(
                                                if (hasFontSet) "Custom font active · tap to change" else "Pick a .ttf file to use across the app",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        if (hasFontSet) {
                                            IconButton(onClick = {
                                                prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
                                                prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)
                                                fontSizeScale = 1.0f
                                                hasFontSet = false
                                                val file = File(context.filesDir, "custom_font.ttf")
                                                file.delete()
                                                (context as? Activity)?.let { a ->
                                                    val intent = a.intent
                                                    a.finish()
                                                    a.startActivity(intent)
                                                }
                                            }) { Icon(Icons.Default.Refresh, "Revert font", tint = MaterialTheme.colorScheme.error) }
                                        }
                                        IconButton(onClick = { fontPickerLauncher.launch("font/ttf") }) {
                                            Icon(Icons.Default.FolderOpen, "Pick font", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (hasFontSet) {
                                        Spacer(Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Size", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                                            Slider(
                                                value = fontSizeScale,
                                                onValueChange = { fontSizeScale = it },
                                                onValueChangeFinished = { prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, fontSizeScale) },
                                                valueRange = 0.8f..1.4f,
                                                steps = 11,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text("${(fontSizeScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(42.dp).padding(start = 8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Liquid Glass ─────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 80L) {
                        Column {
                            Text("Visual Effects", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                RivoExpressiveCard {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Lens,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                "Not supported on this device",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Blur and Liquid Glass require Android 12 or higher",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            } else {
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Material Liquid You Glass",
                                    supporting = "Apply a liquid glass refraction effect to navigation and menus",
                                    leadingIcon = Icons.Outlined.Lens,
                                    iconContainerColor = Color(0xFF00BCD4),
                                    checked = liquidGlass,
                                    onCheckedChange = {
                                        liquidGlass = it
                                        prefs.setBoolean(PreferenceManager.KEY_LIQUID_GLASS, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Elements to have liquid glass effect",
                                    supporting = "Choose which UI elements use the liquid glass effect",
                                    leadingIcon = Icons.Outlined.Layers,
                                    iconContainerColor = Color(0xFF0097A7),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {
                                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.LiquidGlassElementsScreenDestination)
                                    }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Material Blur Effects",
                                    supporting = "Apply a background blur effect to navigation and menus",
                                    leadingIcon = Icons.Outlined.BlurOn,
                                    iconContainerColor = Color(0xFF5C6BC0),
                                    checked = blurEffects,
                                    onCheckedChange = {
                                        blurEffects = it
                                        prefs.setBoolean(PreferenceManager.KEY_BLUR_EFFECTS, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Elements to have blur effect",
                                    supporting = "Choose which UI elements use the blur effect",
                                    leadingIcon = Icons.Outlined.Layers,
                                    iconContainerColor = Color(0xFF3949AB),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {
                                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.BlurEffectsElementsScreenDestination)
                                    }
                                )
                            }
                            }
                        }
                    }
                }

                // ── Call UI ───────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 100L) {
                        Column {
                            Text("Call UI", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Incoming Call UI",
                                    supporting = "Customize the incoming call screen appearance",
                                    leadingIcon = Icons.Outlined.CallReceived,
                                    iconContainerColor = ColorGreen,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.IncomingCallUIScreenDestination) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // ── Caller UI → separate page ─────────────────────────
                                RivoListItem(
                                    headline = "Caller UI",
                                    supporting = "Customize the in-call screen layout and controls",
                                    leadingIcon = Icons.Outlined.Person,
                                    iconContainerColor = ColorBlue,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.CallerUIScreenDestination) }
                                )

                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Calls Section Elements",
                                    supporting = "Toggle Today, Missed, Outgoing, Call Time cards",
                                    leadingIcon = Icons.Default.Dashboard,
                                    iconContainerColor = ColorOrange,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showCallUIDialog = true }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Context Menu Elements",
                                    supporting = "Customize Favourites, Call Logs, and Contacts context menus",
                                    leadingIcon = Icons.Default.MoreVert,
                                    iconContainerColor = ColorPurple,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showContextMenuDialog = true }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Tab Sections",
                                    supporting = "Toggle and drag to reorder tabs in the navigation bar",
                                    leadingIcon = Icons.Default.ViewWeek,
                                    iconContainerColor = ColorIndigo,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showTabSectionsDialog = true }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Default Tab Section",
                                    supporting = "Choose which tab opens when the app starts (currently: ${tabOptions.firstOrNull { it.key == defaultTab }?.label ?: "Calls"})",
                                    leadingIcon = Icons.Default.Tab,
                                    iconContainerColor = ColorIndigo,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showDefaultTabDialog = true }
                                )
                            }
                        }
                    }
                }

                // ── Animations ───────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 115L) {
                        Column {
                            Text("Animations", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Scroll Animation",
                                    supporting = "Fade-in animation for list items as you scroll",
                                    leadingIcon = Icons.Outlined.Animation,
                                    iconContainerColor = ColorBlue,
                                    checked = scrollAnimation,
                                    onCheckedChange = {
                                        scrollAnimation = it
                                        prefs.setBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── UI Element Visibility ────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 130L) {
                        Column {
                            Text("UI Elements", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Pill Style Navigation",
                                    supporting = "Show a floating pill-style nav bar instead of the standard bottom bar",
                                    leadingIcon = Icons.Outlined.ViewStream,
                                    iconContainerColor = ColorTeal,
                                    checked = pillNav,
                                    onCheckedChange = {
                                        pillNav = it
                                        prefs.setBoolean(PreferenceManager.KEY_PILL_NAV, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show Sims In Call Logs",
                                    supporting = "Show a SIM icon with its number on calls in Call Logs",
                                    leadingIcon = Icons.Outlined.SimCard,
                                    iconContainerColor = ColorGreen,
                                    checked = showSimsInCallLogs,
                                    onCheckedChange = {
                                        showSimsInCallLogs = it
                                        prefs.setBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Avatars ──────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 160L) {
                        Column {
                            Text("Avatars", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Show First Letter in Avatar",
                                    supporting = "Displays letter when picture is missing",
                                    leadingIcon = Icons.Outlined.TextFields,
                                    iconContainerColor = ColorAmber,
                                    checked = showFirstLetter,
                                    onCheckedChange = { showFirstLetter = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Use Colorful Avatars",
                                    supporting = "Random colors based on contact name",
                                    leadingIcon = Icons.Outlined.ColorLens,
                                    iconContainerColor = ColorBlue,
                                    checked = colorfulAvatars,
                                    onCheckedChange = { colorfulAvatars = it; prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show Picture in Avatar",
                                    supporting = "Shows the contact picture if available",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = ColorGreen,
                                    checked = showPicture,
                                    onCheckedChange = { showPicture = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_PICTURE, it) }
                                )
                            }
                        }
                    }
                }

                // ── Navigation ───────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 220L) {
                        Column {
                            Text("Navigation", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Icon-Only Bottom Bar",
                                    supporting = "Removes text labels from navigation",
                                    leadingIcon = Icons.Outlined.ViewStream,
                                    iconContainerColor = ColorTeal,
                                    checked = iconOnlyNav,
                                    onCheckedChange = { iconOnlyNav = it; prefs.setBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Open Dialpad by Default",
                                    supporting = "Show dialpad automatically when app starts",
                                    leadingIcon = Icons.Outlined.Dialpad,
                                    iconContainerColor = ColorAmber,
                                    checked = openDialpadDefault,
                                    onCheckedChange = {
                                        openDialpadDefault = it
                                        prefs.setBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── App Icon ─────────────────────────────────────────
                item {
                    Column {
                        Text(
                            "App Icon",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "App Icon",
                                supporting = "Choose the app icon displayed on your home screen",
                                leadingIcon = Icons.Outlined.Apps,
                                iconContainerColor = ColorIndigo,
                                onClick = {
                                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.AppIconScreenDestination)
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }


        }
    }
}
