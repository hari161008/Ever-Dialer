package com.coolappstore.everdialer.by.svhp.view.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.screen.settings.AddMode
import com.coolappstore.everdialer.by.svhp.view.screen.settings.FakeCallAddSheet
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
private val CARD_RADIUS = 28.dp
private val INNER_RADIUS = 4.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AZListScroll(
    contacts: List<Contact>,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    selectionMode: Boolean = false,
    selectedContacts: Set<String> = emptySet(),
    onSelectionModeChange: (Boolean) -> Unit = {},
    onSelectedContactsChange: (Set<String>) -> Unit = {}
) {
    AZListContent(
        contacts = contacts,
        navigator = navigator,
        listState = listState,
        modifier = modifier,
        selectionMode = selectionMode,
        selectedContacts = selectedContacts,
        onSelectMode = { contact ->
            onSelectionModeChange(true)
            onSelectedContactsChange(setOf(contact.id))
        },
        onSelectToggle = { contact ->
            val updated = if (selectedContacts.contains(contact.id))
                selectedContacts - contact.id else selectedContacts + contact.id
            onSelectedContactsChange(updated)
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AZListContent(
    contacts: List<Contact>,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    selectionMode: Boolean = false,
    selectedContacts: Set<String> = emptySet(),
    onSelectMode: (Contact) -> Unit = {},
    onSelectToggle: (Contact) -> Unit = {}
) {
    val grouped = remember(contacts) {
        val mainGroups = contacts.groupBy {
            val firstChar = it.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar else '#'
        }.toMutableMap()

        val finalMap = linkedMapOf<Char, List<Contact>>()
        mainGroups.keys.filter { it.isLetter() }.sorted().forEach { char ->
            finalMap[char] = mainGroups[char]!!
        }
        val hashGroup = mainGroups['#']
        if (hashGroup != null) finalMap['#'] = hashGroup
        finalMap
    }

    // Map each letter to its first LazyColumn item index for sidebar jump
    val alphabetIndices = remember(grouped) {
        val map = mutableMapOf<Char, Int>()
        var currentIndex = 0
        grouped.forEach { (char, group) ->
            map[char] = currentIndex          // stickyHeader index
            currentIndex += 1 + group.size   // header + N contact items
        }
        map
    }

    val scope = rememberCoroutineScope()
    var draggingChar by remember { mutableStateOf<Char?>(null) }

    val scrollingChar by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            alphabetIndices.entries
                .filter { it.value <= firstVisible }
                .maxByOrNull { it.value }
                ?.key ?: alphabetIndices.keys.firstOrNull()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            grouped.forEach { (initial, contactsForChar) ->
                // ── Letter header ──────────────────────────────────────────
                stickyHeader(key = "header_$initial", contentType = "letterHeader") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // ── One lazy item per contact for smooth scrolling ─────────
                itemsIndexed(
                    items = contactsForChar,
                    key = { _, contact -> "${initial}_${contact.id}" },
                    contentType = { _, _ -> "contact" }
                ) { index, contact ->
                    val isOnly   = contactsForChar.size == 1
                    val isFirst  = index == 0
                    val isLast   = index == contactsForChar.lastIndex

                    val shape = when {
                        isOnly  -> RoundedCornerShape(CARD_RADIUS)
                        isFirst -> RoundedCornerShape(
                            topStart = CARD_RADIUS, topEnd = CARD_RADIUS,
                            bottomStart = INNER_RADIUS, bottomEnd = INNER_RADIUS
                        )
                        isLast  -> RoundedCornerShape(
                            topStart = INNER_RADIUS, topEnd = INNER_RADIUS,
                            bottomStart = CARD_RADIUS, bottomEnd = CARD_RADIUS
                        )
                        else    -> RoundedCornerShape(INNER_RADIUS)
                    }

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RivoScrollAnimatedItem(delayMs = (index * 25L).coerceAtMost(250L)) {
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ContactListItem(
                                    contact = contact,
                                    navigator = navigator,
                                    selectionMode = selectionMode,
                                    isSelected = selectedContacts.contains(contact.id),
                                    onSelectMode = { onSelectMode(contact) },
                                    onSelectToggle = { onSelectToggle(contact) }
                                )
                                if (!isLast) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                        }
                    }

                    // Gap between letter groups
                    if (isLast) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        AlphabetSideBar(
            alphabet = alphabetIndices.keys.toList(),
            selectedChar = draggingChar ?: scrollingChar,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            onLetterSelected = { char ->
                draggingChar = char
                val index = alphabetIndices[char] ?: return@AlphabetSideBar
                scope.launch { listState.scrollToItem(index) }
            },
            onDragEnd = { draggingChar = null }
        )

        if (draggingChar != null) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = draggingChar.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Renders a single contact row with the full long-press context menu (Select, View, Edit,
 * Copy number, Share, Move, Favourite, Fake Call, Delete). Used both by the main A-Z contacts
 * list ([AZListContent]) and by [com.coolappstore.everdialer.by.svhp.view.screen.ContactSearchContent]
 * so that contacts found via search get the exact same context menu — including the same
 * Settings → Appearance → "Context Menu Elements" (Contacts) show/hide + ordering.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactListItem(
    contact: Contact,
    navigator: DestinationsNavigator,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectMode: () -> Unit = {},
    onSelectToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var horizontalDragDetected by remember { mutableStateOf(false) }

    val settingsVer by prefs.settingsChanged.collectAsState()
    val fakeCallInContextMenu = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_FAKE_CALL_IN_CONTEXT_MENU, false)
    }
    var showFakeCallSheet by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showMenu) 0.97f else if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "contactItemScale"
    )

    val headline = contact.name.ifEmpty {
        contact.phoneNumbers.firstOrNull() ?: "Unknown"
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFF44336)) },
            title = { Text("Delete Contact") },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"$headline\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        contactsVM.deleteContact(contact.id)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showFakeCallSheet) {
        FakeCallAddSheet(
            mode = AddMode.Number,
            initialNumber = contact.phoneNumbers.firstOrNull() ?: "",
            initialDisplayName = headline,
            onDismiss = { showFakeCallSheet = false },
            onSave = { entry, exactTriggerOverride ->
                FakeCallManager.addEntry(context, prefs, entry, exactTriggerOverride)
                showFakeCallSheet = false
            }
        )
    }

    if (showMoveDialog) {
        val moveTargets = remember { contactsVM.getSaveTargets() }
        MoveContactDialog(
            contactName = headline,
            targets = moveTargets,
            onSelect = { target ->
                showMoveDialog = false
                contactsVM.moveContact(contact, target) { success ->
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

    Box(modifier = Modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (horizontalDragDetected) return@combinedClickable
                        if (selectionMode) {
                            onSelectToggle()
                        } else {
                            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                performAppHaptic(
                                    context,
                                    prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                    prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                                )
                            }
                            navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        horizontalDragDetected = false
                        val downPos = down.position
                        do {
                            val event = awaitPointerEvent()
                            val current = event.changes.firstOrNull() ?: break
                            val dx = kotlin.math.abs(current.position.x - downPos.x)
                            val dy = kotlin.math.abs(current.position.y - downPos.y)
                            if (dx > 28.dp.toPx() && dx > dy * 1.3f) horizontalDragDetected = true
                            if (!current.pressed) break
                        } while (true)
                        isPressed = false
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RivoAvatar(
                name = headline,
                photoUri = contact.photoUri,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!contact.phoneNumbers.firstOrNull().isNullOrEmpty()) {
                    Text(
                        text = contact.phoneNumbers.first(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
            exit  = fadeOut(tween(300)) + shrinkHorizontally(tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle() },
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        val hasNumber = !contact.phoneNumbers.firstOrNull().isNullOrEmpty()
        val contactContextMenuKeys = remember(settingsVer, hasNumber, fakeCallInContextMenu, contact.isFavorite) {
            com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.resolvedKeys(
                prefs,
                com.coolappstore.everdialer.by.svhp.controller.util.ContextMenuPrefs.SECTION_CONTACTS,
                listOf("select", "view_contact", "edit_contact", "copy_number", "share_contact", "move_contact", "toggle_favorite", "fake_call", "delete_contact")
            ).filter { key ->
                when (key) {
                    "copy_number" -> hasNumber
                    "fake_call" -> fakeCallInContextMenu
                    else -> true
                }
            }
        }

        RivoDropdownMenu(
            expanded         = showMenu && !selectionMode,
            onDismissRequest = { showMenu = false }
        ) {
            fun groupOf(key: String) = when (key) {
                "select" -> 0
                "view_contact", "edit_contact", "copy_number", "share_contact" -> 1
                "move_contact", "toggle_favorite", "fake_call" -> 2
                "delete_contact" -> 3
                else -> 1
            }
            var previousGroup: Int? = null
            contactContextMenuKeys.forEach { key ->
                val group = groupOf(key)
                if (previousGroup != null && group != previousGroup) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                            onSelectMode()
                        }
                    )
                    "view_contact" -> RivoDropdownMenuItem(
                        text     = "View contact",
                        icon     = Icons.Default.Person,
                        iconTint = Color(0xFF2196F3),
                        onClick  = {
                            showMenu = false
                            navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                        }
                    )
                    "edit_contact" -> RivoDropdownMenuItem(
                        text     = "Edit contact",
                        icon     = Icons.Default.Edit,
                        iconTint = Color(0xFF9C27B0),
                        onClick  = {
                            showMenu = false
                            navigator.navigate(ContactEditScreenDestination(contactId = contact.id))
                        }
                    )
                    "copy_number" -> RivoDropdownMenuItem(
                        text     = "Copy number",
                        icon     = Icons.Default.ContentCopy,
                        iconTint = Color(0xFF009688),
                        onClick  = {
                            showMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", contact.phoneNumbers.first()))
                            Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    "share_contact" -> RivoDropdownMenuItem(
                        text     = "Share contact",
                        icon     = Icons.Default.Share,
                        iconTint = Color(0xFFFF9800),
                        onClick  = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${contact.name}\n${contact.phoneNumbers.joinToString(", ")}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share contact"))
                        }
                    )
                    "toggle_favorite" -> RivoDropdownMenuItem(
                        text     = if (contact.isFavorite) "Remove from Favourites" else "Add to Favourites",
                        icon     = Icons.Default.Favorite,
                        iconTint = if (contact.isFavorite) Color(0xFFF44336) else Color(0xFFE91E63),
                        isDestructive = contact.isFavorite,
                        onClick  = {
                            showMenu = false
                            contactsVM.toggleFavorite(contact)
                        }
                    )
                    "move_contact" -> RivoDropdownMenuItem(
                        text     = "Move contact",
                        icon     = Icons.Default.DriveFileMove,
                        iconTint = Color(0xFF00897B),
                        onClick  = {
                            showMenu = false
                            showMoveDialog = true
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
                    "delete_contact" -> RivoDropdownMenuItem(
                        text     = "Delete contact",
                        icon     = Icons.Default.Delete,
                        iconTint = Color(0xFFF44336),
                        isDestructive = true,
                        onClick  = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }
} // end AZListContent

@Composable
fun AlphabetSideBar(
    alphabet: List<Char>,
    selectedChar: Char?,
    modifier: Modifier = Modifier,
    onLetterSelected: (Char) -> Unit,
    onDragEnd: () -> Unit
) {
    var columnHeight by remember { mutableStateOf(0) }

    Surface(
        modifier = modifier
            .width(24.dp)
            .wrapContentHeight()
            .onGloballyPositioned { columnHeight = it.size.height }
            .pointerInput(alphabet) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (columnHeight > 0) {
                            val itemHeight = columnHeight.toFloat() / alphabet.size
                            val index = (offset.y / itemHeight).toInt()
                            val char = alphabet.getOrNull(index.coerceIn(0, alphabet.lastIndex))
                            if (char != null) onLetterSelected(char)
                        }
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, _ ->
                    if (columnHeight > 0) {
                        val itemHeight = columnHeight.toFloat() / alphabet.size
                        val index = (change.position.y / itemHeight).toInt()
                        val char = alphabet.getOrNull(index.coerceIn(0, alphabet.lastIndex))
                        if (char != null) onLetterSelected(char)
                    }
                }
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                val isSelected = char == selectedChar
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
