package com.coolappstore.everdialer.by.svhp.controller

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.text.format.DateFormat
import android.view.Gravity
import android.view.KeyEvent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
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
    private var dismissTriggerCallback: (() -> Unit)? = null
    private var backHandlerCallback: (() -> Boolean)? = null

    private val windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
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

        fun previewLastMissedCall(context: Context) {
            if (!Settings.canDrawOverlays(context)) return

            var targetNumber = "+1 234 567 8900"
            var targetName: String? = null
            var targetPhoto: String? = null
            var targetContactId: String? = null
            var targetDate = System.currentTimeMillis() - 240_000L
            var targetRingSec = 2L

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val projection = arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.CACHED_PHOTO_URI,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                    )
                    val selection = "${CallLog.Calls.TYPE} = ?"
                    val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
                    val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 1"

                    context.contentResolver.query(
                        CallLog.Calls.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val num = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""
                            var name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
                            var photo = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI))
                            val date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE))
                            var dur = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION))

                            if (dur <= 0L) {
                                dur = MissedCallDurationStore.getDuration(context, num, date)
                            }

                            if (name.isNullOrBlank() || photo.isNullOrBlank()) {
                                val resolved = resolveContact(context, num)
                                if (name.isNullOrBlank()) name = resolved.name
                                if (photo.isNullOrBlank()) photo = resolved.photoUri
                                targetContactId = resolved.contactId
                            }

                            targetNumber = num
                            targetName = name
                            targetPhoto = photo
                            targetDate = date
                            targetRingSec = dur
                        }
                    }
                } catch (_: Exception) {}
            }

            start(
                context = context,
                number = targetNumber,
                name = targetName ?: targetNumber,
                photoUri = targetPhoto,
                contactId = targetContactId,
                callDate = targetDate,
                ringDurationSec = targetRingSec
            )
        }

        private data class ContactInfo(val name: String?, val photoUri: String?, val contactId: String?)

        private fun resolveContact(context: Context, number: String): ContactInfo {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return ContactInfo(null, null, null)
            }
            return try {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
                val projection = arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                        val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                        val photo = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                            ?: cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI))
                        ContactInfo(name, photo, contactId)
                    } else {
                        ContactInfo(null, null, null)
                    }
                } ?: ContactInfo(null, null, null)
            } catch (_: Exception) {
                ContactInfo(null, null, null)
            }
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
            fitsSystemWindows = false
            isFocusable = true
            isFocusableInTouchMode = true
            setContent {
                Rivo4Theme {
                    MissedCallPopupContent(
                        phoneNumber = phoneNumberState.value,
                        contactName = contactNameState.value,
                        photoUri = photoUriState.value,
                        callDate = callDateState.longValue,
                        ringDurationSec = ringDurationSecState.longValue,
                        contactId = contactIdState.value,
                        onDismiss = { dismissAndStop() },
                        onRegisterDismiss = { dismissTriggerCallback = it },
                        onRegisterBackHandler = { backHandlerCallback = it }
                    )
                }
            }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    if (backHandlerCallback?.invoke() == true) {
                        true
                    } else {
                        dismissTriggerCallback?.invoke() ?: dismissAndStop()
                        true
                    }
                } else {
                    false
                }
            }
        }
        overlayView = cv
        try {
            wm.addView(cv, windowParams)
            cv.requestFocus()
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
        callDate: Long,
        ringDurationSec: Long,
        contactId: String?,
        onDismiss: () -> Unit,
        onRegisterDismiss: (() -> Unit) -> Unit,
        onRegisterBackHandler: (() -> Boolean) -> Unit
    ) {
        val context = LocalContext.current
        var visible by remember { mutableStateOf(false) }
        var isCustomTyping by remember { mutableStateOf(false) }
        var customMessageText by remember { mutableStateOf("") }
        var selectedMessageForAppChoice by remember { mutableStateOf<String?>(null) }
        var selectedSocialApp by remember { mutableStateOf<String?>(null) }
        var selectedSimAccounts by remember { mutableStateOf<List<PhoneAccountHandle>?>(null) }
        val focusRequester = remember { FocusRequester() }

        fun triggerDismiss() {
            if (!visible) return
            visible = false
            scope.launch {
                delay(220)
                onDismiss()
            }
        }

        fun handleBack(): Boolean {
            if (selectedSocialApp != null) {
                selectedSocialApp = null
                return true
            }
            if (selectedSimAccounts != null) {
                selectedSimAccounts = null
                return true
            }
            if (selectedMessageForAppChoice != null) {
                selectedMessageForAppChoice = null
                return true
            }
            if (isCustomTyping) {
                isCustomTyping = false
                return true
            }
            return false
        }

        LaunchedEffect(Unit) {
            onRegisterDismiss { triggerDismiss() }
            onRegisterBackHandler { handleBack() }
            delay(30)
            visible = true
            delay(100)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
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

        // Load customizable quick replies from PreferenceManager
        val prefs = remember(context) { PreferenceManager(context) }
        val customFirst = remember(prefs) {
            prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_CUSTOM_FIRST, false)
        }
        val reply1 = remember(prefs) {
            prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_1, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_1) ?: PreferenceManager.DEFAULT_MISSED_CALL_REPLY_1
        }
        val reply2 = remember(prefs) {
            prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_2, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_2) ?: PreferenceManager.DEFAULT_MISSED_CALL_REPLY_2
        }
        val reply3 = remember(prefs) {
            prefs.getString(PreferenceManager.KEY_MISSED_CALL_QUICK_REPLY_3, PreferenceManager.DEFAULT_MISSED_CALL_REPLY_3) ?: PreferenceManager.DEFAULT_MISSED_CALL_REPLY_3
        }
        val quickReplies = remember(reply1, reply2, reply3, customFirst) {
            val list = mutableListOf<String>()
            if (reply1.isNotBlank()) list.add(reply1.trim())
            if (reply2.isNotBlank()) list.add(reply2.trim())
            if (reply3.isNotBlank()) list.add(reply3.trim())
            if (customFirst) {
                list.add(0, "Type custom...")
            } else {
                list.add("Type custom...")
            }
            list
        }

        // Social apps detection from SocialAppActions
        val whatsAppInstalled = remember(context) { isAnyPackageInstalled(context, WHATSAPP_PACKAGES) }
        val telegramInstalled = remember(context) { isTelegramInstalled(context) }
        val meetInstalled = remember(context) { isGoogleMeetInstalled(context) }
        val truecallerInstalled = remember(context) { isTruecallerInstalled(context) }

        val whatsAppIcon: ImageBitmap? = remember(context, whatsAppInstalled) { if (whatsAppInstalled) getWhatsAppIcon(context) else null }
        val telegramIcon: ImageBitmap? = remember(context, telegramInstalled) { if (telegramInstalled) getTelegramIcon(context) else null }
        val meetIcon: ImageBitmap? = remember(context, meetInstalled) { if (meetInstalled) getGoogleMeetIcon(context) else null }
        val truecallerIcon: ImageBitmap? = remember(context, truecallerInstalled) { if (truecallerInstalled) getTruecallerIcon(context) else null }

        val animatedDimAlpha by animateFloatAsState(
            targetValue = if (visible) 0.55f else 0f,
            animationSpec = tween(220),
            label = "scrimDim"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = animatedDimAlpha))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK && keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        if (handleBack()) {
                            true
                        } else {
                            triggerDismiss()
                            true
                        }
                    } else {
                        false
                    }
                }
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
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
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
                        // ── Top Header Banner (contact pfp blurred with dim, or gradient if no pfp) ──
                        val hasPfp = !photoUri.isNullOrBlank()
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
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        ) {
                            if (hasPfp) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.52f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Brush.verticalGradient(headerGradient))
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Contact PFP with expressive styling
                                    Box(
                                        modifier = Modifier.size(62.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasPfp) {
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
                                                forcePersonIcon = true,
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
                                            color = if (hasPfp || isDark) Color(0xFFFFD56B) else Color(0xFF684900),
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
                                            color = if (hasPfp) Color.White else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (contactName.isNotBlank() && phoneNumber.isNotBlank() && contactName != phoneNumber) {
                                            Text(
                                                text = "$phoneNumber • $formattedTime",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = if (hasPfp) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = formattedTime,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = if (hasPfp) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                                            tint = if (hasPfp) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // View call logs button (curved pill style, reasonable width, centered)
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        onClick = {
                                            performAppHaptic(context, "light")
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                action = "com.coolappstore.everdialer.OPEN_RECENTS"
                                                putExtra("NAV_TO_RECENTS", true)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                            triggerDismiss()
                                        },
                                        shape = CircleShape,
                                        color = if (hasPfp || isDark) Color(0xFF3B2D0E) else Color(0xFF4E3714),
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .padding(vertical = 9.dp, horizontal = 22.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD56B),
                                                modifier = Modifier.size(18.dp)
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
                        }

                        val replyScrollState = rememberScrollState()
                        val contactSimKey = contactId ?: phoneNumber
                        val contactSimChoice = remember(prefs, contactSimKey) { prefs.getContactSimChoice(contactSimKey) }
                        val globalSimPref = remember(prefs) { prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault()) }

                        fun initiateCall() {
                            performAppHaptic(context, "light")
                            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                            val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                            val accounts = if (hasPhoneState && telecomManager != null) {
                                try {
                                    telecomManager.callCapablePhoneAccounts
                                } catch (_: Exception) { emptyList() }
                            } else emptyList()

                            if (accounts.size <= 1) {
                                makeCall(context, phoneNumber, accounts.firstOrNull())
                                triggerDismiss()
                                return
                            }

                            var showPicker = false
                            placeCallWithContactSimPreference(
                                context = context,
                                number = phoneNumber,
                                contactSimChoice = contactSimChoice,
                                globalSimPref = globalSimPref,
                                recentSimSlotForContact = null,
                                onShowSimPicker = {
                                    showPicker = true
                                    selectedSimAccounts = accounts
                                }
                            )
                            if (!showPicker) {
                                triggerDismiss()
                            }
                        }

                        fun sendViaSms(msg: String?) {
                            performAppHaptic(context, "light")
                            val clean = phoneNumber.filter { it.isDigit() || it == '+' }
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
                                if (!msg.isNullOrBlank() && msg != "Type custom...") {
                                    putExtra("sms_body", msg)
                                }
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                            triggerDismiss()
                        }

                        fun sendViaWhatsApp(msg: String?) {
                            performAppHaptic(context, "light")
                            val messageToSend = if (msg != "Type custom...") msg else null
                            openWhatsAppChat(context, phoneNumber, messageToSend)
                            triggerDismiss()
                        }

                        fun sendViaTelegram(msg: String?) {
                            performAppHaptic(context, "light")
                            val messageToSend = if (msg != "Type custom...") msg else null
                            openTelegramChat(context, phoneNumber, messageToSend)
                            triggerDismiss()
                        }

                        val currentCardState = when {
                            selectedSimAccounts != null -> "sim"
                            selectedSocialApp != null -> "social"
                            isCustomTyping -> "custom_type"
                            selectedMessageForAppChoice != null -> "reply"
                            else -> "main"
                        }

                        AnimatedContent(
                            targetState = currentCardState,
                            transitionSpec = {
                                if (targetState != "main") {
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                } else {
                                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                }
                            },
                            label = "CardSectionTransition"
                        ) { state ->
                            when (state) {
                                "main" -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // ── Respond With Message Section ──
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "RESPOND WITH MESSAGE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.8.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        performAppHaptic(context, "light")
                                                        isCustomTyping = true
                                                    }
                                                )
                                            }

                                            val showScrollIndicator by remember {
                                                derivedStateOf {
                                                    replyScrollState.value == 0 && replyScrollState.maxValue > 0
                                                }
                                            }

                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(replyScrollState),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    quickReplies.forEach { text ->
                                                        Surface(
                                                            onClick = {
                                                                performAppHaptic(context, "light")
                                                                if (text == "Type custom...") {
                                                                    isCustomTyping = true
                                                                } else {
                                                                    selectedMessageForAppChoice = text
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(20.dp),
                                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                        ) {
                                                            Text(
                                                                text = formatQuickReplyDisplay(text),
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 13.sp
                                                                ),
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Single indication dot that hides smoothly as soon as scrolling starts
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = showScrollIndicator,
                                                    enter = fadeIn(animationSpec = tween(150)),
                                                    exit = fadeOut(animationSpec = tween(150)),
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .padding(end = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        )

                                        // ── Bottom Action Buttons & Social Container (non-scrollable, all in one place) ──
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Call Button (triggers real phone call honoring SIM settings)
                                            ActionButtonItem(
                                                iconVector = Icons.Default.Call,
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                label = "CALL",
                                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                onClick = {
                                                    initiateCall()
                                                }
                                            )

                                            // 2. SMS Button
                                            ActionButtonItem(
                                                iconVector = Icons.Outlined.Chat,
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                label = "MESSAGE",
                                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                onClick = {
                                                    sendViaSms(null)
                                                }
                                            )

                                            // 3. WhatsApp (if installed)
                                            if (whatsAppInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = whatsAppIcon,
                                                    iconVector = Icons.Default.Chat,
                                                    label = "WhatsApp",
                                                    onClick = {
                                                        performAppHaptic(context, "light")
                                                        selectedSocialApp = "whatsapp"
                                                    }
                                                )
                                            }

                                            // 4. Telegram (if installed)
                                            if (telegramInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = telegramIcon,
                                                    iconVector = Icons.Default.Send,
                                                    label = "Telegram",
                                                    onClick = {
                                                        performAppHaptic(context, "light")
                                                        selectedSocialApp = "telegram"
                                                    }
                                                )
                                            }

                                            // 5. Google Meet (if installed)
                                            if (meetInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = meetIcon,
                                                    iconVector = Icons.Default.VideoCall,
                                                    label = "Meet",
                                                    onClick = {
                                                        performAppHaptic(context, "light")
                                                        selectedSocialApp = "googlemeet"
                                                    }
                                                )
                                            }

                                            // 6. Truecaller (if installed)
                                            if (truecallerInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = truecallerIcon,
                                                    iconVector = Icons.Default.Search,
                                                    label = "Truecaller",
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
                                "custom_type" -> {
                                    val customFocusRequester = remember { FocusRequester() }
                                    LaunchedEffect(Unit) {
                                        delay(150)
                                        try { customFocusRequester.requestFocus() } catch (_: Exception) {}
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    isCustomTyping = false
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Custom Response",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Reply to ${contactName.ifBlank { phoneNumber }}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = customMessageText,
                                            onValueChange = { customMessageText = it },
                                            placeholder = { Text("Type a message...", fontSize = 14.sp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(customFocusRequester),
                                            shape = RoundedCornerShape(16.dp),
                                            minLines = 2,
                                            maxLines = 4,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Send
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSend = {
                                                    if (customMessageText.isNotBlank()) {
                                                        performAppHaptic(context, "light")
                                                        selectedMessageForAppChoice = customMessageText.trim()
                                                        isCustomTyping = false
                                                    }
                                                }
                                            ),
                                            trailingIcon = {
                                                if (customMessageText.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { customMessageText = "" },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Clear,
                                                            contentDescription = "Clear",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    selectedMessageForAppChoice = customMessageText.trim()
                                                    isCustomTyping = false
                                                },
                                                enabled = customMessageText.isNotBlank(),
                                                shape = RoundedCornerShape(14.dp),
                                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Send", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                                "reply" -> {
                                    val chosenMsg = selectedMessageForAppChoice
                                    // ── App Chooser View (non-scrollable, all in one place) ──
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    selectedMessageForAppChoice = null
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Send response via",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (!chosenMsg.isNullOrBlank() && chosenMsg != "Type custom...") "\"${formatQuickReplyDisplay(chosenMsg)}\"" else "Custom message",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Available messaging apps from below (non-scrollable)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Messages (SMS)
                                            ActionButtonItem(
                                                iconVector = Icons.Outlined.Chat,
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                label = "Messages",
                                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                onClick = {
                                                    sendViaSms(chosenMsg)
                                                }
                                            )

                                            // 2. WhatsApp (if installed)
                                            if (whatsAppInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = whatsAppIcon,
                                                    iconVector = Icons.Default.Chat,
                                                    label = "WhatsApp",
                                                    onClick = {
                                                        sendViaWhatsApp(chosenMsg)
                                                    }
                                                )
                                            }

                                            // 3. Telegram (if installed)
                                            if (telegramInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = telegramIcon,
                                                    iconVector = Icons.Default.Send,
                                                    label = "Telegram",
                                                    onClick = {
                                                        sendViaTelegram(chosenMsg)
                                                    }
                                                )
                                            }

                                            // 4. Truecaller (if installed)
                                            if (truecallerInstalled) {
                                                ActionButtonItem(
                                                    iconBitmap = truecallerIcon,
                                                    iconVector = Icons.Default.Search,
                                                    label = "Truecaller",
                                                    onClick = {
                                                        performAppHaptic(context, "light")
                                                        openTruecaller(context, phoneNumber)
                                                        triggerDismiss()
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                                "social" -> {
                                    val app = selectedSocialApp
                                    val appLabel = when (app) {
                                        "whatsapp" -> "WhatsApp"
                                        "telegram" -> "Telegram"
                                        else -> "Google Meet"
                                    }
                                    val appIcon = when (app) {
                                        "whatsapp" -> whatsAppIcon
                                        "telegram" -> telegramIcon
                                        else -> meetIcon
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    selectedSocialApp = null
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            if (appIcon != null) {
                                                Image(
                                                    bitmap = appIcon,
                                                    contentDescription = appLabel,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = appLabel,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = contactName.ifBlank { phoneNumber },
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (app != "googlemeet") {
                                                SocialActionOptionRow(
                                                    icon = Icons.AutoMirrored.Filled.Chat,
                                                    title = "Chat",
                                                    onClick = {
                                                        performAppHaptic(context, "light")
                                                        if (app == "whatsapp") {
                                                            openWhatsAppChat(context, phoneNumber)
                                                        } else {
                                                            openTelegramChat(context, phoneNumber)
                                                        }
                                                        triggerDismiss()
                                                    }
                                                )
                                            }

                                            SocialActionOptionRow(
                                                icon = Icons.Default.Call,
                                                title = "Voice Call",
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    val started = when (app) {
                                                        "whatsapp" -> startWhatsAppVoiceCall(context, phoneNumber)
                                                        "telegram" -> startTelegramVoiceCall(context, phoneNumber)
                                                        else -> startGoogleMeetVoiceCall(context, phoneNumber)
                                                    }
                                                    if (!started) {
                                                        android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    triggerDismiss()
                                                }
                                            )

                                            SocialActionOptionRow(
                                                icon = Icons.Default.Videocam,
                                                title = "Video Call",
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    val started = when (app) {
                                                        "whatsapp" -> startWhatsAppVideoCall(context, phoneNumber)
                                                        "telegram" -> startTelegramVideoCall(context, phoneNumber)
                                                        else -> startGoogleMeetVideoCall(context, phoneNumber)
                                                    }
                                                    if (!started) {
                                                        android.widget.Toast.makeText(context, "$appLabel isn't installed", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    triggerDismiss()
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                                "sim" -> {
                                    val accounts = selectedSimAccounts.orEmpty()
                                    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    performAppHaptic(context, "light")
                                                    selectedSimAccounts = null
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Select SIM Card",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            accounts.forEachIndexed { index, accountHandle ->
                                                val info = try { telecomManager?.getPhoneAccount(accountHandle) } catch (_: Exception) { null }
                                                val simLabel = info?.label?.toString() ?: "SIM ${index + 1}"
                                                val simDesc = info?.shortDescription?.toString()

                                                Surface(
                                                    onClick = {
                                                        performAppHaptic(context, "light")
                                                        makeCall(context, phoneNumber, accountHandle)
                                                        triggerDismiss()
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            modifier = Modifier.size(36.dp),
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.primaryContainer
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.SimCard,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = simLabel,
                                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 14.sp
                                                                ),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (!simDesc.isNullOrBlank()) {
                                                                Text(
                                                                    text = simDesc,
                                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun SocialActionOptionRow(
        icon: ImageVector,
        title: String,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @Composable
    private fun ActionButtonItem(
        iconVector: ImageVector? = null,
        iconBitmap: ImageBitmap? = null,
        containerColor: Color? = null,
        label: String,
        iconTint: Color = MaterialTheme.colorScheme.onSurface,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .then(
                        if (containerColor != null) Modifier.background(containerColor, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = label,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
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

    private fun formatQuickReplyDisplay(text: String): String {
        if (text == "Type custom...") return text
        val words = text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return if (words.size > 3) {
            "${words.take(3).joinToString(" ")}..."
        } else {
            text
        }
    }
}
