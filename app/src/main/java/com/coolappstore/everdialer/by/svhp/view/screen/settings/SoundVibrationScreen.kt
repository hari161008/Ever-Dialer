package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.DialpadToneStyle
import com.coolappstore.everdialer.by.svhp.controller.util.DialpadTonePlayer
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorGreen  = Color(0xFF4CAF50)
private val ColorPink   = Color(0xFFE91E63)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorPurple = Color(0xFF9C27B0)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SoundVibrationScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var dtmfTone by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, false)) }
    var dialpadToneStyle by remember {
        mutableStateOf(DialpadToneStyle.fromKey(prefs.getString(PreferenceManager.KEY_DIALPAD_TONE_STYLE, DialpadToneStyle.STANDARD.key)))
    }
    var showToneStyleDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "soundAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound & Vibration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .alpha(screenAlpha),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Dialpad ─────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "DTMF Tone",
                            supporting = "Dialpad tone that plays during keypress",
                            leadingIcon = Icons.Outlined.Audiotrack,
                            iconContainerColor = ColorGreen,
                            checked = dtmfTone,
                            onCheckedChange = {
                                dtmfTone = it
                                prefs.setBoolean(PreferenceManager.KEY_DTMF_TONE, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoListItem(
                            headline = "Dial Pad Tone",
                            supporting = "${dialpadToneStyle.label} — ${dialpadToneStyle.description}",
                            leadingIcon = toneStyleIcon(dialpadToneStyle),
                            iconContainerColor = ColorPurple,
                            onClick = { showToneStyleDialog = true }
                        )
                    }
                }
            }

            item {
                RivoAnimatedSection(delayMs = 80L) {
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Ringtone Settings",
                            supporting = "Open system sound settings",
                            leadingIcon = Icons.Outlined.MusicNote,
                            iconContainerColor = ColorPink,
                            onClick = { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoListItem(
                            headline = "Do Not Disturb",
                            supporting = "Manage interruption settings",
                            leadingIcon = Icons.Outlined.DoNotDisturb,
                            iconContainerColor = ColorBlue,
                            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // ── Dial Pad Tone Style Dialog ─────────────────────────────────────────
    if (showToneStyleDialog) {
        AlertDialog(
            onDismissRequest = { showToneStyleDialog = false },
            icon = { Icon(Icons.Outlined.Audiotrack, null, tint = ColorPurple) },
            title = { Text("Dial Pad Tone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose the sound that plays when you tap a dialpad key. Tap an option to preview it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    DialpadToneStyle.entries.forEach { style ->
                        val isSelected = dialpadToneStyle == style
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dialpadToneStyle = style
                                        prefs.setString(PreferenceManager.KEY_DIALPAD_TONE_STYLE, style.key)
                                        DialpadTonePlayer.play(context, "5", style)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = toneStyleIcon(style),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        style.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        style.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        dialpadToneStyle = style
                                        prefs.setString(PreferenceManager.KEY_DIALPAD_TONE_STYLE, style.key)
                                        DialpadTonePlayer.play(context, "5", style)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToneStyleDialog = false }) { Text("Done") }
            }
        )
    }
}

private fun toneStyleIcon(style: DialpadToneStyle) = when (style) {
    DialpadToneStyle.STANDARD -> Icons.Outlined.Audiotrack
    DialpadToneStyle.PIANO -> Icons.Outlined.MusicNote
    DialpadToneStyle.WATER_DROP -> Icons.Outlined.Opacity
    DialpadToneStyle.MECHANICAL -> Icons.Outlined.Keyboard
    DialpadToneStyle.SCIFI -> Icons.Outlined.VideogameAsset
}
