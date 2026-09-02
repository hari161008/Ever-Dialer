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
        val settingsListState = androidx.compose.foundation.lazy.rememberLazyListState()
        com.coolappstore.evercallrecorder.by.svhp.ui.common.ScrollHapticsEffect(listState = settingsListState)
        LazyColumn(
            state = settingsListState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    // ORDER: Call Recording Switch → Notifications → Recording → Audio → Security → Languages → About → Debug
                    CallRecordingMasterSwitchSection(preferences, updateTrigger, actions)
                    AppearanceSection(preferences, updateTrigger, actions)
                    RecordingMenuAppearanceSection(preferences, updateTrigger, actions)
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

// ── Universal call recording master switch ────────────────────────────────────

@Composable
private fun CallRecordingMasterSwitchSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val enabled = remember(updateTrigger) { preferences.isCallRecordingEnabled() }
    SettingsSection(title = "Call Recording", icon = Icons.Outlined.FiberManualRecord) {
        ToggleListItem(
            label = "Enable Call Recording",
            description = if (enabled)
                "Call recording is active. Turn off to fully stop all call monitoring."
            else
                "Off — nothing about your calls is monitored or recorded.",
            checked = enabled,
            onCheckedChange = { actions.setCallRecordingEnabled(it) }
        )
    }
}

// ── Appearance section (was Visual) ──────────────────────────────────────────

@Composable
private fun AppearanceSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val isRecordingNotificationsEnabled = remember(updateTrigger) { preferences.isRecordingNotificationsEnabled() }
    val isPostRecordingNotificationEnabled = remember(updateTrigger) { preferences.isPostRecordingFileActionsNotificationEnabled() }
    val isShowToastsEnabled  = remember(updateTrigger) { preferences.isShowToastsEnabled() }
    val isVibrationEnabled   = remember(updateTrigger) { preferences.isVibrationEnabled() }

    // Theme mode, dynamic color, and accent color are no longer configurable here — this
    // module always follows whatever Ever Dialer's own Settings → Interface has set (see
    // AppPreferences.getThemeMode()/isDynamicColorEnabled()), so there's nothing to duplicate.
    SettingsSection(title = stringResource(R.string.settings_section_appearance), icon = Icons.Outlined.Notifications) {
        ToggleListItem(
            label = stringResource(R.string.settings_recording_notifications),
            checked = isRecordingNotificationsEnabled,
            onCheckedChange = { actions.setRecordingNotificationsEnabled(it) }
        )
        ToggleListItem(
            label = stringResource(R.string.settings_post_recording_notification),
            checked = isPostRecordingNotificationEnabled,
            onCheckedChange = { actions.setPostRecordingFileActionsNotificationEnabled(it) },
            description = stringResource(R.string.settings_post_recording_notification_desc)
        )
        ToggleListItem(label = stringResource(R.string.settings_show_toasts), checked = isShowToastsEnabled, onCheckedChange = { actions.setShowToastsEnabled(it) })
        ToggleListItem(label = stringResource(R.string.settings_vibration_enabled), checked = isVibrationEnabled, onCheckedChange = { actions.setVibrationEnabled(it) })
    }
}

// ── Appearance (Call Recording menu placement in Ever Dialer's Settings) ──────────

@Composable
private fun RecordingMenuAppearanceSection(preferences: AppPreferences, updateTrigger: Int, actions: SettingsActions) {
    val showBelowUpdates = remember(updateTrigger) { preferences.isShowRecordingMenuBelowUpdatesEnabled() }
    SettingsSection(title = "Appearance", icon = Icons.Outlined.Palette) {
        ToggleListItem(
            label = "Show Recording Menu Below Updates",
            description = "Normally the Call Recording menu sits below Fake Calls in Ever " +
                "Dialer's Settings. Turn this on to move it up near the top, right below " +
                "Updates, under its own \"Call Recording\" heading.",
            checked = showBelowUpdates,
            onCheckedChange = { actions.setShowRecordingMenuBelowUpdatesEnabled(it) }
        )
    }
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
    val callDetectionMode = remember(updateTrigger) { preferences.getCallDetectionMode() }
    var hasManageOngoingCallsPermission by remember(updateTrigger) { mutableStateOf(actions.hasManageOngoingCallsPermission()) }
    var isGrantingPermission by remember { mutableStateOf(false) }
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
        SectionListItem(icon = storageIcon, headline = stringResource(R.string.settings_recording_folder_label), supporting = storageSupportingText, supportingColor = MaterialTheme.colorScheme.primary, onClick = onStorageClick)
        SectionListItem(icon = Icons.Outlined.DriveFileRenameOutline, headline = stringResource(R.string.settings_file_name_template), supporting = fileNameFormat, supportingColor = MaterialTheme.colorScheme.primary, onClick = { showFileNameFormatDialog = true })
    }

    // Call detection method
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.SettingsPhone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
            }
            Text(text = stringResource(R.string.settings_call_detection_method), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val detectionOptions = listOf(
                    OptionItem(AppPreferences.CallDetectionMode.PHONE_STATE.key, stringResource(R.string.settings_call_detection_phone_state), description = stringResource(R.string.settings_call_detection_phone_state_desc)),
                    OptionItem(AppPreferences.CallDetectionMode.IN_CALL_SERVICE.key, stringResource(R.string.settings_call_detection_in_call_service), description = stringResource(R.string.settings_call_detection_in_call_service_desc))
                )
                M3DropdownField(
                    label = stringResource(R.string.settings_call_detection_method),
                    selected = detectionOptions.find { it.key == callDetectionMode.key } ?: detectionOptions.first(),
                    options = detectionOptions,
                    onOptionSelected = { actions.setCallDetectionMode(AppPreferences.CallDetectionMode.fromKey(it.key)) }
                )
                AnimatedVisibility(visible = callDetectionMode == AppPreferences.CallDetectionMode.IN_CALL_SERVICE, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable(enabled = !hasManageOngoingCallsPermission && !isGrantingPermission) {
                                isGrantingPermission = true
                                actions.grantInCallServicePermission { granted ->
                                    hasManageOngoingCallsPermission = granted
                                    isGrantingPermission = false
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (hasManageOngoingCallsPermission) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (hasManageOngoingCallsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when {
                                hasManageOngoingCallsPermission -> stringResource(R.string.settings_call_detection_permission_granted)
                                isGrantingPermission -> stringResource(R.string.settings_call_detection_grant_permission)
                                else -> stringResource(R.string.settings_call_detection_permission_missing)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasManageOngoingCallsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
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
            override fun setCallRecordingEnabled(enabled: Boolean) {}
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
            override fun setShowRecordingMenuBelowUpdatesEnabled(enabled: Boolean) {}
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
            override fun setCallDetectionMode(mode: AppPreferences.CallDetectionMode) {}
            override fun setPostRecordingFileActionsNotificationEnabled(enabled: Boolean) {}
            override fun hasManageOngoingCallsPermission(): Boolean = false
            override fun grantInCallServicePermission(onResult: (Boolean) -> Unit) { onResult(false) }
        }
        SettingsContent(preferences = dummyPreferences, updateTrigger = 0, actions = dummyActions, contactPickerState = null, onStorageClick = {}, onOpenContactsIncoming = {}, onOpenContactsOutgoing = {}, onConfirmContacts = {}, onDismissContacts = {}, onExportLogs = {}, onBack = {})
    }
}
