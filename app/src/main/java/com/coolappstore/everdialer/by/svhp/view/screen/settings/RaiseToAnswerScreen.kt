package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.coolappstore.everdialer.by.svhp.controller.RaiseToAnswerManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorTeal   = Color(0xFF009688)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorOrange = Color(0xFFFF9800)
private val ColorPurple = Color(0xFF9C27B0)
private val ColorPink   = Color(0xFFE91E63)

/**
 * Full configuration screen for the Raise to Answer feature, ported from the open-source
 * "Raise to Answer" app and adapted to drive Ever Dialer's own call control instead of
 * relying on a separate companion app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun RaiseToAnswerScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    val isSupported = remember { RaiseToAnswerManager.hasRequiredSensors(context) }
    val hasMagnetometer = remember { RaiseToAnswerManager.hasMagnetometer(context) }

    var enabled by remember { mutableStateOf(isSupported && prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, false)) }
    var anyAngle by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ANY_ANGLE, false)) }
    var declineByFlip by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_DECLINE_FLIP, false)) }
    var beepFeedback by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_BEEP, true)) }
    var vibrateFeedback by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_VIBRATE, false)) }

    var visible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "raiseToAnswerAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Raise to Answer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .alpha(screenAlpha)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)

            if (!isSupported) {
                RivoAnimatedSection(delayMs = 0L) {
                        RivoExpressiveCard {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "This device is missing the proximity or motion sensor required for Raise to Answer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                }
            }

            // ── Main toggle ─────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        RaiseToAnswerSectionLabel("Raise to Answer")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Enable Raise to Answer",
                                supporting = "Automatically answer incoming calls by raising the phone to your ear",
                                leadingIcon = Icons.Outlined.Vibration,
                                iconContainerColor = ColorTeal,
                                checked = enabled,
                                modifier = Modifier.settingsSearchHighlight("enable_raise_to_answer", highlightedKey) { highlightedKey = null },
                                onCheckedChange = {
                                    if (isSupported) {
                                        enabled = it
                                        prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, it)
                                    }
                                }
                            )
                        }
                    }
            }

            // ── Detection behaviour ─────────────────────────────────────
            AnimatedVisibility(
                    visible = enabled && isSupported,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    RivoAnimatedSection(delayMs = 60L) {
                        Column {
                            RaiseToAnswerSectionLabel("Detection")
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Answer at Any Angle",
                                    supporting = if (hasMagnetometer)
                                        "Use a simpler detection mode that doesn't require holding the phone upright"
                                    else
                                        "Always on — this device has no compass sensor for precise detection",
                                    leadingIcon = Icons.Outlined.ScreenRotation,
                                    iconContainerColor = ColorBlue,
                                    checked = anyAngle || !hasMagnetometer,
                                    modifier = Modifier.settingsSearchHighlight("answer_any_angle", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        if (hasMagnetometer) {
                                            anyAngle = it
                                            prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ANY_ANGLE, it)
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Decline by Flipping",
                                    supporting = "Flip the phone face-down to decline an incoming call",
                                    leadingIcon = Icons.Outlined.FlipCameraAndroid,
                                    iconContainerColor = ColorOrange,
                                    checked = declineByFlip,
                                    modifier = Modifier.settingsSearchHighlight("decline_by_flipping", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        declineByFlip = it
                                        prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_DECLINE_FLIP, it)
                                    }
                                )
                            }
                        }
                    }
            }

            // ── Feedback ─────────────────────────────────────────────────
            AnimatedVisibility(
                    visible = enabled && isSupported,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    RivoAnimatedSection(delayMs = 120L) {
                        Column {
                            RaiseToAnswerSectionLabel("Feedback")
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Beep Feedback",
                                    supporting = "Play a short tone while sensors are tracking the gesture",
                                    leadingIcon = Icons.Outlined.VolumeUp,
                                    iconContainerColor = ColorPurple,
                                    checked = beepFeedback,
                                    modifier = Modifier.settingsSearchHighlight("raise_beep_feedback", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        beepFeedback = it
                                        prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_BEEP, it)
                                    }
                                )
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Vibrate Feedback",
                                    supporting = "Vibrate briefly while sensors are tracking the gesture",
                                    leadingIcon = Icons.Outlined.Vibration,
                                    iconContainerColor = ColorPink,
                                    checked = vibrateFeedback,
                                    modifier = Modifier.settingsSearchHighlight("raise_vibrate_feedback", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        vibrateFeedback = it
                                        prefs.setBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_VIBRATE, it)
                                    }
                                )
                            }
                        }
                    }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RaiseToAnswerSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}
