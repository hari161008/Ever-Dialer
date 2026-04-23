package com.grinch.rivo4.view.screen.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import com.grinch.rivo4.APP_VERSION
import com.grinch.rivo4.GITHUB_API_RELEASES
import com.grinch.rivo4.controller.util.BackupManager
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.downloadAndInstallApk
import com.grinch.rivo4.controller.util.fetchLatestRelease
import com.grinch.rivo4.controller.util.isNewerVersion
import com.grinch.rivo4.view.components.RivoAnimatedSection
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

    var hapticEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_feedback", true)) }
    var blockUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, false)) }
    var blockHidden by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, false)) }
    var notesEnabled by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)) }

    var updateDialogState by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.Idle) }
    var backupState by remember { mutableStateOf<BackupDialogState>(BackupDialogState.Idle) }

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "settingsAlpha")
    LaunchedEffect(Unit) { visible = true }

    // Restore file picker
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                backupState = BackupDialogState.Restoring
                try {
                    val tmpFile = java.io.File(context.cacheDir, "restore_tmp.everdialer")
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

    // Update dialog
    when (val state = updateDialogState) {
        is UpdateDialogState.Checking -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

    // Backup/Restore dialog
    when (val state = backupState) {
        is BackupDialogState.Restoring -> Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Restoring backup…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is BackupDialogState.BackupSuccess -> AlertDialog(
            onDismissRequest = { backupState = BackupDialogState.Idle },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = ColorGreen) },
            title = { Text("Backup created") },
            text = { Text("Backup saved to:\n${state.path}") },
            confirmButton = { TextButton(onClick = { backupState = BackupDialogState.Idle }) { Text("OK") } }
        )
        is BackupDialogState.RestoreSuccess -> AlertDialog(
            onDismissRequest = { backupState = BackupDialogState.Idle },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = ColorGreen) },
            title = { Text("Restore complete") },
            text = { Text("Your data has been restored successfully. Please restart the app.") },
            confirmButton = { TextButton(onClick = { backupState = BackupDialogState.Idle }) { Text("OK") } }
        )
        is BackupDialogState.Error -> AlertDialog(
            onDismissRequest = { backupState = BackupDialogState.Idle },
            icon = { Icon(Icons.Default.Error, null, tint = ColorRed) },
            title = { Text("Operation failed") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = { backupState = BackupDialogState.Idle }) { Text("OK") } }
        )
        else -> {}
    }

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
            // Check For Updates
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Check For Updates",
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

            // Personalization
            item {
                RivoAnimatedSection(delayMs = 60L) {
                    SectionLabel("Personalization")
                    RivoExpressiveCard {
                        RivoListItem(headline = "Interface", supporting = "Themes, colors, and layout", leadingIcon = Icons.Outlined.Palette, iconContainerColor = ColorPurple, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(InterfaceScreenDestination) })
                        Divider()
                        RivoSwitchListItem(headline = "Haptic Feedback", supporting = "Vibrate on touch and gestures", leadingIcon = Icons.Outlined.Vibration, iconContainerColor = ColorOrange, checked = hapticEnabled, onCheckedChange = { hapticEnabled = it; prefs.setBoolean("haptic_feedback", it) })
                        Divider()
                        RivoListItem(headline = "Sound & Vibration", supporting = "Ringtones and dialpad tones", leadingIcon = Icons.Outlined.VolumeUp, iconContainerColor = ColorBlue, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(SoundVibrationScreenDestination) })
                    }
                }
            }

            // Notes
            item {
                RivoAnimatedSection(delayMs = 100L) {
                    SectionLabel("Notes")
                    RivoExpressiveCard {
                        // Info banner
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    "Your Contact notes are being saved in the documents folder as text files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Divider()
                        RivoSwitchListItem(
                            headline = "Enable Notes Section",
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

            // Backup & Restore
            item {
                RivoAnimatedSection(delayMs = 140L) {
                    SectionLabel("Backup & Restore")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Create Backup",
                            supporting = "Save call logs, notes, favourites and settings",
                            leadingIcon = Icons.Default.Backup,
                            iconContainerColor = ColorGreen,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = {
                                scope.launch {
                                    val file = BackupManager.createBackup(context)
                                    backupState = if (file != null) {
                                        // Share the backup file
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
                        Divider()
                        RivoListItem(
                            headline = "Restore Backup",
                            supporting = "Restore from a .everdialer backup file",
                            leadingIcon = Icons.Default.Restore,
                            iconContainerColor = ColorBrown,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { restoreLauncher.launch("*/*") }
                        )
                    }
                }
            }

            // Calls & System
            item {
                RivoAnimatedSection(delayMs = 180L) {
                    SectionLabel("Calls & System")
                    RivoExpressiveCard {
                        RivoListItem(headline = "Call Accounts", supporting = "SIM cards and calling accounts", leadingIcon = Icons.Outlined.SimCard, iconContainerColor = ColorGreen, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(CallAccountsScreenDestination) })
                    }
                }
            }

            // Privacy
            item {
                RivoAnimatedSection(delayMs = 220L) {
                    SectionLabel("Privacy")
                    RivoExpressiveCard {
                        RivoSwitchListItem(headline = "Block Unknown Callers", supporting = "Silence calls from unidentified numbers", leadingIcon = Icons.Outlined.Block, iconContainerColor = ColorRed, checked = blockUnknown, onCheckedChange = { blockUnknown = it; prefs.setBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, it) })
                        Divider()
                        RivoSwitchListItem(headline = "Block Hidden Numbers", supporting = "Silence private or withheld numbers", leadingIcon = Icons.Outlined.VisibilityOff, iconContainerColor = ColorIndigo, checked = blockHidden, onCheckedChange = { blockHidden = it; prefs.setBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, it) })
                    }
                }
            }

            // App info
            item {
                RivoAnimatedSection(delayMs = 260L) {
                    RivoExpressiveCard {
                        RivoListItem(headline = "About Ever Dialer", supporting = "Version $APP_VERSION · Developer info", leadingIcon = Icons.Outlined.Info, iconContainerColor = ColorBluGrey, trailingIcon = Icons.Default.ChevronRight, onClick = { navigator.navigate(AboutAppScreenDestination) })
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
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
