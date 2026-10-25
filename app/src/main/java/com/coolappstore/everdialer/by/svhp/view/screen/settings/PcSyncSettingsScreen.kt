package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager
import com.coolappstore.everdialer.by.svhp.controller.util.PcSyncStatus
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun PcSyncSettingsScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val syncManager = koinInject<PcBluetoothSyncManager>()
    val scrollState = rememberScrollState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var bluetoothSyncEnabled by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false))
    }

    var hasPermission by remember {
        mutableStateOf(syncManager.hasBluetoothPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        hasPermission = granted
        if (granted && bluetoothSyncEnabled) {
            syncManager.start()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }

    val syncStatus by syncManager.status.collectAsState()
    val connectedDevice by syncManager.connectedDeviceName.collectAsState()
    val lastSyncTime by syncManager.lastSyncTimestamp.collectAsState()

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Settings",
                onBackClick = { navigator.navigateUp() }
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
            // Permission Banner if missing
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Nearby Devices Permission Needed",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "Ever Dialer requires Bluetooth connect permission to link and sync data with Ever Dialer PC.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    SectionLabel("Connection")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Bluetooth PC Sync",
                            supporting = "Synchronize data and enable the desktop landscape view in Ever Dialer PC",
                            leadingIcon = Icons.Outlined.BluetoothSearching,
                            iconContainerColor = Color(0xFF1E88E5),
                            checked = bluetoothSyncEnabled,
                            modifier = Modifier.settingsSearchHighlight("pc_bluetooth_sync", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                bluetoothSyncEnabled = isChecked
                                if (isChecked && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                } else {
                                    syncManager.onBluetoothSyncToggled(isChecked)
                                }
                            }
                        )
                    }

                    AnimatedVisibility(visible = bluetoothSyncEnabled) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Status Banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    val (icon, tint, label) = when (syncStatus) {
                                        PcSyncStatus.CONNECTED -> Triple(
                                            Icons.Outlined.BluetoothConnected,
                                            Color(0xFF4CAF50),
                                            "Connected to ${connectedDevice ?: "Ever Dialer PC"}"
                                        )
                                        PcSyncStatus.SYNCING -> Triple(
                                            Icons.Outlined.Sync,
                                            Color(0xFFFF9800),
                                            "Syncing data with PC..."
                                        )
                                        PcSyncStatus.CONNECTING -> Triple(
                                            Icons.Outlined.BluetoothSearching,
                                            Color(0xFF2196F3),
                                            "Connecting to Ever Dialer PC..."
                                        )
                                        PcSyncStatus.ERROR -> Triple(
                                            Icons.Outlined.ErrorOutline,
                                            Color(0xFFE53935),
                                            if (!hasPermission) "Missing Bluetooth permission" else "Bluetooth error"
                                        )
                                        else -> Triple(
                                            Icons.Outlined.BluetoothSearching,
                                            MaterialTheme.colorScheme.outline,
                                            "Waiting for Ever Dialer PC..."
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(tint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (lastSyncTime != null) {
                                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSyncTime!!))
                                            Text(
                                                text = "Last synced at $timeStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Manual Sync Button
                            FilledTonalButton(
                                onClick = {
                                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.BLUETOOTH_CONNECT,
                                                Manifest.permission.BLUETOOTH_SCAN
                                            )
                                        )
                                    } else {
                                        syncManager.triggerSyncNow()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }
        }
    }
}
