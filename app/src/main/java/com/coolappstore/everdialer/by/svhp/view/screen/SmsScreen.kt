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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.SpeakerNotes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
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
import android.widget.Toast
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.evercallrecorder.by.svhp.ui.common.SwipeableItemContainer
import com.coolappstore.evercallrecorder.by.svhp.ui.common.SwipeActionItem
import com.coolappstore.everdialer.by.svhp.controller.FloatingSmsService
import com.coolappstore.everdialer.by.svhp.controller.util.SwipeActionHelper
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
import com.ramcosta.composedestinations.generated.destinations.NewMessageScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.formatDateHeader
import com.coolappstore.everdialer.by.svhp.controller.util.formatTimeOnly
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import java.text.SimpleDateFormat
import java.util.*

private val OTP_REGEX = Regex(
    """\b(otp|passcode|one[- ]time (?:password|passcode|code)|(?:verification|authentication|auth|security|secret|login|access|confirmation)[- ](?:code|pin|password)|your\s+(?:\w+\s+)?code\s+is|is\s+your\s+(?:\w+\s+)?(?:code|pin)|\d{4,8}\s+is\s+your|use\s+code\s+\d{4,8}|enter\s+(?:this\s+)?otp)\b|(?:\b(?:code|pin|otp)\s*[:=]\s*\d{4,8}\b)""",
    RegexOption.IGNORE_CASE
)

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
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, "none") ?: "none"
    }
    val swipeLeftAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, "none") ?: "none"
    }

    val colorScheme = MaterialTheme.colorScheme
    val leftSwipeActionItem = remember(swipeLeftAction, colorScheme) {
        when (swipeLeftAction) {
            "delete" -> SwipeActionItem("delete", "Delete", Icons.Default.Delete, colorScheme.error, colorScheme.onError, isDestructive = true)
            "read" -> SwipeActionItem("read", "Mark Read", Icons.Outlined.MarkChatRead, colorScheme.secondary, colorScheme.onSecondary, isDestructive = false)
            else -> null
        }
    }
    val rightSwipeActionItem = remember(swipeRightAction, colorScheme) {
        when (swipeRightAction) {
            "call" -> SwipeActionItem("call", "Call", Icons.Default.Call, colorScheme.primary, colorScheme.onPrimary, isDestructive = false)
            "read" -> SwipeActionItem("read", "Mark Read", Icons.Outlined.MarkChatRead, colorScheme.secondary, colorScheme.onSecondary, isDestructive = false)
            "delete" -> SwipeActionItem("delete", "Delete", Icons.Default.Delete, colorScheme.error, colorScheme.onError, isDestructive = true)
            else -> null
        }
    }

    var selectedFilter by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_SMS_SELECTED_FILTER, "all") ?: "all")
    }

    val favoriteNumbers = remember(allContacts) {
        allContacts.filter { it.isFavorite }.flatMap { it.phoneNumbers }.toSet()
    }
    val favoriteNames = remember(allContacts) {
        allContacts.filter { it.isFavorite }.map { it.name.trim().lowercase() }.toSet()
    }

    val conversations = remember(rawConversations, selectedFilter, favoriteNumbers, favoriteNames) {
        val byFilter = when (selectedFilter) {
            "favourites" -> {
                rawConversations.filter { conv ->
                    favoriteNames.contains(conv.contactName?.trim()?.lowercase()) ||
                    favoriteNumbers.any { numbersLikelyMatch(conv.address, it) }
                }
            }
            "contacts" -> rawConversations.filter { !it.contactName.isNullOrBlank() }
            "unknown" -> rawConversations.filter { it.contactName.isNullOrBlank() }
            "otp" -> rawConversations.filter { conv ->
                OTP_REGEX.containsMatchIn(conv.snippet)
            }
            else -> rawConversations
        }
        byFilter.sortedByDescending { it.date }
    }

    val use24HourTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }

    val groupedConversations = remember(conversations, unreadAtTop) {
        conversations.groupBy { conv ->
            if (conv.date <= 0) "Older" else formatDateHeader(conv.date)
        }.mapValues { (_, list) ->
            if (unreadAtTop) {
                val unread = list.filter { !it.isRead }
                val read = list.filter { it.isRead }
                unread + read
            } else {
                list
            }
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
    var threadToDelete by remember { mutableStateOf<SmsConversation?>(null) }
    var showDeleteMultipleDialog by remember { mutableStateOf(false) }
    var showBlockMultipleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cleanSharedText) {
        if (!cleanSharedText.isNullOrBlank()) {
            navigator.navigate(NewMessageScreenDestination(initialText = cleanSharedText))
        }
    }

    LaunchedEffect(Unit) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
        smsVM.refreshConversations()
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) +
                    slideInVertically(animationSpec = androidx.compose.animation.core.tween(400)) { -it / 3 } togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) +
                    slideOutVertically(animationSpec = androidx.compose.animation.core.tween(400)) { -it / 3 }
                },
                label = "SmsTopBarAnim"
            ) { selectionMode ->
                if (selectionMode) {
                    // Quik Multi-Select Top Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedThreadIds.size} selected",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedThreadIds.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                SelectionActionButton(
                                    icon = Icons.Default.SelectAll,
                                    label = if (selectedThreadIds.size == conversations.size && conversations.isNotEmpty()) "Deselect" else "All",
                                    onClick = {
                                        if (selectedThreadIds.size == conversations.size) {
                                            selectedThreadIds.clear()
                                        } else {
                                            selectedThreadIds.clear()
                                            selectedThreadIds.addAll(conversations.map { it.threadId })
                                        }
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.Outlined.MarkChatRead,
                                    label = "Read",
                                    onClick = {
                                        selectedThreadIds.forEach { tid ->
                                            smsVM.markThreadAsRead(tid)
                                        }
                                        selectedThreadIds.clear()
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.AutoMirrored.Outlined.SpeakerNotes,
                                    label = "Float",
                                    onClick = {
                                        if (!android.provider.Settings.canDrawOverlays(context)) {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        } else {
                                            selectedThreadIds.firstOrNull()?.let { tid ->
                                                conversations.find { it.threadId == tid }?.let { conv ->
                                                    FloatingSmsService.start(
                                                        context = context,
                                                        threadId = conv.threadId,
                                                        address = conv.address,
                                                        contactName = conv.contactName,
                                                        photoUri = conv.photoUri
                                                    )
                                                }
                                            }
                                            if (selectedThreadIds.size > 1) {
                                                Toast.makeText(context, "Opened floating window for first selected chat", Toast.LENGTH_SHORT).show()
                                            }
                                            selectedThreadIds.clear()
                                        }
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.Outlined.Block,
                                    label = "Block",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        showBlockMultipleDialog = true
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.Outlined.Delete,
                                    label = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        showDeleteMultipleDialog = true
                                    }
                                )
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

                        // Filter Pills: All, Favourites, Contacts, Unknown, OTP
                        val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                        val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
                        val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                        val usePrimary = isSaturatedActive || !isDynamic
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = listOf(
                                "all" to "All",
                                "favourites" to "Favourites",
                                "contacts" to "Contacts",
                                "unknown" to "Unknown",
                                "otp" to "OTP"
                            )
                            items(filterOptions, key = { it.first }) { (key, label) ->
                                val isSelected = selectedFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFilter = key
                                        prefs.setString(PreferenceManager.KEY_SMS_SELECTED_FILTER, key)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(50.dp),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn(androidx.compose.animation.core.tween(400)) + scaleIn(androidx.compose.animation.core.tween(400), initialScale = 0.8f),
                exit = fadeOut(androidx.compose.animation.core.tween(400)) + scaleOut(androidx.compose.animation.core.tween(400), targetScale = 0.8f)
            ) {
                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
                val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                val usePrimary = isSaturatedActive || !isDynamic
                val fabBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                val fabFg = if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                val fabShape = RoundedCornerShape(17.dp)
                val pillNav = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val baseModifier = Modifier
                    .then(if (pillNav) Modifier.navigationBarsPadding().padding(bottom = 92.dp) else Modifier)
                    .then(if (isLandscape) Modifier.navigationBarsPadding().padding(bottom = 8.dp) else Modifier)

                FloatingActionButton(
                    onClick = {
                        navigator.navigate(NewMessageScreenDestination(initialText = null))
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

            // Conversation items grouped by date header
            groupedConversations.forEach { (header, convsInGroup) ->
                if (header.isNotBlank()) {
                    item(key = "header_$header", contentType = "sectionHeader") {
                        Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                            RivoSectionHeader(title = header)
                        }
                    }
                }

                items(
                    items = convsInGroup,
                    key = { it.threadId },
                    contentType = { "conversationItem" }
                ) { conv ->
                    val isSelected = selectedThreadIds.contains(conv.threadId)

                    val handleSwipeAction: (String) -> Unit = { actionKey ->
                        when (actionKey) {
                            "call" -> makeCall(context, conv.address, null)
                            "read" -> smsVM.markThreadAsRead(conv.threadId)
                            "delete" -> threadToDelete = conv
                        }
                    }

                    SwipeableItemContainer(
                        leftAction = leftSwipeActionItem,
                        rightAction = rightSwipeActionItem,
                        onSwipeLeft = { handleSwipeAction(swipeLeftAction) },
                        onSwipeRight = { handleSwipeAction(swipeRightAction) },
                        enabled = !isSelectionMode,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ConversationItemRow(
                            conversation = conv,
                            timeStr = if (conv.date <= 0) "" else formatTimeOnly(conv.date, use24HourTime),
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
                            }
                        )
                    }
                }
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

    // Block multiple selected threads confirmation
    if (showBlockMultipleDialog) {
        AlertDialog(
            onDismissRequest = { showBlockMultipleDialog = false },
            icon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Block ${selectedThreadIds.size} conversations?") },
            text = { Text("Calls and messages from the selected numbers will be blocked.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toBlock = conversations.filter { it.threadId in selectedThreadIds }
                        toBlock.forEach { conv ->
                            if (conv.address.isNotBlank()) {
                                BlockedNumbersManager.block(context, prefs, conv.address)
                            }
                        }
                        selectedThreadIds.clear()
                        showBlockMultipleDialog = false
                        Toast.makeText(context, "Blocked selected conversations", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockMultipleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (enabled) tint else tint.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItemRow(
    conversation: SmsConversation,
    timeStr: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    autoColor: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val targetBgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        !conversation.isRead -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val animatedBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "ConversationRowBgAnim"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = animatedBgColor,
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
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                        expandHorizontally(animationSpec = androidx.compose.animation.core.tween(300)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                        shrinkHorizontally(animationSpec = androidx.compose.animation.core.tween(300))
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAvatarClick() }
            ) {
                RivoAvatar(
                    name = conversation.contactName ?: conversation.address,
                    photoUri = conversation.photoUri,
                    autoColorAvatars = autoColor,
                    obeySolidIcons = false,
                    size = 48.dp
                )
            }
            Spacer(Modifier.width(14.dp))

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

                    if (timeStr.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!conversation.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.Normal
                        )
                    }
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
        }
    }
}


