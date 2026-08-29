package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.coolappstore.everdialer.by.svhp.controller.VolumeDndAccessibilityService
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun RainModeScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var isAccessibilityGranted by remember {
        mutableStateOf(VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var enabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false)) }
    var vibrateFeedback by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, true)) }
    var timeoutMs by remember { mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_RAIN_MODE_TIMEOUT_MS, PreferenceManager.DEFAULT_RAIN_MODE_TIMEOUT_MS)) }
    var showDurationDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "rainModeAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Rain Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    SettingsBackIconButton(onClick = { navigator.navigateUp() })
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

            SettingsSearchEntryPoint(navigator = navigator)

            // ── Information & Overview Card ─────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                RivoExpressiveCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0288D1).copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Volume Key Sequence Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "When water or rain makes your touch screen unresponsive, press the volume buttons in sequence (Volume Up, Down, Up, Down) within ${timeoutMs / 1000}s between presses to answer an incoming call or decline/end an active call.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Accessibility Permission Warning (if needed) ────────────
            if (!isAccessibilityGranted) {
                RivoAnimatedSection(delayMs = 40L) {
                    RivoExpressiveCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    "Accessibility Service Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                "Rain Mode requires accessibility service permissions to detect hardware volume button sequence presses.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { VolumeDndAccessibilityService.openAccessibilitySettings(context) },
                                shape = RoundedCornerShape(100),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Grant Permission in Settings")
                            }
                        }
                    }
                }
            }

            // ── Main Toggle ─────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 80L) {
                Column {
                    RainModeSectionLabel("Rain Mode")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Enable Rain Mode",
                            supporting = "Answer ringing calls or decline/end active calls by pressing Volume Up, Down, Up, Down (UDUD)",
                            leadingIcon = Icons.Outlined.WaterDrop,
                            iconContainerColor = Color(0xFF0288D1),
                            checked = enabled,
                            modifier = Modifier.settingsSearchHighlight("enable_rain_mode", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                enabled = it
                                prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, it)
                            }
                        )
                    }
                }
            }

            // ── Behavior & Feedback Options ─────────────────────────────
            AnimatedVisibility(visible = enabled) {
                RivoAnimatedSection(delayMs = 120L) {
                    Column {
                        RainModeSectionLabel("Sequence & Feedback")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Required Button Sequence",
                                supporting = "Volume Up → Down → Up → Down (UDUD)",
                                leadingIcon = Icons.Outlined.Pin,
                                iconContainerColor = Color(0xFF009688),
                                onClick = {}
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Vibration Feedback",
                                supporting = "Vibrate when a call is answered or hung up via volume sequence",
                                leadingIcon = Icons.Outlined.Vibration,
                                iconContainerColor = Color(0xFF9C27B0),
                                checked = vibrateFeedback,
                                modifier = Modifier.settingsSearchHighlight("rain_mode_vibrate", highlightedKey) { highlightedKey = null },
                                onCheckedChange = {
                                    vibrateFeedback = it
                                    prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, it)
                                }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Sequence Timeout Delay",
                                supporting = "${timeoutMs / 1000} second${if (timeoutMs != 1000) "s" else ""} maximum delay between button presses",
                                leadingIcon = Icons.Outlined.Timer,
                                iconContainerColor = Color(0xFFFFB300),
                                modifier = Modifier.settingsSearchHighlight("rain_mode_timeout", highlightedKey) { highlightedKey = null },
                                onClick = { showDurationDialog = true }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDurationDialog) {
        val durationOptions = listOf(
            1000 to "1 sec",
            2000 to "2 sec (Default)",
            3000 to "3 sec",
            4000 to "4 sec"
        )
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Sequence Timeout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    durationOptions.forEach { (ms, label) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (timeoutMs == ms) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    timeoutMs = ms
                                    prefs.setInt(PreferenceManager.KEY_RAIN_MODE_TIMEOUT_MS, ms)
                                    showDurationDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = timeoutMs == ms,
                                    onClick = {
                                        timeoutMs = ms
                                        prefs.setInt(PreferenceManager.KEY_RAIN_MODE_TIMEOUT_MS, ms)
                                        showDurationDialog = false
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (timeoutMs == ms) FontWeight.Bold else FontWeight.Normal,
                                    color = if (timeoutMs == ms) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RainModeSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}
