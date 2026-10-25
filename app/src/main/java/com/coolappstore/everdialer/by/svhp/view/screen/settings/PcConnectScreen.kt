package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.LocalCardCornerRadius
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun PcConnectScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val syncManager = koinInject<com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager>()
    val scrollState = rememberScrollState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    val settingsVer by prefs.settingsChanged.collectAsState()
    val bluetoothSyncEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false)
    }

    var syncFavorites by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_FAVOURITES, true))
    }
    var syncCallLogs by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CALL_LOGS, true))
    }
    var syncContacts by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CONTACTS, true))
    }
    var syncSms by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_SMS, true))
    }
    var syncNotes by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_NOTES, true))
    }
    var syncContactPfp by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CONTACT_PFP, true))
    }
    var syncDynamicColors by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_DYNAMIC_COLORS, true))
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "PC Connect",
                onBackClick = { navigator.navigateUp() },
                endActions = {
                    IconButton(
                        onClick = {
                            com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.recordNormalized(0.92f, 0.05f)
                            navigator.navigate(
                                com.ramcosta.composedestinations.generated.destinations.PcSyncSettingsScreenDestination()
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "PC Sync Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!bluetoothSyncEnabled) {
                RivoAnimatedSection(delayMs = 0L) {
                    Surface(
                        onClick = {
                            navigator.navigate(
                                com.ramcosta.composedestinations.generated.destinations.PcSyncSettingsScreenDestination()
                            )
                        },
                        shape = RoundedCornerShape(LocalCardCornerRadius.current),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsSearchHighlight("pc_bluetooth_sync", highlightedKey) { highlightedKey = null }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Bluetooth PC Sync is disabled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Turn on Bluetooth PC Sync from the PC Connect settings page to enable sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Go to Settings",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            RivoAnimatedSection(delayMs = if (!bluetoothSyncEnabled) 50L else 0L) {
                Column {
                    SectionLabel("To sync")
                    RivoExpressiveCard {
                        // 1. Favourites
                        RivoSwitchListItem(
                            headline = "Favourites",
                            supporting = "Sync starred & favorite contacts",
                            leadingIcon = Icons.Default.Favorite,
                            iconContainerColor = Color(0xFFE91E63),
                            checked = syncFavorites,
                            enabled = bluetoothSyncEnabled,
                            modifier = Modifier.settingsSearchHighlight("pc_sync_favourites", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                syncFavorites = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_FAVOURITES, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 2. Call Logs
                        RivoSwitchListItem(
                            headline = "Call Logs",
                            supporting = "Sync incoming, outgoing, and missed call history",
                            leadingIcon = Icons.Default.History,
                            iconContainerColor = Color(0xFF4CAF50),
                            checked = syncCallLogs,
                            enabled = bluetoothSyncEnabled,
                            modifier = Modifier.settingsSearchHighlight("pc_sync_call_logs", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                syncCallLogs = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_CALL_LOGS, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 3. Contacts
                        RivoSwitchListItem(
                            headline = "Contacts",
                            supporting = "Sync full contact list and details",
                            leadingIcon = Icons.Default.Person,
                            iconContainerColor = Color(0xFF2196F3),
                            checked = syncContacts,
                            enabled = bluetoothSyncEnabled,
                            modifier = Modifier.settingsSearchHighlight("pc_sync_contacts", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                syncContacts = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_CONTACTS, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 4. SMS
                        RivoSwitchListItem(
                            headline = "SMS",
                            supporting = "Sync text message conversations and chats",
                            leadingIcon = Icons.Default.Chat,
                            iconContainerColor = Color(0xFF9C27B0),
                            checked = syncSms,
                            enabled = bluetoothSyncEnabled,
                            modifier = Modifier.settingsSearchHighlight("pc_sync_sms", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                syncSms = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_SMS, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                        CardDivider()

                        // 5. Notes
                        RivoSwitchListItem(
                            headline = "Notes",
                            supporting = "Sync call notes and custom notes",
                            leadingIcon = Icons.Default.Note,
                            iconContainerColor = Color(0xFFFF9800),
                            checked = syncNotes,
                            enabled = bluetoothSyncEnabled,
                            modifier = Modifier.settingsSearchHighlight("pc_sync_notes", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                syncNotes = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_NOTES, isChecked)
                                syncManager.triggerSyncNow()
                            }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = if (!bluetoothSyncEnabled) 100L else 50L) {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "Contact PFP",
                        supporting = "Sync contact profile pictures to PC",
                        leadingIcon = Icons.Default.AccountCircle,
                        iconContainerColor = Color(0xFF009688),
                        checked = syncContactPfp,
                        enabled = bluetoothSyncEnabled,
                        modifier = Modifier.settingsSearchHighlight("pc_sync_contact_pfp", highlightedKey) { highlightedKey = null },
                        onCheckedChange = { isChecked ->
                            syncContactPfp = isChecked
                            prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_CONTACT_PFP, isChecked)
                            syncManager.triggerSyncNow()
                        }
                    )
                    CardDivider()
                    RivoSwitchListItem(
                        headline = "Dynamic colors",
                        supporting = "Sync phone's theme color to desktop version",
                        leadingIcon = Icons.Default.Palette,
                        iconContainerColor = Color(0xFF6750A4),
                        checked = syncDynamicColors,
                        enabled = bluetoothSyncEnabled,
                        modifier = Modifier.settingsSearchHighlight("pc_sync_dynamic_colors", highlightedKey) { highlightedKey = null },
                        onCheckedChange = { isChecked ->
                            syncDynamicColors = isChecked
                            prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_DYNAMIC_COLORS, isChecked)
                            syncManager.triggerSyncNow()
                        }
                    )
                }
            }
        }
    }
}
