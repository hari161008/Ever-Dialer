package com.coolappstore.everdialer.by.svhp.view.screen.settings.ringhaptic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.haptics.CustomHapticSequence
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.performScrollHaptic
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalIcons
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalMaterialShapes
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun AutoHapticsSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val ctx = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var isEnabled by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_AUTO, false))
    }
    val sensitivity by remember(settingsVer) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_AUTO_SENSITIVITY, 0.65f))
    }
    val strength by remember(settingsVer) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_AUTO_STRENGTH, 0.8f))
    }

    var sensitivityDraft by remember(sensitivity) { mutableFloatStateOf(sensitivity) }
    var sensitivityTick by remember { mutableIntStateOf(slider01ToTickIndex(sensitivity)) }
    var strengthDraft by remember(strength) { mutableFloatStateOf(strength) }
    var strengthTick by remember { mutableIntStateOf(slider01ToTickIndex(strength)) }

    val featureColor = MaterialTheme.colorScheme.tertiary

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Auto Call Haptics",
                onBackClick = { navigator.navigateUp() },
            )
        },
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Hero
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = featureColor.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(PetalMaterialShapes.Cookie12Sided.toShape())
                            .background(featureColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = "Vibrates in sync with your ringtone beats in real time — even on silent or vibrate-only mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Enable toggle
            PetalGroup {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Enable Auto Beat Haptics",
                        subtitle = "Vibrate with ringtone audio regardless of ring mode",
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            prefs.setBoolean(PreferenceManager.KEY_RING_HAPTIC_AUTO, it)
                            if (it) {
                                prefs.setRingHapticCustomSequence(CustomHapticSequence())
                            }
                        },
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie12Sided,
                    )
                }
            }

            // Sliders
            PetalGroup {
                row { position ->
                    Surface(
                        shape = petalGroupShape(position),
                        color = petalGroupColor(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Beat Sensitivity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                                    Text(
                                        "${(sensitivityDraft * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text("How aggressively beats are detected from ringtone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PetalSlider(
                                value = sensitivityDraft,
                                onValueChange = { v ->
                                    sensitivityDraft = v
                                    val t = slider01ToTickIndex(v)
                                    if (t != sensitivityTick) {
                                        sensitivityTick = t
                                        performScrollHaptic(ctx, 40)
                                    }
                                },
                                onValueChangeFinished = {
                                    prefs.setFloat(PreferenceManager.KEY_RING_HAPTIC_AUTO_SENSITIVITY, sensitivityDraft)
                                },
                                enabled = isEnabled,
                                steps = SliderTickStepsDefault,
                            )
                        }
                    }
                }
                row { position ->
                    Surface(
                        shape = petalGroupShape(position),
                        color = petalGroupColor(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Haptic Strength", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                                    Text(
                                        "${(strengthDraft * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text("Intensity of each vibration pulse", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PetalSlider(
                                value = strengthDraft,
                                onValueChange = { v ->
                                    strengthDraft = v
                                    val t = slider01ToTickIndex(v)
                                    if (t != strengthTick) {
                                        strengthTick = t
                                        performScrollHaptic(ctx, 40)
                                    }
                                },
                                onValueChangeFinished = {
                                    prefs.setFloat(PreferenceManager.KEY_RING_HAPTIC_AUTO_STRENGTH, strengthDraft)
                                },
                                enabled = isEnabled,
                                steps = SliderTickStepsDefault,
                            )
                        }
                    }
                }
            }

            // Info
            Surface(
                color = featureColor.copy(alpha = 0.12f),
                shape = PetalContainmentDefaults.shape,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(PetalIcons.Info, null, tint = featureColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("How Auto Beat Haptics works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Uses Android's audio Visualizer API to analyze real-time audio output and detect beat patterns. Vibrations trigger on each beat — even when your phone is on silent or vibrate-only mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
