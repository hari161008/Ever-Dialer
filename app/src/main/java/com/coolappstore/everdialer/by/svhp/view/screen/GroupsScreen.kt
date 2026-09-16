package com.coolappstore.everdialer.by.svhp.view.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun GroupsScreen(
    navController: NavController,
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val contactGroups by contactsVM.contactGroups.collectAsState()
    val enabledAccountKeys by contactsVM.enabledAccountKeys.collectAsState()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val hiddenGroupIds = remember(settingsVersion) { prefs.getHiddenContactGroupIds() }
    val showGroupsTab = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS, false) }

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<ContactGroup?>(null) }

    LaunchedEffect(Unit) {
        contactsVM.fetchContactGroups()
    }

    val visibleContactGroups = remember(contactGroups, hiddenGroupIds, enabledAccountKeys) {
        contactGroups.filter { it.id !in hiddenGroupIds }.filter { group ->
            com.coolappstore.everdialer.by.svhp.view.components.isGroupMatchingAccountFilter(group, enabledAccountKeys)
        }
    }

    var groupSortOrder by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_GROUPS_SORT_ORDER, "default") ?: "default")
    }

    val sortedContactGroups = remember(visibleContactGroups, groupSortOrder) {
        when (groupSortOrder) {
            "name_asc" -> visibleContactGroups.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            "name_desc" -> visibleContactGroups.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            "count_desc" -> visibleContactGroups.sortedByDescending { it.contactIds.size }
            "count_asc" -> visibleContactGroups.sortedBy { it.contactIds.size }
            else -> visibleContactGroups
        }
    }

    val allGroupedIds = remember(contactGroups) { contactGroups.flatMap { it.contactIds }.toSet() }
    val ungroupedCount = remember(allContacts, allGroupedIds) { allContacts.count { it.id !in allGroupedIds } }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun openGroupInContacts(groupId: String?) {
        if (groupId == null) {
            contactsVM.clearFilters()
        } else {
            contactsVM.setGroupFilter(groupId)
        }
        navController.navigate(ContactScreenDestination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
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

    ProvideScaledDensity(prefs = prefs) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopBar(navController, navigator)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!showGroupsTab && navController.previousBackStackEntry != null) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = "Groups",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var showSortMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort by",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Default") },
                                        onClick = {
                                            groupSortOrder = "default"
                                            prefs.setString(PreferenceManager.KEY_GROUPS_SORT_ORDER, "default")
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (groupSortOrder == "default") {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (A to Z)") },
                                        onClick = {
                                            groupSortOrder = "name_asc"
                                            prefs.setString(PreferenceManager.KEY_GROUPS_SORT_ORDER, "name_asc")
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (groupSortOrder == "name_asc") {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (Z to A)") },
                                        onClick = {
                                            groupSortOrder = "name_desc"
                                            prefs.setString(PreferenceManager.KEY_GROUPS_SORT_ORDER, "name_desc")
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (groupSortOrder == "name_desc") {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Most Contacts") },
                                        onClick = {
                                            groupSortOrder = "count_desc"
                                            prefs.setString(PreferenceManager.KEY_GROUPS_SORT_ORDER, "count_desc")
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (groupSortOrder == "count_desc") {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Least Contacts") },
                                        onClick = {
                                            groupSortOrder = "count_asc"
                                            prefs.setString(PreferenceManager.KEY_GROUPS_SORT_ORDER, "count_asc")
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (groupSortOrder == "count_asc") {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                            var showCleanDialog by remember { mutableStateOf(false) }
                            if (contactGroups.size > 5) {
                                IconButton(onClick = { showCleanDialog = true }) {
                                    Icon(
                                        Icons.Outlined.DeleteSweep,
                                        contentDescription = "Clean Up Groups",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (showCleanDialog) {
                                AlertDialog(
                                    onDismissRequest = { showCleanDialog = false },
                                    title = { Text("Clean Up Groups") },
                                    text = { Text("Do you want to remove all auto-imported system groups and keep only your groups, or clear all groups?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                contactsVM.cleanupAutoImportedGroups()
                                                showCleanDialog = false
                                            }
                                        ) {
                                            Text("Remove Imported")
                                        }
                                    },
                                    dismissButton = {
                                        Row {
                                            TextButton(
                                                onClick = {
                                                    contactsVM.clearAllContactGroups()
                                                    showCleanDialog = false
                                                }
                                            ) {
                                                Text("Clear All", color = MaterialTheme.colorScheme.error)
                                            }
                                            TextButton(onClick = { showCleanDialog = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    }
                                )
                            }
                            IconButton(onClick = { showAddGroupDialog = true }) {
                                Icon(
                                    Icons.Filled.GroupAdd,
                                    contentDescription = "Add Group",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLandscape) {
                    item(key = "search_bar_pill", contentType = "searchBar") {
                        SearchBarPill(
                            navigator = navigator,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                    }
                }

                // "All Contacts" item
                item(key = "all_contacts_group") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openGroupInContacts(null) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RivoIconBox(
                                icon = Icons.Default.People,
                                iconContainerColor = Color(0xFF2196F3),
                                size = 44.dp,
                                iconSize = 24.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "All Contacts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${allContacts.size} contacts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (sortedContactGroups.isEmpty()) {
                    item(key = "empty_groups") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No contact groups found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(sortedContactGroups, key = { it.id }) { group ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { openGroupInContacts(group.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RivoIconBox(
                                    icon = Icons.Default.People,
                                    iconContainerColor = Color(0xFF7C4DFF),
                                    size = 44.dp,
                                    iconSize = 24.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val subtitle = buildString {
                                        append("${group.contactIds.size} contacts")
                                        val rawTarget = group.targetLabel?.ifBlank { null } ?: group.accountName?.ifBlank { null }
                                        if (!rawTarget.isNullOrBlank()) {
                                            val emailInBracketsRegex = Regex("\\s*\\([^)]*@[^)]*\\)")
                                            var cleaned = rawTarget.replace(emailInBracketsRegex, "").trim()
                                            if (cleaned.isBlank() && !group.accountName.isNullOrBlank()) {
                                                cleaned = group.accountName
                                            }
                                            if (cleaned.isNotBlank()) {
                                                append(" · ")
                                                append(cleaned)
                                            }
                                        }
                                    }
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { groupToEdit = group },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Edit group",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // "Ungrouped" item at the last of the list
                item(key = "ungrouped_contacts_group") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { openGroupInContacts(ContactsViewModel.GROUP_ID_UNGROUPED) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RivoIconBox(
                                icon = Icons.Outlined.People,
                                iconContainerColor = Color(0xFF009688),
                                size = 44.dp,
                                iconSize = 24.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Ungrouped",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "$ungroupedCount contacts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
