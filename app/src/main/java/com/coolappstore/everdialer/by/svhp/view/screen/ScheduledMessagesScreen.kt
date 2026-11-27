package com.coolappstore.everdialer.by.svhp.view.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.ScheduledSmsManager
import com.coolappstore.everdialer.by.svhp.modal.data.ScheduledSmsEntry
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SmsChatScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun ScheduledMessagesScreen(
    navigator: DestinationsNavigator,
    threadId: Long = -1L,
    address: String = "",
    contactName: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var refreshKey by remember { mutableIntStateOf(0) }

    var editingEntry by remember { mutableStateOf<ScheduledSmsEntry?>(null) }
    var editedBody by remember { mutableStateOf("") }
    var editedTime by remember { mutableLongStateOf(0L) }
    var showEditDatePicker by remember { mutableStateOf(false) }
    var showEditTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(editingEntry) {
        editingEntry?.let {
            editedBody = it.body
            editedTime = it.scheduledTime
        }
    }

    val editCal = remember(editedTime) {
        Calendar.getInstance().apply {
            if (editedTime > 0) timeInMillis = editedTime
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (editedTime > 0) editedTime else System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = editCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = editCal.get(Calendar.MINUTE),
        is24Hour = false
    )

    val isFilterByThread = threadId > 0 || address.isNotBlank()

    val entries: List<ScheduledSmsEntry> = remember(refreshKey, settingsVer, threadId, address) {
        if (isFilterByThread) {
            ScheduledSmsManager.getEntriesForThreadOrAddress(prefs, threadId, address)
        } else {
            ScheduledSmsManager.loadEntries(prefs).sortedBy { it.scheduledTime }
        }
    }

    val pageTitle = remember(contactName, isFilterByThread) {
        if (!contactName.isNullOrBlank()) "Scheduled for $contactName"
        else "Scheduled Messages"
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = pageTitle,
                onBackClick = { navigator.popBackStack() }
            )
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF00ACC1),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        text = "No scheduled messages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isFilterByThread) "No scheduled messages for this conversation."
                               else "You don't have any scheduled messages across your chats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    val resolvedContact = remember(entry.address, allContacts) {
                        allContacts.firstOrNull { c ->
                            c.phoneNumbers.any { p ->
                                android.telephony.PhoneNumberUtils.compare(context, p, entry.address)
                            }
                        }
                    }
                    val targetName = resolvedContact?.name?.ifBlank { null } ?: contactName ?: entry.address
                    val targetPhoto = resolvedContact?.photoUri

                    val schedTimeStr = remember(entry.scheduledTime) {
                        val cal = Calendar.getInstance().apply { timeInMillis = entry.scheduledTime }
                        val today = Calendar.getInstance()
                        val isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                      cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                        val isTomorrow = cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                                         cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)

                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.scheduledTime))
                        when {
                            isToday -> "Today at $timeStr"
                            isTomorrow -> "Tomorrow at $timeStr"
                            else -> SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(entry.scheduledTime))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingEntry = entry
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RivoAvatar(
                                    name = targetName,
                                    photoUri = targetPhoto,
                                    size = 42.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = Color(0xFF00ACC1),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = schedTimeStr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF00ACC1),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Message Body
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = entry.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        ScheduledSmsManager.removeEntry(context, prefs, entry.id)
                                        refreshKey++
                                        Toast.makeText(context, "Scheduled message cancelled", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cancel")
                                }

                                Spacer(Modifier.width(4.dp))

                                TextButton(
                                    onClick = { editingEntry = entry }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }

                                Spacer(Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        ScheduledSmsManager.removeEntry(context, prefs, entry.id)
                                        smsVM.sendMessage(entry.address, entry.body, entry.subId) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        refreshKey++
                                    },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Send now")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingEntry != null) {
        val currentEditing = editingEntry!!
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Edit Scheduled Message",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editedBody,
                        onValueChange = { editedBody = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Scheduled for",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val formattedDateTime = remember(editedTime) {
                                SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(editedTime))
                            }
                            Text(
                                text = formattedDateTime,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showEditDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Change date", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { showEditTimePicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Change time", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedBody.isBlank()) {
                            Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editedTime <= System.currentTimeMillis()) {
                            Toast.makeText(context, "Scheduled time must be in the future", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val updated = currentEditing.copy(
                            body = editedBody.trim(),
                            scheduledTime = editedTime
                        )
                        ScheduledSmsManager.updateEntry(context, prefs, updated)
                        Toast.makeText(context, "Scheduled message updated", Toast.LENGTH_SHORT).show()
                        editingEntry = null
                        refreshKey++
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMillis = datePickerState.selectedDateMillis
                        if (selectedUtcMillis != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtcMillis
                            }
                            val currentCal = Calendar.getInstance().apply {
                                timeInMillis = if (editedTime > 0) editedTime else System.currentTimeMillis()
                            }
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            editedTime = newCal.timeInMillis
                        }
                        showEditDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDatePicker = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    if (showEditTimePicker) {
        AlertDialog(
            onDismissRequest = { showEditTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentCal = Calendar.getInstance().apply {
                            timeInMillis = if (editedTime > 0) editedTime else System.currentTimeMillis()
                        }
                        val newCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, currentCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        editedTime = newCal.timeInMillis
                        showEditTimePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
