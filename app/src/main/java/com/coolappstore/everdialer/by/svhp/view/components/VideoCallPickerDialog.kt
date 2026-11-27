package com.coolappstore.everdialer.by.svhp.view.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.VideoAppOption
import com.coolappstore.everdialer.by.svhp.controller.util.getAvailableVideoApps
import com.coolappstore.everdialer.by.svhp.controller.util.launchVideoCall
import org.koin.compose.koinInject

/**
 * Lets the user pick which app a video call should go through (carrier, WhatsApp, Meet, Telegram,
 * Signal, Viber, Skype, WeChat, LINE, imo, Zoom). "Remember my choice" saves the pick so later
 * video calls skip this dialog entirely; the saved route can be changed again from the same
 * dialog by long-pressing the contact's Video button.
 */
@Composable
fun VideoCallPickerDialog(
    options: List<VideoAppOption>,
    recipientName: String?,
    recipientNumber: String,
    onDismissRequest: () -> Unit,
    onSelectOption: (VideoAppOption, Boolean) -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoCall,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Video call",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val subtitle = if (!recipientName.isNullOrBlank() && recipientName != recipientNumber) {
                            "$recipientName ($recipientNumber)"
                        } else {
                            recipientNumber
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(options, key = { it.id }) { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectOption(option, rememberChoice) },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = option.icon
                                    if (icon != null) {
                                        Image(
                                            bitmap = icon,
                                            contentDescription = option.name,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.VideoCall,
                                            contentDescription = option.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { rememberChoice = !rememberChoice }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Always use this app for video calls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismissRequest() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** Handle returned by [rememberVideoCallLauncher] — same shape as the app's other launchers. */
class VideoCallLauncher(private val onStart: (String, String?, Boolean) -> Unit) {
    /** Starts a video call to [phoneNumber], using the remembered app when there is one. */
    fun startVideoCall(phoneNumber: String, recipientName: String? = null) =
        onStart(phoneNumber, recipientName, false)

    /** Always shows the app picker, ignoring (but not clearing) the remembered choice. */
    fun chooseVideoApp(phoneNumber: String, recipientName: String? = null) =
        onStart(phoneNumber, recipientName, true)
}

/**
 * Composable-friendly entry point for video calling. Uses the remembered app when one is set,
 * launches straight away when the carrier route is the only option, and otherwise shows
 * [VideoCallPickerDialog] for this call.
 */
@Composable
fun rememberVideoCallLauncher(): VideoCallLauncher {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()

    var showPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var pendingName by remember { mutableStateOf<String?>(null) }
    var options by remember { mutableStateOf<List<VideoAppOption>>(emptyList()) }

    val launcher = remember {
        VideoCallLauncher { number, name, forcePicker ->
            val cleanNumber = number.trim()
            if (cleanNumber.isEmpty()) {
                Toast.makeText(context, "No number to call", Toast.LENGTH_SHORT).show()
                return@VideoCallLauncher
            }

            val available = getAvailableVideoApps(context)
            val remembered = prefs.getString(PreferenceManager.KEY_DEFAULT_VIDEO_APP, null)

            if (!forcePicker && !remembered.isNullOrBlank() && available.any { it.id == remembered }) {
                launchVideoCall(context, remembered, cleanNumber)
                return@VideoCallLauncher
            }

            // Only the carrier route is available — nothing to choose between.
            if (available.size <= 1) {
                launchVideoCall(context, available.firstOrNull()?.id ?: "system", cleanNumber)
                return@VideoCallLauncher
            }

            pendingNumber = cleanNumber
            pendingName = name
            options = available
            showPicker = true
        }
    }

    if (showPicker && options.isNotEmpty()) {
        VideoCallPickerDialog(
            options = options,
            recipientName = pendingName,
            recipientNumber = pendingNumber,
            onDismissRequest = { showPicker = false },
            onSelectOption = { option, remember ->
                if (remember) {
                    prefs.setString(PreferenceManager.KEY_DEFAULT_VIDEO_APP, option.id)
                }
                showPicker = false
                launchVideoCall(context, option.id, pendingNumber)
            }
        )
    }

    return launcher
}
