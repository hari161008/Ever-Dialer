package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.coolappstore.everdialer.by.svhp.controller.util.BackupManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AutoBackupSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val ColorGreen = Color(0xFF4CAF50)
private val ColorRed = Color(0xFFE53935)

private sealed class BackupScreenState {
    object Idle : BackupScreenState()
    object Creating : BackupScreenState()
    object Restoring : BackupScreenState()
    data class BackupSuccess(val path: String) : BackupScreenState()
    object RestoreSuccess : BackupScreenState()
    data class Error(val message: String) : BackupScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun BackupSettingsScreen(
    navigator: DestinationsNavigator,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = koinInject<PreferenceManager>()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var screenState by remember { mutableStateOf<BackupScreenState>(BackupScreenState.Idle) }

    // ── Backup options state ──────────────────────────────────────────────────
    var backupSettings by remember { mutableStateOf(true) }
    var backupCallingCards by remember { mutableStateOf(true) }
    var backupNotes by remember { mutableStateOf(true) }
    var backupContactGroups by remember { mutableStateOf(false) }
    var backupRecordings by remember { mutableStateOf(true) }
    var backupContacts by remember { mutableStateOf(false) }
    var backupCallLogs by remember { mutableStateOf(false) }
    var backupSms by remember { mutableStateOf(false) }

    val hasAnyBackupSelected = backupSettings || backupCallingCards || backupNotes ||
            backupContactGroups || backupRecordings || backupContacts || backupCallLogs || backupSms

    // ── Restore state ─────────────────────────────────────────────────────────
    var selectedRestoreFile by remember { mutableStateOf<File?>(null) }
    var restoreContents by remember { mutableStateOf<BackupManager.BackupContents?>(null) }
    var restoreSettings by remember { mutableStateOf(true) }
    var restoreCallingCards by remember { mutableStateOf(true) }
    var restoreNotes by remember { mutableStateOf(true) }
    var restoreContactGroups by remember { mutableStateOf(false) }
    var restoreRecordings by remember { mutableStateOf(true) }
    var restoreContacts by remember { mutableStateOf(false) }
    var restoreCallLogs by remember { mutableStateOf(false) }
    var restoreSms by remember { mutableStateOf(false) }

    val hasAnyRestoreSelected = restoreContents != null && (
            (restoreSettings && (restoreContents!!.hasSettings || restoreContents!!.hasLiquidGlass)) ||
            (restoreCallingCards && restoreContents!!.hasCallingCards) ||
            (restoreNotes && restoreContents!!.hasNotes) ||
            (restoreContactGroups && restoreContents!!.hasContactGroups) ||
            (restoreRecordings && restoreContents!!.hasRecordings) ||
            (restoreContacts && restoreContents!!.hasContacts) ||
            (restoreCallLogs && restoreContents!!.hasCallLogs) ||
            (restoreSms && restoreContents!!.hasSms)
    )

    // ── File Launchers ────────────────────────────────────────────────────────
    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            screenState = BackupScreenState.Creating
            scope.launch(Dispatchers.IO) {
                try {
                    val ok = context.contentResolver.openOutputStream(uri)?.use { output ->
                        BackupManager.writeBackup(
                            context = context,
                            outputStream = output,
                            backupSettings = backupSettings,
                            backupCallingCards = backupCallingCards,
                            backupNotes = backupNotes,
                            backupContactGroups = backupContactGroups,
                            backupRecordings = backupRecordings,
                            backupContacts = backupContacts,
                            backupCallLogs = backupCallLogs,
                            backupSms = backupSms,
                            backupLiquidGlass = backupSettings
                        )
                    } ?: false
                    withContext(Dispatchers.Main) {
                        screenState = if (ok) {
                            BackupScreenState.BackupSuccess("Backup saved successfully")
                        } else {
                            BackupScreenState.Error("Failed to save backup")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        screenState = BackupScreenState.Error(e.message ?: "Failed to save backup")
                    }
                }
            }
        }
    }

    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tmpFile = File(context.cacheDir, "restore_page_tmp.everdialer")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmpFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val contents = BackupManager.inspectBackup(tmpFile)
                    withContext(Dispatchers.Main) {
                        selectedRestoreFile = tmpFile
                        restoreContents = contents
                        restoreSettings = contents.hasSettings || contents.hasLiquidGlass
                        restoreCallingCards = contents.hasCallingCards
                        restoreNotes = contents.hasNotes
                        restoreContactGroups = contents.hasContactGroups
                        restoreRecordings = contents.hasRecordings
                        restoreContacts = contents.hasContacts
                        restoreCallLogs = contents.hasCallLogs
                        restoreSms = contents.hasSms
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        screenState = BackupScreenState.Error(e.message ?: "Failed to read backup file")
                    }
                }
            }
        }
    }

    fun executeShare() {
        screenState = BackupScreenState.Creating
        scope.launch(Dispatchers.IO) {
            val file = BackupManager.createBackup(
                context = context,
                backupSettings = backupSettings,
                backupCallingCards = backupCallingCards,
                backupNotes = backupNotes,
                backupContactGroups = backupContactGroups,
                backupRecordings = backupRecordings,
                backupContacts = backupContacts,
                backupCallLogs = backupCallLogs,
                backupSms = backupSms,
                backupLiquidGlass = backupSettings
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
                    screenState = BackupScreenState.BackupSuccess(file.absolutePath)
                } else {
                    screenState = BackupScreenState.Error("Failed to create backup")
                }
            }
        }
    }

    fun executeRestore() {
        val file = selectedRestoreFile ?: return
        screenState = BackupScreenState.Restoring
        scope.launch(Dispatchers.IO) {
            try {
                val ok = BackupManager.restoreBackup(
                    context = context,
                    backupFile = file,
                    restoreSettings = restoreSettings,
                    restoreCallingCards = restoreCallingCards,
                    restoreNotes = restoreNotes,
                    restoreContactGroups = restoreContactGroups,
                    restoreRecordings = restoreRecordings,
                    restoreContacts = restoreContacts,
                    restoreCallLogs = restoreCallLogs,
                    restoreSms = restoreSms,
                    restoreLiquidGlass = restoreSettings
                )
                withContext(Dispatchers.Main) {
                    screenState = if (ok) BackupScreenState.RestoreSuccess else BackupScreenState.Error("Restore failed")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    screenState = BackupScreenState.Error(e.message ?: "Unknown error during restore")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Backup & Restore",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + navBarBottom)
        ) {
            // ── Pill Tab Selector ─────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "Create Backup" to Icons.Default.Backup,
                            "Restore Backup" to Icons.Default.Restore
                        )
                        tabs.forEachIndexed { index, (label, icon) ->
                            val isSelected = selectedTab == index
                            val bgAnim by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "pillTabBg_$index"
                            )
                            val fgAnim by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "pillTabFg_$index"
                            )
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = bgAnim,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { selectedTab = index }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = fgAnim,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = fgAnim
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Tab 0: Create Backup ──────────────────────────────────────────
            if (selectedTab == 0) {
                // 1. Save & Share Buttons at the top (above configuration containers)
                item {
                    RivoAnimatedSection(delayMs = 30L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    saveBackupLauncher.launch("EverDialer_Backup_$timestamp.everdialer")
                                },
                                enabled = hasAnyBackupSelected,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Backup", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            Button(
                                onClick = { executeShare() },
                                enabled = hasAnyBackupSelected,
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share Backup", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }

                // 2. Auto Backup Container
                item {
                    val isAutoBackupOn = prefs.isAutoBackupEnabled()
                    RivoAnimatedSection(delayMs = 50L) {
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Auto backup",
                                supporting = if (isAutoBackupOn) "Enabled · Automatic backups scheduled" else "Disabled · Configure automatic backups",
                                leadingIcon = Icons.Outlined.CloudSync,
                                iconContainerColor = Color(0xFF673AB7),
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    navigator.navigate(AutoBackupSettingsScreenDestination())
                                }
                            )
                        }
                    }
                }

                // 3. Data to include
                item {
                    RivoAnimatedSection(delayMs = 70L) {
                        Column {
                            Text(
                                text = "Select data to include in your backup file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Backup settings",
                                    supporting = "Preferences, widgets, liquid glass, blur effects & app configuration",
                                    leadingIcon = Icons.Outlined.Settings,
                                    iconContainerColor = Color(0xFF4CAF50),
                                    checked = backupSettings,
                                    onCheckedChange = { backupSettings = it }
                                )
                                CardDivider()
                                RivoSwitchListItem(
                                    headline = "Backup calling cards",
                                    supporting = "Saved calling cards, contact backgrounds, caller UI & PFP customization",
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
                                CardDivider()
                                RivoSwitchListItem(
                                    headline = "Backup SMS",
                                    supporting = "Device SMS messages and chats",
                                    leadingIcon = Icons.Outlined.Chat,
                                    iconContainerColor = Color(0xFF0288D1),
                                    checked = backupSms,
                                    onCheckedChange = { backupSms = it }
                                )
                            }
                        }
                    }
                }


            }

            // ── Tab 1: Restore Backup ─────────────────────────────────────────
            if (selectedTab == 1) {
                item {
                    RivoAnimatedSection(delayMs = 50L) {
                        Column {
                            Text(
                                text = "Select an .everdialer backup file from your storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
                            RivoExpressiveCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = if (selectedRestoreFile != null) {
                                            selectedRestoreFile!!.name
                                        } else {
                                            "No backup file selected"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (selectedRestoreFile != null) {
                                            "${selectedRestoreFile!!.length() / 1024} KB"
                                        } else {
                                            "Tap below to choose a backup file to restore"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    FilledTonalButton(
                                        onClick = { restoreFilePickerLauncher.launch("*/*") },
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (selectedRestoreFile != null) "Choose Another File" else "Browse Backup File")
                                    }
                                }
                            }
                        }
                    }
                }

                if (restoreContents != null) {
                    val contents = restoreContents!!
                    item {
                        RivoAnimatedSection(delayMs = 100L) {
                            Column {
                                Text(
                                    text = "Select items to restore from this file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                                )
                                RivoExpressiveCard {
                                    RivoSwitchListItem(
                                        headline = "Restore settings",
                                        supporting = if (contents.hasSettings || contents.hasLiquidGlass) "App preferences, widgets, liquid glass, blur effects & configuration" else "Not present in this backup",
                                        leadingIcon = Icons.Outlined.Settings,
                                        iconContainerColor = Color(0xFF4CAF50),
                                        checked = restoreSettings,
                                        onCheckedChange = { restoreSettings = it },
                                        enabled = contents.hasSettings || contents.hasLiquidGlass
                                    )
                                    CardDivider()
                                    RivoSwitchListItem(
                                        headline = "Restore calling cards",
                                        supporting = if (contents.hasCallingCards) "Saved calling cards, contact backgrounds, caller UI & PFP customization" else "Not present in this backup",
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
                                    CardDivider()
                                    RivoSwitchListItem(
                                        headline = "Restore SMS",
                                        supporting = if (contents.hasSms) "Device SMS messages and chats" else "Not present in this backup",
                                        leadingIcon = Icons.Outlined.Chat,
                                        iconContainerColor = Color(0xFF0288D1),
                                        checked = restoreSms,
                                        onCheckedChange = { restoreSms = it },
                                        enabled = contents.hasSms
                                    )
                                }
                            }
                        }
                    }

                    item {
                        RivoAnimatedSection(delayMs = 150L) {
                            Button(
                                onClick = { executeRestore() },
                                enabled = hasAnyRestoreSelected,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(top = 8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore Selected", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    // ── Dialogs for States ────────────────────────────────────────────────────
    when (val state = screenState) {
        is BackupScreenState.Creating -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                icon = { CircularProgressIndicator(modifier = Modifier.size(36.dp)) },
                title = { Text("Creating Backup…", fontWeight = FontWeight.Bold) },
                text = { Text("Please wait while your backup archive is being created.") }
            )
        }
        is BackupScreenState.Restoring -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                icon = { CircularProgressIndicator(modifier = Modifier.size(36.dp)) },
                title = { Text("Restoring Backup…", fontWeight = FontWeight.Bold) },
                text = { Text("Please wait while your selected data is restored.") }
            )
        }
        is BackupScreenState.BackupSuccess -> {
            AlertDialog(
                onDismissRequest = { screenState = BackupScreenState.Idle },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorGreen, modifier = Modifier.size(36.dp)) },
                title = { Text("Backup Created", fontWeight = FontWeight.Bold) },
                text = { Text("Your backup file has been created successfully:\n${state.path}") },
                confirmButton = {
                    TextButton(onClick = { screenState = BackupScreenState.Idle }) {
                        Text("OK")
                    }
                }
            )
        }
        is BackupScreenState.RestoreSuccess -> {
            AlertDialog(
                onDismissRequest = { screenState = BackupScreenState.Idle },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorGreen, modifier = Modifier.size(36.dp)) },
                title = { Text("Restore Complete", fontWeight = FontWeight.Bold) },
                text = { Text("Your data and settings have been restored successfully. It is recommended to restart the app to apply all visual changes.") },
                confirmButton = {
                    TextButton(onClick = { screenState = BackupScreenState.Idle }) {
                        Text("OK")
                    }
                }
            )
        }
        is BackupScreenState.Error -> {
            AlertDialog(
                onDismissRequest = { screenState = BackupScreenState.Idle },
                icon = { Icon(Icons.Default.Error, contentDescription = null, tint = ColorRed, modifier = Modifier.size(36.dp)) },
                title = { Text("Operation Failed", fontWeight = FontWeight.Bold) },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { screenState = BackupScreenState.Idle }) {
                        Text("OK")
                    }
                }
            )
        }
        BackupScreenState.Idle -> {}
    }
}
