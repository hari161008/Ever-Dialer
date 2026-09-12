package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coolappstore.everdialer.by.svhp.modal.data.getPhoneTypeLabel
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccountInfo
import com.coolappstore.everdialer.by.svhp.modal.data.ContactPhone
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactPfpCustomizationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CustomBackgroundPickerScreenDestination
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.controller.util.QrCodeUtils
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallWithSimPreference
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallWithContactSimPreference
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.WHATSAPP_PACKAGES
import com.coolappstore.everdialer.by.svhp.controller.util.isAnyPackageInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isWhatsAppInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isWhatsAppBusinessInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTelegramInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isGoogleMeetInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTruecallerInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.getWhatsAppIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getWhatsAppBusinessIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTelegramIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getGoogleMeetIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTruecallerIcon
import com.coolappstore.everdialer.by.svhp.controller.util.openWhatsAppChat
import com.coolappstore.everdialer.by.svhp.controller.util.openWhatsAppBusinessChat
import com.coolappstore.everdialer.by.svhp.controller.util.openTelegramChat
import com.coolappstore.everdialer.by.svhp.controller.util.openTruecaller
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppBusinessVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppBusinessVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startGoogleMeetVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startGoogleMeetVideoCall
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.koinInject
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.controller.util.deduplicatePhoneNumbers
import com.coolappstore.everdialer.by.svhp.controller.util.ContactRingtoneUtils
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import androidx.compose.material.icons.outlined.Block

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun ContactDetailsScreen(
    contactId: String? = null,
    phoneNumber: String? = null,
    navController: NavController,
    navigator: DestinationsNavigator
) {
    val contactsViewModel: ContactsViewModel = koinActivityViewModel()
    val callLogViewModel: CallLogViewModel = koinActivityViewModel()

    val contacts by contactsViewModel.allContacts.collectAsState()
    val allLogs by callLogViewModel.allCallLogs.collectAsState()

    val contact = remember(contactId, phoneNumber, contacts) {
        if (contactId != null && contactId != "null") contacts.find { it.id == contactId }
        else if (phoneNumber != null) {
            // numbersLikelyMatch: exact digit match always counts, and a suffix match (needed so
            // a contact saved with a country code, e.g. "+917875551234", still resolves from the
            // plain call-log number "7875551234", including across a contact's other saved
            // numbers) is only trusted when both numbers are long enough to be real phone
            // numbers. A previous `.contains()` check here matched any saved number that merely
            // contained the dialed digits as a substring anywhere — e.g. dialing the short code
            // "787" or "875" would wrongly match a saved contact like "7875XXXXXX".
            contacts.find { c ->
                c.phoneNumbers.any { n -> numbersLikelyMatch(phoneNumber, n) }
            }
        }
        else null
    }

    val displayPhone = phoneNumber ?: contact?.phoneNumbers?.firstOrNull() ?: "Unknown"
    val displayName = contact?.name ?: phoneNumber ?: "Unknown"
    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }
    val prefs = koinInject<PreferenceManager>()
    val simPref = remember { prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault()) }

    var showSimPicker by remember { mutableStateOf(false) }
    var showNumberPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    // "Add to Home Screen": pick-a-number step (only shown when the contact has 2+ numbers),
    // then the open-info-vs-call-directly step, before actually pinning the shortcut.
    var showShortcutNumberPicker by remember { mutableStateOf(false) }
    var showShortcutActionPicker by remember { mutableStateOf(false) }
    var pendingShortcutNumber by remember { mutableStateOf<String?>(null) }
    var showDescriptionEditor by remember { mutableStateOf(false) }
    var editingTargetAccount by remember { mutableStateOf<ContactAccountInfo?>(null) }
    var editingInitialDescription by remember { mutableStateOf("") }
    var contactAccounts by remember { mutableStateOf<List<ContactAccountInfo>>(emptyList()) }
    var accountsRefreshTrigger by remember { mutableStateOf(0) }
    var selectedVisibilityAccount by remember { mutableStateOf<ContactAccountInfo?>(null) }
    var showVisibilityDialog by remember { mutableStateOf(false) }
    var showDescriptionOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(contact?.id, accountsRefreshTrigger) {
        val cid = contact?.id
        if (cid != null && cid != "0" && cid != "null") {
            contactAccounts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                contactsViewModel.getContactAccounts(cid)
            }
        } else {
            contactAccounts = emptyList()
        }
    }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showChooseSimDialog by remember { mutableStateOf(false) }
    // Contact Info → "Ringtone" — per-contact custom ringtone (ContactsContract CUSTOM_RINGTONE),
    // same mechanism the system Contacts app and Telecom's incoming-call ringer use. Bumped after
    // the ringtone picker returns so the current-value query below re-runs.
    var ringtoneVersion by remember { mutableStateOf(0) }
    var showChooseDefaultNumberDialog by remember { mutableStateOf(false) }
    // "chat_app" for the Social card's WhatsApp/Telegram quick-action popup: null when hidden,
    // otherwise "whatsapp" or "telegram" to say which app's Chat/Voice Call/Video Call sheet to show.
    var showAppQuickActions by remember { mutableStateOf<String?>(null) }
    // Respect Settings → Appearance → "Context Menu Elements" (Contacts section) customization
    // so the actions shown here always match what's configured for the contact's context menu.
    val settingsVer by prefs.settingsChanged.collectAsState()
    val sim1Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM1_COLOR, PreferenceManager.DEFAULT_SIM1_COLOR)) }
    val sim2Color = remember(settingsVer) { Color(prefs.getInt(PreferenceManager.KEY_SIM2_COLOR, PreferenceManager.DEFAULT_SIM2_COLOR)) }
    val hasTwoSims = remember(settingsVer) {
        prefs.getActiveSimCount() >= 2 || run {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            try { (tm?.callCapablePhoneAccounts?.size ?: 0) >= 2 } catch (_: Throwable) { false }
        }
    }
    var showCallLongPressMenu by remember { mutableStateOf(false) }
    var pendingSimSlotToCall by remember { mutableStateOf<Int?>(null) }
    val hideDuplicateNumbers = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_HIDE_DUPLICATE_NUMBERS_IN_CONTACT, true)
    }
    val contactSimKey = contact?.id ?: phoneNumber ?: displayPhone
    val contactSimChoice = remember(settingsVer, contactSimKey) { prefs.getContactSimChoice(contactSimKey) }
    // Contact Info → "Choose Default Number" — per-contact override of which saved number the
    // header call button dials directly, for contacts saved with 2+ numbers (skips the number
    // picker once set). Same keying as contactSimKey, so it travels with the same contact.
    val contactDefaultNumber = remember(settingsVer, contactSimKey) { prefs.getContactDefaultNumber(contactSimKey) }
        .takeIf { number -> contact != null && number != null && contact.phoneNumbers.contains(number) }

    val contactPhoneNumbers = remember(contact, hideDuplicateNumbers, contactDefaultNumber) {
        val raw = contact?.phoneNumbers?.filter { it.isNotBlank() } ?: emptyList()
        val list = if (hideDuplicateNumbers) deduplicatePhoneNumbers(raw) else raw
        if (contactDefaultNumber != null && contactDefaultNumber in list) {
            listOf(contactDefaultNumber) + (list - contactDefaultNumber)
        } else {
            list
        }
    }

    val contactInfoOrder = remember(settingsVer) {
        prefs.getContactInfoOrder()
    }
    val showQuickActions = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_QUICK_ACTIONS, true) }
    val showContactInfo = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CONTACT_INFO, true) }
    val showSocial = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SOCIAL, true) }
    val showDescription = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_DESCRIPTION, true) }
    val showNotes = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_NOTES, true) }
    val showEvents = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_EVENTS, true) }
    val showRecentActivity = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RECENT_ACTIVITY, true) }
    val showChooseSim = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_SIM, true) }
    val showCallingBackgrounds = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CALLING_BACKGROUNDS, true) }
    val showAdvancedPfp = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_ADVANCED_PFP, true) }
    val showRingtone = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_RINGTONE, true) }
    val showChooseDefaultNumber = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_CHOOSE_DEFAULT_NUMBER, true) }
    val showSavedIn = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CONTACT_INFO_SHOW_SAVED_IN, true) }

    // All this contact's saved numbers, so the Social card can offer a choice when there's more
    // than one (e.g. one saved with a country code, one without) instead of always defaulting to
    // the first saved number — which could be one that isn't actually registered on that app.
    val socialNumbers = remember(contactPhoneNumbers, phoneNumber) {
        contactPhoneNumbers.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(phoneNumber?.takeIf { it.isNotBlank() })
    }
    // App tapped in the Social card but still awaiting a number pick (only used when the contact
    // has 2+ numbers); null once a number has been picked (or there was only one to begin with).
    var pendingSocialApp by remember { mutableStateOf<String?>(null) }
    var socialSelectedNumber by remember { mutableStateOf<String?>(null) }

    val whatsAppInstalled = remember(context) { isWhatsAppInstalled(context) }
    val whatsAppBusinessInstalled = remember(context) { isWhatsAppBusinessInstalled(context) }
    val telegramInstalled = remember(context) { isTelegramInstalled(context) }
    val meetInstalled = remember(context) { isGoogleMeetInstalled(context) }
    val truecallerInstalled = remember(context) { isTruecallerInstalled(context) }
    val hasAnySocialApp = whatsAppInstalled || whatsAppBusinessInstalled || telegramInstalled || meetInstalled || truecallerInstalled

    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
    val contactInfoActionKeys = remember(settingsVer) {
        com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.resolvedKeys(
            prefs,
            com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.SECTION_CONTACTS,
            listOf("select", "view_contact", "edit_contact", "copy_number", "share_contact", "move_contact", "toggle_favorite", "fake_call", "delete_contact")
        ).filter { it in setOf("copy_number", "share_contact", "move_contact", "delete_contact") }
    }

    val contactLogs = remember(contact, phoneNumber, allLogs) {
        // Same numbersLikelyMatch rule as the contact lookup above — plain `.contains()` would
        // pull in call log entries for unrelated short numbers/codes that merely appear as a
        // substring of this contact's/number's digits.
        allLogs.filter { log ->
            (contact != null && (log.contactId == contact.id ||
                contact.phoneNumbers.any { n -> numbersLikelyMatch(log.number, n) })) ||
            (phoneNumber != null && numbersLikelyMatch(log.number, phoneNumber))
        }
    }

    val isFavorite = contact?.isFavorite ?: false
    val isContactBlocked = remember(settingsVer, contact, phoneNumber) {
        if (contact != null) {
            contact.phoneNumbers.any { BlockedNumbersManager.isBlocked(context, prefs, it) }
        } else if (phoneNumber != null && phoneNumber != "Unknown") {
            BlockedNumbersManager.isBlocked(context, prefs, phoneNumber)
        } else false
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedNumberForMenu by remember { mutableStateOf<String?>(null) }
    var selectedEmailForMenu by remember { mutableStateOf<String?>(null) }
    var selectedAddressForMenu by remember { mutableStateOf<String?>(null) }

    var editingNumberValue by remember { mutableStateOf<String?>(null) }
    var originalNumberValue by remember { mutableStateOf<String?>(null) }
    var editingEmailValue by remember { mutableStateOf<String?>(null) }
    var originalEmailValue by remember { mutableStateOf<String?>(null) }
    var editingAddressValue by remember { mutableStateOf<String?>(null) }
    var originalAddressValue by remember { mutableStateOf<String?>(null) }

    // Contact Info → "Ringtone" — per-contact custom ringtone, read straight from Contacts
    // provider so it always reflects reality (including changes made from the system Contacts
    // app), re-queried whenever ringtoneVersion is bumped after the picker returns.
    val contactRingtoneUri = remember(contact?.id, ringtoneVersion) {
        contact?.id?.let { ContactRingtoneUtils.getCustomRingtoneUri(context, it) }
    }
    val contactRingtoneLabel = remember(contactRingtoneUri) { ContactRingtoneUtils.ringtoneLabel(context, contactRingtoneUri) }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val hasPickedExtra = result.data?.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) == true
            @Suppress("DEPRECATION")
            val pickedUri = if (hasPickedExtra) result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) else null
            val toStore: Uri? = when {
                !hasPickedExtra -> Uri.EMPTY // "Silent" chosen → explicitly silent
                pickedUri == null || pickedUri == defaultUri -> null // "Default" chosen → clear custom ringtone
                else -> pickedUri
            }
            contact?.let { c ->
                ContactRingtoneUtils.setCustomRingtoneUri(context, c.id, toStore)
                ringtoneVersion++
            }
        }
    }
    fun openRingtonePicker() {
        val id = contact?.id ?: return
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val existingUri = when (contactRingtoneUri) {
            null -> defaultUri // nothing custom set yet → highlight "Default" instead of "Silent"
            Uri.EMPTY -> Uri.EMPTY // explicitly silent → highlight "Silent"
            else -> contactRingtoneUri
        }
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
        }
        ringtonePickerLauncher.launch(intent)
    }

    fun launchSocialApp(app: String, number: String) {
        if (app == "truecaller") {
            val opened = openTruecaller(context, number)
            if (!opened) android.widget.Toast.makeText(context, "Truecaller isn't installed", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            socialSelectedNumber = number
            showAppQuickActions = app
        }
    }

    fun chooseSocialApp(app: String) {
        if (socialNumbers.isEmpty()) return
        val default = contactDefaultNumber?.takeIf { it in socialNumbers }
        if (default != null) {
            launchSocialApp(app, default)
        } else if (socialNumbers.size > 1) {
            pendingSocialApp = app
        } else {
            launchSocialApp(app, socialNumbers.first())
        }
    }

    // Most recent SIM slot used on a call with this contact, for the "last used SIM for this
    // contact" option — derived straight from this contact's call log history.
    val recentSimSlotForContact = remember(contactLogs) {
        contactLogs.maxByOrNull { it.date }?.simSlot?.takeIf { it >= 0 }
    }

    val initiateCall = { number: String ->
        placeCallWithContactSimPreference(
            context, number, contactSimChoice, simPref, recentSimSlotForContact
        ) {
            pendingNumber = number; showSimPicker = true
        }
    }

    val callWithSimSlot: (Int) -> Unit = { slot ->
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val accounts = try { tm?.callCapablePhoneAccounts } catch (_: Throwable) { null } ?: emptyList()
        val targetAccount = if (accounts.size > slot) accounts[slot] else accounts.firstOrNull()

        val targetNumber = when {
            contactDefaultNumber != null -> contactDefaultNumber
            contact != null && contactPhoneNumbers.size == 1 -> contactPhoneNumbers.first()
            contact != null && contactPhoneNumbers.size > 1 -> null
            displayPhone != "Unknown" -> displayPhone
            else -> null
        }
        if (targetNumber != null) {
            makeCall(context, targetNumber, targetAccount)
        } else if (contact != null && contactPhoneNumbers.size > 1) {
            pendingSimSlotToCall = slot
        }
    }

    if (pendingSimSlotToCall != null && contact != null) {
        NumberPickerDialog(
            numbers = contactPhoneNumbers,
            onDismissRequest = { pendingSimSlotToCall = null },
            onNumberSelected = { num ->
                val slot = pendingSimSlotToCall!!
                pendingSimSlotToCall = null
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val accounts = try { tm?.callCapablePhoneAccounts } catch (_: Throwable) { null } ?: emptyList()
                val targetAccount = if (accounts.size > slot) accounts[slot] else accounts.firstOrNull()
                makeCall(context, num, targetAccount)
            }
        )
    }

    if (showNumberPicker && contact != null) {
        NumberPickerDialog(numbers = contactPhoneNumbers, onDismissRequest = { showNumberPicker = false }, onNumberSelected = { showNumberPicker = false; initiateCall(it) })
    }
    if (showSimPicker && pendingNumber != null) {
        SimPickerDialog(onDismissRequest = { showSimPicker = false }, onSimSelected = { handle -> makeCall(context, pendingNumber!!, handle); showSimPicker = false })
    }
    if (showChooseSimDialog) {
        ChooseSimDialog(
            currentChoice = contactSimChoice,
            onSelect = { choice ->
                prefs.setContactSimChoice(contactSimKey, choice)
                showChooseSimDialog = false
            },
            onDismiss = { showChooseSimDialog = false }
        )
    }
    if (showChooseDefaultNumberDialog && contact != null) {
        ChooseDefaultNumberDialog(
            numbers = contactPhoneNumbers,
            currentChoice = contactDefaultNumber,
            onSelect = { number ->
                prefs.setContactDefaultNumber(contactSimKey, number)
                showChooseDefaultNumberDialog = false
            },
            onDismiss = { showChooseDefaultNumberDialog = false }
        )
    }
    if (pendingSocialApp != null) {
        val app = pendingSocialApp!!
        NumberPickerDialog(
            numbers = socialNumbers,
            onDismissRequest = { pendingSocialApp = null },
            onNumberSelected = { number ->
                pendingSocialApp = null
                launchSocialApp(app, number)
            }
        )
    }
    if (showAppQuickActions != null && socialSelectedNumber != null) {
        val app = showAppQuickActions!!
        val socialPhone = socialSelectedNumber!!
        val appLabel = when (app) {
            "whatsapp" -> "WhatsApp"
            "whatsapp_business" -> "WhatsApp Business"
            "telegram" -> "Telegram"
            else -> "Google Meet"
        }
        AppQuickActionsDialog(
            appName = appLabel,
            onChat = if (app == "googlemeet") null else {
                {
                    showAppQuickActions = null
                    val opened = when (app) {
                        "whatsapp" -> openWhatsAppChat(context, socialPhone)
                        "whatsapp_business" -> openWhatsAppBusinessChat(context, socialPhone)
                        else -> openTelegramChat(context, socialPhone)
                    }
                    if (!opened) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onVoiceCall = {
                showAppQuickActions = null
                val started = when (app) {
                    "whatsapp" -> startWhatsAppVoiceCall(context, socialPhone)
                    "whatsapp_business" -> startWhatsAppBusinessVoiceCall(context, socialPhone)
                    "telegram" -> startTelegramVoiceCall(context, socialPhone)
                    else -> startGoogleMeetVoiceCall(context, socialPhone)
                }
                if (!started) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onVideoCall = {
                showAppQuickActions = null
                val started = when (app) {
                    "whatsapp" -> startWhatsAppVideoCall(context, socialPhone)
                    "whatsapp_business" -> startWhatsAppBusinessVideoCall(context, socialPhone)
                    "telegram" -> startTelegramVideoCall(context, socialPhone)
                    else -> startGoogleMeetVideoCall(context, socialPhone)
                }
                if (!started) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAppQuickActions = null }
        )
    }
    if (showQrDialog) {
        QrCodeDialog(name = displayName, phone = displayPhone, email = contact?.emails?.firstOrNull(), onDismiss = { showQrDialog = false })
    }
    val shortcutNumbers = remember(contactPhoneNumbers, phoneNumber) {
        contactPhoneNumbers.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(phoneNumber?.takeIf { it.isNotBlank() && it != "Unknown" })
    }
    if (showShortcutNumberPicker) {
        NumberPickerDialog(
            numbers = shortcutNumbers,
            onDismissRequest = { showShortcutNumberPicker = false },
            onNumberSelected = { number ->
                showShortcutNumberPicker = false
                pendingShortcutNumber = number
                showShortcutActionPicker = true
            }
        )
    }
    if (showShortcutActionPicker && pendingShortcutNumber != null) {
        val shortcutNumber = pendingShortcutNumber!!
        val shortcutKeyId = contact?.id ?: shortcutNumber
        ShortcutActionDialog(
            onOpenContactInfo = {
                showShortcutActionPicker = false
                if (contact != null) {
                    com.coolappstore.everdialer.by.svhp.controller.util.ContactShortcutUtils.pinOpenContactShortcut(
                        context, contact.id, displayName, contact.photoUri
                    )
                } else {
                    android.widget.Toast.makeText(context, "Save this number as a contact first", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onCallDirectly = {
                showShortcutActionPicker = false
                com.coolappstore.everdialer.by.svhp.controller.util.ContactShortcutUtils.pinCallShortcut(
                    context, shortcutKeyId, displayName, shortcutNumber, contact?.photoUri
                )
            },
            onDismiss = { showShortcutActionPicker = false }
        )
    }
    if (showDescriptionEditor && contact != null) {
        DescriptionEditorDialog(
            contactName = displayName,
            initialDescription = editingInitialDescription,
            accounts = contactAccounts,
            initialSelectedAccount = editingTargetAccount,
            onSave = { newNote, targetAccount, updateAll ->
                showDescriptionEditor = false
                contactsViewModel.updateContactNote(
                    contactId = contact.id,
                    note = newNote.ifBlank { null },
                    targetRawContactId = targetAccount?.rawContactId,
                    updateAllAccounts = updateAll
                )
                accountsRefreshTrigger++
            },
            onDelete = { targetAccount, updateAll ->
                showDescriptionEditor = false
                contactsViewModel.updateContactNote(
                    contactId = contact.id,
                    note = null,
                    targetRawContactId = targetAccount?.rawContactId,
                    updateAllAccounts = updateAll
                )
                accountsRefreshTrigger++
            },
            onDismiss = { showDescriptionEditor = false }
        )
    }
    if (showVisibilityDialog) {
        Dialog(onDismissRequest = { showVisibilityDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Visibility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )

                    val distinctVisibilityAccounts = remember(contactAccounts) {
                        contactAccounts.distinctBy { (it.accountType ?: "") to (it.accountName ?: "") }
                    }

                    if (distinctVisibilityAccounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVisibilityAccount = null
                                    showVisibilityDialog = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "All locations (${distinctVisibilityAccounts.size})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Show across all accounts by default (Recommended)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedVisibilityAccount == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    distinctVisibilityAccounts.forEach { acc ->
                        val isSelected = selectedVisibilityAccount?.let {
                            it.rawContactId == acc.rawContactId ||
                            ((it.accountType ?: "") == (acc.accountType ?: "") && (it.accountName ?: "") == (acc.accountName ?: ""))
                        } == true
                        val icon: androidx.compose.ui.graphics.vector.ImageVector = when {
                            acc.isSim -> Icons.Default.SimCard
                            acc.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                            acc.accountType != null -> Icons.Default.Sync
                            else -> Icons.Default.PhoneAndroid
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVisibilityAccount = acc
                                    showVisibilityDialog = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(acc.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                if (!acc.accountName.isNullOrBlank()) {
                                    Text(
                                        acc.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    if (showNoteEditor) {
        NoteEditorDialog(contactName = displayName, phoneNumber = displayPhone, onDismiss = { showNoteEditor = false })
    }
    if (showMoveDialog && contact != null) {
        val moveTargets = remember { contactsViewModel.getSaveTargets() }
        MoveContactDialog(
            contactName = displayName,
            targets = moveTargets,
            onSelect = { target ->
                showMoveDialog = false
                contactsViewModel.moveContact(contact, target) { success ->
                    android.widget.Toast.makeText(
                        context,
                        if (success) "Moved to ${target.label}" else "Couldn't move contact",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showMoveDialog = false }
        )
    }
    if (showDeleteConfirm && contact != null) {
        DeleteContactDialog(
            contactName = displayName,
            contactId = contact.id,
            contactsViewModel = contactsViewModel,
            onDeleted = {
                showDeleteConfirm = false
                navigator.navigateUp()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
    if (showBlockConfirm) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
            AlertDialog(
                onDismissRequest = { showBlockConfirm = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = if (isContactBlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isContactBlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = if (isContactBlocked) "Unblock $displayName?" else "Block $displayName?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = if (isContactBlocked) "You will start receiving incoming calls and messages from this contact again."
                        else "You will no longer receive incoming calls or messages from this contact. Calls from blocked numbers will be declined automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBlockConfirm = false
                            val numbersToToggle = if (contact != null) contact.phoneNumbers else listOfNotNull(phoneNumber).filter { it != "Unknown" }
                            numbersToToggle.forEach { num ->
                                if (isContactBlocked) {
                                    BlockedNumbersManager.unblock(context, prefs, num)
                                } else {
                                    BlockedNumbersManager.block(context, prefs, num)
                                }
                            }
                        },
                        shape = RoundedCornerShape(100),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isContactBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            contentColor = if (isContactBlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(if (isContactBlocked) "Unblock" else "Block", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    FilledTonalButton(
                        onClick = { showBlockConfirm = false },
                        shape = RoundedCornerShape(100),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        // Background image layer sits behind the whole column so it shows through
        // the transparent header instead of a solid banner.
        Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
            AsyncImage(model = contact?.photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(50.dp), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface))))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { navigator.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (shortcutNumbers.isEmpty()) {
                                        android.widget.Toast.makeText(context, "No phone number to add", android.widget.Toast.LENGTH_SHORT).show()
                                    } else if (shortcutNumbers.size > 1) {
                                        showShortcutNumberPicker = true
                                    } else {
                                        pendingShortcutNumber = shortcutNumbers.first()
                                        showShortcutActionPicker = true
                                    }
                                }) { Icon(Icons.Default.AddToHomeScreen, "Add to Home Screen") }
                                IconButton(onClick = { showQrDialog = true }) { Icon(Icons.Outlined.QrCode2, "QR Code") }
                                if (contact != null) {
                                    IconButton(onClick = { contactsViewModel.toggleFavorite(contact) }) {
                                        Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (isFavorite) Color.Red else LocalContentColor.current)
                                    }
                                    IconButton(onClick = { showBlockConfirm = true }) {
                                        Icon(
                                            if (isContactBlocked) Icons.Default.Block else Icons.Outlined.Block,
                                            contentDescription = if (isContactBlocked) "Unblock" else "Block",
                                            tint = if (isContactBlocked) MaterialTheme.colorScheme.error else LocalContentColor.current
                                        )
                                    }
                                    IconButton(onClick = {
                                        navigator.navigate(ContactEditScreenDestination(contactId = contact.id))
                                    }) { Icon(Icons.Default.Edit, "Edit") }
                                } else if (phoneNumber != null && phoneNumber != "Unknown") {
                                    IconButton(onClick = { showBlockConfirm = true }) {
                                        Icon(
                                            if (isContactBlocked) Icons.Default.Block else Icons.Outlined.Block,
                                            contentDescription = if (isContactBlocked) "Unblock" else "Block",
                                            tint = if (isContactBlocked) MaterialTheme.colorScheme.error else LocalContentColor.current
                                        )
                                    }
                                    IconButton(onClick = {
                                        navigator.navigate(ContactEditScreenDestination(initialPhone = phoneNumber))
                                    }) { Icon(Icons.Default.PersonAdd, "Add Contact") }
                                }
                            }
                        }

                        val avatarGlowColor = remember(displayName) {
                            com.coolappstore.everdialer.by.svhp.view.components.avatarColors[kotlin.math.abs(displayName.hashCode()) % com.coolappstore.everdialer.by.svhp.view.components.avatarColors.size]
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(400.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(330.dp).background(brush = Brush.radialGradient(colors = listOf(avatarGlowColor.copy(alpha = 0.45f), Color.Transparent))).blur(60.dp))
                                RivoAvatar(name = displayName, photoUri = contact?.photoUri, forcePersonIcon = true, modifier = Modifier.size(240.dp), shape = CircleShape)
                            }
                            Text(text = displayName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

            contactInfoOrder.forEach { elementKey ->
                when (elementKey) {
                    "quick_actions" -> {
                        if (showQuickActions) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(50))
                                                .combinedClickable(
                                                    onClick = {
                                                        if (contact != null && contactPhoneNumbers.size > 1) {
                                                            if (contactDefaultNumber != null) initiateCall(contactDefaultNumber)
                                                            else showNumberPicker = true
                                                        }
                                                        else if (contact != null && contactPhoneNumbers.isNotEmpty()) initiateCall(contactPhoneNumbers.first())
                                                        else if (displayPhone != "Unknown") initiateCall(displayPhone)
                                                    },
                                                    onLongClick = {
                                                        showCallLongPressMenu = true
                                                    }
                                                ),
                                            shape = RoundedCornerShape(50),
                                            color = if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(26.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Call", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                                            }
                                        }

                                        RivoDropdownMenu(
                                            expanded = showCallLongPressMenu,
                                            onDismissRequest = { showCallLongPressMenu = false }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    onClick = {
                                                        showCallLongPressMenu = false
                                                        callWithSimSlot(0)
                                                    },
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = sim1Color,
                                                    contentColor = Color.White
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize(),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.SimCard, contentDescription = "SIM 1", modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("SIM 1", fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                if (hasTwoSims) {
                                                    Surface(
                                                        onClick = {
                                                            showCallLongPressMenu = false
                                                            callWithSimSlot(1)
                                                        },
                                                        modifier = Modifier.weight(1f).height(48.dp),
                                                        shape = RoundedCornerShape(16.dp),
                                                        color = sim2Color,
                                                        contentColor = Color.White
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize(),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.SimCard, contentDescription = "SIM 2", modifier = Modifier.size(20.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("SIM 2", fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                            }
                                            if (whatsAppInstalled) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                                RivoDropdownMenuItem(
                                                    text = "Call via WhatsApp",
                                                    iconBitmap = remember(context) { getWhatsAppIcon(context) },
                                                    onClick = {
                                                        showCallLongPressMenu = false
                                                        chooseSocialApp("whatsapp")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            if (displayPhone != "Unknown") context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$displayPhone")))
                                        },
                                        modifier = Modifier.weight(1f).height(64.dp),
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Text", modifier = Modifier.size(26.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Text", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Contact Info
                    "contact_info" -> {
                        if (showContactInfo) {
                            item {
                                RivoExpressiveCard(title = "Contact Info", icon = Icons.Default.Info) {
                                    if (contact != null) {
                                        contactPhoneNumbers.forEachIndexed { index, number ->
                                            val isPrimary = contactDefaultNumber == number
                                            val phoneEntry = contact.phones.firstOrNull { it.number == number || numbersLikelyMatch(it.number, number) }
                                            val typeLabel = if (phoneEntry != null) getPhoneTypeLabel(phoneEntry.type, phoneEntry.label) else "Mobile"
                                            RivoListItem(
                                                headline = number,
                                                supporting = if (isPrimary) "$typeLabel • Primary" else typeLabel,
                                                leadingIcon = Icons.Default.Phone,
                                                compact = contactPhoneNumbers.size > 1,
                                                onClick = { initiateCall(number) },
                                                onLongClick = {
                                                    selectedNumberForMenu = number
                                                }
                                            )
                                            if (index < contactPhoneNumbers.size - 1 || contact.emails.isNotEmpty() || contact.addresses.isNotEmpty()) {
                                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                        contact.emails.forEachIndexed { index, email ->
                                            RivoListItem(
                                                headline = email,
                                                supporting = "Email",
                                                leadingIcon = Icons.Default.Email,
                                                onClick = {
                                                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                                                },
                                                onLongClick = {
                                                    selectedEmailForMenu = email
                                                }
                                            )
                                            if (index < contact.emails.size - 1 || contact.addresses.isNotEmpty()) {
                                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                        contact.addresses.forEachIndexed { index, address ->
                                            RivoListItem(
                                                headline = address,
                                                supporting = "Address",
                                                leadingIcon = Icons.Default.LocationOn,
                                                onClick = {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address")))
                                                },
                                                onLongClick = {
                                                    selectedAddressForMenu = address
                                                }
                                            )
                                            if (index < contact.addresses.size - 1) {
                                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                    } else if (phoneNumber != null && phoneNumber != "Unknown") {
                                        RivoListItem(
                                            headline = phoneNumber,
                                            supporting = "Unknown Number",
                                            leadingIcon = Icons.Default.Phone,
                                            onClick = { initiateCall(phoneNumber) },
                                            onLongClick = {
                                                selectedNumberForMenu = phoneNumber
                                            }
                                        )
                                    }

                                    RivoDropdownMenu(
                                        expanded = selectedNumberForMenu != null,
                                        onDismissRequest = { selectedNumberForMenu = null }
                                    ) {
                                        val menuNum = selectedNumberForMenu ?: return@RivoDropdownMenu
                                        val isPrimaryNum = contactDefaultNumber == menuNum
                                        RivoDropdownMenuItem(
                                            text = "Send message",
                                            icon = Icons.AutoMirrored.Filled.Message,
                                            iconTint = Color(0xFF4CAF50),
                                            onClick = {
                                                selectedNumberForMenu = null
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$menuNum")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: Throwable) {}
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Copy",
                                            icon = Icons.Default.ContentCopy,
                                            iconTint = Color(0xFF2196F3),
                                            onClick = {
                                                selectedNumberForMenu = null
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Phone number", menuNum))
                                                android.widget.Toast.makeText(context, "Number copied", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Edit contact",
                                            icon = Icons.Default.Edit,
                                            iconTint = Color(0xFF9C27B0),
                                            onClick = {
                                                selectedNumberForMenu = null
                                                if (contact != null) {
                                                    navigator.navigate(ContactEditScreenDestination(contactId = contact.id))
                                                } else {
                                                    navigator.navigate(ContactEditScreenDestination(initialPhone = menuNum))
                                                }
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Edit number",
                                            icon = Icons.Default.Edit,
                                            iconTint = Color(0xFF9C27B0),
                                            onClick = {
                                                val numToEdit = menuNum
                                                selectedNumberForMenu = null
                                                editingNumberValue = numToEdit
                                                originalNumberValue = numToEdit
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Share",
                                            icon = Icons.Default.Share,
                                            iconTint = Color(0xFFFF9800),
                                            onClick = {
                                                selectedNumberForMenu = null
                                                val shareText = if (contact != null && displayName.isNotBlank() && displayName != "Unknown") {
                                                    "$displayName\n$menuNum"
                                                } else {
                                                    menuNum
                                                }
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share contact"))
                                            }
                                        )
                                        if (contact != null) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                            if (isPrimaryNum) {
                                                RivoDropdownMenuItem(
                                                    text = "Unset as primary",
                                                    icon = Icons.Outlined.StarOutline,
                                                    iconTint = Color(0xFFFF9800),
                                                    onClick = {
                                                        selectedNumberForMenu = null
                                                        prefs.setContactDefaultNumber(contactSimKey, null)
                                                        android.widget.Toast.makeText(context, "Unset as primary number", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                RivoDropdownMenuItem(
                                                    text = "Set as primary",
                                                    icon = Icons.Default.Star,
                                                    iconTint = Color(0xFFFF9800),
                                                    onClick = {
                                                        selectedNumberForMenu = null
                                                        prefs.setContactDefaultNumber(contactSimKey, menuNum)
                                                        android.widget.Toast.makeText(context, "Set as primary number", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                            RivoDropdownMenuItem(
                                                text = "Delete number",
                                                icon = Icons.Default.Delete,
                                                isDestructive = true,
                                                onClick = {
                                                    selectedNumberForMenu = null
                                                    if (isPrimaryNum) {
                                                        prefs.setContactDefaultNumber(contactSimKey, null)
                                                    }
                                                    contactsViewModel.deletePhoneNumber(contact.id, menuNum) { success ->
                                                        if (success) {
                                                            android.widget.Toast.makeText(context, "Number deleted", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            android.widget.Toast.makeText(context, "Failed to delete number", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    RivoDropdownMenu(
                                        expanded = selectedEmailForMenu != null,
                                        onDismissRequest = { selectedEmailForMenu = null }
                                    ) {
                                        val menuEmail = selectedEmailForMenu ?: return@RivoDropdownMenu
                                        RivoDropdownMenuItem(
                                            text = "Send email",
                                            icon = Icons.Default.Email,
                                            iconTint = Color(0xFF4CAF50),
                                            onClick = {
                                                selectedEmailForMenu = null
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$menuEmail")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: Throwable) {}
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Copy",
                                            icon = Icons.Default.ContentCopy,
                                            iconTint = Color(0xFF2196F3),
                                            onClick = {
                                                selectedEmailForMenu = null
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Email", menuEmail))
                                                android.widget.Toast.makeText(context, "Email copied", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Edit contact",
                                            icon = Icons.Default.Edit,
                                            iconTint = Color(0xFF9C27B0),
                                            onClick = {
                                                selectedEmailForMenu = null
                                                if (contact != null) {
                                                    navigator.navigate(ContactEditScreenDestination(contactId = contact.id))
                                                }
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Edit email",
                                            icon = Icons.Default.Edit,
                                            iconTint = Color(0xFF9C27B0),
                                            onClick = {
                                                val emailToEdit = menuEmail
                                                selectedEmailForMenu = null
                                                editingEmailValue = emailToEdit
                                                originalEmailValue = emailToEdit
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Share",
                                            icon = Icons.Default.Share,
                                            iconTint = Color(0xFFFF9800),
                                            onClick = {
                                                selectedEmailForMenu = null
                                                val shareText = if (contact != null && displayName.isNotBlank() && displayName != "Unknown") {
                                                    "$displayName\n$menuEmail"
                                                } else {
                                                    menuEmail
                                                }
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share contact"))
                                            }
                                        )
                                    }

                                    RivoDropdownMenu(
                                        expanded = selectedAddressForMenu != null,
                                        onDismissRequest = { selectedAddressForMenu = null }
                                    ) {
                                        val menuAddress = selectedAddressForMenu ?: return@RivoDropdownMenu
                                        RivoDropdownMenuItem(
                                            text = "View on map",
                                            icon = Icons.Default.LocationOn,
                                            iconTint = Color(0xFF4CAF50),
                                            onClick = {
                                                selectedAddressForMenu = null
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$menuAddress")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: Throwable) {}
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Copy",
                                            icon = Icons.Default.ContentCopy,
                                            iconTint = Color(0xFF2196F3),
                                            onClick = {
                                                selectedAddressForMenu = null
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Address", menuAddress))
                                                android.widget.Toast.makeText(context, "Address copied", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Edit contact",
                                            icon = Icons.Default.Edit,
                                            iconTint = Color(0xFF9C27B0),
                                            onClick = {
                                                selectedAddressForMenu = null
                                                if (contact != null) {
                                                    navigator.navigate(ContactEditScreenDestination(contactId = contact.id))
                                                }
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Edit location",
                                            icon = Icons.Default.Edit,
                                            iconTint = Color(0xFF9C27B0),
                                            onClick = {
                                                val addressToEdit = menuAddress
                                                selectedAddressForMenu = null
                                                editingAddressValue = addressToEdit
                                                originalAddressValue = addressToEdit
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Share",
                                            icon = Icons.Default.Share,
                                            iconTint = Color(0xFFFF9800),
                                            onClick = {
                                                selectedAddressForMenu = null
                                                val shareText = if (contact != null && displayName.isNotBlank() && displayName != "Unknown") {
                                                    "$displayName\n$menuAddress"
                                                } else {
                                                    menuAddress
                                                }
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share contact"))
                                            }
                                        )
                                    }

                                    if (editingNumberValue != null) {
                                        var editedNumber by remember(editingNumberValue) { mutableStateOf(editingNumberValue ?: "") }
                                        AlertDialog(
                                            onDismissRequest = {
                                                editingNumberValue = null
                                                originalNumberValue = null
                                            },
                                            shape = RoundedCornerShape(28.dp),
                                            title = { Text("Edit number") },
                                            text = {
                                                OutlinedTextField(
                                                    value = editedNumber,
                                                    onValueChange = { editedNumber = it },
                                                    label = { Text("Phone number") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        val oldNum = originalNumberValue ?: ""
                                                        val newNum = editedNumber.trim()
                                                        editingNumberValue = null
                                                        originalNumberValue = null
                                                        if (newNum.isNotBlank() && newNum != oldNum) {
                                                            if (contact != null) {
                                                                val updatedPhones = if (contact.phones.isNotEmpty()) {
                                                                    contact.phones.map { phone ->
                                                                        if (phone.number == oldNum || numbersLikelyMatch(phone.number, oldNum)) {
                                                                            phone.copy(number = newNum)
                                                                        } else phone
                                                                    }
                                                                } else {
                                                                    listOf(ContactPhone(number = newNum))
                                                                }
                                                                val updatedPhoneNumbers = contact.phoneNumbers.map { num ->
                                                                    if (num == oldNum || numbersLikelyMatch(num, oldNum)) newNum else num
                                                                }.let { if (newNum !in it) it + newNum else it }
                                                                val updatedContact = contact.copy(
                                                                    phones = updatedPhones,
                                                                    phoneNumbers = updatedPhoneNumbers
                                                                )
                                                                contactsViewModel.saveContact(
                                                                    contact = updatedContact,
                                                                    originalContact = contact,
                                                                    updateAllAccounts = true
                                                                )
                                                                if (contactDefaultNumber == oldNum) {
                                                                    prefs.setContactDefaultNumber(contactSimKey, newNum)
                                                                }
                                                                android.widget.Toast.makeText(context, "Number updated", android.widget.Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                navigator.navigate(ContactEditScreenDestination(initialPhone = newNum))
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(
                                                    onClick = {
                                                        editingNumberValue = null
                                                        originalNumberValue = null
                                                    }
                                                ) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }

                                    if (editingEmailValue != null) {
                                        var editedEmail by remember(editingEmailValue) { mutableStateOf(editingEmailValue ?: "") }
                                        AlertDialog(
                                            onDismissRequest = {
                                                editingEmailValue = null
                                                originalEmailValue = null
                                            },
                                            shape = RoundedCornerShape(28.dp),
                                            title = { Text("Edit email") },
                                            text = {
                                                OutlinedTextField(
                                                    value = editedEmail,
                                                    onValueChange = { editedEmail = it },
                                                    label = { Text("Email") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        val oldEmail = originalEmailValue ?: ""
                                                        val newEmail = editedEmail.trim()
                                                        editingEmailValue = null
                                                        originalEmailValue = null
                                                        if (newEmail.isNotBlank() && newEmail != oldEmail) {
                                                            if (contact != null) {
                                                                val updatedEmails = contact.emails.map { email ->
                                                                    if (email == oldEmail) newEmail else email
                                                                }.let { if (newEmail !in it) it + newEmail else it }
                                                                val updatedContact = contact.copy(emails = updatedEmails)
                                                                contactsViewModel.saveContact(
                                                                    contact = updatedContact,
                                                                    originalContact = contact,
                                                                    updateAllAccounts = true
                                                                )
                                                                android.widget.Toast.makeText(context, "Email updated", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(
                                                    onClick = {
                                                        editingEmailValue = null
                                                        originalEmailValue = null
                                                    }
                                                ) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }

                                    if (editingAddressValue != null) {
                                        var editedAddress by remember(editingAddressValue) { mutableStateOf(editingAddressValue ?: "") }
                                        AlertDialog(
                                            onDismissRequest = {
                                                editingAddressValue = null
                                                originalAddressValue = null
                                            },
                                            shape = RoundedCornerShape(28.dp),
                                            title = { Text("Edit location") },
                                            text = {
                                                OutlinedTextField(
                                                    value = editedAddress,
                                                    onValueChange = { editedAddress = it },
                                                    label = { Text("Location") },
                                                    singleLine = false,
                                                    maxLines = 3,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        val oldAddress = originalAddressValue ?: ""
                                                        val newAddress = editedAddress.trim()
                                                        editingAddressValue = null
                                                        originalAddressValue = null
                                                        if (newAddress.isNotBlank() && newAddress != oldAddress) {
                                                            if (contact != null) {
                                                                val updatedAddresses = contact.addresses.map { addr ->
                                                                    if (addr == oldAddress) newAddress else addr
                                                                }.let { if (newAddress !in it) it + newAddress else it }
                                                                val updatedContact = contact.copy(addresses = updatedAddresses)
                                                                contactsViewModel.saveContact(
                                                                    contact = updatedContact,
                                                                    originalContact = contact,
                                                                    updateAllAccounts = true
                                                                )
                                                                android.widget.Toast.makeText(context, "Location updated", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(
                                                    onClick = {
                                                        editingAddressValue = null
                                                        originalAddressValue = null
                                                    }
                                                ) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }

                                    // Copy / Share / Move / Delete — the same actions available from the
                                    // contact's long-press context menu, surfaced here too since a contact
                                    // opened straight from search/details had no way to reach them otherwise.
                                    // Visibility follows Settings → Appearance → Context Menu Elements (Contacts).
                                    if (contact != null && contactInfoActionKeys.isNotEmpty()) {
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            contactInfoActionKeys.forEach { key ->
                                                when (key) {
                                                    "copy_number" -> RivoExpressiveButton(
                                                        icon = Icons.Default.ContentCopy,
                                                        label = "Copy",
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                                        size = 52.dp,
                                                        iconSize = 20.dp,
                                                        onClick = {
                                                            val number = contact.phoneNumbers.firstOrNull() ?: displayPhone
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Phone number", number))
                                                            android.widget.Toast.makeText(context, "Number copied", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                    "share_contact" -> RivoExpressiveButton(
                                                        icon = Icons.Default.Share,
                                                        label = "Share",
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                                        size = 52.dp,
                                                        iconSize = 20.dp,
                                                        onClick = {
                                                            val numbersToShare = if (contactPhoneNumbers.isNotEmpty()) contactPhoneNumbers else contact.phoneNumbers
                                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_TEXT, "$displayName\n${numbersToShare.joinToString(", ")}")
                                                            }
                                                            context.startActivity(Intent.createChooser(shareIntent, "Share contact"))
                                                        }
                                                    )
                                                    "move_contact" -> RivoExpressiveButton(
                                                        icon = Icons.Default.DriveFileMove,
                                                        label = "Move",
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                                        size = 52.dp,
                                                        iconSize = 20.dp,
                                                        onClick = { showMoveDialog = true }
                                                    )
                                                    "delete_contact" -> RivoExpressiveButton(
                                                        icon = Icons.Default.Delete,
                                                        label = "Delete",
                                                        containerColor = if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer,
                                                        size = 52.dp,
                                                        iconSize = 20.dp,
                                                        onClick = { showDeleteConfirm = true }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Social — contact through WhatsApp / WA Business / Telegram / Meet / Truecaller. Only displayed when at least
                    // one social app is installed and enabled on the device. Individual apps are only shown if installed/enabled.
                    "social" -> {
                        if (showSocial && hasAnySocialApp) {
                            item {
                                val whatsAppIcon = remember(context) { getWhatsAppIcon(context) }
                                val whatsAppBusinessIcon = remember(context) { getWhatsAppBusinessIcon(context) }
                                val telegramIcon = remember(context) { getTelegramIcon(context) }
                                val meetIcon = remember(context) { getGoogleMeetIcon(context) }
                                val truecallerIcon = remember(context) { getTruecallerIcon(context) }
                                RivoExpressiveCard(title = "Social", icon = Icons.Default.Share) {
                                    val socialScrollState = rememberScrollState()
                                    val isScrollable = socialScrollState.maxValue > 0
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(socialScrollState)
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (whatsAppInstalled) {
                                                RivoExpressiveButton(
                                                    modifier = Modifier.widthIn(min = 76.dp),
                                                    icon = Icons.Default.Chat, iconBitmap = whatsAppIcon, label = "WhatsApp", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                                        chooseSocialApp("whatsapp")
                                                    }
                                                )
                                            }
                                            if (whatsAppBusinessInstalled) {
                                                RivoExpressiveButton(
                                                    modifier = Modifier.widthIn(min = 76.dp),
                                                    icon = Icons.Default.Chat, iconBitmap = whatsAppBusinessIcon, label = "WA Business", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                                        chooseSocialApp("whatsapp_business")
                                                    }
                                                )
                                            }
                                            if (telegramInstalled) {
                                                RivoExpressiveButton(
                                                    modifier = Modifier.widthIn(min = 76.dp),
                                                    icon = Icons.Default.Send, iconBitmap = telegramIcon, label = "Telegram", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                                        chooseSocialApp("telegram")
                                                    }
                                                )
                                            }
                                            if (meetInstalled) {
                                                RivoExpressiveButton(
                                                    modifier = Modifier.widthIn(min = 76.dp),
                                                    icon = Icons.Default.VideoCall, iconBitmap = meetIcon, label = "Meet", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                                        chooseSocialApp("googlemeet")
                                                    }
                                                )
                                            }
                                            if (truecallerInstalled) {
                                                RivoExpressiveButton(
                                                    modifier = Modifier.widthIn(min = 76.dp),
                                                    icon = Icons.Default.Search, iconBitmap = truecallerIcon, label = "Truecaller", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                                        chooseSocialApp("truecaller")
                                                    }
                                                )
                                            }
                                        }
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isScrollable && socialScrollState.canScrollForward,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 4.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                shadowElevation = 3.dp,
                                                modifier = Modifier.size(7.dp)
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Description section (synced with Microsoft Exchange / Gmail contact notes via ContactsContract)
                    "description" -> {
                        if (showDescription) {
                            item {
                                val accountsForDescription = remember(contactAccounts, selectedVisibilityAccount) {
                                    val sel = selectedVisibilityAccount
                                    if (sel != null) {
                                        contactAccounts.filter {
                                            it.rawContactId == sel.rawContactId ||
                                            ((it.accountType ?: "") == (sel.accountType ?: "") && (it.accountName ?: "") == (sel.accountName ?: ""))
                                        }
                                    } else {
                                        contactAccounts
                                    }
                                }

                                data class UniqueDescItem(
                                    val text: String,
                                    val accounts: List<ContactAccountInfo>,
                                    val primaryAccount: ContactAccountInfo
                                )

                                val uniqueDescriptions = remember(accountsForDescription, contact?.note) {
                                    val list = mutableListOf<UniqueDescItem>()
                                    for (acc in accountsForDescription) {
                                        val desc = acc.description?.trim()
                                        if (!desc.isNullOrBlank()) {
                                            val existingIndex = list.indexOfFirst { it.text == desc }
                                            if (existingIndex >= 0) {
                                                val existing = list[existingIndex]
                                                list[existingIndex] = existing.copy(accounts = existing.accounts + acc)
                                            } else {
                                                list.add(UniqueDescItem(text = desc, accounts = listOf(acc), primaryAccount = acc))
                                            }
                                        }
                                    }
                                    if (list.isEmpty() && selectedVisibilityAccount == null && !contact?.note.isNullOrBlank()) {
                                        val fallbackAcc = contactAccounts.firstOrNull() ?: ContactAccountInfo(
                                            rawContactId = 0L,
                                            accountType = null,
                                            accountName = null,
                                            displayName = "Device Storage",
                                            isReadOnly = false,
                                            isSim = false
                                        )
                                        list.add(UniqueDescItem(text = contact.note!!.trim(), accounts = listOf(fallbackAcc), primaryAccount = fallbackAcc))
                                    }
                                    list
                                }

                                RivoExpressiveCard(
                                    title = "Description (Synced Notes)",
                                    icon = Icons.Default.Description,
                                    trailingContent = {
                                        if (contact != null) {
                                            Box {
                                                IconButton(
                                                    onClick = { showDescriptionOverflowMenu = true },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.MoreVert,
                                                        contentDescription = "Description options",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showDescriptionOverflowMenu,
                                                    onDismissRequest = { showDescriptionOverflowMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Visibility") },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.Visibility,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        },
                                                        onClick = {
                                                            showDescriptionOverflowMenu = false
                                                            showVisibilityDialog = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    if (selectedVisibilityAccount != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.FilterList,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Visibility: ${selectedVisibilityAccount!!.displayName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { selectedVisibilityAccount = null },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear visibility filter",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (uniqueDescriptions.isNotEmpty()) {
                                        uniqueDescriptions.forEachIndexed { index, item ->
                                            var isExpanded by remember(item.text) { mutableStateOf(false) }
                                            var hasMoreThan5Lines by remember(item.text) {
                                                mutableStateOf(item.text.lines().size > 5)
                                            }

                                            if (contactAccounts.size > 1 || selectedVisibilityAccount != null) {
                                                val locationNames = item.accounts.map { it.displayName }.distinct().joinToString(", ")
                                                val locationIcon: androidx.compose.ui.graphics.vector.ImageVector = when {
                                                    item.accounts.all { it.isSim } -> Icons.Default.SimCard
                                                    item.accounts.all { it.accountType?.contains("google", ignoreCase = true) == true } -> Icons.Default.AccountCircle
                                                    item.accounts.all { it.accountType != null } -> Icons.Default.Sync
                                                    else -> Icons.Default.PhoneAndroid
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        locationIcon,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = locationNames,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            val annotated = buildClickableAnnotatedString(item.text)
                                            SelectionContainer {
                                                Text(
                                                    text = annotated,
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                                    maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                                                    overflow = TextOverflow.Ellipsis,
                                                    onTextLayout = { textLayoutResult ->
                                                        if (textLayoutResult.lineCount > 5 || textLayoutResult.hasVisualOverflow) {
                                                            hasMoreThan5Lines = true
                                                        }
                                                    }
                                                )
                                            }

                                            if (hasMoreThan5Lines) {
                                                TextButton(
                                                    onClick = { isExpanded = !isExpanded },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(if (isExpanded) "Show less" else "Show more")
                                                }
                                            }

                                            if (contact != null) {
                                                TextButton(
                                                    onClick = {
                                                        editingTargetAccount = if (item.accounts.size == contactAccounts.size) null else item.primaryAccount
                                                        editingInitialDescription = item.text
                                                        showDescriptionEditor = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    val editLabel = if (uniqueDescriptions.size > 1) {
                                                        val names = item.accounts.map { it.displayName }.distinct().joinToString(", ")
                                                        "Edit description ($names)"
                                                    } else {
                                                        "Edit description"
                                                    }
                                                    Text(editLabel)
                                                }
                                            }

                                            if (index < uniqueDescriptions.size - 1) {
                                                HorizontalDivider(
                                                    Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        if (selectedVisibilityAccount == null && contact != null) {
                                            val accountsWithoutDesc = contactAccounts.filter { it.description.isNullOrBlank() && !it.isReadOnly }
                                            if (accountsWithoutDesc.isNotEmpty()) {
                                                HorizontalDivider(
                                                    Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                )
                                                TextButton(
                                                    onClick = {
                                                        editingTargetAccount = accountsWithoutDesc.firstOrNull()
                                                        editingInitialDescription = ""
                                                        showDescriptionEditor = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Add description for another location...")
                                                }
                                            }
                                        }
                                    } else {
                                        if (contact != null) {
                                            if (selectedVisibilityAccount != null) {
                                                Text(
                                                    text = "No description in ${selectedVisibilityAccount!!.displayName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    editingTargetAccount = selectedVisibilityAccount
                                                    editingInitialDescription = ""
                                                    showDescriptionEditor = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    if (selectedVisibilityAccount != null) "Add description for ${selectedVisibilityAccount!!.displayName}..."
                                                    else "Add description..."
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Save contact to sync description with Google & Exchange",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes section (between Contact Info and Recent Activity)
                    "notes" -> {
                        if (showNotes) {
                            item {
                                var currentNote by remember(displayName, displayPhone) {
                                    mutableStateOf(NoteManager.readNote(context, displayName, displayPhone))
                                }
                                LaunchedEffect(showNoteEditor) {
                                    if (!showNoteEditor) currentNote = NoteManager.readNote(context, displayName, displayPhone)
                                }
                                var isNoteExpanded by remember(currentNote) { mutableStateOf(false) }
                                var hasNoteMoreThan5Lines by remember(currentNote) {
                                    mutableStateOf(currentNote.lines().size > 5)
                                }

                                RivoExpressiveCard(title = "Note (Phone Only)", icon = Icons.Default.Note) {
                                    if (currentNote.isNotBlank()) {
                                        // Inline preview with selectable text and clickable links
                                        val annotated = buildClickableAnnotatedString(currentNote)
                                        SelectionContainer {
                                            Text(
                                                text = annotated,
                                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                                maxLines = if (isNoteExpanded) Int.MAX_VALUE else 5,
                                                overflow = TextOverflow.Ellipsis,
                                                onTextLayout = { textLayoutResult ->
                                                    if (textLayoutResult.lineCount > 5 || textLayoutResult.hasVisualOverflow) {
                                                        hasNoteMoreThan5Lines = true
                                                    }
                                                }
                                            )
                                        }
                                        if (hasNoteMoreThan5Lines) {
                                            TextButton(
                                                onClick = { isNoteExpanded = !isNoteExpanded },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    if (isNoteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (isNoteExpanded) "Show less" else "Show more")
                                            }
                                        }
                                        HorizontalDivider(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                    TextButton(
                                        onClick = { showNoteEditor = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (currentNote.isBlank()) "Add note..." else "Edit note")
                                    }

                                    // When "Integrate Notes Section" is turned OFF in Settings → Calls & System,
                                    // this app Notes section and the call recording notes (kept inside Ever Call
                                    // Recorder's playback screen) are merged: surface a quick link here so both
                                    // notes live in one place from the user's perspective. When the toggle is ON
                                    // (default) the two notes sections stay fully separate, as before.
                                    val integrateNotes = remember(settingsVer) {
                                        prefs.getBoolean(PreferenceManager.KEY_INTEGRATE_NOTES, true)
                                    }
                                    if (!integrateNotes) {
                                        HorizontalDivider(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        TextButton(
                                            onClick = {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                                    val launch = Intent(context, com.coolappstore.evercallrecorder.by.svhp.MainActivity::class.java)
                                                    try { context.startActivity(launch) } catch (_: Exception) {}
                                                } else {
                                                    android.widget.Toast.makeText(context, "Call Recording requires Android 11 or newer", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.FiberManualRecord, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            Spacer(Modifier.width(6.dp))
                                            Text("View call recording notes for this contact")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Events & More
                    "events" -> {
                        if (showEvents && contact != null && (contact.events.isNotEmpty() || contact.addresses.isNotEmpty())) {
                            item {
                                RivoExpressiveCard(title = "Events & More", icon = Icons.Default.Event) {
                                    contact.events.forEachIndexed { index, event ->
                                        val isBirthday = event.type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                                        RivoListItem(headline = event.date, supporting = event.label ?: if (isBirthday) "Birthday" else "Event", leadingIcon = if (isBirthday) Icons.Outlined.Cake else Icons.Outlined.Event, onClick = {})
                                        if (index < contact.events.size - 1 || contact.addresses.isNotEmpty()) {
                                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                    contact.addresses.forEachIndexed { index, address ->
                                        RivoListItem(headline = address, supporting = "Address", leadingIcon = Icons.Default.LocationOn, onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address")))
                                        })
                                        if (index < contact.addresses.size - 1) {
                                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recent Activity
                    "recent_activity" -> {
                        if (showRecentActivity && contactLogs.isNotEmpty()) {
                            item {
                                RivoExpressiveCard(title = "Recent Activity", icon = Icons.Default.History) {
                                    Column(modifier = Modifier.animateContentSize()) {
                                        contactLogs.take(3).forEachIndexed { index, log ->
                                            CallLogTileSimple(log)
                                            if (index < 2 && index < contactLogs.size - 1) {
                                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                        if (contactLogs.size > 3) {
                                            TextButton(onClick = { navController.navigate("call_log_detail_screen?contactId=${contactId ?: "null"}&phoneNumber=${phoneNumber ?: "null"}") }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Show full history")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Choose Sim — per-contact override of which SIM is used to call this contact
                    // (saved or unsaved), defaulting to "According to Settings". Available for every
                    // contact, so it always shows here regardless of whether Recent Activity or
                    // Saved In end up rendering around it.
                    "choose_sim" -> {
                        if (showChooseSim) {
                            item {
                                RivoExpressiveCard(title = "Choose Sim", icon = Icons.Default.SimCard) {
                                    RivoListItem(
                                        headline = simChoiceLabel(contactSimChoice),
                                        supporting = "Sim used to call this contact",
                                        leadingIcon = Icons.Default.SimCard,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = { showChooseSimDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // Calling Backgrounds — per-contact override for incoming and ongoing call screens
                    "calling_backgrounds" -> {
                        if (showCallingBackgrounds) {
                            item {
                                val incomingContactBgType = remember(settingsVer, contactSimKey) {
                                    prefs.getString("contact_${contactSimKey}_incoming_bg_type", null)
                                }
                                val ongoingContactBgType = remember(settingsVer, contactSimKey) {
                                    prefs.getString("contact_${contactSimKey}_ongoing_bg_type", null)
                                }
                                val incomingSupporting = when (incomingContactBgType) {
                                    "wallpaper" -> "Device Wallpaper (Customized)"
                                    "picture" -> "Custom Picture"
                                    "video" -> "Custom Video"
                                    "none" -> "None (Solid Background)"
                                    else -> "According to Settings (Default)"
                                }
                                val ongoingSupporting = when (ongoingContactBgType) {
                                    "wallpaper" -> "Device Wallpaper (Customized)"
                                    "picture" -> "Custom Picture"
                                    "video" -> "Custom Video"
                                    "none" -> "None (Solid Background)"
                                    else -> "According to Settings (Default)"
                                }

                                RivoExpressiveCard(title = "Calling Backgrounds", icon = Icons.Default.Wallpaper) {
                                    RivoListItem(
                                        headline = "Incoming Call Background",
                                        supporting = incomingSupporting,
                                        leadingIcon = Icons.Default.CallReceived,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = {
                                            navigator.navigate(
                                                CustomBackgroundPickerScreenDestination(
                                                    isIncoming = true,
                                                    contactKey = contactSimKey.toString(),
                                                    contactDisplayName = contact?.name ?: displayName
                                                )
                                            )
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                    RivoListItem(
                                        headline = "Ongoing Call Background",
                                        supporting = ongoingSupporting,
                                        leadingIcon = Icons.Default.PhoneInTalk,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = {
                                            navigator.navigate(
                                                CustomBackgroundPickerScreenDestination(
                                                    isIncoming = false,
                                                    contactKey = contactSimKey.toString(),
                                                    contactDisplayName = contact?.name ?: displayName
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Advanced PFP — per-contact override for incoming and ongoing call custom contact PFP
                    "advanced_pfp" -> {
                        if (showAdvancedPfp) {
                            item {
                                val incomingContactPfpType = remember(settingsVer, contactSimKey) {
                                    prefs.getString("contact_${contactSimKey}_incoming_custom_pfp_type", null)
                                }
                                val ongoingContactPfpType = remember(settingsVer, contactSimKey) {
                                    prefs.getString("contact_${contactSimKey}_ongoing_custom_pfp_type", null)
                                }
                                val incomingPfpSupporting = when (incomingContactPfpType) {
                                    "wallpaper" -> "Device Wallpaper (Customized)"
                                    "picture" -> "Custom Picture"
                                    "video" -> "Custom Video"
                                    "none" -> "None (Default Face Icon)"
                                    else -> "According to Settings (Default)"
                                }
                                val ongoingPfpSupporting = when (ongoingContactPfpType) {
                                    "wallpaper" -> "Device Wallpaper (Customized)"
                                    "picture" -> "Custom Picture"
                                    "video" -> "Custom Video"
                                    "none" -> "None (Default Face Icon)"
                                    else -> "According to Settings (Default)"
                                }

                                RivoExpressiveCard(title = "Advanced PFP", icon = Icons.Default.AccountCircle) {
                                    RivoListItem(
                                        headline = "Incoming Call PFP",
                                        supporting = incomingPfpSupporting,
                                        leadingIcon = Icons.Default.CallReceived,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = {
                                            navigator.navigate(
                                                ContactPfpCustomizationScreenDestination(
                                                    isIncoming = true,
                                                    contactKey = contactSimKey.toString(),
                                                    contactDisplayName = contact?.name ?: displayName
                                                )
                                            )
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                    RivoListItem(
                                        headline = "Ongoing Call PFP",
                                        supporting = ongoingPfpSupporting,
                                        leadingIcon = Icons.Default.PhoneInTalk,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = {
                                            navigator.navigate(
                                                ContactPfpCustomizationScreenDestination(
                                                    isIncoming = false,
                                                    contactKey = contactSimKey.toString(),
                                                    contactDisplayName = contact?.name ?: displayName
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ringtone — per-contact custom ringtone, defaulting to the system ringtone.
                    // Only meaningful for a saved contact (writes to ContactsContract by contact id).
                    "ringtone" -> {
                        if (showRingtone && contact != null) {
                            item {
                                RivoExpressiveCard(title = "Ringtone", icon = Icons.Default.MusicNote) {
                                    RivoListItem(
                                        headline = contactRingtoneLabel,
                                        supporting = "Ringtone for calls from this contact",
                                        leadingIcon = Icons.Default.MusicNote,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = { openRingtonePicker() }
                                    )
                                }
                            }
                        }
                    }

                    // Choose Default Number — only meaningful (and only shown) when the contact has
                    // 2+ saved numbers, e.g. one saved with a country code and one without.
                    "choose_default_number" -> {
                        if (showChooseDefaultNumber && contact != null && contactPhoneNumbers.size > 1) {
                            item {
                                RivoExpressiveCard(title = "Choose Default Number", icon = Icons.Default.Numbers) {
                                    RivoListItem(
                                        headline = contactDefaultNumber ?: "Ask Every Time",
                                        supporting = "Number used when calling this contact",
                                        leadingIcon = Icons.Default.Numbers,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = { showChooseDefaultNumberDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // Saved In — shows the user which account(s) this contact actually lives in
                    // (Google account(s), SIM, phone storage, etc.), since a contact merged across
                    // multiple sources can be stored in more than one place at once.
                    "saved_in" -> {
                        if (showSavedIn && contact != null && contact.sourceAccounts.isNotEmpty()) {
                            item {
                                RivoExpressiveCard(title = "Saved In", icon = Icons.Default.Storage) {
                                    contact.sourceAccounts.forEachIndexed { index, source ->
                                        val icon = when {
                                            source.startsWith("SIM", ignoreCase = true) -> Icons.Default.SimCard
                                            source.equals("Device Storage", ignoreCase = true) -> Icons.Default.PhoneAndroid
                                            source.equals("WhatsApp", ignoreCase = true) -> Icons.Default.Chat
                                            else -> Icons.Default.AccountCircle
                                        }
                                        RivoListItem(headline = source, leadingIcon = icon, onClick = {})
                                        if (index < contact.sourceAccounts.size - 1) {
                                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

@Composable
fun QrCodeDialog(name: String, phone: String?, email: String?, onDismiss: () -> Unit) {
    val vCard = remember(name, phone, email) { QrCodeUtils.generateVCard(name, phone, email) }
    val qrBitmap = remember(vCard) { QrCodeUtils.generateQrCode(vCard, 600) }
    Dialog(onDismissRequest = onDismiss) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Contact QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    qrBitmap?.let {
                        Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Text(phone ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Close") }
                }
            }
        }
    }
}

private fun buildClickableAnnotatedString(text: String): AnnotatedString {
    val urlPattern = android.util.Patterns.WEB_URL
    return buildAnnotatedString {
        var lastIdx = 0
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            append(text.substring(lastIdx, start))
            val rawUrl = matcher.group()
            val fullUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
            val link = LinkAnnotation.Url(
                url = fullUrl,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = Color(0xFF1E88E5),
                        textDecoration = TextDecoration.Underline
                    )
                )
            )
            withLink(link) {
                append(rawUrl)
            }
            lastIdx = end
        }
        append(text.substring(lastIdx))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionEditorDialog(
    contactName: String,
    initialDescription: String,
    accounts: List<ContactAccountInfo> = emptyList(),
    initialSelectedAccount: ContactAccountInfo? = null,
    onSave: (note: String, targetAccount: ContactAccountInfo?, updateAll: Boolean) -> Unit,
    onDelete: (targetAccount: ContactAccountInfo?, updateAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialDescription) }
    val writableAccounts = remember(accounts) { accounts.filter { !it.isReadOnly } }
    var updateAllLinkedAccounts by remember(initialSelectedAccount) {
        mutableStateOf(initialSelectedAccount == null)
    }
    var selectedTarget by remember(initialSelectedAccount) {
        mutableStateOf(initialSelectedAccount)
    }
    var showAccountPickerDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            onSave(text.trim(), selectedTarget, updateAllLinkedAccounts)
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        com.coolappstore.everdialer.by.svhp.view.theme.ProvideScaledDensity {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Description (Synced Notes)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Syncs with Gmail & Microsoft Exchange",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (initialDescription.isNotBlank()) {
                            IconButton(onClick = { onDelete(selectedTarget, updateAllLinkedAccounts) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Description",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Button(
                            onClick = { onSave(text.trim(), selectedTarget, updateAllLinkedAccounts) },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Save") }
                    }
                }

                if (writableAccounts.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    val (storageTitle, storageSubtitle, storageIcon) = remember(
                        writableAccounts,
                        selectedTarget,
                        updateAllLinkedAccounts
                    ) {
                        val locationNames = writableAccounts.map { it.displayName }.distinct()
                        val allLocationsText = if (locationNames.isNotEmpty()) locationNames.joinToString(", ") else "Device Storage"
                        if (updateAllLinkedAccounts || selectedTarget == null) {
                            Triple("Edit Location", "Present in: $allLocationsText", Icons.Default.Storage)
                        } else {
                            Triple("Edit Location", "Editing only in: ${selectedTarget?.displayName ?: ""}", Icons.Default.Edit)
                        }
                    }

                    RivoExpressiveCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAccountPickerDialog = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    storageIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = storageTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = storageSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { showAccountPickerDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Change")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text("Enter contact description / notes...") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 6
                )
            }
        }
    }

    if (showAccountPickerDialog) {
        Dialog(onDismissRequest = { showAccountPickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Edit location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )

                    val distinctWritableAccounts = remember(writableAccounts) {
                        writableAccounts.distinctBy { (it.accountType ?: "") to (it.accountName ?: "") }
                    }

                    if (distinctWritableAccounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    updateAllLinkedAccounts = true
                                    selectedTarget = null
                                    showAccountPickerDialog = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "All locations (${distinctWritableAccounts.size})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Edit across all accounts by default (Recommended)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (updateAllLinkedAccounts && selectedTarget == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    distinctWritableAccounts.forEach { acc ->
                        val isSelected = !updateAllLinkedAccounts && selectedTarget?.let {
                            it.rawContactId == acc.rawContactId ||
                            ((it.accountType ?: "") == (acc.accountType ?: "") && (it.accountName ?: "") == (acc.accountName ?: ""))
                        } == true
                        val icon: androidx.compose.ui.graphics.vector.ImageVector = when {
                            acc.isSim -> Icons.Default.SimCard
                            acc.accountType?.contains("google", ignoreCase = true) == true -> Icons.Default.AccountCircle
                            acc.accountType != null -> Icons.Default.Sync
                            else -> Icons.Default.PhoneAndroid
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTarget = acc
                                    updateAllLinkedAccounts = false
                                    showAccountPickerDialog = false
                                    if (text.isBlank() && !acc.description.isNullOrBlank()) {
                                        text = acc.description
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(acc.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                if (!acc.accountName.isNullOrBlank()) {
                                    Text(
                                        acc.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
