package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.controller.sms.SmsEventBus
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.screen.SimCardIconWithNumber
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FloatingBubbleData(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val photoUri: String?
)

class FloatingSmsService : Service() {

    private lateinit var wm: WindowManager
    private val activeBubbles = mutableListOf<BubbleHolder>()
    private var chatView: View? = null
    private var currentOpenData: FloatingBubbleData? = null
    private var chatOverlayBackHandler: (() -> Unit)? = null
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private class BubbleHolder(
        val data: FloatingBubbleData,
        val view: ComposeView,
        val params: WindowManager.LayoutParams,
        val setBubbleVisible: (Boolean) -> Unit
    )

    private val chatParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fitInsetsTypes = 0
            fitInsetsSides = 0
        }
    }

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PHOTO_URI = "photo_uri"

        const val EXTRA_THREAD_IDS = "thread_ids"
        const val EXTRA_ADDRESSES = "addresses"
        const val EXTRA_CONTACT_NAMES = "contact_names"
        const val EXTRA_PHOTO_URIS = "photo_uris"

        fun start(context: Context, items: List<FloatingBubbleData>) {
            if (items.isEmpty()) return
            val intent = Intent(context, FloatingSmsService::class.java).apply {
                val tids = items.map { it.threadId }.toLongArray()
                val addrs = ArrayList(items.map { it.address })
                val names = ArrayList(items.map { it.contactName ?: "" })
                val photos = ArrayList(items.map { it.photoUri ?: "" })
                putExtra(EXTRA_THREAD_IDS, tids)
                putStringArrayListExtra(EXTRA_ADDRESSES, addrs)
                putStringArrayListExtra(EXTRA_CONTACT_NAMES, names)
                putStringArrayListExtra(EXTRA_PHOTO_URIS, photos)
            }
            context.startService(intent)
        }

        fun start(context: Context, threadId: Long, address: String, contactName: String? = null, photoUri: String? = null) {
            start(context, listOf(FloatingBubbleData(threadId, address, contactName, photoUri)))
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

        val items = mutableListOf<FloatingBubbleData>()
        val multiThreadIds = intent?.getLongArrayExtra(EXTRA_THREAD_IDS)
        val multiAddresses = intent?.getStringArrayListExtra(EXTRA_ADDRESSES)
        val multiNames = intent?.getStringArrayListExtra(EXTRA_CONTACT_NAMES)
        val multiPhotos = intent?.getStringArrayListExtra(EXTRA_PHOTO_URIS)

        if (multiThreadIds != null && multiThreadIds.isNotEmpty()) {
            for (i in multiThreadIds.indices) {
                val tid = multiThreadIds[i]
                val addr = multiAddresses?.getOrNull(i).orEmpty()
                val name = multiNames?.getOrNull(i)?.takeIf { it.isNotBlank() }
                val photo = multiPhotos?.getOrNull(i)?.takeIf { it.isNotBlank() }
                items.add(FloatingBubbleData(tid, addr, name, photo))
            }
        } else {
            val tid = intent?.getLongExtra(EXTRA_THREAD_ID, -1L) ?: -1L
            val addr = intent?.getStringExtra(EXTRA_ADDRESS).orEmpty()
            val name = intent?.getStringExtra(EXTRA_CONTACT_NAME)
            val photo = intent?.getStringExtra(EXTRA_PHOTO_URI)
            if (tid > 0 || addr.isNotBlank()) {
                items.add(FloatingBubbleData(tid, addr, name, photo))
            }
        }

        for (item in items) {
            addBubble(item)
        }

        return START_NOT_STICKY
    }

    private fun removeBubble(holder: BubbleHolder) {
        try { wm.removeViewImmediate(holder.view) } catch (_: Exception) {}
        activeBubbles.remove(holder)
        if (activeBubbles.isEmpty()) {
            stopSelf()
        }
    }

    private fun addBubble(item: FloatingBubbleData) {
        val existing = activeBubbles.find { it.data.threadId == item.threadId || (item.address.isNotBlank() && it.data.address == item.address) }
        if (existing != null) {
            existing.view.visibility = View.VISIBLE
            existing.setBubbleVisible(true)
            return
        }

        val idx = activeBubbles.size
        val params = WindowManager.LayoutParams(
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
            y = 200 + (idx * 200) % 1100
        }

        val holderRef = arrayOfNulls<BubbleHolder>(1)
        val isBubbleVisibleState = mutableStateOf(true)

        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    val density = LocalDensity.current
                    val bubbleRadiusPx = with(density) { 36.dp.roundToPx() }
                    FloatingSmsBubbleUI(
                        name = item.contactName ?: item.address,
                        photoUri = item.photoUri,
                        params = params,
                        view = this,
                        isVisible = isBubbleVisibleState.value,
                        onTap = {
                            val centerX = params.x + bubbleRadiusPx
                            val centerY = params.y + bubbleRadiusPx
                            scope.launch {
                                activeBubbles.forEach { it.setBubbleVisible(false) }
                                delay(180)
                                showChatWindow(item, centerX, centerY)
                            }
                        },
                        onDismiss = {
                            holderRef[0]?.let { removeBubble(it) }
                        }
                    )
                }
            }
        }
        val holder = BubbleHolder(item, cv, params) { visible ->
            isBubbleVisibleState.value = visible
        }
        holderRef[0] = holder
        activeBubbles.add(holder)
        try {
            wm.addView(cv, params)
        } catch (_: Exception) {
            activeBubbles.remove(holder)
            if (activeBubbles.isEmpty()) stopSelf()
        }
    }

    private class FloatingChatContainerView(
        context: Context,
        private val onBackPress: () -> Unit
    ) : android.widget.FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) {
                    onBackPress()
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
                    ) {
                        onBackPress()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    @Composable
    private fun DismissPill(visible: Boolean, onDismiss: () -> Unit) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(120)) + scaleOut(tween(120))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable { onDismiss() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    @Composable
    private fun FloatingSmsBubbleUI(
        name: String,
        photoUri: String?,
        params: WindowManager.LayoutParams,
        view: View,
        isVisible: Boolean,
        onTap: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        val screenWidthPx = with(density) { config.screenWidthDp.dp.roundToPx() }
        val screenHeightPx = with(density) { config.screenHeightDp.dp.roundToPx() }
        val bubbleSizePx = with(density) { 72.dp.roundToPx() }
        val maxX = (screenWidthPx - bubbleSizePx).coerceAtLeast(0)
        val maxY = (screenHeightPx - bubbleSizePx).coerceAtLeast(0)

        var showDismissOption by remember { mutableStateOf(false) }

        val animScale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "smsBubbleScale"
        )
        val animAlpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "smsBubbleAlpha"
        )

        val pressSource = remember { MutableInteractionSource() }
        val isPressed by pressSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "smsBubblePress"
        )

        val showAbove = params.y >= with(density) { 80.dp.roundToPx() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .wrapContentSize()
                .graphicsLayer {
                    scaleX = animScale
                    scaleY = animScale
                    alpha = animAlpha
                }
        ) {
            if (showAbove) {
                DismissPill(
                    visible = showDismissOption,
                    onDismiss = onDismiss
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var dragged = false
                            var isLongPress = false
                            val downPos = down.position

                            try {
                                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if ((change.position - downPos).getDistance() > viewConfiguration.touchSlop) {
                                            dragged = true
                                            break
                                        }
                                        if (!change.pressed) {
                                            break
                                        }
                                    } while (true)
                                }
                            } catch (_: PointerEventTimeoutCancellationException) {
                                isLongPress = true
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                showDismissOption = !showDismissOption
                            }

                            var change = down
                            while (change.pressed) {
                                val event = awaitPointerEvent()
                                val nextChange = event.changes.firstOrNull { it.id == down.id } ?: break
                                change = nextChange
                                val delta = nextChange.position - nextChange.previousPosition
                                if (!dragged && (nextChange.position - downPos).getDistance() > viewConfiguration.touchSlop) {
                                    dragged = true
                                    showDismissOption = false
                                }
                                if (dragged) {
                                    nextChange.consume()
                                    params.x = (params.x + delta.x.toInt()).coerceIn(0, maxX)
                                    params.y = (params.y + delta.y.toInt()).coerceIn(0, maxY)
                                    try {
                                        wm.updateViewLayout(view, params)
                                    } catch (_: Exception) {}
                                }
                            }

                            if (!isLongPress && !dragged) {
                                showDismissOption = false
                                onTap()
                            }
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

            if (!showAbove) {
                DismissPill(
                    visible = showDismissOption,
                    onDismiss = onDismiss
                )
            }
        }
    }

    private fun showChatWindow(item: FloatingBubbleData, bubbleCenterX: Int, bubbleCenterY: Int) {
        if (chatView != null) return
        currentOpenData = item
        activeBubbles.forEach { it.view.visibility = View.GONE }

        val container = FloatingChatContainerView(this) {
            chatOverlayBackHandler?.invoke()
        }.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    FloatingChatOverlay(
                        threadId = item.threadId,
                        address = item.address,
                        contactName = item.contactName,
                        photoUri = item.photoUri,
                        bubbleCenterX = bubbleCenterX,
                        bubbleCenterY = bubbleCenterY,
                        registerBackHandler = { handler ->
                            chatOverlayBackHandler = handler
                        },
                        onHide = {
                            chatOverlayBackHandler = null
                            dismissChatWindow(minimize = true)
                        },
                        onDismiss = {
                            chatOverlayBackHandler = null
                            dismissChatWindow(minimize = false)
                        },
                        onOpenFullApp = {
                            chatOverlayBackHandler = null
                            val intent = Intent(this@FloatingSmsService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("navigate_to_sms_thread_id", item.threadId)
                                putExtra("navigate_to_sms_address", item.address)
                            }
                            startActivity(intent)
                            dismissChatWindow(minimize = false)
                        }
                    )
                }
            }
        }
        container.addView(
            cv,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        chatView = container
        try {
            wm.addView(container, chatParams)
            container.requestFocus()
        } catch (_: Exception) {
            activeBubbles.forEach {
                it.view.visibility = View.VISIBLE
                it.setBubbleVisible(true)
            }
        }
    }

    private fun dismissChatWindow(minimize: Boolean) {
        try {
            chatView?.let { wm.removeViewImmediate(it) }
        } catch (_: Exception) {}
        chatView = null

        val item = currentOpenData
        currentOpenData = null

        if (minimize) {
            activeBubbles.forEach {
                it.view.visibility = View.VISIBLE
                it.setBubbleVisible(true)
            }
        } else {
            if (item != null) {
                val iter = activeBubbles.iterator()
                while (iter.hasNext()) {
                    val b = iter.next()
                    if (b.data.threadId == item.threadId || (item.address.isNotBlank() && b.data.address == item.address)) {
                        try { wm.removeViewImmediate(b.view) } catch (_: Exception) {}
                        iter.remove()
                    }
                }
            }
            if (activeBubbles.isEmpty()) {
                stopSelf()
            } else {
                activeBubbles.forEach {
                    it.view.visibility = View.VISIBLE
                    it.setBubbleVisible(true)
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        lifecycleOwner.onDestroy()
        try {
            chatView?.let { wm.removeViewImmediate(it) }
        } catch (_: Exception) {}
        chatView = null
        for (b in activeBubbles) {
            try { wm.removeViewImmediate(b.view) } catch (_: Exception) {}
        }
        activeBubbles.clear()
        super.onDestroy()
    }
}

private sealed interface FloatingChatListItem {
    data class DateHeader(val dayKey: Long, val title: String) : FloatingChatListItem
    data class Message(val message: SmsMessage) : FloatingChatListItem
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

private fun buildClickableMessageText(
    text: String,
    linkColor: Color,
    onLinkClick: ((String) -> Unit)? = null
): AnnotatedString {
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
                ),
                linkInteractionListener = onLinkClick?.let { callback ->
                    { annotation ->
                        val u = (annotation as? LinkAnnotation.Url)?.url ?: fullUrl
                        callback(u)
                    }
                }
            )
            withLink(link) {
                append(rawUrl)
            }
            lastIdx = end
        }
        append(text.substring(lastIdx))
    }
}

