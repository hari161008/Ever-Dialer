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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.formatDate
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry

@Composable
fun CallLogTileSimple(log: CallLogEntry) {
    val icon = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
        else -> Icons.Default.Call
    }
    RivoListItem(
        headline = when (log.type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE -> "Missed"
            else -> "Call"
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
    val context = LocalContext.current
    val isContact = log.name != null && log.name != log.number
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        RivoListItem(
            headline = buildString {
                append(log.name ?: log.number)
                if (log.count > 1) append(" (${log.count})")
            },
            supporting = buildString {
                if (log.name != null && log.name != log.number) append(log.number)
            },
            avatarName = log.name ?: log.number,
            photoUri = log.photoUri,
            trailingIcon = when (log.type) {
                CallLog.Calls.MISSED_TYPE   -> Icons.AutoMirrored.Filled.CallMissed
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                else                        -> Icons.Default.Call
            },
            onLongClick = { showMenu = true },
            onClick = { onTileClick(log) }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Call back") },
                leadingIcon = { Icon(Icons.Default.Call, null) },
                onClick = { showMenu = false; onButtonClick(log) }
            )
            DropdownMenuItem(
                text = { Text("Copy number") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = {
                    showMenu = false
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", log.number))
                    Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                }
            )
            if (!isContact) {
                DropdownMenuItem(
                    text = { Text("Add to contacts") },
                    leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                    onClick = {
                        showMenu = false
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, log.number)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Block number") },
                leadingIcon = { Icon(Icons.Default.Block, null) },
                onClick = {
                    showMenu = false
                    Toast.makeText(context, "Number blocked", Toast.LENGTH_SHORT).show()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete from call log", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
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
