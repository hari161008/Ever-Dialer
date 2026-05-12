package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.accounts.AccountManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.File
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator

import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.enqueueApkDownload
import com.coolappstore.everdialer.by.svhp.controller.util.getApkDestinationFile
import com.coolappstore.everdialer.by.svhp.controller.util.installApkAndScheduleDelete
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SoundVibrationScreenDestination
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

@Composable
fun ContactsToDisplayDialog(
    onDismiss: () -> Unit,
    prefs: PreferenceManager
) {
    val context = LocalContext.current

    // Build sources list: Google accounts + SIMs + WhatsApp
    val sources = remember {
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = context.getSystemService(SubscriptionManager::class.java)
                val subs = subManager?.activeSubscriptionInfoList
                if (!subs.isNullOrEmpty()) {
                    subs.forEach { sub ->
                        val simName = sub.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${sub.simSlotIndex + 1}"
                        list.add(ContactSourceItem(
                            key = "sim_${sub.subscriptionId}",
                            label = "SIM",
                            subLabel = simName
                        ))
                    }
                } else {
                    list.add(ContactSourceItem(key = "sim_1", label = "SIM", subLabel = "SIM 1"))
                }
            } else {
                list.add(ContactSourceItem(key = "sim_1", label = "SIM", subLabel = "SIM 1"))
            }
        } catch (_: Exception) {
            list.add(ContactSourceItem(key = "sim_1", label = "SIM", subLabel = "SIM 1"))
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
fun CallSettingsScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val context = LocalContext.current

    var proximityBg by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)) }
    var pocketModePrevention by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) }
    var directCallOnTap by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true)) }
    var showContactsToDisplayDialog by remember { mutableStateOf(false) }
    var defaultSim by remember { mutableStateOf(prefs.getInt("default_sim", 0)) }
    var showSimDialog by remember { mutableStateOf(false) }

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
                                    prefs.setInt("default_sim", index)
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
        topBar = {
            TopAppBar(
                title = { Text("Call Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .alpha(screenAlpha),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Caller Accounts ───────────────────────────────────────────────
            item {
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
                                onClick = { showContactsToDisplayDialog = true }
                            )
                        }
                    }
                }
            }

            // ── Call Behavior ─────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 60L) {
                    Column {
                        CallSettingsSectionLabel("Call Behavior")
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline   = "Proximity Sensor on in background",
                                supporting = "Turn off screen when phone is near ear during a call",
                                leadingIcon = Icons.Outlined.Sensors,
                                iconContainerColor = ColorTeal,
                                checked = proximityBg,
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
                                headline   = "Direct Call on Tap",
                                supporting = "Tap a call log entry to call directly instead of viewing contact info",
                                leadingIcon = Icons.Outlined.Call,
                                iconContainerColor = ColorGreen,
                                checked = directCallOnTap,
                                onCheckedChange = {
                                    directCallOnTap = it
                                    prefs.setBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, it)
                                }
                            )
                        }
                    }
                }
            }

            // ── Sound & Vibration ─────────────────────────────────────────────
            item {
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
                                onClick = { navigator.navigate(SoundVibrationScreenDestination) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
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
