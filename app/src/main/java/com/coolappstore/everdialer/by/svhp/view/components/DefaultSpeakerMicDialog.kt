package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager

@Composable
private fun AudioChoiceRow(
    icon: ImageVector,
    label: String,
    subLabel: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val prefs = org.koin.compose.koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val circleIcons = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CIRCLE_ICONS, false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = if (circleIcons) CircleShape else RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                if (subLabel != null) {
                    Text(
                        subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultSpeakerMicDialog(
    prefs: PreferenceManager,
    onDismiss: () -> Unit
) {
    var selectedSpeaker by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_DEFAULT_SPEAKER_ROUTE, PreferenceManager.SPEAKER_ROUTE_EXTERNAL) ?: PreferenceManager.SPEAKER_ROUTE_EXTERNAL)
    }
    var usePreviousConfig by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_USE_PREVIOUS_CALL_AUDIO_CONFIG, false))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Default speaker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Configure default call audio behavior",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section: Speaker
                        Text(
                            text = "Default Speaker",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                        )

                        AudioChoiceRow(
                            icon = Icons.Default.Bluetooth,
                            label = "External speaker",
                            subLabel = "Uses connected Bluetooth or headset; falls back to ear speaker",
                            selected = selectedSpeaker == PreferenceManager.SPEAKER_ROUTE_EXTERNAL,
                            onClick = {
                                selectedSpeaker = PreferenceManager.SPEAKER_ROUTE_EXTERNAL
                                prefs.setString(PreferenceManager.KEY_DEFAULT_SPEAKER_ROUTE, PreferenceManager.SPEAKER_ROUTE_EXTERNAL)
                            }
                        )

                        AudioChoiceRow(
                            icon = Icons.Default.PhoneInTalk,
                            label = "Ear speaker",
                            subLabel = "Uses phone earpiece",
                            selected = selectedSpeaker == PreferenceManager.SPEAKER_ROUTE_EAR,
                            onClick = {
                                selectedSpeaker = PreferenceManager.SPEAKER_ROUTE_EAR
                                prefs.setString(PreferenceManager.KEY_DEFAULT_SPEAKER_ROUTE, PreferenceManager.SPEAKER_ROUTE_EAR)
                            }
                        )

                        AudioChoiceRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Speaker",
                            subLabel = "Uses phone loudspeaker",
                            selected = selectedSpeaker == PreferenceManager.SPEAKER_ROUTE_SPEAKER,
                            onClick = {
                                selectedSpeaker = PreferenceManager.SPEAKER_ROUTE_SPEAKER
                                prefs.setString(PreferenceManager.KEY_DEFAULT_SPEAKER_ROUTE, PreferenceManager.SPEAKER_ROUTE_SPEAKER)
                            }
                        )

                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(6.dp))

                        // Section: Last used configuration
                        Surface(
                            onClick = {
                                val next = !usePreviousConfig
                                usePreviousConfig = next
                                prefs.setBoolean(PreferenceManager.KEY_USE_PREVIOUS_CALL_AUDIO_CONFIG, next)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (usePreviousConfig) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (usePreviousConfig) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = null,
                                            tint = if (usePreviousConfig) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Use previously used configuration",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (usePreviousConfig) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        "Automatically restore the speaker route used in your previous call",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = usePreviousConfig,
                                    onCheckedChange = {
                                        usePreviousConfig = it
                                        prefs.setBoolean(PreferenceManager.KEY_USE_PREVIOUS_CALL_AUDIO_CONFIG, it)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
