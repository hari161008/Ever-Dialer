package com.modiprincekr.thedialer.view.screen.settings

import android.content.Context
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modiprincekr.thedialer.controller.ContactsViewModel
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.controller.util.makeCall
import com.modiprincekr.thedialer.modal.data.Contact
import com.modiprincekr.thedialer.view.components.RivoAnimatedSection
import com.modiprincekr.thedialer.view.components.RivoAvatar
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.SelectExistingContactDialog
import com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar
import com.modiprincekr.thedialer.view.components.rivoHeadingColor
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SpeedDialScreen(
    navigator: DestinationsNavigator
) {
    SpeedDialScreenContent(
        onBackClick = { navigator.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedDialScreenContent(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    var selectedKeyToAssign by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var pendingContactForNumberChoice by remember { mutableStateOf<Contact?>(null) }

    var showVoicemailDialog by remember { mutableStateOf(false) }
    var assignedActionKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Speed Dial",
                onBackClick = onBackClick,
                endActions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear all") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showClearAllConfirm = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Info Card
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        text = "Speed Dial (Keys 1–9)",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "3x3 Speed Dial Grid",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Long press any key on the dialpad to quickly call. Key 1 is dedicated to Voicemail.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3x3 Circular Grid Card
            RivoAnimatedSection(delayMs = 40L) {
                RivoExpressiveCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val gridRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9")
                        )

                        gridRows.forEach { rowDigits ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                rowDigits.forEach { digit ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        if (digit == "1") {
                                            // Key 1: Voicemail
                                            val customVmNumber = remember(settingsVer) { prefs.getSpeedDialNumber("1") }
                                            val carrierVm = remember {
                                                try {
                                                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                                                    tm?.voiceMailNumber?.ifBlank { null }
                                                } catch (_: Throwable) { null }
                                            }
                                            val activeVmNumber = customVmNumber ?: carrierVm
                                            val customVmName = remember(settingsVer) { prefs.getSpeedDialName("1") }

                                            SpeedDialGridItem(
                                                digit = "1",
                                                isVoicemail = true,
                                                assignedName = customVmName ?: "Voicemail",
                                                assignedNumber = activeVmNumber,
                                                contactPhotoUri = null,
                                                onClick = { showVoicemailDialog = true },
                                                onLongClick = { showVoicemailDialog = true }
                                            )
                                        } else {
                                            // Keys 2-9: Contacts
                                            val number = remember(settingsVer, digit) { prefs.getSpeedDialNumber(digit) }
                                            val name = remember(settingsVer, digit) { prefs.getSpeedDialName(digit) }

                                            val matchedContact = remember(number, name, allContacts) {
                                                if (number.isNullOrBlank() && name.isNullOrBlank()) null
                                                else {
                                                    allContacts.firstOrNull { c ->
                                                        (number != null && c.phoneNumbers.any { phone ->
                                                            val cleanP = phone.filter { it.isDigit() }
                                                            val cleanN = number.filter { it.isDigit() }
                                                            cleanP.isNotEmpty() && (cleanP == cleanN || cleanP.endsWith(cleanN) || cleanN.endsWith(cleanP))
                                                        }) || (name != null && c.name.equals(name, ignoreCase = true))
                                                    }
                                                }
                                            }

                                            val photoUri = matchedContact?.photoUri
                                            val firstName = matchedContact?.firstName?.ifBlank { null }
                                                ?: name?.split(" ")?.firstOrNull()
                                                ?: name
                                                ?: number

                                            val isAssigned = !number.isNullOrBlank()

                                            SpeedDialGridItem(
                                                digit = digit,
                                                isVoicemail = false,
                                                assignedName = firstName,
                                                assignedNumber = number,
                                                contactPhotoUri = photoUri,
                                                onClick = {
                                                    if (isAssigned) {
                                                        assignedActionKey = digit
                                                    } else {
                                                        selectedKeyToAssign = digit
                                                        showContactPicker = true
                                                    }
                                                },
                                                onLongClick = {
                                                    if (isAssigned) {
                                                        assignedActionKey = digit
                                                    } else {
                                                        selectedKeyToAssign = digit
                                                        showContactPicker = true
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
            }
        }
    }

    // Contact Picker Dialog
    if (showContactPicker && selectedKeyToAssign != null) {
        val digit = selectedKeyToAssign!!
        SelectExistingContactDialog(
            contacts = allContacts,
            onSelectContact = { contact ->
                showContactPicker = false
                if (contact.phoneNumbers.size > 1) {
                    pendingContactForNumberChoice = contact
                } else {
                    val primaryNum = prefs.getContactDefaultNumber(contact.id)?.takeIf { it in contact.phoneNumbers }
                    val phone = primaryNum ?: contact.phoneNumbers.firstOrNull { it.isNotBlank() }
                    if (!phone.isNullOrBlank()) {
                        prefs.setSpeedDial(digit, phone, contact.name)
                        Toast.makeText(context, "Key $digit set to ${contact.name}", Toast.LENGTH_SHORT).show()
                    }
                    selectedKeyToAssign = null
                }
            },
            onDismiss = {
                showContactPicker = false
                selectedKeyToAssign = null
            }
        )
    }

    // Phone Number Chooser Dialog (when contact has multiple numbers)
    if (pendingContactForNumberChoice != null && selectedKeyToAssign != null) {
        val contact = pendingContactForNumberChoice!!
        val digit = selectedKeyToAssign!!
        AlertDialog(
            onDismissRequest = {
                pendingContactForNumberChoice = null
                selectedKeyToAssign = null
            },
            title = { Text("Choose Number for ${contact.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.phoneNumbers.forEach { numOption ->
                        Surface(
                            onClick = {
                                pendingContactForNumberChoice = null
                                prefs.setSpeedDial(digit, numOption, contact.name)
                                Toast.makeText(context, "Key $digit set to ${contact.name}", Toast.LENGTH_SHORT).show()
                                selectedKeyToAssign = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = numOption,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingContactForNumberChoice = null
                        selectedKeyToAssign = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Assigned Contact Action Dialog (Keys 2-9)
    if (assignedActionKey != null) {
        val key = assignedActionKey!!
        val number = prefs.getSpeedDialNumber(key)
        val name = prefs.getSpeedDialName(key)

        AlertDialog(
            onDismissRequest = { assignedActionKey = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            title = {
                Text(
                    text = name ?: "Key $key",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!number.isNullOrBlank()) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )

                        FilledTonalButton(
                            onClick = {
                                assignedActionKey = null
                                makeCall(context, number)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Call $number")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val targetKey = key
                            assignedActionKey = null
                            selectedKeyToAssign = targetKey
                            showContactPicker = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reassign / Replace")
                    }

                    TextButton(
                        onClick = {
                            prefs.removeSpeedDial(key)
                            assignedActionKey = null
                            Toast.makeText(context, "Key $key cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Speed Dial")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { assignedActionKey = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Voicemail Configuration Dialog (Key 1)
    if (showVoicemailDialog) {
        val currentVm = prefs.getVoicemailNumber(context)
        val isCustomSet = !prefs.getSpeedDialNumber("1").isNullOrBlank()
        var isEditing by remember { mutableStateOf(currentVm.isNullOrBlank()) }
        var inputNumber by remember { mutableStateOf(currentVm ?: "") }

        AlertDialog(
            onDismissRequest = { showVoicemailDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Voicemail,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (isEditing) "Configure Voicemail" else "Voicemail (Key 1)",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Set the voicemail phone number to dial when holding Key 1 on the dialpad.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = inputNumber,
                            onValueChange = { inputNumber = it },
                            label = { Text("Voicemail Number") },
                            placeholder = { Text("e.g. *86 or phone number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isCustomSet) "Custom Voicemail Number" else "Carrier Voicemail Number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentVm ?: "None",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (!currentVm.isNullOrBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    showVoicemailDialog = false
                                    makeCall(context, currentVm)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Call Voicemail")
                            }
                        }

                        OutlinedButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Change Voicemail Number")
                        }

                        if (isCustomSet) {
                            TextButton(
                                onClick = {
                                    prefs.removeSpeedDial("1")
                                    showVoicemailDialog = false
                                    Toast.makeText(context, "Voicemail reset to carrier default", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reset to Carrier Default")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isEditing) {
                    Button(
                        onClick = {
                            if (inputNumber.isNotBlank()) {
                                prefs.setSpeedDial("1", inputNumber.trim(), "Voicemail")
                                Toast.makeText(context, "Voicemail number saved", Toast.LENGTH_SHORT).show()
                            } else {
                                prefs.removeSpeedDial("1")
                            }
                            showVoicemailDialog = false
                            isEditing = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isEditing && !currentVm.isNullOrBlank()) {
                            isEditing = false
                        } else {
                            showVoicemailDialog = false
                        }
                    }
                ) {
                    Text(if (isEditing && !currentVm.isNullOrBlank()) "Back" else "Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Clear All Speed Dials?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will remove all assigned contacts and custom voicemail configurations (Keys 1–9).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.clearAllSpeedDials()
                        showClearAllConfirm = false
                        Toast.makeText(context, "All speed dials cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * 3x3 Circular Grid Item Composable for Speed Dial.
 * Displays either Voicemail (Key 1), an unassigned slot (+ icon), or an assigned contact (avatar + name).
 * Features a circular container and an overlapping number badge at the bottom-right corner.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialGridItem(
    digit: String,
    isVoicemail: Boolean,
    assignedName: String?,
    assignedNumber: String?,
    contactPhotoUri: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAssigned = isVoicemail || !assignedNumber.isNullOrBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        // Circle + Overlapping Badge Container
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Circle Button
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                shape = CircleShape,
                color = when {
                    isVoicemail -> MaterialTheme.colorScheme.secondaryContainer
                    isAssigned -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    1.dp,
                    if (isAssigned) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                ),
                tonalElevation = if (isAssigned) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isVoicemail -> {
                            Icon(
                                imageVector = Icons.Rounded.Voicemail,
                                contentDescription = "Voicemail",
                                modifier = Modifier.size(34.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        isAssigned -> {
                            RivoAvatar(
                                name = assignedName ?: assignedNumber ?: digit,
                                photoUri = contactPhotoUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Assign Contact",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Overlapping Number Badge (aligned to BottomEnd)
            Surface(
                shape = CircleShape,
                color = when {
                    isVoicemail -> MaterialTheme.colorScheme.primary
                    isAssigned -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = when {
                            isVoicemail -> MaterialTheme.colorScheme.onPrimary
                            isAssigned -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Primary Label: First Name / Voicemail / Empty
        val primaryLabel = when {
            isVoicemail -> assignedName ?: "Voicemail"
            isAssigned -> assignedName ?: assignedNumber ?: "Contact"
            else -> "Empty"
        }

        Text(
            text = primaryLabel,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isAssigned) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isAssigned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        // Secondary Label: Number or Key Indicator
        val secondaryLabel = when {
            isVoicemail -> if (!assignedNumber.isNullOrBlank()) assignedNumber else "Voicemail"
            isAssigned -> assignedNumber ?: ""
            else -> "Key $digit"
        }

        Text(
            text = secondaryLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}
