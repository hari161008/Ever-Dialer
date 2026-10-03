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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.coolappstore.everdialer.by.svhp.controller.util.VoiceSearchHelper
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.rememberVoiceSearchLauncher
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.coolappstore.everdialer.by.svhp.controller.util.ScheduledSmsManager
import com.coolappstore.everdialer.by.svhp.modal.data.ScheduledSmsEntry
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
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

    DisposableEffect(Unit) {
        onDispose {
            smsVM.clearActiveThread()
        }
    }

    var effectiveThreadId by remember(threadId) { mutableStateOf(threadId) }
    val messages by smsVM.currentThreadMessages.collectAsState()
    val listState = rememberLazyListState()

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
    var isInitialLoad by remember { mutableStateOf(true) }
    var previousLatestMessageId by remember { mutableStateOf<Long?>(null) }
    var userJustSentMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        isInitialLoad = false
    }

    LaunchedEffect(latestMessageId, chatListItems.size) {
        if (latestMessageId != null) {
            if (isInitialLoad && previousLatestMessageId == null) {
                previousLatestMessageId = latestMessageId
                listState.scrollToItem(0)
            } else if (latestMessageId != previousLatestMessageId) {
                val wasUserSend = userJustSentMessage
                previousLatestMessageId = latestMessageId
                userJustSentMessage = false
                if (wasUserSend || listState.firstVisibleItemIndex <= 3) {
                    delay(60)
                    listState.animateScrollToItem(0)
                }
            }
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

    var messageText by remember(initialText) { mutableStateOf(initialText ?: "") }
    var showMenu by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<SmsMessage?>(null) }
    var messageToSelect by remember { mutableStateOf<SmsMessage?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteMultipleMessagesDialog by remember { mutableStateOf(false) }

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
        messageText = if (messageText.isBlank()) spokenText else "$messageText $spokenText"
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
        if (!userSelectedSimManually && messages.isNotEmpty() && sortedSims.size == 2) {
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
                smsVM.loadThreadMessages(effectiveThreadId)
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
                    smsVM.loadThreadMessages(resolved)
                }
            } else if (effectiveThreadId > 0) {
                if (eventThreadId == null || eventThreadId == effectiveThreadId) {
                    smsVM.loadThreadMessages(effectiveThreadId)
                }
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Scheduled SMS state
    var showScheduleMenu by remember { mutableStateOf(false) }
    var scheduledVersion by remember { mutableIntStateOf(0) }
    val scheduledMessages = remember(scheduledVersion, effectiveThreadId, address) {
        ScheduledSmsManager.getEntriesForThreadOrAddress(prefs, effectiveThreadId, address)
    }

    fun scheduleMessageAt(timestamp: Long) {
        if (messageText.isBlank()) return
        val textToSchedule = messageText.trim()
        messageText = ""
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

    fun openClockPicker() {
        val c = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                scheduleMessageAt(targetCal.timeInMillis)
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    fun openDatePicker() {
        val c = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val targetCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (targetCal.timeInMillis > System.currentTimeMillis()) {
                            scheduleMessageAt(targetCal.timeInMillis)
                        } else {
                            Toast.makeText(context, "Cannot schedule in the past", Toast.LENGTH_SHORT).show()
                        }
                    },
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    false
                )
                timePicker.show()
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
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
                                .padding(start = 8.dp)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    navigator.navigate(
                                        ContactDetailsScreenDestination(
                                            contactId = matchedContact?.id,
                                            phoneNumber = address
                                        )
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RivoAvatar(
                                    name = contactName ?: address,
                                    photoUri = resolvedPhotoUri,
                                    autoColorAvatars = autoColor,
                                    obeySolidIcons = false,
                                    size = 36.dp
                                )
                                Column(modifier = Modifier.padding(end = 12.dp)) {
                                    Text(
                                        text = contactName ?: address,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!contactName.isNullOrBlank()) {
                                        Text(
                                            text = address,
                                            style = MaterialTheme.typography.bodySmall,
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
                                .padding(start = 8.dp)
                                .size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call"
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
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            RivoDropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                RivoDropdownMenuItem(
                                    text = "Settings",
                                    icon = Icons.Outlined.Settings,
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
                        Text(
                            text = "No messages in this conversation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                        contentPadding = PaddingValues(vertical = 12.dp)
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
                                fadeInSpec = tween(350, easing = FastOutSlowInEasing),
                                placementSpec = tween(500, easing = FastOutSlowInEasing),
                                fadeOutSpec = tween(500, easing = FastOutLinearInEasing)
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
            }

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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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



            // Scheduled SMS Banner
            if (scheduledMessages.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Scheduled Messages (${scheduledMessages.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        scheduledMessages.forEach { entry ->
                            val schedDateStr = remember(entry.scheduledTime) {
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(java.util.Date(entry.scheduledTime))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = schedDateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            ScheduledSmsManager.removeEntry(context, prefs, entry.id)
                                            doSendMessage(entry.body)
                                            scheduledVersion++
                                        }
                                    ) {
                                        Text("Send now", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(
                                        onClick = {
                                            ScheduledSmsManager.removeEntry(context, prefs, entry.id)
                                            scheduledVersion++
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancel scheduled",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // STT Mic Button
                        if (showStt) {
                            IconButton(
                                onClick = {
                                    VoiceSearchHelper.launchVoiceSearch(context, sttLauncher)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Speech to Text",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Text message") },
                            maxLines = 5,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        // Dual-SIM Picker (left side of send button, only shown when device has exactly 2 SIMs)
                        if (sortedSims.size == 2) {
                            Row(
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

                        val canSend = messageText.isNotBlank()
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(42.dp)
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
                                            .size(20.dp)
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
                                    text = "Schedule with Clock",
                                    icon = Icons.Default.AccessTime,
                                    onClick = {
                                        showScheduleMenu = false
                                        openClockPicker()
                                    }
                                )
                                RivoDropdownMenuItem(
                                    text = "Schedule with Date",
                                    icon = Icons.Default.CalendarToday,
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
                        smsVM.deleteThread(effectiveThreadId) {
                            navigator.navigateUp()
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
                        showDeleteMultipleMessagesDialog = false
                        selectedMessageIds.forEach { msgId ->
                            val msg = messages.firstOrNull { it.id == msgId }
                            smsVM.deleteMessage(msgId, effectiveThreadId, msg?.isMms ?: false)
                        }
                        selectedMessageIds.clear()
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
                        smsVM.deleteMessage(msg.id, effectiveThreadId, msg.isMms)
                        messageToDelete = null
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
                    text = "Select",
                    icon = Icons.Default.SelectAll,
                    onClick = {
                        showMessageMenu = false
                        onSelectClick()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                RivoDropdownMenuItem(
                    text = "Copy text",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showMessageMenu = false
                        onCopyClick()
                    }
                )
                RivoDropdownMenuItem(
                    text = "Delete",
                    icon = Icons.Outlined.Delete,
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
