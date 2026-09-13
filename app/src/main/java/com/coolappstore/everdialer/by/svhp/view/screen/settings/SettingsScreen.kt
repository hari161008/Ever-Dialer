package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.controller.util.BackupManager
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultDialerManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollHapticsEffect
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.components.globalSettingsSearchEntries
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.coolappstore.everdialer.by.svhp.view.theme.wpTurnstileControl
import com.coolappstore.everdialer.by.svhp.view.theme.wpTurnstileItem

private val ColorPurple  = Color(0xFF9C27B0)
private val ColorOrange  = Color(0xFFFF9800)
private val ColorBlue    = Color(0xFF2196F3)
private val ColorGreen   = Color(0xFF4CAF50)
private val ColorRed     = Color(0xFFE91E63)
private val ColorTeal    = Color(0xFF009688)
private val ColorIndigo  = Color(0xFF3F51B5)
private val ColorBluGrey = Color(0xFF607D8B)
private val ColorAmber   = Color(0xFFFFC107)
private val ColorBrown   = Color(0xFF795548)
private val ColorCyan    = Color(0xFF00BCD4)

/**
 * The main settings list is built from a fixed sequence of `item { }` blocks (one per section
 * card), in this exact order, after the always-present search field item and the conditional
 * "Set as Default Dialer" banner item. Each entry here lists the `settingsSearchHighlight` keys
 * that live inside that section's item, so a search-result tap can resolve straight to which
 * LazyColumn item index needs to be scrolled to before that row can be brought into view.
 */
