package com.coolappstore.everdialer.by.svhp.view.screen.settings.ringhaptic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.haptics.CustomHapticSequence
import com.coolappstore.everdialer.by.svhp.haptics.HapticPattern
import com.coolappstore.everdialer.by.svhp.haptics.RingHapticManager
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalIcons
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PetalMaterialShapes
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AutoHapticsSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CustomHapticEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun RingHapticScreen(
    navigator: DestinationsNavigator,
) {
    val ctx = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var ringHapticEnabled by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_ENABLED, false))
    }
    var pattern by remember(settingsVer) {
        mutableStateOf(HapticPattern.fromStorageKey(prefs.getString(PreferenceManager.KEY_RING_HAPTIC_PATTERN, HapticPattern.HEAVY_CLICK.name)))
    }
    var intensity by remember(settingsVer) {
        mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_INTENSITY, 1.0f))
    }
    var customSequence by remember(settingsVer) {
        mutableStateOf(prefs.getRingHapticCustomSequence())
    }
    var isAutoActive by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_AUTO, false))
    }
    var forceTouchRouting by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_FORCE_TOUCH_ROUTING, false))
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Ring Haptic",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Permission Card (only shown when READ_PHONE_STATE is not granted)
            PhonePermissionCard()

            // System Vibration Notice Card
            SystemHapticsNoticeCard()

            // Incoming Call Whole Container
            PetalGroup {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Call,
                        title = "Call Haptic",
                        subtitle = "Play a repeating haptic pattern when a call arrives",
                        checked = ringHapticEnabled,
                        onCheckedChange = {
                            ringHapticEnabled = it
                            prefs.setBoolean(PreferenceManager.KEY_RING_HAPTIC_ENABLED, it)
                        },
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie9Sided,
                    )
                }

                row { position ->
                    PetalContentRow(position = position, enabled = ringHapticEnabled) {
                        PatternSelectorWithCustom(
                            pattern = pattern,
                            intensity = intensity,
                            customSequence = customSequence,
                            isAutoActive = isAutoActive,
                            onPatternSelected = { p ->
                                pattern = p
                                prefs.setString(PreferenceManager.KEY_RING_HAPTIC_PATTERN, p.name)
                                RingHapticManager.playSinglePattern(ctx, p, intensity)
                            },
                            onIntensityCommit = { i ->
                                intensity = i
                                prefs.setFloat(PreferenceManager.KEY_RING_HAPTIC_INTENSITY, i)
                            },
                            onOpenCustomEditor = {
                                navigator.navigate(CustomHapticEditorScreenDestination)
                            },
                            onOpenAutoSettings = {
                                prefs.setBoolean(PreferenceManager.KEY_RING_HAPTIC_AUTO, true)
                                prefs.setRingHapticCustomSequence(CustomHapticSequence())
                                navigator.navigate(AutoHapticsSettingsScreenDestination)
                            },
                            onClearCustom = {
                                customSequence = CustomHapticSequence()
                                prefs.setRingHapticCustomSequence(customSequence)
                            },
                            onDisableAuto = {
                                isAutoActive = false
                                prefs.setBoolean(PreferenceManager.KEY_RING_HAPTIC_AUTO, false)
                            },
                        )
                    }
                }

                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.Vibration,
                        title = "Force Touch Engine",
                        subtitle = "Bypass system ring vibration restrictions by routing haptics through the touch feedback engine",
                        checked = forceTouchRouting,
                        onCheckedChange = {
                            forceTouchRouting = it
                            prefs.setBoolean(PreferenceManager.KEY_RING_HAPTIC_FORCE_TOUCH_ROUTING, it)
                        },
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie9Sided,
                        enabled = ringHapticEnabled,
                    )
                }

                row { position ->
                    PetalTestRow(
                        label = "Test Call Haptic",
                        onClick = {
                            RingHapticManager.testCallHaptic(ctx)
                        },
                        position = position,
                        enabled = ringHapticEnabled,
                    )
                }
            }
        }
    }
}
