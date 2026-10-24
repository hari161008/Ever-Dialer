package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.SpeakerNotes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.coolappstore.everdialer.by.svhp.controller.sms.SmsEventBus
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
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
import com.coolappstore.everdialer.by.svhp.view.components.RivoScrollAnimatedItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollHapticsEffect
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.EaseInOutCubic
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
import com.coolappstore.everdialer.by.svhp.controller.util.formatDate
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
    val isSmsEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_ENABLED, false)
    }

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
    val hasInitialLoaded by smsVM.hasInitialLoaded.collectAsState()
    val searchQuery by smsVM.searchQuery.collectAsState()
    val isScrollAnimEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, true)
    }

    // Quik preference: unread at top
    val unreadAtTop = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_UNREAD_AT_TOP, true)
    }
    val autoColor = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, true)
    }
    val showAvatar = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SHOW_AVATAR, true)
    }
    val swipeRightAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, "none") ?: "none"
    }
    val swipeLeftAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, "none") ?: "none"
    }

    val colorScheme = MaterialTheme.colorScheme
    val leftSwipeActionItem = remember(swipeLeftAction, colorScheme) {
        SwipeActionHelper.resolveActionItem(swipeLeftAction, colorScheme)
    }
    val rightSwipeActionItem = remember(swipeRightAction, colorScheme) {
        SwipeActionHelper.resolveActionItem(swipeRightAction, colorScheme)
    }

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listStates = remember { mutableMapOf<String, LazyListState>() }
    val chipRowState = rememberLazyListState()

    val allCategories = remember(settingsVer) { prefs.getAllSmsCategories() }
    val hiddenCategories = remember(settingsVer) { prefs.getHiddenSmsCategories() }
    val visibleCategories = remember(allCategories, hiddenCategories) {
        allCategories.filterNot { hiddenCategories.contains(it.id) }
    }
    val customCategories = remember(settingsVer) { prefs.getCustomSmsCategories() }

    var selectedFilter by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_SMS_SELECTED_FILTER, "all") ?: "all")
    }

    LaunchedEffect(visibleCategories) {
        if (visibleCategories.isNotEmpty() && visibleCategories.none { it.id == selectedFilter }) {
            val fallback = visibleCategories.first().id
            selectedFilter = fallback
            prefs.setString(PreferenceManager.KEY_SMS_SELECTED_FILTER, fallback)
        }
    }

    val favoriteNumbers = remember(allContacts) {
        allContacts.filter { it.isFavorite }.flatMap { it.phoneNumbers }.toSet()
    }
    val favoriteNames = remember(allContacts) {
        allContacts.filter { it.isFavorite }.map { it.name.trim().lowercase() }.toSet()
    }

    val pinnedThreads = remember(settingsVer) { prefs.getPinnedSmsThreads() }
    val starredMessages by smsVM.starredMessages.collectAsState()

    val filterConversations: (String) -> List<SmsConversation> = remember(rawConversations, favoriteNumbers, favoriteNames, customCategories) {
        { filter ->
            val custom = customCategories.firstOrNull { it.id == filter }
            val byFilter = when {
                custom != null -> {
                    rawConversations.filter { conv ->
                        custom.contactPhones.any { numbersLikelyMatch(conv.address, it) }
                    }
                }
                filter == "favourites" -> {
                    rawConversations.filter { conv ->
                        favoriteNames.contains(conv.contactName?.trim()?.lowercase()) ||
                        favoriteNumbers.any { numbersLikelyMatch(conv.address, it) }
                    }
                }
                filter == "contacts" -> rawConversations.filter { !it.contactName.isNullOrBlank() }
                filter == "unknown" -> rawConversations.filter { it.contactName.isNullOrBlank() }
                filter == "otp" -> rawConversations.filter { conv ->
                    OTP_REGEX.containsMatchIn(conv.snippet)
                }
                else -> rawConversations
            }
            byFilter.sortedByDescending { it.date }
        }
    }

    val groupConversations: (List<SmsConversation>) -> Map<String, List<SmsConversation>> = remember(unreadAtTop) {
        { convList ->
            convList.groupBy { conv ->
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
    }

    val conversations = remember(rawConversations, selectedFilter, favoriteNumbers, favoriteNames, pinnedThreads) {
        filterConversations(selectedFilter)
    }

    val use24HourTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }

    LaunchedEffect(selectedFilter) {
        if (selectedFilter == "starred") {
            smsVM.refreshStarredMessages(prefs.getStarredSmsMessages())
        }
        listStates[selectedFilter]?.scrollToItem(0)
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

    LaunchedEffect(Unit) {
        SmsEventBus.newSmsEvent.collect {
            smsVM.refreshConversations()
            if (selectedFilter == "starred") {
                smsVM.refreshStarredMessages(prefs.getStarredSmsMessages())
            }
        }
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
                                    com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.recordNormalized(0.92f, 0.06f)
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
                            state = chipRowState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = visibleCategories.map { it.id to it.name }
                            itemsIndexed(filterOptions, key = { _, it -> it.first }) { index, (key, label) ->
                                val isSelected = selectedFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        if (selectedFilter == key) {
                                            coroutineScope.launch {
                                                listStates[key]?.animateScrollToItem(0)
                                            }
                                        } else {
                                            selectedFilter = key
                                            prefs.setString(PreferenceManager.KEY_SMS_SELECTED_FILTER, key)
                                            coroutineScope.launch {
                                                listStates.getOrPut(key) { LazyListState() }.scrollToItem(0)
                                            }
                                        }
                                        coroutineScope.launch {
                                            chipRowState.animateScrollToItem(index)
                                        }
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
                visible = !isSelectionMode && isSmsEnabled,
                enter = fadeIn(androidx.compose.animation.core.tween(400)) + scaleIn(androidx.compose.animation.core.tween(400), initialScale = 0.8f),
                exit = fadeOut(androidx.compose.animation.core.tween(400)) + scaleOut(androidx.compose.animation.core.tween(400), targetScale = 0.8f)
            ) {
                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
                val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                val usePrimary = isSaturatedActive || !isDynamic
                val fabBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                val fabFg = if (isSaturatedActive && isDark) Color.Black else if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                val fabShape = RoundedCornerShape(17.dp)
                val pillNav = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: ""
                val isNewMessageOpen = (currentRoute.contains("new_message", ignoreCase = true) || com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.isNewMessageOpening) && !com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.isNewMessageClosing
                val newMessageExpandScale by animateFloatAsState(
                    targetValue = if (isNewMessageOpen) 2.6f else 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = if (isNewMessageOpen) 600 else 800,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
                    ),
                    label = "newMessageExpandScale"
                )
                val newMessageFabElevation by animateDpAsState(
                    targetValue = if (isNewMessageOpen) 12.dp else 6.dp,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = if (isNewMessageOpen) 600 else 800,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
                    ),
                    label = "newMessageFabElevation"
                )

                var fabCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                val baseModifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        fabCoordinates = coordinates
                    }
                    .scale(newMessageExpandScale)
                    .then(if (pillNav) Modifier.navigationBarsPadding().padding(bottom = 92.dp) else Modifier.navigationBarsPadding().padding(bottom = 80.dp))
                    .then(if (isLandscape) Modifier.navigationBarsPadding().padding(bottom = 8.dp) else Modifier)

                FloatingActionButton(
                    onClick = {
                        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                            com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic(
                                context,
                                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                            )
                        }
                        fabCoordinates?.let { coords ->
                            val center = coords.boundsInRoot().center
                            val rootSize = coords.findRootCoordinates().size
                            com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.recordTap(
                                center.x,
                                center.y,
                                rootSize.width.toFloat(),
                                rootSize.height.toFloat()
                            )
                        } ?: com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.recordNormalized(0.88f, 0.92f)
                        com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.isNewMessageClosing = false
                        com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.isNewMessageOpening = true
                        com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker.isNewMessageOpen = true
                        navigator.navigate(NewMessageScreenDestination(initialText = null))
                    },
                    containerColor = fabBg,
                    contentColor = fabFg,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = newMessageFabElevation,
                        pressedElevation = newMessageFabElevation,
                        focusedElevation = newMessageFabElevation,
                        hoveredElevation = newMessageFabElevation
                    ),
                    modifier = baseModifier
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Message")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        AnimatedContent(
            targetState = selectedFilter,
            transitionSpec = {
                val filterKeys = listOf("all", "favourites", "starred", "contacts", "unknown", "otp")
                val fromIdx = filterKeys.indexOf(initialState).let { if (it == -1) 0 else it }
                val toIdx = filterKeys.indexOf(targetState).let { if (it == -1) 0 else it }
                val isForward = toIdx >= fromIdx
                val slideOffset = 240

                if (isForward) {
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { slideOffset.coerceAtMost(it / 3) }
                    ) + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(180, easing = FastOutLinearInEasing),
                        targetOffsetX = { -slideOffset.coerceAtMost(it / 3) }
                    ) + fadeOut(
                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                    ))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { -slideOffset.coerceAtMost(it / 3) }
                    ) + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(180, easing = FastOutLinearInEasing),
                        targetOffsetX = { slideOffset.coerceAtMost(it / 3) }
                    ) + fadeOut(
                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                    ))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            label = "SmsTabAnimatedContent"
        ) { currentFilter ->
            val currentListState = listStates.getOrPut(currentFilter) { LazyListState() }
            ScrollHapticsEffect(currentListState)
            val currentConversations = remember(rawConversations, currentFilter, favoriteNumbers, favoriteNames, customCategories) {
                filterConversations(currentFilter)
            }
            val (pinnedConvs, unpinnedConvs) = remember(currentConversations, pinnedThreads) {
                currentConversations.partition { pinnedThreads.contains(it.threadId) }
            }
            val currentGrouped = remember(unpinnedConvs, unreadAtTop) {
                groupConversations(unpinnedConvs)
            }

            LazyColumn(
                state = currentListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp + navBarBottom),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Default SMS App Warning Banner
                if (!isDefaultSms) {
                    item(key = "banner_default_sms") {
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
                if (isSmsEnabled && !hasSmsPermissions) {
                    item(key = "banner_permissions") {
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

                if (!isSmsEnabled) {
                    item(key = "sms_disabled_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Sms,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Text(
                                    "SMS is disabled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Turn on SMS in settings to view and send messages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        navigator.navigate(SmsSettingsScreenDestination())
                                    },
                                    shape = RoundedCornerShape(100)
                                ) {
                                    Text("Open SMS Settings")
                                }
                            }
                        }
                    }
                } else if (currentFilter == "starred") {
                    // Starred Messages Subsection
                    if (starredMessages.isEmpty()) {
                        if (isLoading || !hasInitialLoaded) {
                            item(key = "loading_state_starred") {
                                SmsLoadingView()
                            }
                        } else {
                            item(key = "empty_state_starred") {
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
                                                    Icons.Default.StarOutline,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "No starred messages",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Star important messages by long pressing them in any chat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = starredMessages,
                            key = { _, it -> "starred_${it.message.id}" }
                        ) { index, item ->
                            val isFirst = index == 0
                            val isLast = index == starredMessages.size - 1
                            val cornerRadius = 28.dp
                            val topStart = if (isFirst) cornerRadius else 0.dp
                            val topEnd = if (isFirst) cornerRadius else 0.dp
                            val bottomStart = if (isLast) cornerRadius else 0.dp
                            val bottomEnd = if (isLast) cornerRadius else 0.dp
                            val itemShape = RoundedCornerShape(
                                topStart = topStart,
                                topEnd = topEnd,
                                bottomStart = bottomStart,
                                bottomEnd = bottomEnd
                            )
                            var showStarredMenu by remember { mutableStateOf(false) }
                            RivoScrollAnimatedItem(enabled = isScrollAnimEnabled) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        shape = itemShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navigator.navigate(
                                                        SmsChatScreenDestination(
                                                            threadId = item.message.threadId,
                                                            address = item.message.address,
                                                            contactName = item.contactName,
                                                            photoUri = item.photoUri
                                                        )
                                                    )
                                                },
                                                onLongClick = {
                                                    showStarredMenu = true
                                                }
                                            )
                                    ) {
                                        Column {
                                            if (!isFirst) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    thickness = 0.5.dp
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (showAvatar) {
                                                    RivoAvatar(
                                                        name = item.contactName ?: item.message.address,
                                                        photoUri = item.photoUri,
                                                        autoColorAvatars = autoColor,
                                                        obeySolidIcons = false,
                                                        size = 48.dp
                                                    )
                                                    Spacer(Modifier.width(14.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = item.contactName ?: item.message.address,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Default.Star,
                                                                contentDescription = "Starred",
                                                                tint = Color(0xFFFFB300),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                text = if (item.message.date <= 0) "" else formatTimeOnly(item.message.date, use24HourTime),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = item.message.body.ifBlank { "MMS Message" },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    RivoDropdownMenu(
                                        expanded = showStarredMenu,
                                        onDismissRequest = { showStarredMenu = false }
                                    ) {
                                        RivoDropdownMenuItem(
                                            text = "Unstar message",
                                            icon = Icons.Default.StarOutline,
                                            iconTint = Color(0xFFFFB300),
                                            onClick = {
                                                showStarredMenu = false
                                                prefs.setSmsMessageStarred(item.message.id, false)
                                                smsVM.refreshStarredMessages(prefs.getStarredSmsMessages())
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Open in chat",
                                            icon = Icons.AutoMirrored.Outlined.Chat,
                                            onClick = {
                                                showStarredMenu = false
                                                navigator.navigate(
                                                    SmsChatScreenDestination(
                                                        threadId = item.message.threadId,
                                                        address = item.message.address,
                                                        contactName = item.contactName,
                                                        photoUri = item.photoUri
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            if (isLast) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } else {
                    // Empty or loading state for conversations
                    if (currentConversations.isEmpty()) {
                        if (isLoading || !hasInitialLoaded) {
                            item(key = "loading_state_$currentFilter") {
                                SmsLoadingView()
                            }
                        } else {
                            item(key = "empty_state_$currentFilter") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 60.dp)
                                        .animateItem(
                                            fadeInSpec = tween(260, easing = FastOutSlowInEasing),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                            fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                        ),
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
                }

                    // Pinned conversations container (above all other containers)
                    if (pinnedConvs.isNotEmpty()) {
                        item(key = "header_${currentFilter}_pinned", contentType = "sectionHeader") {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .animateItem(
                                        fadeInSpec = null,
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(450, easing = FastOutLinearInEasing)
                                    )
                            ) {
                                RivoSectionHeader(title = "Pinned")
                            }
                        }

                        itemsIndexed(
                            items = pinnedConvs,
                            key = { _, it -> "pinned_${it.threadId}" },
                            contentType = { _, _ -> "conversationItem" }
                        ) { index, conv ->
                            val isFirst = index == 0
                            val isLast = index == pinnedConvs.size - 1
                            val cornerRadius = 28.dp
                            val topStart = if (isFirst) cornerRadius else 0.dp
                            val topEnd = if (isFirst) cornerRadius else 0.dp
                            val bottomStart = if (isLast) cornerRadius else 0.dp
                            val bottomEnd = if (isLast) cornerRadius else 0.dp
                            val itemShape = RoundedCornerShape(
                                topStart = topStart,
                                topEnd = topEnd,
                                bottomStart = bottomStart,
                                bottomEnd = bottomEnd
                            )
                            val isSelected = selectedThreadIds.contains(conv.threadId)
                            val isPinned = true
                            val isMuted = prefs.isSmsMuted(conv.address)
                            var showConversationMenu by remember { mutableStateOf(false) }

                            val handleSwipeAction: (String) -> Unit = { actionKey ->
                                when (actionKey) {
                                    "call" -> makeCall(context, conv.address, null)
                                    "read" -> smsVM.markThreadAsRead(conv.threadId)
                                    "delete" -> threadToDelete = conv
                                }
                            }

                            RivoScrollAnimatedItem(enabled = isScrollAnimEnabled) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        shape = itemShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            if (!isFirst) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    thickness = 0.5.dp
                                                )
                                            }
                                            SwipeableItemContainer(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = tween(400, easing = FastOutSlowInEasing),
                                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                                    fadeOutSpec = tween(450, easing = FastOutLinearInEasing)
                                                ),
                                                leftAction = leftSwipeActionItem,
                                                rightAction = rightSwipeActionItem,
                                                onSwipeLeft = { handleSwipeAction(swipeLeftAction) },
                                                onSwipeRight = { handleSwipeAction(swipeRightAction) },
                                                enabled = !isSelectionMode,
                                                shape = itemShape
                                            ) {
                                                ConversationItemRow(
                                                    conversation = conv,
                                                    timeStr = if (conv.date <= 0) "" else formatDate(conv.date, use24HourTime),
                                                    isSelected = isSelected,
                                                    isSelectionMode = isSelectionMode,
                                                    autoColor = autoColor,
                                                    showAvatar = showAvatar,
                                                    isPinned = isPinned,
                                                    isMuted = isMuted,
                                                    shape = itemShape,
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            if (isSelected) selectedThreadIds.remove(conv.threadId)
                                                            else selectedThreadIds.add(conv.threadId)
                                                        } else {
                                                            if (conv.threadId > 0) {
                                                                smsVM.loadThreadMessages(conv.threadId)
                                                            }
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
                                                        if (isSelectionMode) {
                                                            if (isSelected) selectedThreadIds.remove(conv.threadId)
                                                            else selectedThreadIds.add(conv.threadId)
                                                        } else {
                                                            showConversationMenu = true
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

                                    RivoDropdownMenu(
                                        expanded = showConversationMenu,
                                        onDismissRequest = { showConversationMenu = false }
                                    ) {
                                        RivoDropdownMenuItem(
                                            text = "Unpin chat",
                                            icon = Icons.Default.PushPin,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            onClick = {
                                                showConversationMenu = false
                                                prefs.setSmsThreadPinned(conv.threadId, false)
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = if (isMuted) "Unmute notifications" else "Mute notifications",
                                            icon = if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            iconTint = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            onClick = {
                                                showConversationMenu = false
                                                prefs.setSmsMuted(conv.address, !isMuted)
                                                Toast.makeText(context, if (isMuted) "Unmuted ${conv.contactName ?: conv.address}" else "Muted notifications from ${conv.contactName ?: conv.address}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Block number",
                                            icon = Icons.Outlined.Block,
                                            iconTint = MaterialTheme.colorScheme.error,
                                            isDestructive = true,
                                            onClick = {
                                                showConversationMenu = false
                                                com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.block(context, prefs, conv.address)
                                                Toast.makeText(context, "Blocked ${conv.contactName ?: conv.address}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = if (conv.isRead) "Mark as unread" else "Mark as read",
                                            icon = if (conv.isRead) Icons.Outlined.MarkChatUnread else Icons.Outlined.MarkChatRead,
                                            onClick = {
                                                showConversationMenu = false
                                                smsVM.markThreadAsRead(conv.threadId)
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Call",
                                            icon = Icons.Default.Call,
                                            onClick = {
                                                showConversationMenu = false
                                                makeCall(context, conv.address, null)
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Select",
                                            icon = Icons.Default.CheckBox,
                                            onClick = {
                                                showConversationMenu = false
                                                selectedThreadIds.add(conv.threadId)
                                            }
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Delete",
                                            icon = Icons.Outlined.Delete,
                                            iconTint = MaterialTheme.colorScheme.error,
                                            isDestructive = true,
                                            onClick = {
                                                showConversationMenu = false
                                                threadToDelete = conv
                                            }
                                        )
                                    }
                                }
                            }
                            if (isLast) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Conversation items grouped by date header
                    currentGrouped.forEach { (header, convsInGroup) ->
                        if (header.isNotBlank()) {
                            item(key = "header_${currentFilter}_$header", contentType = "sectionHeader") {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                        .animateItem(
                                            fadeInSpec = null,
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                            fadeOutSpec = tween(450, easing = FastOutLinearInEasing)
                                        )
                                ) {
                                    RivoSectionHeader(title = header)
                                }
                            }
                        }

                        itemsIndexed(
                            items = convsInGroup,
                            key = { _, it -> it.threadId },
                            contentType = { _, _ -> "conversationItem" }
                        ) { index, conv ->
                            val isFirst = index == 0
                            val isLast = index == convsInGroup.size - 1
                            val cornerRadius = 28.dp
                            val topStart = if (isFirst) cornerRadius else 0.dp
                            val topEnd = if (isFirst) cornerRadius else 0.dp
                            val bottomStart = if (isLast) cornerRadius else 0.dp
                            val bottomEnd = if (isLast) cornerRadius else 0.dp
                            val itemShape = RoundedCornerShape(
                                topStart = topStart,
                                topEnd = topEnd,
                                bottomStart = bottomStart,
                                bottomEnd = bottomEnd
                            )
                            val isSelected = selectedThreadIds.contains(conv.threadId)
                            val isPinned = pinnedThreads.contains(conv.threadId)
                            val isMuted = prefs.isSmsMuted(conv.address)
                            var showConversationMenu by remember { mutableStateOf(false) }

                            val handleSwipeAction: (String) -> Unit = { actionKey ->
                                when (actionKey) {
                                    "call" -> makeCall(context, conv.address, null)
                                    "read" -> smsVM.markThreadAsRead(conv.threadId)
                                    "delete" -> threadToDelete = conv
                                }
                            }

                            RivoScrollAnimatedItem(enabled = isScrollAnimEnabled) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        shape = itemShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            if (!isFirst) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    thickness = 0.5.dp
                                                )
                                            }
                                            SwipeableItemContainer(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = tween(400, easing = FastOutSlowInEasing),
                                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                                    fadeOutSpec = tween(450, easing = FastOutLinearInEasing)
                                                ),
                                                leftAction = leftSwipeActionItem,
                                                rightAction = rightSwipeActionItem,
                                                onSwipeLeft = { handleSwipeAction(swipeLeftAction) },
                                                onSwipeRight = { handleSwipeAction(swipeRightAction) },
                                                enabled = !isSelectionMode,
                                                shape = itemShape
                                            ) {
                                                ConversationItemRow(
                                                    conversation = conv,
                                                    timeStr = if (conv.date <= 0) "" else formatTimeOnly(conv.date, use24HourTime),
                                                    isSelected = isSelected,
                                                    isSelectionMode = isSelectionMode,
                                                    autoColor = autoColor,
                                                    showAvatar = showAvatar,
                                                    isPinned = isPinned,
                                                    isMuted = isMuted,
                                                    shape = itemShape,
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            if (isSelected) selectedThreadIds.remove(conv.threadId)
                                                            else selectedThreadIds.add(conv.threadId)
                                                        } else {
                                                            if (conv.threadId > 0) {
                                                                smsVM.loadThreadMessages(conv.threadId)
                                                            }
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
                                                        if (isSelectionMode) {
                                                            if (isSelected) selectedThreadIds.remove(conv.threadId)
                                                            else selectedThreadIds.add(conv.threadId)
                                                        } else {
                                                            showConversationMenu = true
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

                                    RivoDropdownMenu(
                                        expanded = showConversationMenu,
                                        onDismissRequest = { showConversationMenu = false }
                                    ) {
                                        RivoDropdownMenuItem(
                                            text = if (isPinned) "Unpin chat" else "Pin chat",
                                            icon = Icons.Default.PushPin,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            onClick = {
                                                showConversationMenu = false
                                                prefs.setSmsThreadPinned(conv.threadId, !isPinned)
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = if (isMuted) "Unmute notifications" else "Mute notifications",
                                            icon = if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            iconTint = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            onClick = {
                                                showConversationMenu = false
                                                prefs.setSmsMuted(conv.address, !isMuted)
                                                Toast.makeText(context, if (isMuted) "Unmuted ${conv.contactName ?: conv.address}" else "Muted notifications from ${conv.contactName ?: conv.address}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Block number",
                                            icon = Icons.Outlined.Block,
                                            iconTint = MaterialTheme.colorScheme.error,
                                            isDestructive = true,
                                            onClick = {
                                                showConversationMenu = false
                                                com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.block(context, prefs, conv.address)
                                                Toast.makeText(context, "Blocked ${conv.contactName ?: conv.address}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = if (conv.isRead) "Mark as unread" else "Mark as read",
                                            icon = if (conv.isRead) Icons.Outlined.MarkChatUnread else Icons.Outlined.MarkChatRead,
                                            onClick = {
                                                showConversationMenu = false
                                                smsVM.markThreadAsRead(conv.threadId)
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Call",
                                            icon = Icons.Default.Call,
                                            iconTint = Color(0xFF4CAF50),
                                            onClick = {
                                                showConversationMenu = false
                                                makeCall(context, conv.address, null)
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Select",
                                            icon = Icons.Default.CheckBox,
                                            onClick = {
                                                showConversationMenu = false
                                                selectedThreadIds.add(conv.threadId)
                                            }
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Delete",
                                            icon = Icons.Outlined.Delete,
                                            iconTint = MaterialTheme.colorScheme.error,
                                            isDestructive = true,
                                            onClick = {
                                                showConversationMenu = false
                                                threadToDelete = conv
                                            }
                                        )
                                    }
                                }
                            }
                            if (isLast) Spacer(modifier = Modifier.height(12.dp))
                        }
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
    showAvatar: Boolean = true,
    isPinned: Boolean = false,
    isMuted: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val targetBgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        !conversation.isRead -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }
    val animatedBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "ConversationRowBgAnim"
    )

    Surface(
        shape = shape,
        color = animatedBgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
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

            if (showAvatar) {
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
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.contactName ?: conversation.address,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPinned) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (isMuted) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

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

@Composable
private fun SmsLoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "smsLoading")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smsLoadingScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smsLoadingAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, delayMillis = index * 200, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, delayMillis = index * 200, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha$index"
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                                alpha = dotAlpha
                            }
                    ) {}
                }
            }

            Text(
                text = "Loading messages…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha)
            )
        }
    }
}



