package com.coolappstore.everdialer.by.svhp.view.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.NoteEntry
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.BottomBar
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun NotesScreen(navController: NavController, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val haptic = LocalHapticFeedback.current
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()

    // Build phone→photoUri lookup map
    val phoneToPhotoUri = remember(allContacts) {
        buildMap {
            allContacts.forEach { c ->
                c.phoneNumbers.forEach { num ->
                    put(num.filter { it.isDigit() || it == '+' }, c.photoUri)
                }
            }
        }
    }

    var notes by remember { mutableStateOf(NoteManager.getAllNotes(context)) }
    var showOverflow by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editorNote by remember { mutableStateOf<NoteEntry?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntry?>(null) }

    fun refreshNotes() { notes = NoteManager.getAllNotes(context) }

    if (showEditor && editorNote != null) {
        NoteEditorDialog(
            contactName = editorNote!!.contactName,
            phoneNumber = editorNote!!.phoneNumber,
            onDismiss = { showEditor = false; editorNote = null; refreshNotes() }
        )
    }

    if (showDeleteConfirm && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note") },
            text = { Text("Delete note for ${noteToDelete!!.contactName}?") },
            confirmButton = {
                TextButton(onClick = {
                    NoteManager.deleteNoteFile(noteToDelete!!.file)
                    showDeleteConfirm = false
                    refreshNotes()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hide Notes") },
                                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                                onClick = {
                                    showOverflow = false
                                    prefs.setBoolean(PreferenceManager.KEY_NOTES_ENABLED, false)
                                    navigator.navigateUp()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = { BottomBar(navController, navigator) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Note,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No Notes Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Notes taken during calls appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.file.absolutePath }) { note ->
                        val safePhone = note.phoneNumber.filter { it.isDigit() || it == '+' }
                        val photoUri = phoneToPhotoUri[safePhone]
                        NoteCard(
                            note = note,
                            photoUri = photoUri,
                            onClick = {
                                editorNote = note
                                showEditor = true
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedNote = note
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // Long-press context menu
            if (selectedNote != null) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { selectedNote = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            noteToDelete = selectedNote
                            selectedNote = null
                            showDeleteConfirm = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            val note = selectedNote!!
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Note: ${note.contactName}")
                                putExtra(Intent.EXTRA_TEXT, note.content)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Note"))
                            selectedNote = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(note: NoteEntry, photoUri: String? = null, onClick: () -> Unit, onLongClick: () -> Unit) {
    val dateStr = remember(note.lastModified) {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(note.lastModified))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RivoAvatar(
                name = note.contactName,
                photoUri = photoUri,
                modifier = Modifier.size(44.dp),
                shape = CircleShape
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        note.contactName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (note.phoneNumber.isNotEmpty()) {
                    Text(
                        note.phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    contactName: String,
    phoneNumber: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember {
        mutableStateOf(NoteManager.readNote(context, contactName, phoneNumber))
    }

    ModalBottomSheet(
        onDismissRequest = {
            NoteManager.writeNote(context, contactName, phoneNumber, text)
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
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
                Column {
                    Text(
                        contactName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (phoneNumber.isNotEmpty()) {
                        Text(
                            phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = {
                        NoteManager.writeNote(context, contactName, phoneNumber, text)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                placeholder = { Text("Type your note here...") },
                shape = RoundedCornerShape(16.dp),
                minLines = 8
            )
        }
    }
}
