package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactsHiderScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    var secretCode by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, "") ?: "") }
    val isEnabled = secretCode.isNotEmpty()

    val hiddenIds by remember(prefs) {
        derivedStateOf {
            val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
            if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
        }
    }
    var hiddenIdsState by remember { mutableStateOf(hiddenIds) }

    var hideNames by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false)) }
    var hideMenu  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false)) }

    var showContactPicker by remember { mutableStateOf(false) }
    var contactSearch by remember { mutableStateOf("") }

    fun saveHiddenIds(ids: Set<String>) {
        hiddenIdsState = ids
        prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, ids.joinToString(","))
        
    }

    fun navigateBack() { navigator.navigateUp() }
    BackHandler { navigateBack() }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Contacts Hider", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigateBack() })
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp + navBarBottom)
        ) {

            // ── Secret Code ──────────────────────────────────────────────────
            item {
                RivoExpressiveCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Secret Code",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Type this code in the Dialpad to open your hidden contacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = secretCode,
                            onValueChange = { v ->
                                val digits = v.filter { it.isDigit() }
                                secretCode = digits
                                prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, digits)
                                
                                if (digits.isEmpty() && hideMenu) {
                                    hideMenu = false
                                    prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false)
                                    
                                }
                            },
                            label = { Text("Enter a secret code") },
                            placeholder = { Text("Numbers only") },
                            leadingIcon = { Icon(Icons.Default.Pin, null) },
                            trailingIcon = {
                                if (secretCode.isNotEmpty()) {
                                    IconButton(onClick = {
                                        secretCode = ""
                                        prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, "")
                                        if (hideMenu) {
                                            hideMenu = false
                                            prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false)
                                        }
                                        
                                    }) { Icon(Icons.Default.Close, null) }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        AnimatedVisibility(visible = isEnabled) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Text(
                                    "Contacts Hider is active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }

            // ── Hidden Contacts ───────────────────────────────────────────────
            item {
                RivoExpressiveCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PersonOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Hidden Contacts",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Add contacts to hide them from the contact list",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { showContactPicker = true; contactSearch = "" },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        AnimatedVisibility(visible = hiddenIdsState.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hidden contacts yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (hiddenIdsState.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            val hiddenContacts = allContacts.filter { it.id in hiddenIdsState }
                            hiddenContacts.forEach { contact ->
                                HiddenContactRow(
                                    contact = contact,
                                    onRemove = {
                                        saveHiddenIds(hiddenIdsState - contact.id)
                                    }
                                )
                            }
                            // Also show IDs where contact was deleted but still stored
                            val missingIds = hiddenIdsState - allContacts.map { it.id }.toSet()
                            missingIds.forEach { id ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PersonOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Contact deleted ($id)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.weight(1f))
                                    IconButton(onClick = { saveHiddenIds(hiddenIdsState - id) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Hide name & menu options ───────────────────────────────────────
            item {
                RivoExpressiveCard {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Hide contact name in incoming calls and call logs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Hidden contacts will show only their number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hideNames,
                                onCheckedChange = { v ->
                                    hideNames = v
                                    prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, v)
                                    
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isEnabled) {
                                    if (isEnabled) {
                                        hideMenu = !hideMenu
                                        prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, !hideMenu.not())
                                        
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HideSource,
                                contentDescription = null,
                                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Hide \"Contact Hider\" menu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Text(
                                    if (isEnabled) "The Contacts Hider entry in Settings will be hidden"
                                    else "Set a secret code first to enable this",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 1f else 0.5f)
                                )
                            }
                            Checkbox(
                                checked = hideMenu,
                                onCheckedChange = if (isEnabled) { v ->
                                    hideMenu = v
                                    prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, v)
                                    
                                } else null,
                                enabled = isEnabled
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Contact picker dialog ─────────────────────────────────────────────────
    if (showContactPicker) {
        Dialog(onDismissRequest = { showContactPicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Select contacts to hide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = contactSearch,
                        onValueChange = { contactSearch = it },
                        placeholder = { Text("Search contacts…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (contactSearch.isNotEmpty()) {
                                IconButton(onClick = { contactSearch = "" }) { Icon(Icons.Default.Close, null) }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = allContacts.filter { c ->
                        c.id !in hiddenIdsState && (contactSearch.isBlank() ||
                            c.name.contains(contactSearch, ignoreCase = true) ||
                            c.phoneNumbers.any { it.contains(contactSearch) })
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        saveHiddenIds(hiddenIdsState + contact.id)
                                        showContactPicker = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RivoAvatar(name = contact.name, photoUri = contact.photoUri, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    contact.phoneNumbers.firstOrNull()?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showContactPicker = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenContactRow(contact: Contact, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RivoAvatar(name = contact.name, photoUri = contact.photoUri, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            contact.phoneNumbers.firstOrNull()?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}
