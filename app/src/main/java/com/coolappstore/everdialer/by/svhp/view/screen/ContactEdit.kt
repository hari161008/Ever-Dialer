package com.coolappstore.everdialer.by.svhp.view.screen

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccountInfo
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

    val context = LocalContext.current
    val isNewContact = contactId.isNullOrBlank() || contactId == "0" || contactId == "null"
    val saveTargets = remember { contactsVM.getSaveTargets() }
    var currentAccounts by remember { mutableStateOf<List<ContactAccountInfo>>(emptyList()) }
    var selectedTarget by remember { mutableStateOf<ContactSaveTarget?>(null) }
    var updateAllLinkedAccounts by remember { mutableStateOf(true) }
    var showAccountPickerDialog by remember { mutableStateOf(false) }
    var showMultiAccountConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(contactId, allContacts) {
        if (contactId != null && contactId != "0" && contactId != "null" && !isInitialized) {
            val contact = allContacts.find { it.id == contactId } 
                ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { contactsVM.getContactById(contactId) }
            val accounts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                contactsVM.getContactAccounts(contactId)
            }
            currentAccounts = accounts
            val writable = accounts.filter { !it.isReadOnly }
            if (writable.size == 1) {
                val acc = writable[0]
                selectedTarget = ContactSaveTarget(
                    label = acc.displayName,
                    subLabel = acc.accountName,
                    accountType = acc.accountType,
                    accountName = acc.accountName,
                    isSim = acc.isSim,
                    simSlotIndex = acc.simSlotIndex
                )
            }

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

    LaunchedEffect(isNewContact, saveTargets) {
        if (isNewContact && selectedTarget == null && saveTargets.isNotEmpty()) {
            selectedTarget = saveTargets.firstOrNull { it.accountType?.contains("google", ignoreCase = true) == true }
                ?: saveTargets.firstOrNull { !it.isSim }
                ?: saveTargets.firstOrNull()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri.toString() }
    )

    val performSave: (ContactSaveTarget?, Boolean) -> Unit = { target, allAccounts ->
        val validPhones = phoneFields.map { it.value.trim() }.filter { it.isNotBlank() }
        val finalContactId = if (isNewContact) "0" else contactId
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
            val finalTarget = target ?: selectedTarget
            if (finalTarget?.isSim == true) {
                contactsVM.saveContactToSim(contactToSave, finalTarget.simSlotIndex) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Saved to ${finalTarget.label}" else "Couldn't save to ${finalTarget.label}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                contactsVM.saveContact(
                    contact = contactToSave,
                    accountType = finalTarget?.accountType,
                    accountName = finalTarget?.accountName
                )
            }
        } else {
            contactsVM.saveContact(
                contact = contactToSave,
                accountType = target?.accountType,
                accountName = target?.accountName,
                updateAllAccounts = allAccounts,
                originalContact = resolvedContact
            )
        }
        navigator.navigateUp()
    }

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
                            if (isNewContact) {
                                if (selectedTarget != null) {
                                    performSave(selectedTarget, false)
                                } else {
                                    showAccountPickerDialog = true
                                }
                            } else {
                                val writable = currentAccounts.filter { !it.isReadOnly }
                                if (writable.size > 1 && !updateAllLinkedAccounts && selectedTarget == null) {
                                    showMultiAccountConfirmDialog = true
                                } else {
                                    performSave(selectedTarget, updateAllLinkedAccounts)
                                }
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
                val writableAccounts = currentAccounts.filter { !it.isReadOnly }
                val (storageTitle, storageSubtitle, storageIcon) = remember(
                    isNewContact,
                    currentAccounts,
                    selectedTarget,
                    updateAllLinkedAccounts
                ) {
                    if (isNewContact) {
                        val label = selectedTarget?.label ?: "Device Storage"
                        val sub = selectedTarget?.subLabel
                        val icon = when {
                            selectedTarget?.isSim == true -> Icons.Default.SimCard
                            selectedTarget?.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                            selectedTarget?.accountType != null -> Icons.Default.Sync
                            else -> Icons.Default.PhoneAndroid
                        }
                        Triple("Saving to", if (sub != null) "$label ($sub)" else label, icon)
                    } else {
                        when {
                            writableAccounts.isEmpty() -> {
                                val label = selectedTarget?.label ?: "Device Storage"
                                Triple("Saving to", "$label (Linked to read-only app)", Icons.Default.PhoneAndroid)
                            }
                            writableAccounts.size > 1 && updateAllLinkedAccounts -> {
                                val names = writableAccounts.joinToString(", ") { it.displayName }
                                Triple("Saved in ${writableAccounts.size} accounts", "$names (Unified & Synced)", Icons.Default.Storage)
                            }
                            selectedTarget != null -> {
                                val icon = when {
                                    selectedTarget?.isSim == true -> Icons.Default.SimCard
                                    selectedTarget?.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                                    selectedTarget?.accountType != null -> Icons.Default.Sync
                                    else -> Icons.Default.PhoneAndroid
                                }
                                val sub = selectedTarget?.subLabel
                                Triple("Saving to", if (sub != null) "${selectedTarget?.label} ($sub)" else (selectedTarget?.label ?: ""), icon)
                            }
                            else -> {
                                val names = currentAccounts.joinToString(", ") { it.displayName }
                                Triple("Saved in", names.ifBlank { "Device Storage" }, Icons.Default.Storage)
                            }
                        }
                    }
                }

                RivoSectionHeader(title = "Storage Location")
                RivoExpressiveCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountPickerDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                storageIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = storageTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = storageSubtitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { showAccountPickerDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
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

    if (showAccountPickerDialog) {
        val writableAccounts = currentAccounts.filter { !it.isReadOnly }
        Dialog(onDismissRequest = { showAccountPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        if (isNewContact) "Save contact to" else "Select save location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )

                    if (!isNewContact && writableAccounts.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    updateAllLinkedAccounts = true
                                    selectedTarget = null
                                    showAccountPickerDialog = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "All linked accounts (${writableAccounts.size})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Keep unified across all accounts (Recommended)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (updateAllLinkedAccounts && selectedTarget == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    val targetsToShow = if (!isNewContact && writableAccounts.isNotEmpty()) {
                        writableAccounts.map { acc ->
                            ContactSaveTarget(
                                label = acc.displayName,
                                subLabel = acc.accountName,
                                accountType = acc.accountType,
                                accountName = acc.accountName,
                                isSim = acc.isSim,
                                simSlotIndex = acc.simSlotIndex
                            )
                        }
                    } else {
                        saveTargets
                    }

                    targetsToShow.forEach { target ->
                        val isSelected = !updateAllLinkedAccounts && (
                            (target.accountType == selectedTarget?.accountType && target.accountName == selectedTarget?.accountName) ||
                            (target.isSim && selectedTarget?.isSim == true && target.simSlotIndex == selectedTarget?.simSlotIndex)
                        )
                        val icon: ImageVector = when {
                            target.isSim -> Icons.Default.SimCard
                            target.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                            target.accountType != null -> Icons.Default.Sync
                            else -> Icons.Default.PhoneAndroid
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTarget = target
                                    updateAllLinkedAccounts = false
                                    showAccountPickerDialog = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(target.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                if (target.subLabel != null) {
                                    Text(
                                        target.subLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showAccountPickerDialog = false },
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)
                    ) { Text("Cancel") }
                }
            }
        }
    }

    if (showMultiAccountConfirmDialog) {
        val writableAccounts = currentAccounts.filter { !it.isReadOnly }
        Dialog(onDismissRequest = { showMultiAccountConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Save contact changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    Text(
                        "This contact is linked across multiple accounts. Choose how to save your changes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMultiAccountConfirmDialog = false
                                performSave(null, true)
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "All linked accounts (${writableAccounts.size})",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Keep unified across all accounts (Recommended)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    writableAccounts.forEach { acc ->
                        val target = ContactSaveTarget(
                            label = acc.displayName,
                            subLabel = acc.accountName,
                            accountType = acc.accountType,
                            accountName = acc.accountName,
                            isSim = acc.isSim,
                            simSlotIndex = acc.simSlotIndex
                        )
                        val icon = when {
                            acc.isSim -> Icons.Default.SimCard
                            acc.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                            acc.accountType != null -> Icons.Default.Sync
                            else -> Icons.Default.PhoneAndroid
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMultiAccountConfirmDialog = false
                                    performSave(target, false)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(target.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                if (target.subLabel != null) {
                                    Text(target.subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showMultiAccountConfirmDialog = false },
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)
                    ) { Text("Cancel") }
                }
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
