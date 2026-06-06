package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.telecom.CallAudioState
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.coolappstore.everdialer.by.svhp.view.screen.CallActivity
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingCallService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: ComposeView? = null
    private var menuView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Observable state shared with composables
    private val contactNameState = mutableStateOf("?")
    private val phoneNumberState  = mutableStateOf("")

    private val bubbleParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 20; y = 300
    }

    companion object {
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PHONE_NUMBER = "phone_number"

        fun start(context: Context, name: String, number: String) {
            context.startService(
                Intent(context, FloatingCallService::class.java).apply {
                    putExtra(EXTRA_CONTACT_NAME, name)
                    putExtra(EXTRA_PHONE_NUMBER, number)
                }
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingCallService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        contactNameState.value = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "?"
        phoneNumberState.value  = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        if (bubbleView == null) {
            createBubble()
            observeCallSession()
        }
        return START_STICKY
    }

    // ── Bubble ────────────────────────────────────────────────────────────────

    private fun createBubble() {
        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    BubbleUI(
                        name    = contactNameState.value,
                        onTap   = { showMenu() },
                        onClose = { removeBubble(); stopSelf() }
                    )
                }
            }
        }
        bubbleView = cv
        try { wm.addView(cv, bubbleParams) } catch (_: Exception) { stopSelf() }
    }

    @Composable
    private fun BubbleUI(name: String, onTap: () -> Unit, onClose: () -> Unit) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragged = false
                        do {
                            val event  = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val delta  = change.position - change.previousPosition
                            val dist   = (change.position - down.position).getDistance()
                            if (!dragged && dist > viewConfiguration.touchSlop) dragged = true
                            if (dragged) {
                                change.consume()
                                bubbleParams.x = (bubbleParams.x + delta.x.toInt()).coerceAtLeast(0)
                                bubbleParams.y = (bubbleParams.y + delta.y.toInt()).coerceAtLeast(0)
                                try { wm.updateViewLayout(bubbleView, bubbleParams) } catch (_: Exception) {}
                            }
                            if (!change.pressed) {
                                if (!dragged) onTap()
                                break
                            }
                        } while (true)
                    }
                }
        ) {
            // ── Main circle ───────────────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .shadow(elevation = 10.dp, shape = CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ── Active-call green dot ─────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = Color(0xFF43A047),
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-6).dp)
            ) {}

            // ── Close button (top-right) ──────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onClose)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close bubble",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }

    // ── Menu overlay ──────────────────────────────────────────────────────────

    private fun showMenu() {
        if (menuView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    CallMenuOverlay(
                        contactName = contactNameState.value,
                        phoneNumber = phoneNumberState.value,
                        onDismiss = { dismissMenu() }
                    )
                }
            }
        }
        menuView = cv
        try { wm.addView(cv, params) } catch (_: Exception) { dismissMenu() }
    }

    private fun dismissMenu() {
        try { menuView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
        menuView = null
    }

    @Composable
    private fun CallMenuOverlay(
        contactName: String,
        phoneNumber: String,
        onDismiss: () -> Unit
    ) {
        val context    = LocalContext.current
        val audioState by CallService.audioState.collectAsState()
        val isMuted    = audioState?.isMuted ?: false
        val isSpeaker  = audioState?.route == CallAudioState.ROUTE_SPEAKER

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { /* consume */ })
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Contact info ──────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = contactName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (phoneNumber.isNotEmpty()) {
                                Text(
                                    text  = phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Close menu button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ── Row 1: Speaker, Mute, Notes ───────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallMenuAction(
                            icon    = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            label   = if (isSpeaker) "Earpiece" else "Speaker",
                            tint    = if (isSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            bgColor = if (isSpeaker) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = {
                                CallService.setAudioRoute(
                                    if (isSpeaker) CallAudioState.ROUTE_EARPIECE
                                    else           CallAudioState.ROUTE_SPEAKER
                                )
                                onDismiss()
                            }
                        )
                        CallMenuAction(
                            icon    = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label   = if (isMuted) "Unmute" else "Mute",
                            tint    = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            bgColor = if (isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = {
                                CallService.setMuted(!isMuted)
                                onDismiss()
                            }
                        )
                        CallMenuAction(
                            icon    = Icons.Default.Note,
                            label   = "Notes",
                            tint    = MaterialTheme.colorScheme.onSurface,
                            bgColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = {
                                onDismiss()
                                FloatingNotesService.start(context, contactName, phoneNumber)
                            }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Row 2: Back to call, Hangup ───────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallMenuAction(
                            icon    = Icons.Default.Phone,
                            label   = "Back to call",
                            tint    = MaterialTheme.colorScheme.onPrimaryContainer,
                            bgColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {
                                onDismiss()
                                context.startActivity(
                                    Intent(context, CallActivity::class.java).apply {
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                        )
                                    }
                                )
                            }
                        )
                        CallMenuAction(
                            icon    = Icons.Default.CallEnd,
                            label   = "Hangup",
                            tint    = Color.White,
                            bgColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                onDismiss()
                                CallService.declineCall()
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    @Composable
    private fun CallMenuAction(
        icon: ImageVector,
        label: String,
        tint: Color,
        bgColor: Color,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape   = CircleShape,
                color   = bgColor,
                modifier = Modifier
                    .size(54.dp)
                    .clickable(onClick = onClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private fun observeCallSession() {
        scope.launch {
            CallService.currentCallSession.collect { session ->
                if (session == null) {
                    removeBubble()
                    stopSelf()
                }
            }
        }
    }

    private fun removeBubble() {
        dismissMenu()
        try { bubbleView?.let { wm.removeViewImmediate(it) } } catch (_: Exception) {}
        bubbleView = null
    }

    override fun onDestroy() {
        scope.cancel()
        lifecycleOwner.onDestroy()
        removeBubble()
        super.onDestroy()
    }
}
