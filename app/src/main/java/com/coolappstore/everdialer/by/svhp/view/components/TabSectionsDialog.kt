package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.compose.koinInject

data class TabSectionItem(val key: String, val label: String, val icon: ImageVector)

@Composable
fun TabSectionsDialog(
    onDismissRequest: () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rowHeightDp = 52.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    var tabShowFavorites  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)) }
    var tabShowCalls      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)) }
    var tabShowContacts   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)) }
    var tabShowSms        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true)) }
    var tabShowGroups     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false)) }
    var tabShowRecordings by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)) }
    var tabShowNotes      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)) }
    var tabShowDialpad    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false)) }

    val tabOptions = remember {
        listOf(
            TabSectionItem("favorites",  "Favourites", Icons.Outlined.FavoriteBorder),
            TabSectionItem("calls",      "Calls",      Icons.Outlined.History),
            TabSectionItem("contacts",   "Contacts",   Icons.Outlined.Person),
            TabSectionItem("sms",        "SMS",        Icons.Outlined.Chat),
            TabSectionItem("groups",     "Groups",     Icons.Outlined.Group),
            TabSectionItem("recordings", "Recordings", Icons.Outlined.FiberManualRecord),
            TabSectionItem("notes",      "Note",       Icons.Outlined.Note),
            TabSectionItem("dialpad",    "Dialpad",    Icons.Outlined.Dialpad)
        )
    }

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

    fun tabChecked(key: String): Boolean = when (key) {
        "favorites"  -> tabShowFavorites
        "calls"      -> tabShowCalls
        "contacts"   -> tabShowContacts
        "sms"        -> tabShowSms
        "groups"     -> tabShowGroups
        "recordings" -> tabShowRecordings
        "notes"      -> tabShowNotes
        "dialpad"    -> tabShowDialpad
        else         -> true
    }

    fun setTabChecked(key: String, value: Boolean) {
        when (key) {
            "favorites"  -> { tabShowFavorites = value;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  value) }
            "calls"      -> { tabShowCalls = value;      prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      value) }
            "contacts"   -> { tabShowContacts = value;   prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   value) }
            "sms"        -> { tabShowSms = value;        prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        value) }
            "groups"     -> { tabShowGroups = value;     prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     value) }
            "recordings" -> { tabShowRecordings = value; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, value) }
            "notes"      -> { tabShowNotes = value;      prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      value) }
            "dialpad"    -> { tabShowDialpad = value;    prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    value) }
        }
    }

    fun resetTabSectionsToDefault() {
        val defaults = PreferenceManager.DEFAULT_TAB_ORDER.split(",").map { it.trim() }.filter { it.isNotBlank() }
        tabOrder.clear()
        tabOrder.addAll(defaults)
        persistTabOrder()
        tabShowFavorites  = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)
        tabShowCalls      = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)
        tabShowContacts   = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)
        tabShowSms        = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true)
        tabShowGroups     = false; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false)
        tabShowRecordings = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)
        tabShowNotes      = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)
        tabShowDialpad    = false; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.ViewWeek, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Tab Sections") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Choose which tabs are visible, and drag the handle to reorder them in the navigation bar.",
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
                    tabOrder.forEach { tabKey ->
                        val option = tabOptions.firstOrNull { it.key == tabKey } ?: return@forEach
                        val isDragging = draggedKey == tabKey
                        key(tabKey) {
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
                                                        draggedKey = tabKey
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedKey = null
                                                        dragOffsetY = 0f
                                                        persistTabOrder()
                                                    },
                                                    onDragCancel = {
                                                        draggedKey = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val currentIdx = tabOrder.indexOf(tabKey)
                                                        if (currentIdx != -1) {
                                                            val threshold = rowHeightPx * 0.6f
                                                            if (dragOffsetY > threshold && currentIdx < tabOrder.lastIndex) {
                                                                val item = tabOrder.removeAt(currentIdx)
                                                                tabOrder.add(currentIdx + 1, item)
                                                                dragOffsetY -= rowHeightPx
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                                val item = tabOrder.removeAt(currentIdx)
                                                                tabOrder.add(currentIdx - 1, item)
                                                                dragOffsetY -= rowHeightPx
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
            TextButton(onClick = onDismissRequest) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { resetTabSectionsToDefault() }) { Text("Default") }
        }
    )
}