@Composable
private fun FloatingChatOverlay(
    threadId: Long,
    address: String,
    contactName: String?,
    photoUri: String?,
    bubbleCenterX: Int,
    bubbleCenterY: Int,
    registerBackHandler: (((() -> Unit) -> Unit))? = null,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
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
    val prefs = remember {
        try {
            GlobalContext.get().get<PreferenceManager>()
        } catch (_: Throwable) {
            null
        }
    }

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val pivotX = (bubbleCenterX / screenWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val pivotY = (bubbleCenterY / screenHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val transformOrigin = remember(pivotX, pivotY) { TransformOrigin(pivotX, pivotY) }

    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    fun animateAndClose(action: () -> Unit) {
        if (isClosing) return
        isClosing = true
        isVisible = false
        coroutineScope.launch {
            delay(400)
            action()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
        registerBackHandler?.invoke {
            animateAndClose(onHide)
        }
    }

    val animProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        ),
        label = "floatingChatZoom"
    )

    val cardScale = androidx.compose.ui.util.lerp(0.04f, 1f, animProgress)
    val cardAlpha = animProgress.coerceIn(0f, 1f)
    val backdropAlpha = (0.55f * animProgress).coerceIn(0f, 0.55f)

    var effectiveThreadId by remember { mutableLongStateOf(threadId) }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val signatureEnabled = remember {
        prefs?.getBoolean(PreferenceManager.KEY_SMS_SIGNATURE_ENABLED, false) ?: false
    }
    val signatureText = remember {
        prefs?.getString(PreferenceManager.KEY_SMS_SIGNATURE, "") ?: ""
    }
    val stripUnicode = remember {
        prefs?.getBoolean(PreferenceManager.KEY_SMS_STRIP_UNICODE, false) ?: false
    }

    val is24Hour = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val timeFormat = remember(is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
    }

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

    val initialSubId = remember(sortedSims) {
        if (sortedSims.isEmpty()) null
        else {
            val perAddressSubId = if (address.isNotBlank() && prefs != null) prefs.getInt("last_used_sms_sub_id_$address", -1) else -1
            if (perAddressSubId != -1 && sortedSims.any { it.subscriptionId == perAddressSubId }) {
                perAddressSubId
            } else {
                val globalSubId = prefs?.getInt("last_used_sms_sub_id", -1) ?: -1
                if (globalSubId != -1 && sortedSims.any { it.subscriptionId == globalSubId }) {
                    globalSubId
                } else {
                    sortedSims.firstOrNull()?.subscriptionId
                }
            }
        }
    }

    var selectedSubId by remember(sortedSims) { mutableStateOf(initialSubId) }

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
            delay(2000)
        }
    }

    LaunchedEffect(effectiveThreadId) {
        SmsEventBus.newSmsEvent.collect { eventThreadId ->
            if (effectiveThreadId > 0 && (eventThreadId == null || eventThreadId == effectiveThreadId)) {
                val msgs = withContext(Dispatchers.IO) {
                    smsRepo?.getMessagesForThread(effectiveThreadId) ?: emptyList()
                }
                messages = msgs
            }
        }
    }

    val chatListItems = remember(messages) {
        val items = mutableListOf<FloatingChatListItem>()
        var currentDayKey = -1L
        for (message in messages) {
            val dayKey = getDayKey(message.date)
            if (dayKey != currentDayKey) {
                currentDayKey = dayKey
                items.add(FloatingChatListItem.DateHeader(dayKey, formatChatDateHeader(message.date)))
            }
            items.add(FloatingChatListItem.Message(message))
        }
        items
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    fun sendMessage() {
        if (inputText.isBlank()) return
        var finalBody = inputText.trim()
        if (signatureEnabled && signatureText.isNotBlank()) {
            finalBody = "$finalBody\n$signatureText"
        }
        if (stripUnicode) {
            finalBody = java.text.Normalizer.normalize(finalBody, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        }
        selectedSubId?.let { subId ->
            prefs?.setInt("last_used_sms_sub_id", subId)
            if (address.isNotBlank()) {
                prefs?.setInt("last_used_sms_sub_id_$address", subId)
            }
        }
        val textToSend = finalBody
        inputText = ""
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                smsRepo?.sendSms(address, textToSend, selectedSubId)
                if (effectiveThreadId > 0) {
                    val msgs = smsRepo?.getMessagesForThread(effectiveThreadId) ?: emptyList()
                    withContext(Dispatchers.Main) {
                        messages = msgs
                    }
                }
            }
        }
    }

    val handleOpenLink: (String) -> Unit = remember(context) {
        { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val customUriHandler = remember(handleOpenLink) {
        object : UriHandler {
            override fun openUri(uri: String) {
                handleOpenLink(uri)
            }
        }
    }

    CompositionLocalProvider(LocalUriHandler provides customUriHandler) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { animateAndClose(onHide) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .imePadding()
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                    this.transformOrigin = transformOrigin
                }
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
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
                            size = 36.dp,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contactName ?: address,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!contactName.isNullOrBlank() && address.isNotBlank()) {
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Call Action
                        IconButton(
                            onClick = {
                                if (address.isNotBlank()) {
                                    makeCall(context, address)
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Open in Full App Action
                        IconButton(
                            onClick = onOpenFullApp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Open in app",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // "X" Action: hides chat screen and restores bubble
                        IconButton(
                            onClick = { animateAndClose(onHide) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hide",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Messages list
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)
                ) {
                    items(
                        items = chatListItems.asReversed(),
                        key = { item ->
                            when (item) {
                                is FloatingChatListItem.DateHeader -> "date_${item.dayKey}"
                                is FloatingChatListItem.Message -> if (item.message.isMms) "mms_${item.message.id}" else "sms_${item.message.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is FloatingChatListItem.DateHeader -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                        tonalElevation = 1.dp
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            is FloatingChatListItem.Message -> {
                                val msg = item.message
                                val isOut = msg.isOutgoing
                                val bubbleColor = if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                val textColor = if (isOut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                val linkColor = MaterialTheme.colorScheme.primary
                                var showMessageMenu by remember { mutableStateOf(false) }
                                val isPinned = remember(msg.id) { prefs?.isSmsMessagePinned(msg.id) ?: false }
                                val isStarred = remember(msg.id) { prefs?.isSmsMessageStarred(msg.id) ?: false }

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
                                            .widthIn(max = 280.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (msg.body.isNotBlank()) {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("SMS Message", msg.body))
                                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onLongClick = {
                                                    showMessageMenu = true
                                                }
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            if (msg.isMms && msg.parts.isNotEmpty()) {
                                                msg.parts.filter { it.isImage && !it.uri.isNullOrBlank() }.forEach { part ->
                                                    AsyncImage(
                                                        model = Uri.parse(part.uri),
                                                        contentDescription = "MMS Image",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 200.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .padding(bottom = 6.dp)
                                                    )
                                                }
                                            }

                                            if (msg.body.isNotBlank()) {
                                                val annotated = remember(msg.body, linkColor) {
                                                    buildClickableMessageText(msg.body, linkColor, handleOpenLink)
                                                }
                                                Text(
                                                    text = annotated,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = textColor
                                                )
                                            }

                                            Spacer(Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.align(Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                if (isPinned) {
                                                    Icon(
                                                        imageVector = Icons.Default.PushPin,
                                                        contentDescription = "Pinned",
                                                        tint = textColor.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                                if (isStarred) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Starred",
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                                Text(
                                                    text = timeFormat.format(Date(msg.date)),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = textColor.copy(alpha = 0.65f)
                                                )
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
                                                    prefs?.setSmsMessagePinned(msg.id, !isPinned)
                                                }
                                            )
                                            RivoDropdownMenuItem(
                                                text = if (isStarred) "Unstar message" else "Star message",
                                                icon = if (isStarred) Icons.Default.StarOutline else Icons.Default.Star,
                                                iconTint = Color(0xFFFFB300),
                                                onClick = {
                                                    showMessageMenu = false
                                                    prefs?.setSmsMessageStarred(msg.id, !isStarred)
                                                }
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                            RivoDropdownMenuItem(
                                                text = "Copy text",
                                                icon = Icons.Default.ContentCopy,
                                                iconTint = Color(0xFF009688),
                                                onClick = {
                                                    showMessageMenu = false
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("SMS Message", msg.body))
                                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            RivoDropdownMenuItem(
                                                text = "Share",
                                                icon = Icons.Default.Share,
                                                iconTint = Color(0xFF2196F3),
                                                onClick = {
                                                    showMessageMenu = false
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        putExtra(Intent.EXTRA_TEXT, msg.body)
                                                        type = "text/plain"
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share Message").apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    })
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
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        smsRepo?.deleteMessage(msg.id, msg.isMms)
                                                        if (effectiveThreadId > 0) {
                                                            val updated = smsRepo?.getMessagesForThread(effectiveThreadId) ?: emptyList()
                                                            withContext(Dispatchers.Main) {
                                                                messages = updated
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        // Dual-SIM Picker (shown when device has exactly 2 SIMs)
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
                                                selectedSubId = sim.subscriptionId
                                                prefs?.setInt("last_used_sms_sub_id", sim.subscriptionId)
                                                if (address.isNotBlank()) {
                                                    prefs?.setInt("last_used_sms_sub_id_$address", sim.subscriptionId)
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

                        val canSend = inputText.isNotBlank()
                        IconButton(
                            onClick = { sendMessage() },
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
}
