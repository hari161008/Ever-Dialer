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
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.ui.graphics.toArgb
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
import android.content.ContentUris
import android.net.Uri
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.view.screen.settings.AddMode
import com.coolappstore.everdialer.by.svhp.view.screen.settings.FakeCallAddSheet
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.view.screen.SimCardIconWithNumber
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

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
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val is24H = if (use24HourTime != null) {
        use24HourTime
    } else {
        remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }
    }
    val isMissed = log.type == CallLog.Calls.MISSED_TYPE

    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }

    val trailingContainerColor = if (isMissed) {
        if (isSaturatedActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.errorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }

    val trailingTint = if (isMissed) {
        if (isSaturatedActive) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onErrorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer
    }

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
        trailingIcon = icon,
        trailingIconTint = trailingTint,
        trailingIconContainerColor = trailingContainerColor,
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
    config: CallLogDisplayConfig? = null,
    navigator: DestinationsNavigator? = null
) {
    val context   = LocalContext.current
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val matchedContact = remember(log.contactId, log.number, allContacts) {
        if (!log.contactId.isNullOrBlank() && log.contactId != "0" && log.contactId != "-1" && log.contactId != "null") {
            allContacts.find { it.id == log.contactId }
        } else if (log.number.isNotBlank()) {
            allContacts.find { c -> c.phoneNumbers.any { n -> numbersLikelyMatch(log.number, n) } }
        } else null
    }
    val resolvedContactName = matchedContact?.name?.takeIf { it.isNotBlank() }
        ?: log.name?.takeIf { it.isNotBlank() && it != log.number && !log.isCallerIdName }
    val isContact = resolvedContactName != null
    val isCallerId = !isContact && log.isCallerIdName && !log.name.isNullOrBlank() && log.name != log.number
    var showMenu  by remember { mutableStateOf(false) }
    var showAddContactChoiceDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val isMissed = log.type == CallLog.Calls.MISSED_TYPE

    val trailingContainerColor = if (isMissed) {
        if (isSaturatedActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.errorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }

    val trailingTint = if (isMissed) {
        if (isSaturatedActive) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onErrorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer
    }

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
        isContact -> resolvedContactName ?: log.number
        isCallerId -> log.name ?: log.number
        nameNonContactsAsUnknown -> "Unknown"
        else -> log.number
    }
    val avatarSourceName = when {
        isHiddenContact -> log.number
        isContact -> resolvedContactName ?: log.number
        isCallerId -> log.name ?: log.number
        else -> nationalNumberDigits(log.number).ifEmpty { "Unknown" }
    }

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
        val showNumberOnSupportingLine = !isHiddenContact && (isContact || isCallerId || nameNonContactsAsUnknown)
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
            headlineMaxLines = 2,
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
            trailingIconTint = trailingTint,
            trailingIconContainerColor = trailingContainerColor,
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
            val defaultCallLogOrder = listOf(
                "select", "call_back", "view_contact", "edit_contact", "copy_number", "add_to_contacts",
                "share", "call_chat_via", "send_text", "search_truecaller", "move_contact", "toggle_favorite",
                "block_number", "fake_call", "delete_call_log"
            )
            val callLogContextMenuKeys = remember(settingsVer, isContact, matchedContact, fakeCallInContextMenu, hasAnySocialApp, hasTruecaller) {
                com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.resolvedKeys(
                    prefs,
                    com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.SECTION_CALL_LOGS,
                    defaultCallLogOrder
                ).filter { key ->
                    when (key) {
                        "view_contact"      -> isContact
                        "edit_contact"      -> isContact
                        "add_to_contacts"   -> !isContact
                        "search_truecaller" -> !isContact && log.number.isNotBlank() && hasTruecaller
                        "move_contact"      -> isContact && matchedContact != null
                        "toggle_favorite"   -> isContact && matchedContact != null
                        "fake_call"         -> fakeCallInContextMenu
                        "call_chat_via"     -> log.number.isNotBlank() && hasAnySocialApp
                        "send_text"         -> log.number.isNotBlank()
                        else -> true
                    }
                }
            }

            RivoDropdownMenu(
                expanded          = showMenu,
                onDismissRequest  = { showMenu = false }
            ) {
                fun groupOf(key: String) = when (key) {
                    "select" -> 0
                    "call_back", "view_contact", "edit_contact", "copy_number", "add_to_contacts", "share", "call_chat_via", "send_text", "search_truecaller" -> 1
                    "move_contact", "toggle_favorite", "block_number", "fake_call" -> 2
                    "delete_call_log" -> 3
                    else -> 1
                }
                var previousGroup: Int? = null
                callLogContextMenuKeys.forEach { key ->
                    val group = groupOf(key)
                    if (previousGroup != null && group != previousGroup) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    previousGroup = group
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
                            val hasTwoSims = remember(settingsVer) {
                                prefs.getActiveSimCount() >= 2 || run {
                                    val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                                    try { (tm?.callCapablePhoneAccounts?.size ?: 0) >= 2 } catch (_: Throwable) { false }
                                }
                            }
                            val sim1Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)) }
                            val sim2Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)) }
                            val tm = remember(context) { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }
                            val accounts: List<PhoneAccountHandle> = try { tm?.callCapablePhoneAccounts } catch (_: Throwable) { null } ?: emptyList()
                            val account1 = accounts.getOrNull(0)
                            val account2 = accounts.getOrNull(1)

                            RivoDropdownMenuItem(
                                text     = "Call back",
                                icon     = Icons.Default.Call,
                                iconTint = Color(0xFF4CAF50),
                                onClick  = { showMenu = false; onButtonClick(log) },
                                trailingContent = if (hasTwoSims && log.number.isNotBlank()) {
                                    {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = {
                                                    showMenu = false
                                                    makeCall(context, log.number, account1)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = sim1Color,
                                                contentColor = Color.White,
                                                modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    SimCardIconWithNumber(
                                                        simSlotNumber = "1",
                                                        tint = Color.White,
                                                        isLarge = false
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = {
                                                    showMenu = false
                                                    makeCall(context, log.number, account2)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = sim2Color,
                                                contentColor = Color.White,
                                                modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    SimCardIconWithNumber(
                                                        simSlotNumber = "2",
                                                        tint = Color.White,
                                                        isLarge = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else null
                            )
                        }
                        "view_contact" -> {
                            if (isContact) {
                                RivoDropdownMenuItem(
                                    text     = "View contact",
                                    icon     = Icons.Default.Person,
                                    iconTint = Color(0xFF2196F3),
                                    onClick  = {
                                        showMenu = false
                                        if (navigator != null) {
                                            navigator.navigate(
                                                ContactDetailsScreenDestination(
                                                    contactId = matchedContact?.id ?: log.contactId ?: "null",
                                                    phoneNumber = log.number
                                                )
                                            )
                                        } else {
                                            onTileClick(log)
                                        }
                                    }
                                )
                            }
                        }
                        "edit_contact" -> {
                            if (isContact) {
                                RivoDropdownMenuItem(
                                    text     = "Edit contact",
                                    icon     = Icons.Default.Edit,
                                    iconTint = Color(0xFF9C27B0),
                                    onClick  = {
                                        showMenu = false
                                        val targetId = matchedContact?.id ?: log.contactId
                                        if (targetId != null && targetId.isNotBlank() && targetId != "null" && navigator != null) {
                                            navigator.navigate(ContactEditScreenDestination(contactId = targetId))
                                        } else if (targetId != null && targetId.isNotBlank() && targetId != "null") {
                                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                                data = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, targetId.toLongOrNull() ?: 0L)
                                            }
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        } else if (navigator != null) {
                                            navigator.navigate(ContactEditScreenDestination(initialPhone = log.number))
                                        }
                                    }
                                )
                            }
                        }
                        "copy_number" -> RivoDropdownMenuItem(
                            text     = "Copy number",
                            icon     = Icons.Default.ContentCopy,
                            iconTint = Color(0xFF009688),
                            onClick  = {
                                showMenu = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", log.number))
                                Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        "add_to_contacts" -> {
                            if (!isContact) {
                                RivoDropdownMenuItem(
                                    text     = "Add contact",
                                    icon     = Icons.Default.PersonAdd,
                                    iconTint = Color(0xFF9C27B0),
                                    onClick  = {
                                        showMenu = false
                                        showAddContactChoiceDialog = true
                                    }
                                )
                            }
                        }
                        "share" -> RivoDropdownMenuItem(
                            text     = if (isContact) "Share contact" else "Share",
                            icon     = Icons.Default.Share,
                            iconTint = Color(0xFFFF9800),
                            onClick  = {
                                showMenu = false
                                val primaryNum = log.contactId?.let { prefs.getContactDefaultNumber(it) } ?: log.number
                                val shareText = if (isContact && !displayName.isNullOrBlank() && displayName != log.number) {
                                    "$displayName\n$primaryNum"
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
                        "send_text" -> RivoDropdownMenuItem(
                            text     = "Send text",
                            icon     = Icons.AutoMirrored.Filled.Message,
                            iconTint = Color(0xFF009688),
                            onClick  = {
                                showMenu = false
                                if (log.number.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${log.number}"))
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                }
                            }
                        )
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
                        "move_contact" -> {
                            if (matchedContact != null) {
                                RivoDropdownMenuItem(
                                    text     = "Move contact",
                                    icon     = Icons.Default.DriveFileMove,
                                    iconTint = Color(0xFF00897B),
                                    onClick  = {
                                        showMenu = false
                                        showMoveDialog = true
                                    }
                                )
                            }
                        }
                        "toggle_favorite" -> {
                            if (matchedContact != null) {
                                val isFav = matchedContact.isFavorite
                                RivoDropdownMenuItem(
                                    text     = if (isFav) "Remove from Favourites" else "Add to Favourites",
                                    icon     = Icons.Default.Favorite,
                                    iconTint = if (isFav) Color(0xFFF44336) else Color(0xFFE91E63),
                                    isDestructive = isFav,
                                    onClick  = {
                                        showMenu = false
                                        contactsVM.toggleFavorite(matchedContact)
                                    }
                                )
                            }
                        }
                        "block_number" -> RivoDropdownMenuItem(
                            text     = if (isNumberBlocked) (if (isContact) "Unblock contact" else "Unblock number") else (if (isContact) "Block contact" else "Block number"),
                            icon     = if (isNumberBlocked) Icons.Default.RemoveCircleOutline else Icons.Default.Block,
                            iconTint = if (isNumberBlocked) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            onClick  = {
                                showMenu = false
                                if (log.number.isBlank()) return@RivoDropdownMenuItem
                                BlockedNumbersManager.toggle(context, prefs, log.number)
                                Toast.makeText(
                                    context,
                                    if (isNumberBlocked) (if (isContact) "Contact unblocked" else "Number unblocked") else (if (isContact) "Contact blocked" else "Number blocked"),
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
                }
            }
        }
        }
    }

    if (showMoveDialog && matchedContact != null) {
        val moveTargets = remember { contactsVM.getSaveTargets() }
        MoveContactDialog(
            contactName = displayName,
            targets = moveTargets,
            onSelect = { target ->
                showMoveDialog = false
                contactsVM.moveContact(matchedContact, target) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Moved to ${target.label}" else "Couldn't move contact",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    if (showAddContactChoiceDialog) {
        AddContactChoiceDialog(
            visible = showAddContactChoiceDialog,
            phoneNumber = log.number,
            onDismissRequest = { showAddContactChoiceDialog = false },
            navigator = navigator
        )
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

    val callChatViaNumbers = remember(matchedContact, log.number) {
        matchedContact?.phoneNumbers?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(log.number.takeIf { it.isNotBlank() })
    }
    CallChatViaOverlay(
        phoneNumber = log.number.takeIf { it.isNotBlank() },
        phoneNumbers = callChatViaNumbers,
        showPicker = showCallChatViaPicker,
        onPickerDismiss = { showCallChatViaPicker = false },
        showGoogleMeet = true
    )
}
