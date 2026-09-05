package com.coolappstore.everdialer.by.svhp.controller

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.controller.util.*
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.performAppHaptic
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MissedCallPopupService : Service() {

    private lateinit var wm: WindowManager
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val phoneNumberState = mutableStateOf("")
    private val contactNameState = mutableStateOf("Unknown")
    private val photoUriState = mutableStateOf<String?>(null)
    private val contactIdState = mutableStateOf<String?>(null)
    private val callDateState = mutableLongStateOf(0L)
    private val ringDurationSecState = mutableLongStateOf(0L)

    private val windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        }
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_PHOTO_URI = "extra_photo_uri"
        const val EXTRA_CONTACT_ID = "extra_contact_id"
        const val EXTRA_CALL_DATE = "extra_call_date"
        const val EXTRA_RING_DURATION = "extra_ring_duration"

        fun start(
            context: Context,
            number: String,
            name: String? = null,
            photoUri: String? = null,
            contactId: String? = null,
            callDate: Long = System.currentTimeMillis(),
            ringDurationSec: Long = 0L
        ) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, MissedCallPopupService::class.java).apply {
                putExtra(EXTRA_PHONE_NUMBER, number)
                putExtra(EXTRA_CONTACT_NAME, name ?: number)
                putExtra(EXTRA_PHOTO_URI, photoUri)
                putExtra(EXTRA_CONTACT_ID, contactId)
                putExtra(EXTRA_CALL_DATE, callDate)
                putExtra(EXTRA_RING_DURATION, ringDurationSec)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, MissedCallPopupService::class.java))
            } catch (_: Exception) {}
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

        val number = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val name = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: number.ifBlank { "Unknown" }
        val photo = intent?.getStringExtra(EXTRA_PHOTO_URI)
        val contactId = intent?.getStringExtra(EXTRA_CONTACT_ID)
        val callDate = intent?.getLongExtra(EXTRA_CALL_DATE, System.currentTimeMillis()) ?: System.currentTimeMillis()
        val ringDuration = intent?.getLongExtra(EXTRA_RING_DURATION, 0L) ?: 0L

        phoneNumberState.value = number
        contactNameState.value = name
        photoUriState.value = photo
        contactIdState.value = contactId
        callDateState.longValue = callDate
        ringDurationSecState.longValue = ringDuration

        if (overlayView == null) {
            createOverlayView()
        }

        return START_NOT_STICKY
    }

    private fun createOverlayView() {
        val cv = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                Rivo4Theme {
                    MissedCallPopupContent(
                        phoneNumber = phoneNumberState.value,
                        contactName = contactNameState.value,
                        photoUri = photoUriState.value,
                        contactId = contactIdState.value,
                        callDate = callDateState.longValue,
                        ringDurationSec = ringDurationSecState.longValue,
                        onDismiss = { dismissAndStop() }
                    )
                }
            }
        }
        overlayView = cv
        try {
            wm.addView(cv, windowParams)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun dismissAndStop() {
        scope.launch {
            try {
                overlayView?.let { wm.removeView(it) }
            } catch (_: Exception) {}
            overlayView = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onDestroy()
        try {
            overlayView?.let { wm.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        scope.cancel()
    }

    @Composable
    private fun MissedCallPopupContent(
        phoneNumber: String,
        contactName: String,
        photoUri: String?,
        contactId: String?,
        callDate: Long,
        ringDurationSec: Long,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(30)
            visible = true
        }

        fun triggerDismiss() {
            visible = false
            scope.launch {
                delay(220)
                onDismiss()
            }
        }

        // Relative time string and formatted call received time
        val formattedTime = remember(callDate) {
            val is24Hour = DateFormat.is24HourFormat(context)
            val sdf = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
            sdf.format(Date(callDate))
        }

        var relativeTime by remember { mutableStateOf(formatRelativeTime(callDate)) }
        LaunchedEffect(callDate) {
            while (isActive) {
                relativeTime = formatRelativeTime(callDate)
                delay(15000L)
            }
        }

        val ringText = if (ringDurationSec > 0) "rang ${ringDurationSec}s" else "rang 0s"

        // Social apps detection from SocialAppActions
        val whatsAppInstalled = remember(context) { isAnyPackageInstalled(context, WHATSAPP_PACKAGES) }
        val telegramInstalled = remember(context) { isTelegramInstalled(context) }
        val meetInstalled = remember(context) { isGoogleMeetInstalled(context) }
        val truecallerInstalled = remember(context) { isTruecallerInstalled(context) }

        val whatsAppIcon: ImageBitmap? = remember(context, whatsAppInstalled) { if (whatsAppInstalled) getWhatsAppIcon(context) else null }
        val telegramIcon: ImageBitmap? = remember(context, telegramInstalled) { if (telegramInstalled) getTelegramIcon(context) else null }
        val meetIcon: ImageBitmap? = remember(context, meetInstalled) { if (meetInstalled) getGoogleMeetIcon(context) else null }
        val truecallerIcon: ImageBitmap? = remember(context, truecallerInstalled) { if (truecallerInstalled) getTruecallerIcon(context) else null }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (visible) 0.52f else 0f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    triggerDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.82f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(240)),
                exit = scaleOut(
                    targetScale = 0.88f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 420.dp)
                        .padding(vertical = 24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* prevent outside dismissal */ }
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // ── Top Header Banner (warm golden / expressive gradient) ──
                        val isDark = MaterialTheme.colorScheme.surface.let {
                            androidx.core.graphics.ColorUtils.calculateLuminance(it.hashCode()) < 0.5
                        }
                        val headerGradient = if (isDark) {
                            listOf(
                                Color(0xFF684E12),
                                Color(0xFF423207),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        } else {
                            listOf(
                                Color(0xFFFFECC2),
                                Color(0xFFF6DE98),
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(headerGradient))
                                .padding(18.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Contact PFP with expressive styling
                                    Box(
                                        modifier = Modifier.size(62.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = photoUri,
                                                contentDescription = contactName,
                                                modifier = Modifier
                                                    .size(58.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            RivoAvatar(
                                                name = contactName,
                                                photoUri = null,
                                                modifier = Modifier.size(58.dp)
                                            )
                                        }

                                        // Gold ring & missed call badge
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .size(20.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFFFB300),
                                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                            shadowElevation = 2.dp
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                                contentDescription = null,
                                                tint = Color(0xFF4A3200),
                                                modifier = Modifier.padding(3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Caller details
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Missed call $relativeTime, $ringText",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isDark) Color(0xFFFFD56B) else Color(0xFF684900),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = contactName.ifBlank { phoneNumber },
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (contactName.isNotBlank() && phoneNumber.isNotBlank() && contactName != phoneNumber) {
                                            Text(
                                                text = "$phoneNumber • $formattedTime",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = formattedTime,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Close (X) button
                                    IconButton(
                                        onClick = {
                                            performAppHaptic(context, "light")
                                            triggerDismiss()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss popup",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // View call logs button
                                Surface(
                                    onClick = {
                                        performAppHaptic(context, "light")
                                        val intent = Intent(context, MainActivity::class.java).apply {
                                            action = "com.coolappstore.everdialer.OPEN_CALL_LOGS_DETAIL"
                                            putExtra("contact_id", contactId)
                                            putExtra("phone_number", phoneNumber)
                                            putExtra("NAV_TO_RECENTS", true)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                        triggerDismiss()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF3B2D0E) else Color(0xFF4E3714),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 11.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD56B),
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "View call logs",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.3.sp
                                            ),
                                            color = Color(0xFFFFD56B)
                                        )
                                    }
                                }
                            }
                        }

                        // ── Respond With Message Section ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "RESPOND WITH MESSAGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )

                            val quickReplies = listOf(
                                "Call me back?",
                                "Sorry I'm busy",
                                "I'll call you later",
                                "Type custom..."
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickReplies.forEach { text ->
                                    Surface(
                                        onClick = {
                                            performAppHaptic(context, "light")
                                            val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
                                            if (text == "Type custom...") {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            } else {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                                                    putExtra("sms_body", text)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            }
                                            triggerDismiss()
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // ── Bottom Action Buttons & Social Container ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Call Button
                            ActionButtonItem(
                                iconVector = Icons.Default.Call,
                                label = "CALL",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    performAppHaptic(context, "light")
                                    val clean = phoneNumber.filter { it.isDigit() || it == '+' }
                                    val intent = if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    } else {
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                    triggerDismiss()
                                }
                            )

                            // 2. SMS Button
                            ActionButtonItem(
                                iconVector = Icons.Outlined.Chat,
                                label = "MESSAGE",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                onClick = {
                                    performAppHaptic(context, "light")
                                    val clean = phoneNumber.filter { it.isDigit() || it == '+' }
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                    triggerDismiss()
                                }
                            )

                            // 3. WhatsApp (if installed)
                            if (whatsAppInstalled) {
                                ActionButtonItem(
                                    iconBitmap = whatsAppIcon,
                                    iconVector = Icons.Default.Chat,
                                    label = "WhatsApp",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        performAppHaptic(context, "light")
                                        openWhatsAppChat(context, phoneNumber)
                                        triggerDismiss()
                                    }
                                )
                            }

                            // 4. Telegram (if installed)
                            if (telegramInstalled) {
                                ActionButtonItem(
                                    iconBitmap = telegramIcon,
                                    iconVector = Icons.Default.Send,
                                    label = "Telegram",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        performAppHaptic(context, "light")
                                        openTelegramChat(context, phoneNumber)
                                        triggerDismiss()
                                    }
                                )
                            }

                            // 5. Google Meet (if installed)
                            if (meetInstalled) {
                                ActionButtonItem(
                                    iconBitmap = meetIcon,
                                    iconVector = Icons.Default.VideoCall,
                                    label = "Meet",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        performAppHaptic(context, "light")
                                        startGoogleMeetVoiceCall(context, phoneNumber)
                                        triggerDismiss()
                                    }
                                )
                            }

                            // 6. Truecaller (if installed)
                            if (truecallerInstalled) {
                                ActionButtonItem(
                                    iconBitmap = truecallerIcon,
                                    iconVector = Icons.Default.Search,
                                    label = "Truecaller",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        performAppHaptic(context, "light")
                                        openTruecaller(context, phoneNumber)
                                        triggerDismiss()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionButtonItem(
        iconVector: ImageVector,
        iconBitmap: ImageBitmap? = null,
        label: String,
        containerColor: Color,
        contentColor: Color,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }

    private fun formatRelativeTime(dateMillis: Long): String {
        val diff = System.currentTimeMillis() - dateMillis
        if (diff < 45_000L) return "Just now"
        val minutes = (diff / 60_000L).coerceAtLeast(1L)
        if (minutes < 60L) return "${minutes}m ago"
        val hours = minutes / 60L
        if (hours < 24L) return "${hours}h ago"
        val days = hours / 24L
        return "${days}d ago"
    }
}
