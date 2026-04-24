package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.GITHUB_API_RELEASES
import com.coolappstore.everdialer.by.svhp.controller.util.BackupManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.downloadAndInstallApk
import com.coolappstore.everdialer.by.svhp.controller.util.fetchLatestRelease
import com.coolappstore.everdialer.by.svhp.controller.util.isNewerVersion
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val prefs: PreferenceManager = koinInject()
    val scope = rememberCoroutineScope()

    var blockUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, false)) }
    var blockHidden  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, false)) }
    var notesEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)) }

    // Font state
    val savedFontPath = prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
    var hasFontSet   by remember { mutableStateOf(savedFontPath != null) }
    var fontSizeScale by remember { mutableFloatStateOf(prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)) }

    var updateDialogState by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.Idle) }
    var backupState       by remember { mutableStateOf<BackupDialogState>(BackupDialogState.Idle) }

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "settingsAlpha")
    LaunchedEffect(Unit) { visible = true }

    // Font picker (.ttf) → copy → save → restart
    val fontPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fontFile = File(context.filesDir, "custom_font.ttf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        fontFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    prefs.setString(PreferenceManager.KEY_CUSTOM_FONT_PATH, fontFile.absolutePath)
                    hasFontSet = true
                    // Restart to apply font everywhere
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
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
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
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
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

    // ── Dialogs ───────────────────────────────────────────────────────────────

    when (val state = updateDialogState) {
        is UpdateDialogState.Checking -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Checking for updates…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is UpdateDialogState.UpToDate -> AlertDialog(onDismissRequest = { updateDialogState = UpdateDialogState.Idle }, icon = { Icon(Icons.Default.CheckCircle, null, tint = ColorGreen) }, title = { Text("Up to date") }, text = { Text("The app is running the latest version (v$APP_VERSION).") }, confirmButton = { TextButton(onClick = { updateDialogState = UpdateDialogState.Idle }) { Text("OK") } })
        is UpdateDialogState.UpdateAvailable -> AlertDialog(onDismissRequest = { updateDialogState = UpdateDialogState.Idle }, icon = { Icon(Icons.Default.SystemUpdate, null, tint = ColorBlue) }, title = { Text("Update available") }, text = { Text("A new version v${state.latestVersion} is available. Download and install now?") },
            confirmButton = { Button(onClick = { updateDialogState = UpdateDialogState.Idle; state.apkUrl?.let { downloadAndInstallApk(context, it) } }) { Text("Download & Install") } },
            dismissButton = { TextButton(onClick = { updateDialogState = UpdateDialogState.Idle }) { Text("Later") } })
        is UpdateDialogState.Error -> AlertDialog(onDismissRequest = { updateDialogState = UpdateDialogState.Idle }, icon = { Icon(Icons.Default.Error, null, tint = ColorRed) }, title = { Text("Check failed") }, text = { Text("Could not check for updates. Please try again later.") }, confirmButton = { TextButton(onClick = { updateDialogState = UpdateDialogState.Idle }) { Text("OK") } })
        else -> {}
    }

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
                                            isNewerVersion(release.tagName, APP_VERSION) -> UpdateDialogState.UpdateAvailable(release.tagName, release.apkUrl)
                                            else -> UpdateDialogState.UpToDate
                                        }
                                    }
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
                            RivoListItem(headline = "Incoming Call UI", supporting = "Customize the incoming call screen appearance", leadingIcon = Icons.Outlined.CallReceived, iconContainerColor = ColorGreen, trailingIcon = Icons.Default.ChevronRight, onClick = {})
                            CardDivider()
                            RivoListItem(headline = "Caller UI", supporting = "Customize the in-call screen layout and controls", leadingIcon = Icons.Outlined.Person, iconContainerColor = ColorBlue, trailingIcon = Icons.Default.ChevronRight, onClick = {})
                            CardDivider()
                            // Custom Font row + inline slider
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ColorPurple,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.TextFormat, null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                                    // Revert button (only when font is set)
                                    if (hasFontSet) {
                                        IconButton(
                                            onClick = {
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
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, "Revert font", tint = ColorRed)
                                        }
                                    }
                                    IconButton(onClick = { fontPickerLauncher.launch("font/ttf") }) {
                                        Icon(Icons.Default.FolderOpen, "Pick font", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                // Font size slider (only when font is set)
                                if (hasFontSet) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Size", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                                        Slider(
                                            value = fontSizeScale,
                                            onValueChange = { fontSizeScale = it },
                                            onValueChangeFinished = {
                                                prefs.setFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, fontSizeScale)
                                            },
                                            valueRange = 0.8f..1.4f,
                                            steps = 11,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${(fontSizeScale * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.width(42.dp).padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
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
                            RivoListItem(headline = "Call Accounts", supporting = "SIM cards and calling accounts", leadingIcon = Icons.Outlined.SimCard, iconContainerColor = ColorGreen, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(CallAccountsScreenDestination) })
                        }
                    }
                }
            }

            // ── Privacy ──────────────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 180L) {
                    Column {
                        SectionLabel("Privacy")
                        RivoExpressiveCard {
                            RivoSwitchListItem(headline = "Block Unknown Callers", supporting = "Silence calls from unidentified numbers", leadingIcon = Icons.Outlined.Block, iconContainerColor = ColorRed, checked = blockUnknown, onCheckedChange = { blockUnknown = it; prefs.setBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, it) })
                            CardDivider()
                            RivoSwitchListItem(headline = "Block Hidden Numbers", supporting = "Silence private or withheld numbers", leadingIcon = Icons.Outlined.VisibilityOff, iconContainerColor = ColorIndigo, checked = blockHidden, onCheckedChange = { blockHidden = it; prefs.setBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, it) })
                        }
                    }
                }
            }

            // ── Sound & Vibration ────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 220L) {
                    Column {
                        SectionLabel("Sound & Vibration")
                        RivoExpressiveCard {
                            RivoListItem(headline = "Sound & Vibration", supporting = "Ringtones and dialpad tones", leadingIcon = Icons.Outlined.VolumeUp, iconContainerColor = ColorBlue, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(SoundVibrationScreenDestination) })
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
                                supporting = "Save call logs, notes, favourites and settings",
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
                            RivoListItem(headline = "Restore Backup", supporting = "Restore from a .everdialer backup file", leadingIcon = Icons.Default.Restore, iconContainerColor = ColorBrown, trailingIcon = Icons.Default.ChevronRight, onClick = { restoreLauncher.launch("*/*") })
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
    data class UpdateAvailable(val latestVersion: String, val apkUrl: String?) : UpdateDialogState()
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
