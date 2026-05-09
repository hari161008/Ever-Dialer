package com.coolappstore.everdialer.by.svhp.view.components.tiles

import android.provider.CallLog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.util.formatDate
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry

@Composable
fun CallLogTile(
    log: CallLogEntry,
    modifier: Modifier = Modifier,
    onCallClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isMissed = log.types.any { it == CallLog.Calls.MISSED_TYPE } || (log.types.isEmpty() && log.type == CallLog.Calls.MISSED_TYPE)

    SingleTile(
        title = log.name?.ifEmpty { log.number } ?: log.number.ifEmpty { "Unknown" },
        photoUri = log.photoUri,
        isMissedCall = isMissed,
        modifier = modifier,
        titleTrailing = if (log.isCallerIdName) {
            {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Caller ID",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else null,
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val displayTypes = if (log.types.isNotEmpty()) log.types else listOf(log.type)
                val iconsToShow = displayTypes.take(4)
                val remaining = displayTypes.size - iconsToShow.size

                iconsToShow.forEach { type ->
                    val icon = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                        else -> Icons.Rounded.Call
                    }
                    val tint = if (type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                }

                if (remaining > 0) {
                    Text(text = "+$remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(4.dp))
                Text(text = formatDate(log.date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = {
            IconButton(onClick = onCallClick, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Rounded.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
            }
        },
        onClick = onClick
    )
}
