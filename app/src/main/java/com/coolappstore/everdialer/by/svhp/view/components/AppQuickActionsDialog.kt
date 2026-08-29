package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coolappstore.everdialer.by.svhp.controller.util.WHATSAPP_PACKAGES
import com.coolappstore.everdialer.by.svhp.controller.util.getGoogleMeetIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTelegramIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTruecallerIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getWhatsAppIcon
import com.coolappstore.everdialer.by.svhp.controller.util.isAnyPackageInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isGoogleMeetInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTelegramInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTruecallerInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.openTelegramChat
import com.coolappstore.everdialer.by.svhp.controller.util.openTruecaller
import com.coolappstore.everdialer.by.svhp.controller.util.openWhatsAppChat
import com.coolappstore.everdialer.by.svhp.controller.util.startGoogleMeetVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startGoogleMeetVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVoiceCall

/**
 * Floating popup shown after tapping WhatsApp/Telegram/Google Meet (Contact Info → Social, or the
 * Dialpad's long-press menu), offering the ways to reach the person through that app. [onChat] is
 * null for apps that don't have a chat concept (Google Meet), which hides that row — matching how
 * Google's own Contacts app only offers "Voice call" / "Video call" for Meet.
 */
@Composable
fun AppQuickActionsDialog(
    appName: String,
    onChat: (() -> Unit)? = null,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                if (onChat != null) {
                    AppQuickActionRow(icon = Icons.AutoMirrored.Filled.Chat, label = "Chat", onClick = onChat)
                }
                AppQuickActionRow(icon = Icons.Default.Call, label = "Voice Call", onClick = onVoiceCall)
                AppQuickActionRow(icon = Icons.Default.Videocam, label = "Video Call", onClick = onVideoCall)
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun AppQuickActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/**
 * Self-contained "Call/Chat Via" flow: an app picker (WhatsApp/Telegram, whichever are
 * installed) followed by that app's Chat/Voice Call/Video Call [AppQuickActionsDialog]. Shared by
 * every long-press context menu that offers "Call/Chat Via" (Favourites, Call Logs, Contacts, and
 * the Dialpad's own long-press menu) plus the Dialpad call button's long-press, so all of them
 * present the exact same picker and popup for a given [phoneNumber].
 *
 * [showPicker] is owned by the caller (typically toggled true from a menu item's onClick, right
 * after that menu closes itself). Once an app is chosen here, the Chat/Voice Call/Video Call
 * dialog is tracked internally and needs no further involvement from the caller.
 *
 * [showGoogleMeet] additionally lists a "Google Meet" entry below Telegram; tapping it opens the
 * same Voice Call / Video Call popup as WhatsApp/Telegram (no Chat row, since Meet has none) and
 * places a real Meet call the same way Google's own Contacts app does. [showFakeCall] additionally
 * lists a "Fake Call" entry below Google Meet, invoking [onFakeCall] on tap — off by default since
 * only the Dialpad's call button long-press opts into it.
 */
@Composable
fun CallChatViaOverlay(
    phoneNumber: String?,
    showPicker: Boolean,
    onPickerDismiss: () -> Unit,
    showGoogleMeet: Boolean = false,
    showFakeCall: Boolean = false,
    onFakeCall: (() -> Unit)? = null,
    // All of this contact's saved numbers (e.g. one with a country code, one without). When there
    // are 2+, picking WhatsApp/Telegram/Google Meet first asks which number to use instead of
    // silently defaulting to [phoneNumber] — which fixed the app to whichever number happened to
    // be saved first, even if that's not the one actually registered on WhatsApp/Meet/etc.
    phoneNumbers: List<String> = phoneNumber?.let { listOf(it) } ?: emptyList()
) {
    val allNumbers = remember(phoneNumbers) { phoneNumbers.filter { it.isNotBlank() }.distinct() }
    if (allNumbers.isEmpty()) return
    val context = LocalContext.current
    // App chosen from the picker but still waiting on a number pick (only used when the contact
    // has 2+ numbers); null once a number has been picked (or there was only one to begin with).
    var pendingAppForNumberPick by remember { mutableStateOf<String?>(null) }
    var showAppQuickActions by remember { mutableStateOf<String?>(null) }
    var selectedNumber by remember { mutableStateOf<String?>(null) }

    fun chooseApp(app: String) {
        if (app == "truecaller") {
            if (allNumbers.size > 1) {
                pendingAppForNumberPick = "truecaller"
            } else {
                openTruecaller(context, allNumbers.first())
            }
            return
        }
        if (allNumbers.size > 1) {
            pendingAppForNumberPick = app
        } else {
            selectedNumber = allNumbers.first()
            showAppQuickActions = app
        }
    }

    if (showPicker) {
        val hasWhatsApp = remember(context) { isAnyPackageInstalled(context, WHATSAPP_PACKAGES) }
        val hasTelegram = remember(context) { isTelegramInstalled(context) }
        val hasGoogleMeet = remember(context, showGoogleMeet) { showGoogleMeet && isGoogleMeetInstalled(context) }
        val hasTruecaller = remember(context) { isTruecallerInstalled(context) }
        RivoDropdownMenu(expanded = showPicker, onDismissRequest = onPickerDismiss) {
            if (hasWhatsApp) {
                RivoDropdownMenuItem(
                    text = "WhatsApp",
                    iconBitmap = remember(context) { getWhatsAppIcon(context) },
                    onClick = { onPickerDismiss(); chooseApp("whatsapp") }
                )
            }
            if (hasTelegram) {
                RivoDropdownMenuItem(
                    text = "Telegram",
                    iconBitmap = remember(context) { getTelegramIcon(context) },
                    onClick = { onPickerDismiss(); chooseApp("telegram") }
                )
            }
            if (hasGoogleMeet) {
                RivoDropdownMenuItem(
                    text = "Google Meet",
                    icon = Icons.Default.VideoCall,
                    iconBitmap = remember(context) { getGoogleMeetIcon(context) },
                    onClick = { onPickerDismiss(); chooseApp("googlemeet") }
                )
            }
            if (hasTruecaller) {
                RivoDropdownMenuItem(
                    text = "Truecaller",
                    icon = Icons.Default.Search,
                    iconBitmap = remember(context) { getTruecallerIcon(context) },
                    onClick = { onPickerDismiss(); chooseApp("truecaller") }
                )
            }
            if (showFakeCall && onFakeCall != null) {
                RivoDropdownMenuItem(
                    text = "Fake Call",
                    icon = Icons.Outlined.PhoneCallback,
                    onClick = { onPickerDismiss(); onFakeCall() }
                )
            }
            if (!hasWhatsApp && !hasTelegram && !hasGoogleMeet && !hasTruecaller && !(showFakeCall && onFakeCall != null)) {
                RivoDropdownMenuItem(
                    text = "No apps installed",
                    icon = Icons.Default.Info,
                    onClick = onPickerDismiss
                )
            }
        }
    }

    if (pendingAppForNumberPick != null) {
        val app = pendingAppForNumberPick!!
        NumberPickerDialog(
            numbers = allNumbers,
            onDismissRequest = { pendingAppForNumberPick = null },
            onNumberSelected = { number ->
                pendingAppForNumberPick = null
                if (app == "truecaller") {
                    openTruecaller(context, number)
                } else {
                    selectedNumber = number
                    showAppQuickActions = app
                }
            }
        )
    }

    if (showAppQuickActions != null && selectedNumber != null) {
        val app = showAppQuickActions!!
        val number = selectedNumber!!
        val appLabel = when (app) {
            "whatsapp" -> "WhatsApp"
            "telegram" -> "Telegram"
            else -> "Google Meet"
        }
        AppQuickActionsDialog(
            appName = appLabel,
            onChat = if (app == "googlemeet") null else {
                {
                    showAppQuickActions = null
                    val opened = if (app == "whatsapp") openWhatsAppChat(context, number) else openTelegramChat(context, number)
                    if (!opened) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onVoiceCall = {
                showAppQuickActions = null
                val started = when (app) {
                    "whatsapp" -> startWhatsAppVoiceCall(context, number)
                    "telegram" -> startTelegramVoiceCall(context, number)
                    else -> startGoogleMeetVoiceCall(context, number)
                }
                if (!started) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onVideoCall = {
                showAppQuickActions = null
                val started = when (app) {
                    "whatsapp" -> startWhatsAppVideoCall(context, number)
                    "telegram" -> startTelegramVideoCall(context, number)
                    else -> startGoogleMeetVideoCall(context, number)
                }
                if (!started) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAppQuickActions = null }
        )
    }
}
