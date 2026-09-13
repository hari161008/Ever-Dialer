package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.compose.koinInject

val DEFAULT_SEARCH_FILTER_ORDER = listOf(
    "contacts",
    "non_contacts",
    "recordings",
    "groups",
    "contact_notes",
    "recording_notes",
    "settings"
)

/**
 * Snapshot of the persisted "Filter" checkboxes shown beside the search bar on the Dialpad,
 * Calls, Contacts, and Favourites screens. Every field defaults to true (ticked) except groups,
 * which defaults to false (unticked) per user specification.
 */
data class SearchFilterState(
    val contacts: Boolean = true,
    val nonContacts: Boolean = true,
    val recordings: Boolean = true,
    val groups: Boolean = false,
    val contactNotes: Boolean = true,
    val recordingNotes: Boolean = true,
    val settings: Boolean = true,
    val order: List<String> = DEFAULT_SEARCH_FILTER_ORDER
) {
    val isDefault: Boolean get() = contacts && nonContacts && recordings && !groups && contactNotes && recordingNotes && settings && order == DEFAULT_SEARCH_FILTER_ORDER
}

fun PreferenceManager.getSearchFilterState(): SearchFilterState {
    val savedOrder = getString(PreferenceManager.KEY_SEARCH_FILTER_ORDER, null)
    val parsedOrder = savedOrder?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    val finalOrder = (parsedOrder.filter { it in DEFAULT_SEARCH_FILTER_ORDER } + DEFAULT_SEARCH_FILTER_ORDER.filter { it !in parsedOrder }).distinct()

    return SearchFilterState(
        contacts = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_CONTACTS, true),
        nonContacts = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_NON_CONTACTS, true),
        recordings = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_RECORDINGS, true),
        groups = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_GROUPS, false),
        contactNotes = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_CONTACT_NOTES, true),
        recordingNotes = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_RECORDING_NOTES, true),
        settings = getBoolean(PreferenceManager.KEY_SEARCH_FILTER_SETTINGS, true),
        order = finalOrder
    )
}

/**
 * The round "Filter" button that sits to the right of a search bar. Tapping it opens a
 * checklist; every toggle is written straight to [PreferenceManager].
 * It includes a slider (drag handle) at the right to drag and rearrange the filter results.
 */
@Composable
fun SearchFilterButton(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val prefs = koinInject<PreferenceManager>()
    var expanded by remember { mutableStateOf(false) }
    val settingsVer by prefs.settingsChanged.collectAsState()
    val state = remember(settingsVer) { prefs.getSearchFilterState() }
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowHeightDp = 48.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }

    val buttonBg = when {
        isSaturatedActive && !state.isDefault -> MaterialTheme.colorScheme.primary
        isSaturatedActive -> MaterialTheme.colorScheme.primary
        !state.isDefault -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val buttonFg = when {
        isSaturatedActive -> MaterialTheme.colorScheme.onPrimary
        !state.isDefault -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = buttonBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter search results",
                    tint = buttonFg
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(min = 280.dp, max = 330.dp)
        ) {
            Text(
                "Filter results",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            var draggedKey by remember { mutableStateOf<String?>(null) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }

            val filterOrder = remember {
                mutableStateListOf<String>().apply { addAll(state.order) }
            }

            LaunchedEffect(state.order) {
                if (draggedKey == null && filterOrder != state.order) {
                    filterOrder.clear()
                    filterOrder.addAll(state.order)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                filterOrder.forEach { key ->
                    key(key) {
                        val isDragging = draggedKey == key
                        val label = when (key) {
                            "contacts" -> "Contacts"
                            "non_contacts" -> "Non contacts"
                            "recordings" -> "Recordings"
                            "groups" -> "Groups"
                            "contact_notes" -> "Contact notes"
                            "recording_notes" -> "Recording notes"
                            "settings" -> "Settings"
                            else -> key
                        }
                        val checked = when (key) {
                            "contacts" -> state.contacts
                            "non_contacts" -> state.nonContacts
                            "recordings" -> state.recordings
                            "groups" -> state.groups
                            "contact_notes" -> state.contactNotes
                            "recording_notes" -> state.recordingNotes
                            "settings" -> state.settings
                            else -> true
                        }
                        val onCheckedChange: (Boolean) -> Unit = { newVal ->
                            when (key) {
                                "contacts" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_CONTACTS, newVal)
                                "non_contacts" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_NON_CONTACTS, newVal)
                                "recordings" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_RECORDINGS, newVal)
                                "groups" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_GROUPS, newVal)
                                "contact_notes" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_CONTACT_NOTES, newVal)
                                "recording_notes" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_RECORDING_NOTES, newVal)
                                "settings" -> prefs.setBoolean(PreferenceManager.KEY_SEARCH_FILTER_SETTINGS, newVal)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeightDp)
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    shadowElevation = if (isDragging) 8f else 0f
                                },
                            shape = if (isDragging) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp),
                            color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                            tonalElevation = if (isDragging) 4.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onCheckedChange(!checked) }
                                        .padding(start = 6.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .pointerInput(key) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedKey = key
                                                    dragOffsetY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragEnd = {
                                                    draggedKey = null
                                                    dragOffsetY = 0f
                                                    prefs.setString(PreferenceManager.KEY_SEARCH_FILTER_ORDER, filterOrder.joinToString(","))
                                                },
                                                onDragCancel = {
                                                    draggedKey = null
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    val threshold = rowHeightPx * 0.5f
                                                    while (dragOffsetY > threshold && filterOrder.indexOf(key) < filterOrder.lastIndex) {
                                                        val currentIdx = filterOrder.indexOf(key)
                                                        val item = filterOrder.removeAt(currentIdx)
                                                        filterOrder.add(currentIdx + 1, item)
                                                        dragOffsetY -= rowHeightPx
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    while (dragOffsetY < -threshold && filterOrder.indexOf(key) > 0) {
                                                        val currentIdx = filterOrder.indexOf(key)
                                                        val item = filterOrder.removeAt(currentIdx)
                                                        filterOrder.add(currentIdx - 1, item)
                                                        dragOffsetY += rowHeightPx
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DragHandle,
                                        contentDescription = "Drag to reorder $label",
                                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
