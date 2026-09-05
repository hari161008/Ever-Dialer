package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.accounts.AccountManager
import android.app.DownloadManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import java.io.File
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator

import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.coolappstore.everdialer.by.svhp.controller.RaiseToAnswerManager
import com.coolappstore.everdialer.by.svhp.controller.VolumeDndAccessibilityService
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.enqueueApkDownload
import com.coolappstore.everdialer.by.svhp.controller.util.getApkDestinationFile
import com.coolappstore.everdialer.by.svhp.controller.util.installApkAndScheduleDelete
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SoundVibrationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RaiseToAnswerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RainModeScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.compose.koinInject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private val ColorGreen   = Color(0xFF4CAF50)
private val ColorTeal    = Color(0xFF009688)
private val ColorAmber   = Color(0xFFFFC107)
private val ColorBlue    = Color(0xFF2196F3)
private val ColorPink    = Color(0xFFE91E63)
private val ColorOrange  = Color(0xFFFF9800)

private sealed class DlState {
    object Idle : DlState()
    object Fetching : DlState()
    data class Downloading(val version: String, val downloadId: Long, val progress: Float) : DlState()
    object Error : DlState()
}

// ─── Contacts to Display Dialog ───────────────────────────────────────────────

data class ContactSourceItem(
    val key: String,
    val label: String,
    val subLabel: String? = null
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactsToDisplayDialog(
    onDismiss: () -> Unit,
    prefs: PreferenceManager
) {
    val context = LocalContext.current

    // Enumerating active SIMs via SubscriptionManager requires READ_PHONE_STATE. Without it,
    // activeSubscriptionInfoList silently comes back null/empty on most OEMs (no exception),
    // so the dialog would always fall back to a single generic "Device Storage" row and real
    // SIM 1 / SIM 2 entries would never show up. Request it here so SIMs actually appear.
    val phoneStatePermission = rememberPermissionState(Manifest.permission.READ_PHONE_STATE)
    LaunchedEffect(Unit) {
        if (!phoneStatePermission.status.isGranted) {
            phoneStatePermission.launchPermissionRequest()
        }
    }

    // Build sources list: Google accounts + SIMs + WhatsApp
    val sources = remember(phoneStatePermission.status.isGranted) {
        val list = mutableListOf<ContactSourceItem>()

        // Google accounts
        try {
            val accountManager = AccountManager.get(context)
            val googleAccounts = accountManager.getAccountsByType("com.google")
            googleAccounts.forEach { account ->
                list.add(ContactSourceItem(
                    key = "google_${account.name}",
                    label = "Google",
                    subLabel = account.name
                ))
            }
            if (googleAccounts.isEmpty()) {
                list.add(ContactSourceItem(key = "google_none", label = "Google", subLabel = "No Google accounts"))
            }
        } catch (_: Exception) {
            list.add(ContactSourceItem(key = "google_none", label = "Google", subLabel = "No Google accounts"))
        }

        // SIM accounts
        val hasPhonePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && hasPhonePermission) {
                val subManager = context.getSystemService(SubscriptionManager::class.java)
                val subs = subManager?.activeSubscriptionInfoList
                if (!subs.isNullOrEmpty()) {
                    subs.forEach { sub ->
                        val simName = sub.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${sub.simSlotIndex + 1}"
                        // Key must match ContactsRepository's slot-based scheme ("sim_1", "sim_2", ...
                        // where the number is simSlotIndex + 1), not the raw subscriptionId — the two
                        // are usually different numbers. Using subscriptionId here meant this filter
                        // could never actually match any raw contact, so toggling SIM sources always
                        // silently produced zero contacts.
                        list.add(ContactSourceItem(
                            key = "sim_${sub.simSlotIndex + 1}",
                            label = if (subs.size > 1) "SIM ${sub.simSlotIndex + 1}" else "SIM",
                            subLabel = simName
                        ))
                    }
                    // Real SIMs being present doesn't mean every contact lives on a SIM — plenty
                    // of contacts are stored locally on the device (no Google/SIM account at all).
                    // Those map to key "sim_0" in ContactsRepository, so always offer a separate
                    // "Device Storage" row too, or local-only contacts had no checkbox anywhere
                    // and could never be toggled back on.
                    list.add(ContactSourceItem(key = "sim_0", label = "Device", subLabel = "Device Storage"))
                } else {
                    list.add(ContactSourceItem(key = "sim_0", label = "SIM", subLabel = "Device Storage"))
                }
            } else {
                list.add(ContactSourceItem(key = "sim_0", label = "SIM", subLabel = "Device Storage"))
            }
        } catch (_: Exception) {
            list.add(ContactSourceItem(key = "sim_0", label = "SIM", subLabel = "Device Storage"))
        }

        // WhatsApp
        list.add(ContactSourceItem(key = "whatsapp", label = "WhatsApp"))

        list
    }

    // Load saved enabled keys
    val savedKeys = remember {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, null)
        if (raw.isNullOrBlank()) sources.map { it.key }.toSet()
        else raw.split(",").toSet()
    }
    val checkedKeys = remember { mutableStateOf(savedKeys) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contacts to display") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                sources.forEach { source ->
                    val isChecked = source.key in checkedKeys.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                checkedKeys.value = if (checked) {
                                    checkedKeys.value + source.key
                                } else {
                                    checkedKeys.value - source.key
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = source.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (source.subLabel != null) {
                                Text(
                                    text = source.subLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.setString(
                    PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS,
                    checkedKeys.value.joinToString(",")
                )
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var highlightedKey by remember { mutableStateOf(highlightKey) }

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
    // Live preview so the user can test the slant slider by hand without needing an active
    // call — mirrors the accelerometer-only inclination + isSlanted gate (orientation only,
    // no proximity, so it's testable without covering the sensor).
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
    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var defaultSim by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault())) }
    var showSimDialog by remember { mutableStateOf(false) }

    // ── Volume DND State ──────────────────────────────────────────────
    var volumeDndEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, false)) }
    var volumeDndSequence by remember {
        mutableStateOf(
            prefs.getString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE)
                ?: PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
        )
    }
    var volumeDndLockScreenOnly by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, false))
    }
    var volumeDndTimeoutMs by remember {
        mutableStateOf(
            prefs.getInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, PreferenceManager.DEFAULT_VOLUME_DND_TIMEOUT_MS).toString()
        )
    }
    var showPermissionCardDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAccessibilityGranted by remember { mutableStateOf(VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)) }
    var isDndGranted by remember { mutableStateOf(VolumeDndAccessibilityService.isDndAccessGranted(context)) }
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var missedCallPopupEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_POPUP_ENABLED, false))
    }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
                isDndGranted = VolumeDndAccessibilityService.isDndAccessGranted(context)
                canDrawOverlays = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var visible by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "callSettingsAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    if (showContactsToDisplayDialog) {
        ContactsToDisplayDialog(
            onDismiss = { showContactsToDisplayDialog = false },
            prefs = prefs
        )
    }

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

    if (showSimDialog) {
        AlertDialog(
            onDismissRequest = { showSimDialog = false },
            title = { Text("Default SIM") },
            text = {
                Column {
                    listOf("Ask every time", "SIM 1", "SIM 2").forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultSim == index,
                                onClick = {
                                    defaultSim = index
                                    prefs.setInt(PreferenceManager.KEY_DEFAULT_SIM, index)
                                    showSimDialog = false
                                }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = androidx.compose.ui.Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSimDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Call Settings", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)

            // ── Caller Accounts ───────────────────────────────────────────────
                RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        CallSettingsSectionLabel("Accounts")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Default SIM",
                                supporting = when(defaultSim) {
                                    0 -> "Ask every time"
                                    1 -> "SIM 1"
                                    2 -> "SIM 2"
                                    else -> "Ask every time"
                                },
                                leadingIcon = Icons.Outlined.SimCard,
                                iconContainerColor = ColorGreen,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("default_sim", highlightedKey) { highlightedKey = null },
                                onClick = { showSimDialog = true }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoListItem(
                                headline = "Contacts to display",
                                supporting = "Choose which accounts' contacts are shown",
                                leadingIcon = Icons.Outlined.Contacts,
                                iconContainerColor = ColorBlue,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("contacts_to_display", highlightedKey) { highlightedKey = null },
                                onClick = { showContactsToDisplayDialog = true }
                            )
                        }
                    }
                }

            // ── Call Behavior ─────────────────────────────────────────────────
                RivoAnimatedSection(delayMs = 60L) {
                    Column {
                        CallSettingsSectionLabel("Call Behavior")
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
                                                android.net.Uri.parse("package:${context.packageName}")
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } else {
                                        floatingCall = newValue
                                        prefs.setBoolean(PreferenceManager.KEY_FLOATING_CALL, newValue)
                                    }
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            val raiseToAnswerSupported = remember { RaiseToAnswerManager.hasRequiredSensors(context) }
                            var raiseToAnswerEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, false) && raiseToAnswerSupported) }
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            var rainModeEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false)) }
                            RivoListItem(
                                headline   = "Rain Mode",
                                supporting = if (rainModeEnabled) "On (Shake to answer/reject)" else "Off",
                                leadingIcon = Icons.Outlined.WaterDrop,
                                iconContainerColor = Color(0xFF0288D1),
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("rain_mode_link", highlightedKey) { highlightedKey = null },
                                onClick = { navigator.navigate(RainModeScreenDestination()) }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            var autoRedial by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_ENABLED, false)) }
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
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
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
                            if (missedCallPopupEnabled && canDrawOverlays) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 56.dp, end = 16.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            com.coolappstore.everdialer.by.svhp.controller.MissedCallPopupService.start(
                                                context = context,
                                                number = "+1 234 567 8900",
                                                name = "Amma",
                                                callDate = System.currentTimeMillis() - 240000L,
                                                ringDurationSec = 2L
                                            )
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

            // ── Sound & Vibration ─────────────────────────────────────────────
                RivoAnimatedSection(delayMs = 120L) {
                    Column {
                        CallSettingsSectionLabel("Sound & Vibration")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Sound & Vibration",
                                supporting = "Ringtones and dialpad tones",
                                leadingIcon = Icons.Outlined.VolumeUp,
                                iconContainerColor = ColorBlue,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("sound_vibration_link", highlightedKey) { highlightedKey = null },
                                onClick = { navigator.navigate(SoundVibrationScreenDestination()) }
                            )
                        }
                    }
                }

            // ── Volume DND ────────────────────────────────────────────────────
                RivoAnimatedSection(delayMs = 150L) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CallSettingsSectionLabel("Volume DND")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Volume DND",
                                supporting = "Toggle Do Not Disturb (DND) using a volume button sequence",
                                leadingIcon = Icons.Outlined.DoNotDisturbOn,
                                iconContainerColor = Color(0xFF7C4DFF),
                                checked = volumeDndEnabled,
                                modifier = Modifier.settingsSearchHighlight("volume_dnd", highlightedKey) { highlightedKey = null },
                                onCheckedChange = { newValue ->
                                    volumeDndEnabled = newValue
                                    prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_ENABLED, newValue)
                                    if (newValue) {
                                        isAccessibilityGranted = VolumeDndAccessibilityService.isAccessibilityServiceEnabled(context)
                                        isDndGranted = VolumeDndAccessibilityService.isDndAccessGranted(context)
                                        if (!isAccessibilityGranted || !isDndGranted) {
                                            showPermissionCardDialog = true
                                        }
                                    }
                                }
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Missing permissions warning banner if any permission is missing
                                if (!isAccessibilityGranted || !isDndGranted) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    "Permissions Required",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            Text(
                                                "Volume DND requires Accessibility Service to capture volume button presses and Do Not Disturb permission to toggle DND.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (!isAccessibilityGranted) {
                                                    Button(
                                                        onClick = { VolumeDndAccessibilityService.openAccessibilitySettings(context) },
                                                        shape = RoundedCornerShape(14.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.error,
                                                            contentColor = MaterialTheme.colorScheme.onError
                                                        ),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                                    ) {
                                                        Text("Accessibility", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (!isDndGranted) {
                                                    Button(
                                                        onClick = { VolumeDndAccessibilityService.openDndAccessSettings(context) },
                                                        shape = RoundedCornerShape(14.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.error,
                                                            contentColor = MaterialTheme.colorScheme.onError
                                                        ),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                                    ) {
                                                        Text("DND Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                                // Volume Combination Card
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Volume button combination",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (volumeDndSequence != PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE) {
                                                TextButton(
                                                    onClick = {
                                                        volumeDndSequence = PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE
                                                        prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, PreferenceManager.DEFAULT_VOLUME_DND_SEQUENCE)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Reset (UUDD)", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Click volume buttons in this order with under ${volumeDndTimeoutMs}ms delay to trigger DND:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Dynamic Sequence Badges
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(12.dp),
                                                horizontalArrangement = if (volumeDndSequence.isEmpty()) Arrangement.Center else Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (volumeDndSequence.isEmpty()) {
                                                    Text(
                                                        "No keys added (tap below to add)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    volumeDndSequence.forEachIndexed { _, char ->
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = if (char == 'U') MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                            contentColor = if (char == 'U') MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                                            shadowElevation = 1.dp
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (char == 'U') Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Text(
                                                                    text = if (char == 'U') "UP" else "DOWN",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Horizontal action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (volumeDndSequence.length < 16) {
                                                        val updated = volumeDndSequence + "U"
                                                        volumeDndSequence = updated
                                                        prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, updated)
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Vol Up", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Button(
                                                onClick = {
                                                    if (volumeDndSequence.length < 16) {
                                                        val updated = volumeDndSequence + "D"
                                                        volumeDndSequence = updated
                                                        prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, updated)
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Outlined.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Vol Down", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            FilledTonalButton(
                                                onClick = {
                                                    if (volumeDndSequence.isNotEmpty()) {
                                                        val updated = volumeDndSequence.dropLast(1)
                                                        volumeDndSequence = updated
                                                        prefs.setString(PreferenceManager.KEY_VOLUME_DND_SEQUENCE, updated)
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                                enabled = volumeDndSequence.isNotEmpty()
                                            ) {
                                                Icon(Icons.AutoMirrored.Outlined.Backspace, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                // Delay Timeout Settings Card
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Timer,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Trigger Delay Timeout",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Material You value pill badge
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Text(
                                                    text = "${volumeDndTimeoutMs.ifEmpty { "600" }} ms",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Maximum delay in milliseconds between button clicks. Smaller numbers require faster clicks.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Responsive Slider with Material You styling
                                        val currentTimeoutFloat = volumeDndTimeoutMs.toFloatOrNull()?.coerceIn(100f, 3000f) ?: 600f
                                        Slider(
                                            value = currentTimeoutFloat,
                                            onValueChange = { newMs ->
                                                val rounded = (newMs / 50).toInt() * 50
                                                volumeDndTimeoutMs = rounded.toString()
                                                prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, rounded)
                                            },
                                            valueRange = 100f..3000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = volumeDndTimeoutMs,
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }.take(5)
                                                volumeDndTimeoutMs = digits
                                                val num = digits.toIntOrNull()
                                                if (num != null && num in 100..5000) {
                                                   prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, num)
                                                }
                                            },
                                            label = { Text("Delay (milliseconds)") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            trailingIcon = {
                                                Text("ms", modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        )

                                        // Screen-adaptive FlowRow for preset chips with Material You colors & selection
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val presets = listOf(
                                                300 to "300ms (Fastest)",
                                                400 to "400ms (Fast)",
                                                600 to "600ms (Default)",
                                                800 to "800ms (Normal)",
                                                1200 to "1.2s (Relaxed)"
                                            )
                                            val currentMs = volumeDndTimeoutMs.toIntOrNull()
                                            presets.forEach { (presetMs, label) ->
                                                val isSelected = currentMs == presetMs
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        volumeDndTimeoutMs = presetMs.toString()
                                                        prefs.setInt(PreferenceManager.KEY_VOLUME_DND_TIMEOUT_MS, presetMs)
                                                    },
                                                    label = {
                                                        Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                    },
                                                    leadingIcon = if (isSelected) {
                                                        {
                                                            Icon(
                                                                Icons.Filled.Check,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else null,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                                    ),
                                                    border = null
                                                )
                                            }
                                        }
                                    }
                                }

                                // Lock screen only card
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                val newVal = !volumeDndLockScreenOnly
                                                volumeDndLockScreenOnly = newVal
                                                prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, newVal)
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ScreenLockPortrait,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Enable only in lock screen & screen off",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Only triggers when screen is off or on lock screen; ignored on home screen",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = volumeDndLockScreenOnly,
                                            onCheckedChange = { newVal ->
                                                volumeDndLockScreenOnly = newVal
                                                prefs.setBoolean(PreferenceManager.KEY_VOLUME_DND_LOCK_SCREEN_ONLY, newVal)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── Permission Card Popup Dialog ──────────────────────────────────────────
    if (showPermissionCardDialog && (!isAccessibilityGranted || !isDndGranted)) {
        AlertDialog(
            onDismissRequest = { showPermissionCardDialog = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = { Text("Permissions Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "To detect volume button sequences and toggle Do Not Disturb (DND), please grant the following permissions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isAccessibilityGranted) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.Accessibility, null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Accessibility Service", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Required to capture volume key combinations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { VolumeDndAccessibilityService.openAccessibilitySettings(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (!isDndGranted) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Outlined.DoNotDisturbOn, null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Do Not Disturb Access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Required to toggle system DND state", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { VolumeDndAccessibilityService.openDndAccessSettings(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Grant", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPermissionCardDialog = false }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
internal fun CallRecordingDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val recorderPkg = "com.coolappstore.evercallrecorder.by.svhp"
    val githubUrl   = "https://github.com/hari161008/Ever-Call-Recorder"
    val apiUrl      = "https://api.github.com/repos/hari161008/Ever-Call-Recorder/releases/latest"
    val apkFile     = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EverCallRecorder.apk")

    // Check state on every composition so it reacts after install
    val pm = context.packageManager
    val isInstalled = remember { mutableStateOf(try { pm.getPackageInfo(recorderPkg, 0); true } catch (_: Exception) { false }) }
    val apkAlreadyDownloaded = remember { mutableStateOf(apkFile.exists() && apkFile.length() > 0L) }

    // Refresh install state when dialog is shown (handles post-install return)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            isInstalled.value = try { pm.getPackageInfo(recorderPkg, 0); true } catch (_: Exception) { false }
            apkAlreadyDownloaded.value = apkFile.exists() && apkFile.length() > 0L
        }
    }

    var dlState by remember { mutableStateOf<DlState>(DlState.Idle) }

    // Poll download progress
    if (dlState is DlState.Downloading) {
        val state = dlState as DlState.Downloading
        LaunchedEffect(state.downloadId) {
            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
            while (true) {
                delay(300)
                val cursor = dm.query(DownloadManager.Query().setFilterById(state.downloadId))
                if (!cursor.moveToFirst()) { cursor.close(); break }
                val dmStatus   = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total      = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                cursor.close()
                when (dmStatus) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        dlState = DlState.Idle
                        apkAlreadyDownloaded.value = apkFile.exists()
                        // Launch standard package installer via FileProvider — most reliable method
                        launchApkInstaller(context, apkFile)
                        break
                    }
                    DownloadManager.STATUS_FAILED -> { dlState = DlState.Error; break }
                    else -> {
                        val p = if (total > 0L) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                        dlState = state.copy(progress = p)
                    }
                }
            }
        }
    }

    when (val state = dlState) {
        is DlState.Fetching -> Dialog(onDismissRequest = {}) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Fetching latest release…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is DlState.Downloading -> Dialog(onDismissRequest = {}) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.FiberManualRecord, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    Text("Downloading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("v${state.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${(state.progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Please wait…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        is DlState.Error -> AlertDialog(
            onDismissRequest = { dlState = DlState.Idle; onDismiss() },
            icon = { Icon(Icons.Default.Error, null, tint = Color(0xFFE53935)) },
            title = { Text("Download Failed") },
            text = { Text("Could not download Ever Call Recorder. Please try again or visit GitHub.") },
            confirmButton = { TextButton(onClick = { dlState = DlState.Idle; onDismiss() }) { Text("OK") } }
        )
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(Icons.Default.FiberManualRecord, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                },
                title = { Text("Call Recording") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Ever Call Recorder is a companion app that adds call recording support.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isInstalled.value) {
                            Text("Ever Call Recorder is already installed.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                        } else if (apkAlreadyDownloaded.value) {
                            Text("APK already downloaded. Tap Install to proceed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                confirmButton = {
                    when {
                        isInstalled.value -> {
                            Button(
                                onClick = {
                                    // Re-check at click time so it reflects actual install state
                                    val launch = try { pm.getLaunchIntentForPackage(recorderPkg) } catch (_: Exception) { null }
                                    if (launch != null) {
                                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launch)
                                    }
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("Open App") }
                        }
                        apkAlreadyDownloaded.value -> {
                            Button(
                                onClick = { launchApkInstaller(context, apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Install") }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    dlState = DlState.Fetching
                                    scope.launch {
                                        try {
                                            val releaseInfo = withContext(Dispatchers.IO) {
                                                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                                                conn.setRequestProperty("Accept", "application/vnd.github+json")
                                                conn.connectTimeout = 10_000
                                                conn.readTimeout = 10_000
                                                conn.connect()
                                                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                                                val tag = json.optString("tag_name", "").trimStart('v')
                                                val assets = json.optJSONArray("assets")
                                                var url: String? = null
                                                if (assets != null) {
                                                    for (i in 0 until assets.length()) {
                                                        val a = assets.getJSONObject(i)
                                                        if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                                                            url = a.optString("browser_download_url")
                                                            break
                                                        }
                                                    }
                                                }
                                                Pair(tag, url)
                                            }
                                            val (version, apkUrl) = releaseInfo
                                            if (apkUrl != null) {
                                                // Delete stale APK before re-download
                                                if (apkFile.exists()) apkFile.delete()
                                                val req = DownloadManager.Request(Uri.parse(apkUrl))
                                                    .setTitle("Ever Call Recorder")
                                                    .setDescription("Downloading v$version…")
                                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "EverCallRecorder.apk")
                                                    .setMimeType("application/vnd.android.package-archive")
                                                    .setAllowedOverMetered(true)
                                                    .setAllowedOverRoaming(true)
                                                val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
                                                val dlId = dm.enqueue(req)
                                                dlState = DlState.Downloading(version, dlId, 0f)
                                            } else {
                                                dlState = DlState.Error
                                            }
                                        } catch (_: Exception) {
                                            dlState = DlState.Error
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                            ) { Text("Download") }
                        }
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
                        }) { Text("GitHub") }
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                    }
                }
            )
        }
    }
}

/** Install an APK via the standard Android package installer (FileProvider URI). */
internal fun launchApkInstaller(context: Context, file: File) {
    try {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
private fun CallSettingsSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}
