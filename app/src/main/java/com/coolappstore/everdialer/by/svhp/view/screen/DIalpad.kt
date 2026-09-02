package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.DialpadToneStyle
import com.coolappstore.everdialer.by.svhp.controller.util.DialpadTonePlayer
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.normalizeNumberDigits
import com.coolappstore.everdialer.by.svhp.controller.util.getWhatsAppIcon
import com.coolappstore.everdialer.by.svhp.controller.util.getTelegramIcon
import com.coolappstore.everdialer.by.svhp.controller.util.isAnyPackageInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.isTelegramInstalled
import com.coolappstore.everdialer.by.svhp.controller.util.WHATSAPP_PACKAGES
import com.coolappstore.everdialer.by.svhp.controller.util.openWhatsAppChat
import com.coolappstore.everdialer.by.svhp.controller.util.openTelegramChat
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startWhatsAppVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVoiceCall
import com.coolappstore.everdialer.by.svhp.controller.util.startTelegramVideoCall
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.controller.util.placeCallHonoringContactSim
import com.coolappstore.everdialer.by.svhp.view.components.AppQuickActionsDialog
import com.coolappstore.everdialer.by.svhp.view.components.CallChatViaOverlay
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.view.components.SimPickerDialog
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.getSearchFilterState
import com.coolappstore.everdialer.by.svhp.view.components.tiles.SingleTile
import com.coolappstore.everdialer.by.svhp.view.components.tiles.TileGroup
import com.coolappstore.everdialer.by.svhp.view.screen.settings.AddMode
import com.coolappstore.everdialer.by.svhp.view.screen.settings.FakeCallAddSheet
import com.coolappstore.everdialer.by.svhp.controller.UssdRepository
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HiddenContactsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.Locale
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.drawPlainBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.blur
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.lens
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.colorControls
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import androidx.activity.compose.BackHandler
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop

/**
 * Keeps the in-progress dialed digits alive across the dialpad bottom sheet being dismissed
 * (e.g. by swiping down on the drag handle) and reopened. The sheet's composable is fully torn
 * down on dismiss, so a plain `remember` loses the typed number; this small in-memory holder
 * survives that as long as the process is alive, matching what users expect from a dialer.
 */
private object DialpadDraftHolder {
    var pendingNumber: String = ""
}

/** Extra (non-contact) result rows shown below matched contacts in the dialpad's search-results
 *  panel, gated by the persisted "Filter" checkboxes (see [SearchFilterButton]). */
private sealed class DialpadExtraResult {
    data class NonContact(val entry: CallLogEntry) : DialpadExtraResult()
    data class Recording(val item: RecordingItem) : DialpadExtraResult()
    data class ContactNote(val note: com.coolappstore.everdialer.by.svhp.controller.util.NoteEntry) : DialpadExtraResult()
    data class RecordingNote(val item: RecordingItem) : DialpadExtraResult()
}

