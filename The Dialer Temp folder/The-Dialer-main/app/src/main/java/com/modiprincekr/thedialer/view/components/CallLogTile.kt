package com.modiprincekr.thedialer.view.components

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.ContactPhone
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
import com.modiprincekr.thedialer.controller.util.BlockedNumbersManager
import com.modiprincekr.thedialer.controller.util.FakeCallManager
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.controller.util.WHATSAPP_PACKAGES
import com.modiprincekr.thedialer.controller.util.isAnyPackageInstalled
import com.modiprincekr.thedialer.controller.util.isTelegramInstalled
import com.modiprincekr.thedialer.controller.util.isGoogleMeetInstalled
import com.modiprincekr.thedialer.view.components.SwipeableItemContainer
import com.modiprincekr.thedialer.controller.util.SwipeActionHelper
import com.modiprincekr.thedialer.controller.util.isTruecallerInstalled
import com.modiprincekr.thedialer.controller.util.formatDate
import com.modiprincekr.thedialer.controller.util.formatTimeOnly
import com.modiprincekr.thedialer.controller.util.formatCallLogDate
import com.modiprincekr.thedialer.controller.util.formatDuration
import android.content.ContentUris
import android.net.Uri
import com.modiprincekr.thedialer.controller.ContactsViewModel
import com.modiprincekr.thedialer.controller.util.numbersLikelyMatch
import com.modiprincekr.thedialer.modal.data.RecordingItem
import androidx.compose.material.icons.rounded.PlayArrow
import com.modiprincekr.thedialer.modal.data.CallLogEntry
import com.modiprincekr.thedialer.modal.data.Contact
import com.modiprincekr.thedialer.view.components.SwipeActionItem
import com.modiprincekr.thedialer.view.screen.settings.AddMode
import com.modiprincekr.thedialer.view.screen.settings.FakeCallAddSheet
import com.modiprincekr.thedialer.controller.util.makeCall
import com.modiprincekr.thedialer.controller.util.getPhoneAccountForSimSlot
import com.modiprincekr.thedialer.view.screen.SimCardIconWithNumber
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
val SimCardNotchShape = GenericShape { size, _ ->
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
    val showMissedCallDuration: Boolean = false,
    val groupCallsByLatest: Boolean = false,
    val showTotalCallsMade: Boolean = false,
    val showSims: Boolean = true,
    val hideNames: Boolean = false,
    val hiddenIds: Set<String> = emptySet(),
    val nameNonContactsAsUnknown: Boolean = true,
    val fakeCallInContextMenu: Boolean = false,
    val showAvatars: Boolean = true,
    val sim1Color: Color = Color(PreferenceManager.DEFAULT_SIM1_COLOR),
    val sim2Color: Color = Color(PreferenceManager.DEFAULT_SIM2_COLOR),
    val isScrollAnimEnabled: Boolean = true,
    val showNumbersInCallLogs: Boolean = true,
    val isSaturatedActive: Boolean = false,
    val leftSwipeKey: String = "call",
    val rightSwipeKey: String = "send_text",
    val leftSwipeAction: SwipeActionItem? = null,
    val rightSwipeAction: SwipeActionItem? = null
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

fun findMatchingRecording(
    log: CallLogEntry,
    recordings: List<RecordingItem>,
    contactPhoneNumbers: List<String> = emptyList()
): RecordingItem? {
    if (recordings.isEmpty()) return null

    // Call timestamps to match against (single or grouped dates)
    val callTimestamps = if (log.dates.isNotEmpty()) log.dates else listOf(log.date)

    // Filter recordings that belong to this phone number / contact
    val candidateRecordings = recordings.filter { rec ->
        val recDate = rec.date?.time ?: return@filter false
        val numberMatches = numbersLikelyMatch(rec.phoneNumber, log.number) ||
                contactPhoneNumbers.any { numbersLikelyMatch(rec.phoneNumber, it) } ||
                ((rec.phoneNumber.isBlank() || rec.phoneNumber == "Unknown") &&
                        !rec.contactName.isNullOrBlank() && !log.name.isNullOrBlank() &&
                        rec.contactName.equals(log.name, ignoreCase = true))

        if (!numberMatches) return@filter false

        // Timing match: check if recDate is close to any of the call's timestamps
        // Allow up to 120s (2 mins) window before/after call start, or within call duration interval
        callTimestamps.any { callStart ->
            val diff = kotlin.math.abs(recDate - callStart)
            val callDurationMs = log.duration * 1000L
            diff <= 120_000L || (recDate in (callStart - 30_000L)..(callStart + callDurationMs + 30_000L))
        }
    }

    if (candidateRecordings.isEmpty()) return null

    // Pick the best match with the smallest timing difference to the closest call timestamp
    return candidateRecordings.minByOrNull { rec ->
        val recDate = rec.date!!.time
        callTimestamps.minOf { callStart -> kotlin.math.abs(recDate - callStart) }
    }
}

@Composable
fun CallLogTileSimple(
    log: CallLogEntry,
    use24HourTime: Boolean? = null,
    matchingRecording: RecordingItem? = null,
    onRecordingClick: ((RecordingItem) -> Unit)? = null,
    onCallClick: ((CallLogEntry) -> Unit)? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val is24H = if (use24HourTime != null) {
        use24HourTime
    } else {
        remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }
    }
    val isMissed = log.type == CallLog.Calls.MISSED_TYPE || log.type == 5 || log.types.any { it == CallLog.Calls.MISSED_TYPE || it == 5 }
    val isBlocked = log.type == CallLog.Calls.BLOCKED_TYPE || log.types.any { it == CallLog.Calls.BLOCKED_TYPE }

    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }

    val trailingContainerColor = if (isMissed || isBlocked) {
        if (isSaturatedActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.errorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }

    val trailingTint = if (isMissed || isBlocked) {
        if (isSaturatedActive) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onErrorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer
    }

    val icon = when {
        log.isWhatsApp && log.isVideo           -> Icons.Default.Videocam
        log.type == CallLog.Calls.INCOMING_TYPE || log.type == 7 || log.types.any { it == CallLog.Calls.INCOMING_TYPE || it == 7 } -> Icons.AutoMirrored.Filled.CallReceived
        log.type == CallLog.Calls.OUTGOING_TYPE || log.types.any { it == CallLog.Calls.OUTGOING_TYPE } -> Icons.AutoMirrored.Filled.CallMade
        log.type == CallLog.Calls.MISSED_TYPE || log.type == 5 || log.types.any { it == CallLog.Calls.MISSED_TYPE || it == 5 }   -> Icons.AutoMirrored.Filled.CallMissed
        log.type == CallLog.Calls.BLOCKED_TYPE || log.types.any { it == CallLog.Calls.BLOCKED_TYPE }  -> Icons.Default.Block
        else                                    -> Icons.Default.Call
    }
    val showMissedCallDuration = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SHOW_MISSED_CALL_DURATION, false) }
    val ringDurationText = if (isBlocked && log.duration > 0) "${log.duration}s rang" else if (isMissed && showMissedCallDuration && log.duration > 0) "${log.duration}s rang" else null
    val durationText = if (isMissed || isBlocked) ringDurationText else if (log.duration > 0) android.text.format.DateUtils.formatElapsedTime(log.duration) else null

    val count = if (log.count > 1) log.count else if (log.callIds.size > 1) log.callIds.size else if (log.types.size > 1) log.types.size else 1
    val baseHeadline = when {
        log.isWhatsApp && log.isVideo           -> if (log.sourcePackage == "com.whatsapp.w4b") "WA Business Video" else "WhatsApp Video"
        log.isWhatsApp                          -> if (log.sourcePackage == "com.whatsapp.w4b") "WA Business Voice" else "WhatsApp Voice"
        log.type == CallLog.Calls.INCOMING_TYPE || log.type == 7 -> "Incoming"
        log.type == CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        log.type == CallLog.Calls.MISSED_TYPE   -> "Missed"
        log.type == 5                           -> "Rejected"
        log.type == CallLog.Calls.BLOCKED_TYPE  -> "Blocked"
        else                                    -> "Call"
    }

    RivoListItem(
        headline = if (count > 1) "$baseHeadline ($count)" else baseHeadline,
        headlineEndContent = if (count > 1) {
            {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        } else null,
        supporting = "${formatDate(log.date, is24H)}${if (durationText != null) " • $durationText" else ""}",
        trailingStartContent = if (onCallClick != null || (matchingRecording != null && onRecordingClick != null)) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (matchingRecording != null && onRecordingClick != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onRecordingClick(matchingRecording) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Recording",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                    if (onCallClick != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onCallClick(log) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else null,
        trailingIcon = icon,
        trailingIconTint = trailingTint,
        trailingIconContainerColor = trailingContainerColor,
        onClick = {
            onCallClick?.invoke(log)
        }
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
    navigator: DestinationsNavigator? = null,
    matchedContact: Contact? = null
) {
    val context   = LocalContext.current
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    val candidateName = (log.name ?: log.number).takeIf { it.isNotBlank() && it != "Unknown" }
    val isWhatsAppGroup = log.isWhatsApp && candidateName != null && (candidateName.contains("&") || candidateName.contains(","))
    val waGroupParts = remember(isWhatsAppGroup, candidateName) {
        if (isWhatsAppGroup && candidateName != null) {
            candidateName.split(Regex("[&,]")).map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()
    }
    val p1Name = waGroupParts.getOrNull(0) ?: ""
    val p2Name = waGroupParts.getOrNull(1) ?: ""
    val c1 = remember(allContacts, p1Name) {
        if (p1Name.isBlank()) null
        else {
            val d = p1Name.filter { it.isDigit() }
            val norm = if (d.length >= 7) d.takeLast(10) else null
            allContacts.firstOrNull { c ->
                (norm != null && c.phoneNumbers.any { it.filter { ch -> ch.isDigit() }.takeLast(10) == norm }) ||
                c.name.trim().equals(p1Name, ignoreCase = true)
            }
        }
    }
    val c2 = remember(allContacts, p2Name) {
        if (p2Name.isBlank()) null
        else {
            val d = p2Name.filter { it.isDigit() }
            val norm = if (d.length >= 7) d.takeLast(10) else null
            allContacts.firstOrNull { c ->
                (norm != null && c.phoneNumbers.any { it.filter { ch -> ch.isDigit() }.takeLast(10) == norm }) ||
                c.name.trim().equals(p2Name, ignoreCase = true)
            }
        }
    }

    val waSingleContact = remember(allContacts, candidateName, log.number, matchedContact, isWhatsAppGroup) {
        if (matchedContact != null) matchedContact
        else if (log.isWhatsApp && !isWhatsAppGroup && !candidateName.isNullOrBlank()) {
            val digits = log.number.filter { it.isDigit() }
            val norm = if (digits.length >= 7) digits.takeLast(10) else null
            allContacts.firstOrNull { c ->
                (norm != null && c.phoneNumbers.any { it.filter { ch -> ch.isDigit() }.takeLast(10) == norm }) ||
                c.name.trim().equals(candidateName, ignoreCase = true)
            }
        } else null
    }

    val effectiveContact = matchedContact ?: waSingleContact
    val resolvedContactName = effectiveContact?.name?.takeIf { it.isNotBlank() }
        ?: (if (log.isWhatsApp && !candidateName.isNullOrBlank()) candidateName else log.name?.takeIf { it.isNotBlank() && it != log.number && !log.isCallerIdName })
    val isContact = effectiveContact != null || (log.isWhatsApp && !candidateName.isNullOrBlank())
    val isCallerId = !isContact && log.isCallerIdName && !log.name.isNullOrBlank() && log.name != log.number
    var showMenu  by remember { mutableStateOf(false) }
    var showAddContactChoiceDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showMultiCallPicker by remember { mutableStateOf(false) }

    val prefs = koinInject<PreferenceManager>()
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = config?.isSaturatedActive ?: remember(isDark) { prefs.isSaturatedForTheme(isDark) }
    val isMissed = log.type == CallLog.Calls.MISSED_TYPE || log.type == 5 || log.types.any { it == CallLog.Calls.MISSED_TYPE || it == 5 }
    val isBlocked = log.type == CallLog.Calls.BLOCKED_TYPE || log.types.any { it == CallLog.Calls.BLOCKED_TYPE }

    val trailingContainerColor = if (isMissed || isBlocked) {
        if (isSaturatedActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.errorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }

    val trailingTint = if (isMissed || isBlocked) {
        if (isSaturatedActive) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onErrorContainer
    } else {
        if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer
    }

    val fakeCallInContextMenu: Boolean
    val showAvatars: Boolean
    val use24HourTime: Boolean
    val showTalkTime: Boolean
    val showMissedCallDuration: Boolean
    val groupCallsByLatest: Boolean
    val showTotalCallsMade: Boolean
    val hideNames: Boolean
    val hiddenIds: Set<String>
    val nameNonContactsAsUnknown: Boolean
    val showSimsSetting: Boolean
    val sim1Color: Color
    val sim2Color: Color
    val showNumbersInCallLogs: Boolean

    if (config != null) {
        fakeCallInContextMenu = config.fakeCallInContextMenu
        showAvatars = config.showAvatars
        use24HourTime = config.use24HourTime
        showTalkTime = config.showTalkTime
        showMissedCallDuration = config.showMissedCallDuration
        groupCallsByLatest = config.groupCallsByLatest
        showTotalCallsMade = config.showTotalCallsMade
        hideNames = config.hideNames
        hiddenIds = config.hiddenIds
        nameNonContactsAsUnknown = config.nameNonContactsAsUnknown
        showSimsSetting = config.showSims
        sim1Color = config.sim1Color
        sim2Color = config.sim2Color
        showNumbersInCallLogs = config.showNumbersInCallLogs
    } else {
        fakeCallInContextMenu = prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
        showAvatars = prefs.getBoolean(PreferenceManager.KEY_SHOW_AVATARS_CALLS, true)
        use24HourTime = prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false)
        showTalkTime = prefs.getBoolean(PreferenceManager.KEY_SHOW_TALK_TIME_IN_CALL_LOGS, false)
        showMissedCallDuration = prefs.getBoolean(PreferenceManager.KEY_SHOW_MISSED_CALL_DURATION, false)
        groupCallsByLatest = prefs.getBoolean(PreferenceManager.KEY_GROUP_CALLS_BY_LATEST, false)
        showTotalCallsMade = groupCallsByLatest || prefs.getBoolean(PreferenceManager.KEY_SHOW_TOTAL_CALLS_MADE, false)
        hideNames = prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false)
        hiddenIds = run {
            val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
            if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
        }
        nameNonContactsAsUnknown = prefs.getBoolean(PreferenceManager.KEY_NAME_NON_CONTACTS_AS_UNKNOWN, true)
        showSimsSetting = prefs.getBoolean(PreferenceManager.KEY_SHOW_SIMS_IN_CALL_LOGS, prefs.getShowSimsInCallLogsDefault())
        sim1Color = Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR))
        sim2Color = Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR))
        showNumbersInCallLogs = prefs.getBoolean(PreferenceManager.KEY_SHOW_NUMBERS_IN_CALL_LOGS, true)
    }

    var showFakeCallSheet by remember { mutableStateOf(false) }
    var showCallChatViaPicker by remember { mutableStateOf(false) }
    var showSaveToAlternateDialog by remember { mutableStateOf(false) }

    val isHiddenContact = remember(log.contactId, hiddenIds, hideNames) {
        hideNames && hiddenIds.isNotEmpty() && log.contactId != null && log.contactId in hiddenIds
    }
    val altMatch = remember(log.number, isContact) {
        if (!isContact && log.number.isNotBlank()) {
            com.modiprincekr.thedialer.controller.util.AlternateNumberRepository.findByNumber(context, log.number)
        } else null
    }
    val isAlt = log.isAlternate || altMatch != null
    val altName = log.alternateName ?: altMatch?.name
    val displayName = when {
        log.isMergedCall || log.isMultiCall -> {
            val name1 = (if (isContact) resolvedContactName else log.name)?.ifBlank { null } ?: log.number
            val name2 = log.secondName?.ifBlank { null } ?: log.secondNumber ?: ""
            if (name2.isNotBlank()) "$name1 & $name2" else name1
        }
        isHiddenContact -> log.number
        log.isWhatsApp && !candidateName.isNullOrBlank() -> candidateName
        isContact -> resolvedContactName ?: log.number
        !altName.isNullOrBlank() -> altName
        isCallerId -> log.name ?: log.number
        nameNonContactsAsUnknown -> "Unknown"
        else -> log.number
    }
    val avatarSourceName = when {
        log.isMergedCall -> "Conference"
        log.isMultiCall -> "Group"
        isWhatsAppGroup -> "Group"
        isHiddenContact -> log.number
        isContact -> resolvedContactName ?: log.number
        !altName.isNullOrBlank() -> altName
        isCallerId -> log.name ?: log.number
        else -> nationalNumberDigits(log.number).ifEmpty { "Unknown" }
    }

    val resolvedPhotoUri = effectiveContact?.photoUri ?: log.photoUri

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
        val showNumberOnSupportingLine = log.isMultiCall || (!isHiddenContact && ((isContact && showNumbersInCallLogs) || (!isContact && (isCallerId || nameNonContactsAsUnknown))))
        val simBadge: (@Composable () -> Unit)? = if (showSimBadge) ({
            SimSlotBadge(
                slot = log.simSlot,
                overrideColor = if (log.simSlot == 0) sim1Color else sim2Color,
                fontSize = 8.sp,
                modifier = Modifier.size(width = 14.dp, height = 16.dp)
            )
        }) else null
        val whatsAppBadge: (@Composable () -> Unit)? = if (log.isWhatsApp) ({
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (log.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (log.sourcePackage == "com.whatsapp.w4b") "Business" else "WhatsApp",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
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
        val altBadge: (@Composable () -> Unit)? = if (isAlt) ({
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "Alt",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }) else null
        val colorScheme = MaterialTheme.colorScheme
        val isAutoBw = (prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto") == "auto_bw"
        val leftSwipeKey = config?.leftSwipeKey ?: prefs.getSwipeAction("call_logs", "left")
        val rightSwipeKey = config?.rightSwipeKey ?: prefs.getSwipeAction("call_logs", "right")
        val leftSwipeAction = config?.leftSwipeAction ?: remember(leftSwipeKey, colorScheme) { SwipeActionHelper.resolveActionItem(leftSwipeKey, colorScheme) }
        val rightSwipeAction = config?.rightSwipeAction ?: remember(rightSwipeKey, colorScheme) { SwipeActionHelper.resolveActionItem(rightSwipeKey, colorScheme) }

        val handleSwipeAction: (String) -> Unit = { actionKey ->
            when (actionKey) {
                "call", "call_back" -> onButtonClick(log)
                "send_text" -> {
                    if (log.number.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${log.number}"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
                "delete", "delete_call_log" -> {
                    if (log.isWhatsApp) {
                        com.modiprincekr.thedialer.controller.util.WhatsAppCallRepository.deleteByNumberAndDates(
                            context, log.number, (log.dates + log.date).distinct()
                        )
                        onDelete?.invoke()
                        Toast.makeText(context, "Deleted from call log", Toast.LENGTH_SHORT).show()
                    } else {
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
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not delete", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                "select" -> onSelectMode?.invoke(log)
                "copy_number" -> {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("phone_number", log.number))
                    Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                }
                "view_contact" -> {
                    if (log.isMultiCall && !log.secondNumber.isNullOrBlank()) {
                        showMultiCallPicker = true
                    } else if (isContact) {
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
                    } else {
                        showAddContactChoiceDialog = true
                    }
                }
                "edit_contact" -> {
                    if (isContact && navigator != null) {
                        navigator.navigate(ContactEditScreenDestination(contactId = matchedContact?.id ?: log.contactId ?: "null"))
                    } else {
                        showAddContactChoiceDialog = true
                    }
                }
                "add_to_contacts" -> showAddContactChoiceDialog = true
                "share", "share_contact" -> {
                    val shareText = if (isContact) "${displayName}: ${log.number}" else log.number
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                }
                "call_chat_via" -> showCallChatViaPicker = true
                "fake_call" -> showFakeCallSheet = true
                "toggle_favorite" -> matchedContact?.let { contactsVM.toggleFavorite(it) }
                "block_number" -> {
                    val isNumberBlocked = BlockedNumbersManager.isBlocked(context, prefs, log.number)
                    BlockedNumbersManager.toggle(context, prefs, log.number)
                    Toast.makeText(
                        context,
                        if (isNumberBlocked) "Number unblocked" else "Number blocked",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                "search_truecaller" -> {
                    val isLaunched = com.modiprincekr.thedialer.controller.util.openTruecaller(context, log.number)
                    if (!isLaunched) {
                        Toast.makeText(context, "Truecaller is not installed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        SwipeableItemContainer(
            leftAction = leftSwipeAction,
            rightAction = rightSwipeAction,
            onSwipeLeft = { handleSwipeAction(leftSwipeKey) },
            onSwipeRight = { handleSwipeAction(rightSwipeKey) },
            enabled = !selectionMode,
            shape = RoundedCornerShape(16.dp)
        ) {
            RivoListItem(
                headline = buildString {
                    append(displayName)
                    if (log.count > 1 && !showTotalCallsMade && !log.isMultiCall) append(" (${log.count})")
                },
                headlineMaxLines = 2,
                supporting = when {
                    log.isMergedCall -> {
                        val num1 = log.number
                        val num2 = log.secondNumber ?: ""
                        if (num2.isNotBlank()) "$num1 • $num2" else num1
                    }
                    log.isMultiCall -> {
                        val num1 = log.number
                        val num2 = log.secondNumber ?: ""
                        if (num2.isNotBlank()) "1 active, 1 on hold • $num1 & $num2" else "1 active, 1 on hold"
                    }
                    log.isWhatsApp -> {
                        val realNumber = when {
                            log.number.isNotBlank() && log.number.any { it.isDigit() } && !log.number.contains("@") && !log.number.equals(displayName, ignoreCase = true) -> log.number
                            effectiveContact != null && effectiveContact.phoneNumbers.isNotEmpty() -> effectiveContact.phoneNumbers.first()
                            else -> null
                        }
                        when {
                            realNumber != null -> if (log.isVideo) "Video Call • $realNumber" else realNumber
                            log.isVideo -> "Video Call"
                            else -> "Voice Call"
                        }
                    }
                    showNumberOnSupportingLine -> log.number
                    else -> null
                },
                avatarName  = if (showAvatars && !isWhatsAppGroup && (!log.isMultiCall || log.secondNumber.isNullOrBlank())) avatarSourceName else null,
                avatarForcePersonIcon = !isContact && !log.isMultiCall && !isAlt && !log.isWhatsApp,
                photoUri    = if (showAvatars && !isWhatsAppGroup && !log.isMultiCall) resolvedPhotoUri else null,
                customAvatar = if (showAvatars && ((log.isMultiCall && !log.secondNumber.isNullOrBlank()) || (isWhatsAppGroup && waGroupParts.size >= 2))) {
                    {
                        if (isWhatsAppGroup && waGroupParts.size >= 2) {
                            DualSplitAvatar(
                                name1 = c1?.name ?: p1Name,
                                number1 = c1?.phoneNumbers?.firstOrNull() ?: p1Name,
                                photoUri1 = c1?.photoUri,
                                name2 = c2?.name ?: p2Name,
                                number2 = c2?.phoneNumbers?.firstOrNull() ?: p2Name,
                                photoUri2 = c2?.photoUri,
                                size = 48.dp
                            )
                        } else {
                            val p1 = (if (isContact) resolvedContactName else log.name)?.ifBlank { null } ?: log.number
                            val p2 = log.secondName?.ifBlank { null } ?: log.secondNumber ?: ""
                            DualSplitAvatar(
                                name1 = p1,
                                number1 = log.number,
                                photoUri1 = resolvedPhotoUri,
                                name2 = p2,
                                number2 = log.secondNumber ?: "",
                                photoUri2 = log.secondPhotoUri,
                                size = 48.dp
                            )
                        }
                    }
                } else null,
                avatarObeySolidIcons = false,
                headlineStartContent = if (!log.isWhatsApp && !showNumberOnSupportingLine) simBadge else null,
                headlineEndContent = if (altBadge != null || totalCallsBadge != null) ({
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        altBadge?.invoke()
                        totalCallsBadge?.invoke()
                    }
                }) else null,
                supportingStartContent = if (log.isWhatsApp) whatsAppBadge else if (showNumberOnSupportingLine) simBadge else null,
                trailingText = formatTimeOnly(log.date, use24HourTime),
                trailingTextColor = if (isMissed || isBlocked) MaterialTheme.colorScheme.error else null,
                trailingSubText = if (groupCallsByLatest) {
                    val dateText = formatCallLogDate(log.date)
                    if (isBlocked && log.duration > 0) {
                        "$dateText • ${log.duration}s"
                    } else if (isMissed && showMissedCallDuration && log.duration > 0) {
                        "$dateText • ${log.duration}s"
                    } else if (showTalkTime && !isMissed) {
                        val durationText = formatDuration(log.duration)
                        "$dateText • $durationText"
                    } else {
                        dateText
                    }
                } else {
                    if (isBlocked) {
                        if (log.duration > 0) "${log.duration}s" else null
                    } else if (isMissed) {
                        if (showMissedCallDuration && log.duration > 0) "${log.duration}s" else null
                    } else if (showTalkTime) {
                        formatDuration(log.duration)
                    } else null
                },
                trailingSubTextColor = if (groupCallsByLatest) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                } else {
                    if (isMissed || isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                trailingIcon = when {
                    log.isMergedCall -> Icons.Default.CallMerge
                    log.isMultiCall -> Icons.Default.PhonePaused
                    log.type == CallLog.Calls.MISSED_TYPE || log.type == 5 || log.types.any { it == CallLog.Calls.MISSED_TYPE || it == 5 }   -> Icons.AutoMirrored.Filled.CallMissed
                    log.type == CallLog.Calls.BLOCKED_TYPE || log.types.any { it == CallLog.Calls.BLOCKED_TYPE }  -> Icons.Default.Block
                    log.isWhatsApp && log.isVideo           -> Icons.Default.Videocam
                    log.type == CallLog.Calls.INCOMING_TYPE || log.type == 7 || log.types.any { it == CallLog.Calls.INCOMING_TYPE || it == 7 } -> Icons.AutoMirrored.Filled.CallReceived
                    log.type == CallLog.Calls.OUTGOING_TYPE || log.types.any { it == CallLog.Calls.OUTGOING_TYPE } -> Icons.AutoMirrored.Filled.CallMade
                    else                        -> Icons.Default.Call
                },
                trailingIconTint = trailingTint,
                trailingIconContainerColor = trailingContainerColor,
                onAvatarClick = if (showAvatars && onAvatarClick != null) ({
                    if (log.isMultiCall && !log.secondNumber.isNullOrBlank()) {
                        showMultiCallPicker = true
                    } else {
                        onAvatarClick(log.copy(contactId = matchedContact?.id ?: log.contactId, photoUri = resolvedPhotoUri))
                    }
                }) else null,
                onLongClick = {
                    if (selectionMode) onSelectToggle?.invoke(log)
                    else showMenu = true
                },
                isMenuOpen  = showMenu && !selectionMode,
                onClick     = {
                    if (selectionMode) onSelectToggle?.invoke(log)
                    else if (log.isMultiCall && !log.secondNumber.isNullOrBlank()) {
                        showMultiCallPicker = true
                    } else onTileClick(log)
                }
            )
        }

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
                "save_to_alternate",
                "share", "call_chat_via", "send_text", "search_truecaller", "move_contact", "toggle_favorite",
                "block_number", "fake_call", "delete_call_log"
            )
            val callLogContextMenuKeys = remember(settingsVer, isContact, matchedContact, fakeCallInContextMenu, hasAnySocialApp, hasTruecaller) {
                com.modiprincekr.thedialer.controller.util.ContextMenuPrefs.resolvedKeys(
                    prefs,
                    com.modiprincekr.thedialer.controller.util.ContextMenuPrefs.SECTION_CALL_LOGS,
                    defaultCallLogOrder
                ).filter { key ->
                    when (key) {
                        "view_contact"      -> isContact
                        "edit_contact"      -> isContact
                        "add_to_contacts"   -> !isContact
                        "save_to_alternate" -> !isContact && log.number.isNotBlank()
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
                            iconTint = if (isAutoBw) colorScheme.primary else Color(0xFF9C27B0),
                            onClick  = {
                                showMenu = false
                                onSelectMode?.invoke(log)
                            }
                        )
                        "call_back" -> {
                            if (log.isWhatsApp) {
                                RivoDropdownMenuItem(
                                    text     = if (log.isVideo) "WhatsApp Video call" else "WhatsApp Voice call",
                                    icon     = if (log.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                    iconTint = Color(0xFF4CAF50),
                                    onClick  = { showMenu = false; onButtonClick(log) }
                                )
                            } else {
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
                                val account1 = getPhoneAccountForSimSlot(context, accounts, 0)
                                val account2 = getPhoneAccountForSimSlot(context, accounts, 1)

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
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        SimSlotBadge(slot = 0)
                                                    }
                                                }
                                                Surface(
                                                    onClick = {
                                                        showMenu = false
                                                        makeCall(context, log.number, account2)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        SimSlotBadge(slot = 1)
                                                    }
                                                }
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                        "view_contact" -> {
                            if (isContact || log.isMultiCall) {
                                RivoDropdownMenuItem(
                                    text     = if (log.isMultiCall) "View contacts" else "View contact",
                                    icon     = if (log.isMultiCall) Icons.Default.Group else Icons.Default.Person,
                                    iconTint = Color(0xFF2196F3),
                                    onClick  = {
                                        showMenu = false
                                        if (log.isMultiCall && !log.secondNumber.isNullOrBlank()) {
                                            showMultiCallPicker = true
                                        } else if (navigator != null) {
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
                        "save_to_alternate" -> {
                            if (!isContact) {
                                RivoDropdownMenuItem(
                                    text     = "Save to Alternate",
                                    icon     = Icons.Outlined.ContactPhone,
                                    iconTint = Color(0xFF00897B),
                                    onClick  = {
                                        showMenu = false
                                        showSaveToAlternateDialog = true
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
                                        val isLaunched: Boolean = com.modiprincekr.thedialer.controller.util.openTruecaller(context, log.number)
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
                                    iconTint = if (isAutoBw) colorScheme.primary else if (isFav) Color(0xFFF44336) else Color(0xFFE91E63),
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
                                if (log.isWhatsApp) {
                                    com.modiprincekr.thedialer.controller.util.WhatsAppCallRepository.deleteByNumberAndDates(
                                        context, log.number, (log.dates + log.date).distinct()
                                    )
                                    onDelete?.invoke()
                                    Toast.makeText(context, "Deleted from call log", Toast.LENGTH_SHORT).show()
                                } else {
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
        contactId = matchedContact?.id,
        phoneNumbers = callChatViaNumbers,
        showPicker = showCallChatViaPicker,
        onPickerDismiss = { showCallChatViaPicker = false },
        showGoogleMeet = true
    )

    if (showMultiCallPicker && log.isMultiCall && !log.secondNumber.isNullOrBlank()) {
        MultiCallContactSelectionDialog(
            name1 = resolvedContactName ?: log.name,
            number1 = log.number,
            contactId1 = matchedContact?.id ?: log.contactId,
            photoUri1 = resolvedPhotoUri,
            name2 = log.secondName,
            number2 = log.secondNumber ?: "",
            contactId2 = log.secondContactId,
            photoUri2 = log.secondPhotoUri,
            isMergedCall = log.isMergedCall,
            onSelectContact = { selectedId, selectedNum ->
                showMultiCallPicker = false
                if (navigator != null) {
                    navigator.navigate(
                        ContactDetailsScreenDestination(
                            contactId = selectedId ?: "null",
                            phoneNumber = selectedNum
                        )
                    )
                } else {
                    onTileClick(log.copy(number = selectedNum, contactId = selectedId))
                }
            },
            onDismiss = { showMultiCallPicker = false }
        )
    }

    if (showSaveToAlternateDialog) {
        val existingAlt = altMatch ?: remember(log.number) {
            com.modiprincekr.thedialer.controller.util.AlternateNumberRepository.findByNumber(context, log.number)
        }
        com.modiprincekr.thedialer.view.screen.AddEditAlternateNumberDialog(
            initialItem = existingAlt,
            initialNumber = log.number,
            onDismiss = { showSaveToAlternateDialog = false },
            onSave = { name, number, tag, note ->
                val toSave = existingAlt?.copy(
                    name = name,
                    phoneNumber = number,
                    categoryTag = tag,
                    note = note
                ) ?: com.modiprincekr.thedialer.modal.data.AlternateNumber(
                    name = name,
                    phoneNumber = number,
                    categoryTag = tag,
                    note = note
                )
                com.modiprincekr.thedialer.controller.util.AlternateNumberRepository.save(context, toSave)
                showSaveToAlternateDialog = false
            }
        )
    }
}
