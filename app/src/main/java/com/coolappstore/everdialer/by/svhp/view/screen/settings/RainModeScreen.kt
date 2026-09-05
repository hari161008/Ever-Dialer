package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.RainModeManager
import com.coolappstore.everdialer.by.svhp.controller.VolumeDndAccessibilityService
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun RainModeScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    val hasAccelerometer = remember { RainModeManager.hasRequiredSensors(context) }

    var enabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false)) }
    var vibrateFeedback by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, true)) }
    var shakeIntensity by remember {
        mutableFloatStateOf(
            prefs.getFloat(
                PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY,
                PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
            )
        )
    }
    var incomingAction by remember {
        mutableStateOf(
            prefs.getString(
                PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION,
                PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
            ) ?: PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
        )
    }
    var endActiveCall by remember {
        mutableStateOf(
            prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, true)
        )
    }

    var showActionDialog by remember { mutableStateOf(false) }

    // Live Shake Test State
    var lastLiveShakeTime by remember { mutableLongStateOf(0L) }
    var isShakingActive by remember { mutableStateOf(false) }

    // Sensor listener for live interactive test in settings screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, shakeIntensity) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val detector = RainModeManager.ShakePatternDetector(shakeIntensity) {
            lastLiveShakeTime = System.currentTimeMillis()
            isShakingActive = true
            VolumeDndAccessibilityService.performVibration(context, longArrayOf(0, 70))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    detector.processEvent(event)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accel != null) {
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            detector.reset()
            sm?.unregisterListener(listener)
        }
    }

    // Auto-clear live shake highlight after 1.5 seconds
    LaunchedEffect(lastLiveShakeTime) {
        if (lastLiveShakeTime > 0L) {
            delay(1500L)
            isShakingActive = false
        }
    }

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

            // ── Sensor Warning (if accelerometer is missing) ────────────
            if (!hasAccelerometer) {
                RivoAnimatedSection(delayMs = 0L) {
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
                                    "Accelerometer Sensor Missing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                "Rain Mode requires an accelerometer hardware sensor to detect shake gestures, but none was detected on this device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Main Toggle ─────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    RainModeSectionLabel("Rain Mode")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Enable Rain Mode",
                            supporting = "Shake your phone to answer ringing calls or decline/end active calls",
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
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                        // ── Shake Intensity Adjustment ──────────────────────
                        Column {
                            RainModeSectionLabel("Shake Sensitivity")
                            RivoExpressiveCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Shake Intensity",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val percent = (shakeIntensity * 100).toInt()
                                            val threshold = RainModeManager.calculateThresholdG(shakeIntensity)
                                            val thresholdFormatted = String.format(java.util.Locale.US, "%.1f", threshold)
                                            val sensitivityLabel = when {
                                                shakeIntensity < 0.34f -> "Firm shake ($percent% • ~${thresholdFormatted}g)"
                                                shakeIntensity < 0.67f -> "Moderate shake ($percent% • ~${thresholdFormatted}g)"
                                                else -> "Gentle shake ($percent% • ~${thresholdFormatted}g)"
                                            }
                                            Text(
                                                text = sensitivityLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                shakeIntensity = PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
                                                prefs.setFloat(
                                                    PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY,
                                                    shakeIntensity
                                                )
                                            }
                                        ) {
                                            Text("Reset")
                                        }
                                    }

                                    Slider(
                                        value = shakeIntensity,
                                        onValueChange = { shakeIntensity = it },
                                        onValueChangeFinished = {
                                            prefs.setFloat(
                                                PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY,
                                                shakeIntensity
                                            )
                                        },
                                        valueRange = 0f..1f
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Firm (Strong)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Gentle (Light)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Live Interactive Test Card
                                    val testBgColor by animateColorAsState(
                                        targetValue = if (isShakingActive) Color(0xFF2ECC71).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                        label = "liveTestBg"
                                    )
                                    val testBorderColor by animateColorAsState(
                                        targetValue = if (isShakingActive) Color(0xFF2ECC71) else Color.Transparent,
                                        label = "liveTestBorder"
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = testBgColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, testBorderColor, RoundedCornerShape(14.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isShakingActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.outline)
                                            )
                                            Text(
                                                text = if (isShakingActive) "Shake Gesture Detected!" else "Live test: Shake left-right-left-right to test threshold",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isShakingActive) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isShakingActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Call Actions & Feedback ─────────────────────────
                        Column {
                            RainModeSectionLabel("Call Actions & Feedback")
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Incoming Call Action",
                                    supporting = if (incomingAction == "decline") "Decline incoming call on shake" else "Answer incoming call on shake",
                                    leadingIcon = if (incomingAction == "decline") Icons.Outlined.CallEnd else Icons.Outlined.Call,
                                    iconContainerColor = if (incomingAction == "decline") Color(0xFFE53935) else Color(0xFF43A047),
                                    trailingIcon = Icons.Outlined.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("rain_mode_action", highlightedKey) { highlightedKey = null },
                                    onClick = { showActionDialog = true }
                                )
                                CardDivider()
                                RivoSwitchListItem(
                                    headline = "Shake to End Active Call",
                                    supporting = "Shake your device while in an active call to hang up",
                                    leadingIcon = Icons.Outlined.PhonePaused,
                                    iconContainerColor = Color(0xFFE65100),
                                    checked = endActiveCall,
                                    modifier = Modifier.settingsSearchHighlight("rain_mode_end_active", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        endActiveCall = it
                                        prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, it)
                                    }
                                )
                                CardDivider()
                                RivoSwitchListItem(
                                    headline = "Vibration Feedback",
                                    supporting = "Vibrate when a call is answered or hung up via shake gesture",
                                    leadingIcon = Icons.Outlined.Vibration,
                                    iconContainerColor = Color(0xFF9C27B0),
                                    checked = vibrateFeedback,
                                    modifier = Modifier.settingsSearchHighlight("rain_mode_vibrate", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        vibrateFeedback = it
                                        prefs.setBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, it)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Incoming Call Action",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val actions = listOf(
                        "answer" to "Answer Call (Default)",
                        "decline" to "Decline Call"
                    )
                    actions.forEach { (key, label) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (incomingAction == key) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    incomingAction = key
                                    prefs.setString(PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION, key)
                                    showActionDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = incomingAction == key,
                                    onClick = {
                                        incomingAction = key
                                        prefs.setString(PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION, key)
                                        showActionDialog = false
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (incomingAction == key) FontWeight.Bold else FontWeight.Normal,
                                    color = if (incomingAction == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) {
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
