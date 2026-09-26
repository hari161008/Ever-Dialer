package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.CallLogsCache
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.Contact

enum class ContactSortOption(val id: String, val label: String, val icon: ImageVector) {
    NAME("name", "Name", Icons.Outlined.SortByAlpha),
    RECENTLY_CALLED("recently_called", "Recently called", Icons.Outlined.History),
    FREQUENTLY("frequently", "Frequently", Icons.Outlined.TrendingUp);

    companion object {
        fun fromId(id: String?): ContactSortOption = when (id?.lowercase()) {
            "recently_called", "date_added" -> RECENTLY_CALLED
            "frequently" -> FREQUENTLY
            else -> NAME
        }
    }
}

data class ContactCallStats(
    var callCount: Int = 0,
    var lastCallTime: Long = 0L
)

fun buildContactCallStatsMap(
    contacts: List<Contact>,
    callLogs: List<CallLogEntry>
): Map<String, ContactCallStats> {
    if (callLogs.isEmpty()) {
        return emptyMap()
    }

    val contactByNumber = HashMap<String, String>()
    val contactBySuffix = HashMap<String, String>()
    for (contact in contacts) {
        for (rawPhone in contact.phoneNumbers) {
            val digits = rawPhone.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                contactByNumber.putIfAbsent(digits, contact.id)
                if (digits.length >= 7) {
                    contactBySuffix.putIfAbsent(digits.takeLast(7), contact.id)
                }
            }
        }
    }

    val statsMap = HashMap<String, ContactCallStats>()

    for (log in callLogs) {
        val targetContactId = if (!log.contactId.isNullOrBlank()) {
            log.contactId
        } else {
            val logDigits = log.number.filter { it.isDigit() }
            if (logDigits.isEmpty()) null
            else contactByNumber[logDigits] ?: if (logDigits.length >= 7) contactBySuffix[logDigits.takeLast(7)] else null
        }

        if (targetContactId != null) {
            val stats = statsMap.getOrPut(targetContactId) { ContactCallStats() }
            stats.callCount += log.count
            val latestDate = if (log.dates.isNotEmpty()) log.dates.max() else log.date
            if (latestDate > stats.lastCallTime) {
                stats.lastCallTime = latestDate
            }
        }
    }

    return statsMap
}

fun List<Contact>.sortContacts(
    option: ContactSortOption,
    ascending: Boolean,
    callLogs: List<CallLogEntry> = emptyList()
): List<Contact> {
    val actualLogs = if (callLogs.isNotEmpty()) callLogs else (CallLogsCache.getInMemoryCache() ?: emptyList())
    return when (option) {
        ContactSortOption.NAME -> {
            if (ascending) {
                sortedWith(
                    compareBy<Contact> {
                        val first = it.name.trimStart().firstOrNull()?.uppercaseChar()
                        when {
                            first == null -> 2
                            first in 'A'..'Z' -> 0
                            first.isLetter() -> 1
                            else -> 2
                        }
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.trimStart().ifBlank { "zzzz" } }
                )
            } else {
                sortedWith(
                    compareBy<Contact> {
                        val first = it.name.trimStart().firstOrNull()?.uppercaseChar()
                        when {
                            first == null -> 2
                            first in 'A'..'Z' -> 0
                            first.isLetter() -> 1
                            else -> 2
                        }
                    }.thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.trimStart().ifBlank { "" } }
                )
            }
        }
        ContactSortOption.RECENTLY_CALLED -> {
            val statsMap = buildContactCallStatsMap(this, actualLogs)
            if (ascending) {
                sortedWith(
                    compareBy<Contact> {
                        val time = statsMap[it.id]?.lastCallTime ?: if (it.lastTimeContacted > 0L) it.lastTimeContacted else 0L
                        if (time > 0L) time else Long.MAX_VALUE
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.trimStart() }
                )
            } else {
                sortedWith(
                    compareByDescending<Contact> {
                        statsMap[it.id]?.lastCallTime ?: if (it.lastTimeContacted > 0L) it.lastTimeContacted else 0L
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.trimStart() }
                )
            }
        }
        ContactSortOption.FREQUENTLY -> {
            val statsMap = buildContactCallStatsMap(this, actualLogs)
            if (ascending) {
                sortedWith(
                    compareBy<Contact> {
                        statsMap[it.id]?.callCount ?: if (it.timesContacted > 0) it.timesContacted else 0
                    }.thenBy {
                        statsMap[it.id]?.lastCallTime ?: if (it.lastTimeContacted > 0L) it.lastTimeContacted else 0L
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.trimStart() }
                )
            } else {
                sortedWith(
                    compareByDescending<Contact> {
                        statsMap[it.id]?.callCount ?: if (it.timesContacted > 0) it.timesContacted else 0
                    }.thenByDescending {
                        statsMap[it.id]?.lastCallTime ?: if (it.lastTimeContacted > 0L) it.lastTimeContacted else 0L
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name.trimStart() }
                )
            }
        }
    }
}

@Composable
fun ContactSortButton(
    sortBy: String,
    ascending: Boolean,
    onSortChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOption = ContactSortOption.fromId(sortBy)

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort contacts",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentOption.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Direction indicator arrow with instant toggle click
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 12.dp)
                        ) {
                            onSortChanged(sortBy, !ascending)
                        }
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (ascending) "Ascending" else "Descending",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 230.dp)
        ) {
            Text(
                text = "Sort contacts by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ContactSortOption.entries.forEach { option ->
                val isSelected = option == currentOption
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Up arrow
                                val isUpActive = isSelected && ascending
                                Surface(
                                    onClick = {
                                        onSortChanged(option.id, true)
                                        expanded = false
                                    },
                                    shape = CircleShape,
                                    color = if (isUpActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Ascending",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isUpActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Down arrow
                                val isDownActive = isSelected && !ascending
                                Surface(
                                    onClick = {
                                        onSortChanged(option.id, false)
                                        expanded = false
                                    },
                                    shape = CircleShape,
                                    color = if (isDownActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Descending",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isDownActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        if (isSelected) {
                            onSortChanged(option.id, !ascending)
                        } else {
                            val defaultAsc = when (option) {
                                ContactSortOption.NAME -> true
                                ContactSortOption.RECENTLY_CALLED -> false
                                ContactSortOption.FREQUENTLY -> false
                            }
                            onSortChanged(option.id, defaultAsc)
                        }
                        expanded = false
                    }
                )
            }
        }
    }
}
