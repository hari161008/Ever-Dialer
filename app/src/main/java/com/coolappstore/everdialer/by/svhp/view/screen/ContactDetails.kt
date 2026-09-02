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
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.QrCode2
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
import com.coolappstore.everdialer.by.svhp.controller.util.isTelegramInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isGoogleMeetInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTruecallerInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.getWhatsAppIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTelegramIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getGoogleMeetIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTruecallerIcon
import com.coolappstore.everdialer.by.svhp.controller.util.openWhatsAppChat
import com.coolappstore.everdialer.by.svhp.controller.util.openTelegramChat
import com.coolappstore.everdialer.by.svhp.controller.util.openTruecaller
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startGoogleMeetVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startGoogleMeetVideoCall
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.koinInject
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.controller.util.ContactRingtoneUtils
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import androidx.compose.material.icons.outlined.Block

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
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
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
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
    // All this contact's saved numbers, so the Social card can offer a choice when there's more
    // than one (e.g. one saved with a country code, one without) instead of always defaulting to
    // the first saved number — which could be one that isn't actually registered on that app.
    val socialNumbers = remember(contact, phoneNumber) {
        contact?.phoneNumbers?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(phoneNumber?.takeIf { it.isNotBlank() })
    }
    // App tapped in the Social card but still awaiting a number pick (only used when the contact
    // has 2+ numbers); null once a number has been picked (or there was only one to begin with).
    var pendingSocialApp by remember { mutableStateOf<String?>(null) }
    var socialSelectedNumber by remember { mutableStateOf<String?>(null) }

    val whatsAppInstalled = remember(context) { isAnyPackageInstalled(context, WHATSAPP_PACKAGES) }
    val telegramInstalled = remember(context) { isTelegramInstalled(context) }
    val meetInstalled = remember(context) { isGoogleMeetInstalled(context) }
    val truecallerInstalled = remember(context) { isTruecallerInstalled(context) }
    val hasAnySocialApp = whatsAppInstalled || telegramInstalled || meetInstalled || truecallerInstalled

    // Respect Settings → Appearance → "Context Menu Elements" (Contacts section) customization
    // so the actions shown here always match what's configured for the contact's context menu.
    val settingsVer by prefs.settingsChanged.collectAsState()
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
            contact.phoneNumbers.any { BlockedNumbersManager.isBlocked(prefs, it) }
        } else if (phoneNumber != null && phoneNumber != "Unknown") {
            BlockedNumbersManager.isBlocked(prefs, phoneNumber)
        } else false
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Contact Info → "Choose Sim" — per-contact override of which SIM is used to call this
    // contact. Keyed by the saved contact's id, or the raw number for an unsaved/unknown one, so
    // both kinds of contacts remember their own choice independently. Defaults to "According to
    // settings" (falls back to the app-wide default SIM setting).
    val contactSimKey = contact?.id ?: phoneNumber ?: displayPhone
    val contactSimChoice = remember(settingsVer, contactSimKey) { prefs.getContactSimChoice(contactSimKey) }
    // Contact Info → "Choose Default Number" — per-contact override of which saved number the
    // header call button dials directly, for contacts saved with 2+ numbers (skips the number
    // picker once set). Same keying as contactSimKey, so it travels with the same contact.
    val contactDefaultNumber = remember(settingsVer, contactSimKey) { prefs.getContactDefaultNumber(contactSimKey) }
        .takeIf { number -> contact != null && number != null && contact.phoneNumbers.contains(number) }

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

    // Entrance / exit animation
    var screenVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    fun navigateBack() {
        isClosing = true
        scope.launch {
            kotlinx.coroutines.delay(420)
            navigator.navigateUp()
        }
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (screenVisible && !isClosing) 1f else 0f,
        animationSpec = if (isClosing) tween(380, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                        else tween(500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "screenAlpha"
    )
    val screenOffsetY by animateDpAsState(
        targetValue = if (screenVisible && !isClosing) 0.dp else if (isClosing) 80.dp else 56.dp,
        animationSpec = if (isClosing) tween(400, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                        else spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
        label = "screenOffsetY"
    )
    LaunchedEffect(Unit) { screenVisible = true }
    BackHandler { navigateBack() }

    val initiateCall = { number: String ->
        placeCallWithContactSimPreference(
            context, number, contactSimChoice, simPref, recentSimSlotForContact
        ) {
            pendingNumber = number; showSimPicker = true
        }
    }

    if (showNumberPicker && contact != null) {
        NumberPickerDialog(numbers = contact.phoneNumbers, onDismissRequest = { showNumberPicker = false }, onNumberSelected = { showNumberPicker = false; initiateCall(it) })
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
            numbers = contact.phoneNumbers,
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
            "telegram" -> "Telegram"
            else -> "Google Meet"
        }
        AppQuickActionsDialog(
            appName = appLabel,
            onChat = if (app == "googlemeet") null else {
                {
                    showAppQuickActions = null
                    val opened = if (app == "whatsapp") openWhatsAppChat(context, socialPhone) else openTelegramChat(context, socialPhone)
                    if (!opened) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onVoiceCall = {
                showAppQuickActions = null
                val started = when (app) {
                    "whatsapp" -> startWhatsAppVoiceCall(context, socialPhone)
                    "telegram" -> startTelegramVoiceCall(context, socialPhone)
                    else -> startGoogleMeetVoiceCall(context, socialPhone)
                }
                if (!started) android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onVideoCall = {
                showAppQuickActions = null
                val started = when (app) {
                    "whatsapp" -> startWhatsAppVideoCall(context, socialPhone)
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
    val shortcutNumbers = remember(contact, phoneNumber) {
        contact?.phoneNumbers?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFF44336)) },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to permanently delete \"$displayName\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    contactsViewModel.deleteContact(contact.id)
                    navigateBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    if (showBlockConfirm) {
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Box(modifier = Modifier.fillMaxSize().alpha(screenAlpha).offset(y = screenOffsetY)) {

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
                                IconButton(onClick = { navigateBack() }) {
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

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (contact != null && contact.phoneNumbers.size > 1) {
                                    if (contactDefaultNumber != null) initiateCall(contactDefaultNumber)
                                    else showNumberPicker = true
                                }
                                else if (displayPhone != "Unknown") initiateCall(displayPhone)
                            },
                            modifier = Modifier.weight(1f).height(64.dp),
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

                // Contact Info
                item {
                    RivoExpressiveCard(title = "Contact Info", icon = Icons.Default.Info) {
                        if (contact != null) {
                            contact.phoneNumbers.forEachIndexed { index, number ->
                                RivoListItem(
                                    headline = number,
                                    supporting = "Mobile",
                                    leadingIcon = Icons.Default.Phone,
                                    compact = contact.phoneNumbers.size > 1,
                                    onClick = { initiateCall(number) },
                                    onLongClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Phone number", number))
                                        android.widget.Toast.makeText(context, "Number copied", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                                if (index < contact.phoneNumbers.size - 1 || contact.emails.isNotEmpty() || contact.addresses.isNotEmpty()) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                            contact.emails.forEachIndexed { index, email ->
                                RivoListItem(headline = email, supporting = "Email", leadingIcon = Icons.Default.Email, onClick = {
                                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                                })
                                if (index < contact.emails.size - 1 || contact.addresses.isNotEmpty()) {
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
                        } else if (phoneNumber != null && phoneNumber != "Unknown") {
                            RivoListItem(
                                headline = phoneNumber,
                                supporting = "Unknown Number",
                                leadingIcon = Icons.Default.Phone,
                                onClick = { initiateCall(phoneNumber) },
                                onLongClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Phone number", phoneNumber))
                                    android.widget.Toast.makeText(context, "Number copied", android.widget.Toast.LENGTH_SHORT).show()
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
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "$displayName\n${contact.phoneNumbers.joinToString(", ")}")
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

                // Notes section (between Contact Info and Recent Activity)
                item {
                    var currentNote by remember(displayName, displayPhone) {
                        mutableStateOf(NoteManager.readNote(context, displayName, displayPhone))
                    }
                    LaunchedEffect(showNoteEditor) {
                        if (!showNoteEditor) currentNote = NoteManager.readNote(context, displayName, displayPhone)
                    }

                    RivoExpressiveCard(title = "Notes", icon = Icons.Default.Note) {
                        if (currentNote.isNotBlank()) {
                            // Inline preview with clickable links
                            val annotated = buildClickableAnnotatedString(currentNote)
                            ClickableText(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                onClick = { offset ->
                                    annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                                        val url = if (ann.item.startsWith("http")) ann.item else "https://${ann.item}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            )
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

                // Social — contact through WhatsApp / Telegram / Meet / Truecaller. Only displayed when at least
                // one social app is installed and enabled on the device. Individual apps are only shown if installed/enabled.
                if (hasAnySocialApp) {
                    item {
                        val whatsAppIcon = remember(context) { getWhatsAppIcon(context) }
                        val telegramIcon = remember(context) { getTelegramIcon(context) }
                        val meetIcon = remember(context) { getGoogleMeetIcon(context) }
                        val truecallerIcon = remember(context) { getTruecallerIcon(context) }
                        RivoExpressiveCard(title = "Social", icon = Icons.Default.Share) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (whatsAppInstalled) {
                                    RivoExpressiveButton(icon = Icons.Default.Chat, iconBitmap = whatsAppIcon, label = "WhatsApp", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                        chooseSocialApp("whatsapp")
                                    })
                                }
                                if (telegramInstalled) {
                                    RivoExpressiveButton(icon = Icons.Default.Send, iconBitmap = telegramIcon, label = "Telegram", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                        chooseSocialApp("telegram")
                                    })
                                }
                                if (meetInstalled) {
                                    RivoExpressiveButton(icon = Icons.Default.VideoCall, iconBitmap = meetIcon, label = "Meet", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                        chooseSocialApp("googlemeet")
                                    })
                                }
                                if (truecallerInstalled) {
                                    RivoExpressiveButton(icon = Icons.Default.Search, iconBitmap = truecallerIcon, label = "Truecaller", size = 56.dp, iconSize = 22.dp, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, onClick = {
                                        if (displayPhone == "Unknown") return@RivoExpressiveButton
                                        chooseSocialApp("truecaller")
                                    })
                                }
                            }
                        }
                    }
                }

                // Events & More
                if (contact != null && (contact.events.isNotEmpty() || contact.addresses.isNotEmpty())) {
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

                // Recent Activity
                if (contactLogs.isNotEmpty()) {
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

                // Choose Sim — per-contact override of which SIM is used to call this contact
                // (saved or unsaved), defaulting to "According to Settings". Available for every
                // contact, so it always shows here regardless of whether Recent Activity or
                // Saved In end up rendering around it.
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

                // Calling Backgrounds — per-contact override for incoming and ongoing call screens
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

                // Advanced PFP — per-contact override for incoming and ongoing call custom contact PFP
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

                // Ringtone — per-contact custom ringtone, defaulting to the system ringtone.
                // Only meaningful for a saved contact (writes to ContactsContract by contact id).
                if (contact != null) {
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

                // Choose Default Number — only meaningful (and only shown) when the contact has
                // 2+ saved numbers, e.g. one saved with a country code and one without.
                if (contact != null && contact.phoneNumbers.size > 1) {
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

                // Saved In — shows the user which account(s) this contact actually lives in
                // (Google account(s), SIM, phone storage, etc.), since a contact merged across
                // multiple sources can be stored in more than one place at once.
                if (contact != null && contact.sourceAccounts.isNotEmpty()) {
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

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun QrCodeDialog(name: String, phone: String?, email: String?, onDismiss: () -> Unit) {
    val vCard = remember(name, phone, email) { QrCodeUtils.generateVCard(name, phone, email) }
    val qrBitmap = remember(vCard) { QrCodeUtils.generateQrCode(vCard, 600) }
    Dialog(onDismissRequest = onDismiss) {
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

private fun buildClickableAnnotatedString(text: String): AnnotatedString {
    val urlPattern = android.util.Patterns.WEB_URL
    return buildAnnotatedString {
        var lastIdx = 0
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            append(text.substring(lastIdx, start))
            pushStringAnnotation("URL", matcher.group())
            withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF1E88E5), textDecoration = TextDecoration.Underline)) {
                append(text.substring(start, end))
            }
            pop()
            lastIdx = end
        }
        append(text.substring(lastIdx))
    }
}
