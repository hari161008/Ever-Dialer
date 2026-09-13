package com.coolappstore.everdialer.by.svhp.view.components

import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * A floating popup dialog with 2 options:
 * 1. "Add a new contact"
 * 2. "Add a new number to existing contact"
 *
 * Used from Call Logs context menu, Dialpad ("Add Contact" button & overflow menu),
 * and Contact Info screen when viewing an unknown/unsaved number.
 */
@Composable
fun AddContactChoiceDialog(
    visible: Boolean,
    phoneNumber: String,
    onDismissRequest: () -> Unit,
    navigator: DestinationsNavigator? = null
) {
    if (!visible) return
    val context = LocalContext.current
    val cleanNumber = phoneNumber.trim()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    var showPicker by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(true) }

    if (showPicker) {
        SelectExistingContactDialog(
            contacts = allContacts,
            onSelectContact = { contact ->
                showPicker = false
                onDismissRequest()
                if (navigator != null) {
                    navigator.navigate(
                        ContactEditScreenDestination(
                            contactId = contact.id,
                            initialPhone = cleanNumber.ifEmpty { null }
                        )
                    )
                } else {
                    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                        type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                        if (cleanNumber.isNotEmpty()) {
                            putExtra(ContactsContract.Intents.Insert.PHONE, cleanNumber)
                        }
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open contacts", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = {
                showPicker = false
                onDismissRequest()
            }
        )
    }

    if (menuOpen && !showPicker) {
        RivoDropdownMenu(
            expanded = true,
            onDismissRequest = {
                menuOpen = false
                onDismissRequest()
            },
            modifier = Modifier.width(310.dp)
        ) {
            RivoDropdownMenuItem(
                text = "Add a new contact",
                icon = Icons.Default.PersonAdd,
                iconTint = Color(0xFF4CAF50),
                onClick = {
                    menuOpen = false
                    onDismissRequest()
                    if (navigator != null) {
                        navigator.navigate(
                            ContactEditScreenDestination(initialPhone = cleanNumber.ifEmpty { null })
                        )
                    } else {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            if (cleanNumber.isNotEmpty()) {
                                putExtra(ContactsContract.Intents.Insert.PHONE, cleanNumber)
                            }
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open contact editor", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            RivoDropdownMenuItem(
                text = "Add a new number to existing contact",
                icon = Icons.Default.Person,
                iconTint = Color(0xFF2196F3),
                maxLines = 2,
                onClick = {
                    menuOpen = false
                    if (navigator != null) {
                        showPicker = true
                    } else {
                        onDismissRequest()
                        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                            if (cleanNumber.isNotEmpty()) {
                                putExtra(ContactsContract.Intents.Insert.PHONE, cleanNumber)
                            }
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open contacts", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SelectExistingContactDialog(
    contacts: List<Contact>,
    onSelectContact: (Contact) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts.sortedBy { it.name.lowercase() }
        } else {
            val q = searchQuery.trim()
            contacts.filter { c ->
                c.name.contains(q, ignoreCase = true) ||
                c.phoneNumbers.any { it.contains(q) }
            }.sortedBy { it.name.lowercase() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Column {
                        SettingsPillTopAppBar(
                            title = "Select Contact",
                            onBackClick = onDismiss
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search contacts…") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (contacts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredContacts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "No contacts found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredContacts, key = { it.id }) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectContact(contact) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    RivoAvatar(
                                        name = contact.name,
                                        photoUri = contact.photoUri,
                                        modifier = Modifier.size(46.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name.ifBlank { "Unknown" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (contact.phoneNumbers.isNotEmpty()) {
                                            Text(
                                                text = contact.phoneNumbers.first(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
