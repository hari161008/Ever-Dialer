package com.grinch.rivo4.view.screen.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoAnimatedSection
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)
private val ColorPink   = Color(0xFFE91E63)
private val ColorBlue   = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SoundVibrationScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var dtmfTone by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, true)) }
    var dialpadVibration by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, true)) }

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
            item {
                RivoAnimatedSection(delayMs = 60L) {
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
                        RivoSwitchListItem(
                            headline = "Dialpad Vibration",
                            supporting = "Vibration that plays during keypress",
                            leadingIcon = Icons.Outlined.Vibration,
                            iconContainerColor = ColorOrange,
                            checked = dialpadVibration,
                            onCheckedChange = {
                                dialpadVibration = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, it)
                            }
                        )
                    }
                }
            }

            item {
                RivoAnimatedSection(delayMs = 160L) {
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
}
