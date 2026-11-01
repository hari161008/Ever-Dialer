package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.NotificationManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.everdialer.by.svhp.controller.PcSyncService
import com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.supernova.networkswitch.util.AutomationSwitchStore
import org.koin.compose.koinInject

private val ColorBlue   = Color(0xFF1E88E5)
private val ColorRed    = Color(0xFFE53935)
private val ColorOrange = Color(0xFFFB8C00)
private val ColorGreen  = Color(0xFF43A047)
private val ColorTeal   = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun NotificationsSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val recorderPrefs = remember { AppPreferences(context) }
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    // Missed Call notification state (default on, provider default system)
    var missedCallNotification by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, true))
    }
    var missedCallProvider by remember {
        mutableStateOf(
            prefs.getString(
                PreferenceManager.KEY_MISSED_CALL_NOTIFICATION_PROVIDER,
                PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM
            ) ?: PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM
        )
    }

    // PC Connect notification state
    var showPcNotification by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_SHOW_NOTIFICATION, true))
    }

    // Call Recording notification states
    var recordingNotifications by remember {
        mutableStateOf(recorderPrefs.isRecordingNotificationsEnabled())
    }
    var postRecordingNotification by remember {
        mutableStateOf(recorderPrefs.isPostRecordingFileActionsNotificationEnabled())
    }
    var showToasts by remember {
        mutableStateOf(recorderPrefs.isShowToastsEnabled())
    }

    // 4G/5G Switcher notification state
    val nsNotificationsFlow = remember { AutomationSwitchStore.notificationsEnabled }
    val nsNotifications by nsNotificationsFlow.collectAsState()

    LaunchedEffect(Unit) {
        AutomationSwitchStore.init(context)
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Notifications",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Missed Calls ──────────────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Missed Calls",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Missed Call Notification",
                            supporting = if (missedCallNotification) {
                                if (missedCallProvider == PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER) {
                                    "Prompted by Ever Dialer notification"
                                } else {
                                    "Prompted by Android System"
                                }
                            } else "Missed call notifications disabled",
                            leadingIcon = Icons.AutoMirrored.Filled.CallMissed,
                            iconContainerColor = ColorRed,
                            checked = missedCallNotification,
                            modifier = Modifier.settingsSearchHighlight("missed_call_notification", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                missedCallNotification = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, isChecked)
                                if (isChecked) {
                                    if (missedCallProvider == PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER) {
                                        MissedCallBadgeManager.cancelSystemMissedCallNotification(context)
                                        MissedCallBadgeManager.updateBadge(context)
                                    }
                                } else {
                                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(
                                        MissedCallBadgeManager.MISSED_CALLS_NOTIF_ID
                                    )
                                    MissedCallBadgeManager.cancelSystemMissedCallNotification(context)
                                    MissedCallBadgeManager.markMissedCallsAsRead(context)
                                }
                            }
                        )

                        AnimatedVisibility(visible = missedCallNotification) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "Notification Provider",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        FilterChip(
                                            selected = missedCallProvider == PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM,
                                            onClick = {
                                                missedCallProvider = PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM
                                                prefs.setString(
                                                    PreferenceManager.KEY_MISSED_CALL_NOTIFICATION_PROVIDER,
                                                    PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM
                                                )
                                                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(
                                                    MissedCallBadgeManager.MISSED_CALLS_NOTIF_ID
                                                )
                                                prefs.setInt("active_ever_dialer_missed_count", 0)
                                            },
                                            label = { Text("System") },
                                            leadingIcon = if (missedCallProvider == PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            shape = CircleShape
                                        )
                                        FilterChip(
                                            selected = missedCallProvider == PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER,
                                            onClick = {
                                                missedCallProvider = PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER
                                                prefs.setString(
                                                    PreferenceManager.KEY_MISSED_CALL_NOTIFICATION_PROVIDER,
                                                    PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER
                                                )
                                                MissedCallBadgeManager.cancelSystemMissedCallNotification(context)
                                                MissedCallBadgeManager.updateBadge(context)
                                            },
                                            label = { Text("Ever Dialer") },
                                            leadingIcon = if (missedCallProvider == PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            shape = CircleShape
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── PC Connect ────────────────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 40L) {
                Column {
                    Text(
                        "PC Connect",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Notification",
                            supporting = "Show connected to PC notification in notification bar",
                            leadingIcon = Icons.Outlined.Notifications,
                            iconContainerColor = ColorBlue,
                            checked = showPcNotification,
                            modifier = Modifier.settingsSearchHighlight("pc_sync_notification", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                showPcNotification = isChecked
                                prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_SHOW_NOTIFICATION, isChecked)
                                PcSyncService.updateNotificationVisibility(context, isChecked)
                            }
                        )
                    }
                }
            }

            // ── Call Recording ────────────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 40L) {
                Column {
                    Text(
                        "Call Recording",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Recording Notifications",
                            supporting = "Show notification during call recording",
                            leadingIcon = Icons.Outlined.Notifications,
                            iconContainerColor = ColorRed,
                            checked = recordingNotifications,
                            modifier = Modifier.settingsSearchHighlight("recording_notifications", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                recordingNotifications = isChecked
                                recorderPrefs.setRecordingNotificationsEnabled(isChecked)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Post-recording Notification",
                            supporting = "Show notification with options to play, share, or delete after recording finishes",
                            leadingIcon = Icons.Outlined.NotificationImportant,
                            iconContainerColor = ColorOrange,
                            checked = postRecordingNotification,
                            modifier = Modifier.settingsSearchHighlight("post_recording_notification", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                postRecordingNotification = isChecked
                                recorderPrefs.setPostRecordingFileActionsNotificationEnabled(isChecked)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Toast Notifications",
                            supporting = "Show status toasts for recording actions",
                            leadingIcon = Icons.Outlined.Info,
                            iconContainerColor = ColorGreen,
                            checked = showToasts,
                            modifier = Modifier.settingsSearchHighlight("show_recording_toasts", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                showToasts = isChecked
                                recorderPrefs.setShowToastsEnabled(isChecked)
                            }
                        )
                    }
                }
            }

            // ── 4G/5G Switcher ────────────────────────────────────────────────────
            RivoAnimatedSection(delayMs = 80L) {
                Column {
                    Text(
                        "4G/5G Switcher",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Automation Service Notification",
                            supporting = "Show notification in status bar while automation service is running",
                            leadingIcon = Icons.Outlined.SignalCellularAlt,
                            iconContainerColor = ColorTeal,
                            checked = nsNotifications,
                            modifier = Modifier.settingsSearchHighlight("automation_notifications", highlightedKey) { highlightedKey = null },
                            onCheckedChange = { isChecked ->
                                AutomationSwitchStore.setNotificationsEnabled(context, isChecked)
                                com.supernova.networkswitch.service.AutomationService.updateNotification(context)
                            }
                        )
                    }
                }
            }
        }
    }
}
