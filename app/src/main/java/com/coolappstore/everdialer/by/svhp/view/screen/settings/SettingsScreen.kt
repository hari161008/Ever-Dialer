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
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.controller.util.BackupManager
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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.generated.destinations.CallSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

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
@Destination<RootGraph>
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
            prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }
    // Keep this in sync if a number gets blocked/unblocked elsewhere (Calls tab or Contacts tab
    // context menus) while this screen is alive in the back stack.
    LaunchedEffect(rateReviewSettingsVersion) {
        blockedContactsList = prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    var backupState       by remember { mutableStateOf<BackupDialogState>(BackupDialogState.Idle) }

    var visible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    fun navigateBack() {
        isClosing = true
        scope.launch {
            delay(280)
            navigator.navigateUp()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible && !isClosing) 1f else 0f,
        animationSpec = if (isClosing) tween(280, easing = FastOutLinearInEasing) else tween(350),
        label = "settingsAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible && !isClosing) 0.dp else if (isClosing) 60.dp else 30.dp,
        animationSpec = if (isClosing) tween(300, easing = FastOutLinearInEasing)
                        else spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "settingsOffsetY"
    )
    LaunchedEffect(Unit) { visible = true }

    // Restore file picker
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                backupState = BackupDialogState.Restoring
                try {
                    val tmpFile = File(context.cacheDir, "restore_tmp.everdialer")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val ok = BackupManager.restoreBackup(context, tmpFile)
                    tmpFile.delete()
                    backupState = if (ok) BackupDialogState.RestoreSuccess else BackupDialogState.Error("Restore failed")
                } catch (e: Exception) {
                    backupState = BackupDialogState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    // Default dialer
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
    var isDefaultDialer by remember { mutableStateOf(telecomManager.defaultDialerPackage == context.packageName) }
    val defaultDialerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultDialer = telecomManager.defaultDialerPackage == context.packageName
    }
    val activity = context as? Activity
    DisposableEffect(activity) {
        val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
                isDefaultDialer = telecomManager.defaultDialerPackage == context.packageName
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    // Ever Call Recorder is now bundled directly inside this app (no separate install needed).

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
    if (showBlockedNumbersDialog) {
        val callLogRepo: ICallLogRepository = koinInject()
        val contactsRepo: IContactsRepository = koinInject()

        var recentNumbers by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var contactNumbers by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        var searchQuery by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            isLoading = true
            try {
                val logs = callLogRepo.getCallLogs()
                val seen = mutableSetOf<String>()
                val result = mutableListOf<Triple<String, String, String?>>()
                for (log in logs) {
                    val num = log.number
                    if (num.isBlank() || !seen.add(num)) continue
                    val contact = try { contactsRepo.getContactByNumber(num) } catch (_: Exception) { null }
                    result.add(Triple(num, contact?.name ?: num, contact?.photoUri))
                }
                recentNumbers = result
            } catch (_: Exception) {}
            try {
                contactNumbers = contactsRepo.getContacts()
                    .filter { it.phoneNumbers.isNotEmpty() }
                    .flatMap { c -> c.phoneNumbers.map { num -> Triple(num, c.name, c.photoUri) } }
                    .distinctBy { it.first }
                    .sortedBy { it.second }
            } catch (_: Exception) {}
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
            if (!blockedContactsList.contains(number)) {
                com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.block(context, prefs, number)
                blockedContactsList = blockedContactsList + number
            }
        }

        val maxDialogHeightDp = LocalConfiguration.current.screenHeightDp.dp * 0.82f
        Dialog(onDismissRequest = { showBlockedNumbersDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(max = maxDialogHeightDp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Block, null, tint = ColorRed, modifier = Modifier.size(20.dp))
                        Text("Block a Number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search name or number…", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotBlank(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) }
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Call Logs", "Contacts", "Manual").forEachIndexed { index, label ->
                            val selected = blockedNumbersTab == index
                            val bgColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                label = "tabBg"
                            )
                            val txtColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "tabTxt"
                            )
                            Surface(
                                onClick = { blockedNumbersTab = index },
                                shape = RoundedCornerShape(50),
                                color = bgColor,
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = txtColor
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

                    Box(modifier = Modifier.heightIn(min = 80.dp, max = 320.dp)) {
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
                                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                                    Text("Loading call logs…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        } else if (filteredRecents.isEmpty()) {
                                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                                Text(if (searchQuery.isBlank()) "No call logs found." else "No results for \"$searchQuery\"",
                                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        } else {
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                items(filteredRecents, key = { it.first }) { (number, name, photoUri) ->
                                                    val alreadyBlocked = blockedContactsList.contains(number)
                                                    Surface(
                                                        modifier = Modifier.animateItem(),
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(34.dp))
                                                            Spacer(Modifier.width(10.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                                                if (name != number) Text(number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                            }
                                                            TextButton(onClick = { blockNumber(number) }, enabled = !alreadyBlocked, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                                                Text(if (alreadyBlocked) "Blocked" else "Block", style = MaterialTheme.typography.labelSmall, color = if (alreadyBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
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
                                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                                    Text("Loading contacts…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        } else if (filteredContacts.isEmpty()) {
                                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                                Text(if (searchQuery.isBlank()) "No contacts found." else "No results for \"$searchQuery\"",
                                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        } else {
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                items(filteredContacts, key = { it.first }) { (number, name, photoUri) ->
                                                    val alreadyBlocked = blockedContactsList.contains(number)
                                                    Surface(
                                                        modifier = Modifier.animateItem(),
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(34.dp))
                                                            Spacer(Modifier.width(10.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                                                if (name != number) Text(number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                            }
                                                            TextButton(onClick = { blockNumber(number) }, enabled = !alreadyBlocked, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                                                Text(if (alreadyBlocked) "Blocked" else "Block", style = MaterialTheme.typography.labelSmall, color = if (alreadyBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = blockedNumberInput,
                                            onValueChange = { blockedNumberInput = it },
                                            label = { Text("Enter number to block") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            trailingIcon = {
                                                AnimatedVisibility(
                                                    visible = blockedNumberInput.isNotBlank(),
                                                    enter = fadeIn() + scaleIn(),
                                                    exit = fadeOut() + scaleOut()
                                                ) {
                                                    IconButton(onClick = {
                                                        val num = blockedNumberInput.trim()
                                                        if (num.isNotBlank()) blockNumber(num)
                                                        blockedNumberInput = ""
                                                    }) { Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary) }
                                                }
                                            }
                                        )
                                        Button(
                                            onClick = {
                                                val num = blockedNumberInput.trim()
                                                if (num.isNotBlank()) blockNumber(num)
                                                blockedNumberInput = ""
                                            },
                                            enabled = blockedNumberInput.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(50)
                                        ) { Text("Block Number") }
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showBlockedNumbersDialog = false }) { Text("Done") }
                    }
                }
            }
        }
    }

    // ── Block List Detail Dialog ───────────────────────────────────────────────
    if (showBlockListDialog) {
        val contactsRepo: IContactsRepository = koinInject()
        var blockedWithInfo by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
        LaunchedEffect(blockedContactsList) {
            blockedWithInfo = blockedContactsList.map { number ->
                val contact = try { contactsRepo.getContactByNumber(number) } catch (_: Exception) { null }
                Triple(number, contact?.name ?: number, contact?.photoUri)
            }
        }

        val maxBlockListHeightDp = LocalConfiguration.current.screenHeightDp.dp * 0.82f
        Dialog(onDismissRequest = { showBlockListDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(max = maxBlockListHeightDp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Block, null, tint = ColorRed, modifier = Modifier.size(20.dp))
                        Text("Blocked Numbers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(50), color = ColorRed.copy(alpha = 0.12f)) {
                            Text("${blockedContactsList.size}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ColorRed, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                        IconButton(
                            onClick = { showBlockListDialog = false; showBlockedNumbersDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add number", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

                    if (blockedContactsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f), modifier = Modifier.size(40.dp))
                                Text("No numbers blocked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showBlockListDialog = false; showBlockedNumbersDialog = true }) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Block a number")
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(blockedWithInfo) { index, (number, name, photoUri) ->
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(38.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            if (name != number) Text(number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        IconButton(onClick = {
                                            com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.unblock(context, prefs, number)
                                            blockedContactsList = blockedContactsList.toMutableList().also { it.removeAt(index) }
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Close, "Remove", tint = ColorRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showBlockListDialog = false }) { Text("Close") }
                    }
                }
            }
        }
    }

    // ── Backup Dialogs ────────────────────────────────────────────────────────
    when (val state = backupState) {
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
    val settingsSearchEntries = listOf(
        // ── Rows that live directly on this screen ──────────────────────────
        SettingsSearchEntry("Check For Updates", "Current version: v$APP_VERSION", "check_for_updates", Icons.Default.SystemUpdate, ColorAmber),
        SettingsSearchEntry("Rate and Review", "Share your feedback about Ever Dialer", "rate_and_review", Icons.Default.Star, ColorCyan),
        SettingsSearchEntry("Check Ratings and Reviews", "See what others are saying about Ever Dialer", "check_ratings", Icons.Default.Reviews, ColorGreen),
        SettingsSearchEntry("More Apps", "Check out other apps from the developer", "more_apps", Icons.Default.Apps, ColorIndigo),
        SettingsSearchEntry("Donate", "Support this open source project", "donate", Icons.Default.Favorite, ColorRed),
        SettingsSearchEntry("Interface", "Themes, colors, and layout", "interface", Icons.Outlined.Palette, ColorPurple),
        SettingsSearchEntry("Tap Haptics", "Vibration on taps across the app", "tap_haptics", Icons.Outlined.Vibration, ColorPurple),
        SettingsSearchEntry("Scroll Haptics", "Vibrate on scroll gestures across the app", "scroll_haptics", Icons.Outlined.SwipeVertical, ColorIndigo),
        SettingsSearchEntry("Authentication", "App lock, biometrics, and PIN/password", "authentication", Icons.Default.Fingerprint, Color(0xFF6750A4)),
        SettingsSearchEntry("App Settings", "Call settings, network switcher, and notes", "app_settings", Icons.Outlined.Tune, ColorTeal),
        SettingsSearchEntry("Contacts Hider", "Hide contacts behind a secret code", "contacts_hider", Icons.Outlined.Lock, Color(0xFF5E35B1)),
        SettingsSearchEntry("Fake Call", "Schedule fake incoming calls without calling the real person", "fake_call", Icons.Outlined.PhoneCallback, ColorRed),
        SettingsSearchEntry("Call Recording", "Open Ever Call Recorder", "call_recording", Icons.Default.FiberManualRecord, Color(0xFFE53935)),
        SettingsSearchEntry("Silence Unknown Callers", "Automatically decline calls from unknown numbers", "silence_unknown", Icons.Outlined.PhoneDisabled, ColorRed),
        SettingsSearchEntry("Blocked Numbers", "Numbers you've blocked from calling you", "blocked_numbers", Icons.Outlined.PersonOff, ColorBluGrey),
        SettingsSearchEntry("Auto Check For Updates", "Automatically check for updates when the app opens", "auto_check_updates", Icons.Default.Autorenew, ColorAmber) { it.navigate(UpdatesScreenDestination) },
        SettingsSearchEntry("Create Backup", "Save app configuration and notes", "create_backup", Icons.Default.Backup, ColorGreen),
        SettingsSearchEntry("Restore Backup", "Restore app configuration and notes", "restore_backup", Icons.Default.Restore, ColorBrown),
        SettingsSearchEntry("About Ever Dialer", "Version $APP_VERSION · Developer info", "about_app", Icons.Outlined.Info, ColorBluGrey),

        // ── App Settings screen ──────────────────────────────────────────────
        SettingsSearchEntry("Call Settings", "SIM, contacts to display, call behavior", "nav_call_settings", Icons.Outlined.Call, ColorTeal) { it.navigate(AppSettingsScreenDestination(highlightKey = "nav_call_settings")) },
        SettingsSearchEntry("4G/5G Switcher", "Quickly switch network mode per app", "network_switcher", Icons.Outlined.NetworkCell, ColorBlue) { it.navigate(AppSettingsScreenDestination(highlightKey = "network_switcher")) },
        SettingsSearchEntry("Integrate Notes Section", "Show notes alongside call recordings", "integrate_notes", Icons.Outlined.Notes, ColorGreen) { it.navigate(AppSettingsScreenDestination(highlightKey = "integrate_notes")) },
        SettingsSearchEntry("Delete Notes With Recording", "Remove the note when its recording is deleted", "delete_notes_with_recording", Icons.Outlined.NoteAlt, ColorRed) { it.navigate(AppSettingsScreenDestination(highlightKey = "delete_notes_with_recording")) },

        // ── Call Settings screen ─────────────────────────────────────────────
        SettingsSearchEntry("Default SIM", "Which SIM is used to place calls", "default_sim", Icons.Outlined.SimCard, ColorGreen) { it.navigate(CallSettingsScreenDestination(highlightKey = "default_sim")) },
        SettingsSearchEntry("Contacts to display", "Choose which accounts' contacts are shown", "contacts_to_display", Icons.Outlined.Contacts, ColorBlue) { it.navigate(CallSettingsScreenDestination(highlightKey = "contacts_to_display")) },
        SettingsSearchEntry("Proximity Sensor on in background", "Turn off screen when phone is near ear during a call", "proximity_sensor_bg", Icons.Outlined.Sensors, ColorTeal) { it.navigate(CallSettingsScreenDestination(highlightKey = "proximity_sensor_bg")) },
        SettingsSearchEntry("Device Orientation with Proximity Sensor", "Combine orientation and proximity to prevent false screen-offs during a call", "proximity_orientation_bg", Icons.Outlined.ScreenLockPortrait, ColorRed) { it.navigate(CallSettingsScreenDestination(highlightKey = "proximity_orientation_bg")) },
        SettingsSearchEntry("Pocket Mode Prevention", "Block accidental answer/decline when phone is in pocket", "pocket_mode_prevention", Icons.Outlined.Sensors, ColorAmber) { it.navigate(CallSettingsScreenDestination(highlightKey = "pocket_mode_prevention")) },
        SettingsSearchEntry("Floating Ongoing Call", "Draggable floating bubble during calls", "floating_ongoing_call", Icons.Outlined.Sensors, ColorBlue) { it.navigate(CallSettingsScreenDestination(highlightKey = "floating_ongoing_call")) },
        SettingsSearchEntry("Direct Call on Tap", "Tap a call log entry to call directly", "direct_call_on_tap", Icons.Outlined.Call, ColorGreen) { it.navigate(CallSettingsScreenDestination(highlightKey = "direct_call_on_tap")) },
        SettingsSearchEntry("Auto Speaker", "Switch to loudspeaker when phone is away from ear", "auto_speaker", Icons.Outlined.VolumeUp, ColorRed) { it.navigate(CallSettingsScreenDestination(highlightKey = "auto_speaker")) },
        SettingsSearchEntry("Auto Redial", "Automatically redial on rejected/unanswered/busy calls", "auto_redial", Icons.Default.Replay, ColorBlue) { it.navigate(CallSettingsScreenDestination(highlightKey = "auto_redial")) },

        // ── Raise to Answer screen ───────────────────────────────────────────
        SettingsSearchEntry("Enable Raise to Answer", "Answer calls by raising the phone to your ear", "enable_raise_to_answer", Icons.Outlined.Vibration, ColorTeal) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "enable_raise_to_answer")) },
        SettingsSearchEntry("Answer at Any Angle", "Raise to Answer sensitivity", "answer_any_angle", Icons.Outlined.Vibration, ColorTeal) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "answer_any_angle")) },
        SettingsSearchEntry("Decline by Flipping", "Flip the phone face down to decline a call", "decline_by_flipping", Icons.Outlined.Vibration, ColorRed) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "decline_by_flipping")) },
        SettingsSearchEntry("Raise to Answer Beep Feedback", "Play a beep when raise/flip is detected", "raise_beep_feedback", Icons.Outlined.Vibration, ColorAmber) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "raise_beep_feedback")) },
        SettingsSearchEntry("Raise to Answer Vibrate Feedback", "Vibrate when raise/flip is detected", "raise_vibrate_feedback", Icons.Outlined.Vibration, ColorPurple) { it.navigate(RaiseToAnswerScreenDestination(highlightKey = "raise_vibrate_feedback")) },

        // ── Sound & Vibration screen ──────────────────────────────────────────
        SettingsSearchEntry("DTMF Tone", "Play tones when dialing digits", "dtmf_tone", Icons.Outlined.VolumeUp, ColorBlue) { it.navigate(SoundVibrationScreenDestination(highlightKey = "dtmf_tone")) },
        SettingsSearchEntry("Dial Pad Tone", "Choose the dialpad key tone", "dialpad_tone", Icons.Outlined.VolumeUp, ColorTeal) { it.navigate(SoundVibrationScreenDestination(highlightKey = "dialpad_tone")) },
        SettingsSearchEntry("Ringtone Settings", "Choose your incoming call ringtone", "ringtone_settings", Icons.Outlined.VolumeUp, ColorAmber) { it.navigate(SoundVibrationScreenDestination(highlightKey = "ringtone_settings")) },
        SettingsSearchEntry("Do Not Disturb", "Manage Do Not Disturb access", "dnd_settings", Icons.Outlined.VolumeUp, ColorIndigo) { it.navigate(SoundVibrationScreenDestination(highlightKey = "dnd_settings")) },

        // ── Authentication (Biometric) screen ────────────────────────────────
        SettingsSearchEntry("Authentication Method", "System biometrics, PIN, or password", "auth_method", Icons.Default.Fingerprint, Color(0xFF6750A4)) { it.navigate(BiometricScreenDestination(highlightKey = "auth_method")) },
        SettingsSearchEntry("Lock App on Open", "Require authentication whenever the app opens", "lock_app_open", Icons.Default.Fingerprint, ColorRed) { it.navigate(BiometricScreenDestination(highlightKey = "lock_app_open")) },
        SettingsSearchEntry("Lock Call Actions", "Require authentication for sensitive call actions", "lock_call_actions", Icons.Default.Fingerprint, ColorTeal) { it.navigate(BiometricScreenDestination(highlightKey = "lock_call_actions")) },

        // ── Interface screen ──────────────────────────────────────────────────
        SettingsSearchEntry("Dynamic Colors", "Match app colors to your wallpaper (Material You)", "dynamic_colors", Icons.Outlined.Palette, ColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "dynamic_colors")) },
        SettingsSearchEntry("Material Liquid You Glass", "Liquid glass visual effects", "liquid_glass_toggle", Icons.Outlined.Palette, ColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "liquid_glass_toggle")) },
        SettingsSearchEntry("Elements to have liquid glass effect", "Choose where liquid glass effects apply", "liquid_glass_elements_link", Icons.Outlined.Palette, ColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "liquid_glass_elements_link")) },
        SettingsSearchEntry("Material Blur Effects", "Blur effects across the interface", "blur_effects_toggle", Icons.Outlined.Palette, ColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "blur_effects_toggle")) },
        SettingsSearchEntry("Elements to have blur effect", "Choose where blur effects apply", "blur_effects_elements_link", Icons.Outlined.Palette, ColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "blur_effects_elements_link")) },
        SettingsSearchEntry("Hangup Animation", "Animate the screen when a call ends", "hangup_animation", Icons.Outlined.Palette, ColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "hangup_animation")) },
        SettingsSearchEntry("Incoming Call UI", "Customize the incoming call screen", "incoming_call_ui_link", Icons.Outlined.Palette, ColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "incoming_call_ui_link")) },
        SettingsSearchEntry("Ongoing Call UI", "Customize the in-call screen layout", "caller_ui_link", Icons.Outlined.Palette, ColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "caller_ui_link")) },
        SettingsSearchEntry("Calls Section Elements", "Choose what shows in the Calls tab", "calls_section_elements", Icons.Outlined.Palette, ColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "calls_section_elements")) },
        SettingsSearchEntry("Context Menu Elements", "Choose what shows in long-press menus", "context_menu_elements", Icons.Outlined.Palette, ColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "context_menu_elements")) },
        SettingsSearchEntry("Tab Sections", "Choose which bottom tabs are visible", "tab_sections", Icons.Outlined.Palette, ColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "tab_sections")) },
        SettingsSearchEntry("Default Tab Section", "Which tab opens when you launch the app", "default_tab_section", Icons.Outlined.Palette, ColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "default_tab_section")) },
        SettingsSearchEntry("Scroll Animation", "Animate list scrolling", "scroll_animation", Icons.Outlined.Palette, ColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "scroll_animation")) },
        SettingsSearchEntry("Pill Style Navigation", "Pill-shaped bottom navigation bar", "pill_style_nav", Icons.Outlined.Palette, ColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "pill_style_nav")) },
        SettingsSearchEntry("Show Sims In Call Logs", "Show which SIM a call used in the call log", "show_sims_call_logs", Icons.Outlined.Palette, ColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "show_sims_call_logs")) },
        SettingsSearchEntry("Name non contacts as Unknown", "Display Unknown or phone number for unsaved callers", "name_non_contacts_as_unknown", Icons.Outlined.Palette, ColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "name_non_contacts_as_unknown")) },
        SettingsSearchEntry("Auto Delete Unknown No in call log", "Automatically clean up unknown-number entries", "auto_delete_unknown_calllog", Icons.Outlined.Palette, ColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "auto_delete_unknown_calllog")) },

        SettingsSearchEntry("Call Time Format in call logs", "12-hour or 24-hour time format", "call_time_format", Icons.Outlined.Palette, ColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "call_time_format")) },
        SettingsSearchEntry("Icon-Only Bottom Bar", "Hide labels on the bottom navigation bar", "icon_only_bottom_bar", Icons.Outlined.Palette, ColorIndigo) { it.navigate(InterfaceScreenDestination(highlightKey = "icon_only_bottom_bar")) },
        SettingsSearchEntry("Open Dialpad by Default", "Launch straight into the dialpad", "open_dialpad_default", Icons.Outlined.Palette, ColorBlue) { it.navigate(InterfaceScreenDestination(highlightKey = "open_dialpad_default")) },
        SettingsSearchEntry("Show First Letter in Avatar", "Fallback avatar shows a contact's initial", "avatar_first_letter", Icons.Outlined.Palette, ColorAmber) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_first_letter")) },
        SettingsSearchEntry("Use Colorful Avatars", "Give fallback avatars varied colors", "colorful_avatars", Icons.Outlined.Palette, ColorPurple) { it.navigate(InterfaceScreenDestination(highlightKey = "colorful_avatars")) },
        SettingsSearchEntry("Show Picture in Avatar", "Show a contact's photo in their avatar", "avatar_picture", Icons.Outlined.Palette, ColorGreen) { it.navigate(InterfaceScreenDestination(highlightKey = "avatar_picture")) },
        SettingsSearchEntry("App Icon", "Choose a custom launcher icon", "app_icon_link", Icons.Outlined.Palette, ColorRed) { it.navigate(InterfaceScreenDestination(highlightKey = "app_icon_link")) },
        SettingsSearchEntry("App Name", "Change the name shown for the app", "app_name_link", Icons.Outlined.Badge, ColorTeal) { it.navigate(InterfaceScreenDestination(highlightKey = "app_name_link")) },

        // ── Incoming Call UI screen ───────────────────────────────────────────
        SettingsSearchEntry("Default Message", "Quick-reply message shown for incoming calls", "default_message_link", Icons.Outlined.Message, ColorBlue) { it.navigate(IncomingCallUIScreenDestination(highlightKey = "default_message_link")) },

        // ── About screen ───────────────────────────────────────────────────────
        SettingsSearchEntry("Made By Hari", "Developer info", "made_by_hari", Icons.Outlined.Info, ColorBluGrey) { it.navigate(AboutAppScreenDestination(highlightKey = "made_by_hari")) },
        SettingsSearchEntry("Source Code", "View Ever Dialer's source on GitHub", "source_code", Icons.Outlined.Info, ColorBluGrey) { it.navigate(AboutAppScreenDestination(highlightKey = "source_code")) },
        SettingsSearchEntry("Telegram App Support Group", "Get help and discuss the app", "telegram_support", Icons.Outlined.Info, ColorBlue) { it.navigate(AboutAppScreenDestination(highlightKey = "telegram_support")) },
        SettingsSearchEntry("App Recommending Channel in Telegram", "Follow for app announcements", "telegram_channel", Icons.Outlined.Info, ColorBlue) { it.navigate(AboutAppScreenDestination(highlightKey = "telegram_channel")) },
        SettingsSearchEntry("My Other App (Everlasting Android Tweak)", "Check out the developer's other app", "other_app_link", Icons.Outlined.Info, ColorIndigo) { it.navigate(AboutAppScreenDestination(highlightKey = "other_app_link")) }
    )
    val filteredSettingsResults = if (settingsSearchQuery.isBlank()) emptyList()
        else settingsSearchEntries.filter {
            it.title.contains(settingsSearchQuery, ignoreCase = true) || it.subtitle.contains(settingsSearchQuery, ignoreCase = true)
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
    LaunchedEffect(highlightedSettingKey, settingsSearchQuery, isDefaultDialer) {
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
            listState.animateScrollToItem(0)
        }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigateBack() })
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        BackHandler { navigateBack() }
        ScrollHapticsEffect(listState = listState)
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).alpha(alpha).offset(y = offsetY).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            // While showing search results, every visible item below the search field is one
            // row of the same result group, so the list-wide gap must be 0 there — otherwise the
            // grouped rows render with a visible gap between them despite [groupedRowShape]
            // making them look like a single continuous card. A separate Spacer item restores the
            // normal 16dp gap between the search field and the results/empty-state below it.
            verticalArrangement = if (settingsSearchQuery.isNotBlank()) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(16.dp)

        ) {

            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            modifier = Modifier.fillMaxWidth(),
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                                    defaultDialerLauncher.launch(intent)
                                } else {
                                    val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                                        .putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                    defaultDialerLauncher.launch(intent)
                                }
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
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hariprabhu.com/Ever-Dialer/#donate")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
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
                                headline = "App Settings",
                                supporting = "Call settings, network switcher, and notes",
                                leadingIcon = Icons.Outlined.Tune,
                                iconContainerColor = ColorTeal,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("app_settings", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = { navigator.navigate(AppSettingsScreenDestination()) }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Open System Additional Settings",
                                supporting = "Manage phone accounts in Android system settings",
                                leadingIcon = Icons.Outlined.Settings,
                                iconContainerColor = ColorBluGrey,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    try {
                                        val intent = Intent().apply {
                                            component = ComponentName(
                                                "com.android.phone",
                                                "com.android.phone.settings.PhoneAccountSettingsActivity"
                                            )
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            context.startActivity(
                                                Intent(android.provider.Settings.ACTION_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Couldn't open system settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
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
                                supporting = "Save app configuration and notes",
                                leadingIcon = Icons.Default.Backup,
                                iconContainerColor = ColorGreen,
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("create_backup", highlightedSettingKey) { highlightedSettingKey = null },
                                onClick = {
                                    scope.launch {
                                        val file = BackupManager.createBackup(context)
                                        backupState = if (file != null) {
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/octet-stream"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Save Backup"))
                                            BackupDialogState.BackupSuccess(file.absolutePath)
                                        } else {
                                            BackupDialogState.Error("Failed to create backup")
                                        }
                                    }
                                }
                            )
                            CardDivider()
                            RivoListItem(headline = "Restore Backup", supporting = "Restore app configuration and notes", leadingIcon = Icons.Default.Restore, iconContainerColor = ColorBrown, trailingIcon = Icons.Default.ChevronRight, modifier = Modifier.settingsSearchHighlight("restore_backup", highlightedSettingKey) { highlightedSettingKey = null }, onClick = { restoreLauncher.launch("*/*") })
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

private data class SettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val key: String,
    val icon: ImageVector,
    val iconContainerColor: Color,
    // When null, the row lives on this screen and is highlighted in place. When set, the
    // search result belongs to a nested settings screen — navigate there and let that
    // screen pick up `highlightKey` to scroll/flash the row once it composes.
    val navigateTo: ((DestinationsNavigator) -> Unit)? = null
)



private sealed class BackupDialogState {
    object Idle : BackupDialogState()
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
