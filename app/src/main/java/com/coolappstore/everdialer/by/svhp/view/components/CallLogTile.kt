package com.coolappstore.everdialer.by.svhp.view.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.WHATSAPP_PACKAGES
import com.coolappstore.everdialer.by.svhp.controller.util.isAnyPackageInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTelegramInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isGoogleMeetInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTruecallerInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.formatDate
import com.coolappstore.everdialer.by.svhp.controller.util.formatTimeOnly
import com.coolappstore.everdialer.by.svhp.controller.util.formatCallLogDate
import com.coolappstore.everdialer.by.svhp.controller.util.formatDuration
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.view.screen.settings.AddMode
import com.coolappstore.everdialer.by.svhp.view.screen.settings.FakeCallAddSheet
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.view.screen.SimCardIconWithNumber
import org.koin.compose.koinInject

/**
 * A small, clean SIM-card-chip badge — solid flat color with the slot number in bold, so the
 * number stays clearly legible at small sizes. Defaults to a rectangle with one clipped corner
 * (like a real SIM card); pass [shape] to use a plain rounded rect instead (used on the ongoing
 * call screen, where the badge is shown much smaller and the notch's diagonal edge doesn't
 * anti-alias as cleanly). Text is centered using a matched line height and no font padding
 * (rather than a fixed manual offset) so the digit stays perfectly centered at every size.
 */
private val SimCardNotchShape = GenericShape { size, _ ->
    val notch = size.minDimension * 0.42f
    moveTo(notch, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(0f, notch)
    close()
}

@Immutable
data class CallLogDisplayConfig(
    val use24HourTime: Boolean = false,
    val showTalkTime: Boolean = false,
    val groupCallsByLatest: Boolean = false,
    val showTotalCallsMade: Boolean = false,
    val showSims: Boolean = true,
    val hideNames: Boolean = false,
    val hiddenIds: Set<String> = emptySet(),
    val nameNonContactsAsUnknown: Boolean = true,
    val fakeCallInContextMenu: Boolean = false,
    val sim1Color: Color = Color(PreferenceManager.DEFAULT_SIM1_COLOR),
    val sim2Color: Color = Color(PreferenceManager.DEFAULT_SIM2_COLOR),
    val isScrollAnimEnabled: Boolean = true
)

@Composable
fun SimSlotBadge(
    slot: Int,
    modifier: Modifier = Modifier,
    shape: Shape = SimCardNotchShape,
    overrideColor: Color? = null,
    fontSize: androidx.compose.ui.unit.TextUnit? = null
) {
    val color = if (overrideColor != null) {
        overrideColor
    } else {
        val prefs = org.koin.compose.koinInject<PreferenceManager>()
        val settingsVer by prefs.settingsChanged.collectAsState()
        val sim1Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)) }
        val sim2Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)) }
        if (slot == 0) sim1Color else sim2Color
    }

    if (fontSize != null) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (slot == 0) "1" else "2",
                color = Color.White,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .size(width = 18.dp, height = 21.dp)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            val resolvedFontSize = (maxHeight.value * 0.52f).sp
            Text(
                text = if (slot == 0) "1" else "2",
                color = Color.White,
                fontSize = resolvedFontSize,
                lineHeight = resolvedFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

/**
 * Best-effort "national" digits of a phone number with any country code stripped, used so the
 * avatar for an unsaved number shows that number's actual first digit (and varies in color by
 * number) instead of a generic fixed "Unknown" placeholder. There's no phone-number library in
 * this project to parse country codes properly, so this uses the common heuristic that national
 * numbers are 10 digits long (true for India, the US/Canada, and many other countries) and
 * treats anything beyond the last 10 digits as the country code.
 */
private fun nationalNumberDigits(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

@Composable
fun CallLogTileSimple(log: CallLogEntry, use24HourTime: Boolean? = null) {
    val is24H = if (use24HourTime != null) {
        use24HourTime
    } else {
        val prefs = koinInject<PreferenceManager>()
        val settingsVer by prefs.settingsChanged.collectAsState()
        remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }
    }
    val isMissed = log.type == CallLog.Calls.MISSED_TYPE

    val icon = when (log.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        CallLog.Calls.MISSED_TYPE   -> Icons.AutoMirrored.Filled.CallMissed
        else                        -> Icons.Default.Call
    }
    val ringDurationText = if (isMissed && log.duration > 0) "${log.duration}s rang" else null
    val durationText = if (isMissed) ringDurationText else if (log.duration > 0) android.text.format.DateUtils.formatElapsedTime(log.duration) else null

    RivoListItem(
        headline = when (log.type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE   -> "Missed"
            else                        -> "Call"
        },
        supporting = "${formatDate(log.date, is24H)}${if (durationText != null) " • $durationText" else ""}",
        leadingIcon = icon,
        iconContainerColor = if (isMissed) MaterialTheme.colorScheme.errorContainer else null,
        onClick = { }
    )
}

fun toSuperscript(number: Int): String {
    val superscripts = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹'
    )
    return number.toString().map { superscripts[it] ?: it }.joinToString("")
}

