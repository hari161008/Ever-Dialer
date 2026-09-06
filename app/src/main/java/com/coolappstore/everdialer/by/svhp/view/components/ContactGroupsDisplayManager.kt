package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsToDisplaySheet(
    accounts: List<ContactAccount>,
    groups: List<ContactGroup>,
    selectedAccountKey: String?,
    selectedGroupId: String?,
    totalCount: Int,
    onSelectAccount: (String?) -> Unit,
    onSelectGroup: (String) -> Unit,
    onDismiss: () -> Unit,
    contactsVM: ContactsViewModel
) {
    val prefs = koinInject<PreferenceManager>()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<ContactGroup?>(null) }

    val settingsVersion by prefs.settingsChanged.collectAsState()
    val hiddenGroupIds = remember(settingsVersion) { prefs.getHiddenContactGroupIds() }
    val enabledAccountKeys by contactsVM.enabledAccountKeys.collectAsState()

    val filteredGroups = remember(groups, enabledAccountKeys) {
        if (enabledAccountKeys == null) groups
        else {
            groups.filter { group ->
                val key = if (group.accountType != null || group.accountName != null) {
                    "${group.accountType ?: ""}:${group.accountName ?: ""}"
                } else null
                if (key != null) key in enabledAccountKeys!!
                else true
            }
        }
    }

    // Reorderable groups state
    var groupsList by remember(filteredGroups) { mutableStateOf(filteredGroups) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isGroupsExpanded by remember { mutableStateOf(false) }

    fun dismissSmoothly(action: (() -> Unit)? = null) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                action?.invoke()
                onDismiss()
            }
        }
    }

    if (showAddGroupDialog) {
        AddContactGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onSave = { newGroup ->
                contactsVM.addContactGroup(newGroup)
                showAddGroupDialog = false
            }
        )
    }

    if (groupToEdit != null) {
        EditContactGroupDialog(
            group = groupToEdit!!,
            onDismiss = { groupToEdit = null },
            onSave = { updatedGroup ->
                contactsVM.addContactGroup(updatedGroup)
                groupToEdit = null
            }
        )
    }

    if (showDeleteGroupDialog) {
        DeleteContactGroupDialog(
            groups = groupsList,
            onDismiss = { showDeleteGroupDialog = false },
            onDelete = { groupId ->
                contactsVM.deleteContactGroup(groupId)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(width = 36.dp, height = 4.dp)
                    ) {}
                }
            }
        }
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            val isDragging = draggedIndex != null
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState(), enabled = !isDragging)
                    .padding(bottom = 24.dp)
            ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Contacts & Groups",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { dismissSmoothly() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ════════════════════════════════════════════════════════════════
            // 1. MANAGE CONTACT GROUPS HEADING
            // ════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "MANAGE CONTACT GROUPS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "Drag slider handle to reorder groups",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Contact Groups list with drag-and-drop slider
            if (groupsList.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "No contact groups created yet. Tap Add to create one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val displayedGroupsList = if (groupsList.size > 4 && !isGroupsExpanded) groupsList.take(4) else groupsList

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    displayedGroupsList.forEachIndexed { index, group ->
                        val isSelected = selectedGroupId == group.id
                        val isBeingDragged = draggedIndex == index

                        val elevation by animateDpAsState(
                            if (isBeingDragged) 8.dp else 0.dp,
                            label = "groupElevation"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .zIndex(if (isBeingDragged) 10f else 1f)
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragOffsetY
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else if (isBeingDragged) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shadowElevation = elevation
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dismissSmoothly { onSelectGroup(group.id) }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RivoIconBox(
                                    icon = Icons.Default.People,
                                    iconContainerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF7C4DFF),
                                    size = 36.dp,
                                    iconSize = 18.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    val subtitle = buildString {
                                        append("${group.contactIds.size} contacts")
                                        if (!group.targetLabel.isNullOrBlank()) {
                                            append(" · ")
                                            append(group.targetLabel)
                                        } else if (!group.accountName.isNullOrBlank()) {
                                            append(" · ")
                                            append(group.accountName)
                                        }
                                    }
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                val isGroupHidden = group.id in hiddenGroupIds

                                // Hide / Show Group in Contact section Button
                                IconButton(
                                    onClick = {
                                        contactsVM.toggleContactGroupVisibility(group.id, !isGroupHidden)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (isGroupHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (isGroupHidden) "Show in contacts" else "Hide from contacts",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isGroupHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                               else MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Edit Group Button
                                IconButton(
                                    onClick = { groupToEdit = group },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Edit group",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Drag & Drop Slider Handle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures(
                                                onDragStart = {
                                                    draggedIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                    contactsVM.reorderContactGroups(groupsList)
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount
                                                    val currentIdx = draggedIndex ?: return@detectVerticalDragGestures
                                                    val threshold = 140f
                                                    if (dragOffsetY > threshold && currentIdx < groupsList.size - 1) {
                                                        val mutable = groupsList.toMutableList()
                                                        val item = mutable.removeAt(currentIdx)
                                                        mutable.add(currentIdx + 1, item)
                                                        groupsList = mutable
                                                        draggedIndex = currentIdx + 1
                                                        dragOffsetY -= threshold
                                                        contactsVM.reorderContactGroups(mutable)
                                                    } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                        val mutable = groupsList.toMutableList()
                                                        val item = mutable.removeAt(currentIdx)
                                                        mutable.add(currentIdx - 1, item)
                                                        groupsList = mutable
                                                        draggedIndex = currentIdx - 1
                                                        dragOffsetY += threshold
                                                        contactsVM.reorderContactGroups(mutable)
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    Icon(
                                        Icons.Outlined.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (groupsList.size > 4) {
                        TextButton(
                            onClick = { isGroupsExpanded = !isGroupsExpanded },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                if (isGroupsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isGroupsExpanded) "Show less" else "Show more (${groupsList.size - 4}+)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Action buttons for groups (Add & Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteGroupDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    enabled = groupsList.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }

                Button(
                    onClick = { showAddGroupDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Group")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ════════════════════════════════════════════════════════════════
            // 2. CONTACTS TO DISPLAY (ACCOUNTS / SOURCES)
            // ════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "CONTACTS TO DISPLAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "Select accounts to filter displayed contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val enabledAccountKeys by contactsVM.enabledAccountKeys.collectAsState()
            val allKeys = remember(accounts) { accounts.map { it.key }.toSet() }
            val isAllChecked = enabledAccountKeys == null || (accounts.isNotEmpty() && accounts.all { it.key in (enabledAccountKeys ?: allKeys) })

            // "All Contacts" row with Checkbox
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isAllChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isAllChecked) {
                                contactsVM.setEnabledAccountKeys(emptySet())
                            } else {
                                contactsVM.setEnabledAccountKeys(null)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RivoIconBox(
                        icon = Icons.Default.People,
                        iconContainerColor = Color(0xFF2196F3),
                        size = 40.dp,
                        iconSize = 22.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "All Contacts",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isAllChecked) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllChecked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "$totalCount contacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Checkbox(
                        checked = isAllChecked,
                        onCheckedChange = { checked ->
                            if (checked) {
                                contactsVM.setEnabledAccountKeys(null)
                            } else {
                                contactsVM.setEnabledAccountKeys(emptySet())
                            }
                        }
                    )
                }
            }

            // Individual Accounts / Sources with Checkboxes
            accounts.forEach { acc ->
                val isAccChecked = enabledAccountKeys == null || acc.key in (enabledAccountKeys ?: allKeys)
                val (accIcon, accColor) = when {
                    acc.accountType.contains("google", ignoreCase = true) ->
                        Icons.Default.Email to Color(0xFFE53935)
                    acc.key == "sim_1" ->
                        Icons.Default.SimCard to Color(0xFF4CAF50)
                    acc.key == "sim_2" ->
                        Icons.Default.SimCard to Color(0xFF009688)
                    acc.key.startsWith("sim_") && acc.key != "sim_0" ->
                        Icons.Default.SimCard to Color(0xFF00BCD4)
                    acc.key.equals("whatsapp", ignoreCase = true) || acc.accountType.contains("whatsapp", ignoreCase = true) ->
                        Icons.Default.ChatBubble to Color(0xFF25D366)
                    acc.accountType.contains("tachyon", ignoreCase = true) || acc.accountType.contains("meet", ignoreCase = true) ->
                        Icons.Default.VideoCall to Color(0xFF00897B)
                    acc.accountType.contains("exchange", ignoreCase = true) || acc.accountType.contains("outlook", ignoreCase = true) ->
                        Icons.Default.Business to Color(0xFF0078D4)
                    acc.accountType.contains("telegram", ignoreCase = true) ->
                        Icons.Default.Send to Color(0xFF29B6F6)
                    acc.accountType.contains("signal", ignoreCase = true) ->
                        Icons.Default.Security to Color(0xFF3A76F0)
                    acc.key == "sim_0" || acc.accountType.isBlank() ->
                        Icons.Default.PhoneAndroid to Color(0xFF607D8B)
                    else ->
                        Icons.Default.AccountCircle to Color(0xFF7C4DFF)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAccChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val current = (enabledAccountKeys ?: allKeys).toMutableSet()
                                if (acc.key in current) current.remove(acc.key) else current.add(acc.key)
                                if (accounts.isNotEmpty() && accounts.all { it.key in current }) {
                                    contactsVM.setEnabledAccountKeys(null)
                                } else {
                                    contactsVM.setEnabledAccountKeys(current)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RivoIconBox(
                            icon = accIcon,
                            iconContainerColor = accColor,
                            size = 40.dp,
                            iconSize = 22.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                acc.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isAccChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAccChecked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${acc.contactCount} contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Checkbox(
                            checked = isAccChecked,
                            onCheckedChange = { checked ->
                                val current = (enabledAccountKeys ?: allKeys).toMutableSet()
                                if (checked) current.add(acc.key) else current.remove(acc.key)
                                if (accounts.isNotEmpty() && accounts.all { it.key in current }) {
                                    contactsVM.setEnabledAccountKeys(null)
                                } else {
                                    contactsVM.setEnabledAccountKeys(current)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactGroupDialog(
    onDismiss: () -> Unit,
    onSave: (ContactGroup) -> Unit
) {
    val contactsVM: ContactsViewModel = org.koin.compose.viewmodel.koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val filteredContacts = remember(allContacts, searchQuery) {
        if (searchQuery.isBlank()) allContacts
        else allContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumbers.any { num -> num.contains(searchQuery) }
        }
    }

    if (showTargetPicker) {
        SelectGroupSaveTargetDialog(
            groupName = groupName.trim(),
            targets = contactsVM.getSaveTargets(),
            onSelect = { target ->
                val newGroup = ContactGroup(
                    name = groupName.trim(),
                    contactIds = selectedContactIds.toList(),
                    accountType = target.accountType,
                    accountName = target.accountName,
                    targetLabel = target.label + (if (target.subLabel != null) " (${target.subLabel})" else "")
                )
                showTargetPicker = false
                onSave(newGroup)
            },
            onDismiss = { showTargetPicker = false }
        )
    }

    val prefs = koinInject<PreferenceManager>()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header with icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RivoIconBox(
                            icon = Icons.Default.GroupAdd,
                            iconContainerColor = MaterialTheme.colorScheme.primary,
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        Column {
                            Text(
                                "Create Contact Group",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Organize contacts into custom groups",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group Name Input
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Family, Work, Friends") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (groupName.isNotEmpty()) {
                            IconButton(onClick = { groupName = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts by name or number…") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selection Header Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "${selectedContactIds.size} of ${filteredContacts.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    selectedContactIds = filteredContacts.map { it.id }.toSet()
                                }
                            ) {
                                Text("Select All", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { selectedContactIds = emptySet() }
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Multi-selection contact list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val isChecked = contact.id in selectedContactIds
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedContactIds = if (isChecked) {
                                            selectedContactIds - contact.id
                                        } else {
                                            selectedContactIds + contact.id
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RivoAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    modifier = Modifier.size(38.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        contact.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (contact.phoneNumbers.isNotEmpty()) {
                                        Text(
                                            contact.phoneNumbers.first(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedContactIds = if (checked) {
                                            selectedContactIds + contact.id
                                        } else {
                                            selectedContactIds - contact.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                showTargetPicker = true
                            }
                        },
                        enabled = groupName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Group")
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactGroupDialog(
    group: ContactGroup,
    onDismiss: () -> Unit,
    onSave: (ContactGroup) -> Unit
) {
    val contactsVM: ContactsViewModel = org.koin.compose.viewmodel.koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    var groupName by remember { mutableStateOf(group.name) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedContactIds by remember { mutableStateOf<Set<String>>(group.contactIds.toSet()) }

    val filteredContacts = remember(allContacts, searchQuery) {
        if (searchQuery.isBlank()) allContacts
        else allContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumbers.any { num -> num.contains(searchQuery) }
        }
    }

    val prefs = koinInject<PreferenceManager>()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header with icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RivoIconBox(
                            icon = Icons.Outlined.Edit,
                            iconContainerColor = MaterialTheme.colorScheme.primary,
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        Column {
                            Text(
                                "Edit Contact Group",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Update group name and member contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group Name Input
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Family, Work, Friends") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (groupName.isNotEmpty()) {
                            IconButton(onClick = { groupName = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts by name or number…") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selection Header Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "${selectedContactIds.size} of ${filteredContacts.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    selectedContactIds = filteredContacts.map { it.id }.toSet()
                                }
                            ) {
                                Text("Select All", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { selectedContactIds = emptySet() }
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Multi-selection contact list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val isChecked = contact.id in selectedContactIds
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedContactIds = if (isChecked) {
                                            selectedContactIds - contact.id
                                        } else {
                                            selectedContactIds + contact.id
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RivoAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    modifier = Modifier.size(38.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        contact.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (contact.phoneNumbers.isNotEmpty()) {
                                        Text(
                                            contact.phoneNumbers.first(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedContactIds = if (checked) {
                                            selectedContactIds + contact.id
                                        } else {
                                            selectedContactIds - contact.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                val updatedGroup = ContactGroup(
                                    id = group.id,
                                    name = groupName.trim(),
                                    contactIds = selectedContactIds.toList(),
                                    accountType = group.accountType,
                                    accountName = group.accountName,
                                    targetLabel = group.targetLabel
                                )
                                onSave(updatedGroup)
                            }
                        },
                        enabled = groupName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
}

@Composable
fun DeleteContactGroupDialog(
    groups: List<ContactGroup>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    var selectedGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        val count = selectedGroupIds.size
        val firstGroupName = groups.find { it.id in selectedGroupIds }?.name ?: "Group"
        val titleText = if (count == 1) "Delete \"$firstGroupName\"?" else "Delete $count Contact Groups?"
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(titleText, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to delete the selected contact group(s)? The contacts inside these groups will not be deleted."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedGroupIds.forEach { id -> onDelete(id) }
                            selectedGroupIds = emptySet()
                            showConfirmDialog = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.70f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RivoIconBox(
                            icon = Icons.Outlined.Delete,
                            iconContainerColor = MaterialTheme.colorScheme.error,
                            size = 44.dp,
                            iconSize = 22.dp
                        )
                        Column {
                            Text(
                                "Delete Contact Groups",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Select groups to remove",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No contact groups to delete.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Selection control banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedGroupIds.size} of ${groups.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        selectedGroupIds = groups.map { it.id }.toSet()
                                    }
                                ) {
                                    Text("Select All", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(
                                    onClick = { selectedGroupIds = emptySet() }
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(groups, key = { it.id }) { group ->
                            val isChecked = group.id in selectedGroupIds
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isChecked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGroupIds = if (isChecked) {
                                                selectedGroupIds - group.id
                                            } else {
                                                selectedGroupIds + group.id
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RivoIconBox(
                                            icon = Icons.Default.People,
                                            iconContainerColor = if (isChecked) MaterialTheme.colorScheme.error else Color(0xFF7C4DFF),
                                            size = 36.dp,
                                            iconSize = 18.dp
                                        )

                                        Column {
                                            Text(
                                                group.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isChecked) MaterialTheme.colorScheme.error
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${group.contactIds.size} contacts",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedGroupIds = if (checked) {
                                                selectedGroupIds + group.id
                                            } else {
                                                selectedGroupIds - group.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = selectedGroupIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedGroupIds.isNotEmpty()) "Delete (${selectedGroupIds.size})" else "Delete")
                    }
                }
            }
        }
    }
}
}

@Composable
fun SelectGroupSaveTargetDialog(
    groupName: String,
    targets: List<com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget>,
    onSelect: (com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget) -> Unit,
    onDismiss: () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    Dialog(onDismissRequest = onDismiss) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.FolderShared,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Save Group To",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (groupName.isNotBlank()) "Choose account/storage for \"$groupName\"" else "Choose account or storage destination",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    val groupTargets = targets.filter { !it.isSim }
                    if (groupTargets.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(
                                        com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget(
                                            label = "Device",
                                            subLabel = "This phone only"
                                        )
                                    )
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF607D8B).copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF607D8B), modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Device", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("This phone only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        groupTargets.forEach { target ->
                            val (icon, tint) = when {
                                target.accountType?.contains("google", ignoreCase = true) == true ->
                                    Icons.Default.Email to Color(0xFFE53935)
                                target.accountType?.contains("exchange", ignoreCase = true) == true ||
                                target.accountType?.contains("outlook", ignoreCase = true) == true ->
                                    Icons.Default.Business to Color(0xFF0078D4)
                                target.accountType == null ->
                                    Icons.Default.PhoneAndroid to Color(0xFF607D8B)
                                else ->
                                    Icons.Default.AccountCircle to MaterialTheme.colorScheme.primary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(target) }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = tint.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(target.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    if (target.subLabel != null) {
                                        Text(
                                            target.subLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                    }
                }
            }
        }
    }
}

