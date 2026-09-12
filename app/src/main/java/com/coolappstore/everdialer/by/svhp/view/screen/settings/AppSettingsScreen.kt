package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coolappstore.everdialer.by.svhp.controller.MissedCallPopupService
import com.coolappstore.everdialer.by.svhp.controller.RaiseToAnswerManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.RainModeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RaiseToAnswerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.VolumeDndScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorGreen  = Color(0xFF4CAF50)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorPink   = Color(0xFFE91E63)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun AppSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var highlightedKey by remember { mutableStateOf(highlightKey) }

    // ── Call Behavior State ───────────────────────────────────────────
    var proximityBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)) }
    var proximityOrientationBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_ORIENTATION_BG, false)) }
    var slantThreshold by remember {
        mutableFloatStateOf(
            prefs.getFloat(
                PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD,
                PreferenceManager.DEFAULT_PROXIMITY_ORIENTATION_SLANT_THRESHOLD
            )
        )
    }
    var previewWouldTurnOff by remember { mutableStateOf(false) }
    DisposableEffect(proximityOrientationBg, slantThreshold) {
        if (!proximityOrientationBg) {
            previewWouldTurnOff = false
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val ax = event.values[0]; val ay = event.values[1]; val az = event.values[2]
                val normOfG = kotlin.math.sqrt(ax * ax + ay * ay + az * az)
                if (normOfG > 0f) {
                    val nx = ax / normOfG; val ny = ay / normOfG; val nz = az / normOfG
                    val inclination = Math.toDegrees(kotlin.math.atan2(nx, ny).toDouble()).toInt()
                    val angleFromFlatDeg = Math.toDegrees(kotlin.math.acos(nz.coerceIn(-1f, 1f).toDouble()))
                    val angleFromFlatOrBelow = kotlin.math.min(angleFromFlatDeg, 180.0 - angleFromFlatDeg)
                    val isSlanted = angleFromFlatOrBelow > slantThreshold.toDouble()
                    previewWouldTurnOff = isSlanted && inclination in -90..90
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    var pocketModePrevention by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) }
    var floatingCall by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FLOATING_CALL, false)) }
    var directCallOnTap by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)) }
    var autoSpeaker by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_SPEAKER, false)) }
    val raiseToAnswerSupported = remember { RaiseToAnswerManager.hasRequiredSensors(context) }
    var raiseToAnswerEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, false) && raiseToAnswerSupported) }
    var rainModeEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false)) }
    var autoRedial by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_ENABLED, false)) }

    // ── Missed Call Popup State ───────────────────────────────────────
    var missedCallPopupEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, false))
    }
    var quickReply1 by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_1, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_1) ?: "")
    }
    var quickReply2 by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_2, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_2) ?: "")
    }
    var quickReply3 by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_3, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_3) ?: "")
    }
    var customFirst by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_CUSTOM_FIRST, false))
    }
    var alwaysShowAfterCallEnds by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ALWAYS_SHOW_MISSED_CALL_POPUP_AFTER_CALL_END, false))
    }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    // ── Lifecycle & Permissions ───────────────────────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── Notes Integration State ───────────────────────────────────────
    var integrateNotes by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, true)) }
    var deleteNotesWithRecording by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DELETE_NOTES_WITH_RECORDING, false)) }

    // ── Dialogs ───────────────────────────────────────────────────────
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.CallMissed, null, tint = ColorAmber) },
            title = { Text("Display Over Other Apps") },
            text = {
                Text("To display the missed call popup over other apps when an incoming call is missed, Ever Dialer requires the 'Display over other apps' permission.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, true)
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Interesting Settings !",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSearchEntryPoint(navigator = navigator)

            // ── Call Behavior ─────────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    AppSettingsSectionLabel("Call Behavior")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline   = "Device Orientation with Proximity Sensor",
                            supporting = "Uses raise-to-ear orientation together with the proximity sensor to turn off the screen, preventing false screen-offs (e.g. on earpiece speaker or when opening the status bar) on phones with sensitive proximity sensors",
                            leadingIcon = Icons.Outlined.ScreenLockPortrait,
                            iconContainerColor = ColorPink,
                            checked = proximityOrientationBg,
                            modifier = Modifier.settingsSearchHighlight("proximity_orientation_bg", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                proximityOrientationBg = it
                                prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_ORIENTATION_BG, it)
                            }
                        )
                        AnimatedVisibility(visible = proximityOrientationBg) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Slant sensitivity",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            slantThreshold = PreferenceManager.DEFAULT_PROXIMITY_ORIENTATION_SLANT_THRESHOLD
                                            prefs.setFloat(PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD, slantThreshold)
                                        }
                                    ) {
                                        Text("Reset")
                                    }
                                }
                                Slider(
                                    value = 115f - slantThreshold,
                                    onValueChange = { slantThreshold = 115f - it },
                                    onValueChangeFinished = {
                                        prefs.setFloat(PreferenceManager.KEY_PROXIMITY_ORIENTATION_SLANT_THRESHOLD, slantThreshold)
                                    },
                                    valueRange = 30f..85f,
                                    steps = 10
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (previewWouldTurnOff) Color(0xFF2ECC71) else Color(0xFFE74C3C))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (previewWouldTurnOff)
                                            "Live test: screen would turn OFF right now"
                                        else
                                            "Live test: screen stays ON right now",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Proximity Sensor on in background",
                            supporting = "Turn off screen when phone is near ear during a call",
                            leadingIcon = Icons.Outlined.Sensors,
                            iconContainerColor = ColorTeal,
                            checked = proximityBg,
                            modifier = Modifier.settingsSearchHighlight("proximity_sensor_bg", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                proximityBg = it
                                prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_BG, it)
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Pocket Mode Prevention",
                            supporting = "Block accidental answer/decline when phone is in pocket",
                            leadingIcon = Icons.Outlined.Sensors,
                            iconContainerColor = ColorAmber,
                            checked = pocketModePrevention,
                            modifier = Modifier.settingsSearchHighlight("pocket_mode_prevention", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                pocketModePrevention = it
                                prefs.setBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, it)
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Floating Ongoing Call",
                            supporting = "Show a draggable floating bubble during calls. Requires 'Display over other apps' permission.",
                            leadingIcon = Icons.Outlined.Sensors,
                            iconContainerColor = ColorBlue,
                            checked = floatingCall,
                            modifier = Modifier.settingsSearchHighlight("floating_ongoing_call", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { newValue ->
                                if (newValue && !Settings.canDrawOverlays(context)) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    floatingCall = newValue
                                    prefs.setBoolean(PreferenceManager.KEY_FLOATING_CALL, newValue)
                                }
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Direct Call on Tap",
                            supporting = "Tap a call log entry to call directly instead of viewing contact info",
                            leadingIcon = Icons.Outlined.Call,
                            iconContainerColor = ColorGreen,
                            checked = directCallOnTap,
                            modifier = Modifier.settingsSearchHighlight("direct_call_on_tap", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                directCallOnTap = it
                                prefs.setBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, it)
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Auto Speaker",
                            supporting = "Automatically switch to loudspeaker when phone is away from ear, and back to earpiece when near",
                            leadingIcon = Icons.Outlined.VolumeUp,
                            iconContainerColor = ColorPink,
                            checked = autoSpeaker,
                            modifier = Modifier.settingsSearchHighlight("auto_speaker", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                autoSpeaker = it
                                prefs.setBoolean(PreferenceManager.KEY_AUTO_SPEAKER, it)
                            }
                        )
                        CardDivider()
                        RivoListItem(
                            headline   = "Raise to Answer",
                            supporting = if (!raiseToAnswerSupported)
                                "Not supported on this device"
                            else if (raiseToAnswerEnabled) "On" else "Off",
                            leadingIcon = Icons.Outlined.Vibration,
                            iconContainerColor = Color(0xFF009688),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("raise_to_answer_link", highlightedKey) { highlightedKey = null },
                            onClick = { navigator.navigate(RaiseToAnswerScreenDestination()) }
                        )
                        CardDivider()
                        RivoListItem(
                            headline   = "Rain Mode",
                            supporting = if (rainModeEnabled) "On (Shake to answer/reject)" else "Off",
                            leadingIcon = Icons.Outlined.WaterDrop,
                            iconContainerColor = Color(0xFF0288D1),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("rain_mode_link", highlightedKey) { highlightedKey = null },
                            onClick = { navigator.navigate(RainModeScreenDestination()) }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Auto Redial",
                            supporting = "When a call is rejected, unanswered, or busy, show an option to automatically redial",
                            leadingIcon = Icons.Default.Replay,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = autoRedial,
                            modifier = Modifier.settingsSearchHighlight("auto_redial", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                autoRedial = it
                                prefs.setBoolean(PreferenceManager.KEY_AUTO_REDIAL_ENABLED, it)
                            }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Missed Call Popup",
                            supporting = "Show an interactive popup over other apps with caller info, quick responses, and social apps when a call is missed",
                            leadingIcon = Icons.AutoMirrored.Filled.CallMissed,
                            iconContainerColor = ColorAmber,
                            checked = missedCallPopupEnabled && canDrawOverlays,
                            modifier = Modifier.settingsSearchHighlight("missed_call_popup", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (!Settings.canDrawOverlays(context)) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        missedCallPopupEnabled = true
                                        prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, true)
                                    }
                                } else {
                                    missedCallPopupEnabled = false
                                    prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, false)
                                }
                            }
                        )
                        AnimatedVisibility(
                            visible = missedCallPopupEnabled && canDrawOverlays,
                            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Custom Quick Responses",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Customize the 3 quick reply messages shown in the missed call popup. Leave a box empty to hide that response.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = quickReply1,
                                    onValueChange = {
                                        quickReply1 = it
                                        prefs.setString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_1, it)
                                    },
                                    label = { Text("Response 1") },
                                    placeholder = { Text(PreferenceManager.DEFAULT_MISSED_CALL_REPLY_1) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = quickReply2,
                                    onValueChange = {
                                        quickReply2 = it
                                        prefs.setString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_2, it)
                                    },
                                    label = { Text("Response 2") },
                                    placeholder = { Text(PreferenceManager.DEFAULT_MISSED_CALL_REPLY_2) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = quickReply3,
                                    onValueChange = {
                                        quickReply3 = it
                                        prefs.setString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_3, it)
                                    },
                                    label = { Text("Response 3") },
                                    placeholder = { Text(PreferenceManager.DEFAULT_MISSED_CALL_REPLY_3) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            customFirst = !customFirst
                                            prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_CUSTOM_FIRST, customFirst)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = customFirst,
                                        onCheckedChange = {
                                            customFirst = it
                                            prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_CUSTOM_FIRST, it)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Put \"Type custom\" first",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Show the custom message button first in the response list",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            alwaysShowAfterCallEnds = !alwaysShowAfterCallEnds
                                            prefs.setBoolean(PreferenceManager.KEY_ALWAYS_SHOW_MISSED_CALL_POPUP_AFTER_CALL_END, alwaysShowAfterCallEnds)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = alwaysShowAfterCallEnds,
                                        onCheckedChange = {
                                            alwaysShowAfterCallEnds = it
                                            prefs.setBoolean(PreferenceManager.KEY_ALWAYS_SHOW_MISSED_CALL_POPUP_AFTER_CALL_END, it)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Always show missed call popup after every call ends",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Show popup after every call ends with only the custom response option",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            MissedCallPopupService.previewLastMissedCall(context)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Preview Popup", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── More Settings ─────────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 30L) {
                Column {
                    AppSettingsSectionLabel("More Settings")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "4G/5G Switcher",
                            supporting = "Quickly toggle your network mode",
                            leadingIcon = Icons.Outlined.SignalCellularAlt,
                            iconContainerColor = Color(0xFF00897B),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("network_switcher", highlightedKey) { highlightedKey = null },
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(context, com.supernova.networkswitch.presentation.ui.activity.MainActivity::class.java)
                                    )
                                } catch (_: Exception) {}
                            }
                        )
                        CardDivider()
                        RivoListItem(
                            headline = "Volume DND",
                            supporting = "Toggle Do Not Disturb using a volume button sequence",
                            leadingIcon = Icons.Outlined.DoNotDisturbOn,
                            iconContainerColor = Color(0xFF7C4DFF),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("volume_dnd", highlightedKey) { highlightedKey = null },
                            onClick = { navigator.navigate(VolumeDndScreenDestination()) }
                        )
                        CardDivider()
                        RivoSwitchListItem(
                            headline   = "Integrate Notes Section",
                            supporting = if (integrateNotes)
                                "Call recording notes stay separate from the app's Notes section"
                            else
                                "Call recording notes are merged into the app's Notes section",
                            leadingIcon = Icons.Default.Note,
                            iconContainerColor = Color(0xFFE53935),
                            checked = integrateNotes,
                            modifier = Modifier.settingsSearchHighlight("integrate_notes", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                integrateNotes = it
                                prefs.setBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, it)
                            }
                        )
                        AnimatedVisibility(visible = integrateNotes) {
                            Column {
                                CardDivider()
                                RivoSwitchListItem(
                                    headline   = "Delete Notes With Recording",
                                    supporting = "Also delete the linked note in Notes when its call recording is deleted",
                                    leadingIcon = Icons.Default.DeleteSweep,
                                    iconContainerColor = Color(0xFF6D4C41),
                                    checked = deleteNotesWithRecording,
                                    modifier = Modifier.settingsSearchHighlight("delete_notes_with_recording", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        deleteNotesWithRecording = it
                                        prefs.setBoolean(PreferenceManager.KEY_DELETE_NOTES_WITH_RECORDING, it)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun AppSettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}
