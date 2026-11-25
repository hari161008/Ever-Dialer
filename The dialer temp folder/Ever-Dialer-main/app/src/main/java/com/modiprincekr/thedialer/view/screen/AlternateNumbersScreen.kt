package com.modiprincekr.thedialer.view.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modiprincekr.thedialer.controller.util.AlternateNumberRepository
import com.modiprincekr.thedialer.controller.util.makeCall
import com.modiprincekr.thedialer.controller.util.openWhatsAppChat
import com.modiprincekr.thedialer.modal.data.AlternateNumber
import com.modiprincekr.thedialer.view.components.ExpressiveAvatar
import com.modiprincekr.thedialer.view.components.RivoAvatar
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun AlternateNumbersScreen(
    navigator: DestinationsNavigator,
    prefillNumber: String? = null
) {
    val context = LocalContext.current
    val allNumbers by AlternateNumberRepository.alternateNumbersFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(AlternateNumber.TAG_ALL) }

    var editingItem by remember { mutableStateOf<AlternateNumber?>(null) }
    var showAddEditDialog by remember { mutableStateOf(!prefillNumber.isNullOrBlank()) }
    var itemToDelete by remember { mutableStateOf<AlternateNumber?>(null) }

    val filteredList = remember(allNumbers, searchQuery, selectedTag) {
        val q = searchQuery.trim().lowercase()
        val digits = q.filter { it.isDigit() }
        allNumbers.filter { item ->
            val tagMatch = selectedTag == AlternateNumber.TAG_ALL || item.categoryTag.equals(selectedTag, ignoreCase = true)
            if (!tagMatch) return@filter false

            if (q.isEmpty()) return@filter true

            val nameMatch = item.name.lowercase().contains(q)
            val noteMatch = item.note.lowercase().contains(q)
            val numberMatch = item.phoneNumber.contains(q) || (digits.isNotEmpty() && item.normalizedNumber.contains(digits))
            val catMatch = item.categoryTag.lowercase().contains(q)

            nameMatch || noteMatch || numberMatch || catMatch
        }
    }

    val tags = remember {
        listOf(AlternateNumber.TAG_ALL) + AlternateNumber.PRESET_TAGS
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alternate Numbers",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingItem = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Number") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 80.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search alternate numbers...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val isSelected = selectedTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTag = tag },
                        label = { Text(tag) },
                        leadingIcon = if (tag != AlternateNumber.TAG_ALL) {
                            {
                                Icon(
                                    imageVector = getTagIcon(tag),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Numbers List or Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.ContactPhone,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTag != AlternateNumber.TAG_ALL)
                                "No matching alternate numbers"
                            else "No alternate numbers saved",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTag != AlternateNumber.TAG_ALL)
                                "Try a different search query or filter"
                            else "Save numbers in-app without syncing to device contacts or polluting WhatsApp profile visibility.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp + navBarBottom
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        AlternateNumberCard(
                            item = item,
                            onCallClick = { makeCall(context, item.phoneNumber) },
                            onSmsClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${item.phoneNumber}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onWhatsAppClick = {
                                openWhatsAppChat(context, item.phoneNumber)
                            },
                            onEditClick = {
                                editingItem = item
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                itemToDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditAlternateNumberDialog(
            initialItem = editingItem,
            initialNumber = if (editingItem == null) prefillNumber else null,
            onDismiss = {
                showAddEditDialog = false
                editingItem = null
            },
            onSave = { name, number, tag, note ->
                val record = AlternateNumber(
                    id = editingItem?.id ?: java.util.UUID.randomUUID().toString(),
                    name = name.trim(),
                    phoneNumber = number.trim(),
                    categoryTag = tag,
                    note = note.trim(),
                    createdAt = editingItem?.createdAt ?: System.currentTimeMillis()
                )
                AlternateNumberRepository.save(context, record)
                showAddEditDialog = false
                editingItem = null
                Toast.makeText(context, "Alternate number saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Alternate Number?") },
            text = { Text("Remove \"${target.name}\" (${target.phoneNumber}) from your Alternate Numbers? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        AlternateNumberRepository.delete(context, target.id)
                        itemToDelete = null
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlternateNumberCard(
    item: AlternateNumber,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveAvatar(
                    name = item.name,
                    size = 46.dp
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Category Tag Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = getTagIcon(item.categoryTag),
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = item.categoryTag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.note.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Action Buttons
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Call button
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // SMS button
                    IconButton(
                        onClick = onSmsClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "SMS",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Direct WhatsApp button
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Edit button
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlternateNumberDialog(
    initialItem: AlternateNumber? = null,
    initialNumber: String? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, tag: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(initialItem?.phoneNumber ?: (initialNumber ?: "")) }
    var selectedTag by remember { mutableStateOf(initialItem?.categoryTag ?: AlternateNumber.TAG_OTHER) }
    var note by remember { mutableStateOf(initialItem?.note ?: "") }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (initialItem != null) Icons.Outlined.Edit else Icons.Outlined.ContactPhone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(if (initialItem != null) "Edit Alternate Number" else "Add Alternate Number")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (isError && it.isNotBlank()) isError = false
                    },
                    label = { Text("Name *") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    isError = isError && name.isBlank(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        if (isError && it.isNotBlank()) isError = false
                    },
                    label = { Text("Phone Number *") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    singleLine = true,
                    isError = isError && phoneNumber.isBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category Tag",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlternateNumber.PRESET_TAGS.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getTagIcon(tag),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phoneNumber.isBlank()) {
                        isError = true
                    } else {
                        onSave(name, phoneNumber, selectedTag, note)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getTagIcon(tag: String): ImageVector {
    return when (tag.lowercase()) {
        "shop" -> Icons.Outlined.Storefront
        "delivery" -> Icons.Outlined.LocalShipping
        "studio" -> Icons.Outlined.Palette
        "work" -> Icons.Outlined.Business
        else -> Icons.Outlined.Bookmark
    }
}
