/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.coolappstore.evercallrecorder.by.svhp.R
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioCodec
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioSource
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyConfig
import com.coolappstore.evercallrecorder.by.svhp.system.PersistentFolderPickerContract
import com.coolappstore.evercallrecorder.by.svhp.system.copyToClipboard
import com.coolappstore.evercallrecorder.by.svhp.system.openGithub
import com.coolappstore.evercallrecorder.by.svhp.system.openGithubReportIssue
import com.coolappstore.evercallrecorder.by.svhp.system.openTelegramSupportGroup
import com.coolappstore.evercallrecorder.by.svhp.system.openTelegramChannel
import com.coolappstore.evercallrecorder.by.svhp.system.openUrlInBrowser
import com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper
import com.coolappstore.evercallrecorder.by.svhp.system.takePersistableFolderPermission
import com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallTarget
import com.coolappstore.evercallrecorder.by.svhp.ui.common.*
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.*
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit = {}, onOpenWebView: (url: String, enableDownloads: Boolean, extraBottomDp: Int) -> Unit = { _, _, _ -> }, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val updateTrigger by viewModel.updateTrigger.collectAsState()
    val contactPickerViewModel: ContactPickerViewModel = viewModel()
    val contactPickerState by contactPickerViewModel.contactPickerState.collectAsState()
    var showStorageChoiceDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(PersistentFolderPickerContract()) { uri ->
        if (uri != null) {
            context.takePersistableFolderPermission(uri)
            viewModel.preferences.setRecordingFolderUri(uri)
            viewModel.preferences.setStorageMode(AppPreferences.StorageMode.SAF_FOLDER)
        }
        viewModel.refresh()
    }
    val exportLogLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) viewModel.exportLogs(uri)
    }

    BackHandler { onBack() }
    SettingsContent(
        preferences = viewModel.preferences,
        updateTrigger = updateTrigger,
        actions = viewModel,
        contactPickerState = contactPickerState,
        onStorageClick = { showStorageChoiceDialog = true },
        onOpenContactsIncoming = { contactPickerViewModel.openContactPicker(ContactPickerType.INCOMING) },
        onOpenContactsOutgoing = { contactPickerViewModel.openContactPicker(ContactPickerType.OUTGOING) },
        onConfirmContacts = { numbers -> contactPickerViewModel.confirmContactPicker(numbers); viewModel.refresh() },
        onDismissContacts = { contactPickerViewModel.dismissContactPicker() },
        onExportLogs = { exportLogLauncher.launch("evercallrecorder_bug_report.log") },
        onBack = onBack,
        onOpenWebView = onOpenWebView,
        modifier = modifier
    )

    if (showStorageChoiceDialog) {
        StorageLocationDialog(
            onChooseFolder = {
                showStorageChoiceDialog = false
                folderPickerLauncher.launch(null)
            },
            onChoosePrivate = {
                viewModel.preferences.setStorageMode(AppPreferences.StorageMode.PRIVATE)
                viewModel.refresh()
                showStorageChoiceDialog = false
            },
            onDismiss = { showStorageChoiceDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    preferences: AppPreferences,
    updateTrigger: Int,
    actions: SettingsActions,
    contactPickerState: ContactPickerState?,
    onStorageClick: () -> Unit,
    onOpenContactsIncoming: () -> Unit,
    onOpenContactsOutgoing: () -> Unit,
    onConfirmContacts: (Set<String>) -> Unit,
    onDismissContacts: () -> Unit,
    onExportLogs: () -> Unit,
    onBack: () -> Unit = {},
    onOpenWebView: (url: String, enableDownloads: Boolean, extraBottomDp: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showLicensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    // ORDER: Updates → Rate And Review → Appearance → Recording → Audio → Security → Languages → About → Debug
                    UpdatesSection(preferences, updateTrigger, actions)
                    RateAndReviewSection(onOpenWebView = onOpenWebView)
                    AppearanceSection(preferences, updateTrigger, actions)
                    RecordingSection(preferences, updateTrigger, actions, onStorageClick, onOpenContactsIncoming, onOpenContactsOutgoing)
                    AutoDeleteSection(preferences, updateTrigger, actions)
                    AudioSection(preferences, updateTrigger, actions)
                    SecuritySection(preferences, updateTrigger, actions)
                    LanguagesSection(preferences, updateTrigger, actions)
                    AboutSection(versionString = actions.getAppVersion(), onShowLicenses = { showLicensesDialog = true })
                    DebugSection(preferences, updateTrigger, actions, onExportLogs)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showLicensesDialog) {
        Dialog(onDismissRequest = { showLicensesDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column {
                    Text(text = stringResource(R.string.general_licenses), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                    val libraries by produceLibraries(R.raw.aboutlibraries)
                    LibrariesContainer(libraries, Modifier.fillMaxSize().weight(1f), showAuthor = true, showLicenseBadges = true, showFundingBadges = false, showVersion = true, showDescription = true)
                    TextButton(onClick = { showLicensesDialog = false }, modifier = Modifier.align(Alignment.End).padding(8.dp)) { Text(stringResource(R.string.general_close)) }
                }
            }
        }
    }

    contactPickerState?.let { picker ->
        ContactSelectionDialog(
            title = when (picker.type) {
                ContactPickerType.INCOMING -> stringResource(R.string.settings_select_contacts_incoming)
                ContactPickerType.OUTGOING -> stringResource(R.string.settings_select_contacts_outgoing)
            },
            contacts = picker.contacts,
            initialSelection = picker.selectedNumbers,
            onConfirm = onConfirmContacts,
            onDismiss = onDismissContacts
        )
    }
}

// ── Updates section ───────────────────────────────────────────────────────────

private sealed class UpdateState {
    object Idle                                                              : UpdateState()
    object Checking                                                          : UpdateState()
    object UpToDate                                                          : UpdateState()
    data class UpdateAvailable(val version: String, val apkUrl: String?)     : UpdateState()
    data class Downloading(val version: String, val progress: Float)         : UpdateState()
    object Installing                                                        : UpdateState()
    object Error                                                             : UpdateState()
}

@Composable
private fun UpdatesSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val context           = LocalContext.current
    val scope             = rememberCoroutineScope()
    val appVersion        = remember { actions.getAppVersion() }
    var autoUpdateEnabled by remember(updateTrigger) { mutableStateOf(preferences.isAutoUpdateCheckEnabled()) }
    var updateState       by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    // Checks GitHub and stops at UpdateAvailable for user confirmation (or skips to install if cached)
    fun checkForUpdates() {
        if (updateState is UpdateState.Checking || updateState is UpdateState.Downloading) return
        scope.launch {
            updateState = UpdateState.Checking
            val release = com.coolappstore.evercallrecorder.by.svhp.system.fetchLatestRelease(
                com.coolappstore.evercallrecorder.by.svhp.AppUrls.GITHUB_API_RELEASES
            )
            val currentRaw = Regex("""(\d+\.\d+[.\d]*)""").find(appVersion)?.value ?: "0"
            when {
                release == null -> updateState = UpdateState.Error
                !com.coolappstore.evercallrecorder.by.svhp.system.isNewerVersion(release.tagName, currentRaw) ->
                    updateState = UpdateState.UpToDate
                com.coolappstore.evercallrecorder.by.svhp.system.isApkReadyToInstall(context, release.tagName) -> {
                    // Already downloaded for this version — go straight to install
                    updateState = UpdateState.Installing
                    com.coolappstore.evercallrecorder.by.svhp.system.installApkAndScheduleDelete(
                        context, com.coolappstore.evercallrecorder.by.svhp.system.getApkDestinationFile()
                    )
                    kotlinx.coroutines.delay(2_000)
                    updateState = UpdateState.Idle
                }
                else -> {
                    // Show confirmation card — download starts only after user taps Download
                    updateState = UpdateState.UpdateAvailable(release.tagName, release.apkUrl)
                }
            }
        }
    }

    // Called when user confirms the download from the UpdateAvailable card
    fun startDownload(version: String, apkUrl: String?) {
        if (apkUrl == null) { updateState = UpdateState.Error; return }
        scope.launch {
            val downloadId = com.coolappstore.evercallrecorder.by.svhp.system.enqueueApkDownload(context, apkUrl)
            if (downloadId == null) { updateState = UpdateState.Error; return@launch }

            updateState = UpdateState.Downloading(version, 0f)

            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            while (true) {
                kotlinx.coroutines.delay(350)
                val query  = android.app.DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (!cursor.moveToFirst()) { cursor.close(); updateState = UpdateState.Error; break }
                val status     = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total      = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                cursor.close()
                when (status) {
                    android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                        com.coolappstore.evercallrecorder.by.svhp.system.saveDownloadedVersion(context, version)
                        updateState = UpdateState.Installing
                        com.coolappstore.evercallrecorder.by.svhp.system.installApkAndScheduleDelete(
                            context, com.coolappstore.evercallrecorder.by.svhp.system.getApkDestinationFile()
                        )
                        kotlinx.coroutines.delay(2_000)
                        updateState = UpdateState.Idle
                        break
                    }
                    android.app.DownloadManager.STATUS_FAILED -> { updateState = UpdateState.Error; break }
                    else -> {
                        val prog = if (total > 0L) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                        if (updateState is UpdateState.Downloading)
                            updateState = (updateState as UpdateState.Downloading).copy(progress = prog)
                    }
                }
            }
        }
    }

    SettingsSection(title = stringResource(R.string.settings_section_updates), icon = Icons.Outlined.SystemUpdate) {

        // ── Version hero card ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Ever Call Recorder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        Regex("""(\\d+\\.\\d+[.\\d]*)""").find(appVersion)?.value?.let { "Version $it" } ?: appVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // ── Animated update status card ────────────────────────────────────
        AnimatedContent(
            targetState = updateState,
            transitionSpec = {
                (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                 scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.94f))
                    .togetherWith(fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.94f))
            },
            label = "updateStatus"
        ) { state ->
            when (state) {
                is UpdateState.Idle -> {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Button(
                            onClick = { checkForUpdates() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check for Updates", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                is UpdateState.Checking -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Column {
                            Text("Checking for updates", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text("Connecting to GitHub…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                is UpdateState.UpToDate -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { checkForUpdates() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("You\'re up to date!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("Tap to check again", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("v${state.version} Available", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Text("New version ready to download", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { updateState = UpdateState.Idle },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Not Now") }
                            Button(
                                onClick = { startDownload(state.version, state.apkUrl) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }

                is UpdateState.Downloading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Downloading v${state.version}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text("Installing automatically after download", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text("${if (state.progress > 0f) "%.1f MB downloaded".format(state.progress * 50) else "Starting…"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                is UpdateState.Installing -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Opening installer…", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text("Follow the on-screen instructions to complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                is UpdateState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            .clickable { checkForUpdates() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Check failed", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text("Tap to retry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.Refresh, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        ToggleListItem(
            label = "Auto Check For Updates",
            checked = autoUpdateEnabled,
            description = "Automatically check for updates when the app opens",
            onCheckedChange = {
                autoUpdateEnabled = it
                actions.setAutoUpdateCheckEnabled(it)
            }
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ── Rate and Review section ───────────────────────────────────────────────────

@Composable
private fun RateAndReviewSection(
    onOpenWebView: (url: String, enableDownloads: Boolean, extraBottomDp: Int) -> Unit
) {
    val context = LocalContext.current

    SettingsSection(title = "Rate and Review", icon = Icons.Outlined.Star) {
        SectionListItem(
            icon = Icons.Outlined.RateReview,
            headline = "Rate And Review",
            supporting = "Share your feedback via our Google Form",
            onClick = { context.openUrlInBrowser(com.coolappstore.evercallrecorder.by.svhp.AppUrls.RATE_AND_REVIEW) }
        )
        SectionListItem(
            icon = Icons.Outlined.Reviews,
            headline = "Check Ratings And Reviews",
            supporting = "See what others are saying about the app",
            onClick = { onOpenWebView(com.coolappstore.evercallrecorder.by.svhp.AppUrls.CHECK_RATINGS, false, 24) }
        )
        SectionListItem(
            icon = Icons.Outlined.Apps,
            headline = "More Apps",
            supporting = "Explore other apps by the developer",
            onClick = { onOpenWebView(com.coolappstore.evercallrecorder.by.svhp.AppUrls.MORE_APPS, true, 48) }
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ── Appearance section (was Visual) ──────────────────────────────────────────

@Composable
private fun AppearanceSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val currentThemeMode     = remember(updateTrigger) { preferences.getThemeMode() }
    val isDynamicColorEnabled= remember(updateTrigger) { preferences.isDynamicColorEnabled() }
    val isRecordingNotificationsEnabled = remember(updateTrigger) { preferences.isRecordingNotificationsEnabled() }
    val isShowToastsEnabled  = remember(updateTrigger) { preferences.isShowToastsEnabled() }
    val isVibrationEnabled   = remember(updateTrigger) { preferences.isVibrationEnabled() }
    val accentArgb           = remember(updateTrigger) { preferences.getAccentColor() }

    SettingsSection(title = stringResource(R.string.settings_section_appearance), icon = Icons.Outlined.Palette) {
        // Pill-style theme mode selector
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.settings_theme_mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            ThemeModePillSelector(current = currentThemeMode, onSelect = { actions.setThemeMode(it) })
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        ToggleListItem(
            label = stringResource(R.string.settings_dynamic_color),
            checked = isDynamicColorEnabled,
            onCheckedChange = { actions.setDynamicColorEnabled(it) }
        )
        // Color picker when dynamic color is OFF
        AnimatedVisibility(visible = !isDynamicColorEnabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Text(text = stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                InlineColorPicker(currentArgb = accentArgb, onColorChanged = { actions.setAccentColor(it) })
                Spacer(Modifier.height(8.dp))
            }
        }
        ToggleListItem(
            label = stringResource(R.string.settings_recording_notifications),
            checked = isRecordingNotificationsEnabled,
            onCheckedChange = { actions.setRecordingNotificationsEnabled(it) }
        )
        ToggleListItem(label = stringResource(R.string.settings_show_toasts), checked = isShowToastsEnabled, onCheckedChange = { actions.setShowToastsEnabled(it) })
        ToggleListItem(label = stringResource(R.string.settings_vibration_enabled), checked = isVibrationEnabled, onCheckedChange = { actions.setVibrationEnabled(it) })
    }
}

@Composable
private fun ThemeModePillSelector(current: AppPreferences.ThemeMode, onSelect: (AppPreferences.ThemeMode) -> Unit) {
    data class PillOption(val mode: AppPreferences.ThemeMode, val label: String)
    val options = listOf(
        PillOption(AppPreferences.ThemeMode.LIGHT,   "Light"),
        PillOption(AppPreferences.ThemeMode.DARK,    "Dark"),
        PillOption(AppPreferences.ThemeMode.SYSTEM,  "Auto"),
        PillOption(AppPreferences.ThemeMode.WHITE,   "White"),
        PillOption(AppPreferences.ThemeMode.BLACK,   "Black"),
        PillOption(AppPreferences.ThemeMode.AUTO_WB, "Auto W/B"),
    )
    // Two rows of 3 pills
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(options.take(3), options.drop(3)).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    val selected = current == option.mode
                    Surface(
                        onClick = { onSelect(option.mode) },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}



// ── Inline HSV Color Picker ───────────────────────────────────────────────────

@Composable
private fun InlineColorPicker(currentArgb: Int, onColorChanged: (Int) -> Unit) {
    // Extract HSV from stored color
    val initialHsv = remember(currentArgb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(currentArgb, it) }
    }

    var hue by remember(currentArgb) { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember(currentArgb) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(currentArgb) { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember(currentArgb) { mutableStateOf(argbToHex(currentArgb)) }
    var hexValid by remember { mutableStateOf(true) }

    val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    fun commit() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        hexText = argbToHex(argb)
        onColorChanged(argb)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // SV Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(hue) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val s = (down.position.x / size.width).coerceIn(0f, 1f)
                        val v = (1f - down.position.y / size.height).coerceIn(0f, 1f)
                        sat = s; value = v; commit()
                        down.consume()
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                sat = (ch.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - ch.position.y / size.height).coerceIn(0f, 1f)
                                commit()
                                ch.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                // Thumb
                val thumbX = sat * size.width
                val thumbY = (1f - value) * size.height
                drawCircle(color = currentColor, radius = 10.dp.toPx(), center = Offset(thumbX, thumbY))
                drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(thumbX, thumbY), style = Stroke(width = 2.dp.toPx()))
            }
        }

        // Hue strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        hue = (down.position.x / size.width * 360f).coerceIn(0f, 360f)
                        commit(); down.consume()
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                hue = (ch.position.x / size.width * 360f).coerceIn(0f, 360f)
                                commit(); ch.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val hueColors = listOf(
                Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color(0xFF8B00FF), Color.Red
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Brush.horizontalGradient(hueColors))
                // Thumb line
                val thumbX = (hue / 360f) * size.width
                drawCircle(color = Color.White, radius = (size.height / 2f) - 2.dp.toPx(), center = Offset(thumbX, size.height / 2f), style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = (size.height / 2f) - 1.dp.toPx(), center = Offset(thumbX, size.height / 2f), style = Stroke(width = 1.dp.toPx()))
            }
        }

        // Preview + Hex input
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Color preview swatch
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            // Hex input
            OutlinedTextField(
                value = hexText,
                onValueChange = { raw ->
                    hexText = raw
                    val cleaned = raw.trimStart('#')
                    if (cleaned.length == 6) {
                        try {
                            val parsed = android.graphics.Color.parseColor("#$cleaned")
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                            onColorChanged(parsed)
                            hexValid = true
                        } catch (_: Exception) { hexValid = false }
                    } else { hexValid = false }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.settings_accent_color_hex)) },
                singleLine = true,
                isError = !hexValid,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                prefix = { Text("#", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
            )
        }
    }
}

private fun argbToHex(argb: Int): String {
    return "%06X".format(argb and 0x00FFFFFF)
}

// ── Languages section ─────────────────────────────────────────────────────────

@Composable
private fun LanguagesSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val currentLanguage = remember {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.isEmpty) "" else currentLocales[0]?.toLanguageTag() ?: ""
    }
    val languageOptions = remember(context) {
        val options = mutableListOf(OptionItem("", resources.getString(R.string.settings_language_system)))
        @SuppressLint("DiscouragedApi")
        val resId = resources.getIdentifier("_generated_res_locale_config", "xml", context.packageName)
        try {
            val parser = resources.getXml(resId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val localeName = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                    if (localeName != null) {
                        val locale = Locale.forLanguageTag(localeName)
                        val displayName = locale.getDisplayName(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                        options.add(OptionItem(localeName, displayName))
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) { options.add(OptionItem("en", "English (Provided as fallback)")) }
        options.distinctBy { it.key }
    }

    SettingsSection(title = stringResource(R.string.settings_section_language), icon = Icons.Outlined.Language) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            M3DropdownField(
                label = stringResource(R.string.settings_language),
                selected = languageOptions.find { it.key == currentLanguage } ?: languageOptions.first(),
                options = languageOptions,
                onOptionSelected = { actions.setAppLanguage(it.key) }
            )
        }
    }
}

// ── About section ─────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(versionString: String, onShowLicenses: () -> Unit) {
    val context = LocalContext.current
    val serverVersion = ScrcpyConfig.SCRCPY_VERSION
    SettingsSection(title = stringResource(R.string.settings_section_about), icon = Icons.Outlined.Info) {
        SectionListItem(icon = Icons.Outlined.Storage, headline = versionString, supporting = stringResource(R.string.settings_scrcpy_server, serverVersion))
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onShowLicenses, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.settings_view_licenses)) }
        }
        SectionListItem(
            icon = Icons.Outlined.Code,
            headline = stringResource(R.string.settings_open_github),
            supporting = stringResource(R.string.settings_open_github_description),
            onClick = { context.openGithub() }
        )
        SectionListItem(
            icon = Icons.Outlined.Forum,
            headline = stringResource(R.string.settings_telegram_support),
            supporting = stringResource(R.string.settings_telegram_support_description),
            onClick = { context.openTelegramSupportGroup() }
        )
        SectionListItem(
            icon = Icons.Outlined.Campaign,
            headline = stringResource(R.string.settings_telegram_channel),
            supporting = stringResource(R.string.settings_telegram_channel_description),
            onClick = { context.openTelegramChannel() }
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ── Recording section ─────────────────────────────────────────────────────────

@Composable
private fun RecordingSection(
    preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions,
    onStorageClick: () -> Unit, onOpenContactsIncoming: () -> Unit, onOpenContactsOutgoing: () -> Unit
) {
    val context = LocalContext.current
    val storageMode          = remember(updateTrigger) { preferences.getStorageMode() }
    val recordingFolderLabel = remember(updateTrigger) { SafHelper.getFolderDisplayNameOrNull(context, preferences.getRecordingFolderUri()) }
    val fileNameFormat       = remember(updateTrigger) { preferences.getFileNameTemplate() }
    val autoRecordIncoming   = remember(updateTrigger) { preferences.isAutoRecordIncomingEnabled() }
    val autoRecordOutgoing   = remember(updateTrigger) { preferences.isAutoRecordOutgoingEnabled() }
    val ignoreAnonymousIncoming    = remember(updateTrigger) { preferences.isIgnoreAnonymousIncomingEnabled() }
    val ignoreCrossCountryIncoming = remember(updateTrigger) { preferences.isIgnoreCrossCountryIncomingEnabled() }
    val ignoreContactsModeIncoming = remember(updateTrigger) { preferences.getIgnoreContactsModeIncoming() }
    val ignoreContactsModeOutgoing = remember(updateTrigger) { preferences.getIgnoreContactsModeOutgoing() }
    val ignoreCrossCountryOutgoing = remember(updateTrigger) { preferences.isIgnoreCrossCountryOutgoingEnabled() }
    val ignoredContactsIncomingCount = remember(updateTrigger) { preferences.getIgnoredContactsIncoming().size }
    val ignoredContactsOutgoingCount = remember(updateTrigger) { preferences.getIgnoredContactsOutgoing().size }
    var showFileNameFormatDialog by remember { mutableStateOf(false) }

    val recordOnAnswer = remember(updateTrigger) { preferences.isRecordOnAnswerEnabled() }

    val appLockEnabled = remember(updateTrigger) { preferences.isAppLockEnabled() }
    val appLockMethod  = remember(updateTrigger) { preferences.getAppLockMethod() }
    var showAppLockSetupDialog by remember { mutableStateOf(false) }
    var showAppLockVerifyDialog by remember { mutableStateOf(false) }
    var pendingAfterAppLockVerify by remember { mutableStateOf<(() -> Unit)?>(null) }

    val recordWhatsAppCalls = remember(updateTrigger) { preferences.isRecordWhatsAppCallsEnabled() }
    val recordTelegramCalls = remember(updateTrigger) { preferences.isRecordTelegramCallsEnabled() }
    var showAppCallRecordingDialog by remember { mutableStateOf(false) }
    var showAppCallRecordingUnavailableDialog by remember { mutableStateOf(false) }
    val appCallRecordingSummary = buildList {
        if (recordWhatsAppCalls) add(stringResource(R.string.app_call_target_whatsapp))
        if (recordTelegramCalls) add(stringResource(R.string.app_call_target_telegram))
    }.let { enabledTargets ->
        if (enabledTargets.isEmpty()) stringResource(R.string.settings_app_call_recording_off) else enabledTargets.joinToString(", ")
    }
    val appCallRecordingEnabled = recordWhatsAppCalls || recordTelegramCalls

    val storageSupportingText = when (storageMode) {
        AppPreferences.StorageMode.PRIVATE    -> stringResource(R.string.storage_mode_private_label)
        AppPreferences.StorageMode.SAF_FOLDER -> recordingFolderLabel ?: stringResource(R.string.settings_tap_to_select_folder)
        null                                   -> stringResource(R.string.settings_tap_to_select_folder)
    }
    val storageIcon = if (storageMode == AppPreferences.StorageMode.PRIVATE) Icons.Outlined.Lock else Icons.Outlined.Folder

    SettingsSection(title = stringResource(R.string.settings_section_recording), icon = Icons.Outlined.FiberManualRecord) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.PhoneCallback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            headlineContent = { Text(stringResource(R.string.settings_record_on_answer), style = MaterialTheme.typography.bodyMedium) },
            supportingContent = { Text(stringResource(R.string.settings_record_on_answer_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                Switch(checked = recordOnAnswer, onCheckedChange = { actions.setRecordOnAnswer(it) })
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        ListItem(
            modifier = Modifier.clickable {
                if (appLockEnabled) {
                    pendingAfterAppLockVerify = { showAppLockSetupDialog = true }
                    showAppLockVerifyDialog = true
                } else {
                    showAppLockSetupDialog = true
                }
            },
            leadingContent = {
                Crossfade(targetState = appLockEnabled, label = "appLockRowIcon") { enabled ->
                    Icon(
                        imageVector = if (enabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            headlineContent = { Text("App Lock", style = MaterialTheme.typography.bodyMedium) },
            supportingContent = {
                Text(
                    text = if (appLockEnabled) "Protected with ${appLockMethodLabel(appLockMethod)}" else "Off · tap to set up",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (appLockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = appLockEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showAppLockSetupDialog = true
                        } else {
                            pendingAfterAppLockVerify = { actions.disableAppLock() }
                            showAppLockVerifyDialog = true
                        }
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        SectionListItem(
            icon = Icons.Outlined.Forum,
            headline = stringResource(R.string.settings_app_call_recording_label),
            supporting = appCallRecordingSummary,
            supportingColor = if (appCallRecordingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { showAppCallRecordingUnavailableDialog = true }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        SectionListItem(icon = storageIcon, headline = stringResource(R.string.settings_recording_folder_label), supporting = storageSupportingText, supportingColor = MaterialTheme.colorScheme.primary, onClick = onStorageClick)
        SectionListItem(icon = Icons.Outlined.DriveFileRenameOutline, headline = stringResource(R.string.settings_file_name_template), supporting = fileNameFormat, supportingColor = MaterialTheme.colorScheme.primary, onClick = { showFileNameFormatDialog = true })
    }

    // Incoming
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Rounded.CallReceived, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            }
            Text(text = stringResource(R.string.settings_auto_record_incoming), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ToggleListItem(label = stringResource(R.string.settings_auto_record_incoming), checked = autoRecordIncoming, onCheckedChange = { actions.setAutoRecordIncoming(it) })
                AnimatedVisibility(visible = autoRecordIncoming, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ToggleListItem(label = stringResource(R.string.settings_ignore_anonymous_incoming), checked = ignoreAnonymousIncoming, onCheckedChange = { actions.setIgnoreAnonymousIncoming(it) })
                        ToggleListItem(label = stringResource(R.string.settings_ignore_cross_country_incoming), checked = ignoreCrossCountryIncoming, onCheckedChange = { actions.setIgnoreCrossCountryIncoming(it) }, enabled = ignoreAnonymousIncoming)
                        IgnoreContactsOptions(label = stringResource(R.string.settings_ignore_contacts_incoming), selectedEnum = ignoreContactsModeIncoming, selectedCount = ignoredContactsIncomingCount, onSelected = { actions.setIgnoreContactsModeIncoming(it) }, onSelectContacts = onOpenContactsIncoming)
                    }
                }
            }
        }
    }

    // Outgoing
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Rounded.CallMade, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            }
            Text(text = stringResource(R.string.settings_auto_record_outgoing), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ToggleListItem(label = stringResource(R.string.settings_auto_record_outgoing), checked = autoRecordOutgoing, onCheckedChange = { actions.setAutoRecordOutgoing(it) })
                AnimatedVisibility(visible = autoRecordOutgoing, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ToggleListItem(label = stringResource(R.string.settings_ignore_cross_country_outgoing), checked = ignoreCrossCountryOutgoing, onCheckedChange = { actions.setIgnoreCrossCountryOutgoing(it) })
                        IgnoreContactsOptions(label = stringResource(R.string.settings_ignore_contacts_outgoing), selectedEnum = ignoreContactsModeOutgoing, selectedCount = ignoredContactsOutgoingCount, onSelected = { actions.setIgnoreContactsModeOutgoing(it) }, onSelectContacts = onOpenContactsOutgoing)
                    }
                }
            }
        }
    }

    if (showFileNameFormatDialog) {
        FileNameFormatDialog(initialFormat = fileNameFormat, onConfirm = { format -> actions.setFileNameTemplate(format); showFileNameFormatDialog = false }, onDismiss = { showFileNameFormatDialog = false })
    }

    if (showAppCallRecordingUnavailableDialog) {
        AlertDialog(
            onDismissRequest = {
                showAppCallRecordingUnavailableDialog = false
                showAppCallRecordingDialog = true
            },
            confirmButton = {
                TextButton(onClick = {
                    showAppCallRecordingUnavailableDialog = false
                    showAppCallRecordingDialog = true
                }) {
                    Text(stringResource(R.string.general_close))
                }
            },
            text = { Text(stringResource(R.string.settings_app_call_recording_unavailable_message)) }
        )
    }

    if (showAppCallRecordingDialog) {
        AppCallRecordingDialog(
            whatsAppEnabled = recordWhatsAppCalls,
            telegramEnabled = recordTelegramCalls,
            onWhatsAppToggle = { enabled -> actions.setRecordCallsFromApp(AppCallTarget.WHATSAPP, enabled) },
            onTelegramToggle = { enabled -> actions.setRecordCallsFromApp(AppCallTarget.TELEGRAM, enabled) },
            onDismiss = { showAppCallRecordingDialog = false }
        )
    }

    if (showAppLockSetupDialog) {
        AppLockSetupDialog(
            onSetPin = { pin -> actions.setAppLockPin(pin) },
            onSetPassword = { password -> actions.setAppLockPassword(password) },
            onSetBiometric = { actions.setAppLockBiometric() },
            onDismiss = { showAppLockSetupDialog = false }
        )
    }

    if (showAppLockVerifyDialog) {
        AppLockVerifyDialog(
            method = appLockMethod,
            onVerifySecret = { secret -> actions.verifyAppLockSecret(secret) },
            onVerified = {
                showAppLockVerifyDialog = false
                val pending = pendingAfterAppLockVerify
                pendingAfterAppLockVerify = null
                pending?.invoke()
            },
            onDismiss = {
                showAppLockVerifyDialog = false
                pendingAfterAppLockVerify = null
            }
        )
    }
}

// ── Auto Delete section ───────────────────────────────────────────────────────

@Composable
private fun AutoDeleteSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    var timeEnabled  by remember(updateTrigger) { mutableStateOf(preferences.isAutoDeleteByTimeEnabled()) }
    var timeValue    by remember(updateTrigger) { mutableStateOf(preferences.getAutoDeleteByTimeValue().toString()) }
    var timeUnit     by remember(updateTrigger) { mutableStateOf(preferences.getAutoDeleteByTimeUnit()) }
    var spaceEnabled by remember(updateTrigger) { mutableStateOf(preferences.isAutoDeleteBySpaceEnabled()) }
    var spaceValue   by remember(updateTrigger) { mutableStateOf(preferences.getAutoDeleteBySpaceValue().toString()) }
    var spaceUnit    by remember(updateTrigger) { mutableStateOf(preferences.getAutoDeleteBySpaceUnit()) }

    SettingsSection(title = "Auto Delete", icon = Icons.Outlined.DeleteSweep) {

        // ── Time-based sub-section ─────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(13.dp)) }
                Text("Auto Delete With Respect To Time", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = timeEnabled,
                    onCheckedChange = { timeEnabled = it; actions.setAutoDeleteByTimeEnabled(it) },
                    modifier = Modifier.scale(0.82f)
                )
            }
            AnimatedVisibility(
                visible = timeEnabled,
                enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                exit  = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                    Text("Delete recordings older than:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = timeValue,
                            onValueChange = { v ->
                                val d = v.filter { it.isDigit() }.take(5)
                                timeValue = d
                                d.toIntOrNull()?.let { actions.setAutoDeleteByTimeValue(it) }
                            },
                            modifier = Modifier.width(88.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            label = { Text("Amount", style = MaterialTheme.typography.labelSmall) }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("hours", "days").forEach { opt ->
                                val sel = timeUnit == opt
                                Surface(
                                    onClick = { timeUnit = opt; actions.setAutoDeleteByTimeUnit(opt) },
                                    shape = CircleShape,
                                    color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = if (sel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), Alignment.Center) {
                                        Text(opt.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    if (timeValue.toIntOrNull() != null && timeValue.isNotBlank()) {
                        Text("Recordings older than $timeValue ${timeUnit} will be deleted on next app open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── Space-based sub-section ────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(13.dp)) }
                Text("Auto Delete With Respect To Space", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = spaceEnabled,
                    onCheckedChange = { spaceEnabled = it; actions.setAutoDeleteBySpaceEnabled(it) },
                    modifier = Modifier.scale(0.82f)
                )
            }
            AnimatedVisibility(
                visible = spaceEnabled,
                enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                exit  = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                    Text("Delete oldest recordings when folder exceeds:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = spaceValue,
                            onValueChange = { v ->
                                val d = v.filter { it.isDigit() }.take(6)
                                spaceValue = d
                                d.toIntOrNull()?.let { actions.setAutoDeleteBySpaceValue(it) }
                            },
                            modifier = Modifier.width(96.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            label = { Text("Size", style = MaterialTheme.typography.labelSmall) }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("mb", "gb").forEach { opt ->
                                val sel = spaceUnit == opt
                                Surface(
                                    onClick = { spaceUnit = opt; actions.setAutoDeleteBySpaceUnit(opt) },
                                    shape = CircleShape,
                                    color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = if (sel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), Alignment.Center) {
                                        Text(opt.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    if (spaceValue.toIntOrNull() != null && spaceValue.isNotBlank()) {
                        Text("Oldest recordings deleted when folder exceeds $spaceValue ${spaceUnit.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Audio section ─────────────────────────────────────────────────────────────

@Composable
private fun AudioSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val isDebugEnabled = remember(updateTrigger) { preferences.isDebugEnabled() }
    val audioSource    = remember(updateTrigger) { preferences.getAudioSource() }
    val audioCodec     = remember(updateTrigger) { preferences.getAudioCodec() }
    val savedBitRate   = remember(updateTrigger) { preferences.getAudioBitRate() }
    SettingsSection(title = stringResource(R.string.settings_section_audio), icon = Icons.Outlined.Equalizer) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val currentSdk = Build.VERSION.SDK_INT
            val audioSourceOptions = ScrcpyAudioSource.entries.filter { !it.isDebugOnly || isDebugEnabled }.map { source ->
                OptionItem(key = source.cliKey, label = stringResource(source.titleResId), description = stringResource(source.descriptionResId), enabled = currentSdk >= source.minApi && (source.maxApi == null || currentSdk <= source.maxApi))
            }
            val selectedAudio = audioSourceOptions.find { it.key == audioSource } ?: audioSourceOptions.first()
            M3DropdownField(label = stringResource(R.string.settings_audio_source), selected = selectedAudio, options = audioSourceOptions, onOptionSelected = { actions.setAudioSource(it.key) })
            selectedAudio.description?.let { desc -> Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
            val codecOptions = ScrcpyAudioCodec.entries.map { OptionItem(it.cliKey, stringResource(it.titleResId)) }
            M3DropdownField(label = stringResource(R.string.settings_audio_codec), selected = codecOptions.find { it.key == audioCodec } ?: codecOptions.first(), options = codecOptions, onOptionSelected = { actions.setAudioCodec(it.key) })
            if (!LocalInspectionMode.current && audioCodec != ScrcpyAudioCodec.AAC.cliKey) {
                Text(text = stringResource(R.string.settings_audio_bitrate_recommendation), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
            val bitrateOptions = listOf(8000, 16000, 32000, 64000, 128000).map { OptionItem(it.toString(), stringResource(R.string.audio_bitrate_kbps, it / 1000)) }
            M3DropdownField(label = stringResource(R.string.settings_audio_bitrate), selected = bitrateOptions.find { it.key == savedBitRate.toString() } ?: bitrateOptions.first(), options = bitrateOptions, onOptionSelected = { actions.setAudioBitRate(it.key.toInt()) })
        }
    }
}

// ── Security section ──────────────────────────────────────────────────────────

@Composable
private fun SecuritySection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val autoManageShizuku    = remember(updateTrigger) { preferences.isShizukuAutoManageEnabled() }
    val shizukuStartOnRecord = remember(updateTrigger) { preferences.isShizukuStartOnRecordEnabled() }
    val shizukuKeepAlive     = remember(updateTrigger) { preferences.isShizukuKeepAliveEnabled() }
    val shizukuAuthKey       = remember(updateTrigger) { preferences.getShizukuAuthKey() }
    SettingsSection(title = stringResource(R.string.settings_section_security), icon = Icons.Outlined.Shield) {
        ToggleListItem(label = stringResource(R.string.settings_shizuku_auto_manage), checked = autoManageShizuku, onCheckedChange = { actions.setShizukuAutoManageEnabled(it) }, description = stringResource(R.string.settings_shizuku_auto_manage_desc))
        AnimatedVisibility(visible = autoManageShizuku, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column {
                var textState by remember(shizukuAuthKey) { mutableStateOf(shizukuAuthKey) }
                val keyboardController = LocalSoftwareKeyboardController.current
                var isFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text(stringResource(R.string.settings_shizuku_auth_key)) },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    visualTransformation = if (isFocused) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password, showKeyboardOnFocus = true),
                    keyboardActions = KeyboardActions(onDone = { actions.setShizukuAuthKey(textState); keyboardController?.hide() })
                )
                ToggleListItem(label = stringResource(R.string.settings_shizuku_start_on_record), checked = shizukuStartOnRecord, onCheckedChange = { actions.setShizukuStartOnRecordEnabled(it) }, description = stringResource(R.string.settings_shizuku_start_on_record_desc))
                ToggleListItem(label = stringResource(R.string.settings_shizuku_keep_alive), checked = shizukuKeepAlive, onCheckedChange = { actions.setShizukuKeepAliveEnabled(it) }, description = stringResource(R.string.settings_shizuku_keep_alive_desc))
            }
        }
    }
}

// ── Debug section ─────────────────────────────────────────────────────────────

@Composable
private fun DebugSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions, onExportLogs: () -> Unit) {
    val isDebugEnabled    = remember(updateTrigger) { preferences.isDebugEnabled() }
    val debugCallerNumber = remember(updateTrigger) { preferences.getDebugCallerNumber() }
    val isLoggingEnabled  = remember(updateTrigger) { preferences.isLoggingEnabled() }
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.settings_section_debug), icon = Icons.Outlined.BugReport) {
        ToggleListItem(label = stringResource(R.string.settings_debug_logging_enabled), checked = isLoggingEnabled, onCheckedChange = { actions.setLoggingEnabled(it) }, description = if (!isLoggingEnabled) stringResource(R.string.settings_debug_logging_enabled_description) else null)
        if (isLoggingEnabled) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = stringResource(R.string.settings_debug_logging_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                Text(text = stringResource(R.string.settings_debug_logging_steps), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(R.string.settings_debug_logging_step_warning), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                if (isDebugEnabled) { Spacer(modifier = Modifier.height(5.dp)); Text(text = stringResource(R.string.settings_debug_logging_step_warning_no_redaction), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExportLogs, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.settings_debug_logging_generate_report)) }
                    OutlinedButton(onClick = { context.openGithubReportIssue() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.settings_debug_logging_report_on_github)) }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), thickness = 0.5.dp)
        ToggleListItem(label = stringResource(R.string.settings_debug_mode), checked = isDebugEnabled, onCheckedChange = { actions.setDebugEnabled(it) }, description = stringResource(R.string.settings_debug_mode_description))
        if (isDebugEnabled) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var textState by remember(debugCallerNumber) { mutableStateOf(debugCallerNumber) }
                val allowedChars = "^[0-9+-]*$".toRegex()
                val keyboardController = LocalSoftwareKeyboardController.current
                OutlinedTextField(
                    value = textState,
                    onValueChange = { newValue -> if (newValue.matches(allowedChars)) { textState = newValue; actions.setDebugCallerNumber(newValue) } },
                    label = { Text(stringResource(R.string.settings_debug_caller_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Phone, showKeyboardOnFocus = true),
                    keyboardActions = KeyboardActions(onDone = { actions.setDebugCallerNumber(textState); keyboardController?.hide() })
                )
                DebugActionGrid(actions)
            }
        }
    }
}

// ── Shared helper composables ─────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            }
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)).padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SectionListItem(icon: ImageVector, headline: String, supporting: String? = null, supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: (() -> Unit)? = null) {
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ListItem(modifier = mod, leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }, headlineContent = { Text(headline, style = MaterialTheme.typography.bodyMedium) }, supportingContent = supporting?.let { { Text(it, color = supportingColor, style = MaterialTheme.typography.bodySmall) } }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
}

@Composable
private fun IgnoreContactsOptions(label: String, selectedEnum: AppPreferences.IgnoreContactsMode, selectedCount: Int, onSelected: (AppPreferences.IgnoreContactsMode) -> Unit, onSelectContacts: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AppPreferences.IgnoreContactsMode.entries.forEach { mode ->
                val selected = selectedEnum == mode
                val chipLabel = when (mode) {
                    AppPreferences.IgnoreContactsMode.NONE     -> stringResource(R.string.settings_ignore_contacts_none)
                    AppPreferences.IgnoreContactsMode.ALL      -> stringResource(R.string.settings_ignore_contacts_all)
                    AppPreferences.IgnoreContactsMode.SELECTED -> stringResource(R.string.settings_ignore_contacts_selected)
                }
                Surface(
                    onClick = { onSelected(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp)) {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        if (selectedEnum == AppPreferences.IgnoreContactsMode.SELECTED) {
            Button(onClick = onSelectContacts, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Text(stringResource(R.string.settings_select_contacts, selectedCount))
            }
        }
    }
}

@Composable
private fun DebugActionGrid(actions: SettingsActions) {
    val items = listOf(DebugAction.RINGING to stringResource(R.string.settings_debug_action_ringing), DebugAction.OFFHOOK to stringResource(R.string.settings_debug_action_offhook), DebugAction.IDLE to stringResource(R.string.settings_debug_action_idle))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items.forEach { (action, label) -> FilledTonalButton(onClick = { actions.triggerDebugAction(action) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(label, style = MaterialTheme.typography.labelSmall) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        val mockContext = LocalContext.current
        val dummyPreferences = AppPreferences(mockContext)
        val dummyActions = object : SettingsActions {
            override fun setAutoRecordIncoming(enabled: Boolean) {}
            override fun setAutoRecordOutgoing(enabled: Boolean) {}
            override fun setRecordOnAnswer(enabled: Boolean) {}
            override fun setVibrationEnabled(enabled: Boolean) {}
            override fun setIgnoreAnonymousIncoming(enabled: Boolean) {}
            override fun setIgnoreCrossCountryIncoming(enabled: Boolean) {}
            override fun setIgnoreCrossCountryOutgoing(enabled: Boolean) {}
            override fun setIgnoreContactsModeIncoming(modeEnum: AppPreferences.IgnoreContactsMode) {}
            override fun setIgnoreContactsModeOutgoing(modeEnum: AppPreferences.IgnoreContactsMode) {}
            override fun setAudioSource(source: String) {}
            override fun setAudioCodec(codec: String) {}
            override fun setAudioBitRate(bitRate: Int) {}
            override fun setThemeMode(mode: AppPreferences.ThemeMode) {}
            override fun setDynamicColorEnabled(enabled: Boolean) {}
            override fun setShowToastsEnabled(enabled: Boolean) {}
            override fun setRecordingNotificationsEnabled(enabled: Boolean) {}
            override fun setAppLanguage(languageCode: String) {}
            override fun setLoggingEnabled(enabled: Boolean) {}
            override fun setDebugEnabled(enabled: Boolean) {}
            override fun setDebugCallerNumber(number: String) {}
            override fun triggerDebugAction(action: DebugAction) {}
            override fun exportLogs(uri: Uri) {}
            override fun getAppVersion(): String = "Version 3.0.0 (Mock)"
            override fun setShizukuAutoManageEnabled(enabled: Boolean) {}
            override fun setShizukuStartOnRecordEnabled(enabled: Boolean) {}
            override fun setShizukuKeepAliveEnabled(enabled: Boolean) {}
            override fun setShizukuAuthKey(key: String) {}
            override fun setFileNameTemplate(template: String) {}
            override fun setAccentColor(argb: Int) {}
            override fun setAutoDeleteByTimeEnabled(enabled: Boolean) {}
            override fun setAutoDeleteByTimeValue(value: Int) {}
            override fun setAutoDeleteByTimeUnit(unit: String) {}
            override fun setAutoDeleteBySpaceEnabled(enabled: Boolean) {}
            override fun setAutoDeleteBySpaceValue(value: Int) {}
            override fun setAutoDeleteBySpaceUnit(unit: String) {}
            override fun setAutoUpdateCheckEnabled(enabled: Boolean) {}
            override fun setAppLockPin(pin: String) {}
            override fun setAppLockPassword(password: String) {}
            override fun setAppLockBiometric() {}
            override fun disableAppLock() {}
            override fun verifyAppLockSecret(secret: String): Boolean = true
            override fun setRecordCallsFromApp(target: AppCallTarget, enabled: Boolean) {}
        }
        SettingsContent(preferences = dummyPreferences, updateTrigger = 0, actions = dummyActions, contactPickerState = null, onStorageClick = {}, onOpenContactsIncoming = {}, onOpenContactsOutgoing = {}, onConfirmContacts = {}, onDismissContacts = {}, onExportLogs = {}, onBack = {})
    }
}
