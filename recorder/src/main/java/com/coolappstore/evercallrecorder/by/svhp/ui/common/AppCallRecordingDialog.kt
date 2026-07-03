/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolappstore.evercallrecorder.by.svhp.R
import com.coolappstore.evercallrecorder.by.svhp.system.openNotificationListenerSettings
import com.coolappstore.evercallrecorder.by.svhp.system.permissions.PermissionChecks
import com.coolappstore.evercallrecorder.by.svhp.ui.theme.ShizucallrecorderTheme

/**
 * Floating popup for the "Record calls from apps" setting.
 *
 * Lets the user opt in to automatically recording voice/video calls placed through WhatsApp and/or
 * Telegram, the same way normal phone calls are recorded. Both checkboxes are unticked by default; ticking
 * one writes straight to preferences (no separate Save step), exactly like the rest of the switches in
 * Settings.
 *
 * Detection (see [com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallNotificationListenerService])
 * needs the system "Notification access" permission, so this dialog surfaces that requirement and links
 * straight to the system screen to grant it when it's missing — re-checking automatically when the user
 * comes back to the app.
 *
 * @param whatsAppEnabled  Current WhatsApp checkbox state.
 * @param telegramEnabled  Current Telegram checkbox state.
 * @param onWhatsAppToggle Called with the new boolean when the WhatsApp checkbox is tapped.
 * @param onTelegramToggle Called with the new boolean when the Telegram checkbox is tapped.
 * @param onDismiss        Called when the user closes the popup.
 */
@Composable
fun AppCallRecordingDialog(
    whatsAppEnabled: Boolean,
    telegramEnabled: Boolean,
    onWhatsAppToggle: (Boolean) -> Unit,
    onTelegramToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationAccess by remember { mutableStateOf(PermissionChecks.hasNotificationListenerPermission(context)) }

    // Re-check when the user comes back from the system "Notification access" screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = PermissionChecks.hasNotificationListenerPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(stringResource(R.string.settings_app_call_recording_dialog_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_app_call_recording_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasNotificationAccess) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.settings_app_call_recording_access_granted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    text = stringResource(R.string.settings_app_call_recording_permission_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_app_call_recording_permission_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { context.openNotificationListenerSettings() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                                ) {
                                    Text(stringResource(R.string.settings_app_call_recording_grant_access))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                AppCallTargetCheckboxRow(
                    label = stringResource(R.string.app_call_target_whatsapp),
                    checked = whatsAppEnabled,
                    onCheckedChange = onWhatsAppToggle
                )
                AppCallTargetCheckboxRow(
                    label = stringResource(R.string.app_call_target_telegram),
                    checked = telegramEnabled,
                    onCheckedChange = onTelegramToggle
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_close)) }
        }
    )
}

/** A single tappable "app + checkbox" row, e.g. the WhatsApp or Telegram row inside [AppCallRecordingDialog]. */
@Composable
private fun AppCallTargetCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppCallRecordingDialogPreview() {
    ShizucallrecorderTheme {
        AppCallRecordingDialog(
            whatsAppEnabled = false,
            telegramEnabled = true,
            onWhatsAppToggle = {},
            onTelegramToggle = {},
            onDismiss = {}
        )
    }
}
