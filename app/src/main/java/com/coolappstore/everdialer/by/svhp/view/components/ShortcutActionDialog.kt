package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Second step of the "Add to Home Screen" flow (Contact Info screen): lets the person choose
 * what tapping the pinned shortcut should do — jump to this contact's info page, or place the
 * call immediately.
 */
@Composable
fun ShortcutActionDialog(
    onOpenContactInfo: () -> Unit,
    onCallDirectly: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Add to Home Screen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    ShortcutActionRow(
                        icon = Icons.Default.Person,
                        label = "Open contact info",
                        subLabel = "Shortcut opens this contact's info page",
                        onClick = onOpenContactInfo
                    )
                    ShortcutActionRow(
                        icon = Icons.Default.Call,
                        label = "Call directly",
                        subLabel = "Shortcut calls this contact right away",
                        onClick = onCallDirectly
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun ShortcutActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
