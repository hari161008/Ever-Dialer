package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
private val ColorOrange = Color(0xFFFF9800)
private val ColorPink   = Color(0xFFE91E63)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorPurple = Color(0xFF9C27B0)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SoundVibrationScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var dtmfTone by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, true)) }
    var dialpadVibration by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_VIBRATION, true)) }
    var appHapticsEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) }
    var appHapticsStrength by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "strong") ?: "strong") }

    var visible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "soundAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    fun triggerTestVibration(strength: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                val vibrator = vm?.defaultVibrator
                val effect = if (strength == "strong")
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                else
                    VibrationEffect.createOneShot(40, 80)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Vibrator::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (strength == "strong")
                        VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                    else
                        VibrationEffect.createOneShot(40, 80)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(if (strength == "strong") 80L else 40L)
                }
            }
        } catch (_: Exception) {}
    }

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
            // ── Haptics across the app ──────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Haptics across the app",
                            supporting = "Vibrate on taps and interactions",
                            leadingIcon = Icons.Outlined.Vibration,
                            iconContainerColor = ColorPurple,
                            checked = appHapticsEnabled,
                            onCheckedChange = {
                                appHapticsEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_APP_HAPTICS, it)
                            }
                        )
                        if (appHapticsEnabled) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Strength",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                // Strong pill
                                Surface(
                                    onClick = {
                                        appHapticsStrength = "strong"
                                        prefs.setString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "strong")
                                        triggerTestVibration("strong")
                                    },
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (appHapticsStrength == "strong")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        "Strong",
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (appHapticsStrength == "strong") FontWeight.Bold else FontWeight.Normal,
                                        color = if (appHapticsStrength == "strong")
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Light pill
                                Surface(
                                    onClick = {
                                        appHapticsStrength = "light"
                                        prefs.setString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light")
                                        triggerTestVibration("light")
                                    },
                                    shape = RoundedCornerShape(50.dp),
                                    color = if (appHapticsStrength == "light")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        "Light",
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (appHapticsStrength == "light") FontWeight.Bold else FontWeight.Normal,
                                        color = if (appHapticsStrength == "light")
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Dialpad ─────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 80L) {
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