private val settingsSectionKeyGroups: List<List<String>> = listOf(
    listOf("check_for_updates", "call_recording", "rate_and_review", "check_ratings", "more_apps", "donate"),
    listOf("interface"),
    listOf("tap_haptics", "scroll_haptics"),
    listOf("authentication"),
    listOf("app_settings", "contacts_hider", "fake_call", "call_recording"),
    listOf("silence_unknown", "blocked_numbers"),
    listOf("create_backup", "restore_backup"),
    listOf("about_app")
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()

    var silenceUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SILENCE_UNKNOWN, false)) }
    val rateReviewSettingsVersion by prefs.settingsChanged.collectAsState()
    // Set from the bundled Ever Call Recorder module's own Settings screen (writes straight into
    // this same "rivo_prefs" file), so just read it fresh each time this screen composes.
    val showRecordingMenuBelowUpdates = remember { prefs.getBoolean(PreferenceManager.KEY_SHOW_RECORDING_MENU_BELOW_UPDATES, false) }
    val hideRateAndReview = remember(rateReviewSettingsVersion) {
        prefs.getBoolean(PreferenceManager.KEY_HIDE_RATE_AND_REVIEW, false) ||
            prefs.getBoolean(PreferenceManager.KEY_RATE_REVIEW_HIDDEN_SECRET, false)
    }
    var notesEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)) }
    var proximityBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)) }
    var tapHapticsEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) }
    var scrollHapticsEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, false)) }
    var scrollCmPerHaptic by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_SCROLL_CM_PER_HAPTIC, 1.5f)) }
    var scrollHapticStrength by remember { mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_SCROLL_HAPTIC_STRENGTH, 60)) }
    var pocketModePrevention by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) }
    var directCallOnTap by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)) }

    // Haptics popup state
    var showHapticsDialog by remember { mutableStateOf(false) }
    var hapticsStrength by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_HAPTICS_STRENGTH, "light") ?: "light") }

    // Blocked numbers dialog state
    var showBlockedNumbersDialog by remember { mutableStateOf(false) }
    var showBlockListDialog by remember { mutableStateOf(false) }
    var blockedNumbersTab by remember { mutableStateOf(0) }
    var blockedNumberInput by remember { mutableStateOf("") }
    var blockedContactsList by remember {
        mutableStateOf(
            com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.getBlockedList(context, prefs)
        )
    }
    // Keep this in sync if a number gets blocked/unblocked elsewhere (Calls tab or Contacts tab
    // context menus) while this screen is alive in the back stack.
    LaunchedEffect(rateReviewSettingsVersion) {
        blockedContactsList = com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.getBlockedList(context, prefs)
    }

    var backupState       by remember { mutableStateOf<BackupDialogState>(BackupDialogState.Idle) }
    var showBackupDialog  by remember { mutableStateOf(false) }
    var pendingBackupSettings by remember { mutableStateOf(true) }
    var pendingBackupCallingCards by remember { mutableStateOf(true) }
    var pendingBackupNotes by remember { mutableStateOf(true) }
    var pendingBackupContactGroups by remember { mutableStateOf(false) }
    var pendingBackupRecordings by remember { mutableStateOf(true) }
    var pendingBackupContacts by remember { mutableStateOf(false) }
    var pendingBackupCallLogs by remember { mutableStateOf(false) }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var pendingRestoreContents by remember { mutableStateOf<BackupManager.BackupContents?>(null) }
    var showDonateDialog by remember { mutableStateOf(false) }

    // Save backup file picker
    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            backupState = BackupDialogState.Creating
            scope.launch(Dispatchers.IO) {
                try {
                    val ok = context.contentResolver.openOutputStream(uri)?.use { output ->
                        BackupManager.writeBackup(
                            context,
                            output,
                            pendingBackupSettings,
                            pendingBackupCallingCards,
                            pendingBackupNotes,
                            pendingBackupContactGroups,
                            pendingBackupRecordings,
                            pendingBackupContacts,
                            pendingBackupCallLogs
                        )
                    } ?: false
                    withContext(Dispatchers.Main) {
                        backupState = if (ok) {
                            BackupDialogState.BackupSuccess("Backup saved successfully")
                        } else {
                            BackupDialogState.Error("Failed to save backup")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        backupState = BackupDialogState.Error(e.message ?: "Failed to save backup")
                    }
                }
            }
        }
    }

    // Restore file picker
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tmpFile = File(context.cacheDir, "restore_tmp.everdialer")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val contents = BackupManager.inspectBackup(tmpFile)
                    withContext(Dispatchers.Main) {
                        pendingRestoreFile = tmpFile
                        pendingRestoreContents = contents
                        showRestoreDialog = true
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        backupState = BackupDialogState.Error(e.message ?: "Failed to read backup file")
                    }
                }
            }
        }
    }

    // Default dialer
    var isDefaultDialer by remember { mutableStateOf(DefaultDialerManager.isDefaultDialer(context)) }
    val defaultDialerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultDialer = DefaultDialerManager.isDefaultDialer(context)
    }
    val activity = context as? Activity
    DisposableEffect(activity) {
        val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
                isDefaultDialer = DefaultDialerManager.isDefaultDialer(context)
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    if (showDonateDialog) {
        com.coolappstore.everdialer.by.svhp.view.components.DonateOptionDialog(
            onDismiss = { showDonateDialog = false },
            onOpenBrowser = {
                showDonateDialog = false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hariprabhu.com/Ever-Dialer/#donate")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            onOpenInApp = {
                showDonateDialog = false
                navigator.navigate(com.ramcosta.composedestinations.generated.destinations.DonateWebViewScreenDestination)
            }
        )
    }

    // ── Haptics Dialog ────────────────────────────────────────────────────────
    if (showHapticsDialog) {
        fun triggerPreviewVibration(strength: String) {
            val duration = if (strength == "strong") 80L else 40L
            val amplitude = if (strength == "strong") 255 else 80
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                }
            } catch (_: Exception) {}
        }

        // Custom intensity: 0f..1f stored in prefs
        var customIntensity by remember {
            mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
        }

        AlertDialog(
            onDismissRequest = { showHapticsDialog = false },
            icon = { Icon(Icons.Outlined.Vibration, null, tint = ColorPurple) },
            title = { Text("Tap Haptics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Tap Haptics", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = tapHapticsEnabled,
                            onCheckedChange = {
                                tapHapticsEnabled = it
                                prefs.setBoolean(PreferenceManager.KEY_APP_HAPTICS, it)
                            }
                        )
                    }

                    if (tapHapticsEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

                        Text("Strength", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        // Three-way segmented control: Light / Strong / Custom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            listOf("light" to "Light", "strong" to "Strong", "custom" to "Custom").forEach { (key, label) ->
                                val selected = hapticsStrength == key
                                Surface(
                                    onClick = {
                                        hapticsStrength = key
                                        prefs.setString(PreferenceManager.KEY_HAPTICS_STRENGTH, key)
                                        if (key != "custom") triggerPreviewVibration(key)
                                        else {
                                            // preview with current custom intensity
                                            val dur = (10 + customIntensity * 70).toLong().coerceIn(10, 80)
                                            val amp = (40  + (customIntensity * 215)).toInt().coerceIn(40, 255)
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                                    vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                                    v.vibrate(VibrationEffect.createOneShot(dur, amp))
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom intensity slider — only shown when "Custom" is selected
                        androidx.compose.animation.AnimatedVisibility(
                            visible = hapticsStrength == "custom",
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            var lastVibratedSegment by remember { mutableIntStateOf(-1) }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Custom Intensity",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = customIntensity,
                                    onValueChange = { v ->
                                        customIntensity = v
                                        prefs.setFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, v)
                                        // Vibrate every ~6% of range change for continuous multi-level feedback
                                        val segment = (v * 16).toInt()
                                        if (segment != lastVibratedSegment) {
                                            lastVibratedSegment = segment
                                            val dur = (8 + v * 55).toLong().coerceIn(8, 63)
                                            val amp = (30 + (v * 180)).toInt().coerceIn(30, 210)
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                                    vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    val v2 = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        v2.vibrate(VibrationEffect.createOneShot(dur, amp))
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        v2.vibrate(dur)
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    onValueChangeFinished = {
                                        // Final vibration at full saved intensity
                                        val dur = (10 + customIntensity * 70).toLong().coerceIn(10, 80)
                                        val amp = (40  + (customIntensity * 215)).toInt().coerceIn(40, 255)
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                val v2 = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                                v2.vibrate(VibrationEffect.createOneShot(dur, amp))
                                            }
                                        } catch (_: Exception) {}
                                        lastVibratedSegment = -1
                                    },
                                    valueRange = 0f..1f,
                                    steps = 15,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Softer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Stronger", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (hapticsStrength == "custom") {
                                    val dur = (10 + customIntensity * 70).toLong().coerceIn(10, 80)
                                    val amp = (40  + (customIntensity * 215)).toInt().coerceIn(40, 255)
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                            v.vibrate(VibrationEffect.createOneShot(dur, amp))
                                        }
                                    } catch (_: Exception) {}
                                } else {
                                    triggerPreviewVibration(hapticsStrength)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Vibration, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Preview Haptic")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHapticsDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Blocked Numbers Dialog ────────────────────────────────────────────────
    // ── Blocked Numbers Dialog (Add to Block List) ───────────────────────────
    if (showBlockedNumbersDialog) {
        val callLogRepo: ICallLogRepository = koinInject()
        val contactsRepo: IContactsRepository = koinInject()

        var recentNumbers by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var contactNumbers by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var searchQuery by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val contactsList = try { contactsRepo.getContacts() } catch (_: Exception) { emptyList() }
                val numberToContact = HashMap<String, Pair<String, String?>>()
                for (c in contactsList) {
                    for (p in c.phoneNumbers) {
                        val clean = p.replace(" ", "").replace("-", "").trim()
                        numberToContact[clean] = Pair(c.name, c.photoUri)
                        numberToContact[p] = Pair(c.name, c.photoUri)
                    }
                }

                contactNumbers = contactsList
                    .filter { it.phoneNumbers.isNotEmpty() }
                    .flatMap { c -> c.phoneNumbers.map { num -> Triple(num, c.name, c.photoUri) } }
                    .distinctBy { it.first }
                    .sortedBy { it.second }

                val logs = try { callLogRepo.getCallLogs() } catch (_: Exception) { emptyList() }
                val seen = HashSet<String>()
                val recentRes = ArrayList<Triple<String, String, String?>>()
                for (log in logs) {
                    val num = log.number
                    if (num.isBlank() || !seen.add(num)) continue
                    val clean = num.replace(" ", "").replace("-", "").trim()
                    val matched = numberToContact[clean] ?: numberToContact[num]
                    val name = matched?.first ?: (log.name?.takeIf { it.isNotBlank() } ?: num)
                    val photo = matched?.second
                    recentRes.add(Triple(num, name, photo))
                }
                recentNumbers = recentRes
            }
            isLoading = false
        }

        val filteredRecents = remember(recentNumbers, searchQuery) {
            if (searchQuery.isBlank()) recentNumbers
            else recentNumbers.filter { (num, name, _) ->
                name.contains(searchQuery, ignoreCase = true) || num.contains(searchQuery)
            }
        }
        val filteredContacts = remember(contactNumbers, searchQuery) {
            if (searchQuery.isBlank()) contactNumbers
            else contactNumbers.filter { (num, name, _) ->
                name.contains(searchQuery, ignoreCase = true) || num.contains(searchQuery)
            }
        }

        fun blockNumber(number: String) {
            com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.block(context, prefs, number)
            blockedContactsList = com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.getBlockedList(context, prefs)
        }

        val maxDialogHeightDp = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        Dialog(onDismissRequest = { showBlockedNumbersDialog = false }) {
            com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .heightIn(max = maxDialogHeightDp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dialog Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                "Block a Number",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showBlockedNumbersDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                            }
                        }

                        // Search Bar (for Recent / Contacts tabs)
                        if (blockedNumbersTab != 2) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search name or number…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        AnimatedVisibility(
                                            visible = searchQuery.isNotBlank(),
                                            enter = fadeIn() + scaleIn(),
                                            exit = fadeOut() + scaleOut()
                                        ) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        // Expressive Pill Tabs
                        val tabItems = listOf(
                            Triple("Recents", Icons.Outlined.History, 0),
                            Triple("Contacts", Icons.Outlined.Contacts, 1),
                            Triple("Manual", Icons.Outlined.Edit, 2)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tabItems.forEach { (label, icon, index) ->
                                    val selected = blockedNumbersTab == index
                                    val bgColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        label = "tabBg"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabTxt"
                                    )
                                    Surface(
                                        onClick = { blockedNumbersTab = index },
                                        shape = RoundedCornerShape(50),
                                        color = bgColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = contentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tab Content Area
                        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(min = 180.dp, max = 340.dp)) {
                            AnimatedContent(
                                targetState = blockedNumbersTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                                    } else {
                                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                                    }
                                },
                                label = "blockedNumbersTabContent"
                            ) { tab ->
                                when (tab) {
                                    0 -> {
                                        Crossfade(targetState = isLoading, label = "recentsLoading") { loading ->
                                            if (loading) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(36.dp),
                                                            strokeWidth = 3.5.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            "Loading call logs…",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else if (filteredRecents.isEmpty()) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(36.dp))
                                                        Text(
                                                            if (searchQuery.isBlank()) "No call logs found." else "No results for \"$searchQuery\"",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    items(filteredRecents, key = { it.first }) { (number, name, photoUri) ->
                                                        val alreadyBlocked = blockedContactsList.contains(number)
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                RivoAvatar(
                                                                    name = name,
                                                                    photoUri = photoUri,
                                                                    modifier = Modifier.size(38.dp),
                                                                    shape = RoundedCornerShape(12.dp)
                                                                )
                                                                Spacer(Modifier.width(12.dp))
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        name,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        maxLines = 1
                                                                    )
                                                                    if (name != number) {
                                                                        Text(
                                                                            number,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                }
                                                                if (alreadyBlocked) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(50),
                                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                                            Text("Blocked", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                                        }
                                                                    }
                                                                } else {
                                                                    FilledTonalButton(
                                                                        onClick = { blockNumber(number) },
                                                                        shape = RoundedCornerShape(50),
                                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                        modifier = Modifier.height(34.dp)
                                                                    ) {
                                                                        Icon(Icons.Outlined.Block, null, modifier = Modifier.size(14.dp))
                                                                        Spacer(Modifier.width(4.dp))
                                                                        Text("Block", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        Crossfade(targetState = isLoading, label = "contactsLoading") { loading ->
                                            if (loading) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(36.dp),
                                                            strokeWidth = 3.5.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            "Loading contacts…",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else if (filteredContacts.isEmpty()) {
                                                Box(Modifier.fillMaxSize().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Outlined.Contacts, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(36.dp))
                                                        Text(
                                                            if (searchQuery.isBlank()) "No contacts found." else "No results for \"$searchQuery\"",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    items(filteredContacts, key = { it.first }) { (number, name, photoUri) ->
                                                        val alreadyBlocked = blockedContactsList.contains(number)
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                RivoAvatar(
                                                                    name = name,
                                                                    photoUri = photoUri,
                                                                    modifier = Modifier.size(38.dp),
                                                                    shape = RoundedCornerShape(12.dp)
                                                                )
                                                                Spacer(Modifier.width(12.dp))
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        name,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        maxLines = 1
                                                                    )
                                                                    if (name != number) {
                                                                        Text(
                                                                            number,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                }
                                                                if (alreadyBlocked) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(50),
                                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                                            Text("Blocked", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                                        }
                                                                    }
                                                                } else {
                                                                    FilledTonalButton(
                                                                        onClick = { blockNumber(number) },
                                                                        shape = RoundedCornerShape(50),
                                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                        modifier = Modifier.height(34.dp)
                                                                    ) {
                                                                        Icon(Icons.Outlined.Block, null, modifier = Modifier.size(14.dp))
                                                                        Spacer(Modifier.width(4.dp))
                                                                        Text("Block", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = blockedNumberInput,
                                                onValueChange = { blockedNumberInput = it },
                                                label = { Text("Phone number to block") },
                                                placeholder = { Text("+1 234 567 8900") },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                },
                                                trailingIcon = {
                                                    AnimatedVisibility(
                                                        visible = blockedNumberInput.isNotBlank(),
                                                        enter = fadeIn() + scaleIn(),
                                                        exit = fadeOut() + scaleOut()
                                                    ) {
                                                        IconButton(onClick = { blockedNumberInput = "" }) {
                                                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                            )

                                            Button(
                                                onClick = {
                                                    val num = blockedNumberInput.trim()
                                                    if (num.isNotBlank()) {
                                                        blockNumber(num)
                                                        blockedNumberInput = ""
                                                    }
                                                },
                                                enabled = blockedNumberInput.trim().isNotBlank(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                )
                                            ) {
                                                Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Block Number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showBlockedNumbersDialog = false },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Done", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Block List Detail Dialog ───────────────────────────────────────────────
    if (showBlockListDialog) {
        val contactsRepo: IContactsRepository = koinInject()
        var blockedWithInfo by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var listSearchQuery by remember { mutableStateOf("") }
        var isListLoading by remember { mutableStateOf(true) }

        LaunchedEffect(blockedContactsList) {
            isListLoading = true
            withContext(Dispatchers.IO) {
                blockedWithInfo = blockedContactsList.map { number ->
                    val contact = try { contactsRepo.getContactByNumber(number) } catch (_: Exception) { null }
                    Triple(number, contact?.name ?: number, contact?.photoUri)
                }
            }
            isListLoading = false
        }

        val filteredBlocked = remember(blockedWithInfo, listSearchQuery) {
            if (listSearchQuery.isBlank()) blockedWithInfo
            else blockedWithInfo.filter { (num, name, _) ->
                name.contains(listSearchQuery, ignoreCase = true) || num.contains(listSearchQuery)
            }
        }

        val maxBlockListHeightDp = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        val canUseSystem = remember(context) { com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.canUseSystemBlockList(context) }

        Dialog(onDismissRequest = { showBlockListDialog = false }) {
            com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .heightIn(max = maxBlockListHeightDp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                "Blocked Numbers",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    "${blockedContactsList.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { showBlockListDialog = false; showBlockedNumbersDialog = true },
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Outlined.Add, "Add number", modifier = Modifier.size(18.dp))
                            }
                        }

                        // System Sync Banner
                        if (canUseSystem) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Sync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "System-wide blocking active",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.openSystemBlockedNumbers(context)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("System Settings", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Search when there are multiple blocked numbers
                        if (blockedContactsList.size > 3) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextField(
                                    value = listSearchQuery,
                                    onValueChange = { listSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search blocked list…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        if (listSearchQuery.isNotBlank()) {
                                            IconButton(onClick = { listSearchQuery = "" }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        // List Content
                        Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(min = 160.dp, max = 340.dp)) {
                            if (blockedContactsList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.PersonOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "No numbers blocked",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        FilledTonalButton(
                                            onClick = { showBlockListDialog = false; showBlockedNumbersDialog = true },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Block a number", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else if (filteredBlocked.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No matching blocked numbers",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(filteredBlocked, key = { _, item -> item.first }) { _, (number, name, photoUri) ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RivoAvatar(
                                                    name = name,
                                                    photoUri = photoUri,
                                                    modifier = Modifier.size(38.dp),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1
                                                    )
                                                    if (name != number) {
                                                        Text(
                                                            number,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.unblock(context, prefs, number)
                                                        blockedContactsList = com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.getBlockedList(context, prefs)
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Unblock",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showBlockListDialog = false },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Close", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Backup Dialogs ────────────────────────────────────────────────────────
    if (showBackupDialog) {
        CreateBackupDialog(
            onDismiss = { showBackupDialog = false },
            onShare = { backupSettings, backupCallingCards, backupNotes, backupContactGroups, backupRecordings, backupContacts, backupCallLogs ->
                showBackupDialog = false
                backupState = BackupDialogState.Creating
                scope.launch(Dispatchers.IO) {
                    val file = BackupManager.createBackup(
                        context,
                        backupSettings,
                        backupCallingCards,
                        backupNotes,
                        backupContactGroups,
                        backupRecordings,
                        backupContacts,
                        backupCallLogs
                    )
                    withContext(Dispatchers.Main) {
                        if (file != null) {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                            backupState = BackupDialogState.BackupSuccess(file.absolutePath)
                        } else {
                            backupState = BackupDialogState.Error("Failed to create backup")
                        }
                    }
                }
            },
            onSave = { backupSettings, backupCallingCards, backupNotes, backupContactGroups, backupRecordings, backupContacts, backupCallLogs ->
                showBackupDialog = false
                pendingBackupSettings = backupSettings
                pendingBackupCallingCards = backupCallingCards
                pendingBackupNotes = backupNotes
                pendingBackupContactGroups = backupContactGroups
                pendingBackupRecordings = backupRecordings
                pendingBackupContacts = backupContacts
                pendingBackupCallLogs = backupCallLogs
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                saveBackupLauncher.launch("EverDialer_Backup_$timestamp.everdialer")
            }
        )
    }

    if (showRestoreDialog && pendingRestoreContents != null && pendingRestoreFile != null) {
        val restoreFile = pendingRestoreFile!!
        val contents = pendingRestoreContents!!
        RestoreBackupDialog(
            contents = contents,
            onDismiss = {
                showRestoreDialog = false
                pendingRestoreFile?.delete()
                pendingRestoreFile = null
                pendingRestoreContents = null
            },
            onRestore = { restoreSettings, restoreCallingCards, restoreNotes, restoreContactGroups, restoreRecordings, restoreContacts, restoreCallLogs ->
                showRestoreDialog = false
                backupState = BackupDialogState.Restoring
                scope.launch(Dispatchers.IO) {
                    try {
                        val ok = BackupManager.restoreBackup(
                            context,
                            restoreFile,
                            restoreSettings,
                            restoreCallingCards,
                            restoreNotes,
                            restoreContactGroups,
                            restoreRecordings,
                            restoreContacts,
                            restoreCallLogs
                        )
                        restoreFile.delete()
                        withContext(Dispatchers.Main) {
                            pendingRestoreFile = null
                            pendingRestoreContents = null
                            backupState = if (ok) BackupDialogState.RestoreSuccess else BackupDialogState.Error("Restore failed")
                        }
                    } catch (e: Exception) {
                        restoreFile.delete()
                        withContext(Dispatchers.Main) {
                            pendingRestoreFile = null
                            pendingRestoreContents = null
                            backupState = BackupDialogState.Error(e.message ?: "Unknown error")
                        }
                    }
                }
            }
        )
    }

    when (val state = backupState) {
        is BackupDialogState.Creating -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Creating backup…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is BackupDialogState.Restoring -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Restoring backup…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is BackupDialogState.BackupSuccess -> AlertDialog(onDismissRequest = { backupState = BackupDialogState.Idle }, icon = { Icon(Icons.Default.CheckCircle, null, tint = ColorGreen) }, title = { Text("Backup created") }, text = { Text("Backup saved to:\n${state.path}") }, confirmButton = { TextButton(onClick = { backupState = BackupDialogState.Idle }) { Text("OK") } })
        is BackupDialogState.RestoreSuccess -> AlertDialog(onDismissRequest = { backupState = BackupDialogState.Idle }, icon = { Icon(Icons.Default.CheckCircle, null, tint = ColorGreen) }, title = { Text("Restore complete") }, text = { Text("Your data has been restored successfully. Please restart the app.") }, confirmButton = { TextButton(onClick = { backupState = BackupDialogState.Idle }) { Text("OK") } })
        is BackupDialogState.Error -> AlertDialog(onDismissRequest = { backupState = BackupDialogState.Idle }, icon = { Icon(Icons.Default.Error, null, tint = ColorRed) }, title = { Text("Operation failed") }, text = { Text(state.message) }, confirmButton = { TextButton(onClick = { backupState = BackupDialogState.Idle }) { Text("OK") } })
        else -> {}
    }

    // ── Settings-only search ─────────────────────────────────────────────────
    // Deliberately separate from the app-wide unified search (Contacts / Non contacts / Notes /
    // Recordings) — typing here only ever searches settings screens and toggles, never contacts
    // or notes, and there's no Filter button since there's nothing to filter by category.
    var settingsSearchQuery by remember { mutableStateOf("") }
    val settingsSearchEntries = globalSettingsSearchEntries
    val filteredSettingsResults = remember(settingsSearchQuery) {
        val q = settingsSearchQuery.trim()
        if (q.isBlank()) emptyList()
        else settingsSearchEntries.filter {
            com.coolappstore.everdialer.by.svhp.controller.util.matchesFuzzySearch(it.title, q) ||
                    com.coolappstore.everdialer.by.svhp.controller.util.matchesFuzzySearch(it.subtitle, q)
        }
    }
    // The key of the setting row that should scroll into view and flash, most recently
    // requested from a search result tap. Rows read this via settingsSearchHighlight().
    // Seeded from the `highlightKey` nav arg when arriving here from a search result tapped
    // on a different settings page (see SettingsSearchEntryPoint).
    var highlightedSettingKey by remember { mutableStateOf(highlightKey) }
    // The main settings list below is a LazyColumn: rows far down (e.g. "About Ever Dialer")
    // simply aren't composed until scrolled near, so settingsSearchHighlight()'s
    // BringIntoViewRequester silently has nothing to scroll to and a search-result tap on a
    // far-down setting appeared to do nothing. Each `item { }` block below corresponds to one
    // entry here, in the same order, so we can resolve a highlighted key to its containing
    // item's index and jump the list there first — after that, the row itself is composed and
    // settingsSearchHighlight can bring it precisely into view and flash it.
    LaunchedEffect(highlightedSettingKey) {
        val key = highlightedSettingKey
        if (key != null && settingsSearchQuery.isBlank()) {
            val sectionIndex = settingsSectionKeyGroups.indexOfFirst { key in it }
            if (sectionIndex >= 0) {
                val searchFieldItem = 1
                val bannerItem = if (!isDefaultDialer) 1 else 0
                listState.scrollToItem((searchFieldItem + bannerItem + sectionIndex).coerceAtLeast(0))
            }
        }
    }
    LaunchedEffect(settingsSearchQuery.isNotBlank()) {
        if (settingsSearchQuery.isNotBlank()) {
            listState.scrollToItem(0)
        }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar(
                title = "Settings",
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        ScrollHapticsEffect(listState = listState)
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            // While showing search results, every visible item below the search field is one
            // row of the same result group, so the list-wide gap must be 0 there — otherwise the
            // grouped rows render with a visible gap between them despite [groupedRowShape]
            // making them look like a single continuous card. A separate Spacer item restores the
            // normal 16dp gap between the search field and the results/empty-state below it.
            verticalArrangement = if (settingsSearchQuery.isNotBlank()) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(16.dp)

        ) {

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().wpTurnstileControl(delayMs = 30),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Feature count pill badge above search bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Total features: ${settingsSearchEntries.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Main Search Box
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = settingsSearchQuery,
                            onValueChange = { settingsSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search settings") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                AnimatedVisibility(visible = settingsSearchQuery.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                                    IconButton(onClick = { settingsSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            if (settingsSearchQuery.isNotBlank()) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                if (filteredSettingsResults.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No settings found for \"$settingsSearchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Each matched row is its own LazyColumn item (itemsIndexed) instead of being
                    // eagerly forEach-composed inside a single non-lazy item {}. That non-lazy
                    // pattern is what made typing feel laggy/hardcoded: every keystroke recomposed
                    // and measured every matched row at once before the frame could show the new
                    // character. Rows are now composed/measured only as they scroll into view,
                    // while [groupedRowShape] keeps the same rounded-card look as RivoExpressiveCard
                    // — and since verticalArrangement is 0dp while searching (see above), there's
                    // no gap between rows either.
                    itemsIndexed(
                        items = filteredSettingsResults,
                        key = { _, entry -> "settings_search_${entry.key}" }
                    ) { index, entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().wpTurnstileItem(index),
                            shape = groupedRowShape(index, filteredSettingsResults.size),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column {
                                RivoListItem(
                                    headline = entry.title,
                                    supporting = entry.subtitle,
                                    leadingIcon = entry.icon,
                                    iconContainerColor = entry.iconContainerColor,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        settingsSearchQuery = ""
                                        if (entry.navigateTo != null) {
                                            entry.navigateTo.invoke(navigator)
                                        } else {
                                            highlightedSettingKey = entry.key
                                        }
                                    }
                                )
                                if (index < filteredSettingsResults.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {

            // ── Default Dialer Warning Banner ──────────────────────────────────
            if (!isDefaultDialer) {
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        Surface(
                            onClick = {
                                DefaultDialerManager.requestDefaultDialer(defaultDialerLauncher, context)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Set as Default Dialer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Required for calls and call log access", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // ── Updates ──────────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        SectionLabel("Updates")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline  = "Check For Updates",
                                supporting = "Current version: v$APP_VERSION",
                                leadingIcon = Icons.Default.SystemUpdate,
                                iconContainerColor = ColorAmber,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("check_for_updates", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = {
                                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.UpdatesScreenDestination)
                                }
                            )
                        }
                    }
                }
            }

            // ── Call Recording (moved here via "Show Recording Menu Below Updates") ──
            if (showRecordingMenuBelowUpdates) item {
                RivoAnimatedSection(delayMs = 10L) {
                    Column {
                        SectionLabel("Call Recording")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Call Recording",
                                supporting = "Open Ever Call Recorder",
                                leadingIcon = Icons.Default.FiberManualRecord,
                                iconContainerColor = Color(0xFFE53935),
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("call_recording", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = {
                                    NavBarVisibilityState.hideForSettingsEntry = true
                                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination(openedFromSettings = true))
                                }
                            )
                        }
                    }
                }
            }

            // ── Rate And Review ───────────────────────────────────────────────
            if (!hideRateAndReview) item {
                RivoAnimatedSection(delayMs = 30L) {
                    Column {
                        SectionLabel("Rate And Review")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Rate and Review",
                                supporting = "Share your feedback about Ever Dialer",
                                leadingIcon = Icons.Default.Star,
                                iconContainerColor = ColorCyan,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("rate_and_review", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSdY2WYWDFfvLScsBBxfCWzozyA_4sHUCzfR1JycfzJKASvbfQ/viewform?usp=header"))
                                    context.startActivity(intent)
                                }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Check Ratings and Reviews",
                                supporting = "See what others are saying about Ever Dialer",
                                leadingIcon = Icons.Default.Reviews,
                                iconContainerColor = ColorGreen,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("check_ratings", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(RatingsWebViewScreenDestination) }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "More Apps",
                                supporting = "Check out other apps from the developer",
                                leadingIcon = Icons.Default.Apps,
                                iconContainerColor = ColorIndigo,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("more_apps", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.MoreAppsWebViewScreenDestination) }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Donate",
                                supporting = "Support this open source project",
                                leadingIcon = Icons.Default.Favorite,
                                iconContainerColor = ColorRed,
                                trailingIcon = Icons.Default.OpenInNew,
                                modifier = Modifier.settingsSearchHighlight("donate", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { showDonateDialog = true }
                            )
                        }
                    }
                }
            }


            // ── Appearance ───────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 60L) {
                    Column {
                        SectionLabel("Appearance")
                        RivoExpressiveCard {
                            RivoListItem(headline = "Interface", supporting = "Themes, colors, and layout", leadingIcon = Icons.Outlined.Palette, iconContainerColor = ColorPurple, trailingIcon = Icons.Default.ChevronRight, modifier = Modifier.settingsSearchHighlight("interface", highlightedSettingKey) { highlightedSettingKey = null }, onClick = { navigator.navigate(InterfaceScreenDestination()) })
                        }
                    }
                }
            }

            // ── Haptics Across App ───────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 80L) {
                    Column {
                        SectionLabel("Haptics Across App")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline   = "Tap Haptics",
                                supporting = if (tapHapticsEnabled) "On · ${hapticsStrength.replaceFirstChar { it.uppercase() }}" else "Off",
                                leadingIcon = Icons.Outlined.Vibration,
                                iconContainerColor = ColorPurple,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("tap_haptics", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { showHapticsDialog = true }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline   = "Scroll Haptics",
                                supporting = "Vibrate on scroll gestures across the app",
                                leadingIcon = Icons.Outlined.SwipeVertical,
                                iconContainerColor = ColorIndigo,
                                checked = scrollHapticsEnabled,
                                modifier = Modifier.settingsSearchHighlight("scroll_haptics", highlightedSettingKey) { highlightedSettingKey = null },
                                onCheckedChange = {
                                    scrollHapticsEnabled = it
                                    prefs.setBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, it)
                                }
                            )
                            AnimatedVisibility(visible = scrollHapticsEnabled) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // ── Slider: Haptic Interval ──
                                    // 1 haptic per X cm. Range 0.5–5.0 cm.
                                    val cmLabel = "1 per %.1f cm".format(scrollCmPerHaptic)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Haptic Interval",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = cmLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Slider(
                                        value = scrollCmPerHaptic,
                                        onValueChange = { v ->
                                            val snapped = (v * 10f).roundToInt() / 10f
                                            scrollCmPerHaptic = snapped
                                            prefs.setFloat(PreferenceManager.KEY_SCROLL_CM_PER_HAPTIC, snapped)
                                        },
                                        valueRange = 0.5f..5.0f,
                                        steps = 44,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // ── Slider: Haptic Strength ──
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Haptic Strength",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = scrollHapticStrength.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Slider(
                                        value = scrollHapticStrength.toFloat(),
                                        onValueChange = { v ->
                                            val snapped = v.roundToInt().coerceIn(1, 255)
                                            scrollHapticStrength = snapped
                                            prefs.setInt(PreferenceManager.KEY_SCROLL_HAPTIC_STRENGTH, snapped)
                                        },
                                        valueRange = 1f..255f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Authentication ───────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 110L) {
                    Column {
                        SectionLabel("Authentication")
                        RivoExpressiveCard {
                            val biometricsType = remember(prefs.settingsChanged.collectAsState().value) {
                                prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""
                            }
                            val biometricsLabel = when (biometricsType) {
                                "system"   -> "System Biometrics"
                                "pin"      -> "Custom PIN"
                                "password" -> "Custom Password"
                                else       -> "Not configured"
                            }
                            RivoListItem(
                                headline   = "Authentication",
                                supporting = biometricsLabel,
                                leadingIcon = Icons.Default.Fingerprint,
                                iconContainerColor = Color(0xFF6750A4),
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("authentication", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(BiometricScreenDestination()) }
                            )
                        }
                    }
                }
            }

            // ── Calls & System ───────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 140L) {
                    Column {
                        SectionLabel("Calls & System")

                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Interesting Settings !",
                                supporting = "Call features, network switcher, and notes",
                                leadingIcon = Icons.Outlined.Tune,
                                iconContainerColor = ColorTeal,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("app_settings", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(AppSettingsScreenDestination()) }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Sim And Call Placement",
                                supporting = "Default SIM, SIM colors, confirm calls, and contacts",
                                leadingIcon = Icons.Outlined.SimCard,
                                iconContainerColor = ColorGreen,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("sim_and_call_placement", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(SimAndCallPlacementScreenDestination()) }
                            )
                            CardDivider()
                            val hiderMenuHidden = remember(prefs.settingsChanged.collectAsState().value) {
                                prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_MENU, false)
                            }
                            AnimatedVisibility(visible = !hiderMenuHidden) {
                                Column {
                                    RivoListItem(
                                        headline = "Contacts Hider",
                                        supporting = "Hide contacts behind a secret code",
                                        leadingIcon = Icons.Outlined.Lock,
                                        iconContainerColor = Color(0xFF5E35B1),
                                        trailingIcon = Icons.Default.ChevronRight,
                                        modifier = Modifier.settingsSearchHighlight("contacts_hider", highlightedSettingKey) { highlightedSettingKey = null },
                                        onClick = { navigator.navigate(ContactsHiderScreenDestination) }
                                    )
                                    CardDivider()
                                }
                            }
                            RivoListItem(
                                headline = "Fake Call",
                                supporting = "Schedule fake incoming calls without calling the real person",
                                leadingIcon = Icons.Outlined.PhoneCallback,
                                iconContainerColor = ColorRed,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("fake_call", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(FakeCallScreenDestination) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Call Recording (bundled Ever Call Recorder app) ────
                        if (!showRecordingMenuBelowUpdates) {
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Call Recording",
                                supporting = "Open Ever Call Recorder",
                                leadingIcon = Icons.Default.FiberManualRecord,
                                iconContainerColor = Color(0xFFE53935),
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("call_recording", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = {
                                    // Flip this before navigating (not inside RecordingsScreen's own
                                    // effect) so BottomBar never sees a frame where the destination
                                    // looks like a disabled tab it should redirect away from — even
                                    // when the "Recordings" tab has been hidden via Tab Sections.
                                    NavBarVisibilityState.hideForSettingsEntry = true
                                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination(openedFromSettings = true))
                                }
                            )
                        }
                        }
                    }
                }
            }

            // ── Spam ─────────────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 180L) {
                    Column {
                        SectionLabel("Spam")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline   = "Silence Unknown Callers",
                                supporting = "Automatically decline calls from unknown numbers",
                                leadingIcon = Icons.Outlined.PhoneDisabled,
                                iconContainerColor = ColorRed,
                                checked = silenceUnknown,
                                modifier = Modifier.settingsSearchHighlight("silence_unknown", highlightedSettingKey) { highlightedSettingKey = null },
                                onCheckedChange = {
                                    silenceUnknown = it
                                    prefs.setBoolean(PreferenceManager.KEY_SILENCE_UNKNOWN, it)
                                }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Blocked Numbers",
                                supporting = if (blockedContactsList.isEmpty()) "No numbers blocked"
                                             else "${blockedContactsList.size} number(s) blocked",
                                leadingIcon = Icons.Outlined.PersonOff,
                                iconContainerColor = ColorBluGrey,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("blocked_numbers", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { showBlockListDialog = true }
                            )
                        }
                    }
                }
            }


            // ── Backup & Restore ─────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 260L) {
                    Column {
                        SectionLabel("Backup & Restore")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline   = "Create Backup",
                                supporting = "Save app configuration, settings and calling cards",
                                leadingIcon = Icons.Default.Backup,
                                iconContainerColor = ColorGreen,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("create_backup", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = {
                                    showBackupDialog = true
                                }
                            )
                            CardDivider()
                            RivoListItem(headline = "Restore Backup", supporting = "Restore app configuration, settings and calling cards", leadingIcon = Icons.Default.Restore, iconContainerColor = ColorBrown, trailingIcon = Icons.Default.ChevronRight, modifier = Modifier.settingsSearchHighlight("restore_backup", highlightedSettingKey) { highlightedSettingKey = null }, onClick = { restoreLauncher.launch("*/*") })
                        }
                    }
                }
            }

            // ── About ────────────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 300L) {
                    Column {
                        SectionLabel("About")
                        RivoExpressiveCard {
                            RivoListItem(headline = "About Ever Dialer", supporting = "Version $APP_VERSION · Developer info", leadingIcon = Icons.Outlined.Info, iconContainerColor = ColorBluGrey, trailingIcon = Icons.Default.ChevronRight, modifier = Modifier.settingsSearchHighlight("about_app", highlightedSettingKey) { highlightedSettingKey = null }, onClick = { navigator.navigate(AboutAppScreenDestination()) })
                        }
                    }
                }
            }

            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

private sealed class BackupDialogState {
    object Idle : BackupDialogState()
    object Creating : BackupDialogState()
    object Restoring : BackupDialogState()
    data class BackupSuccess(val path: String) : BackupDialogState()
    object RestoreSuccess : BackupDialogState()
    data class Error(val message: String) : BackupDialogState()
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
internal fun CardDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Corner shape for a row inside a visually-grouped "card" of lazily rendered settings-search
 * results — rounded only on the outer edge of the first/last row in the group so consecutive
 * rows still read as one continuous card, just like [RivoExpressiveCard], while each row is its
 * own LazyColumn item (see the settings-search results in [SettingsScreen]).
 */
private fun groupedRowShape(index: Int, count: Int, corner: androidx.compose.ui.unit.Dp = 28.dp): androidx.compose.ui.graphics.Shape {
    val top = if (index == 0) corner else 0.dp
    val bottom = if (index == count - 1) corner else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onShare: (backupSettings: Boolean, backupCallingCards: Boolean, backupNotes: Boolean, backupContactGroups: Boolean, backupRecordings: Boolean, backupContacts: Boolean, backupCallLogs: Boolean) -> Unit,
    onSave: (backupSettings: Boolean, backupCallingCards: Boolean, backupNotes: Boolean, backupContactGroups: Boolean, backupRecordings: Boolean, backupContacts: Boolean, backupCallLogs: Boolean) -> Unit
) {
    var backupSettings by remember { mutableStateOf(true) }
    var backupCallingCards by remember { mutableStateOf(true) }
    var backupNotes by remember { mutableStateOf(true) }
    var backupContactGroups by remember { mutableStateOf(false) }
    var backupRecordings by remember { mutableStateOf(true) }
    var backupContacts by remember { mutableStateOf(false) }
    var backupCallLogs by remember { mutableStateOf(false) }

    val hasAnySelected = backupSettings || backupCallingCards || backupNotes || backupContactGroups || backupRecordings || backupContacts || backupCallLogs
    val maxContainerHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp

    val prefs = koinInject<PreferenceManager>()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Create Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Select items to include in your backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxContainerHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        RivoExpressiveCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            RivoSwitchListItem(
                                headline = "Backup settings",
                                supporting = "App preferences, Call Recorder & Network Switcher settings",
                                leadingIcon = Icons.Outlined.Settings,
                                iconContainerColor = Color(0xFF4CAF50),
                                checked = backupSettings,
                                onCheckedChange = { backupSettings = it }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Backup calling cards",
                                supporting = "Saved calling cards, contact backgrounds and PFP customization",
                                leadingIcon = Icons.Outlined.ContactPhone,
                                iconContainerColor = Color(0xFF2196F3),
                                checked = backupCallingCards,
                                onCheckedChange = { backupCallingCards = it }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Backup notes",
                                supporting = "Contact notes and general standalone notes",
                                leadingIcon = Icons.Outlined.Notes,
                                iconContainerColor = Color(0xFF9C27B0),
                                checked = backupNotes,
                                onCheckedChange = { backupNotes = it }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Backup contact groups",
                                supporting = "Custom contact groups and shown contact references",
                                leadingIcon = Icons.Outlined.Groups,
                                iconContainerColor = Color(0xFF3F51B5),
                                checked = backupContactGroups,
                                onCheckedChange = { backupContactGroups = it }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Backup call recordings",
                                supporting = "Audio recordings, favourites and recording notes",
                                leadingIcon = Icons.Outlined.Mic,
                                iconContainerColor = Color(0xFFE53935),
                                checked = backupRecordings,
                                onCheckedChange = { backupRecordings = it }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Backup contacts",
                                supporting = "Device & local contacts list and details",
                                leadingIcon = Icons.Outlined.Contacts,
                                iconContainerColor = Color(0xFFFF9800),
                                checked = backupContacts,
                                onCheckedChange = { backupContacts = it }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Backup call logs",
                                supporting = "Device call history and logs",
                                leadingIcon = Icons.Outlined.History,
                                iconContainerColor = Color(0xFF009688),
                                checked = backupCallLogs,
                                onCheckedChange = { backupCallLogs = it }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onShare(backupSettings, backupCallingCards, backupNotes, backupContactGroups, backupRecordings, backupContacts, backupCallLogs) },
                            enabled = hasAnySelected,
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        Button(
                            onClick = { onSave(backupSettings, backupCallingCards, backupNotes, backupContactGroups, backupRecordings, backupContacts, backupCallLogs) },
                            enabled = hasAnySelected,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreBackupDialog(
    contents: BackupManager.BackupContents,
    onDismiss: () -> Unit,
    onRestore: (restoreSettings: Boolean, restoreCallingCards: Boolean, restoreNotes: Boolean, restoreContactGroups: Boolean, restoreRecordings: Boolean, restoreContacts: Boolean, restoreCallLogs: Boolean) -> Unit
) {
    var restoreSettings by remember { mutableStateOf(contents.hasSettings) }
    var restoreCallingCards by remember { mutableStateOf(contents.hasCallingCards) }
    var restoreNotes by remember { mutableStateOf(contents.hasNotes) }
    var restoreContactGroups by remember { mutableStateOf(contents.hasContactGroups) }
    var restoreRecordings by remember { mutableStateOf(contents.hasRecordings) }
    var restoreContacts by remember { mutableStateOf(contents.hasContacts) }
    var restoreCallLogs by remember { mutableStateOf(contents.hasCallLogs) }

    val hasAnySelected = (restoreSettings && contents.hasSettings) ||
            (restoreCallingCards && contents.hasCallingCards) ||
            (restoreNotes && contents.hasNotes) ||
            (restoreContactGroups && contents.hasContactGroups) ||
            (restoreRecordings && contents.hasRecordings) ||
            (restoreContacts && contents.hasContacts) ||
            (restoreCallLogs && contents.hasCallLogs)

    val maxContainerHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp

    val prefs = koinInject<PreferenceManager>()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity(prefs = prefs) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Restore Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Select items to restore from this backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxContainerHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        RivoExpressiveCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            RivoSwitchListItem(
                                headline = "Restore settings",
                                supporting = if (contents.hasSettings) "App preferences, Call Recorder & Network Switcher settings" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.Settings,
                                iconContainerColor = Color(0xFF4CAF50),
                                checked = restoreSettings,
                                onCheckedChange = { restoreSettings = it },
                                enabled = contents.hasSettings
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Restore calling cards",
                                supporting = if (contents.hasCallingCards) "Saved calling cards, contact backgrounds and PFP customization" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.ContactPhone,
                                iconContainerColor = Color(0xFF2196F3),
                                checked = restoreCallingCards,
                                onCheckedChange = { restoreCallingCards = it },
                                enabled = contents.hasCallingCards
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Restore notes",
                                supporting = if (contents.hasNotes) "Contact notes and general standalone notes" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.Notes,
                                iconContainerColor = Color(0xFF9C27B0),
                                checked = restoreNotes,
                                onCheckedChange = { restoreNotes = it },
                                enabled = contents.hasNotes
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Restore contact groups",
                                supporting = if (contents.hasContactGroups) "Custom contact groups and shown contact references" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.Groups,
                                iconContainerColor = Color(0xFF3F51B5),
                                checked = restoreContactGroups,
                                onCheckedChange = { restoreContactGroups = it },
                                enabled = contents.hasContactGroups
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Restore call recordings",
                                supporting = if (contents.hasRecordings) "Audio recordings, favourites and recording notes" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.Mic,
                                iconContainerColor = Color(0xFFE53935),
                                checked = restoreRecordings,
                                onCheckedChange = { restoreRecordings = it },
                                enabled = contents.hasRecordings
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Restore contacts",
                                supporting = if (contents.hasContacts) "Device & local contacts list and details" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.Contacts,
                                iconContainerColor = Color(0xFFFF9800),
                                checked = restoreContacts,
                                onCheckedChange = { restoreContacts = it },
                                enabled = contents.hasContacts
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline = "Restore call logs",
                                supporting = if (contents.hasCallLogs) "Device call history and logs" else "Not present in this backup",
                                leadingIcon = Icons.Outlined.History,
                                iconContainerColor = Color(0xFF009688),
                                checked = restoreCallLogs,
                                onCheckedChange = { restoreCallLogs = it },
                                enabled = contents.hasCallLogs
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onRestore(
                                    restoreSettings,
                                    restoreCallingCards,
                                    restoreNotes,
                                    restoreContactGroups,
                                    restoreRecordings,
                                    restoreContacts,
                                    restoreCallLogs
                                )
                            },
                            enabled = hasAnySelected,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
