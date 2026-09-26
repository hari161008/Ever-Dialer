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
import androidx.compose.material.icons.outlined.AccessTime
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
import com.coolappstore.everdialer.by.svhp.modal.data.Contact

enum class ContactSortOption(val id: String, val label: String, val icon: ImageVector) {
    NAME("name", "Name", Icons.Outlined.SortByAlpha),
    DATE_ADDED("date_added", "Date added", Icons.Outlined.AccessTime),
    FREQUENTLY("frequently", "Frequently", Icons.Outlined.TrendingUp);

    companion object {
        fun fromId(id: String?): ContactSortOption =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: NAME
    }
}

fun List<Contact>.sortContacts(option: ContactSortOption, ascending: Boolean): List<Contact> {
    return when (option) {
        ContactSortOption.NAME -> {
            if (ascending) {
                sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.ifBlank { "zzzz" } })
            } else {
                sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.ifBlank { "" } })
            }
        }
        ContactSortOption.DATE_ADDED -> {
            if (ascending) {
                // Oldest first
                sortedWith(compareBy<Contact> {
                    if (it.dateAdded > 0L) it.dateAdded else (it.id.toLongOrNull() ?: 0L)
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            } else {
                // Newest first
                sortedWith(compareByDescending<Contact> {
                    if (it.dateAdded > 0L) it.dateAdded else (it.id.toLongOrNull() ?: 0L)
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
        }
        ContactSortOption.FREQUENTLY -> {
            if (ascending) {
                // Least frequently contacted first
                sortedWith(compareBy<Contact> { it.timesContacted }
                    .thenBy { it.lastTimeContacted }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            } else {
                // Most frequently contacted first
                sortedWith(compareByDescending<Contact> { it.timesContacted }
                    .thenByDescending { it.lastTimeContacted }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
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
                                ContactSortOption.DATE_ADDED -> false
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