@Composable
private fun DialpadExtraResultTile(
    result: DialpadExtraResult,
    onCallNumber: (String) -> Unit,
    onOpenContactInfo: (String) -> Unit
) {
    // Tapping the row body dials directly only when Settings → Call Settings → "Direct Call on
    // Tap" is on; otherwise it opens Contact Info instead (see onCallNumber passed in by the
    // caller). Tapping the avatar/pfp always opens the contact info page for that number
    // (pre-filled as "Unknown"/add-new since it isn't a saved contact yet) regardless of that
    // setting — matching how saved-contact rows in this same search results list already treat
    // pfp taps (onAvatarClick -> ContactDetailsScreen).
    when (result) {
        is DialpadExtraResult.NonContact -> SingleTile(
            title = result.entry.name?.ifEmpty { result.entry.number } ?: result.entry.number,
            subtitle = if (result.entry.name.isNullOrEmpty() || result.entry.name == result.entry.number) null else result.entry.number,
            icon = Icons.Default.Person,
            phoneNumber = result.entry.number,
            onAvatarClick = { onOpenContactInfo(result.entry.number) },
            onClick = { onCallNumber(result.entry.number) }
        )
        is DialpadExtraResult.Recording -> SingleTile(
            title = result.item.contactName?.ifBlank { result.item.phoneNumber } ?: result.item.phoneNumber,
            subtitle = result.item.phoneNumber,
            icon = Icons.Default.Mic,
            phoneNumber = result.item.phoneNumber,
            onAvatarClick = { if (result.item.phoneNumber.isNotBlank()) onOpenContactInfo(result.item.phoneNumber) },
            onClick = { if (result.item.phoneNumber.isNotBlank()) onCallNumber(result.item.phoneNumber) }
        )
        is DialpadExtraResult.ContactNote -> SingleTile(
            title = result.note.contactName.ifBlank { result.note.phoneNumber.ifBlank { "Unknown" } },
            subtitle = result.note.content,
            icon = Icons.Default.StickyNote2,
            phoneNumber = result.note.phoneNumber,
            onAvatarClick = { if (result.note.phoneNumber.isNotBlank()) onOpenContactInfo(result.note.phoneNumber) },
            onClick = { if (result.note.phoneNumber.isNotBlank()) onCallNumber(result.note.phoneNumber) }
        )
        is DialpadExtraResult.RecordingNote -> SingleTile(
            title = result.item.contactName?.ifBlank { result.item.phoneNumber } ?: result.item.phoneNumber,
            subtitle = result.item.noteText,
            icon = Icons.Default.Mic,
            phoneNumber = result.item.phoneNumber,
            onAvatarClick = { if (result.item.phoneNumber.isNotBlank()) onOpenContactInfo(result.item.phoneNumber) },
            onClick = { if (result.item.phoneNumber.isNotBlank()) onCallNumber(result.item.phoneNumber) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun DialPadScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
    initialNumber: String? = null
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )
    val sheetScope = rememberCoroutineScope()

    // See DialPadContent's `closing` param doc — flipping this (rather than clearing focus from
    // out here) is what actually reaches the search field's real focus manager, since the sheet
    // renders in its own separate Dialog window.
    var dialpadClosing by remember { mutableStateOf(false) }
    // Catch a swipe-down or scrim-tap dismiss the moment the drag/tap decides to close the sheet
    // (sheetState.targetValue flips to Hidden), not only once onDismissRequest fires after the
    // sheet's own hide animation has already finished playing — otherwise the search field keeps
    // its focus and the keyboard stays up for that whole animation before finally hiding, which is
    // exactly the "sometimes" glitch (X button / back already set dialpadClosing immediately and
    // don't have this gap; only swipe/scrim-tap went through the delayed onDismissRequest path).
    //
    // sheetState.targetValue actually starts at Hidden too, for the single frame before the sheet's
    // own internal LaunchedEffect calls show() to open it — so naively treating "target == Hidden"
    // as "closing" misfired on that very first frame and permanently blocked the search field from
    // ever focusing (via DialPadContent's `closing`-gated focusProperties) even on a fresh open.
    // hasOpenedOnce guards against that: we only start treating a Hidden target as a real close
    // once the sheet has actually reached a non-Hidden target at least once first.
    var hasOpenedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue }.collect { target ->
            if (target != SheetValue.Hidden) {
                hasOpenedOnce = true
            } else if (hasOpenedOnce) {
                dialpadClosing = true
            }
        }
    }

    // Lock the window so the keyboard never pushes the bottom sheet up.
    // WindowCompat.setDecorFitsSystemWindows(false) in MainActivity normally causes
    // the sheet to resize with the IME; overriding softInputMode here prevents that.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val prevMode = window?.attributes?.softInputMode ?: 0
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            window?.setSoftInputMode(prevMode)
        }
    }

    // ModalBottomSheet.animateTo(targetValue, animationSpec) is internal, so we can't hand it a
    // custom spec to slow down its own animation. Instead we drive a manual slide + scrim fade
    // over the sheet content ourselves — slow and smooth — then finish the real dismiss/
    // navigation once that completes. Animating the scrim's alpha down alongside the slide (not
    // just translating the content) avoids the dim background cutting out abruptly the instant
    // the Dialog is torn down at the end, leaving the same (quicker) swipe-to-dismiss untouched.
    val closeProgress = remember { Animatable(0f) }
    val slowSlideSpec = tween<Float>(durationMillis = 650, easing = FastOutSlowInEasing)
    // Fixed dp offsets don't clear the screen on taller devices, so the sheet gets torn down
    // mid-slide and appears to vanish abruptly. Basing it on the actual screen height (plus a
    // buffer for the status/nav bars) guarantees it's fully off-screen before removal.
    val dialpadConfiguration = LocalConfiguration.current
    val slideDistance = dialpadConfiguration.screenHeightDp.dp + 150.dp

    // ModalBottomSheet fires onDismissRequest itself once sheetState reaches Hidden (e.g. after
    // a swipe-down completes). Without this guard, that auto-fire would also call navigateUp()
    // right after we navigate forward to Contact Info, popping DialPadScreen off the back stack —
    // so pressing back from Contact Info would skip past the dialpad entirely instead of
    // returning to it.
    var didNavigateAway by remember { mutableStateOf(false) }

    fun finishDismiss() {
        if (!didNavigateAway) navigator.navigateUp()
    }

    // Used by swipe-down / scrim tap / predictive back — sheetState has already played its own
    // (quicker) hide animation by the time onDismissRequest fires, so just finish up.
    fun animateDismiss() {
        if (didNavigateAway) return
        dialpadClosing = true
        finishDismiss()
    }

    // Used by the X button — plays our own slow, smooth slide-down instead of relying on the
    // sheet's quicker default.
    fun closeWithSlowAnimation() {
        if (didNavigateAway) return
        dialpadClosing = true
        sheetScope.launch {
            closeProgress.animateTo(1f, slowSlideSpec)
        }.invokeOnCompletion {
            finishDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { animateDismiss() },
        sheetState = sheetState,
        // The default containerColor/shape/dragHandle paint their own background surface that
        // sits BEHIND whatever we put in the content slot below. Animating only our content with
        // Modifier.offset moved the buttons/text but left that background surface fixed in
        // place, so the background appeared static while only the elements slid down. Making the
        // sheet itself fully transparent and drawing our own background + handle inside the
        // offset Box (below) means the background now moves together with the content as one
        // unit during the slide.
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        // No dim behind the sheet.
        scrimColor = Color.Transparent,
        // Stop the sheet from reserving its own system-bar inset padding around the content —
        // that reserved space stayed transparent once containerColor became Transparent, showing
        // as a gap between the sheet's visible bottom and the actual screen edge. We apply the
        // navigation-bar inset ourselves inside the Surface below instead, so the background
        // extends all the way to the screen edge and only the content is padded above it.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = slideDistance * closeProgress.value),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            // System back should feel identical to tapping the X — the same slow, smooth
            // slide-down — instead of the sheet's own quicker default dismiss.
            BackHandler(enabled = true) { closeWithSlowAnimation() }
            Column(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(width = 36.dp, height = 4.dp)
                    ) {}
                }
                DialPadContent(
                    initialNumber = initialNumber,
                    navigator = navigator,
                    onDismiss = { closeWithSlowAnimation() },
                    closing = dialpadClosing,
                    // Because this screen's content is a ModalBottomSheet (its own Dialog window), the
                    // NavHost's normal slide/fade destination transition can't visually animate it — a
                    // Dialog window ignores the parent's transition offsets. So instead of navigating
                    // immediately (which just swapped destinations with no visible motion), we play our
                    // own slow, smooth slide-down first, then navigate once it's finished — giving
                    // Contact Info a gentle reveal instead of popping in instantly.
                    onNavigateToContact = { contactId, phoneNumber ->
                        didNavigateAway = true
                        navigator.navigate(ContactDetailsScreenDestination(contactId = contactId, phoneNumber = phoneNumber))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadContent(
    initialNumber: String? = null,
    navigator: DestinationsNavigator? = null,
    onDismiss: (() -> Unit)? = null,
    showHeader: Boolean = false,
    /** Optional override for opening Contact Info (e.g. to play a bottom-sheet hide animation
     *  first when this content is hosted inside [DialPadScreen]'s ModalBottomSheet). Falls back
     *  to a plain [navigator] navigation when this content isn't sheet-hosted. */
    onNavigateToContact: ((contactId: String?, phoneNumber: String?) -> Unit)? = null,
    /** Flips to true right when the hosting sheet (DialPadScreen / Recents' inline quick-dial
     *  sheet) starts closing, for any dismiss path — X button, swipe, scrim tap, or back. Must be
     *  read here rather than have the caller clear focus itself: when this content is hosted
     *  inside a ModalBottomSheet, the sheet renders in its own Dialog window with its own
     *  LocalFocusManager/LocalSoftwareKeyboardController, separate from the one in the composable
     *  that hosts the ModalBottomSheet call — clearing focus/hiding the keyboard from out there
     *  silently misses the search field's real owner, which is what caused the keyboard to
     *  glitch/flash back open only sometimes (whenever that mismatch happened to matter). */
    closing: Boolean = false
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // See the `closing` param doc above — this is the correctly-scoped focus manager/keyboard
    // controller for whichever window this content actually lives in.
    LaunchedEffect(closing) {
        if (closing) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            // Compose's keyboardController.hide() goes through the IME's WindowInsetsController
            // too, but some OEM keyboards/Android builds don't reliably honor it. Going straight
            // through WindowInsetsControllerCompat on the real Android View is the lower-level,
            // more forceful equivalent and catches the cases the Compose call alone misses.
            ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
        }
    }
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    // Settings → App Settings → Call Settings → "Direct Call on Tap". When off, tapping a
    // search-result row (saved contact or non-contact) should open Contact Info instead of
    // placing a call directly — same behaviour Recents/Favorites already apply.
    val directCallOnTap = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_DIRECT_CALL_ON_TAP, true) }
    // Settings → App Settings → Appearance → "Dialpad Memory". When on (default), the typed
    // number survives the Dialpad being closed or a call being placed. When off, the Dialpad is
    // wiped clean in both of those cases.
    val dialpadMemoryEnabled = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, true) }

    val allContacts by contactsVM.allContacts.collectAsState()
    fun navigateToContact(contactId: String? = null, phoneNumber: String? = null) {
        if (onNavigateToContact != null) onNavigateToContact(contactId, phoneNumber)
        else navigator?.navigate(ContactDetailsScreenDestination(contactId = contactId, phoneNumber = phoneNumber))
    }
    var number by remember {
        mutableStateOf(initialNumber ?: if (dialpadMemoryEnabled) DialpadDraftHolder.pendingNumber else "")
    }
    // Where new digits get inserted / backspace deletes from. Defaults to the end of the number
    // (normal typing behaviour), but the user can tap anywhere in the number to move it, so they
    // can fill in a missing digit in the middle without having to delete and retype everything.
    var cursorPosition by remember { mutableIntStateOf(number.length) }

    // Route every edit through these so the cursor position stays correct and consistent no
    // matter where the edit originates from (dialpad keys, backspace, paste, clipboard banner,
    // clearing on secret-code detection, etc.)
    fun insertAtCursor(text: String) {
        val at = cursorPosition.coerceIn(0, number.length)
        number = number.substring(0, at) + text + number.substring(at)
        cursorPosition = at + text.length
    }
    fun backspaceAtCursor() {
        val at = cursorPosition.coerceIn(0, number.length)
        if (at > 0) {
            number = number.removeRange(at - 1, at)
            cursorPosition = at - 1
        }
    }
    fun replaceNumber(text: String) {
        number = text
        cursorPosition = text.length
    }
    // Keep the draft holder in sync so dismissing the sheet (including swipe-down-to-dismiss)
    // and reopening it restores whatever digits were typed, instead of clearing them — but only
    // when Dialpad Memory is on. When it's off, never let anything typed reach the holder, so a
    // stale number can't leak back in the next time the Dialpad opens.
    LaunchedEffect(number, dialpadMemoryEnabled) {
        DialpadDraftHolder.pendingNumber = if (dialpadMemoryEnabled) number else ""
    }
    // Dialpad Memory off: wipe the typed number the moment the hosting sheet starts closing
    // (X button, swipe, scrim tap, or back), for any dismiss path.
    LaunchedEffect(closing, dialpadMemoryEnabled) {
        if (closing && !dialpadMemoryEnabled) {
            replaceNumber("")
        }
    }
    // Dialpad Memory off: wipe the typed number once a call has actually been dispatched, so
    // reopening the Dialpad (without the sheet ever fully closing, e.g. from Recents) also
    // starts blank.
    fun forgetNumberIfMemoryDisabled() {
        if (!dialpadMemoryEnabled) replaceNumber("")
    }

    // Collect USSD / MMI responses from CallService and show inline dialog
    val ussdResult by UssdRepository.response.collectAsState()
    DisposableEffect(Unit) { onDispose { UssdRepository.clear() } }

    ussdResult?.let { (request, response) ->
        AlertDialog(
            onDismissRequest = { UssdRepository.clear() },
            title = {
                Text(
                    request,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = { Text(response, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { UssdRepository.clear() }) {
                    Text("OK")
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    val soundPool = remember { buildDtmfSoundPool(context) }

    // Filter state for the search bar's Filter button (Contacts / Non contacts / Contact
    // notes / Recording notes), persisted so it's remembered across app restarts.
    val settingsVerForFilter by prefs.settingsChanged.collectAsState()
    val searchFilterState = remember(settingsVerForFilter) { prefs.getSearchFilterState() }
    val callLogVM: CallLogViewModel = koinActivityViewModel()
    val callLogsForSearch by callLogVM.allCallLogs.collectAsState()
    val recordingsVM: HomeViewModel = viewModel()
    val recordingsForSearch by recordingsVM.allRecordings.collectAsState()

    // Extra (non-contact) results shown below matched contacts while searching — numbers from
    // the call log that aren't saved contacts, plus contact/recording notes whose content
    // matches. The "Non contacts" section reacts to *either* a typed text query (searchQuery)
    // *or* digits typed on the dialpad itself (number) so unsaved numbers from call history show
    // up while dialing a number too, not just while using the text search field. The name/note
    // based sections (contact notes, recordings, recording notes) stay text-query-only since
    // there's nothing meaningful to match against a bare typed number for those.
    val extraSearchResults = remember(searchQuery, number, callLogsForSearch, recordingsForSearch, searchFilterState) {
        val q = searchQuery.trim()
        val numberQuery = number.trim()
        val effectiveQuery = q.ifEmpty { numberQuery }
        if (effectiveQuery.isEmpty()) emptyList()
        else {
            val results = mutableListOf<DialpadExtraResult>()
            if (searchFilterState.nonContacts) {
                val seen = LinkedHashMap<String, CallLogEntry>()
                callLogsForSearch.asSequence()
                    .filter { it.contactId.isNullOrBlank() }
                    .forEach { entry ->
                        val key = normalizeNumberDigits(entry.number).filter { it.isDigit() }.takeLast(9)
                            .ifBlank { entry.number }
                        seen.putIfAbsent(key, entry)
                    }
                seen.values.filter { entry ->
                    entry.number.replace(" ", "").contains(effectiveQuery.replace(" ", "")) ||
                            (q.isNotEmpty() && entry.isCallerIdName && (entry.name?.contains(q, ignoreCase = true) == true))
                }.take(3).forEach { results.add(DialpadExtraResult.NonContact(it)) }
            }
            // Notes and call recordings are intentionally excluded from the dialpad's search
            // list — it only ever shows contacts, phone numbers, or unknown (non-contact)
            // numbers. Notes/recordings still show up in the dedicated Search screen.
            results
        }
    }


    val t9Enabled = prefs.getBoolean(PreferenceManager.KEY_T9_DIALING, true)
    var showSimPicker by remember { mutableStateOf(false) }
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }
    var pendingSearchCallNumber by remember { mutableStateOf<String?>(null) }

    // "Fake Call" entry in the long-press context menu (toggled from the Fake Call screen)
    val fakeCallInContextMenu = remember(settingsState) {
        prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
    }
    var showFakeCallSheet by remember { mutableStateOf(false) }

    // Helper: place a call respecting the contact's own "Choose Sim" preference (falling back to
    // the app-wide default SIM setting), same resolution as Contact Info → Choose Sim. contactKey
    // defaults to a saved contact matching [num], or the raw number itself when there's no match.
    fun placeCallWithSimPreference(num: String, contactKey: String = allContacts.firstOrNull { c -> c.phoneNumbers.any { numbersLikelyMatch(it, num) } }?.id ?: num) {
        var placed = true
        placeCallHonoringContactSim(context, prefs, contactKey, num) {
            placed = false
            pendingSearchCallNumber = num
            showSimPicker = true
        }
        if (placed) forgetNumberIfMemoryDisabled()
    }

    val clipText = remember {
        clipboard.getText()?.text?.filter { it.isDigit() || it == '+' || it == '*' || it == '#' } ?: ""
    }
    var showClipboardBanner by remember { mutableStateOf(clipText.length in 7..15) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    // "Call/Chat Via" app picker (WhatsApp/Telegram), shown from the call button's long-press
    // and the long-press overflow menu — see CallChatViaOverlay for what happens after a pick.
    var showAppPicker by remember { mutableStateOf(false) }
    var searchFieldFocused by remember { mutableStateOf(false) }

    // When search field is focused, intercept back press to dismiss keyboard and restore dialpad
    BackHandler(enabled = searchFieldFocused) {
        focusManager.clearFocus()
        searchQuery = ""
    }
    var openDialpadDefault by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, true))
    }

    // Search results from search bar
    val searchResults by remember(searchQuery, number, allContacts, t9Enabled, searchFilterState) {
        derivedStateOf {
            val q = searchQuery.trim()
            val n = number
            if (!searchFilterState.contacts) emptyList()
            else when {
                q.isNotEmpty() -> allContacts.filter { c ->
                    c.name.contains(q, ignoreCase = true) ||
                    c.phoneNumbers.any { it.contains(q) }
                }.take(5)
                n.isNotEmpty() -> allContacts.filter { contact ->
                    val matchesNumber = contact.phoneNumbers.any { it.replace(" ", "").contains(n) }
                    val matchesName = if (t9Enabled) {
                        val t9Name = T9Matcher.convertNameToT9(contact.name)
                        t9Name.contains(n)
                    } else false
                    matchesNumber || matchesName
                }.take(3)
                else -> emptyList()
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (number.isNotEmpty()) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "numberScale"
    )

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true) {
            val numToCall = pendingSearchCallNumber ?: number
            pendingSearchCallNumber = null
            val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            if (hasPhoneState) {
                placeCallWithSimPreference(numToCall)
            } else {
                makeCall(context, numToCall)
                forgetNumberIfMemoryDisabled()
            }
        } else {
            pendingSearchCallNumber = null
        }
    }

    // Auto-process Android hidden/secret codes and MMI codes as the user types
    fun processSecretCodeIfNeeded(input: String): Boolean {
        val code = input.trim()
        if (code.length < 3) return false

        // ── Pattern 0: *#06# (IMEI) / *#07# (SAR info)  ─────────────────────────────
        // These look like MMI/USSD codes (they even used to be handled that way in this
        // app), but they are NOT network requests at all — dialing them out via
        // TelecomManager.placeCall() sends them to the SIM/carrier as if they were a real
        // number, which is exactly what was causing the SIM-picker prompt / failed "call"
        // instead of the expected system info screen. Stock dialers intercept these two
        // locally, before ever touching Telecom, and this app now does the same.
        //
        // Note: reading the real IMEI via TelephonyManager.getImei() requires
        // READ_PRIVILEGED_PHONE_STATE on Android 10+, which only privileged system apps
        // can hold — being the default dialer does not grant it. So instead of showing a
        // (permission-blocked) in-app dialog, we open Android's own "About phone → IMEI
        // information" settings screen, which is what actually has that privilege and is
        // guaranteed to exist on every device.
        if (code == "*#06#") {
            try {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
            return true
        }
        if (code == "*#07#") {
            // "SAR information" doesn't have one stable intent action across all OEMs/OS
            // versions the way IMEI does, so try the couple of known ones first and fall
            // back to the general device-info settings screen (still a real system menu,
            // not a failed call) if none of them resolve on this device.
            val sarActions = listOf(
                "android.settings.SAR_INFORMATION",
                "android.settings.RF_EXPOSURE_SETTINGS",
                android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
            )
            for (action in sarActions) {
                try {
                    context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    break
                } catch (_: Exception) { /* try next action */ }
            }
            return true
        }

        // ── Pattern 1: *#*#DIGITS#*#*  (Android secret activity codes, e.g. testing menu) ──
        // These end with #*#* so a plain endsWith("#") check misses them entirely
        val secretMatch = Regex("^\\*#\\*#(\\d+)#\\*#\\*$").find(code)
        if (secretMatch != null) {
            val digits = secretMatch.groupValues[1]
            // Fire every known delivery mechanism unconditionally rather than only
            // falling back to the classic broadcasts when sendDialerSpecialCode() throws.
            // sendDialerSpecialCode() is a fire-and-forget AIDL call — it reports no
            // success/failure back to us, so "didn't throw" is not proof the code was
            // actually delivered anywhere. Different codes are ultimately owned by
            // different apps (Settings' Testing menu for 4636, Calendar Storage for 225,
            // Play Services for 426, an OEM diagnostics app for the hardware-test codes,
            // etc.) and some only listen on one of these two channels, so sending both
            // maximizes the chance whichever app owns this particular code receives it.
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                telephonyManager?.sendDialerSpecialCode(digits)
            } catch (_: Exception) {}
            val uri = android.net.Uri.parse("android_secret_code://$digits")
            try {
                context.sendBroadcast(
                    Intent("android.provider.Telephony.SECRET_CODE", uri).apply {
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                )
            } catch (_: Exception) {}
            try {
                context.sendBroadcast(
                    Intent("android.telephony.action.SECRET_CODE", uri).apply {
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                )
            } catch (_: Exception) {}
            return true
        }

        // ── Pattern 2: USSD / MMI codes  ──────────────────────────────────────────
        // *124#  *123#  *199#  ##002#  *21*N#  *#21#  *#62#
        // (*#06# and *#07# are intercepted above and never reach this branch.)
        val decoded = try { android.net.Uri.decode(code) } catch (_: Exception) { code }
        if (!((decoded.startsWith("*") || decoded.startsWith("#")) &&
              decoded.endsWith("#"))) return false

        // Dial USSD/MMI codes exactly like a normal call via TelecomManager.placeCall()
        // (same approach RivoPhoneApp uses). The carrier's telephony stack recognises the
        // MMI/USSD prefix itself and drives the whole USSD session — including any
        // interactive multi-step menu — through Android's own native USSD dialog, and
        // placing a real call also lets CallService's connection-event listener (see
        // isUssdNumber() below) pick up and surface the response inline when the
        // carrier/OEM supplies one. This is far more reliable than
        // TelephonyManager.sendUssdRequest(), which only supports a single
        // non-interactive request/response and fails outright on many devices, carriers,
        // and dual-SIM setups.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            placeCallWithSimPreference(decoded)
        } else {
            pendingSearchCallNumber = decoded
            callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
        }
        return true
    }


    fun initiateCall(num: String) {
        val cleanNum = num.trim()
        if (cleanNum.isEmpty() || cleanNum == "Unknown") return
        // Check Contacts Hider secret code first
        val secretCode = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_CODE, "") ?: ""
        if (secretCode.isNotEmpty() && cleanNum == secretCode) {
            replaceNumber("")
            navigator?.navigate(HiddenContactsScreenDestination)
            return
        }
        // MMI/USSD codes (*#06#, *#*#4636#*#*, *21*1234#, *124#, ##002#, etc.) are handled
        // by processSecretCodeIfNeeded which uses telecomManager.placeCall() with the raw URI
        // for proper carrier-stack routing without looping back to this app.
        if (processSecretCodeIfNeeded(cleanNum)) {
            replaceNumber("")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            if (hasPhoneState) {
                placeCallWithSimPreference(cleanNum)
            } else {
                makeCall(context, cleanNum)
                forgetNumberIfMemoryDisabled()
            }
        } else {
            pendingSearchCallNumber = cleanNum
            callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
        }
    }

    if (showSimPicker) {
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, pendingSearchCallNumber ?: number, handle)
                pendingSearchCallNumber = null
                showSimPicker = false
                forgetNumberIfMemoryDisabled()
            }
        )
    }

    if (showFakeCallSheet) {
        FakeCallAddSheet(
            mode = AddMode.Number,
            initialNumber = number,
            onDismiss = { showFakeCallSheet = false },
            onSave = { entry, exactTriggerOverride ->
                FakeCallManager.addEntry(context, prefs, entry, exactTriggerOverride)
                showFakeCallSheet = false
            }
        )
    }

    // "Call/Chat Via" — WhatsApp/Telegram app picker followed by the Chat/Voice Call/Video Call
    // popup, shared with every other long-press context menu in the app (see CallChatViaOverlay).
    // Triggered from the call button's long-press and from the "Call/Chat Via" overflow menu item.
    // The Dialpad additionally opts into Google Meet (below Telegram) and Fake Call (below Google
    // Meet), since the call button's long-press is the one place those two also make sense.
    // If the dialed number matches a saved contact, offer every number saved on that contact
    // (e.g. one with a country code, one without) instead of just the one currently typed.
    val callChatViaContact = remember(number, allContacts) {
        if (number.isBlank()) null else allContacts.find { c -> c.phoneNumbers.any { numbersLikelyMatch(number, it) } }
    }
    CallChatViaOverlay(
        phoneNumber = number.takeIf { it.isNotEmpty() },
        phoneNumbers = callChatViaContact?.phoneNumbers?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(number.takeIf { it.isNotEmpty() }),
        showPicker = showAppPicker,
        onPickerDismiss = { showAppPicker = false },
        showGoogleMeet = true,
        showFakeCall = fakeCallInContextMenu,
        onFakeCall = { showFakeCallSheet = true }
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // Landscape: side-by-side layout — left=search+search results, right=dialpad keys+number+actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left panel: search bar + results only
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            // Once the hosting sheet starts closing, permanently refuse focus —
                            // stronger than reactively clearing/hiding after the fact, since it
                            // guarantees the keyboard can't be re-triggered by window refocus or
                            // any other later event during the close animation/teardown.
                            .focusProperties { canFocus = !closing },
                        placeholder = { Text("Search contacts...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            showKeyboardOnFocus = false
                        )
                    )

                    // Closes the whole dialpad UI, separate from the search field's own "clear text" X.
                    if (onDismiss != null) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close dialpad", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Number display — below search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (number.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DialpadNumberDisplay(
                        number = number,
                        fontSize = if (number.length > 11) 24 else 30,
                        cursorPosition = cursorPosition,
                        onCursorPositionChange = { cursorPosition = it }
                    )
                }

                // Search results
                AnimatedVisibility(
                    visible = searchResults.isNotEmpty() || extraSearchResults.isNotEmpty(),
                    enter = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                            expandVertically(tween(420, easing = FastOutSlowInEasing)),
                    exit  = fadeOut(tween(280, easing = FastOutLinearInEasing)) +
                            shrinkVertically(tween(320, easing = FastOutLinearInEasing))
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            searchResults.forEach { contact ->
                                SingleTile(
                                    title    = contact.name,
                                    subtitle = contact.phoneNumbers.firstOrNull(),
                                    photoUri = contact.photoUri,
                                    onAvatarClick = {
                                        navigateToContact(contactId = contact.id)
                                    },
                                    onClick  = {
                                        if (directCallOnTap) {
                                            val num = contact.phoneNumbers.firstOrNull() ?: return@SingleTile
                                            initiateCall(num)
                                        } else {
                                            navigateToContact(contactId = contact.id)
                                        }
                                    }
                                )
                            }
                            extraSearchResults.forEach { extra ->
                                DialpadExtraResultTile(
                                    result = extra,
                                    onCallNumber = { num ->
                                        if (directCallOnTap) initiateCall(num) else navigateToContact(phoneNumber = num)
                                    },
                                    onOpenContactInfo = { num ->
                                        navigateToContact(phoneNumber = num)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }

            // Right panel: dialpad keys + action buttons below
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))
                    val subKeys = mapOf("1" to "   ","2" to "ABC","3" to "DEF","4" to "GHI","5" to "JKL","6" to "MNO","7" to "PQRS","8" to "TUV","9" to "WXYZ","0" to "+")
                    keys.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { key ->
                                DialPadKey(
                                    number = key,
                                    letters = subKeys[key] ?: "",
                                    soundPool = soundPool,
                                    context = context,
                                    onClick = { digit ->
                                        insertAtCursor(digit)
                                    },
                                    onLongClick = if (key == "0") ({ insertAtCursor("+") }) else null,
                                    compact = true
                                )
                            }
                        }
                    }
                    // Action row — below the keys
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FadeScaleBox(visible = number.isNotEmpty()) {
                            DialerActionExpressive(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                        type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
                                    }
                                    context.startActivity(intent)
                                },
                                icon = Icons.Default.PersonAdd,
                                contentDescription = "Add Contact",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                        DialerActionExpressive(
                            onClick = {
                                if (number.isNotEmpty()) {
                                    initiateCall(number)
                                }
                            },
                            onLongClick = if (number.isNotEmpty()) ({ showAppPicker = true }) else null,
                            icon = Icons.Default.Call,
                            contentDescription = "Call",
                            containerColor = Color(0xFF34A853),
                            contentColor = Color.White,
                            modifier = Modifier.width(96.dp).height(64.dp),
                            isLarge = true,
                            isCallButton = true
                        )
                        BackspaceActionButton(
                            number = number,
                            onBackspace = { backspaceAtCursor() },
                            onClear = { replaceNumber("") }
                        )
                    }
                }
            }
        }
    } else {
        // Prevent keyboard from auto-opening on composition
        LaunchedEffect(Unit) { focusManager.clearFocus() }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Layout: search bar fixed at top, scrollable middle (results/pills/clipboard),
        // dialpad card fixed at bottom. Nothing moves when results appear.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {

        // ── Search bar — always visible at top of screen ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    // Once the hosting sheet starts closing, permanently refuse focus — stronger
                    // than reactively clearing/hiding after the fact, since it guarantees the
                    // keyboard can't be re-triggered by window refocus or any other later event
                    // during the close animation/teardown.
                    .focusProperties { canFocus = !closing }
                    .onFocusChanged { focusState -> searchFieldFocused = focusState.isFocused },
                placeholder = { Text("Search contacts...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    showKeyboardOnFocus = false
                )
            )

            // Closes the whole dialpad UI — separate from the search field's own "clear text" X,
            // which only appears once text is typed and just empties the field instead.
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close dialpad", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Middle section: results / pills / clipboard (scrollable, fills space) ──
        // Swallow leftover scroll delta while the list is mid-scroll so a fast fling back up to
        // the top result doesn't bleed residual velocity into the parent ModalBottomSheet and
        // get misread as swipe-to-dismiss on that same gesture. Only once the finger is lifted
        // and a fresh gesture starts while already resting at the top do we allow that next
        // gesture's overscroll to reach the sheet again — so a second, deliberate pull is needed
        // to close it, exactly like before.
        val dialpadResultsScrollState = rememberScrollState()
        var canDismissAtTop by remember { mutableStateOf(true) }
        val swallowOverscroll = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                ): Offset {
                    if (dialpadResultsScrollState.value > 0) return available
                    return if (canDismissAtTop) Offset.Zero else available
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.changedToDown() }) {
                                canDismissAtTop = dialpadResultsScrollState.value == 0
                            }
                        }
                    }
                }
                .nestedScroll(swallowOverscroll)
                .verticalScroll(dialpadResultsScrollState),
            verticalArrangement = Arrangement.Top
        ) {

        // Search results
        AnimatedVisibility(
            visible = searchResults.isNotEmpty() || extraSearchResults.isNotEmpty(),
            enter = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                    expandVertically(tween(420, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(280, easing = FastOutLinearInEasing)) +
                    shrinkVertically(tween(320, easing = FastOutLinearInEasing))
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        searchResults.forEach { contact ->
                            SingleTile(
                                title    = contact.name,
                                subtitle = contact.phoneNumbers.firstOrNull(),
                                photoUri = contact.photoUri,
                                onAvatarClick = {
                                    navigateToContact(contactId = contact.id)
                                },
                                onClick  = {
                                    if (directCallOnTap) {
                                        val num = contact.phoneNumbers.firstOrNull() ?: return@SingleTile
                                        initiateCall(num)
                                    } else {
                                        navigateToContact(contactId = contact.id)
                                    }
                                }
                            )
                        }
                        extraSearchResults.forEach { extra ->
                            DialpadExtraResultTile(
                                    result = extra,
                                    onCallNumber = { num ->
                                        if (directCallOnTap) initiateCall(num) else navigateToContact(phoneNumber = num)
                                    },
                                    onOpenContactInfo = { num ->
                                        navigateToContact(phoneNumber = num)
                                    }
                                )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = number.isNotEmpty() && searchResults.isEmpty() && extraSearchResults.isEmpty() && searchQuery.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, number)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create contact", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, number)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to contact", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // Clipboard banner
        AnimatedVisibility(
            visible = showClipboardBanner,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Text(text = clipText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                    TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); replaceNumber(clipText); showClipboardBanner = false }) {
                        Text("Use", color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showClipboardBanner = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        } // end scrollable middle Column

        // ── Dialpad card — hides with animation when search is active ──
        val showDialpad = !searchFieldFocused && searchQuery.isEmpty()
        AnimatedVisibility(
            visible = showDialpad,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing))
        ) {

        Spacer(modifier = Modifier.height(2.dp))

        // ── Dialpad card — always at bottom, never moves ───────────────
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            // Scale based on the SMALLER of width-derived and height-derived factors
            // so the dialpad always fits on screen regardless of device size.
            val refWidth = 360f
            val availableWidth = maxWidth.value
            val widthScale = (availableWidth / refWidth).coerceIn(0.6f, 1.4f)

            // Height budget: total screen height minus search bar (~64dp) minus spacing (~24dp)
            // The dialpad card needs: header(~44dp) + 4 key rows + action row(~60dp) + padding(~28dp)
            // Reference key height = 54dp, so 4 rows = 216dp + overhead ~140dp = ~356dp total card
            // (kept intentionally compact so the scrollable search-results area above gets more room)
            val cardHeightBudget = (screenHeight.value - 64f - 24f).coerceAtLeast(200f)
            val refCardHeight = 400f
            val heightScale = (cardHeightBudget / refCardHeight).coerceIn(0.55f, 1.4f)

            val scaleFactor = minOf(widthScale, heightScale)

            val keyWidth: Dp  = (98 * scaleFactor).dp
            val keyHeight: Dp = (62 * scaleFactor).dp
            val actionSize: Dp = (58 * scaleFactor).dp
            val callW: Dp  = (102 * scaleFactor).dp
            val callH: Dp  = (66 * scaleFactor).dp
            val keySpacing: Dp = (7 * scaleFactor).dp

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = (13 * scaleFactor).coerceIn(5f, 13f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(keySpacing)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = if (number.isEmpty()) 58.dp else 0.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (number.isNotEmpty())
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showOverflowMenu = true
                                    }
                                )
                                .padding(vertical = 9.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DialpadNumberDisplay(
                                number = number,
                                fontSize = ((if (number.length > 11) 27 else 34) * scaleFactor).coerceIn(17f, 38f).toInt(),
                                cursorPosition = cursorPosition,
                                onCursorPositionChange = { cursorPosition = it },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showOverflowMenu = true
                                }
                            )
                        }
                        RivoDropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            if (number.isNotEmpty()) {
                                RivoDropdownMenuItem(
                                    text     = "Copy",
                                    icon     = Icons.Default.ContentCopy,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick  = {
                                        clipboard.setText(AnnotatedString(number))
                                        showOverflowMenu = false
                                    }
                                )
                            }
                            val pasteText = clipboard.getText()?.text
                                ?.filter { it.isDigit() || it == '+' || it == '*' || it == '#' } ?: ""
                            if (pasteText.isNotEmpty()) {
                                RivoDropdownMenuItem(
                                    text     = "Paste",
                                    icon     = Icons.Default.ContentPaste,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick  = {
                                        replaceNumber(pasteText)
                                        showOverflowMenu = false
                                    }
                                )
                            }
                            if (fakeCallInContextMenu) {
                                RivoDropdownMenuItem(
                                    text     = "Fake Call",
                                    icon     = Icons.Outlined.PhoneCallback,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick  = {
                                        showOverflowMenu = false
                                        showFakeCallSheet = true
                                    }
                                )
                            }
                            if (number.isNotEmpty()) {
                                RivoDropdownMenuItem(
                                    text     = "Call/Chat Via",
                                    icon     = Icons.AutoMirrored.Filled.Chat,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick  = {
                                        showOverflowMenu = false
                                        showAppPicker = true
                                    }
                                )
                            }

                        }
                    }
                }


                // Dialpad keys
                val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))
                val subKeys = mapOf("1" to "   ","2" to "ABC","3" to "DEF","4" to "GHI","5" to "JKL","6" to "MNO","7" to "PQRS","8" to "TUV","9" to "WXYZ","0" to "+")

                keys.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { key ->
                            DialPadKey(
                                number = key,
                                letters = subKeys[key] ?: "",
                                soundPool = soundPool,
                                context = context,
                                onClick = { digit ->
                                    insertAtCursor(digit)
                                },
                                onLongClick = if (key == "0") ({ insertAtCursor("+") }) else null,
                                overrideWidth = keyWidth,
                                overrideHeight = keyHeight,
                                scaleFactor = scaleFactor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FadeScaleBox(visible = number.isNotEmpty()) {
                        DialerActionExpressive(
                            onClick = {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, number)
                                }
                                context.startActivity(intent)
                            },
                            icon = Icons.Default.PersonAdd,
                            contentDescription = "Add Contact",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(actionSize)
                        )
                    }

                    val lgBackdrop = LocalLiquidGlassBackdrop.current
                    val lgDialpadEnabled = remember(settingsState) {
                        prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) &&
                        prefs.getBoolean(PreferenceManager.KEY_LG_DIALPAD_CALL_BUTTON, false)
                    }
                    val blurDialpadEnabled = remember(settingsState) {
                        prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) &&
                        prefs.getBoolean(PreferenceManager.KEY_BLUR_DIALPAD_CALL_BUTTON, false) &&
                        !lgDialpadEnabled
                    }
                    DialerActionExpressive(
                        onClick = {
                            if (number.isNotEmpty()) {
                                initiateCall(number)
                            } else {
                                val latestCall = callLogsForSearch.firstOrNull { it.number.isNotBlank() }
                                if (latestCall != null) {
                                    replaceNumber(latestCall.number)
                                }
                            }
                        },
                        onLongClick = if (number.isNotEmpty()) ({ showAppPicker = true }) else null,
                        icon = Icons.Default.Call,
                        contentDescription = "Call",
                        containerColor = Color(0xFF34A853),
                        contentColor = Color.White,
                        modifier = Modifier.width(callW).height(callH),
                        isLarge = true,
                        liquidGlassBackdrop = lgBackdrop,
                        liquidGlassEnabled = lgDialpadEnabled,
                        blurEnabled = blurDialpadEnabled,
                        isCallButton = true
                    )

                    BackspaceActionButton(
                        number = number,
                        onBackspace = { backspaceAtCursor() },
                        onClear = { replaceNumber("") },
                        size = actionSize
                    )
                }
            }
        }
        } // end BoxWithConstraints (dialpad card)

        } // end AnimatedVisibility (dialpad card)

        Spacer(modifier = Modifier.height(4.dp))
        } // end outer Column
        } // end BoxWithConstraints (screen)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerActionExpressive(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    modifier: Modifier = Modifier.size(64.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isLarge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    liquidGlassBackdrop: com.coolappstore.everdialer.by.svhp.liquidglass.Backdrop? = null,
    liquidGlassEnabled: Boolean = false,
    blurEnabled: Boolean = false,
    isCallButton: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val coroutineScope = rememberCoroutineScope()
    var isTapPulse by remember { mutableStateOf(false) }
    var tapJob by remember { mutableStateOf<Job?>(null) }
    val isVisuallyPressed = isPressed || isTapPulse

    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val settingsState by prefs.settingsChanged.collectAsState()
    val isSaturatedActive = remember(settingsState, isDark) { prefs.isSaturatedForTheme(isDark) }

    val triggerTapPulse = {
        tapJob?.cancel()
        tapJob = coroutineScope.launch {
            isTapPulse = true
            delay(150L)
            isTapPulse = false
        }
    }

    val wrappedOnClick: () -> Unit = {
        triggerTapPulse()
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onClick()
    }
    val wrappedOnLongClick: (() -> Unit)? = if (onLongClick != null) ({
        triggerTapPulse()
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onLongClick()
    }) else null

    val cornerRadius by animateDpAsState(
        targetValue = if (isVisuallyPressed) (if (isLarge) 14.dp else 10.dp) else (if (isLarge) 28.dp else 24.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy), label = "ButtonShape"
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisuallyPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy), label = "ButtonScale"
    )

    val restingBgColor = when {
        isCallButton -> if (isSaturatedActive) MaterialTheme.colorScheme.primary else Color(0xFF34A853)
        isSaturatedActive -> androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, 0.35f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val targetBgColor = if (isVisuallyPressed) MaterialTheme.colorScheme.primary else restingBgColor
    val animatedBgColor by animateColorAsState(targetBgColor, spring(stiffness = Spring.StiffnessMedium), "ActionBtnBgColor")

    val restingContentColor = when {
        isCallButton -> Color.White
        isSaturatedActive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.primary.toArgb()) > 0.45
    val pressedContentColor = if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White
    val targetContentColor = if (isVisuallyPressed) pressedContentColor else restingContentColor
    val animatedContentColor by animateColorAsState(targetContentColor, spring(stiffness = Spring.StiffnessMedium), "ActionBtnContentColor")

    val useLiquidGlass = liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && liquidGlassBackdrop != null
    val buttonShape = RoundedCornerShape(cornerRadius)
    val useBackdropBlur = blurEnabled && !useLiquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    if (useLiquidGlass && liquidGlassBackdrop != null) {
        Box(
            modifier = modifier
                .scale(scale)
                .drawBackdrop(
                    backdrop = liquidGlassBackdrop,
                    shape = { buttonShape },
                    effects = {
                        val d = density
                        colorControls(saturation = 1.3f)
                        blur(2f * d)
                        lens(refractionHeight = 18f * d, refractionAmount = 52f * d)
                    },
                    highlight = { Highlight.Default }
                )
                .combinedClickable(
                    onClick = wrappedOnClick,
                    onLongClick = wrappedOnLongClick,
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Surface(
                shape = buttonShape,
                color = animatedBgColor.copy(alpha = 0.5f),
                contentColor = animatedContentColor,
                modifier = Modifier.matchParentSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
                }
            }
        }
    } else if (useBackdropBlur && liquidGlassBackdrop != null) {
        Surface(
            modifier = modifier
                .scale(scale)
                .drawPlainBackdrop(
                    backdrop = liquidGlassBackdrop,
                    shape    = { buttonShape },
                    effects  = { blur(30f * density) }
                )
                .combinedClickable(
                    onClick = wrappedOnClick,
                    onLongClick = wrappedOnLongClick,
                    interactionSource = interactionSource,
                    indication = null
                ),
            shape = buttonShape,
            color = animatedBgColor.copy(alpha = 0.72f),
            contentColor = animatedContentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
            }
        }
    } else {
        Surface(
            modifier = modifier
                .scale(scale)
                .combinedClickable(onClick = wrappedOnClick, onLongClick = wrappedOnLongClick, interactionSource = interactionSource, indication = null),
            shape = buttonShape,
            color = animatedBgColor,
            contentColor = animatedContentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadKey(
    number: String,
    letters: String,
    soundPool: SoundPool,
    context: Context,
    onClick: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
    compact: Boolean = false,
    overrideWidth: Dp? = null,
    overrideHeight: Dp? = null,
    scaleFactor: Float = 1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val coroutineScope = rememberCoroutineScope()
    var isTapPulse by remember { mutableStateOf(false) }
    var tapJob by remember { mutableStateOf<Job?>(null) }
    val isVisuallyPressed = isPressed || isTapPulse

    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val settingsState by prefs.settingsChanged.collectAsState()
    val isSaturatedActive = remember(settingsState, isDark) { prefs.isSaturatedForTheme(isDark) }

    val cornerRadius by animateDpAsState(
        targetValue = if (isVisuallyPressed) 10.dp else (if (compact) 24.dp else 28.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ButtonShapeAnimation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisuallyPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "DialKeyScale"
    )

    val restingBgColor = if (isSaturatedActive) {
        androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, 0.35f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val targetBgColor = if (isVisuallyPressed) MaterialTheme.colorScheme.primary else restingBgColor
    val bgColor by animateColorAsState(targetBgColor, spring(stiffness = Spring.StiffnessMedium), "DialKeyColor")

    val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.primary.toArgb()) > 0.45
    val pressedTextColor = if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White

    val restingMainTextColor = if (isSaturatedActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val targetMainTextColor = if (isVisuallyPressed) pressedTextColor else restingMainTextColor
    val mainTextColor by animateColorAsState(targetMainTextColor, spring(stiffness = Spring.StiffnessMedium), "DialKeyTextColor")

    val restingSubTextColor = if (isSaturatedActive) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f)
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    }
    val targetSubTextColor = if (isVisuallyPressed) pressedTextColor.copy(alpha = 0.85f) else restingSubTextColor
    val subTextColor by animateColorAsState(targetSubTextColor, spring(stiffness = Spring.StiffnessMedium), "DialKeySubTextColor")

    val keyWidth = overrideWidth ?: if (compact) 82.dp else 100.dp
    val keyHeight = overrideHeight ?: if (compact) 52.dp else 68.dp
    val mainFontSize = (if (compact) 18f else 22f) * scaleFactor.coerceIn(0.6f, 1.4f)
    val subFontSize = (10f * scaleFactor.coerceIn(0.6f, 1.4f))

    val triggerTapPulse = {
        tapJob?.cancel()
        tapJob = coroutineScope.launch {
            isTapPulse = true
            delay(150L)
            isTapPulse = false
        }
    }

    Surface(
        modifier = Modifier
            .size(width = keyWidth, height = keyHeight)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    triggerTapPulse()
                    if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (prefs.getBoolean(PreferenceManager.KEY_DTMF_TONE, false)) playDtmf(context, number, soundPool, prefs)
                    onClick(number)
                },
                onLongClick = if (onLongClick != null) ({
                    triggerTapPulse()
                    if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }) else null
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = bgColor
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text(text = number, style = MaterialTheme.typography.headlineMedium.copy(fontSize = mainFontSize.sp), color = mainTextColor, fontWeight = FontWeight.Medium)
            if (letters.isNotBlank()) {
                Text(text = letters, style = MaterialTheme.typography.labelSmall.copy(fontSize = subFontSize.sp), color = subTextColor, letterSpacing = 1.sp)
            }
        }
    }
}

