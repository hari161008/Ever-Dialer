package com.coolappstore.everdialer.by.svhp.view.screen

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

private data class EditableField(val id: Long, val value: String)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun ContactEditScreen(
    contactId: String? = null,
    initialName: String? = null,
    initialPhone: String? = null,
    navigator: DestinationsNavigator
) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    
    var resolvedContact by remember { mutableStateOf<Contact?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var nextFieldId by remember { mutableLongStateOf(100L) }

    var name by remember { mutableStateOf(initialName ?: "") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    val phoneFields = remember { 
        mutableStateListOf<EditableField>().apply { 
            add(EditableField(1L, initialPhone ?: "")) 
        } 
    }
    val emailFields = remember { mutableStateListOf<EditableField>().apply { add(EditableField(2L, "")) } }
    val addressFields = remember { mutableStateListOf<EditableField>().apply { add(EditableField(3L, "")) } }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(contactId, allContacts) {
        if (contactId != null && contactId != "0" && contactId != "null" && !isInitialized) {
            val contact = allContacts.find { it.id == contactId } 
                ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { contactsVM.getContactById(contactId) }
            if (contact != null) {
                resolvedContact = contact
                name = contact.name
                photoUri = contact.photoUri
                
                phoneFields.clear()
                if (contact.phoneNumbers.isNotEmpty()) {
                    contact.phoneNumbers.forEach { phoneFields.add(EditableField(nextFieldId++, it)) }
                } else {
                    phoneFields.add(EditableField(nextFieldId++, ""))
                }

                emailFields.clear()
                if (contact.emails.isNotEmpty()) {
                    contact.emails.forEach { emailFields.add(EditableField(nextFieldId++, it)) }
                } else {
                    emailFields.add(EditableField(nextFieldId++, ""))
                }

                addressFields.clear()
                if (contact.addresses.isNotEmpty()) {
                    contact.addresses.forEach { addressFields.add(EditableField(nextFieldId++, it)) }
                } else {
                    addressFields.add(EditableField(nextFieldId++, ""))
                }
                description = contact.note ?: ""
                isInitialized = true
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri.toString() }
    )

    val context = LocalContext.current
    var showSaveTargetDialog by remember { mutableStateOf(false) }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }
    val isNewContact = contactId.isNullOrBlank() || contactId == "0" || contactId == "null"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isNewContact) "Create Contact" else "Edit Contact",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val validPhones = phoneFields.map { it.value.trim() }.filter { it.isNotBlank() }
                    Button(
                        onClick = {
                            val finalContactId = if (isNewContact) "0" else (contactId ?: "0")
                            val contactToSave = Contact(
                                id = finalContactId,
                                name = name.trim(),
                                phoneNumbers = validPhones,
                                emails = emailFields.map { it.value.trim() }.filter { it.isNotBlank() },
                                addresses = addressFields.map { it.value.trim() }.filter { it.isNotBlank() },
                                photoUri = photoUri,
                                note = description.trim().ifBlank { null },
                                isFavorite = resolvedContact?.isFavorite ?: false,
                                sourceAccounts = resolvedContact?.sourceAccounts ?: emptyList(),
                                events = resolvedContact?.events ?: emptyList()
                            )
                            if (isNewContact) {
                                pendingContact = contactToSave
                                showSaveTargetDialog = true
                            } else {
                                contactsVM.saveContact(contactToSave)
                                navigator.navigateUp()
                            }
                        },
                        enabled = name.isNotBlank() && validPhones.isNotEmpty(),
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        RivoAvatar(
                            name = name,
                            photoUri = photoUri,
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape
                        )
                        
                        Row {
                            if (photoUri != null) {
                                SmallFloatingActionButton(
                                    onClick = { photoUri = null },
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            
                            SmallFloatingActionButton(
                                onClick = { 
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            
            item {
                RivoSectionHeader(title = "Identity")
                RivoExpressiveCard {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            
            item {
                RivoSectionHeader(title = "Phone Numbers")
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        phoneFields.forEachIndexed { index, field ->
                            key(field.id) {
                                EditField(
                                    value = field.value,
                                    onValueChange = { newValue ->
                                        phoneFields[index] = field.copy(value = newValue)
                                    },
                                    label = "Phone",
                                    icon = Icons.Default.Phone,
                                    onDelete = if (phoneFields.size > 1) { { phoneFields.removeAt(index) } } else null
                                )
                            }
                        }
                        TextButton(
                            onClick = { phoneFields.add(EditableField(nextFieldId++, "")) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Phone")
                        }
                    }
                }
            }

            
            item {
                RivoSectionHeader(title = "Emails")
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        emailFields.forEachIndexed { index, field ->
                            key(field.id) {
                                EditField(
                                    value = field.value,
                                    onValueChange = { newValue ->
                                        emailFields[index] = field.copy(value = newValue)
                                    },
                                    label = "Email",
                                    icon = Icons.Default.Email,
                                    onDelete = if (emailFields.size > 1) { { emailFields.removeAt(index) } } else null
                                )
                            }
                        }
                        TextButton(
                            onClick = { emailFields.add(EditableField(nextFieldId++, "")) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Email")
                        }
                    }
                }
            }

            
            item {
                RivoSectionHeader(title = "Address")
                RivoExpressiveCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        addressFields.forEachIndexed { index, field ->
                            key(field.id) {
                                EditField(
                                    value = field.value,
                                    onValueChange = { newValue ->
                                        addressFields[index] = field.copy(value = newValue)
                                    },
                                    label = "Address",
                                    icon = Icons.Default.LocationOn,
                                    onDelete = if (addressFields.size > 1) { { addressFields.removeAt(index) } } else null
                                )
                            }
                        }
                        TextButton(
                            onClick = { addressFields.add(EditableField(nextFieldId++, "")) },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Address")
                        }
                    }
                }
            }
            
            item {
                RivoSectionHeader(title = "Description")
                RivoExpressiveCard {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Notes synced with Google / Exchange") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Description, null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showSaveTargetDialog && pendingContact != null) {
        val saveTargets = remember { contactsVM.getSaveTargets() }
        SaveContactToDialog(
            targets = saveTargets,
            onSelect = { target ->
                val contactToSave = pendingContact!!
                if (target.isSim) {
                    contactsVM.saveContactToSim(contactToSave, target.simSlotIndex) { success ->
                        Toast.makeText(
                            context,
                            if (success) "Saved to ${target.label}" else "Couldn't save to ${target.label}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    contactsVM.saveContact(contactToSave, target.accountType, target.accountName)
                }
                showSaveTargetDialog = false
                navigator.navigateUp()
            },
            onDismiss = { showSaveTargetDialog = false }
        )
    }
}

@Composable
private fun SaveContactToDialog(
    targets: List<ContactSaveTarget>,
    onSelect: (ContactSaveTarget) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Save contact to",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                targets.forEach { target ->
                    val icon: ImageVector = when {
                        target.isSim -> Icons.Default.SimCard
                        target.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                        target.accountType != null -> Icons.Default.Sync
                        else -> Icons.Default.PhoneAndroid
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(target) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(target.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    onDelete: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    null, 
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
