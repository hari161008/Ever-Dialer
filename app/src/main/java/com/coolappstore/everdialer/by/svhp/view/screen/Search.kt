package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.coolappstore.everdialer.by.svhp.controller.CallLogViewModel
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.NoteEntry
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.normalizeNumberDigits
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.components.tiles.SingleTile
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.coolappstore.everdialer.by.svhp.view.components.NavBarVisibilityState
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * Matches note text against a query loosely: words can appear in any order and punctuation is
 * ignored, so a note like "today 5:30 pm" is still found by "pm today" or "5 30". Falls back to
 * a plain substring check for a single-token query so simple searches behave as before.
 */
private fun matchesNoteQuery(text: String, query: String): Boolean {
    if (query.isBlank()) return false
    fun normalize(s: String) = s.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
    val normalizedText = normalize(text)
    val tokens = normalize(query).split(" ").filter { it.isNotBlank() }
    if (tokens.isEmpty()) return text.contains(query, ignoreCase = true)
    return tokens.all { normalizedText.contains(it) }
}

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>
@Composable
fun SearchScreen(navController: NavController, navigator: DestinationsNavigator) {
    val permState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ContactSearchContent(
                navigator = navigator,
                isGranted = permState.status == PermissionStatus.Granted,
                onRequestPermission = { permState.launchPermissionRequest() },
                listState = listState
            )
        }
    }
}

/**
 * Corner shape for a row inside a visually-grouped "card" of lazily rendered results — rounded
 * only on the outer edge of the first/last row in the group so consecutive rows still read as one
 * continuous card, just like [RivoExpressiveCard], while each row is its own LazyColumn item.
 */
