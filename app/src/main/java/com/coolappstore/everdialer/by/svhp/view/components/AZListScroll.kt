package com.coolappstore.everdialer.by.svhp.view.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
private val CARD_RADIUS = 28.dp
private val INNER_RADIUS = 4.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AZListScroll(
    contacts: List<Contact>,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
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
                                ContactListItem(contact = contact, navigator = navigator)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactListItem(
    contact: Contact,
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = koinInject<PreferenceManager>()
    var showMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var horizontalDragDetected by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "contactItemScale"
    )

    val headline = contact.name.ifEmpty {
        contact.phoneNumbers.firstOrNull() ?: "Unknown"
    }

    Box(modifier = Modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (horizontalDragDetected) return@combinedClickable
                        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                            performAppHaptic(
                                context,
                                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                            )
                        }
                        navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
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

        RivoDropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            RivoDropdownMenuItem(
                text     = "View contact",
                icon     = Icons.Default.Person,
                iconTint = Color(0xFF2196F3),
                onClick  = {
                    showMenu = false
                    navigator.navigate(ContactDetailsScreenDestination(contactId = contact.id))
                }
            )
            RivoDropdownMenuItem(
                text     = "Edit contact",
                icon     = Icons.Default.Edit,
                iconTint = Color(0xFF9C27B0),
                onClick  = {
                    showMenu = false
                    navigator.navigate(ContactEditScreenDestination(contactId = contact.id))
                }
            )
            if (!contact.phoneNumbers.firstOrNull().isNullOrEmpty()) {
                RivoDropdownMenuItem(
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
            }
            RivoDropdownMenuItem(
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
        }
    }
}

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
