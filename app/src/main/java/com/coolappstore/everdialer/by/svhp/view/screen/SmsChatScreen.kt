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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.VoiceSearchHelper
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.rememberVoiceSearchLauncher
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
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

    var messageText by remember(initialText) { mutableStateOf(initialText ?: "") }
    var showMenu by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<SmsMessage?>(null) }
    var messageToSelect by remember { mutableStateOf<SmsMessage?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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

    var selectedSubId by remember(activeSims) {
        mutableStateOf(activeSims.firstOrNull()?.subscriptionId)
    }

    LaunchedEffect(effectiveThreadId, address) {
        if (effectiveThreadId <= 0 && address.isNotBlank()) {
            val resolved = smsVM.getOrCreateThreadId(address)
            if (resolved > 0) {
                effectiveThreadId = resolved
            }
        }
        if (effectiveThreadId > 0) {
            while (isActive) {
                smsVM.loadThreadMessages(effectiveThreadId)
                delay(1500)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(isImeVisible, imeBottom) {
        if (isImeVisible && messages.isNotEmpty()) {
            delay(60)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    fun doSendMessage(rawText: String) {
        var finalBody = rawText
        if (signatureEnabled && signatureText.isNotBlank()) {
            finalBody = "$finalBody\n$signatureText"
        }
        if (stripUnicode) {
            finalBody = Normalizer.normalize(finalBody, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        }

        smsVM.sendMessage(address, finalBody, selectedSubId) { success ->
            if (!success) {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun initiateSend() {
        if (messageText.isBlank()) return
        val textToProcess = messageText.trim()
        messageText = ""

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
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                navigator.navigate(
                                    ContactDetailsScreenDestination(
                                        contactId = matchedContact?.id,
                                        phoneNumber = address
                                    )
                                )
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RivoAvatar(
                            name = contactName ?: address,
                            photoUri = resolvedPhotoUri,
                            size = 38.dp
                        )
                        Column {
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
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        makeCall(context, address, null)
                    }) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages, key = { if (it.isMms) "mms_${it.id}" else "sms_${it.id}" }) { message ->
                            ChatBubble(
                                message = message,
                                timeStr = timeFormat.format(Date(message.date)),
                                fontSize = chatFontSize.sp,
                                onSelectClick = {
                                    messageToSelect = message
                                },
                                onCopyClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("SMS Message", message.body))
                                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteClick = {
                                    messageToDelete = message
                                }
                            )
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

            // SIM selector pill if multi-SIM
            if (activeSims.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeSims.forEach { sim ->
                        val isSelected = selectedSubId == sim.subscriptionId
                        val simName = sim.displayName?.toString()?.ifBlank { "SIM ${sim.simSlotIndex + 1}" } ?: "SIM ${sim.simSlotIndex + 1}"
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubId = sim.subscriptionId },
                            label = { Text(simName, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder = { Text("Text message") },
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    val canSend = messageText.isNotBlank()
                    IconButton(
                        onClick = { initiateSend() },
                        enabled = canSend,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: SmsMessage,
    timeStr: String,
    fontSize: TextUnit,
    onSelectClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOut = message.isOutgoing
    val bubbleColor = if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val timeColor = textColor.copy(alpha = 0.7f)

    var showMessageMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOut) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOut) 16.dp else 4.dp,
                bottomEnd = if (isOut) 4.dp else 16.dp
            ),
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        showMessageMenu = true
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
                    Text(
                        text = message.body,
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
    }
}