private fun groupedRowShape(index: Int, count: Int, corner: androidx.compose.ui.unit.Dp = 20.dp): Shape {
    val top = if (index == 0) corner else 0.dp
    val bottom = if (index == count - 1) corner else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSearchContent(
    navigator: DestinationsNavigator,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (!isGranted) {
        PermissionDeniedView(
            icon = Icons.Default.Person,
            title = "Contacts Permission Required",
            description = "To search your contacts and identify incoming calls, Ever Dialer needs access to your contacts.",
            onGrantClick = onRequestPermission
        )
        return
    }

    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val callLogVM: CallLogViewModel = koinActivityViewModel()
    // Owned by the bundled Ever Call Recorder module — reused here (read-only) purely to search
    // recording notes; it manages its own loading/refresh lifecycle independently.
    val recordingsVM: HomeViewModel = viewModel()

    val contacts by contactsVM.allContacts.collectAsState()
    val callLogs by callLogVM.allCallLogs.collectAsState()
    val recordings by recordingsVM.allRecordings.collectAsState()

    val settingsVer by prefs.settingsChanged.collectAsState()
    val filterState = remember(settingsVer) { prefs.getSearchFilterState() }

    // TextFieldValue (not a plain String) so the cursor position survives this screen being
    // recreated — e.g. navigating into a contact's details and pressing back. With a plain
    // String, re-requesting focus on the freshly recomposed field always snapped the cursor
    // back to index 0 instead of staying at the end of the restored text.
    var queryFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val query = queryFieldValue.text
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Only auto-focus + show the keyboard the very first time this screen is genuinely entered,
    // not every time it re-enters composition — which also happens when returning here after a
    // destination pushed on top of it (e.g. tapping a result to open the Dialpad, then closing
    // the Dialpad) navigates back. Without this guard, LaunchedEffect(Unit) refires on that
    // return trip and pops the keyboard back open right as the Dialpad page closes.
    var hasAutoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasAutoFocused) {
            hasAutoFocused = true
            focusRequester.requestFocus()
            // Restore the cursor to the end of any already-typed query rather than letting the
            // newly (re)created text field default the selection back to the very start.
            queryFieldValue = queryFieldValue.copy(selection = TextRange(queryFieldValue.text.length))
            keyboardController?.show()
        }
    }

    // ── Precomputed search index over `contacts` ────────────────────────────────────────────
    // Built only when the contacts list itself changes (cold start / cache refresh) instead of
    // on every keystroke. This is what keeps typing instant even with 2,500+ contacts — a
    // keystroke now does a cheap `contains` over already-lowercased/normalized strings instead
    // of re-lowercasing every contact's name and re-stripping spaces from every phone number on
    // every single character typed.
    data class IndexedContact(val contact: Contact, val nameLower: String, val numbersNormalized: List<String>)
    val contactIndex = remember(contacts) {
        contacts.map { c ->
            IndexedContact(
                contact = c,
                nameLower = c.name.lowercase(),
                numbersNormalized = c.phoneNumbers.map { it.replace(" ", "") }
            )
        }
    }

    // Notes live on disk as individual files — reading them is real I/O, so this loads once per
    // Search session instead of on every keystroke.
    var allNotes by remember { mutableStateOf<List<NoteEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        allNotes = withContext(Dispatchers.IO) {
            runCatching { NoteManager.getAllNotes(context) }.getOrDefault(emptyList())
        }
    }

    val globalSettings = remember { buildGlobalSettingsSearchEntries() }

    // ── Synchronous, same-frame search ──────────────────────────────────────────────────────
    // Deliberately NOT a LaunchedEffect/coroutine + mutableStateOf combo: that pattern always
    // renders one extra frame with the *previous* (often empty) results before the new ones
    // land, which is exactly the "flashes no results for a moment" bug. Filtering here runs
    // directly inside composition via `remember`, keyed on the query and data it depends on, so
    // the results list used below is always correct on the very first frame that reflects the
    // new keystroke — nothing to await, nothing to flicker. It's cheap enough to be instant even
    // for large contact lists because it reuses the precomputed, already-lowercased/normalized
    // `contactIndex` above instead of re-processing every contact on every character.
    data class SearchResults(
        val contacts: List<Contact>,
        val nonContacts: List<CallLogEntry>,
        val notes: List<NoteEntry>,
        val recordingNotes: List<RecordingItem>,
        val recordings: List<RecordingItem>,
        val settings: List<GlobalSettingsSearchEntry>
    )
    val emptySearchResults = remember { SearchResults(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()) }
    val searchResults = remember(query, contactIndex, callLogs, allNotes, recordings, globalSettings, filterState) {
        val q = query
        if (q.isBlank()) return@remember emptySearchResults

        val qLower = q.lowercase()
        val qDigits = q.replace(" ", "")

        val fc = if (!filterState.contacts) emptyList()
        else contactIndex.filter { entry ->
            entry.nameLower.contains(qLower) || entry.numbersNormalized.any { it.contains(qDigits) }
        }.map { it.contact }

        // Numbers that show up in the call log but aren't saved as a contact — i.e. what
        // "Non contacts" in the Filter menu refers to. Deduplicated by normalized number,
        // keeping the most recent entry (callLogs is already date-descending).
        val ncr = if (!filterState.nonContacts) emptyList()
        else {
            val seen = LinkedHashMap<String, CallLogEntry>()
            callLogs.asSequence()
                .filter { it.contactId.isNullOrBlank() }
                .forEach { entry ->
                    val key = normalizeNumberDigits(entry.number).filter { it.isDigit() }.takeLast(9)
                        .ifBlank { entry.number }
                    seen.putIfAbsent(key, entry)
                }
            seen.values.filter { entry ->
                entry.number.replace(" ", "").contains(qDigits) ||
                        (entry.isCallerIdName && (entry.name?.contains(q, ignoreCase = true) == true))
            }
        }

        // Notes attached to a contact/number (from the call screen or contact info screen).
        val cnr = if (!filterState.contactNotes) emptyList()
        else allNotes.filter { note ->
            note.contactName.contains(q, ignoreCase = true) ||
                    note.phoneNumber.contains(q.filter { c -> c.isDigit() || c == '+' }.ifEmpty { q }, ignoreCase = true) ||
                    matchesNoteQuery(note.content, q)
        }

        // Notes attached to individual call recordings (call recorder's playback screen).
        val rnr = if (!filterState.recordingNotes) emptyList()
        else recordings.filter { it.noteText.isNotBlank() && matchesNoteQuery(it.noteText, q) }

        // The call recordings themselves — matched by the caller's name/number rather than
        // by note content (that's `rnr` above). Excludes anything already counted there so
        // the same recording doesn't show up twice under two different headings.
        val rr = if (!filterState.recordings) emptyList()
        else recordings.filter { rec ->
            rec !in rnr &&
                    ((rec.contactName?.contains(q, ignoreCase = true) == true) ||
                            rec.phoneNumber.replace(" ", "").contains(qDigits))
        }

        val sr = globalSettings.filter { entry ->
            entry.title.contains(q, ignoreCase = true) || entry.subtitle.contains(q, ignoreCase = true)
        }

        SearchResults(fc, ncr, cnr, rnr, rr, sr)
    }
    val filteredContacts = searchResults.contacts
    val nonContactResults = searchResults.nonContacts
    val contactNoteResults = searchResults.notes
    val recordingNoteResults = searchResults.recordingNotes
    val recordingResults = searchResults.recordings
    val settingResults = searchResults.settings

    val totalResults = filteredContacts.size + nonContactResults.size + recordingResults.size +
            contactNoteResults.size + recordingNoteResults.size + settingResults.size
    val hasAnyResults = totalResults > 0

    val saturatedColors = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_SATURATED_COLORS, false) }
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val isSaturatedDark = saturatedColors && isDark

    val searchBarBg = if (isSaturatedDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val searchBarFg = if (isSaturatedDark) Color(0xFF1C1B1F) else MaterialTheme.colorScheme.onSurface
    val searchBarPlaceholder = if (isSaturatedDark) Color(0xFF1C1B1F).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Search bar + filter button
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = searchBarBg,
                shadowElevation = 0.dp
            ) {
                TextField(
                    value = queryFieldValue,
                    onValueChange = { queryFieldValue = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("Search contacts or numbers", color = searchBarPlaceholder) },
                    leadingIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = searchBarFg)
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { queryFieldValue = TextFieldValue("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = searchBarFg)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = searchBarFg,
                        unfocusedTextColor = searchBarFg,
                        cursorColor = searchBarFg,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
            SearchFilterButton()
        }

        // Call this number chip
        if (query.isNotEmpty() && query.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Call $query",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        navigator.navigate(DialPadScreenDestination(initialNumber = query))
                    }) {
                        Text("Open Dialpad", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Which top-level state the body below is in — used as the AnimatedContent key so
        // switching between them (e.g. blank prompt → results appearing as you type) gets the
        // same kind of sliding/fading reveal as the Dialpad's search results panel, instead of
        // just instantly swapping.
        val searchUiState = when {
            contacts.isEmpty() -> "loading"
            query.isBlank() -> "blank"
            !hasAnyResults -> "empty"
            else -> "results"
        }
        AnimatedContent(
            targetState = searchUiState,
            transitionSpec = {
                (fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 6 } +
                        expandVertically(tween(380, easing = FastOutSlowInEasing), expandFrom = Alignment.Top))
                    .togetherWith(
                        fadeOut(tween(220, easing = FastOutLinearInEasing)) +
                                shrinkVertically(tween(220, easing = FastOutLinearInEasing), shrinkTowards = Alignment.Top)
                    )
            },
            label = "SearchResultsState"
        ) { state ->
            when (state) {
                "loading" -> RivoLoadingIndicatorView()
                "blank" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                "Search contacts or numbers",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                "empty" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                "No results for \"$query\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                ScrollHapticsEffect(listState = listState)
                // ── Virtualized results list ────────────────────────────────────────────────
                // Every row below is its own LazyColumn item (itemsIndexed) instead of being
                // eagerly `forEach`-composed inside a single non-lazy `item {}`. That non-lazy
                // pattern is exactly what made typing feel laggy/hardcoded on large contact
                // lists: every keystroke recomposed and measured *every* matched row at once
                // (hundreds of ContactListItems, each with its own avatar image load, gesture
                // detector, and animation) before the frame could even show the new character.
                // Rows are now composed/measured only when they actually scroll into view —
                // the same "only pay for what's on screen" approach that keeps the Dialpad's
                // inline search snappy — while still keeping the same look via
                // [groupedRowShape] (rounded top/bottom only) and the exact same tile
                // composables/animations already used elsewhere.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "$totalResults Result${if (totalResults != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (filteredContacts.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Contacts")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // Same long-press context menu as the main Contacts list (Select, View,
                        // Edit, Copy number, Share, Move, Favourite, Fake Call, Delete) — this
                        // was previously missing here, so searched contacts couldn't be
                        // moved/deleted/etc. without opening the full contact list. Visibility
                        // and ordering stay in sync with Settings → Appearance → Context Menu
                        // Elements (Contacts), since ContactListItem reads the same preferences.
                        itemsIndexed(
                            items = filteredContacts,
                            key = { _, contact -> "contact_${contact.id}" }
                        ) { index, contact ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    ),
                                shape = groupedRowShape(index, filteredContacts.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column {
                                    ContactListItem(
                                        contact = contact,
                                        navigator = navigator
                                    )
                                    if (index < filteredContacts.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (nonContactResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Non Contacts")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = nonContactResults,
                            key = { _, entry -> "noncontact_${entry.number}_${entry.date}" }
                        ) { index, entry ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    ),
                                shape = groupedRowShape(index, nonContactResults.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column {
                                    SingleTile(
                                        title = entry.name?.ifEmpty { entry.number } ?: entry.number,
                                        subtitle = if (entry.name.isNullOrEmpty() || entry.name == entry.number) null else entry.number,
                                        icon = Icons.Default.Person,
                                        phoneNumber = entry.number,
                                        onAvatarClick = {
                                            navigator.navigate(ContactDetailsScreenDestination(phoneNumber = entry.number))
                                        },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                navigator.navigate(DialPadScreenDestination(initialNumber = entry.number))
                                            }) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            navigator.navigate(DialPadScreenDestination(initialNumber = entry.number))
                                        }
                                    )
                                    if (index < nonContactResults.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (contactNoteResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Notes")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = contactNoteResults,
                            key = { _, note -> "note_${note.file.absolutePath}" }
                        ) { index, note ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    ),
                                shape = groupedRowShape(index, contactNoteResults.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column {
                                    SingleTile(
                                        title = note.contactName.ifBlank { note.phoneNumber.ifBlank { "Unknown" } },
                                        subtitle = note.content,
                                        icon = Icons.Default.StickyNote2,
                                        phoneNumber = note.phoneNumber,
                                        onAvatarClick = {
                                            navigator.navigate(ContactDetailsScreenDestination(phoneNumber = note.phoneNumber))
                                        },
                                        supportingContent = {
                                            Text(
                                                note.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            NavBarVisibilityState.hideForSearchResult = true
                                            navigator.navigate(NotesScreenDestination(highlightQuery = query))
                                        }
                                    )
                                    if (index < contactNoteResults.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (recordingResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Recordings")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = recordingResults,
                            key = { _, rec -> "recording_${rec.uri}" }
                        ) { index, rec ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    ),
                                shape = groupedRowShape(index, recordingResults.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column {
                                    SingleTile(
                                        title = rec.contactName?.ifBlank { rec.phoneNumber } ?: rec.phoneNumber,
                                        subtitle = rec.phoneNumber,
                                        icon = Icons.Default.Mic,
                                        phoneNumber = rec.phoneNumber,
                                        onAvatarClick = {
                                            navigator.navigate(ContactDetailsScreenDestination(phoneNumber = rec.phoneNumber))
                                        },
                                        onClick = {
                                            NavBarVisibilityState.hideForSettingsEntry = true
                                            navigator.navigate(
                                                RecordingsScreenDestination(
                                                    openedFromSettings = true,
                                                    openedRecordingUri = rec.uri.toString()
                                                )
                                            )
                                        }
                                    )
                                    if (index < recordingResults.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (recordingNoteResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Recording Notes")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = recordingNoteResults,
                            key = { _, rec -> "recordingnote_${rec.uri}" }
                        ) { index, rec ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    ),
                                shape = groupedRowShape(index, recordingNoteResults.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column {
                                    SingleTile(
                                        title = rec.contactName?.ifBlank { rec.phoneNumber } ?: rec.phoneNumber,
                                        subtitle = rec.noteText,
                                        icon = Icons.Default.Mic,
                                        phoneNumber = rec.phoneNumber,
                                        onAvatarClick = {
                                            navigator.navigate(ContactDetailsScreenDestination(phoneNumber = rec.phoneNumber))
                                        },
                                        supportingContent = {
                                            Text(
                                                rec.noteText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            NavBarVisibilityState.hideForSettingsEntry = true
                                            navigator.navigate(
                                                RecordingsScreenDestination(
                                                    openedFromSettings = true,
                                                    openedRecordingUri = rec.uri.toString()
                                                )
                                            )
                                        }
                                    )
                                    if (index < recordingNoteResults.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (settingResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Settings")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = settingResults,
                            key = { _, entry -> "setting_${entry.key}_${entry.title}" }
                        ) { index, entry ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    ),
                                shape = groupedRowShape(index, settingResults.size),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column {
                                    RivoListItem(
                                        headline = entry.title,
                                        supporting = entry.subtitle,
                                        leadingIcon = entry.icon,
                                        iconContainerColor = entry.iconContainerColor,
                                        trailingIcon = Icons.Default.ChevronRight,
                                        onClick = {
                                            keyboardController?.hide()
                                            NavBarVisibilityState.hideForSettingsEntry = true
                                            entry.navigateTo(navigator)
                                        }
                                    )
                                    if (index < settingResults.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
                }
            }
        }
    }
}