/**
 * Renders a phone number with smooth per-character animations:
 * - New chars slide up + fade + scale in from below
 * - Deleted chars slide down + fade + scale out
 * - Existing chars animate their horizontal position smoothly when neighbours appear/disappear
 *
 * Also renders a movable, blinking text cursor at [cursorPosition]. Tapping a digit moves the
 * cursor to just before or after it (whichever half was tapped), and tapping the trailing empty
 * space moves the cursor to the end — so a missing/wrong digit in the middle of the number can be
 * fixed in place instead of having to delete and retype everything after it.
 *
 * Uses a stable monotonically-increasing ID per character insertion so Compose can
 * distinguish "same char shifted left" from "new char at this slot".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialpadNumberDisplay(
    number: String,
    fontSize: Int,
    cursorPosition: Int = number.length,
    onCursorPositionChange: (Int) -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val easeOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val textColor   = MaterialTheme.colorScheme.onSurface
    val textStyle   = MaterialTheme.typography.displaySmall.copy(
        fontSize   = fontSize.sp,
        fontWeight = FontWeight.Light
    )

    // Stable list of (uniqueId, char) — each insertion gets a fresh monotonic id
    // so position shifts use animateItem, not a full recompose.
    val idCounter  = remember { mutableStateOf(0) }
    val stableChars = remember { mutableStateListOf<Pair<Int, Char>>() }

    LaunchedEffect(number) {
        val current = stableChars.map { it.second }.joinToString("")
        if (number == current) return@LaunchedEffect

        // Diff by common prefix/suffix so an insert or delete anywhere in the middle of the
        // string (not just at the end) only touches the characters that actually changed —
        // everything else keeps its stable id and simply slides over.
        val minLen = minOf(current.length, number.length)
        var prefixLen = 0
        while (prefixLen < minLen && current[prefixLen] == number[prefixLen]) prefixLen++

        var suffixLen = 0
        val maxSuffix = minLen - prefixLen
        while (suffixLen < maxSuffix &&
            current[current.length - 1 - suffixLen] == number[number.length - 1 - suffixLen]
        ) suffixLen++

        val removeCount = current.length - prefixLen - suffixLen
        repeat(removeCount) {
            if (stableChars.size > prefixLen) stableChars.removeAt(prefixLen)
        }
        val insertText = number.substring(prefixLen, number.length - suffixLen)
        insertText.forEachIndexed { i, ch ->
            stableChars.add(prefixLen + i, Pair(idCounter.value++, ch))
        }
    }

    // Blinking caret alpha
    val cursorBlink = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by cursorBlink.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 500
                0f at 501
                0f at 999
            }
        ),
        label = "cursorAlpha"
    )

    val clampedCursor = cursorPosition.coerceIn(0, stableChars.size)

    LazyRow(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        userScrollEnabled     = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Leading spacer matching the trailing tap zone's width below, so the digits (and the
        // cursor) are actually centered in the box instead of being pulled off-center by an
        // unbalanced zone that only exists on the trailing side.
        item(key = "leading_cursor_area") {
            Box(modifier = Modifier.width(28.dp))
        }
        itemsIndexed(
            items = stableChars,
            key   = { _, pair -> pair.first }
        ) { index, pair ->
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }

            val offsetY by animateDpAsState(
                targetValue  = if (appeared) 0.dp else 20.dp,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "charOffY"
            )
            val alpha by animateFloatAsState(
                targetValue  = if (appeared) 1f else 0f,
                animationSpec = tween(360, easing = easeOutExpo),
                label = "charAlpha"
            )
            val scale by animateFloatAsState(
                targetValue  = if (appeared) 1f else 0.55f,
                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "charScale"
            )

            Box(contentAlignment = Alignment.CenterStart) {
                // A thin blinking bar rendered just before this character when the cursor sits
                // here, so it visually sits between the two adjacent digits.
                if (clampedCursor == index) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-2).dp)
                            .width(2.dp)
                            .height(with(LocalDensity.current) { textStyle.fontSize.toDp() * 0.9f })
                            .align(Alignment.CenterStart)
                            .graphicsLayer { this.alpha = cursorAlpha }
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    text     = pair.second.toString(),
                    style    = textStyle,
                    color    = textColor,
                    modifier = Modifier
                        .animateItem(
                            placementSpec  = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            fadeInSpec     = tween(360, easing = easeOutExpo),
                            fadeOutSpec    = tween(220)
                        )
                        .offset(y = offsetY)
                        .alpha(alpha)
                        .scale(scale)
                        .pointerInput(pair.first) {
                            detectTapGestures(
                                onLongPress = { onLongPress() }
                            ) { tapOffset ->
                                // Tapping the left half of a digit places the cursor before it,
                                // the right half places it after — like a normal text field.
                                val newPos = if (tapOffset.x < size.width / 2f) index else index + 1
                                onCursorPositionChange(newPos)
                            }
                        }
                )
            }
        }
        // Trailing tap target so the user can move the cursor to the very end even when there's
        // no character there (e.g. an empty number, or after the last digit).
        item(key = "trailing_cursor_area") {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(with(LocalDensity.current) { textStyle.fontSize.toDp() * 1.4f })
                    .pointerInput(stableChars.size) {
                        detectTapGestures(
                            onLongPress = { onLongPress() }
                        ) { onCursorPositionChange(stableChars.size) }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (clampedCursor == stableChars.size) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(with(LocalDensity.current) { textStyle.fontSize.toDp() * 0.9f })
                            .graphicsLayer { this.alpha = cursorAlpha }
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

object T9Matcher {
    fun convertNameToT9(name: String): String {
        return name.uppercase(Locale.getDefault()).map { char ->
            when (char) {
                'A', 'B', 'C' -> '2'
                'D', 'E', 'F' -> '3'
                'G', 'H', 'I' -> '4'
                'J', 'K', 'L' -> '5'
                'M', 'N', 'O' -> '6'
                'P', 'Q', 'R', 'S' -> '7'
                'T', 'U', 'V' -> '8'
                'W', 'X', 'Y', 'Z' -> '9'
                else -> '0'
            }
        }.joinToString("")
    }
}

private fun buildDtmfSoundPool(context: Context): SoundPool {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    return SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build()
}

private fun playDtmf(context: Context, key: String, soundPool: SoundPool, prefs: PreferenceManager) {
    val style = DialpadToneStyle.fromKey(prefs.getString(PreferenceManager.KEY_DIALPAD_TONE_STYLE, DialpadToneStyle.STANDARD.key))
    DialpadTonePlayer.play(context, key, style)
}

@Composable
private fun FadeScaleBox(visible: Boolean, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            content()
        }
    }
}

/**
 * A backspace button that — unlike [FadeScaleBox]-wrapped actions — stays in the composition at
 * all times. [FadeScaleBox] fully removes/re-adds its content via AnimatedVisibility, which means
 * a tap that lands during the fade-out/fade-in transition (e.g. while rapidly deleting digits)
 * can be dropped because the hit target is mid-animation-out of the tree. Here the button is
 * always present and always hit-testable; only its visual alpha/scale animate, and the action
 * itself is simply a no-op once the number is empty — so every tap reliably registers.
 */
@Composable
private fun BackspaceActionButton(
    number: String,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    size: Dp = 64.dp
) {
    val hasContent = number.isNotEmpty()
    val alpha by animateFloatAsState(if (hasContent) 1f else 0f, label = "BackspaceAlpha")
    val scale by animateFloatAsState(if (hasContent) 1f else 0.7f, label = "BackspaceScale")
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        DialerActionExpressive(
            onLongClick = { if (hasContent) onClear() },
            onClick = { if (hasContent) onBackspace() },
            icon = Icons.Default.Backspace,
            contentDescription = "Backspace",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(size)
        )
    }
}
