package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BlurMaskFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingNotesService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()

    companion object {
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PHONE_NUMBER = "phone_number"

        fun start(context: Context, contactName: String, phoneNumber: String) {
            context.startService(
                Intent(context, FloatingNotesService::class.java).apply {
                    putExtra(EXTRA_CONTACT_NAME, contactName)
                    putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val name   = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "Unknown"
        val number = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        removeOverlay()
        showOverlay(name, number)
        return START_NOT_STICKY
    }

    private fun showOverlay(contactName: String, phoneNumber: String) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity       = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    FloatingNoteOverlay(
                        contactName = contactName,
                        phoneNumber = phoneNumber,
                        onDismiss   = { removeOverlay(); stopSelf() }
                    )
                }
            }
        }
        overlayView = cv
        try { windowManager.addView(cv, params) } catch (_: Exception) { stopSelf() }
    }

    private fun removeOverlay() {
        try { overlayView?.let { windowManager.removeViewImmediate(it) } } catch (_: Exception) {}
        overlayView = null
    }

    override fun onDestroy() {
        lifecycleOwner.onDestroy()
        removeOverlay()
        super.onDestroy()
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingNoteOverlay(
    contactName: String,
    phoneNumber: String,
    onDismiss: () -> Unit
) {
    val context      = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var text         by remember { mutableStateOf(NoteManager.readNote(context, contactName, phoneNumber)) }

    // ── Animation state ───────────────────────────────────────────────────────
    // `showing` drives both enter and exit. Flip to false to play exit then call onDismiss.
    var showing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(40); showing = true }

    fun saveAndDismiss() {
        coroutineScope.launch {
            NoteManager.writeNote(context, contactName, phoneNumber, text)
            showing = false                    // trigger exit animation
            delay(420)                         // wait for it to finish
            onDismiss()
        }
    }

    // Shadow color captured once
    val shadowColor = Color.Black.copy(alpha = 0.22f).toArgb()

    // Outer Box: transparent, keyboard-aware, tap outside = save & close
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .clickable(onClick = { saveAndDismiss() }),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showing,
            enter   = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessLow
                ),
                initialScale  = 0.82f
            ) + fadeIn(tween(460, easing = FastOutSlowInEasing)),
            exit    = scaleOut(
                animationSpec = tween(380, easing = FastOutLinearInEasing),
                targetScale   = 0.86f
            ) + fadeOut(tween(340, easing = FastOutLinearInEasing))
        ) {
            // Card wrapper with manual rounded shadow via BlurMaskFilter
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = {})  // consume – block scrim dismiss
                    .drawBehind {
                        val blurR = 22.dp.toPx()
                        val offY  =  8.dp.toPx()
                        drawIntoCanvas { canvas ->
                            val paint = androidx.compose.ui.graphics.Paint()
                            paint.asFrameworkPaint().apply {
                                isAntiAlias = true
                                color       = shadowColor
                                maskFilter  = BlurMaskFilter(blurR, BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.drawRoundRect(
                                left    = blurR,
                                top     = blurR,
                                right   = size.width  - blurR,
                                bottom  = size.height - blurR + offY,
                                radiusX = 24.dp.toPx(),
                                radiusY = 24.dp.toPx(),
                                paint   = paint
                            )
                        }
                    },
                shape           = RoundedCornerShape(24.dp),
                color           = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation  = 2.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Header ────────────────────────────────────────────────
                    CardElement(index = 0) {
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
                            // Only Close button – Save icon removed
                            SpringIconButton(onClick = { saveAndDismiss() }) {
                                Icon(
                                    Icons.Default.Close, "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Text field ────────────────────────────────────────────
                    CardElement(index = 1) {
                        OutlinedTextField(
                            value         = text,
                            onValueChange = { text = it },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp, max = 240.dp),
                            placeholder   = { Text("Type your note here…") },
                            shape         = RoundedCornerShape(14.dp),
                            minLines      = 3,
                            maxLines      = 9
                        )
                    }

                    // ── Save & Close button ───────────────────────────────────
                    CardElement(index = 2) {
                        val btnSrc     = remember { MutableInteractionSource() }
                        val btnPressed by btnSrc.collectIsPressedAsState()
                        val btnScale   by animateFloatAsState(
                            targetValue   = if (btnPressed) 0.91f else 1f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            label         = "btnScale"
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick           = { saveAndDismiss() },
                                interactionSource = btnSrc,
                                modifier          = Modifier.graphicsLayer(scaleX = btnScale, scaleY = btnScale),
                                shape             = RoundedCornerShape(12.dp)
                            ) { Text("Save & Close") }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Staggered fade + slide-up entrance for each card element. */
@Composable
private fun CardElement(index: Int, content: @Composable BoxScope.() -> Unit) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(160L + index * 65L); ready = true }

    val alpha by animateFloatAsState(
        targetValue   = if (ready) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label         = "ceAlpha$index"
    )
    val transY by animateFloatAsState(
        targetValue   = if (ready) 0f else 20f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label         = "ceY$index"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha, translationY = transY),
        content = content
    )
}

/** Icon button with a satisfying spring press scale. */
@Composable
private fun SpringIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val src     = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale   by animateFloatAsState(
        targetValue   = if (pressed) 0.78f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "sibScale"
    )
    IconButton(
        onClick           = onClick,
        interactionSource = src,
        modifier          = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) { content() }
}
