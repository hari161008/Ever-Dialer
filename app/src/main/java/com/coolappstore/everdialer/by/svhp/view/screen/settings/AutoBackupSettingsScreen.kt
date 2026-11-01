package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.coolappstore.everdialer.by.svhp.controller.util.AutoBackupManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.LocalCardCornerRadius
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun AutoBackupSettingsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()

    DisposableEffect(Unit) {
        NavBarVisibilityState.hideForSettingsEntry = true
        onDispose {
            NavBarVisibilityState.hideForSettingsEntry = false
        }
    }

    // Auto backup master state
    var autoBackupEnabled by remember { mutableStateOf(prefs.isAutoBackupEnabled()) }
    var folderUriStr by remember { mutableStateOf(prefs.getAutoBackupFolderUri()) }

    // Frequency state
    var intervalValueText by remember { mutableStateOf(prefs.getAutoBackupIntervalValue().toString()) }
    var intervalUnit by remember { mutableStateOf(prefs.getAutoBackupIntervalUnit()) }

    // What to backup state
    var backupSettings by remember { mutableStateOf(prefs.isAutoBackupItemSettings()) }
    var backupCallingCards by remember { mutableStateOf(prefs.isAutoBackupItemCallingCards()) }
    var backupNotes by remember { mutableStateOf(prefs.isAutoBackupItemNotes()) }
    var backupGroups by remember { mutableStateOf(prefs.isAutoBackupItemGroups()) }
    var backupRecordings by remember { mutableStateOf(prefs.isAutoBackupItemRecordings()) }
    var backupContacts by remember { mutableStateOf(prefs.isAutoBackupItemContacts()) }
    var backupCallLogs by remember { mutableStateOf(prefs.isAutoBackupItemCallLogs()) }
    var backupSms by remember { mutableStateOf(prefs.isAutoBackupItemSms()) }

    var showItemsDialog by remember { mutableStateOf(false) }

    // Auto delete state
    var autoDeleteTimeEnabled by remember { mutableStateOf(prefs.isAutoDeleteBackupsTimeEnabled()) }
    var autoDeleteTimeValueText by remember { mutableStateOf(prefs.getAutoDeleteBackupsTimeValue().toString()) }
    var autoDeleteTimeUnit by remember { mutableStateOf(prefs.getAutoDeleteBackupsTimeUnit()) }

    var autoDeleteCountEnabled by remember { mutableStateOf(prefs.isAutoDeleteBackupsCountEnabled()) }
    var autoDeleteCountValueText by remember { mutableStateOf(prefs.getAutoDeleteBackupsCountValue().toString()) }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            folderUriStr = uri.toString()
            prefs.setAutoBackupFolderUri(uri.toString())
        }
    }

    // Derive folder display label
    val isFolderSet = !folderUriStr.isNullOrBlank()
    val folderDisplayName = remember(folderUriStr) {
        if (!folderUriStr.isNullOrBlank()) {
            try {
                val doc = DocumentFile.fromTreeUri(context, Uri.parse(folderUriStr))
                doc?.name ?: folderUriStr
            } catch (_: Exception) {
                folderUriStr
            }
        } else {
            "No folder selected"
        }
    }

    // Summary of selected backup items
    val backupItemsSummary = remember(
        backupSettings, backupCallingCards, backupNotes,
        backupGroups, backupRecordings, backupContacts, backupCallLogs, backupSms
    ) {
        val list = mutableListOf<String>()
        if (backupSettings) list.add("Settings")
        if (backupCallingCards) list.add("Cards")
        if (backupNotes) list.add("Notes")
        if (backupGroups) list.add("Groups")
        if (backupRecordings) list.add("Recordings")
        if (backupContacts) list.add("Contacts")
        if (backupCallLogs) list.add("Call Logs")
        if (backupSms) list.add("SMS")
        if (list.isEmpty()) "None selected" else list.joinToString(", ")
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Auto Backup",
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
            // ── Master Switch Container ───────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Auto Backup",
                            supporting = if (autoBackupEnabled) "Automatic backup is turned on" else "Turn on to automatically backup app data",
                            leadingIcon = Icons.Outlined.CloudSync,
                            iconContainerColor = Color(0xFF673AB7),
                            checked = autoBackupEnabled,
                            onCheckedChange = { checked ->
                                autoBackupEnabled = checked
                                prefs.setAutoBackupEnabled(checked)
                                if (checked) {
                                    AutoBackupManager.scheduleAutoBackup(context)
                                } else {
                                    AutoBackupManager.cancelAutoBackup(context)
                                }
                            }
                        )
                    }
                }
            }

            // ── Features Below Master Switch ─────────────────────────────────
            item {
                val contentAlpha = if (autoBackupEnabled) 1f else 0.38f
                val isFolderMissing = autoBackupEnabled && !isFolderSet
                val lowerFeaturesEnabled = autoBackupEnabled && isFolderSet
                val lowerFeaturesAlpha = if (lowerFeaturesEnabled) 1f else 0.38f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Folder Selector Container (Red when not set)
                    RivoAnimatedSection(delayMs = 30L) {
                        Column {
                            Text(
                                text = "BACKUP LOCATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isFolderMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                            )
                            RivoExpressiveCard(
                                containerColor = if (isFolderMissing) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = if (isFolderMissing) Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(LocalCardCornerRadius.current)) else Modifier
                            ) {
                                RivoListItem(
                                    headline = "Backup Folder",
                                    supporting = if (isFolderSet) folderDisplayName else "No folder selected • Tap to select a backup folder",
                                    leadingIcon = if (isFolderMissing) Icons.Outlined.FolderOff else Icons.Outlined.Folder,
                                    iconContainerColor = if (isFolderMissing) MaterialTheme.colorScheme.error else Color(0xFF009688),
                                    trailingIcon = Icons.Default.FolderOpen,
                                    trailingIconTint = if (isFolderMissing) MaterialTheme.colorScheme.error else null,
                                    onClick = {
                                        if (autoBackupEnabled) {
                                            folderPickerLauncher.launch(null)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // ── Features below folder selector: greyed out if folder not set ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(lowerFeaturesAlpha),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 2. What to Backup Container
                        RivoAnimatedSection(delayMs = 60L) {
                            Column {
                                Text(
                                    text = "DATA SELECTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                                )
                                RivoExpressiveCard {
                                    RivoListItem(
                                        headline = "What to Backup",
                                        supporting = backupItemsSummary,
                                        leadingIcon = Icons.Outlined.Checklist,
                                        iconContainerColor = Color(0xFF2196F3),
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = {
                                            if (lowerFeaturesEnabled) {
                                                showItemsDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                    // 3. Backup Interval Container (Number text box + 3 buttons: Minutes, Hours, Days)
                    RivoAnimatedSection(delayMs = 90L) {
                        Column {
                            Text(
                                text = "BACKUP FREQUENCY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                            )
                            RivoExpressiveCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Backup every:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = intervalValueText,
                                            onValueChange = { input ->
                                                if (lowerFeaturesEnabled) {
                                                    val digitsOnly = input.filter { it.isDigit() }.take(4)
                                                    intervalValueText = digitsOnly
                                                    digitsOnly.toIntOrNull()?.let { num ->
                                                        if (num > 0) {
                                                            prefs.setAutoBackupIntervalValue(num)
                                                            AutoBackupManager.scheduleAutoBackup(context)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.width(88.dp),
                                            singleLine = true,
                                            enabled = lowerFeaturesEnabled,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(14.dp),
                                            label = { Text("Value", style = MaterialTheme.typography.labelSmall) }
                                        )

                                        // 3 Buttons: Minutes, Hours, Days (only one chosen)
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "minutes" to "Minutes",
                                                "hours" to "Hours",
                                                "days" to "Days"
                                            ).forEach { (unitKey, unitLabel) ->
                                                val isSelected = intervalUnit.equals(unitKey, ignoreCase = true)
                                                Surface(
                                                    onClick = {
                                                        if (lowerFeaturesEnabled) {
                                                            intervalUnit = unitKey
                                                            prefs.setAutoBackupIntervalUnit(unitKey)
                                                            AutoBackupManager.scheduleAutoBackup(context)
                                                        }
                                                    },
                                                    enabled = lowerFeaturesEnabled,
                                                    shape = CircleShape,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Text(
                                                            text = unitLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (intervalValueText.isNotBlank()) {
                                        Text(
                                            text = "A backup will automatically be created every $intervalValueText ${intervalUnit.lowercase()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Auto Delete Backups Container (Time-based and Count-based)
                    RivoAnimatedSection(delayMs = 120L) {
                        Column {
                            Text(
                                text = "AUTO DELETE BACKUPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                            )
                            RivoExpressiveCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // ── Time-based auto delete ──
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Timer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Text(
                                                "Auto Delete With Respect To Time",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = autoDeleteTimeEnabled,
                                                enabled = lowerFeaturesEnabled,
                                                onCheckedChange = { checked ->
                                                    autoDeleteTimeEnabled = checked
                                                    prefs.setAutoDeleteBackupsTimeEnabled(checked)
                                                }
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = autoDeleteTimeEnabled,
                                            enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                                            exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                                            ) {
                                                Text(
                                                    "Delete backups older than:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = autoDeleteTimeValueText,
                                                        onValueChange = { v ->
                                                            if (lowerFeaturesEnabled) {
                                                                val d = v.filter { it.isDigit() }.take(4)
                                                                autoDeleteTimeValueText = d
                                                                d.toIntOrNull()?.let { prefs.setAutoDeleteBackupsTimeValue(it) }
                                                            }
                                                        },
                                                        modifier = Modifier.width(88.dp),
                                                        singleLine = true,
                                                        enabled = lowerFeaturesEnabled,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        shape = RoundedCornerShape(14.dp),
                                                        label = { Text("Amount", style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        listOf("hours", "days").forEach { opt ->
                                                            val sel = autoDeleteTimeUnit == opt
                                                            Surface(
                                                                onClick = {
                                                                    if (lowerFeaturesEnabled) {
                                                                        autoDeleteTimeUnit = opt
                                                                        prefs.setAutoDeleteBackupsTimeUnit(opt)
                                                                    }
                                                                },
                                                                enabled = lowerFeaturesEnabled,
                                                                shape = CircleShape,
                                                                color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                                border = if (sel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                                modifier = Modifier.height(40.dp)
                                                            ) {
                                                                Box(
                                                                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                                    Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        opt.replaceFirstChar { it.uppercase() },
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                                        color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                if (autoDeleteTimeValueText.isNotBlank()) {
                                                    Text(
                                                        "Backups older than $autoDeleteTimeValueText $autoDeleteTimeUnit will be deleted automatically",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    // ── Count-based auto delete ──
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Storage,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Text(
                                                "Auto Delete With Respect To Backup Count",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = autoDeleteCountEnabled,
                                                enabled = lowerFeaturesEnabled,
                                                onCheckedChange = { checked ->
                                                    autoDeleteCountEnabled = checked
                                                    prefs.setAutoDeleteBackupsCountEnabled(checked)
                                                }
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = autoDeleteCountEnabled,
                                            enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                                            exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                                            ) {
                                                Text(
                                                    "Maximum number of backups to keep:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = autoDeleteCountValueText,
                                                        onValueChange = { v ->
                                                            if (lowerFeaturesEnabled) {
                                                                val d = v.filter { it.isDigit() }.take(3)
                                                                autoDeleteCountValueText = d
                                                                d.toIntOrNull()?.let { prefs.setAutoDeleteBackupsCountValue(it) }
                                                            }
                                                        },
                                                        modifier = Modifier.width(96.dp),
                                                        singleLine = true,
                                                        enabled = lowerFeaturesEnabled,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        shape = RoundedCornerShape(14.dp),
                                                        label = { Text("Count", style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                    Text(
                                                        "backups",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (autoDeleteCountValueText.isNotBlank()) {
                                                    Text(
                                                        "Oldest backups exceeding $autoDeleteCountValueText files will be deleted automatically",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // end inner Column for features below folder selector
                }
            }
        }
    }
    }

    // ── Popup Dialog: What to Backup ──────────────────────────────────────────
    if (showItemsDialog) {
        AlertDialog(
            onDismissRequest = { showItemsDialog = false },
            title = {
                Text("What to Backup Automatically", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val items = listOf(
                        Triple("Backup settings", "Preferences, widgets, liquid glass & blur effects", backupSettings) to { checked: Boolean ->
                            backupSettings = checked
                            prefs.setAutoBackupItemSettings(checked)
                            prefs.setAutoBackupItemLiquidGlass(checked)
                        },
                        Triple("Backup calling cards", "Cards, backgrounds, caller UI & PFP customization", backupCallingCards) to { checked: Boolean ->
                            backupCallingCards = checked
                            prefs.setAutoBackupItemCallingCards(checked)
                        },
                        Triple("Backup notes", "Contact notes and standalone notes", backupNotes) to { checked: Boolean ->
                            backupNotes = checked
                            prefs.setAutoBackupItemNotes(checked)
                        },
                        Triple("Backup contact groups", "Custom contact groups", backupGroups) to { checked: Boolean ->
                            backupGroups = checked
                            prefs.setAutoBackupItemGroups(checked)
                        },
                        Triple("Backup recordings", "Call recordings and audio files", backupRecordings) to { checked: Boolean ->
                            backupRecordings = checked
                            prefs.setAutoBackupItemRecordings(checked)
                        },
                        Triple("Backup contacts", "Device contacts and details", backupContacts) to { checked: Boolean ->
                            backupContacts = checked
                            prefs.setAutoBackupItemContacts(checked)
                        },
                        Triple("Backup call logs", "Call history and duration details", backupCallLogs) to { checked: Boolean ->
                            backupCallLogs = checked
                            prefs.setAutoBackupItemCallLogs(checked)
                        },
                        Triple("Backup SMS", "SMS messages and threads", backupSms) to { checked: Boolean ->
                            backupSms = checked
                            prefs.setAutoBackupItemSms(checked)
                        }
                    )

                    items.forEach { (item, onToggle) ->
                        val (title, subtitle, checked) = item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggle(!checked) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = checked,
                                onCheckedChange = onToggle
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showItemsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
