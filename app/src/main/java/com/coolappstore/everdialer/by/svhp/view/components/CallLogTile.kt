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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.draw.clip
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
 * A small, clean SIM-card-chip badge — solid flat color with the slot number in bold, so the
 * number stays clearly legible at small sizes. Defaults to a rectangle with one clipped corner
 * (like a real SIM card); pass [shape] to use a plain rounded rect instead (used on the ongoing
 * call screen, where the badge is shown much smaller and the notch's diagonal edge doesn't
 * anti-alias as cleanly). Text is centered using a matched line height and no font padding
 * (rather than a fixed manual offset) so the digit stays perfectly centered at every size.
 */
private val SimCardNotchShape = GenericShape { size, _ ->
    val notch = size.minDimension * 0.42f
    moveTo(notch, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(0f, notch)
    close()
}

@Composable
fun SimSlotBadge(slot: Int, modifier: Modifier = Modifier, shape: Shape = SimCardNotchShape) {
    val color = if (slot == 0) Color(0xFF2E7D32) else Color(0xFFC62828)
    BoxWithConstraints(
        modifier = Modifier
            .size(width = 18.dp, height = 21.dp) // default size — callers can override via `modifier`
            .then(modifier)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        val fontSize = (maxHeight.value * 0.52f).sp
        Text(
            text = if (slot == 0) "1" else "2",
            color = Color.White,
            fontSize = fontSize,
            lineHeight = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

/**
 * Best-effort "national" digits of a phone number with any country code stripped, used so the
 * avatar for an unsaved number shows that number's actual first digit (and varies in color by
 * number) instead of a generic fixed "Unknown" placeholder. There's no phone-number library in
 * this project to parse country codes properly, so this uses the common heuristic that national
 * numbers are 10 digits long (true for India, the US/Canada, and many other countries) and
 * treats anything beyond the last 10 digits as the country code.
 */
private fun nationalNumberDigits(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

@Composable
fun CallLogTileSimple(log: CallLogEntry) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val use24HourTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }

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
        supporting = "${formatDate(log.date, use24HourTime)}${if (log.duration > 0) " • ${android.text.format.DateUtils.formatElapsedTime(log.duration)}" else ""}",
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
    val use24HourTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }
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
    val displayName = when {
        isHiddenContact -> log.number
        isContact -> log.name!!
        else -> "Unknown"
    }
    // Headline shows "Unknown" for unsaved numbers, but the avatar should still look like a real
    // per-number identity — first digit of the actual number (country code stripped) and a color
    // that varies by number — rather than every unsaved caller getting an identical grey/green "U".
    val avatarSourceName = when {
        isHiddenContact -> log.number
        isContact -> log.name!!
        else -> nationalNumberDigits(log.number).ifEmpty { "Unknown" }
    }

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
        val showSimsSetting = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault()) }
        val showSimBadge = showSimsSetting && log.simSlot in 0..1
        // The number shows on the *supporting* line under the name/"Unknown" headline, unless
        // hidden-name masking already put the number on the headline itself. Put the badge to
        // the right of whichever line is actually showing the number.
        val numberOnSupportingLine = !isHiddenContact
        val simBadge: (@Composable () -> Unit)? = if (showSimBadge) ({ SimSlotBadge(slot = log.simSlot) }) else null
        RivoListItem(
            headline = buildString {
                append(displayName)
                if (log.count > 1) append(" (${log.count})")
            },
            supporting = buildString {
                if (!isHiddenContact) append(log.number)
            },
            avatarName  = avatarSourceName,
            photoUri    = log.photoUri,
            headlineEndContent = if (!numberOnSupportingLine) simBadge else null,
            supportingEndContent = if (numberOnSupportingLine) simBadge else null,
            trailingText = formatTimeOnly(log.date, use24HourTime),
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
