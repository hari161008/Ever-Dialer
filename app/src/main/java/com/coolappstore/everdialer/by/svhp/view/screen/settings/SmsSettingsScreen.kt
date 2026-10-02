package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

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
    val showStt = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SHOW_STT, true)
    }
    val swipeRightAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, "call") ?: "call"
    }
    val swipeLeftAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, "delete") ?: "delete"
    }

    // Dialog state controllers
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
            // Default App Status Card
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
                                color = if (isDefaultSms) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isDefaultSms) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isDefaultSms) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isDefaultSms) "Ever Dialer is Default SMS App" else "Not Default SMS App",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isDefaultSms) "Full SMS & MMS functionality enabled" else "Tap to set as default to send and receive messages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isDefaultSms) {
                                Button(
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
                        Text(
                            "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column {
                                // Text Size
                                SettingsItem(
                                    icon = Icons.Outlined.FormatSize,
                                    title = "Message text size",
                                    subtitle = when (textSize) {
                                        14f -> "Small (14sp)"
                                        16f -> "Normal (16sp)"
                                        18f -> "Large (18sp)"
                                        22f -> "Extra Large (22sp)"
                                        else -> "${textSize.toInt()}sp"
                                    },
                                    onClick = { showTextSizeDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Auto Color Avatars
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Palette,
                                    title = "Automatic avatar colors",
                                    subtitle = "Generate distinct colors for contacts",
                                    checked = autoColorAvatars,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Speech to text button
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Mic,
                                    title = "Show Speech-to-Text button",
                                    subtitle = "Display microphone button in message composer",
                                    checked = showStt,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_SHOW_STT, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Prevent Screenshots
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Security,
                                    title = "Prevent screenshots",
                                    subtitle = "Block screen captures and screen recording in SMS",
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
                            }
                        }
                    }
                }
            }

            // General & Sending Section
            item {
                RivoAnimatedSection(delayMs = 80L) {
                    Column {
                        Text(
                            "Sending & Receiving",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column {
                                // Delayed sending
                                SettingsItem(
                                    icon = Icons.Outlined.Timer,
                                    title = "Delayed sending",
                                    subtitle = if (sendDelaySeconds == 0) "No delay" else "$sendDelaySeconds seconds countdown",
                                    onClick = { showSendDelayDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Delivery Reports
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.DoneAll,
                                    title = "Delivery reports",
                                    subtitle = "Request SMS delivery confirmation from carrier",
                                    checked = deliveryReports,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_DELIVERY_REPORTS, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Unread at top
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.MarkChatUnread,
                                    title = "Unread conversations at top",
                                    subtitle = "Pin unread chats above read conversations",
                                    checked = unreadAtTop,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_UNREAD_AT_TOP, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Signature
                                SettingsItem(
                                    icon = Icons.Outlined.Draw,
                                    title = "Signature",
                                    subtitle = if (signatureEnabled && signatureText.isNotBlank()) signatureText else if (signatureEnabled) "Enabled (empty)" else "Disabled",
                                    onClick = { showSignatureDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Strip unicode
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Translate,
                                    title = "Strip unicode characters",
                                    subtitle = "Convert accented characters to plain ASCII for GSM 7-bit compatibility",
                                    checked = stripUnicode,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_STRIP_UNICODE, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Long as MMS
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.Mms,
                                    title = "Send long messages as MMS",
                                    subtitle = "Convert messages longer than 3 SMS segments to MMS",
                                    checked = longAsMms,
                                    onCheckedChange = { prefs.setBoolean(PreferenceManager.KEY_SMS_LONG_AS_MMS, it) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Max MMS size
                                SettingsItem(
                                    icon = Icons.Outlined.Attachment,
                                    title = "Maximum MMS size",
                                    subtitle = maxMmsSize,
                                    onClick = { showMmsSizeDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Mobile numbers only
                                SettingsSwitchItem(
                                    icon = Icons.Outlined.PhoneAndroid,
                                    title = "Mobile numbers only",
                                    subtitle = "Filter out landlines when composing a new message",
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
                        Text(
                            "Swipe Actions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column {
                                SettingsItem(
                                    icon = Icons.Outlined.SwipeRight,
                                    title = "Swipe right action",
                                    subtitle = when (swipeRightAction) {
                                        "call" -> "Direct Call"
                                        "read" -> "Mark as Read / Unread"
                                        "delete" -> "Delete Conversation"
                                        else -> "None"
                                    },
                                    onClick = { showSwipeRightDialog = true }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                SettingsItem(
                                    icon = Icons.Outlined.SwipeLeft,
                                    title = "Swipe left action",
                                    subtitle = when (swipeLeftAction) {
                                        "delete" -> "Delete Conversation"
                                        "read" -> "Mark as Read / Unread"
                                        else -> "None"
                                    },
                                    onClick = { showSwipeLeftDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            // Sync Section
            item {
                RivoAnimatedSection(delayMs = 160L) {
                    RivoExpressiveCard {
                        SettingsItem(
                            icon = Icons.Outlined.Sync,
                            title = "Sync messages & threads",
                            subtitle = "Force reload all SMS and MMS conversations from system telephony provider",
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
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
