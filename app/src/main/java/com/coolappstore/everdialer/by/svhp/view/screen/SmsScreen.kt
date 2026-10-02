package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.SmsConversation
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollHapticsEffect
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun SmsScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
    initialSharedText: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var isDefaultSms by remember { mutableStateOf(DefaultSmsManager.isDefaultSms(context)) }
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
        smsVM.refreshConversations()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermissions = permissions[Manifest.permission.READ_SMS] == true &&
                            permissions[Manifest.permission.SEND_SMS] == true
        if (hasSmsPermissions) {
            smsVM.refreshConversations()
        }
    }

    val rawConversations by smsVM.filteredConversations.collectAsState()
    val isLoading by smsVM.isLoading.collectAsState()
    val searchQuery by smsVM.searchQuery.collectAsState()

    // Quik preference: unread at top
    val unreadAtTop = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_UNREAD_AT_TOP, true)
    }
    val autoColor = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, true)
    }
    val swipeRightAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, "call") ?: "call"
    }
    val swipeLeftAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, "delete") ?: "delete"
    }

    val conversations = remember(rawConversations, unreadAtTop) {
        if (unreadAtTop) {
            val unread = rawConversations.filter { !it.isRead }
            val read = rawConversations.filter { it.isRead }
            unread + read
        } else {
            rawConversations
        }
    }

    // Selection mode (Quik multi-select action mode)
    val selectedThreadIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedThreadIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedThreadIds.clear()
    }

    val cleanSharedText = remember(initialSharedText) {
        initialSharedText?.takeIf {
            it.isNotBlank() && it != "{initialSharedText}" && it != "null"
        }
    }
    var showComposeDialog by remember { mutableStateOf(!cleanSharedText.isNullOrBlank()) }
    var composeInitialText by remember { mutableStateOf(cleanSharedText) }
    var threadToDelete by remember { mutableStateOf<SmsConversation?>(null) }
    var showDeleteMultipleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cleanSharedText) {
        if (!cleanSharedText.isNullOrBlank()) {
            composeInitialText = cleanSharedText
            showComposeDialog = true
        }
    }

    LaunchedEffect(Unit) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
        smsVM.refreshConversations()
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val yearFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val nowCalendar = remember { Calendar.getInstance() }

    fun formatMessageDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val msgCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when {
            msgCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
            msgCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR) -> {
                timeFormat.format(Date(timestamp))
            }
            msgCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) -> {
                dateFormat.format(Date(timestamp))
            }
            else -> {
                yearFormat.format(Date(timestamp))
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Quik Multi-Select Top Bar
                TopAppBar(
                    title = { Text("${selectedThreadIds.size} selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedThreadIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedThreadIds.size == conversations.size) {
                                selectedThreadIds.clear()
                            } else {
                                selectedThreadIds.clear()
                                selectedThreadIds.addAll(conversations.map { it.threadId })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            selectedThreadIds.forEach { tid ->
                                smsVM.markThreadAsRead(tid)
                            }
                            selectedThreadIds.clear()
                        }) {
                            Icon(Icons.Outlined.MarkChatRead, contentDescription = "Mark as Read")
                        }
                        IconButton(onClick = {
                            showDeleteMultipleDialog = true
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            } else {
                Column {
                    TopBar(navController = navController, navigator = navigator)
                    // Quik Header with Settings shortcut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Messages",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Mark all as read
                            IconButton(onClick = {
                                conversations.filter { !it.isRead }.forEach { conv ->
                                    smsVM.markThreadAsRead(conv.threadId)
                                }
                            }) {
                                Icon(
                                    Icons.Outlined.DoneAll,
                                    contentDescription = "Mark all as read",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Quik Settings
                            IconButton(onClick = {
                                navigator.navigate(SmsSettingsScreenDestination())
                            }) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "Quik SMS Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
                val fabBg = if (isSaturatedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                val fabFg = if (isSaturatedActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                val fabShape = RoundedCornerShape(17.dp)
                val pillNav = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val baseModifier = Modifier
                    .then(if (pillNav) Modifier.navigationBarsPadding().padding(bottom = 92.dp) else Modifier)
                    .then(if (isLandscape) Modifier.navigationBarsPadding().padding(bottom = 8.dp) else Modifier)

                FloatingActionButton(
                    onClick = {
                        composeInitialText = null
                        showComposeDialog = true
                    },
                    containerColor = fabBg,
                    contentColor = fabFg,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp,
                        focusedElevation = 6.dp,
                        hoveredElevation = 6.dp
                    ),
                    modifier = baseModifier
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Message")
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        val listState = rememberLazyListState()
        ScrollHapticsEffect(listState)
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Default SMS App Warning Banner
            if (!isDefaultSms) {
                item {
                    Surface(
                        onClick = {
                            DefaultSmsManager.requestDefaultSms(defaultSmsLauncher, context)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Sms,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Set as Default SMS App",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Allow Ever Dialer to send, receive, and manage your text messages seamlessly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Permissions prompt banner
            if (!hasSmsPermissions) {
                item {
                    Surface(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "SMS Permission Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Tap to grant SMS permissions so Ever Dialer can display your conversations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (conversations.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Chat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                if (searchQuery.isNotBlank()) "No conversations match '$searchQuery'" else "No messages yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Start a conversation by tapping the button below",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Conversation items
            items(conversations, key = { it.threadId }) { conv ->
                val isSelected = selectedThreadIds.contains(conv.threadId)

                ConversationItemRow(
                    conversation = conv,
                    dateStr = formatMessageDate(conv.date),
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    autoColor = autoColor,
                    onClick = {
                        if (isSelectionMode) {
                            if (isSelected) selectedThreadIds.remove(conv.threadId)
                            else selectedThreadIds.add(conv.threadId)
                        } else {
                            navigator.navigate(
                                SmsChatScreenDestination(
                                    threadId = conv.threadId,
                                    address = conv.address,
                                    contactName = conv.contactName,
                                    photoUri = conv.photoUri
                                )
                            )
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            selectedThreadIds.add(conv.threadId)
                        }
                    },
                    onAvatarClick = {
                        if (!isSelectionMode) {
                            navigator.navigate(
                                ContactDetailsScreenDestination(
                                    phoneNumber = conv.address
                                )
                            )
                        }
                    },
                    onDeleteClick = {
                        threadToDelete = conv
                    },
                    onMarkReadClick = {
                        smsVM.markThreadAsRead(conv.threadId)
                    },
                    onCallClick = {
                        makeCall(context, conv.address, null)
                    }
                )
            }
        }
    }

    // Delete single thread confirmation
    threadToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { threadToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete conversation?") },
            text = { Text("Delete conversation with ${conv.contactName ?: conv.address}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        smsVM.deleteThread(conv.threadId)
                        threadToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete multiple selected threads confirmation
    if (showDeleteMultipleDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMultipleDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedThreadIds.size} conversations?") },
            text = { Text("All messages in the selected conversations will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedThreadIds.forEach { tid ->
                            smsVM.deleteThread(tid)
                        }
                        selectedThreadIds.clear()
                        showDeleteMultipleDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMultipleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Compose New Message Dialog
    if (showComposeDialog) {
        ComposeMessageDialog(
            contacts = allContacts,
            mobileOnly = prefs.getBoolean(PreferenceManager.KEY_SMS_MOBILE_ONLY, false),
            onDismiss = {
                showComposeDialog = false
                composeInitialText = null
            },
            onRecipientSelected = { number, name ->
                showComposeDialog = false
                val textToSend = composeInitialText
                composeInitialText = null
                navigator.navigate(
                    SmsChatScreenDestination(
                        threadId = -1L,
                        address = number,
                        contactName = name,
                        initialText = textToSend
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItemRow(
    conversation: SmsConversation,
    dateStr: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    autoColor: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkReadClick: () -> Unit,
    onCallClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        !conversation.isRead -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onAvatarClick() }
                ) {
                    RivoAvatar(
                        name = conversation.contactName ?: conversation.address,
                        photoUri = conversation.photoUri,
                        size = 48.dp
                    )
                }
                Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.contactName ?: conversation.address,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!conversation.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.snippet.ifBlank { "No message text" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!conversation.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (!conversation.isRead) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!conversation.isRead) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        ) {}
                    }
                }
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    RivoDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        RivoDropdownMenuItem(
                            text = "Call",
                            icon = Icons.Default.Call,
                            onClick = {
                                showMenu = false
                                onCallClick()
                            }
                        )
                        RivoDropdownMenuItem(
                            text = if (conversation.isRead) "Mark as unread" else "Mark as read",
                            icon = if (conversation.isRead) Icons.Outlined.MarkChatUnread else Icons.Outlined.MarkChatRead,
                            onClick = {
                                showMenu = false
                                onMarkReadClick()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoDropdownMenuItem(
                            text = "Delete",
                            icon = Icons.Outlined.Delete,
                            isDestructive = true,
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeMessageDialog(
    contacts: List<Contact>,
    mobileOnly: Boolean = false,
    onDismiss: () -> Unit,
    onRecipientSelected: (String, String?) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, rawInput, mobileOnly) {
        val q = rawInput.trim().lowercase()
        val baseList = if (mobileOnly) {
            contacts.filter { c -> c.phoneNumbers.any { it.isNotBlank() } }
        } else contacts

        if (q.isBlank()) {
            baseList.take(25)
        } else {
            baseList.filter { c ->
                c.name.lowercase().contains(q) ||
                c.phoneNumbers.any { it.contains(q) }
            }.take(25)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Message", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    placeholder = { Text("Type a name or phone number") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (rawInput.isNotBlank()) {
                            IconButton(onClick = { rawInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(12.dp))

                // If typed input looks like a phone number, give direct send option
                val cleanedNumber = rawInput.trim()
                if (cleanedNumber.any { it.isDigit() }) {
                    Surface(
                        onClick = { onRecipientSelected(cleanedNumber, null) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Send to $cleanedNumber", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Filtered contact list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val primaryNum = contact.phoneNumbers.firstOrNull() ?: ""
                        Surface(
                            onClick = {
                                if (primaryNum.isNotBlank()) {
                                    onRecipientSelected(primaryNum, contact.name)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RivoAvatar(
                                    name = contact.name,
                                    photoUri = contact.photoUri,
                                    size = 40.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(primaryNum, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
