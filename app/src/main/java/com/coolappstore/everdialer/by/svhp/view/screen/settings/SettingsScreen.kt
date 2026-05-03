package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.GITHUB_API_RELEASES
import com.coolappstore.everdialer.by.svhp.controller.util.BackupManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.enqueueApkDownload
import com.coolappstore.everdialer.by.svhp.controller.util.fetchLatestRelease
import com.coolappstore.everdialer.by.svhp.controller.util.getApkDestinationFile
import com.coolappstore.everdialer.by.svhp.controller.util.installApkAndScheduleDelete
import com.coolappstore.everdialer.by.svhp.controller.util.isNewerVersion
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.generated.destinations.CallSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()

    var silenceUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SILENCE_UNKNOWN, false)) }
    var notesEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)) }
    var proximityBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)) }
    var tapHapticsEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) }
    var scrollHapticsEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, false)) }
    var autoUpdateEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_UPDATE_CHECK, true)) }
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

    // Font state
    val savedFontPath = prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
    var hasFontSet   by remember { mutableStateOf(savedFontPath != null) }
    var fontSizeScale by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)) }

    var updateDialogState by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.Idle) }
    var backupState       by remember { mutableStateOf<BackupDialogState>(BackupDialogState.Idle) }

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "settingsAlpha")
    LaunchedEffect(Unit) { visible = true }

    // Font picker
    val fontPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fontFile = File(context.filesDir, "custom_font.ttf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        fontFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, fontFile.absolutePath)
                    hasFontSet = true
                    (context as? Activity)?.let { activity ->
                        val intent = activity.intent
                        activity.finish()
                        activity.startActivity(intent)
                    }
                } catch (_: Exception) {}
            }
        }
    }

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
                val updated = blockedContactsList + number
                blockedContactsList = updated
                prefs.setString(PreferenceManager.KEY_BLOCKED_CONTACTS, updated.joinToString(","))
            }
        }

        Dialog(onDismissRequest = { showBlockedNumbersDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Block, null, tint = ColorRed, modifier = Modifier.size(22.dp))
                        Text("Blocked Numbers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name or number…") },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Call Logs", "Contacts", "Manual").forEachIndexed { index, label ->
                            val selected = blockedNumbersTab == index
                            Surface(
                                onClick = { blockedNumbersTab = index },
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

                    Box(modifier = Modifier.heightIn(min = 80.dp, max = 320.dp)) {
                        when (blockedNumbersTab) {
                            0 -> {
                                if (isLoading) {
                                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                } else if (filteredRecents.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                        Text(if (searchQuery.isBlank()) "No call logs found." else "No results for \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(filteredRecents, key = { it.first }) { (number, name, photoUri) ->
                                            val alreadyBlocked = blockedContactsList.contains(number)
                                            Surface(shape = RoundedCornerShape(10.dp), color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceVariant) {
                                                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(38.dp))
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
                            1 -> {
                                if (isLoading) {
                                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                } else if (filteredContacts.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                        Text(if (searchQuery.isBlank()) "No contacts found." else "No results for \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(filteredContacts, key = { it.first }) { (number, name, photoUri) ->
                                            val alreadyBlocked = blockedContactsList.contains(number)
                                            Surface(shape = RoundedCornerShape(10.dp), color = if (alreadyBlocked) MaterialTheme.colorScheme.errorContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceVariant) {
                                                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(38.dp))
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
                            2 -> {
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
                                            if (blockedNumberInput.isNotBlank()) {
                                                IconButton(onClick = {
                                                    val num = blockedNumberInput.trim()
                                                    if (num.isNotBlank()) blockNumber(num)
                                                    blockedNumberInput = ""
                                                }) { Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary) }
                                            }
                                        }
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        Button(
                                            onClick = { blockNumber(searchQuery.trim()); searchQuery = "" },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(50)
                                        ) { Text("Block \"${searchQuery.trim()}\"") }
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

        Dialog(onDismissRequest = { showBlockListDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Block, null, tint = ColorRed, modifier = Modifier.size(22.dp))
                        Text("Block List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(50), color = ColorRed.copy(alpha = 0.12f)) {
                            Text("${blockedContactsList.size}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ColorRed, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

                    if (blockedContactsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f), modifier = Modifier.size(44.dp))
                                Text("No numbers blocked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(blockedWithInfo) { index, (number, name, photoUri) ->
                                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(46.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            if (name != number) Text(number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        IconButton(onClick = {
                                            val updated = blockedContactsList.toMutableList().also { it.removeAt(index) }
                                            blockedContactsList = updated
                                            prefs.setString(PreferenceManager.KEY_BLOCKED_CONTACTS, updated.joinToString(","))
                                        }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Close, "Remove", tint = ColorRed, modifier = Modifier.size(18.dp))
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

    // ── Update Dialogs ────────────────────────────────────────────────────────
    when (val state = updateDialogState) {

        is UpdateDialogState.Checking -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Checking for updates…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        is UpdateDialogState.UpToDate -> AlertDialog(
            onDismissRequest = { updateDialogState = UpdateDialogState.Idle },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = ColorGreen) },
            title = { Text("Up to date") },
            text = { Text("The app is running the latest version (v$APP_VERSION).") },
            confirmButton = { TextButton(onClick = { updateDialogState = UpdateDialogState.Idle }) { Text("OK") } }
        )

        // ── Confirmation popup before downloading ──
        is UpdateDialogState.ConfirmUpdate -> AlertDialog(
            onDismissRequest = { updateDialogState = UpdateDialogState.Idle },
            icon = { Icon(Icons.Default.SystemUpdate, null, tint = ColorBlue) },
            title = { Text("Update Available") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version v${state.latestVersion} is available.")
                    Text("Would you like to download and install it now?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val url = state.apkUrl
                    if (url != null) {
                        val downloadId = enqueueApkDownload(context, url)
                        if (downloadId != null) {
                            updateDialogState = UpdateDialogState.Downloading(state.latestVersion, url, downloadId, 0f)
                        } else {
                            updateDialogState = UpdateDialogState.Error
                        }
                    } else {
                        updateDialogState = UpdateDialogState.Error
                    }
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogState = UpdateDialogState.Idle }) { Text("Not Now") }
            }
        )

        // ── Accurate download progress ──
        is UpdateDialogState.Downloading -> {
            // Poll DownloadManager for real progress
            LaunchedEffect(state.downloadId) {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                while (true) {
                    delay(300)
                    val query = DownloadManager.Query().setFilterById(state.downloadId)
                    val cursor = dm.query(query)
                    if (!cursor.moveToFirst()) { cursor.close(); break }

                    val dmStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    cursor.close()

                    when (dmStatus) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            updateDialogState = UpdateDialogState.Idle
                            val file = getApkDestinationFile()
                            installApkAndScheduleDelete(context, file)
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            updateDialogState = UpdateDialogState.Error
                            break
                        }
                        else -> {
                            val progress = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                            updateDialogState = state.copy(progress = progress)
                        }
                    }
                }
            }

            Dialog(onDismissRequest = {}) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.SystemUpdate, null, tint = ColorBlue, modifier = Modifier.size(36.dp))
                        Text("Downloading Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("v${state.latestVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${(state.progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Please wait…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        is UpdateDialogState.Error -> AlertDialog(
            onDismissRequest = { updateDialogState = UpdateDialogState.Idle },
            icon = { Icon(Icons.Default.Error, null, tint = ColorRed) },
            title = { Text("Check failed") },
            text = { Text("Could not check for updates. Please try again later.") },
            confirmButton = { TextButton(onClick = { updateDialogState = UpdateDialogState.Idle }) { Text("OK") } }
        )

        else -> {}
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

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = { navigator.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).alpha(alpha),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

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
                                onClick = {
                                    scope.launch {
                                        updateDialogState = UpdateDialogState.Checking
                                        val release = fetchLatestRelease(GITHUB_API_RELEASES)
                                        updateDialogState = when {
                                            release == null -> UpdateDialogState.Error
                                            isNewerVersion(release.tagName, APP_VERSION) ->
                                                UpdateDialogState.ConfirmUpdate(release.tagName, release.apkUrl)
                                            else -> UpdateDialogState.UpToDate
                                        }
                                    }
                                }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Rate and Review",
                                supporting = "Share your feedback about Ever Dialer",
                                leadingIcon = Icons.Default.Star,
                                iconContainerColor = ColorCyan,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSdY2WYWDFfvLScsBBxfCWzozyA_4sHUCzfR1JycfzJKASvbfQ/viewform?usp=header"))
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
                            RivoListItem(headline = "Interface", supporting = "Themes, colors, and layout", leadingIcon = Icons.Outlined.Palette, iconContainerColor = ColorPurple, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(InterfaceScreenDestination) })
                            CardDivider()
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = ColorPurple.copy(alpha = 0.18f), modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.TextFormat, null, tint = ColorPurple, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Custom Font", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        Text(
                                            if (hasFontSet) "Custom font active · tap to change" else "Pick a .ttf file to use across the app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (hasFontSet) {
                                        IconButton(onClick = {
                                            prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
                                            prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)
                                            fontSizeScale = 1.0f
                                            hasFontSet = false
                                            val file = File(context.filesDir, "custom_font.ttf")
                                            file.delete()
                                            (context as? Activity)?.let { a ->
                                                val intent = a.intent
                                                a.finish()
                                                a.startActivity(intent)
                                            }
                                        }) { Icon(Icons.Default.Refresh, "Revert font", tint = ColorRed) }
                                    }
                                    IconButton(onClick = { fontPickerLauncher.launch("font/ttf") }) {
                                        Icon(Icons.Default.FolderOpen, "Pick font", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (hasFontSet) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Size", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                                        Slider(
                                            value = fontSizeScale,
                                            onValueChange = { fontSizeScale = it },
                                            onValueChangeFinished = { prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, fontSizeScale) },
                                            valueRange = 0.8f..1.4f,
                                            steps = 11,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("${(fontSizeScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(42.dp).padding(start = 8.dp))
                                    }
                                }
                            }
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
                                onClick = { showHapticsDialog = true }
                            )
                            CardDivider()
                            RivoSwitchListItem(
                                headline   = "Scroll Haptics",
                                supporting = "Vibrate on scroll gestures across the app",
                                leadingIcon = Icons.Outlined.SwipeVertical,
                                iconContainerColor = ColorIndigo,
                                checked = scrollHapticsEnabled,
                                onCheckedChange = {
                                    scrollHapticsEnabled = it
                                    prefs.setBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, it)
                                }
                            )
                        }
                    }
                }
            }

            // ── Notes ────────────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 100L) {
                    Column {
                        SectionLabel("Notes")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline   = "Enable Notes Section",
                                supporting = "Show Notes tab in bottom navigation",
                                leadingIcon = Icons.Outlined.StickyNote2,
                                iconContainerColor = ColorTeal,
                                checked = notesEnabled,
                                onCheckedChange = {
                                    notesEnabled = it
                                    prefs.setBoolean(PreferenceManager.KEY_NOTES_ENABLED, it)
                                }
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
                                headline = if (isDefaultDialer) "Default Dialer" else "Set as Default Dialer",
                                supporting = if (isDefaultDialer) "Ever Dialer is your default phone app" else "Required for calls and call log access",
                                leadingIcon = Icons.Outlined.Phone,
                                iconContainerColor = if (isDefaultDialer) ColorGreen else ColorOrange,
                                trailingIcon = if (isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                                onClick = {
                                    if (!isDefaultDialer) {
                                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                        defaultDialerLauncher.launch(intent)
                                    }
                                }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Call Settings",
                                supporting = "Accounts, sensor, pocket mode, and sound",
                                leadingIcon = Icons.Outlined.Call,
                                iconContainerColor = ColorTeal,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { navigator.navigate(CallSettingsScreenDestination) }
                            )
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
                                onCheckedChange = {
                                    silenceUnknown = it
                                    prefs.setBoolean(PreferenceManager.KEY_SILENCE_UNKNOWN, it)
                                }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Blocked Numbers",
                                supporting = "${blockedContactsList.size} number(s) blocked",
                                leadingIcon = Icons.Outlined.PersonOff,
                                iconContainerColor = ColorBluGrey,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { showBlockedNumbersDialog = true }
                            )
                            CardDivider()
                            RivoListItem(
                                headline = "Tap to see the Block list",
                                supporting = if (blockedContactsList.isEmpty()) "No numbers blocked"
                                             else "${blockedContactsList.size} number(s) blocked",
                                leadingIcon = Icons.Outlined.Block,
                                iconContainerColor = ColorRed,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = { showBlockListDialog = true }
                            )
                        }
                    }
                }
            }


            // ── Auto Check For Updates ────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 240L) {
                    Column {
                        SectionLabel("Auto Check For Updates")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline   = "Auto Check For Updates",
                                supporting = "Automatically check for updates when the app opens",
                                leadingIcon = Icons.Default.Autorenew,
                                iconContainerColor = ColorAmber,
                                checked = autoUpdateEnabled,
                                onCheckedChange = {
                                    autoUpdateEnabled = it
                                    prefs.setBoolean(PreferenceManager.KEY_AUTO_UPDATE_CHECK, it)
                                }
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
                            RivoListItem(headline = "Restore Backup", supporting = "Restore app configuration and notes", leadingIcon = Icons.Default.Restore, iconContainerColor = ColorBrown, trailingIcon = Icons.Default.ChevronRight, onClick = { restoreLauncher.launch("*/*") })
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
                            RivoListItem(headline = "About Ever Dialer", supporting = "Version $APP_VERSION · Developer info", leadingIcon = Icons.Outlined.Info, iconContainerColor = ColorBluGrey, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(AboutAppScreenDestination) })
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

private sealed class UpdateDialogState {
    object Idle : UpdateDialogState()
    object Checking : UpdateDialogState()
    object UpToDate : UpdateDialogState()
    data class ConfirmUpdate(val latestVersion: String, val apkUrl: String?) : UpdateDialogState()
    data class Downloading(val latestVersion: String, val apkUrl: String?, val downloadId: Long, val progress: Float) : UpdateDialogState()
    object Error : UpdateDialogState()
}

private sealed class BackupDialogState {
    object Idle : BackupDialogState()
    object Restoring : BackupDialogState()
    data class BackupSuccess(val path: String) : BackupDialogState()
    object RestoreSuccess : BackupDialogState()
    data class Error(val message: String) : BackupDialogState()
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
