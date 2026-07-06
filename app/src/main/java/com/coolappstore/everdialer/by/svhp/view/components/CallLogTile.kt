package com.coolappstore.everdialer.by.svhp.view.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.formatDate
import com.coolappstore.everdialer.by.svhp.controller.util.formatTimeOnly
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.view.screen.settings.AddMode
import com.coolappstore.everdialer.by.svhp.view.screen.settings.FakeCallAddSheet
import org.koin.compose.koinInject

/**
 * A small colored circular badge that shows the SIM slot number (1 or 2) so the SIM can be
 * identified at a glance by color AND number, instead of relying on color alone.
 */
@Composable
fun SimSlotBadge(slot: Int, modifier: Modifier = Modifier) {
    val color = if (slot == 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    Box(
        modifier = modifier
            .size(16.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (slot == 0) "1" else "2",
            color = Color.White,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CallLogTileSimple(log: CallLogEntry) {
    val icon = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE   -> Icons.AutoMirrored.Filled.CallMissed
        else                        -> Icons.Default.Call
    }
    RivoListItem(
        headline = when (log.type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE   -> "Missed"
            else                        -> "Call"
        },
        supporting = "${formatDate(log.date)}${if (log.duration > 0) " • ${android.text.format.DateUtils.formatElapsedTime(log.duration)}" else ""}",
        leadingIcon = icon,
        onClick = { }
    )
}

@Composable
fun CallLogTile(
    log: CallLogEntry,
    onTileClick: (CallLogEntry) -> Unit,
    onButtonClick: (CallLogEntry) -> Unit,
    onAvatarClick: ((CallLogEntry) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onSelectToggle: ((CallLogEntry) -> Unit)? = null,
    onSelectMode: ((CallLogEntry) -> Unit)? = null
) {
    val context   = LocalContext.current
    val isContact = log.name != null && log.name != log.number
    var showMenu  by remember { mutableStateOf(false) }

    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val fakeCallInContextMenu = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
    }
    var showFakeCallSheet by remember { mutableStateOf(false) }

    // Contacts Hider: mask name if enabled and this contact is hidden
    val hideNames = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false) }
    val hiddenIds = remember(settingsVer) {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
    }
    val contactsRepo = koinInject<IContactsRepository>()
    val isHiddenContact = remember(log.number, hiddenIds, hideNames) {
        if (!hideNames || hiddenIds.isEmpty() || log.number.isBlank()) false
        else {
            val c = try { contactsRepo.getContactByNumber(log.number) } catch (_: Exception) { null }
            c != null && c.id in hiddenIds
        }
    }
    val displayName = if (isHiddenContact) log.number else (log.name ?: log.number)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
            exit  = fadeOut(tween(300)) + shrinkHorizontally(tween(300))
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle?.invoke(log) },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Box(modifier = Modifier.weight(1f)) {
        val showSimBadge = remember { com.coolappstore.everdialer.by.svhp.controller.util.hasDualSim(context) } && log.simSlot in 0..1
        RivoListItem(
            headline = buildString {
                append(displayName)
                if (log.count > 1) append(" (${log.count})")
            },
            supporting = buildString {
                if (!isHiddenContact && log.name != null && log.name != log.number) append(log.number)
            },
            avatarName  = displayName,
            photoUri    = log.photoUri,
            trailingStartContent = if (showSimBadge) ({
                SimSlotBadge(
                    slot = log.simSlot,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }) else null,
            trailingText = formatTimeOnly(log.date),
            trailingIcon = when (log.type) {
                CallLog.Calls.MISSED_TYPE   -> Icons.AutoMirrored.Filled.CallMissed
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                else                        -> Icons.Default.Call
            },
            onAvatarClick = if (onAvatarClick != null) ({ onAvatarClick(log) }) else null,
            onLongClick = {
                if (selectionMode) onSelectToggle?.invoke(log)
                else showMenu = true
            },
            isMenuOpen  = showMenu && !selectionMode,
            onClick     = {
                if (selectionMode) onSelectToggle?.invoke(log)
                else onTileClick(log)
            }
        )

        // Respect Settings → Appearance → "Context Menu Elements" customization (show/hide + order)
        val callLogContextMenuKeys = remember(settingsVer, isContact, fakeCallInContextMenu) {
            com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.resolvedKeys(
                prefs,
                com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.SECTION_CALL_LOGS,
                listOf("select", "call_back", "copy_number", "add_to_contacts", "block_number", "fake_call", "delete_call_log")
            ).filter { key ->
                when (key) {
                    "add_to_contacts" -> !isContact
                    "fake_call" -> fakeCallInContextMenu
                    else -> true
                }
            }
        }

        RivoDropdownMenu(
            expanded          = showMenu,
            onDismissRequest  = { showMenu = false }
        ) {
            callLogContextMenuKeys.forEachIndexed { index, key ->
                if (key == "delete_call_log" && index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                when (key) {
                    "select" -> RivoDropdownMenuItem(
                        text     = "Select",
                        icon     = Icons.Default.CheckBox,
                        iconTint = Color(0xFF9C27B0),
                        onClick  = {
                            showMenu = false
                            onSelectMode?.invoke(log)
                        }
                    )
                    "call_back" -> RivoDropdownMenuItem(
                        text     = "Call back",
                        icon     = Icons.Default.Call,
                        iconTint = Color(0xFF4CAF50),
                        onClick  = { showMenu = false; onButtonClick(log) }
                    )
                    "copy_number" -> RivoDropdownMenuItem(
                        text     = "Copy number",
                        icon     = Icons.Default.ContentCopy,
                        iconTint = Color(0xFF2196F3),
                        onClick  = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", log.number))
                            Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    "add_to_contacts" -> RivoDropdownMenuItem(
                        text     = "Add to contacts",
                        icon     = Icons.Default.PersonAdd,
                        iconTint = Color(0xFF9C27B0),
                        onClick  = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                putExtra(ContactsContract.Intents.Insert.PHONE, log.number)
                            }
                            context.startActivity(intent)
                        }
                    )
                    "block_number" -> RivoDropdownMenuItem(
                        text     = "Block number",
                        icon     = Icons.Default.Block,
                        iconTint = Color(0xFFFF9800),
                        onClick  = {
                            showMenu = false
                            Toast.makeText(context, "Number blocked", Toast.LENGTH_SHORT).show()
                        }
                    )
                    "fake_call" -> RivoDropdownMenuItem(
                        text     = "Fake Call",
                        icon     = Icons.Outlined.PhoneCallback,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick  = {
                            showMenu = false
                            showFakeCallSheet = true
                        }
                    )
                    "delete_call_log" -> RivoDropdownMenuItem(
                        text          = "Delete from call log",
                        icon          = Icons.Default.Delete,
                        isDestructive = true,
                        onClick       = {
                            showMenu = false
                            try {
                                // Delete only this specific call log entry by its exact timestamp
                                context.contentResolver.delete(
                                    CallLog.Calls.CONTENT_URI,
                                    "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
                                    arrayOf(log.number, log.date.toString())
                                )
                                onDelete?.invoke()
                                Toast.makeText(context, "Deleted from call log", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not delete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                if (key == "select" && index < callLogContextMenuKeys.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        }
    }

    if (showFakeCallSheet) {
        FakeCallAddSheet(
            mode = AddMode.Number,
            initialNumber = log.number,
            initialDisplayName = log.name ?: log.number,
            onDismiss = { showFakeCallSheet = false },
            onSave = { entry, exactTriggerOverride ->
                FakeCallManager.addEntry(context, prefs, entry, exactTriggerOverride)
                showFakeCallSheet = false
            }
        )
    }
}
