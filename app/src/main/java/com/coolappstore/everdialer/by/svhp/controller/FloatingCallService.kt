package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.telecom.CallAudioState
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.coolappstore.everdialer.by.svhp.view.screen.CallActivity
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.*

class FloatingCallService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: ComposeView? = null
    private var menuView:   ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val scope          = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val contactNameState = mutableStateOf("?")
    private val phoneNumberState  = mutableStateOf("")
    private val photoUriState     = mutableStateOf<String?>(null)
    private val menuVisibleState  = mutableStateOf(false)

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_CONFIGURATION_CHANGED) return
            scope.launch {
                delay(80) // let window system settle after rotation
                val display = wm.defaultDisplay
                @Suppress("DEPRECATION")
                val screenW = display.width
                @Suppress("DEPRECATION")
                val screenH = display.height
                bubbleParams.x = bubbleParams.x.coerceIn(0, (screenW - 200).coerceAtLeast(0))
                bubbleParams.y = bubbleParams.y.coerceIn(0, (screenH - 200).coerceAtLeast(0))
                try { bubbleView?.let { wm.updateViewLayout(it, bubbleParams) } } catch (_: Exception) {}
            }
        }
    }

    // Bubble: small, draggable, non-focusable, extends to screen edges
    private val bubbleParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 320 }

    // Menu: MATCH_PARENT so the Surface background fills all the way behind nav bar
    private val menuParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    companion object {
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_PHOTO_URI    = "photo_uri"

        fun start(context: Context, name: String, number: String, photoUri: String? = null) {
            context.startService(Intent(context, FloatingCallService::class.java).apply {
                putExtra(EXTRA_CONTACT_NAME, name); putExtra(EXTRA_PHONE_NUMBER, number)
                putExtra(EXTRA_PHOTO_URI, photoUri)
            })
        }
        fun stop(context: Context) =
            context.stopService(Intent(context, FloatingCallService::class.java))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
        registerReceiver(configReceiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        contactNameState.value = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "?"
        phoneNumberState.value  = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        photoUriState.value     = intent?.getStringExtra(EXTRA_PHOTO_URI)
        if (bubbleView == null) { createBubble(); observeAll() }
        return START_STICKY
    }

    // ── Bubble ────────────────────────────────────────────────────────────────

    private fun createBubble() {
        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { Rivo4Theme { BubbleUI(contactNameState.value, photoUriState.value) { if (menuView == null) showMenu() else dismissMenu() } } }
        }
        bubbleView = cv
        try { wm.addView(cv, bubbleParams) } catch (_: Exception) { stopSelf() }
    }

    @Composable
    private fun BubbleUI(name: String, photoUri: String?, onTap: () -> Unit) {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(50); entered = true }

        val entryScale by animateFloatAsState(
            targetValue   = if (entered) 1f else 0.15f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label         = "bEntry"
        )
        val entryAlpha by animateFloatAsState(
            targetValue   = if (entered) 1f else 0f,
            animationSpec = tween(480, easing = FastOutSlowInEasing),
            label         = "bAlpha"
        )

        // Active-call pulsing dot
        val pulseTrans = rememberInfiniteTransition(label = "pulse")
        val pulseScale by pulseTrans.animateFloat(
            initialValue  = 1f, targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label         = "dotPulse"
        )

        // Press spring
        val pressSource  = remember { MutableInteractionSource() }
        val isPressed    by pressSource.collectIsPressedAsState()
        val pressScale   by animateFloatAsState(
            targetValue   = if (isPressed) 0.87f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label         = "bPress"
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer(
                    scaleX = entryScale * pressScale,
                    scaleY = entryScale * pressScale,
                    alpha  = entryAlpha
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragged = false
                        do {
                            val event  = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val delta  = change.position - change.previousPosition
                            if (!dragged && (change.position - down.position).getDistance() > viewConfiguration.touchSlop)
                                dragged = true
                            if (dragged) {
                                change.consume()
                                bubbleParams.x = (bubbleParams.x + delta.x.toInt()).coerceAtLeast(0)
                                bubbleParams.y = (bubbleParams.y + delta.y.toInt()).coerceAtLeast(0)
                                try { wm.updateViewLayout(bubbleView, bubbleParams) } catch (_: Exception) {}
                            }
                            if (!change.pressed) { if (!dragged) onTap(); break }
                        } while (true)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape           = CircleShape,
                color           = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation  = 0.dp,
                shadowElevation = 0.dp,
                modifier        = Modifier.size(58.dp)
            ) {
                if (!photoUri.isNullOrEmpty()) {
                    com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar(
                        name     = name,
                        photoUri = photoUri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text       = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Dynamic-color active-call dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-3).dp, y = (-3).dp)
            ) {
                Surface(
                    shape           = CircleShape,
                    color           = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    tonalElevation  = 0.dp,
                    shadowElevation = 0.dp,
                    modifier        = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                ) {}
                Surface(
                    shape           = CircleShape,
                    color           = MaterialTheme.colorScheme.primary,
                    tonalElevation  = 0.dp,
                    shadowElevation = 0.dp,
                    modifier        = Modifier.size(11.dp).align(Alignment.Center)
                ) {}
            }
        }
    }

    // ── Menu sheet ────────────────────────────────────────────────────────────

    private fun showMenu() {
        if (menuView != null) return
        menuVisibleState.value = false
        bubbleView?.visibility = View.GONE
        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    CallMenuSheet(
                        contactName = contactNameState.value,
                        phoneNumber = phoneNumberState.value,
                        visible     = menuVisibleState.value,
                        onDismiss   = { dismissMenu() },
                        onAction    = { performAction(it) }
                    )
                }
            }
        }
        menuView = cv
        try { wm.addView(cv, menuParams) } catch (_: Exception) { dismissMenu(); return }
        scope.launch { delay(40); menuVisibleState.value = true }
    }

    private fun dismissMenu() {
        menuVisibleState.value = false
        // Hide the bubble the moment the close animation starts, then bring it back once the
        // floating popup window has actually been torn down (unless the call screen itself is
        // in the foreground, in which case observeAll() is already keeping it hidden).
        bubbleView?.visibility = View.GONE
        scope.launch {
            delay(440)
            try { menuView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
            menuView = null
            if (!CallActivity.isInForeground.value) {
                bubbleView?.visibility = View.VISIBLE
            }
        }
    }

    private fun performAction(action: MenuAction) {
        menuVisibleState.value = false
        scope.launch {
            delay(280)
            when (action) {
                is MenuAction.Speaker    -> CallService.setAudioRoute(action.route)
                is MenuAction.Mute       -> CallService.setMuted(action.mute)
                is MenuAction.Notes      -> FloatingNotesService.start(action.ctx, action.name, action.number)
                is MenuAction.BackToCall -> action.ctx.startActivity(
                    Intent(action.ctx, CallActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                MenuAction.Close -> { delay(150); removeBubble(); stopSelf() }
                MenuAction.Hangup -> CallService.declineCall()
            }
            delay(200)
            try { menuView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
            menuView = null
            if (action != MenuAction.Close && !CallActivity.isInForeground.value) {
                bubbleView?.visibility = View.VISIBLE
            }
        }
    }

    sealed class MenuAction {
        data class Speaker(val route: Int)                              : MenuAction()
        data class Mute(val mute: Boolean)                              : MenuAction()
        data class Notes(val ctx: Context, val name: String, val number: String) : MenuAction()
        data class BackToCall(val ctx: Context)                         : MenuAction()
        object Close   : MenuAction()
        object Hangup  : MenuAction()
    }

    @Composable
    private fun CallMenuSheet(
        contactName: String,
        phoneNumber: String,
        visible: Boolean,
        onDismiss: () -> Unit,
        onAction: (MenuAction) -> Unit
    ) {
        val context    = LocalContext.current
        val audioState by CallService.audioState.collectAsState()
        val isMuted    = audioState?.isMuted ?: false
        val isSpeaker  = audioState?.route == CallAudioState.ROUTE_SPEAKER

        // Full-screen Box – card is at BottomCenter so its background fills behind nav bar.
        // Tapping anywhere in the empty area above the sheet (i.e. not on the sheet itself,
        // since the sheet's own children consume their own taps first) dismisses the menu,
        // which brings the floating bubble back once the window is torn down.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter   = slideInVertically(
                    animationSpec  = tween(520, easing = EaseOutCubic),
                    initialOffsetY = { it }
                ) + fadeIn(tween(400, easing = FastOutSlowInEasing)),
                exit    = slideOutVertically(
                    animationSpec = tween(400, easing = EaseInCubic),
                    targetOffsetY = { it }
                ) + fadeOut(tween(300, easing = FastOutLinearInEasing))
            ) {
                Surface(
                    modifier        = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    shape           = RoundedCornerShape(32.dp),
                    color           = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation  = 4.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Surface(
                                modifier        = Modifier.fillMaxSize(),
                                shape           = CircleShape,
                                color           = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                                tonalElevation  = 0.dp,
                                shadowElevation = 0.dp
                            ) {}
                        }

                        Spacer(Modifier.height(14.dp))

                        // Contact info row
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = contactName,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                                if (phoneNumber.isNotEmpty()) {
                                    Text(
                                        text  = phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val dimSrc     = remember { MutableInteractionSource() }
                            val dimPressed by dimSrc.collectIsPressedAsState()
                            val dimScale   by animateFloatAsState(
                                targetValue   = if (dimPressed) 0.85f else 1f,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                label         = "dimScale"
                            )
                            FilledIconButton(
                                onClick           = onDismiss,
                                interactionSource = dimSrc,
                                modifier          = Modifier.graphicsLayer(scaleX = dimScale, scaleY = dimScale),
                                colors            = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                        Spacer(Modifier.height(14.dp))

                        // Row 1: Speaker · Mute · Notes
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment     = Alignment.Top
                        ) {
                            SheetAction(0,
                                if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                if (isSpeaker) "Earpiece" else "Speaker",
                                if (isSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                if (isSpeaker) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) { onAction(MenuAction.Speaker(if (isSpeaker) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER)) }

                            SheetAction(1,
                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                if (isMuted) "Unmute" else "Mute",
                                if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                if (isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) { onAction(MenuAction.Mute(!isMuted)) }

                            SheetAction(2,
                                Icons.Default.Note, "Notes",
                                MaterialTheme.colorScheme.onTertiaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            ) { onAction(MenuAction.Notes(context, contactName, phoneNumber)) }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Row 2: Back to call · Close Floating Circle · Hangup
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment     = Alignment.Top
                        ) {
                            SheetAction(3,
                                Icons.Default.Phone, "Back to call",
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.primaryContainer
                            ) { onAction(MenuAction.BackToCall(context)) }

                            SheetAction(4,
                                Icons.Default.Close, "Close Floating Circle",
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            ) { onAction(MenuAction.Close) }

                            HangupSheetAction(5) { onAction(MenuAction.Hangup) }
                        }

                        Spacer(Modifier.height(16.dp))
                        // Fills exactly the navigation bar height so Surface bg extends behind it
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }

    // ── Sheet action button ───────────────────────────────────────────────────

    @Composable
    private fun SheetAction(
        index: Int,
        icon: ImageVector,
        label: String,
        tint: Color,
        bgColor: Color,
        onClick: () -> Unit
    ) {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(100L + index * 55L); entered = true }
        val eScale by animateFloatAsState(
            targetValue   = if (entered) 1f else 0.2f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
            label         = "eScale$index"
        )
        val eAlpha by animateFloatAsState(
            targetValue   = if (entered) 1f else 0f,
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            label         = "eAlpha$index"
        )
        val pSrc      = remember { MutableInteractionSource() }
        val pPressed  by pSrc.collectIsPressedAsState()
        val pScale    by animateFloatAsState(
            targetValue   = if (pPressed) 0.83f else 1f,
            animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
            label         = "pScale$index"
        )

        Column(
            modifier = Modifier
                .width(80.dp)
                .graphicsLayer(scaleX = eScale * pScale, scaleY = eScale * pScale, alpha = eAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape           = CircleShape,
                color           = bgColor,
                tonalElevation  = 0.dp,
                shadowElevation = 0.dp,
                modifier        = Modifier
                    .size(54.dp)
                    .clickable(interactionSource = pSrc, indication = null, onClick = onClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun HangupSheetAction(index: Int, onClick: () -> Unit) {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(100L + index * 55L); entered = true }
        val eScale by animateFloatAsState(
            targetValue   = if (entered) 1f else 0.2f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
            label         = "heScale"
        )
        val eAlpha by animateFloatAsState(
            targetValue   = if (entered) 1f else 0f,
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            label         = "heAlpha"
        )
        val pSrc     = remember { MutableInteractionSource() }
        val pPressed by pSrc.collectIsPressedAsState()
        val pScale   by animateFloatAsState(
            targetValue   = if (pPressed) 0.83f else 1f,
            animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
            label         = "hpScale"
        )

        Column(
            modifier = Modifier
                .width(80.dp)
                .graphicsLayer(scaleX = eScale * pScale, scaleY = eScale * pScale, alpha = eAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape           = CircleShape,
                color           = Color(0xFFB71C1C),
                tonalElevation  = 0.dp,
                shadowElevation = 0.dp,
                modifier        = Modifier
                    .size(54.dp)
                    .clickable(interactionSource = pSrc, indication = null, onClick = onClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CallEnd, "Hangup", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            Text(
                text      = "Hangup",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeAll() {
        scope.launch {
            CallService.currentCallSession.collect { if (it == null) { removeBubble(); stopSelf() } }
        }
        scope.launch {
            CallActivity.isInForeground.collect { inForeground ->
                bubbleView?.visibility = if (inForeground) View.GONE else View.VISIBLE
                if (inForeground && menuView != null) {
                    menuVisibleState.value = false
                    delay(440)
                    try { menuView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
                    menuView = null
                }
            }
        }
    }

    private fun removeBubble() {
        menuVisibleState.value = false
        try { menuView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
        menuView = null
        try { bubbleView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
        bubbleView = null
    }

    override fun onDestroy() {
        unregisterReceiver(configReceiver)
        scope.cancel(); lifecycleOwner.onDestroy(); removeBubble(); super.onDestroy()
    }
}
