package com.coolappstore.everdialer.by.svhp.view.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.formatDate
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry

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
    onDelete: (() -> Unit)? = null
) {
    val context   = LocalContext.current
    val isContact = log.name != null && log.name != log.number
    var showMenu  by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        RivoListItem(
            headline = buildString {
                append(log.name ?: log.number)
                if (log.count > 1) append(" (${log.count})")
            },
            supporting = buildString {
                if (log.name != null && log.name != log.number) append(log.number)
            },
            avatarName  = log.name ?: log.number,
            photoUri    = log.photoUri,
            trailingIcon = when (log.type) {
                CallLog.Calls.MISSED_TYPE   -> Icons.AutoMirrored.Filled.CallMissed
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                else                        -> Icons.Default.Call
            },
            onLongClick = { showMenu = true },
            isMenuOpen  = showMenu,
            onClick     = { onTileClick(log) }
        )

        RivoDropdownMenu(
            expanded          = showMenu,
            onDismissRequest  = { showMenu = false }
        ) {
            RivoDropdownMenuItem(
                text     = "Call back",
                icon     = Icons.Default.Call,
                iconTint = Color(0xFF4CAF50),
                onClick  = { showMenu = false; onButtonClick(log) }
            )
            RivoDropdownMenuItem(
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
            if (!isContact) {
                RivoDropdownMenuItem(
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
            }
            RivoDropdownMenuItem(
                text     = "Block number",
                icon     = Icons.Default.Block,
                iconTint = Color(0xFFFF9800),
                onClick  = {
                    showMenu = false
                    Toast.makeText(context, "Number blocked", Toast.LENGTH_SHORT).show()
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            RivoDropdownMenuItem(
                text          = "Delete from call log",
                icon          = Icons.Default.Delete,
                isDestructive = true,
                onClick       = {
                    showMenu = false
                    try {
                        context.contentResolver.delete(
                            CallLog.Calls.CONTENT_URI,
                            "${CallLog.Calls.NUMBER} = ?",
                            arrayOf(log.number)
                        )
                        onDelete?.invoke()
                        Toast.makeText(context, "Deleted from call log", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not delete", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
