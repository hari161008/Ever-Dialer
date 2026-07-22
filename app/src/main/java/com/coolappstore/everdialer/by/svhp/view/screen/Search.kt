package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.components.tiles.SingleTile
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.RecordingItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

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
            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    }
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Restore the cursor to the end of any already-typed query rather than letting the
        // newly (re)created text field default the selection back to the very start.
        queryFieldValue = queryFieldValue.copy(selection = TextRange(queryFieldValue.text.length))
        keyboardController?.show()
    }

    val filteredContacts = remember(query, contacts, filterState.contacts) {
        if (!filterState.contacts || query.isBlank()) emptyList()
        else contacts.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.phoneNumbers.any { number -> number.replace(" ", "").contains(query.replace(" ", "")) }
        }
    }

    // Numbers that show up in the call log but aren't saved as a contact — i.e. what "Non
    // contacts" in the Filter menu refers to. Deduplicated by normalized number, keeping the
    // most recent entry (callLogs is already date-descending).
    val nonContactResults = remember(query, callLogs, filterState.nonContacts) {
        if (!filterState.nonContacts || query.isBlank()) emptyList()
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
                entry.number.replace(" ", "").contains(query.replace(" ", "")) ||
                        (entry.isCallerIdName && (entry.name?.contains(query, ignoreCase = true) == true))
            }
        }
    }

    // Notes attached to a contact/number (from the call screen or contact info screen).
    val contactNoteResults = remember(query, filterState.contactNotes) {
        if (!filterState.contactNotes || query.isBlank()) emptyList()
        else NoteManager.getAllNotes(context).filter { note ->
            note.contactName.contains(query, ignoreCase = true) ||
                    note.phoneNumber.contains(query.filter { c -> c.isDigit() || c == '+' }.ifEmpty { query }, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
        }
    }

    // Notes attached to individual call recordings (call recorder's playback screen).
    val recordingNoteResults = remember(query, recordings, filterState.recordingNotes) {
        if (!filterState.recordingNotes || query.isBlank()) emptyList()
        else recordings.filter { it.noteText.isNotBlank() && it.noteText.contains(query, ignoreCase = true) }
    }

    // The call recordings themselves — matched by the caller's name/number rather than by note
    // content (that's [recordingNoteResults] above). Excludes anything already counted there so
    // the same recording doesn't show up twice under two different headings.
    val recordingResults = remember(query, recordings, filterState.recordings, recordingNoteResults) {
        if (!filterState.recordings || query.isBlank()) emptyList()
        else recordings.filter { rec ->
            rec !in recordingNoteResults &&
                    ((rec.contactName?.contains(query, ignoreCase = true) == true) ||
                            rec.phoneNumber.replace(" ", "").contains(query.replace(" ", "")))
        }
    }

    val totalResults = filteredContacts.size + nonContactResults.size + recordingResults.size +
            contactNoteResults.size + recordingNoteResults.size
    val hasAnyResults = totalResults > 0

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
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp
            ) {
                TextField(
                    value = queryFieldValue,
                    onValueChange = { queryFieldValue = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("Search contacts or numbers") },
                    leadingIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                            IconButton(onClick = { queryFieldValue = TextFieldValue("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
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
        AnimatedVisibility(
            visible = query.isNotEmpty() && query.all { it.isDigit() || it == '+' || it == '-' || it == ' ' },
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
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

        when {
            contacts.isEmpty() -> RivoLoadingIndicatorView()
            query.isBlank() -> {
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
            !hasAnyResults -> {
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        RivoSectionHeader(title = "$totalResults Result${if (totalResults != 1) "s" else ""}")
                    }

                    if (filteredContacts.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Contacts")
                            Spacer(modifier = Modifier.height(8.dp))
                            RivoExpressiveCard {
                                // Same long-press context menu as the main Contacts list (Select, View,
                                // Edit, Copy number, Share, Move, Favourite, Fake Call, Delete) — this
                                // was previously missing here, so searched contacts couldn't be
                                // moved/deleted/etc. without opening the full contact list. Visibility
                                // and ordering stay in sync with Settings → Appearance → Context Menu
                                // Elements (Contacts), since ContactListItem reads the same preferences.
                                filteredContacts.forEachIndexed { index, contact ->
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
                    }

                    if (nonContactResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Non Contacts")
                            Spacer(modifier = Modifier.height(8.dp))
                            RivoExpressiveCard {
                                nonContactResults.forEachIndexed { index, entry ->
                                    SingleTile(
                                        title = entry.name?.ifEmpty { entry.number } ?: entry.number,
                                        subtitle = if (entry.name.isNullOrEmpty() || entry.name == entry.number) null else entry.number,
                                        icon = Icons.Default.Person,
                                        phoneNumber = entry.number,
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
                    }

                    if (contactNoteResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Notes")
                            Spacer(modifier = Modifier.height(8.dp))
                            RivoExpressiveCard {
                                contactNoteResults.forEachIndexed { index, note ->
                                    SingleTile(
                                        title = note.contactName.ifBlank { note.phoneNumber.ifBlank { "Unknown" } },
                                        subtitle = note.content,
                                        icon = Icons.Default.StickyNote2,
                                        phoneNumber = note.phoneNumber,
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
                    }

                    if (recordingResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Recordings")
                            Spacer(modifier = Modifier.height(8.dp))
                            RivoExpressiveCard {
                                recordingResults.forEachIndexed { index, rec ->
                                    SingleTile(
                                        title = rec.contactName?.ifBlank { rec.phoneNumber } ?: rec.phoneNumber,
                                        subtitle = rec.phoneNumber,
                                        icon = Icons.Default.Mic,
                                        phoneNumber = rec.phoneNumber,
                                        onClick = {
                                            navigator.navigate(RecordingsScreenDestination(openedFromSettings = true))
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
                    }

                    if (recordingNoteResults.isNotEmpty()) {
                        item {
                            RivoSectionHeader(title = "Recording Notes")
                            Spacer(modifier = Modifier.height(8.dp))
                            RivoExpressiveCard {
                                recordingNoteResults.forEachIndexed { index, rec ->
                                    SingleTile(
                                        title = rec.contactName?.ifBlank { rec.phoneNumber } ?: rec.phoneNumber,
                                        subtitle = rec.noteText,
                                        icon = Icons.Default.Mic,
                                        phoneNumber = rec.phoneNumber,
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
                                            navigator.navigate(RecordingsScreenDestination(openedFromSettings = true))
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
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}
