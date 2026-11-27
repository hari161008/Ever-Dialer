package com.coolappstore.everdialer.by.svhp.view.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.coolappstore.everdialer.by.svhp.controller.FloatingSmsService
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.TextUnit
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.sms.SmsEventBus
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.SmsCache
import com.coolappstore.everdialer.by.svhp.controller.util.VoiceSearchHelper
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.getPhoneAccountForSimSlot
import com.coolappstore.everdialer.by.svhp.view.components.SimSlotBadge
import android.telecom.TelecomManager
import com.coolappstore.everdialer.by.svhp.controller.util.rememberVoiceSearchLauncher
import com.coolappstore.everdialer.by.svhp.controller.util.ScheduledSmsManager
import com.coolappstore.everdialer.by.svhp.modal.data.ScheduledSmsEntry
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsClickTracker
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ScheduledMessagesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SmsChatScreen(
    navigator: DestinationsNavigator,
    threadId: Long,
    address: String,
    contactName: String? = null,
    photoUri: String? = null,
    initialText: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isThreadLoading by smsVM.isThreadLoading.collectAsState()

    DisposableEffect(threadId, address) {
        smsVM.prepareThread(threadId, address)
        onDispose {
            smsVM.clearActiveThread(threadId)
        }
    }

    var effectiveThreadId by remember(threadId) { mutableStateOf(threadId) }
    val vmCurrentMessages by smsVM.currentThreadMessages.collectAsState()
    val cachedMessages: List<SmsMessage> = remember(effectiveThreadId, address) {
        (if (effectiveThreadId > 0) SmsCache.getThreadMessages(effectiveThreadId) else null)
            ?: (if (address.isNotBlank()) SmsCache.getThreadMessagesByAddress(address) else null)
            ?: emptyList()
    }
    val messages: List<SmsMessage> = remember(vmCurrentMessages, cachedMessages, effectiveThreadId) {
        if (vmCurrentMessages.isNotEmpty() && (effectiveThreadId <= 0 || vmCurrentMessages.any { it.threadId == effectiveThreadId })) {
            vmCurrentMessages
        } else if (cachedMessages.isNotEmpty()) {
            cachedMessages
        } else {
            vmCurrentMessages
        }
    }
    var hasCompletedInitialFetch by remember(effectiveThreadId, address) {
        mutableStateOf<Boolean>(cachedMessages.isNotEmpty())
    }
    LaunchedEffect(vmCurrentMessages, isThreadLoading) {
        if (vmCurrentMessages.isNotEmpty() || !isThreadLoading) {
            hasCompletedInitialFetch = true
        }
    }
    val listState = remember(effectiveThreadId) { LazyListState() }

    val chatListItems = remember(messages) {
        val items = mutableListOf<ChatListItem>()
        var currentDayKey = -1L
        for (message in messages) {
            val dayKey = getDayKey(message.date)
            if (dayKey != currentDayKey) {
                currentDayKey = dayKey
                items.add(ChatListItem.DateHeader(dayKey, formatChatDateHeader(message.date)))
            }
            items.add(ChatListItem.Message(message))
        }
        items
    }

    val latestMessageId = remember(messages) { messages.lastOrNull()?.id }
    var initialScrolledThreadId by remember { mutableStateOf<Long?>(null) }
    var userJustSentMessage by remember { mutableStateOf(false) }

    // Scroll to bottom immediately once when thread messages first load
    LaunchedEffect(effectiveThreadId, messages.isNotEmpty()) {
        if (messages.isNotEmpty() && initialScrolledThreadId != effectiveThreadId) {
            initialScrolledThreadId = effectiveThreadId
            listState.scrollToItem(0)
        }
    }

    // Scroll to bottom when user explicitly sends a message
    LaunchedEffect(latestMessageId) {
        if (userJustSentMessage && latestMessageId != null) {
            userJustSentMessage = false
            listState.animateScrollToItem(0)
        }
    }

    val settingsVer by prefs.settingsChanged.collectAsState()
    val chatFontSize = remember(settingsVer) {
        prefs.getFloat(PreferenceManager.KEY_SMS_CHAT_TEXT_SIZE, PreferenceManager.DEFAULT_SMS_CHAT_TEXT_SIZE)
    }
    val sendDelaySeconds = remember(settingsVer) {
        prefs.getInt(PreferenceManager.KEY_SMS_SEND_DELAY_SECONDS, 0)
    }
    val signatureEnabled = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SIGNATURE_ENABLED, false)
    }
    val signatureText = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SIGNATURE, "") ?: ""
    }
    val stripUnicode = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_STRIP_UNICODE, false)
    }
    val showStt = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_SHOW_STT, true)
    }
    val autoColor = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, true)
    }

    val sendButtonScale = remember { Animatable(1f) }
    val sendIconOffset = remember { Animatable(0f) }
    val sendIconRotation = remember { Animatable(0f) }

    var messageText by remember(initialText, effectiveThreadId, address) {
        val initial = if (!initialText.isNullOrBlank()) initialText else prefs.getSmsDraft(effectiveThreadId, address)
        mutableStateOf(initial)
    }
    var showMenu by remember { mutableStateOf(false) }
    var isPinned by remember(settingsVer, effectiveThreadId) {
        mutableStateOf(prefs.isSmsThreadPinned(effectiveThreadId))
    }
    var isMuted by remember(settingsVer, address) {
        mutableStateOf(prefs.isSmsMuted(address))
    }
    var isBlocked by remember(settingsVer, address) {
        mutableStateOf(BlockedNumbersManager.isBlocked(context, prefs, address))
    }
    var messageToDelete by remember { mutableStateOf<SmsMessage?>(null) }
    var messageToSelect by remember { mutableStateOf<SmsMessage?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteMultipleMessagesDialog by remember { mutableStateOf(false) }
    var showDefaultSmsRequiredDialog by remember { mutableStateOf(false) }
    var isDefaultSms by remember { mutableStateOf(DefaultSmsManager.isDefaultSms(context)) }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
    }

    // Multi-select state
    val selectedMessageIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedMessageIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedMessageIds.clear()
    }

    // Quik Delayed Sending Countdown state
    var pendingSendJob by remember { mutableStateOf<Job?>(null) }
    var pendingCountdown by remember { mutableIntStateOf(0) }
    var pendingMessagePayload by remember { mutableStateOf<String?>(null) }

    // Speech to text launcher
    val sttLauncher = rememberVoiceSearchLauncher { spokenText ->
        val updated = if (messageText.isBlank()) spokenText else "$messageText $spokenText"
        messageText = updated
        prefs.saveSmsDraft(effectiveThreadId, address, updated)
    }

    // Resolve matched contact accurately using PhoneNumberUtils
    val matchedContact = remember(allContacts, address) {
        if (address.isBlank()) null
        else allContacts.firstOrNull { c ->
            c.phoneNumbers.any { num ->
                PhoneNumberUtils.compare(context, num, address)
            }
        }
    }

    // Resolve contact photo: use explicitly passed photoUri first, then matchedContact's photoUri.
    // If none exists, keep null (NEVER fallback to other contacts!)
    val resolvedPhotoUri = remember(photoUri, matchedContact) {
        if (!photoUri.isNullOrBlank()) photoUri
        else matchedContact?.photoUri
    }

    // Available SIM subscriptions
    val subscriptionManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        } else null
    }

    val activeSims: List<SubscriptionInfo> = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_PHONE_STATE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    val sortedSims: List<SubscriptionInfo> = remember(activeSims) {
        activeSims.sortedBy { it.simSlotIndex }
    }

    var userSelectedSimManually by remember { mutableStateOf(false) }

    val initialSubId = remember(sortedSims) {
        if (sortedSims.isEmpty()) null
        else {
            val perAddressSubId = if (address.isNotBlank()) prefs.getInt("last_used_sms_sub_id_$address", -1) else -1
            if (perAddressSubId != -1 && sortedSims.any { it.subscriptionId == perAddressSubId }) {
                perAddressSubId
            } else {
                val globalSubId = prefs.getInt("last_used_sms_sub_id", -1)
                if (globalSubId != -1 && sortedSims.any { it.subscriptionId == globalSubId }) {
                    globalSubId
                } else {
                    sortedSims.firstOrNull()?.subscriptionId
                }
            }
        }
    }

    var selectedSubId by remember(sortedSims) {
        mutableStateOf(initialSubId)
    }

    LaunchedEffect(messages) {
        val hasStoredPref = address.isNotBlank() && prefs.getInt("last_used_sms_sub_id_$address", -1) != -1
        if (!userSelectedSimManually && !hasStoredPref && messages.isNotEmpty() && sortedSims.size == 2) {
            val lastUsedInChat = messages.lastOrNull { m ->
                m.subId != null && sortedSims.any { it.subscriptionId == m.subId }
            }?.subId
            if (lastUsedInChat != null) {
                selectedSubId = lastUsedInChat
            }
        }
    }

    LaunchedEffect(effectiveThreadId, address) {
        while (isActive) {
            if (effectiveThreadId <= 0 && address.isNotBlank()) {
                val resolved = smsVM.getOrCreateThreadId(address)
                if (resolved > 0) {
                    effectiveThreadId = resolved
                }
            }
            if (effectiveThreadId > 0) {
                smsVM.loadThreadMessages(effectiveThreadId, address)
            }
            delay(1500)
        }
    }

    LaunchedEffect(address) {
        SmsEventBus.newSmsEvent.collect { eventThreadId ->
            if (effectiveThreadId <= 0 && address.isNotBlank()) {
                val resolved = smsVM.getOrCreateThreadId(address)
                if (resolved > 0) {
                    effectiveThreadId = resolved
                    smsVM.loadThreadMessages(resolved, address)
                }
            } else if (effectiveThreadId > 0) {
                if (eventThreadId == null || eventThreadId == effectiveThreadId) {
                    smsVM.loadThreadMessages(effectiveThreadId, address)
                }
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Scheduled SMS state
    var showScheduleMenu by remember { mutableStateOf(false) }
    var scheduledVersion by remember { mutableIntStateOf(0) }
    val scheduledMessages = remember(scheduledVersion, settingsVer, effectiveThreadId, address) {
        ScheduledSmsManager.getEntriesForThreadOrAddress(prefs, effectiveThreadId, address)
    }

    fun scheduleMessageAt(timestamp: Long) {
        if (messageText.isBlank()) return
        val textToSchedule = messageText.trim()
        messageText = ""
        prefs.clearSmsDraft(effectiveThreadId, address)
        val entry = ScheduledSmsEntry(
            id = UUID.randomUUID().toString(),
            threadId = effectiveThreadId,
            address = address,
            body = textToSchedule,
            subId = selectedSubId,
            scheduledTime = timestamp
        )
        ScheduledSmsManager.addEntry(context, prefs, entry)
        scheduledVersion++
        Toast.makeText(context, "Message scheduled", Toast.LENGTH_SHORT).show()
    }

    var showModernTimePicker by remember { mutableStateOf(false) }
    var showModernDatePicker by remember { mutableStateOf(false) }
    var scheduledDateMillis by remember { mutableStateOf<Long?>(null) }

    val initialHour = remember {
        val c = Calendar.getInstance()
        (c.get(Calendar.HOUR_OF_DAY) + ((c.get(Calendar.MINUTE) + 5) / 60)) % 24
    }
    val initialMinute = remember {
        (Calendar.getInstance().get(Calendar.MINUTE) + 5) % 60
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    fun openClockPicker() {
        scheduledDateMillis = null
        showModernTimePicker = true
    }

    fun openDatePicker() {
        showModernDatePicker = true
    }

    fun doSendMessage(rawText: String) {
        var finalBody = rawText
        if (signatureEnabled && signatureText.isNotBlank()) {
            finalBody = "$finalBody\n$signatureText"
        }
        if (stripUnicode) {
            finalBody = Normalizer.normalize(finalBody, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        }

        selectedSubId?.let { subId ->
            prefs.setInt("last_used_sms_sub_id", subId)
            if (address.isNotBlank()) {
                prefs.setInt("last_used_sms_sub_id_$address", subId)
            }
        }

        userJustSentMessage = true
        smsVM.sendMessage(address, finalBody, selectedSubId) { success ->
            if (!success) {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
        coroutineScope.launch {
            delay(50)
            listState.animateScrollToItem(0)
        }
    }

    fun initiateSend() {
        if (messageText.isBlank()) return
        val textToProcess = messageText.trim()
        messageText = ""
        prefs.clearSmsDraft(effectiveThreadId, address)

        // Smooth and slow tactile animation when send button is clicked
        coroutineScope.launch {
            launch {
                sendButtonScale.animateTo(
                    targetValue = 0.72f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
                sendButtonScale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
                sendButtonScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                sendIconOffset.animateTo(
                    targetValue = -12f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
                sendIconOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                sendIconRotation.animateTo(
                    targetValue = -35f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
                sendIconRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing)
                )
            }
        }

        if (sendDelaySeconds > 0) {
            pendingMessagePayload = textToProcess
            pendingCountdown = sendDelaySeconds
            pendingSendJob?.cancel()
            pendingSendJob = coroutineScope.launch {
                while (pendingCountdown > 0) {
                    delay(1000)
                    pendingCountdown -= 1
                }
                val payload = pendingMessagePayload
                pendingMessagePayload = null
                if (!payload.isNullOrBlank()) {
                    doSendMessage(payload)
                }
            }
        } else {
            doSendMessage(textToProcess)
        }
    }

    fun cancelPendingSend() {
        pendingSendJob?.cancel()
        pendingSendJob = null
        pendingMessagePayload?.let { restored ->
            messageText = restored
        }
        pendingMessagePayload = null
        pendingCountdown = 0
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + slideInVertically(animationSpec = tween(260)) { -it / 3 }) togetherWith
                    (fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { -it / 3 })
                },
                label = "ChatTopBarAnim"
            ) { selectionMode ->
                if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedMessageIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        FilledIconButton(
                            onClick = { selectedMessageIds.clear() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(46.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(24.dp))
                        }
                    },
                    actions = {
                        FilledIconButton(
                            onClick = {
                                if (selectedMessageIds.size == messages.size) {
                                    selectedMessageIds.clear()
                                } else {
                                    selectedMessageIds.clear()
                                    selectedMessageIds.addAll(messages.map { it.id })
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val selectedBodies = messages
                                    .filter { selectedMessageIds.contains(it.id) }
                                    .joinToString("\n") { it.body }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SMS Messages", selectedBodies))
                                Toast.makeText(context, "${selectedMessageIds.size} message(s) copied", Toast.LENGTH_SHORT).show()
                                selectedMessageIds.clear()
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                showDeleteMultipleMessagesDialog = true
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        var pillCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .onGloballyPositioned { pillCoords = it }
                                .clip(CircleShape)
                                .clickable {
                                    pillCoords?.let { coords ->
                                        val center = coords.boundsInRoot().center
                                        val rootSize = coords.findRootCoordinates().size
                                        SettingsClickTracker.recordTap(
                                            center.x, center.y, rootSize.width.toFloat(), rootSize.height.toFloat()
                                        )
                                    }
                                    navigator.navigate(
                                        ContactDetailsScreenDestination(
                                            contactId = matchedContact?.id,
                                            phoneNumber = address
                                        )
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RivoAvatar(
                                    name = contactName ?: address,
                                    photoUri = resolvedPhotoUri,
                                    autoColorAvatars = autoColor,
                                    obeySolidIcons = false,
                                    size = 44.dp
                                )
                                Column(modifier = Modifier.padding(end = 12.dp)) {
                                    Text(
                                        text = contactName ?: address,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!contactName.isNullOrBlank()) {
                                        Text(
                                            text = address,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        FilledIconButton(
                            onClick = { navigator.navigateUp() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    actions = {
                        FilledIconButton(
                            onClick = {
                                makeCall(context, address, null)
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box {
                            FilledIconButton(
                                onClick = { showMenu = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            val tm = remember(context) { context.getSystemService(android.content.Context.TELECOM_SERVICE) as? TelecomManager }
                            val accounts = try { tm?.callCapablePhoneAccounts } catch (_: Throwable) { null } ?: emptyList()
                            val hasTwoSims = accounts.size >= 2
                            val account1 = getPhoneAccountForSimSlot(context, accounts, 0)
                            val account2 = getPhoneAccountForSimSlot(context, accounts, 1)

                            RivoDropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                RivoDropdownMenuItem(
                                    text = "Call",
                                    icon = Icons.Default.Call,
                                    iconTint = Color(0xFF4CAF50),
                                    onClick = {
                                        showMenu = false
                                        makeCall(context, address, null)
                                    },
                                    trailingContent = if (hasTwoSims && address.isNotBlank()) {
                                        {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    onClick = {
                                                        showMenu = false
                                                        makeCall(context, address, account1)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        SimSlotBadge(slot = 0)
                                                    }
                                                }
                                                Surface(
                                                    onClick = {
                                                        showMenu = false
                                                        makeCall(context, address, account2)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.size(width = 36.dp, height = 32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        SimSlotBadge(slot = 1)
                                                    }
                                                }
                                            }
                                        }
                                    } else null
                                )
                                RivoDropdownMenuItem(
                                    text = "Float",
                                    icon = Icons.Outlined.PictureInPicture,
                                    iconTint = Color(0xFF00ACC1),
                                    onClick = {
                                        showMenu = false
                                        if (!android.provider.Settings.canDrawOverlays(context)) {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        } else {
                                            FloatingSmsService.start(
                                                context = context,
                                                threadId = effectiveThreadId,
                                                address = address,
                                                contactName = contactName ?: matchedContact?.name,
                                                photoUri = resolvedPhotoUri
                                            )
                                        }
                                    }
                                )
                                RivoDropdownMenuItem(
                                    text = if (matchedContact != null) "Contact details" else "Add to contacts",
                                    icon = Icons.Outlined.Person,
                                    iconTint = Color(0xFF2196F3),
                                    onClick = {
                                        showMenu = false
                                        navigator.navigate(
                                            ContactDetailsScreenDestination(
                                                phoneNumber = address
                                            )
                                        )
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoDropdownMenuItem(
                                    text = if (isPinned) "Unpin chat" else "Pin chat",
                                    icon = Icons.Default.PushPin,
                                    iconTint = Color(0xFFFF9800),
                                    onClick = {
                                        showMenu = false
                                        val next = !isPinned
                                        isPinned = next
                                        prefs.setSmsThreadPinned(effectiveThreadId, next)
                                        Toast.makeText(context, if (next) "Chat pinned" else "Chat unpinned", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                RivoDropdownMenuItem(
                                    text = if (isMuted) "Unmute notifications" else "Mute notifications",
                                    icon = if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    iconTint = Color(0xFF9C27B0),
                                    onClick = {
                                        showMenu = false
                                        val next = !isMuted
                                        isMuted = next
                                        prefs.setSmsMuted(address, next)
                                        Toast.makeText(context, if (next) "Muted notifications from ${contactName ?: address}" else "Unmuted ${contactName ?: address}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                RivoDropdownMenuItem(
                                    text = "Mark as unread",
                                    icon = Icons.Outlined.MarkChatUnread,
                                    iconTint = Color(0xFF3F51B5),
                                    onClick = {
                                        showMenu = false
                                        smsVM.markThreadAsUnread(effectiveThreadId)
                                        Toast.makeText(context, "Marked as unread", Toast.LENGTH_SHORT).show()
                                        navigator.navigateUp()
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoDropdownMenuItem(
                                    text = if (isBlocked) "Unblock number" else "Block number",
                                    icon = if (isBlocked) Icons.Default.RemoveCircleOutline else Icons.Outlined.Block,
                                    iconTint = if (isBlocked) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                    onClick = {
                                        showMenu = false
                                        val next = !isBlocked
                                        isBlocked = next
                                        BlockedNumbersManager.toggle(context, prefs, address)
                                        Toast.makeText(context, if (next) "Blocked ${contactName ?: address}" else "Unblocked ${contactName ?: address}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                RivoDropdownMenuItem(
                                    text = "Scheduled messages",
                                    icon = Icons.Default.CalendarMonth,
                                    iconTint = Color(0xFF009688),
                                    onClick = {
                                        showMenu = false
                                        navigator.navigate(
                                            ScheduledMessagesScreenDestination(
                                                threadId = effectiveThreadId,
                                                address = address,
                                                contactName = contactName ?: matchedContact?.name
                                            )
                                        )
                                    }
                                )
                                RivoDropdownMenuItem(
                                    text = "Settings",
                                    icon = Icons.Outlined.Settings,
                                    iconTint = Color(0xFF607D8B),
                                    onClick = {
                                        showMenu = false
                                        navigator.navigate(SmsSettingsScreenDestination())
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoDropdownMenuItem(
                                    text = "Delete conversation",
                                    icon = Icons.Outlined.Delete,
                                    iconTint = MaterialTheme.colorScheme.error,
                                    isDestructive = true,
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Pinned message banner on top above the chat
            val pinnedMessages = remember(messages, settingsVer) {
                messages.filter { prefs.isSmsMessagePinned(it.id) }
            }
            AnimatedVisibility(
                visible = pinnedMessages.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val pinnedMsg = pinnedMessages.lastOrNull()
                if (pinnedMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                val reversedItems = chatListItems.asReversed()
                                val targetIndex = reversedItems.indexOfFirst {
                                    it is ChatListItem.Message && it.message.id == pinnedMsg.id
                                }
                                if (targetIndex >= 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (pinnedMessages.size > 1) "Pinned message (${pinnedMessages.size})" else "Pinned message",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = pinnedMsg.body.ifBlank { "MMS Message" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    prefs.setSmsMessagePinned(pinnedMsg.id, false)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Unpin message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Message bubbles list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hasCompletedInitialFetch || isThreadLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "No messages in this conversation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp + navBarBottom)
                    ) {
                        items(
                            items = chatListItems.asReversed(),
                            key = { item ->
                                when (item) {
                                    is ChatListItem.DateHeader -> "date_${item.dayKey}"
                                    is ChatListItem.Message -> if (item.message.isMms) "mms_${item.message.id}" else "sms_${item.message.id}"
                                }
                            }
                        ) { item ->
                            val itemAnimModifier = Modifier.animateItem(
                                fadeInSpec = tween(200, easing = FastOutSlowInEasing),
                                placementSpec = null,
                                fadeOutSpec = tween(200, easing = FastOutLinearInEasing)
                            )
                            when (item) {
                                is ChatListItem.DateHeader -> {
                                    ChatDateHeader(
                                        text = item.title,
                                        modifier = itemAnimModifier
                                    )
                                }
                                is ChatListItem.Message -> {
                                    val message = item.message
                                    ChatBubble(
                                        modifier = itemAnimModifier,
                                        message = message,
                                        timeStr = timeFormat.format(Date(message.date)),
                                        fontSize = chatFontSize.sp,
                                        isSelected = selectedMessageIds.contains(message.id),
                                        isSelectionMode = isSelectionMode,
                                        hasTwoSims = sortedSims.size == 2,
                                        sortedSims = sortedSims,
                                        onBubbleClick = {
                                            if (isSelectionMode) {
                                                if (selectedMessageIds.contains(message.id)) {
                                                    selectedMessageIds.remove(message.id)
                                                } else {
                                                    selectedMessageIds.add(message.id)
                                                }
                                            }
                                        },
                                        onSelectClick = {
                                            if (!selectedMessageIds.contains(message.id)) {
                                                selectedMessageIds.add(message.id)
                                            }
                                        },
                                        onCopyClick = {
                                            messageToSelect = message
                                        },
                                        onDeleteClick = {
                                            messageToDelete = message
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating bottom container (delayed sending countdown + floating input bar on transparent background)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Quik Delayed Sending Countdown Banner
                    AnimatedVisibility(
                        visible = pendingCountdown > 0,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(
                                        progress = { (pendingCountdown.toFloat() / sendDelaySeconds.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        "Sending in ${pendingCountdown}s...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                TextButton(onClick = { cancelPendingSend() }) {
                                    Text("UNDO", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Floating Input Bar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Separate Floating Voice Button with circular primary color background
                        if (showStt) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp),
                                tonalElevation = 2.dp,
                                shadowElevation = 4.dp
                            ) {
                                IconButton(
                                    onClick = {
                                        VoiceSearchHelper.launchVoiceSearch(context, sttLauncher)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Speech to Text",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Floating Text Box Pill with perfect circular sides & increased height
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 64.dp)
                                .heightIn(max = 140.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BasicTextField(
                                value = messageText,
                                onValueChange = {
                                    messageText = it
                                    prefs.saveSmsDraft(effectiveThreadId, address, it)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    lineHeight = 23.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                maxLines = 5,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (messageText.isEmpty()) {
                                            Text(
                                                text = "Text message",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = 17.sp,
                                                    lineHeight = 23.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            // Dual-SIM Picker (left side of send button, only shown when device has exactly 2 SIMs)
                            if (sortedSims.size == 2) {
                                Row(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    sortedSims.forEach { sim ->
                                        val isSelected = selectedSubId == sim.subscriptionId
                                        val slotNumber = (sim.simSlotIndex + 1).toString()
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    userSelectedSimManually = true
                                                    selectedSubId = sim.subscriptionId
                                                    prefs.setInt("last_used_sms_sub_id", sim.subscriptionId)
                                                    if (address.isNotBlank()) {
                                                        prefs.setInt("last_used_sms_sub_id_$address", sim.subscriptionId)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SimCardIconWithNumber(
                                                simSlotNumber = slotNumber,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                isLarge = false
                                            )
                                        }
                                    }
                                }
                            }

                            // Calendar icon with badge if scheduled messages exist for this chat
                            if (scheduledMessages.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        navigator.navigate(
                                            ScheduledMessagesScreenDestination(
                                                threadId = effectiveThreadId,
                                                address = address,
                                                contactName = contactName ?: matchedContact?.name
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .size(46.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Text(
                                                    text = scheduledMessages.size.toString(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = "Scheduled messages (${scheduledMessages.size})",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Send Button: circular curve matching the pill's circular side with uniform space
                            val canSend = messageText.isNotBlank()
                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .graphicsLayer {
                                            scaleX = sendButtonScale.value
                                            scaleY = sendButtonScale.value
                                        }
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            enabled = canSend,
                                            onClick = { initiateSend() },
                                            onLongClick = { showScheduleMenu = true }
                                        )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(26.dp)
                                                .graphicsLayer {
                                                    translationX = sendIconOffset.value * 0.7f
                                                    translationY = sendIconOffset.value * 0.7f
                                                    rotationZ = sendIconRotation.value
                                                }
                                        )
                                    }
                                }

                                RivoDropdownMenu(
                                    expanded = showScheduleMenu,
                                    onDismissRequest = { showScheduleMenu = false }
                                ) {
                                    RivoDropdownMenuItem(
                                        text = "Schedule with Time",
                                        icon = Icons.Default.AccessTime,
                                        iconTint = Color(0xFF00ACC1),
                                        onClick = {
                                            showScheduleMenu = false
                                            openClockPicker()
                                        }
                                    )
                                    RivoDropdownMenuItem(
                                        text = "Schedule with Date",
                                        icon = Icons.Default.CalendarToday,
                                        iconTint = Color(0xFF4CAF50),
                                        onClick = {
                                            showScheduleMenu = false
                                            openDatePicker()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Delete conversation confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete conversation?") },
            text = { Text("All messages in this conversation will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (!DefaultSmsManager.isDefaultSms(context)) {
                            showDefaultSmsRequiredDialog = true
                        } else {
                            smsVM.deleteThread(effectiveThreadId) {
                                navigator.navigateUp()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Select message text dialog
    messageToSelect?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToSelect = null },
            title = { Text("Select text") },
            text = {
                SelectionContainer {
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = chatFontSize.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SMS Message", msg.body))
                        Toast.makeText(context, "Copied all", Toast.LENGTH_SHORT).show()
                        messageToSelect = null
                    }
                ) {
                    Text("Copy all")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToSelect = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Delete multiple messages confirmation dialog
    if (showDeleteMultipleMessagesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMultipleMessagesDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedMessageIds.size} messages?") },
            text = { Text("The selected messages will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = selectedMessageIds.map { msgId ->
                            val msg = messages.firstOrNull { it.id == msgId }
                            msgId to (msg?.isMms ?: false)
                        }
                        selectedMessageIds.clear()
                        showDeleteMultipleMessagesDialog = false
                        if (!DefaultSmsManager.isDefaultSms(context)) {
                            showDefaultSmsRequiredDialog = true
                        } else {
                            smsVM.deleteMultipleMessages(toDelete, effectiveThreadId)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMultipleMessagesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete single message confirmation dialog
    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete message?") },
            text = { Text("This message will be deleted permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mId = msg.id
                        val isMms = msg.isMms
                        messageToDelete = null
                        if (!DefaultSmsManager.isDefaultSms(context)) {
                            showDefaultSmsRequiredDialog = true
                        } else {
                            smsVM.deleteMessage(mId, effectiveThreadId, isMms)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Default SMS App Required Dialog
    if (showDefaultSmsRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultSmsRequiredDialog = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Default SMS app required") },
            text = { Text("Android requires Ever Dialer to be set as the default SMS app to delete messages or conversations from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDefaultSmsRequiredDialog = false
                        DefaultSmsManager.requestDefaultSms(defaultSmsLauncher, context)
                    }
                ) {
                    Text("Set as default")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDefaultSmsRequiredDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modern Material You DatePicker Dialog
    if (showModernDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showModernDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMillis = datePickerState.selectedDateMillis
                        if (selectedUtcMillis != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            scheduledDateMillis = localCal.timeInMillis
                            showModernDatePicker = false
                            showModernTimePicker = true
                        } else {
                            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Next", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showModernDatePicker = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    // Modern Material You TimePicker Dialog
    if (showModernTimePicker) {
        AlertDialog(
            onDismissRequest = {
                showModernTimePicker = false
                scheduledDateMillis = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetCal = Calendar.getInstance().apply {
                            if (scheduledDateMillis != null) {
                                timeInMillis = scheduledDateMillis!!
                            }
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (scheduledDateMillis == null && targetCal.timeInMillis <= System.currentTimeMillis()) {
                            targetCal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        if (targetCal.timeInMillis > System.currentTimeMillis()) {
                            scheduleMessageAt(targetCal.timeInMillis)
                            showModernTimePicker = false
                            scheduledDateMillis = null
                        } else {
                            Toast.makeText(context, "Cannot schedule in the past", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Schedule", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showModernTimePicker = false
                        scheduledDateMillis = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    text = if (scheduledDateMillis != null) "Select Time" else "Schedule Message",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors()
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

private sealed interface ChatListItem {
    data class DateHeader(val dayKey: Long, val title: String) : ChatListItem
    data class Message(val message: SmsMessage) : ChatListItem
}

private fun getDayKey(timestamp: Long): Long {
    var time = timestamp
    if (time in 1..999999999999L) {
        time *= 1000L
    }
    val cal = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun formatChatDateHeader(timestamp: Long): String {
    var time = timestamp
    if (time in 1..999999999999L) {
        time *= 1000L
    }
    val cal = Calendar.getInstance()
    val todayYear = cal.get(Calendar.YEAR)
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)

    cal.timeInMillis = time
    val msgYear = cal.get(Calendar.YEAR)
    val msgDay = cal.get(Calendar.DAY_OF_YEAR)

    return when {
        todayYear == msgYear && todayDay == msgDay -> "Today"
        todayYear == msgYear && todayDay - msgDay == 1 -> "Yesterday"
        todayYear - msgYear == 1 && todayDay == 1 && msgDay >= 365 -> "Yesterday"
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(time))
    }
}

@Composable
private fun ChatDateHeader(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private fun buildClickableMessageText(text: String, linkColor: Color): AnnotatedString {
    val urlPattern = android.util.Patterns.WEB_URL
    return buildAnnotatedString {
        var lastIdx = 0
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            append(text.substring(lastIdx, start))
            val rawUrl = matcher.group() ?: ""
            val fullUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
            val link = LinkAnnotation.Url(
                url = fullUrl,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
            )
            withLink(link) {
                append(rawUrl)
            }
            lastIdx = end
        }
        append(text.substring(lastIdx))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    modifier: Modifier = Modifier,
    message: SmsMessage,
    timeStr: String,
    fontSize: TextUnit,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    hasTwoSims: Boolean = false,
    sortedSims: List<SubscriptionInfo> = emptyList(),
    onBubbleClick: () -> Unit,
    onSelectClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOut = message.isOutgoing
    val baseBubbleColor = if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val bubbleColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else baseBubbleColor,
        animationSpec = tween(250),
        label = "BubbleColor"
    )
    val rowBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(250),
        label = "RowBg"
    )
    val textColor = if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val timeColor = textColor.copy(alpha = 0.7f)

    var showMessageMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val isPinned = remember(settingsVer, message.id) { prefs.isSmsMessagePinned(message.id) }
    val isStarred = remember(settingsVer, message.id) { prefs.isSmsMessageStarred(message.id) }
    val showSimInChat = remember(settingsVer) { prefs.isShowSimInChat(context) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onBubbleClick()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        showMessageMenu = true
                    } else {
                        onBubbleClick()
                    }
                }
            ),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelectionMode && !isOut,
            enter = fadeIn(tween(250)) + expandHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                expandFrom = Alignment.End
            ) + scaleIn(initialScale = 0.6f, animationSpec = tween(250)),
            exit = fadeOut(tween(200)) + shrinkHorizontally(
                animationSpec = tween(200),
                shrinkTowards = Alignment.End
            ) + scaleOut(targetScale = 0.6f, animationSpec = tween(200))
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onBubbleClick() },
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOut) 16.dp else 4.dp,
                bottomEnd = if (isOut) 4.dp else 16.dp
            ),
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onBubbleClick()
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            showMessageMenu = true
                        } else {
                            onBubbleClick()
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // MMS Image attachments
                if (message.isMms && message.parts.isNotEmpty()) {
                    message.parts.filter { it.isImage && !it.uri.isNullOrBlank() }.forEach { part ->
                        AsyncImage(
                            model = Uri.parse(part.uri),
                            contentDescription = "MMS Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 6.dp)
                        )
                    }
                }

                if (message.body.isNotBlank()) {
                    val linkColor = MaterialTheme.colorScheme.primary
                    val annotatedBody = remember(message.body, linkColor) {
                        buildClickableMessageText(message.body, linkColor)
                    }
                    Text(
                        text = annotatedBody,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
                        color = textColor
                    )
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = timeColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (hasTwoSims && showSimInChat) {
                        val simSlot = if (message.subId != null) {
                            sortedSims.firstOrNull { it.subscriptionId == message.subId }?.simSlotIndex
                                ?: if (message.subId == 1) 0 else if (message.subId == 2) 1 else 0
                        } else 0
                        SimSlotBadge(
                            slot = simSlot,
                            modifier = Modifier.size(width = 13.dp, height = 15.dp),
                            fontSize = 8.sp
                        )
                    }
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = timeColor
                    )
                    if (isOut) {
                        val iconVector = when (message.deliveryStatus) {
                            0 -> Icons.Default.DoneAll // Delivered
                            else -> Icons.Default.Check // Sent
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = "Status",
                            tint = timeColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            RivoDropdownMenu(
                expanded = showMessageMenu,
                onDismissRequest = { showMessageMenu = false }
            ) {
                RivoDropdownMenuItem(
                    text = if (isPinned) "Unpin message" else "Pin message",
                    icon = Icons.Default.PushPin,
                    iconTint = Color(0xFFFF9800),
                    onClick = {
                        showMessageMenu = false
                        prefs.setSmsMessagePinned(message.id, !isPinned)
                    }
                )
                RivoDropdownMenuItem(
                    text = if (isStarred) "Unstar message" else "Star message",
                    icon = if (isStarred) Icons.Default.StarOutline else Icons.Default.Star,
                    iconTint = Color(0xFFFFB300),
                    onClick = {
                        showMessageMenu = false
                        prefs.setSmsMessageStarred(message.id, !isStarred)
                        smsVM.refreshStarredMessages(prefs.getStarredSmsMessages())
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                RivoDropdownMenuItem(
                    text = "Select",
                    icon = Icons.Default.SelectAll,
                    iconTint = Color(0xFF2196F3),
                    onClick = {
                        showMessageMenu = false
                        onSelectClick()
                    }
                )
                RivoDropdownMenuItem(
                    text = "Copy text",
                    icon = Icons.Default.ContentCopy,
                    iconTint = Color(0xFF009688),
                    onClick = {
                        showMessageMenu = false
                        onCopyClick()
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
                        showMessageMenu = false
                        onDeleteClick()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode && isOut,
            enter = fadeIn(tween(250)) + expandHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                expandFrom = Alignment.Start
            ) + scaleIn(initialScale = 0.6f, animationSpec = tween(250)),
            exit = fadeOut(tween(200)) + shrinkHorizontally(
                animationSpec = tween(200),
                shrinkTowards = Alignment.Start
            ) + scaleOut(targetScale = 0.6f, animationSpec = tween(200))
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onBubbleClick() },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
