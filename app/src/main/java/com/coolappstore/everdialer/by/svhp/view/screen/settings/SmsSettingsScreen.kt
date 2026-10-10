package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BlockedSmsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

private val ColorBlue     = Color(0xFF2196F3)
private val ColorGreen    = Color(0xFF4CAF50)
private val ColorAmber    = Color(0xFFFF9800)
private val ColorPurple   = Color(0xFF9C27B0)
private val ColorIndigo   = Color(0xFF3F51B5)
private val ColorTeal     = Color(0xFF009688)
private val ColorRed      = Color(0xFFE53935)
private val ColorCyan     = Color(0xFF00BCD4)
private val ColorBluGrey  = Color(0xFF607D8B)

private fun updateSmsLauncherAlias(context: android.content.Context, enabled: Boolean, appName: String) {
    val pm = context.packageManager
    val aliasMap = mapOf(
        "Ever SMS" to "${context.packageName}.SmsLauncherAliasEverSms",
        "Ever Messages" to "${context.packageName}.SmsLauncherAliasEverMessages",
        "SMS" to "${context.packageName}.SmsLauncherAliasSms",
        "Messages" to "${context.packageName}.SmsLauncherAliasMessages",
        "Chats" to "${context.packageName}.SmsLauncherAliasChats"
    )
    val oldAlias = "${context.packageName}.SmsLauncherAlias"
    val targetAlias = aliasMap[appName] ?: aliasMap["Ever SMS"]!!

    try {
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, oldAlias),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (_: Exception) {}

    aliasMap.forEach { (_, componentNameStr) ->
        try {
            val shouldEnable = enabled && (componentNameStr == targetAlias)
            val newState = if (shouldEnable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val current = pm.getComponentEnabledSetting(ComponentName(context.packageName, componentNameStr))
            if (current != newState) {
                pm.setComponentEnabledSetting(
                    ComponentName(context.packageName, componentNameStr),
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SmsSettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var isDefaultSms by remember { mutableStateOf(DefaultSmsManager.isDefaultSms(context)) }
    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
        smsVM.refreshConversations()
    }

    // Settings States
    val textSize = remember(settingsVer) {
        prefs.getFloat(PreferenceManager.KEY_SMS_CHAT_TEXT_SIZE, PreferenceManager.DEFAULT_SMS_CHAT_TEXT_SIZE)
    }
    val unreadAtTop = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_UNREAD_AT_TOP, true)
    }
    val sendDelaySeconds = remember(settingsVer) {
        prefs.getInt(PreferenceManager.KEY_SMS_SEND_DELAY_SECONDS, 0)
    }
    val deliveryReports = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_DELIVERY_REPORTS, false)
    }
    val signatureEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SIGNATURE_ENABLED, false)
    }
    val signatureText = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SIGNATURE, "") ?: ""
    }
    val stripUnicode = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_STRIP_UNICODE, false)
    }
    val mobileOnly = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_MOBILE_ONLY, false)
    }
    val longAsMms = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_LONG_AS_MMS, false)
    }
    val maxMmsSize = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_MAX_MMS_SIZE, "300KB") ?: "300KB"
    }
    val disableScreenshots = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_DISABLE_SCREENSHOTS, false)
    }
    val autoColorAvatars = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, true)
    }
    val showAvatar = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SHOW_AVATAR, true)
    }
    val showStt = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SHOW_STT, true)
    }
    val swipeRightAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, "none") ?: "none"
    }
    val swipeLeftAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, "none") ?: "none"
    }
    val floatingBubble = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_FLOATING_BUBBLE, false)
    }
    val smsLauncherIconEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_LAUNCHER_ICON_ENABLED, true)
    }
    val smsAppName = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_LAUNCHER_APP_NAME, "Ever SMS") ?: "Ever SMS"
    }
    var blockedCount by remember { mutableStateOf(BlockedNumbersManager.getBlockedList(prefs).size) }
    LaunchedEffect(settingsVer) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = BlockedNumbersManager.getBlockedList(context, prefs)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                blockedCount = list.size
            }
        }
    }

    // Dialog state controllers
    var showAppNameDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showSendDelayDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showMmsSizeDialog by remember { mutableStateOf(false) }
    var showSwipeRightDialog by remember { mutableStateOf(false) }
    var showSwipeLeftDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "SMS & Messages Settings",
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default App Status Card (only shown when Ever Dialer is not default)
            if (!isDefaultSms) {
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        RivoExpressiveCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Not Default SMS App",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Tap to set as default to send and receive messages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { DefaultSmsManager.requestDefaultSms(defaultSmsLauncher, context) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Set Default")
                                }
                            }
                        }
                    }
                }
            }

            // Appearance Section
            item {
                RivoAnimatedSection(delayMs = 40L) {
                    Column {
                        RivoSectionHeader("Appearance")
                        RivoExpressiveCard {
                            Column {
                                // Text Size
                                RivoListItem(
                                    headline = "Message text size",
                                    supporting = when (textSize) {
                                        14f -> "Small (14sp)"
                                        16f -> "Normal (16sp)"
                                        18f -> "Large (18sp)"
                                        22f -> "Extra Large (22sp)"
                                        else -> "${textSize.toInt()}sp"
                                    },
                                    leadingIcon = Icons.Outlined.FormatSize,
                                    iconContainerColor = ColorPurple,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showTextSizeDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Auto Color Avatars
                                RivoSwitchListItem(
                                    headline = "Automatic avatar colors",
                                    supporting = "Generate distinct colors for contacts",
                                    leadingIcon = Icons.Outlined.Palette,
                                    iconContainerColor = ColorGreen,
                                    checked = autoColorAvatars,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Show Avatar
                                RivoSwitchListItem(
                                    headline = "Show avatar",
                                    supporting = "Show contact avatars in message logs",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = ColorPurple,
                                    checked = showAvatar,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_SHOW_AVATAR, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Speech to text button
                                RivoSwitchListItem(
                                    headline = "Show Speech-to-Text button",
                                    supporting = "Display microphone button in message composer",
                                    leadingIcon = Icons.Outlined.Mic,
                                    iconContainerColor = ColorAmber,
                                    checked = showStt,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_SHOW_STT, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Prevent Screenshots
                                RivoSwitchListItem(
                                    headline = "Prevent screenshots",
                                    supporting = "Block screen captures and screen recording in SMS",
                                    leadingIcon = Icons.Outlined.Security,
                                    iconContainerColor = ColorRed,
                                    checked = disableScreenshots,
                                    onCheckedChange = { checked ->
                                        prefs.setBoolean(PreferenceManager.KEY_SMS_DISABLE_SCREENSHOTS, checked)
                                        (context as? Activity)?.window?.let { window ->
                                            if (checked) {
                                                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                            } else {
                                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Floating SMS
                                RivoSwitchListItem(
                                    headline = "Floating SMS",
                                    supporting = "Float a bubble on incoming messages to chat without leaving your current app. You can also float any chat from its options menu.",
                                    leadingIcon = Icons.Outlined.SpeakerNotes,
                                    iconContainerColor = ColorBlue,
                                    checked = floatingBubble,
                                    onCheckedChange = { checked ->
                                        if (checked && !Settings.canDrawOverlays(context)) {
                                            try {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                        prefs.setBoolean(PreferenceManager.KEY_SMS_FLOATING_BUBBLE, checked)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Direct App Drawer Icon for SMS
                                RivoSwitchListItem(
                                    headline = "SMS app icon in App Drawer",
                                    supporting = "Show a dedicated icon in your app drawer to launch SMS directly",
                                    leadingIcon = Icons.Outlined.Apps,
                                    iconContainerColor = ColorIndigo,
                                    checked = smsLauncherIconEnabled,
                                    onCheckedChange = { enabled ->
                                        prefs.setBoolean(PreferenceManager.KEY_SMS_LAUNCHER_ICON_ENABLED, enabled)
                                        updateSmsLauncherAlias(context, enabled, smsAppName)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // SMS App Name
                                RivoListItem(
                                    headline = "App name",
                                    supporting = smsAppName,
                                    leadingIcon = Icons.Outlined.Badge,
                                    iconContainerColor = if (smsLauncherIconEnabled) ColorTeal else MaterialTheme.colorScheme.surfaceVariant,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = if (!smsLauncherIconEnabled) Modifier.alpha(0.4f) else Modifier,
                                    onClick = {
                                        if (smsLauncherIconEnabled) {
                                            showAppNameDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // General & Sending Section
            item {
                RivoAnimatedSection(delayMs = 80L) {
                    Column {
                        RivoSectionHeader("Sending & Receiving")
                        RivoExpressiveCard {
                            Column {
                                // Delayed sending
                                RivoListItem(
                                    headline = "Delayed sending",
                                    supporting = if (sendDelaySeconds == 0) "No delay" else "$sendDelaySeconds seconds countdown",
                                    leadingIcon = Icons.Outlined.Timer,
                                    iconContainerColor = ColorBlue,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showSendDelayDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Delivery Reports
                                RivoSwitchListItem(
                                    headline = "Delivery reports",
                                    supporting = "Request SMS delivery confirmation from carrier",
                                    leadingIcon = Icons.Outlined.DoneAll,
                                    iconContainerColor = ColorTeal,
                                    checked = deliveryReports,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_DELIVERY_REPORTS, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Unread at top
                                RivoSwitchListItem(
                                    headline = "Unread conversations at top",
                                    supporting = "Pin unread chats above read conversations",
                                    leadingIcon = Icons.Outlined.MarkChatUnread,
                                    iconContainerColor = ColorIndigo,
                                    checked = unreadAtTop,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_UNREAD_AT_TOP, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Signature
                                RivoListItem(
                                    headline = "Signature",
                                    supporting = if (signatureEnabled && signatureText.isNotBlank()) signatureText else if (signatureEnabled) "Enabled (empty)" else "Disabled",
                                    leadingIcon = Icons.Outlined.Draw,
                                    iconContainerColor = ColorAmber,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showSignatureDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Strip unicode
                                RivoSwitchListItem(
                                    headline = "Strip unicode characters",
                                    supporting = "Convert accented characters to plain ASCII for GSM 7-bit compatibility",
                                    leadingIcon = Icons.Outlined.Translate,
                                    iconContainerColor = ColorCyan,
                                    checked = stripUnicode,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_STRIP_UNICODE, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Long as MMS
                                RivoSwitchListItem(
                                    headline = "Send long messages as MMS",
                                    supporting = "Convert messages longer than 3 SMS segments to MMS",
                                    leadingIcon = Icons.Outlined.Mms,
                                    iconContainerColor = ColorPurple,
                                    checked = longAsMms,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_LONG_AS_MMS, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Max MMS size
                                RivoListItem(
                                    headline = "Maximum MMS size",
                                    supporting = maxMmsSize,
                                    leadingIcon = Icons.Outlined.Attachment,
                                    iconContainerColor = ColorBluGrey,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showMmsSizeDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Mobile numbers only
                                RivoSwitchListItem(
                                    headline = "Mobile numbers only",
                                    supporting = "Filter out landlines when composing a new message",
                                    leadingIcon = Icons.Outlined.PhoneAndroid,
                                    iconContainerColor = ColorGreen,
                                    checked = mobileOnly,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_MOBILE_ONLY, it) }
                                )
                            }
                        }
                    }
                }
            }

            // Gestures & Swipe Actions Section
            item {
                RivoAnimatedSection(delayMs = 120L) {
                    Column {
                        RivoSectionHeader("Swipe Actions")
                        RivoExpressiveCard {
                            Column {
                                RivoListItem(
                                    headline = "Swipe right action",
                                    supporting = when (swipeRightAction) {
                                        "call" -> "Direct Call"
                                        "read" -> "Mark as Read / Unread"
                                        "delete" -> "Delete Conversation"
                                        else -> "None"
                                    },
                                    leadingIcon = Icons.Outlined.SwipeRight,
                                    iconContainerColor = ColorGreen,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showSwipeRightDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                RivoListItem(
                                    headline = "Swipe left action",
                                    supporting = when (swipeLeftAction) {
                                        "delete" -> "Delete Conversation"
                                        "read" -> "Mark as Read / Unread"
                                        else -> "None"
                                    },
                                    leadingIcon = Icons.Outlined.SwipeLeft,
                                    iconContainerColor = ColorRed,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showSwipeLeftDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            // Blocked SMS Section
            item {
                RivoAnimatedSection(delayMs = 140L) {
                    Column {
                        RivoSectionHeader("Blocked SMS")
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "Blocked senders & numbers",
                                supporting = if (blockedCount == 0) "No blocked SMS senders"
                                             else "$blockedCount blocked sender${if (blockedCount > 1) "s" else ""}",
                                leadingIcon = Icons.Outlined.Block,
                                iconContainerColor = ColorRed,
                                trailingIcon = Icons.Default.ChevronRight,
                                onClick = {
                                    navigator.navigate(BlockedSmsScreenDestination())
                                }
                            )
                        }
                    }
                }
            }

            // Sync Section
            item {
                RivoAnimatedSection(delayMs = 160L) {
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Sync messages & threads",
                            supporting = "Force reload all SMS and MMS conversations from system telephony provider",
                            leadingIcon = Icons.Outlined.Sync,
                            iconContainerColor = ColorBlue,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = {
                                smsVM.refreshConversations()
                                navigator.navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showTextSizeDialog) {
        val options = listOf(14f to "Small (14sp)", 16f to "Normal (16sp)", 18f to "Large (18sp)", 22f to "Extra Large (22sp)")
        AlertDialog(
            onDismissRequest = { showTextSizeDialog = false },
            title = { Text("Message Text Size") },
            text = {
                Column {
                    options.forEach { (size, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setFloat(PreferenceManager.KEY_SMS_CHAT_TEXT_SIZE, size)
                                    showTextSizeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = textSize == size,
                                onClick = {
                                    prefs.setFloat(PreferenceManager.KEY_SMS_CHAT_TEXT_SIZE, size)
                                    showTextSizeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTextSizeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSendDelayDialog) {
        val options = listOf(0 to "No delay", 3 to "3 seconds", 5 to "5 seconds", 10 to "10 seconds")
        AlertDialog(
            onDismissRequest = { showSendDelayDialog = false },
            title = { Text("Delayed Sending") },
            text = {
                Column {
                    options.forEach { (sec, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setInt(PreferenceManager.KEY_SMS_SEND_DELAY_SECONDS, sec)
                                    showSendDelayDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sendDelaySeconds == sec,
                                onClick = {
                                    prefs.setInt(PreferenceManager.KEY_SMS_SEND_DELAY_SECONDS, sec)
                                    showSendDelayDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSendDelayDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSignatureDialog) {
        var tempSignature by remember { mutableStateOf(signatureText) }
        var tempEnabled by remember { mutableStateOf(signatureEnabled) }

        AlertDialog(
            onDismissRequest = { showSignatureDialog = false },
            title = { Text("Message Signature") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable signature", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = tempEnabled, onCheckedChange = { tempEnabled = it })
                    }
                    if (tempEnabled) {
                        OutlinedTextField(
                            value = tempSignature,
                            onValueChange = { tempSignature = it },
                            placeholder = { Text("e.g. Sent via Ever Dialer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.setBoolean(PreferenceManager.KEY_SMS_SIGNATURE_ENABLED, tempEnabled)
                    prefs.setString(PreferenceManager.KEY_SMS_SIGNATURE, tempSignature)
                    showSignatureDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSignatureDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMmsSizeDialog) {
        val options = listOf("100KB", "200KB", "300KB", "600KB", "1MB", "2MB")
        AlertDialog(
            onDismissRequest = { showMmsSizeDialog = false },
            title = { Text("Maximum MMS Size") },
            text = {
                Column {
                    options.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setString(PreferenceManager.KEY_SMS_MAX_MMS_SIZE, opt)
                                    showMmsSizeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = maxMmsSize == opt,
                                onClick = {
                                    prefs.setString(PreferenceManager.KEY_SMS_MAX_MMS_SIZE, opt)
                                    showMmsSizeDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(opt, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMmsSizeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSwipeRightDialog) {
        val options = listOf("call" to "Direct Call", "read" to "Mark as Read / Unread", "delete" to "Delete Conversation", "none" to "None")
        AlertDialog(
            onDismissRequest = { showSwipeRightDialog = false },
            title = { Text("Swipe Right Action") },
            text = {
                Column {
                    options.forEach { (action, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, action)
                                    showSwipeRightDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = swipeRightAction == action,
                                onClick = {
                                    prefs.setString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, action)
                                    showSwipeRightDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwipeRightDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSwipeLeftDialog) {
        val options = listOf("delete" to "Delete Conversation", "read" to "Mark as Read / Unread", "none" to "None")
        AlertDialog(
            onDismissRequest = { showSwipeLeftDialog = false },
            title = { Text("Swipe Left Action") },
            text = {
                Column {
                    options.forEach { (action, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, action)
                                    showSwipeLeftDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = swipeLeftAction == action,
                                onClick = {
                                    prefs.setString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, action)
                                    showSwipeLeftDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwipeLeftDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppNameDialog) {
        val appNameOptions = listOf("Ever SMS", "Ever Messages", "SMS", "Messages", "Chats")
        AlertDialog(
            onDismissRequest = { showAppNameDialog = false },
            title = { Text("App Name") },
            text = {
                Column {
                    appNameOptions.forEach { nameOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setString(PreferenceManager.KEY_SMS_LAUNCHER_APP_NAME, nameOption)
                                    updateSmsLauncherAlias(context, smsLauncherIconEnabled, nameOption)
                                    showAppNameDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = smsAppName == nameOption,
                                onClick = {
                                    prefs.setString(PreferenceManager.KEY_SMS_LAUNCHER_APP_NAME, nameOption)
                                    updateSmsLauncherAlias(context, smsLauncherIconEnabled, nameOption)
                                    showAppNameDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(nameOption, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppNameDialog = false }) { Text("Cancel") }
            }
        )
    }
}