@Composable
fun CallLogTile(
    log: CallLogEntry,
    onTileClick: (CallLogEntry) -> Unit,
    onButtonClick: (CallLogEntry) -> Unit,
    onAvatarClick: ((CallLogEntry) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onSelectToggle: ((CallLogEntry) -> Unit)? = null,
    onSelectMode: ((CallLogEntry) -> Unit)? = null,
    totalCallsCount: Int? = null,
    config: CallLogDisplayConfig? = null
) {
    val context   = LocalContext.current
    val isContact = log.name != null && log.name != log.number
    var showMenu  by remember { mutableStateOf(false) }

    val fakeCallInContextMenu: Boolean
    val use24HourTime: Boolean
    val showTalkTime: Boolean
    val groupCallsByLatest: Boolean
    val showTotalCallsMade: Boolean
    val hideNames: Boolean
    val hiddenIds: Set<String>
    val nameNonContactsAsUnknown: Boolean
    val showSimsSetting: Boolean
    val sim1Color: Color
    val sim2Color: Color

    if (config != null) {
        fakeCallInContextMenu = config.fakeCallInContextMenu
        use24HourTime = config.use24HourTime
        showTalkTime = config.showTalkTime
        groupCallsByLatest = config.groupCallsByLatest
        showTotalCallsMade = config.showTotalCallsMade
        hideNames = config.hideNames
        hiddenIds = config.hiddenIds
        nameNonContactsAsUnknown = config.nameNonContactsAsUnknown
        showSimsSetting = config.showSims
        sim1Color = config.sim1Color
        sim2Color = config.sim2Color
    } else {
        val prefs = koinInject<PreferenceManager>()
        val settingsVer by prefs.settingsChanged.collectAsState()
        fakeCallInContextMenu = remember(settingsVer) {
            prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
        }
        use24HourTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }
        showTalkTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, false) }
        groupCallsByLatest = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false) }
        showTotalCallsMade = remember(settingsVer) {
            groupCallsByLatest || prefs.getBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
        }
        hideNames = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false) }
        hiddenIds = remember(settingsVer) {
            val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
            if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
        }
        nameNonContactsAsUnknown = remember(settingsVer) {
            prefs.getBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, true)
        }
        showSimsSetting = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault()) }
        sim1Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)) }
        sim2Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)) }
    }

    var showFakeCallSheet by remember { mutableStateOf(false) }
    var showCallChatViaPicker by remember { mutableStateOf(false) }

    val isHiddenContact = remember(log.contactId, hiddenIds, hideNames) {
        hideNames && hiddenIds.isNotEmpty() && log.contactId != null && log.contactId in hiddenIds
    }
    val displayName = when {
        isHiddenContact -> log.number
        isContact -> log.name!!
        nameNonContactsAsUnknown -> "Unknown"
        else -> log.number
    }
    val avatarSourceName = when {
        isHiddenContact -> log.number
        isContact -> log.name!!
        else -> nationalNumberDigits(log.number).ifEmpty { "Unknown" }
    }

    val isMissed = log.type == CallLog.Calls.MISSED_TYPE

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
            exit  = fadeOut(tween(300)) + shrinkHorizontally(tween(300))
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle?.invoke(log) },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Box(modifier = Modifier.weight(1f)) {
        val showSimBadge = showSimsSetting && log.simSlot in 0..1
        val showNumberOnSupportingLine = !isHiddenContact && (isContact || nameNonContactsAsUnknown)
        val simBadge: (@Composable () -> Unit)? = if (showSimBadge) ({
            SimSlotBadge(
                slot = log.simSlot,
                overrideColor = if (log.simSlot == 0) sim1Color else sim2Color,
                fontSize = 8.sp,
                modifier = Modifier.size(width = 14.dp, height = 16.dp)
            )
        }) else null
        val totalCallsBadge: (@Composable () -> Unit)? = if (showTotalCallsMade) {
            val total = totalCallsCount ?: log.count
            {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else null
        RivoListItem(
            headline = buildString {
                append(displayName)
                if (log.count > 1 && !showTotalCallsMade) append(" (${log.count})")
            },
            supporting = if (showNumberOnSupportingLine) log.number else null,
            avatarName  = avatarSourceName,
            avatarForcePersonIcon = !isContact,
            photoUri    = log.photoUri,
            headlineStartContent = if (!showNumberOnSupportingLine) simBadge else null,
            headlineEndContent = totalCallsBadge,
            supportingStartContent = if (showNumberOnSupportingLine) simBadge else null,
            trailingText = formatTimeOnly(log.date, use24HourTime),
            trailingTextColor = if (isMissed) MaterialTheme.colorScheme.error else null,
            trailingSubText = if (groupCallsByLatest) {
                val dateText = formatCallLogDate(log.date)
                if (showTalkTime && !isMissed) {
                    val durationText = formatDuration(log.duration)
                    "$dateText • $durationText"
                } else {
                    dateText
                }
            } else {
                if (isMissed) {
                    if (log.duration > 0) "${log.duration}s" else null
                } else if (showTalkTime) {
                    formatDuration(log.duration)
                } else null
            },
            trailingSubTextColor = if (groupCallsByLatest) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            } else {
                if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            },
            trailingIcon = when (log.type) {
                CallLog.Calls.MISSED_TYPE   -> Icons.AutoMirrored.Filled.CallMissed
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                else                        -> Icons.Default.Call
            },
            trailingIconTint = if (isMissed) MaterialTheme.colorScheme.error else null,
            onAvatarClick = if (onAvatarClick != null) ({ onAvatarClick(log) }) else null,
            onLongClick = {
                if (selectionMode) onSelectToggle?.invoke(log)
                else showMenu = true
            },
            isMenuOpen  = showMenu && !selectionMode,
            onClick     = {
                if (selectionMode) onSelectToggle?.invoke(log)
                else onTileClick(log)
            }
        )

        if (showMenu) {
            val prefs = koinInject<PreferenceManager>()
            val settingsVer by prefs.settingsChanged.collectAsState()
            val isNumberBlocked = remember(settingsVer, log.number) { BlockedNumbersManager.isBlocked(context, prefs, log.number) }
            val hasWhatsApp = remember(context) { isAnyPackageInstalled(context, WHATSAPP_PACKAGES) }
            val hasTelegram = remember(context) { isTelegramInstalled(context) }
            val hasGoogleMeet = remember(context) { isGoogleMeetInstalled(context) }
            val hasTruecaller = remember(context) { isTruecallerInstalled(context) }
            val hasAnySocialApp = hasWhatsApp || hasTelegram || hasGoogleMeet || hasTruecaller

            // Respect Settings → Appearance → "Context Menu Elements" customization (show/hide + order)
            val callLogContextMenuKeys = remember(settingsVer, isContact, fakeCallInContextMenu) {
                com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.resolvedKeys(
                    prefs,
                    com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.SECTION_CALL_LOGS,
                    listOf("select", "call_back", "call_chat_via", "search_truecaller", "copy_number", "share", "add_to_contacts", "block_number", "fake_call", "delete_call_log")
                ).filter { key ->
                    when (key) {
                        "add_to_contacts" -> !isContact
                        "search_truecaller" -> !isContact && log.number.isNotBlank()
                        "fake_call" -> fakeCallInContextMenu
                        "call_chat_via" -> log.number.isNotBlank()
                        else -> true
                    }
                }
            }

            RivoDropdownMenu(
                expanded          = showMenu,
                onDismissRequest  = { showMenu = false }
            ) {
            callLogContextMenuKeys.forEachIndexed { index, key ->
                if (key == "delete_call_log" && index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                when (key) {
                    "select" -> RivoDropdownMenuItem(
                        text     = "Select",
                        icon     = Icons.Default.CheckBox,
                        iconTint = Color(0xFF9C27B0),
                        onClick  = {
                            showMenu = false
                            onSelectMode?.invoke(log)
                        }
                    )
                    "call_back" -> {
                        val showSimButtonsInDialpad = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SHOW_SIM_BUTTONS_IN_DIALPAD, false) }
                        val hasTwoSims = remember(settingsVer) {
                            prefs.getActiveSimCount() >= 2 || run {
                                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                                try { (tm?.callCapablePhoneAccounts?.size ?: 0) >= 2 } catch (_: Throwable) { false }
                            }
                        }
                        if (hasTwoSims && showSimButtonsInDialpad) {
                            val sim1Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)) }
                            val sim2Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)) }
                            val tm = remember(context) { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }
                            val accounts: List<PhoneAccountHandle> = try { tm?.callCapablePhoneAccounts } catch (_: Throwable) { null } ?: emptyList()
                            val account1 = accounts.getOrNull(0)
                            val account2 = accounts.getOrNull(1)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        showMenu = false
                                        makeCall(context, log.number, account1)
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = sim1Color,
                                    contentColor = Color.White
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SimCardIconWithNumber(
                                            simSlotNumber = "1",
                                            tint = Color.White,
                                            isLarge = false
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SIM 1", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Surface(
                                    onClick = {
                                        showMenu = false
                                        makeCall(context, log.number, account2)
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = sim2Color,
                                    contentColor = Color.White
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SimCardIconWithNumber(
                                            simSlotNumber = "2",
                                            tint = Color.White,
                                            isLarge = false
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SIM 2", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            RivoDropdownMenuItem(
                                text     = "Call back",
                                icon     = Icons.Default.Call,
                                iconTint = Color(0xFF4CAF50),
                                onClick  = { showMenu = false; onButtonClick(log) }
                            )
                        }
                    }
                    "call_chat_via" -> {
                        if (hasAnySocialApp) {
                            RivoDropdownMenuItem(
                                text     = "Call/Chat Via",
                                icon     = Icons.AutoMirrored.Filled.Chat,
                                iconTint = Color(0xFF00BFA5),
                                onClick  = {
                                    showMenu = false
                                    showCallChatViaPicker = true
                                }
                            )
                        }
                    }
                    "search_truecaller" -> {
                        if (hasTruecaller) {
                            RivoDropdownMenuItem(
                                text     = "Search Truecaller",
                                icon     = Icons.Default.Search,
                                iconTint = Color(0xFF0084FF),
                                onClick  = {
                                    showMenu = false
                                    val isLaunched: Boolean = com.coolappstore.everdialer.by.svhp.controller.util.openTruecaller(context, log.number)
                                    if (!isLaunched) {
                                        Toast.makeText(context, "Truecaller is not installed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                    "copy_number" -> RivoDropdownMenuItem(
                        text     = "Copy number",
                        icon     = Icons.Default.ContentCopy,
                        iconTint = Color(0xFF2196F3),
                        onClick  = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", log.number))
                            Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    "share" -> RivoDropdownMenuItem(
                        text     = "Share",
                        icon     = Icons.Default.Share,
                        iconTint = Color(0xFFFF9800),
                        onClick  = {
                            showMenu = false
                            val primaryNum = log.contactId?.let { prefs.getContactDefaultNumber(it) } ?: log.number
                            val shareText = if (!log.name.isNullOrBlank() && log.name != log.number) {
                                "${log.name}\n$primaryNum"
                            } else {
                                primaryNum
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share contact"))
                        }
                    )
                    "add_to_contacts" -> RivoDropdownMenuItem(
                        text     = "Add to contacts",
                        icon     = Icons.Default.PersonAdd,
                        iconTint = Color(0xFF9C27B0),
                        onClick  = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                putExtra(ContactsContract.Intents.Insert.PHONE, log.number)
                            }
                            context.startActivity(intent)
                        }
                    )
                    "block_number" -> RivoDropdownMenuItem(
                        text     = if (isNumberBlocked) "Unblock number" else "Block number",
                        icon     = if (isNumberBlocked) Icons.Default.RemoveCircleOutline else Icons.Default.Block,
                        iconTint = if (isNumberBlocked) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        onClick  = {
                            showMenu = false
                            if (log.number.isBlank()) return@RivoDropdownMenuItem
                            BlockedNumbersManager.toggle(context, prefs, log.number)
                            Toast.makeText(
                                context,
                                if (isNumberBlocked) "Number unblocked" else "Number blocked",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    "fake_call" -> RivoDropdownMenuItem(
                        text     = "Fake Call",
                        icon     = Icons.Outlined.PhoneCallback,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick  = {
                            showMenu = false
                            showFakeCallSheet = true
                        }
                    )
                    "delete_call_log" -> RivoDropdownMenuItem(
                        text          = "Delete from call log",
                        icon          = Icons.Default.Delete,
                        isDestructive = true,
                        onClick       = {
                            showMenu = false
                            try {
                                val allIds = log.callIds.filter { it > 0 }.distinct()
                                if (allIds.isNotEmpty()) {
                                    allIds.chunked(500).forEach { chunk ->
                                        val inClause = chunk.joinToString(",")
                                        context.contentResolver.delete(
                                            CallLog.Calls.CONTENT_URI,
                                            "${CallLog.Calls._ID} IN ($inClause)",
                                            null
                                        )
                                    }
                                }
                                val targetDates = (log.dates + log.date).distinct()
                                targetDates.chunked(100).forEach { dateChunk ->
                                    val placeholders = dateChunk.map { "?" }.joinToString(",")
                                    val args = (listOf(log.number) + dateChunk.map { it.toString() }).toTypedArray()
                                    context.contentResolver.delete(
                                        CallLog.Calls.CONTENT_URI,
                                        "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} IN ($placeholders)",
                                        args
                                    )
                                }
                                onDelete?.invoke()
                                Toast.makeText(context, "Deleted from call log", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not delete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                if (key == "select" && index < callLogContextMenuKeys.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        }
        }
    }

    if (showFakeCallSheet) {
        val prefs = koinInject<PreferenceManager>()
        FakeCallAddSheet(
            mode = AddMode.Number,
            initialNumber = log.number,
            initialDisplayName = log.name ?: log.number,
            onDismiss = { showFakeCallSheet = false },
            onSave = { entry, exactTriggerOverride ->
                FakeCallManager.addEntry(context, prefs, entry, exactTriggerOverride)
                showFakeCallSheet = false
            }
        )
    }

    if (showCallChatViaPicker) {
        val contactsRepo = koinInject<IContactsRepository>()
        var callChatViaNumbers by remember(log.number) { mutableStateOf<List<String>?>(null) }
        LaunchedEffect(log.number) {
            if (callChatViaNumbers == null) {
                callChatViaNumbers = try { contactsRepo.getContactByNumber(log.number)?.phoneNumbers } catch (_: Exception) { null }
            }
        }
        CallChatViaOverlay(
            phoneNumber = log.number.takeIf { it.isNotBlank() },
            phoneNumbers = callChatViaNumbers?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(log.number.takeIf { it.isNotBlank() }),
            showPicker = showCallChatViaPicker,
            onPickerDismiss = { showCallChatViaPicker = false },
            showGoogleMeet = true
        )
    }
}
