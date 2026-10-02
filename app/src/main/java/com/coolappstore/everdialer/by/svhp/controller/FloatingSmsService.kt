package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Locale

class FloatingSmsService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: ComposeView? = null
    private var chatView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val threadIdState = mutableLongStateOf(-1L)
    private val addressState = mutableStateOf("")
    private val contactNameState = mutableStateOf<String?>(null)
    private val photoUriState = mutableStateOf<String?>(null)

    private val bubbleParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 320
    }

    private val chatParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PHOTO_URI = "photo_uri"

        fun start(context: Context, threadId: Long, address: String, contactName: String? = null, photoUri: String? = null) {
            context.startService(Intent(context, FloatingSmsService::class.java).apply {
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_PHOTO_URI, photoUri)
            })
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingSmsService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        threadIdState.longValue = intent?.getLongExtra(EXTRA_THREAD_ID, -1L) ?: -1L
        addressState.value = intent?.getStringExtra(EXTRA_ADDRESS) ?: ""
        contactNameState.value = intent?.getStringExtra(EXTRA_CONTACT_NAME)
        photoUriState.value = intent?.getStringExtra(EXTRA_PHOTO_URI)

        if (bubbleView == null && chatView == null) {
            createBubble()
        }
        return START_NOT_STICKY
    }

    private fun createBubble() {
        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    FloatingSmsBubbleUI(
                        name = contactNameState.value ?: addressState.value,
                        photoUri = photoUriState.value,
                        onTap = {
                            showChatWindow()
                        }
                    )
                }
            }
        }
        bubbleView = cv
        try {
            wm.addView(cv, bubbleParams)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    @Composable
    private fun FloatingSmsBubbleUI(name: String, photoUri: String?, onTap: () -> Unit) {
        val pressSource = remember { MutableInteractionSource() }
        val isPressed by pressSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "smsBubblePress"
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragged = false
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val delta = change.position - change.previousPosition
                            if (!dragged && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                dragged = true
                            }
                            if (dragged) {
                                change.consume()
                                bubbleParams.x = (bubbleParams.x + delta.x.toInt()).coerceAtLeast(0)
                                bubbleParams.y = (bubbleParams.y + delta.y.toInt()).coerceAtLeast(0)
                                try {
                                    wm.updateViewLayout(bubbleView, bubbleParams)
                                } catch (_: Exception) {}
                            }
                            if (!change.pressed) {
                                if (!dragged) {
                                    onTap()
                                }
                                break
                            }
                        } while (true)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                modifier = Modifier.size(56.dp)
            ) {
                if (!photoUri.isNullOrEmpty()) {
                    RivoAvatar(
                        name = name,
                        photoUri = photoUri,
                        obeySolidIcons = false,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    private fun showChatWindow() {
        if (chatView != null) return
        bubbleView?.visibility = View.GONE

        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    FloatingChatOverlay(
                        threadId = threadIdState.longValue,
                        address = addressState.value,
                        contactName = contactNameState.value,
                        photoUri = photoUriState.value,
                        onMinimize = {
                            dismissChatWindow(minimize = true)
                        },
                        onClose = {
                            dismissChatWindow(minimize = false)
                            stopSelf()
                        },
                        onOpenFullApp = {
                            val intent = Intent(this@FloatingSmsService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("navigate_to_sms_thread_id", threadIdState.longValue)
                                putExtra("navigate_to_sms_address", addressState.value)
                            }
                            startActivity(intent)
                            dismissChatWindow(minimize = false)
                            stopSelf()
                        }
                    )
                }
            }
        }
        chatView = cv
        try {
            wm.addView(cv, chatParams)
        } catch (_: Exception) {
            bubbleView?.visibility = View.VISIBLE
        }
    }

    private fun dismissChatWindow(minimize: Boolean) {
        try {
            chatView?.let { wm.removeViewImmediate(it) }
        } catch (_: Exception) {}
        chatView = null

        if (minimize) {
            bubbleView?.visibility = View.VISIBLE
        } else {
            removeBubble()
        }
    }

    private fun removeBubble() {
        try {
            bubbleView?.let { wm.removeViewImmediate(it) }
        } catch (_: Exception) {}
        bubbleView = null
    }

    override fun onDestroy() {
        scope.cancel()
        lifecycleOwner.onDestroy()
        dismissChatWindow(minimize = false)
        super.onDestroy()
    }
}

@Composable
private fun FloatingChatOverlay(
    threadId: Long,
    address: String,
    contactName: String?,
    photoUri: String?,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onOpenFullApp: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val smsRepo = remember {
        try {
            GlobalContext.get().get<ISmsRepository>()
        } catch (_: Throwable) {
            null
        }
    }

    var effectiveThreadId by remember { mutableLongStateOf(threadId) }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(effectiveThreadId, address) {
        if (effectiveThreadId <= 0 && address.isNotBlank()) {
            val resolved = withContext(Dispatchers.IO) {
                smsRepo?.getOrCreateThreadId(address) ?: -1L
            }
            if (resolved > 0) {
                effectiveThreadId = resolved
            }
        }
        while (isActive) {
            if (effectiveThreadId > 0) {
                val msgs = withContext(Dispatchers.IO) {
                    smsRepo?.getMessagesForThread(effectiveThreadId) ?: emptyList()
                }
                messages = msgs
            }
            delay(1500)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .imePadding()
            .clickable(onClick = onMinimize),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RivoAvatar(
                            name = contactName ?: address,
                            photoUri = photoUri,
                            obeySolidIcons = false,
                            size = 38.dp,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contactName ?: address,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                        IconButton(onClick = onOpenFullApp, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open in app", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onMinimize, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Minimize", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isOut = msg.isOutgoing
                        val bubbleColor = if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        val textColor = if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

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
                                modifier = Modifier.widthIn(max = 260.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = timeFormat.format(java.util.Date(msg.date)),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = textColor.copy(alpha = 0.65f),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Input bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Text message", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        val canSend = inputText.isNotBlank()
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    val textToSend = inputText.trim()
                                    inputText = ""
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            smsRepo?.sendSms(address, textToSend, null)
                                            if (effectiveThreadId > 0) {
                                                messages = smsRepo?.getMessagesForThread(effectiveThreadId) ?: emptyList()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
