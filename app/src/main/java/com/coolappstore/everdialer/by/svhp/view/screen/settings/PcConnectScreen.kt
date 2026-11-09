package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager
import com.coolappstore.everdialer.by.svhp.controller.util.PcSyncStatus
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.LocalCardCornerRadius
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun PcConnectScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val syncManager = koinInject<PcBluetoothSyncManager>()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    val settingsVer by prefs.settingsChanged.collectAsState()
    val bluetoothSyncEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false)
    }

    val syncStatus by syncManager.status.collectAsState()
    val connectedDevice by syncManager.connectedDeviceName.collectAsState()
    val lastSyncTime by syncManager.lastSyncTimestamp.collectAsState()

    var hasPermission by remember {
        mutableStateOf(syncManager.hasBluetoothPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        hasPermission = granted
        if (granted) {
            if (!bluetoothSyncEnabled) {
                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, true)
                syncManager.onBluetoothSyncToggled(true)
            }
            syncManager.triggerSyncNow()
        }
    }

    fun triggerHaptic() {
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            performAppHaptic(
                context,
                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
            )
        }
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
            // ── Unified Connected Hero Card & Sync Action Area (Above "To sync") ──
            RivoAnimatedSection(delayMs = 0L) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingsSearchHighlight("pc_bluetooth_sync", highlightedKey) { highlightedKey = null }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── Top Hero Banner (Dual Pane) ──
                        ExpressivePcConnectionHeroCard(
                            syncStatus = syncStatus,
                            connectedDevice = connectedDevice,
                            bluetoothSyncEnabled = bluetoothSyncEnabled,
                            onCardClick = {
                                if (!bluetoothSyncEnabled) {
                                    navigator.navigate(
                                        com.ramcosta.composedestinations.generated.destinations.PcSyncSettingsScreenDestination()
                                    )
                                }
                            }
                        )

                        // ── Device & Last Sync Stat Cards ──
                        PcConnectionStatsRow(
                            syncStatus = syncStatus,
                            connectedDevice = connectedDevice,
                            lastSyncTime = lastSyncTime,
                            bluetoothSyncEnabled = bluetoothSyncEnabled
                        )

                        // ── Modern Tactile Action Button ──
                        ExpressivePcSyncActionArea(
                            syncStatus = syncStatus,
                            bluetoothSyncEnabled = bluetoothSyncEnabled,
                            onSyncClick = {
                                triggerHaptic()
                                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                } else if (!bluetoothSyncEnabled) {
                                    navigator.navigate(
                                        com.ramcosta.composedestinations.generated.destinations.PcSyncSettingsScreenDestination()
                                    )
                                } else {
                                    syncManager.triggerSyncNow()
                                }
                            }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 60L) {
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

// ─── Expressive Hero Status Card (Dual Pane Layout) ───────────────────────

@Composable
private fun ExpressivePcConnectionHeroCard(
    syncStatus: PcSyncStatus,
    connectedDevice: String?,
    bluetoothSyncEnabled: Boolean,
    onCardClick: () -> Unit
) {
    val containerColor = when {
        !bluetoothSyncEnabled || syncStatus == PcSyncStatus.DISABLED -> MaterialTheme.colorScheme.surfaceContainerHigh
        syncStatus == PcSyncStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
        syncStatus == PcSyncStatus.SYNCING -> MaterialTheme.colorScheme.primaryContainer
        syncStatus == PcSyncStatus.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
        syncStatus == PcSyncStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val (titleColor, subtitleColor, tagColor) = when {
        !bluetoothSyncEnabled || syncStatus == PcSyncStatus.DISABLED -> Triple(
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.primary
        )
        syncStatus == PcSyncStatus.CONNECTED || syncStatus == PcSyncStatus.SYNCING -> Triple(
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.primary
        )
        syncStatus == PcSyncStatus.CONNECTING -> Triple(
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.secondary
        )
        syncStatus == PcSyncStatus.ERROR -> Triple(
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.error
        )
        else -> Triple(
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.primary
        )
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(400),
        label = "heroPcContainerColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!bluetoothSyncEnabled) Modifier.clickable { onCardClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        color = animatedContainerColor,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Left Pane: Hero Status Icon & Badge ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroPcStatusIcon(syncStatus = syncStatus, bluetoothSyncEnabled = bluetoothSyncEnabled)

                AnimatedContent(
                    targetState = Pair(syncStatus, bluetoothSyncEnabled),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "pcStatusBadge"
                ) { (status, enabled) ->
                    when {
                        !enabled || status == PcSyncStatus.DISABLED -> ExpressiveStatusChip(
                            text = "Disabled",
                            containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        status == PcSyncStatus.SYNCING -> ExpressiveStatusChip(
                            text = "Syncing…",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            pulsing = true
                        )
                        status == PcSyncStatus.CONNECTING -> ExpressiveStatusChip(
                            text = "Connecting…",
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            pulsing = true
                        )
                        status == PcSyncStatus.CONNECTED -> ExpressiveStatusChip(
                            text = "Connected",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        status == PcSyncStatus.ERROR -> ExpressiveStatusChip(
                            text = "Error",
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                        else -> ExpressiveStatusChip(
                            text = "Disconnected",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Right Pane: App Name, Headline & Description ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "EVER DIALER PC",
                    style = MaterialTheme.typography.labelSmall,
                    color = tagColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                AnimatedContent(
                    targetState = Triple(syncStatus, bluetoothSyncEnabled, connectedDevice),
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                    },
                    label = "heroPcTitle"
                ) { (status, enabled, device) ->
                    val (title, subtitle) = when {
                        !enabled || status == PcSyncStatus.DISABLED ->
                            "PC Sync Disabled" to "Tap to enable in PC Sync Settings"
                        status == PcSyncStatus.SYNCING ->
                            "Syncing Data…" to (if (!device.isNullOrBlank()) "Syncing with $device" else "Transmitting data to PC")
                        status == PcSyncStatus.CONNECTING ->
                            "Connecting to PC…" to (if (!device.isNullOrBlank()) "Connecting to $device" else "Establishing Bluetooth link")
                        status == PcSyncStatus.CONNECTED ->
                            "Connected to PC" to (if (!device.isNullOrBlank()) "Linked with $device" else "Ready to sync with Ever Dialer PC")
                        status == PcSyncStatus.ERROR ->
                            "Connection Failed" to "Could not connect to Ever Dialer PC"
                        else ->
                            "Ready to Connect" to "Waiting for Ever Dialer PC connection"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPcStatusIcon(syncStatus: PcSyncStatus, bluetoothSyncEnabled: Boolean) {
    val infinite = rememberInfiniteTransition(label = "heroPcIconAnim")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "heroPcSpin"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroPcPulse"
    )

    val (icon, iconBgColor, iconTintColor) = when {
        !bluetoothSyncEnabled || syncStatus == PcSyncStatus.DISABLED -> Triple(
            Icons.Outlined.BluetoothSearching,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        syncStatus == PcSyncStatus.SYNCING -> Triple(
            Icons.Outlined.Sync,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.primary
        )
        syncStatus == PcSyncStatus.CONNECTING -> Triple(
            Icons.Outlined.BluetoothSearching,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.secondary
        )
        syncStatus == PcSyncStatus.CONNECTED -> Triple(
            Icons.Outlined.BluetoothConnected,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.primary
        )
        syncStatus == PcSyncStatus.ERROR -> Triple(
            Icons.Outlined.ErrorOutline,
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.error
        )
        else -> Triple(
            Icons.Outlined.BluetoothSearching,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.primary
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = iconBgColor,
        modifier = Modifier
            .size(64.dp)
            .scale(if (syncStatus == PcSyncStatus.CONNECTED) pulse else 1f),
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = Pair(syncStatus, bluetoothSyncEnabled),
                transitionSpec = {
                    (fadeIn(tween(250)) + scaleIn(initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.6f))
                },
                label = "heroPcIconGlyph"
            ) { (status, enabled) ->
                val modifier = if (enabled && (status == PcSyncStatus.SYNCING || status == PcSyncStatus.CONNECTING)) {
                    Modifier.rotate(spin)
                } else Modifier

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    pulsing: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "statusChipPulse")
    val alphaAnim by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chipAlpha"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = Modifier.alpha(if (pulsing) alphaAnim else 1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─── PC Connection Stats Row ───────────────────────────────────────────────

@Composable
private fun PcConnectionStatsRow(
    syncStatus: PcSyncStatus,
    connectedDevice: String?,
    lastSyncTime: Long?,
    bluetoothSyncEnabled: Boolean
) {
    val isConnected = bluetoothSyncEnabled && (syncStatus == PcSyncStatus.CONNECTED || syncStatus == PcSyncStatus.SYNCING)

    val deviceText = when {
        !bluetoothSyncEnabled || syncStatus == PcSyncStatus.DISABLED -> "Disabled"
        !connectedDevice.isNullOrBlank() -> connectedDevice
        syncStatus == PcSyncStatus.CONNECTING -> "Connecting…"
        syncStatus == PcSyncStatus.ERROR -> "Error"
        else -> "None"
    }

    val lastSyncFormatted = formatSyncTime(lastSyncTime)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ExpressivePcStatCard(
            label = "Connected PC",
            value = deviceText,
            icon = Icons.Outlined.Devices,
            modifier = Modifier.weight(1f),
            iconTint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            highlighted = isConnected,
            containerBgColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                              else MaterialTheme.colorScheme.surfaceContainerHigh
        )

        ExpressivePcStatCard(
            label = "Last Synced",
            value = lastSyncFormatted,
            icon = if (syncStatus == PcSyncStatus.SYNCING) Icons.Outlined.Sync else Icons.Outlined.CheckCircle,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.primary,
            highlighted = syncStatus == PcSyncStatus.SYNCING,
            containerBgColor = if (syncStatus == PcSyncStatus.SYNCING) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                              else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun ExpressivePcStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    highlighted: Boolean = false,
    containerBgColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val textColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtextColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerBgColor,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.16f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtextColor,
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Expressive PC Sync Action Area ────────────────────────────────────────

@Composable
private fun ExpressivePcSyncActionArea(
    syncStatus: PcSyncStatus,
    bluetoothSyncEnabled: Boolean,
    onSyncClick: () -> Unit
) {
    AnimatedContent(
        targetState = Pair(syncStatus, bluetoothSyncEnabled),
        transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically { it / 3 }) togetherWith
                (fadeOut(tween(140)) + slideOutVertically { -it / 3 })
        },
        label = "pcSyncActionArea"
    ) { (status, enabled) ->
        when {
            !enabled || status == PcSyncStatus.DISABLED -> {
                Button(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Configure in PC Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            status == PcSyncStatus.SYNCING -> {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Syncing with PC…", fontWeight = FontWeight.Bold)
                }
            }

            status == PcSyncStatus.CONNECTING -> {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Connecting…", fontWeight = FontWeight.Bold)
                }
            }

            status == PcSyncStatus.ERROR -> {
                FilledTonalButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Retry Sync", fontWeight = FontWeight.Bold)
                }
            }

            else -> {
                Button(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sync Now",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatSyncTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "Never"
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
