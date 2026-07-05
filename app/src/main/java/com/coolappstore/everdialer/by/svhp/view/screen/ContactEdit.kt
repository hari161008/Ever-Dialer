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
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ContactEditScreen(
    contactId: String? = null,
    initialName: String? = null,
    initialPhone: String? = null,
    navigator: DestinationsNavigator
) {
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    
    val existingContact = remember(contactId, allContacts) {
        if (contactId != null && contactId != "0" && contactId != "null") {
            allContacts.find { it.id == contactId }
        } else null
    }

    var name by remember(existingContact) { mutableStateOf(existingContact?.name ?: initialName ?: "") }
    var photoUri by remember(existingContact) { mutableStateOf<String?>(existingContact?.photoUri) }
    
    val phoneNumbers = remember(existingContact) { 
        mutableStateListOf<String>().apply { 
            if (existingContact != null && existingContact.phoneNumbers.isNotEmpty()) {
                addAll(existingContact.phoneNumbers)
            } else if (!initialPhone.isNullOrBlank()) {
                add(initialPhone)
            }
            if (isEmpty()) add("") 
        } 
    }
    
    val emails = remember(existingContact) { 
        mutableStateListOf<String>().apply { 
            if (existingContact != null && existingContact.emails.isNotEmpty()) {
                addAll(existingContact.emails)
            }
            if (isEmpty()) add("")
        } 
    }
    
    val addresses = remember(existingContact) { 
        mutableStateListOf<String>().apply { 
            if (existingContact != null && existingContact.addresses.isNotEmpty()) {
                addAll(existingContact.addresses)
            }
            if (isEmpty()) add("")
        } 
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri.toString() }
    )

    val context = LocalContext.current
    var showSaveTargetDialog by remember { mutableStateOf(false) }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }
    val isNewContact = contactId == null || contactId == "0"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (contactId == null || contactId == "0") "Create Contact" else "Edit Contact",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val contactToSave = Contact(
                                id = contactId ?: "0",
                                name = name,
                                phoneNumbers = phoneNumbers.filter { it.isNotBlank() },
                                emails = emails.filter { it.isNotBlank() },
                                addresses = addresses.filter { it.isNotBlank() },
                                photoUri = photoUri
                            )
                            if (isNewContact) {
                                pendingContact = contactToSave
                                showSaveTargetDialog = true
                            } else {
                                contactsVM.saveContact(contactToSave)
                                navigator.navigateUp()
                            }
                        },
                        enabled = name.isNotBlank() && phoneNumbers.any { it.isNotBlank() },
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
                .fillMaxSize(),
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
                        phoneNumbers.forEachIndexed { index, phone ->
                            EditField(
                                value = phone,
                                onValueChange = { phoneNumbers[index] = it },
                                label = "Phone",
                                icon = Icons.Default.Phone,
                                onDelete = if (phoneNumbers.size > 1) { { phoneNumbers.removeAt(index) } } else null
                            )
                        }
                        TextButton(
                            onClick = { phoneNumbers.add("") },
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
                        emails.forEachIndexed { index, email ->
                            EditField(
                                value = email,
                                onValueChange = { emails[index] = it },
                                label = "Email",
                                icon = Icons.Default.Email,
                                onDelete = if (emails.size > 1) { { emails.removeAt(index) } } else null
                            )
                        }
                        TextButton(
                            onClick = { emails.add("") },
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
                        addresses.forEachIndexed { index, address ->
                            EditField(
                                value = address,
                                onValueChange = { addresses[index] = it },
                                label = "Address",
                                icon = Icons.Default.LocationOn,
                                onDelete = if (addresses.size > 1) { { addresses.removeAt(index) } } else null
                            )
                        }
                        TextButton(
                            onClick = { addresses.add("") },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Address")
                        }
                    }
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
