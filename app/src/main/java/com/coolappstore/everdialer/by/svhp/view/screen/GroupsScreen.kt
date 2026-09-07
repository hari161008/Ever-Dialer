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
import androidx.compose.ui.platform.LocalContext
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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

                if (visibleContactGroups.isEmpty()) {
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
                    items(visibleContactGroups, key = { it.id }) { group ->
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

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
