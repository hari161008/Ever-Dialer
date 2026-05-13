package com.coolappstore.everdialer.by.svhp.view.screen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.coolappstore.everdialer.by.svhp.controller.CallService
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.math.roundToInt
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.compose.ui.util.lerp

class CallActivity : ComponentActivity() {

    private val contactsRepo: IContactsRepository by inject()
    private val callLogRepo: ICallLogRepository by inject()
    private val prefs: PreferenceManager by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null

    // Pocket mode prevention
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var isPocketBlocked = false
    private val proxSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) return
            val maxRange = event.sensor.maximumRange
            isPocketBlocked = event.values[0] < maxRange * 0.1f
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setupProximitySensor()
        // Prevent notification shade from being pulled down during a call
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        enableEdgeToEdge()
        // Register pocket mode proximity listener
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximitySensor?.let {
            sensorManager?.registerListener(proxSensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val heldSession by CallService.heldCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()
                val settingsVersion by prefs.settingsChanged.collectAsState()

                val call = session?.call
                val callState = session?.state

                val proximityBgEnabled = remember(settingsVersion) {
                    prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)
                }
                val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER

                LaunchedEffect(callState, isSpeakerOn, proximityBgEnabled) {
                    when (callState) {
                        Call.STATE_ACTIVE, Call.STATE_DIALING -> {
                            if (proximityBgEnabled && !isSpeakerOn) {
                                acquireProximityLock()
                            } else {
                                releaseProximityLock()
                            }
                        }
                        else -> releaseProximityLock()
                    }
                    if (session == null || callState == Call.STATE_DISCONNECTED) {
                        delay(800)
                        finishAndRemoveTask()
                    }
                }

                if (call != null && session != null) {
                    val number = call.details?.handle?.schemeSpecificPart ?: ""
                    // Stable initial values — number shown immediately, replaced by
                    // contact name in-place once async lookup completes (no layout shift
                    // because the composable tree is already present and sized).
                    // Start empty so the layout is stable from the first frame.
                    // contactName is filled by the async lookup; until then we
                    // show the number as a subtitle-style fallback (see the status
                    // text below the name), so there is no visible content gap.
                    var contactName by remember { mutableStateOf("") }
                    var photoUri by remember { mutableStateOf<String?>(null) }

                    val heldCall = heldSession?.call
                    val heldNumber = heldCall?.details?.handle?.schemeSpecificPart ?: ""
                    var heldContactName by remember(heldNumber) { mutableStateOf(heldNumber.ifEmpty { "Unknown" }) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            val contact = contactsRepo.getContactByNumber(number)
                            if (contact != null) {
                                contactName = contact.name
                                photoUri = contact.photoUri
                            } else {
                                contactName = number
                            }
                        } else {
                            contactName = "Unknown"
                        }
                    }

                    LaunchedEffect(heldNumber) {
                        if (heldNumber.isNotEmpty()) {
                            contactsRepo.getContactByNumber(heldNumber)?.let {
                                heldContactName = it.name
                            }
                        }
                    }

                    val answeredFromNotification = intent?.getBooleanExtra("ANSWERED_FROM_NOTIFICATION", false) ?: false
                    ExpressiveCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_ACTIVE,
                        contactName = contactName,
                        phoneNumber = number,
                        photoUri = photoUri,
                        audioState = audioState,
                        hasHeldCall = heldSession != null && heldSession?.state != Call.STATE_DISCONNECTED && heldSession?.state != Call.STATE_DISCONNECTING,
                        heldCallName = heldContactName,
                        contactsRepo = contactsRepo,
                        callLogRepo = callLogRepo,
                        prefs = prefs,
                        isPocketBlocked = { isPocketBlocked },
                        skipIncomingScreen = answeredFromNotification
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val session = CallService.currentCallSession.value
        val audioState = CallService.audioState.value
        val proximityBgEnabled = prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_BG, true)
        val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
        val callState = session?.state
        if (proximityBgEnabled && !isSpeakerOn &&
            (callState == Call.STATE_ACTIVE || callState == Call.STATE_DIALING)) {
            acquireProximityLock()
        } else if (!proximityBgEnabled || isSpeakerOn) {
            releaseProximityLock()
        }
    }

    private fun setupProximitySensor() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Rivo::Prox")
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.requestDismissKeyguard(this, null)
    }

    override fun onDestroy() { super.onDestroy(); releaseProximityLock(); sensorManager?.unregisterListener(proxSensorListener) }
    private fun acquireProximityLock() { if (proximityWakeLock?.isHeld == false) proximityWakeLock?.acquire() }
    private fun releaseProximityLock() { if (proximityWakeLock?.isHeld == true) proximityWakeLock?.release() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    phoneNumber: String = "",
    photoUri: String?,
    audioState: CallAudioState?,
    hasHeldCall: Boolean = false,
    heldCallName: String = "",
    contactsRepo: IContactsRepository? = null,
    callLogRepo: ICallLogRepository? = null,
    prefs: PreferenceManager? = null,
    isPocketBlocked: () -> Boolean = { false },
    skipIncomingScreen: Boolean = false
) {
    val context = LocalView.current.context
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
    val isBluetoothActive = audioState?.route == CallAudioState.ROUTE_BLUETOOTH

    // Bluetooth availability detection
    var isBluetoothConnected by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        // Initial check via supported routes in audioState or via BluetoothProfile proxy
        fun checkBtConnected(): Boolean {
            val supportedMask = audioState?.supportedRouteMask ?: 0
            return (supportedMask and CallAudioState.ROUTE_BLUETOOTH) != 0
        }
        isBluetoothConnected = checkBtConnected()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                when (intent.action) {
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                        isBluetoothConnected = state == BluetoothProfile.STATE_CONNECTED
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                        if (state == BluetoothAdapter.STATE_OFF) isBluetoothConnected = false
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Also update bluetooth connected from audioState changes
    LaunchedEffect(audioState) {
        val supportedMask = audioState?.supportedRouteMask ?: 0
        if ((supportedMask and CallAudioState.ROUTE_BLUETOOTH) != 0) {
            isBluetoothConnected = true
        }
    }
    // When answered from notification, skip the incoming screen by treating
    // a brief STATE_RINGING flash as already-active for UI purposes
    val effectiveCallState = if (skipIncomingScreen && callState == Call.STATE_RINGING) Call.STATE_ACTIVE else callState
    var isOnHold by remember { mutableStateOf(false) }
    var showNoteWindow by remember { mutableStateOf(false) }
    var showMergeConfirm by remember { mutableStateOf(false) }
    var showAddPersonSheet by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var dtmfInput by remember { mutableStateOf("") }

    // Auto-dismiss merge confirm dialog if the held call disappears (3rd person hung up)
    LaunchedEffect(hasHeldCall) {
        if (!hasHeldCall) {
            showMergeConfirm = false
            showAddPersonSheet = false
            if (callState == Call.STATE_ACTIVE) {
                isOnHold = false
            }
        }
    }

    var callDuration by remember { mutableLongStateOf(0L) }
    val isDark = isSystemInDarkTheme()

    // Hangup button width from prefs (0.1f .. 1.0f)
    val settingsVersion by (prefs?.settingsChanged ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
    val hangupWidthFraction = remember(settingsVersion) {
        prefs?.getFloat(PreferenceManager.KEY_HANGUP_WIDTH, 0.5f) ?: 0.5f
    }
    var noteText by remember { mutableStateOf("") }

    LaunchedEffect(phoneNumber) {
        if (phoneNumber.isNotEmpty() && noteText.isBlank()) {
            val existing = NoteManager.readNoteByPhone(context, phoneNumber)
            if (existing.isNotBlank()) noteText = existing
        }
    }

    LaunchedEffect(contactName) {
        if (phoneNumber.isNotEmpty() && noteText.isBlank()) {
            val existing = NoteManager.readNote(context, contactName, phoneNumber)
            if (existing.isNotBlank()) noteText = existing
        }
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(noteText) {
        if (phoneNumber.isNotEmpty() && noteText.isNotBlank()) {
            NoteManager.writeNote(context, contactName, phoneNumber, noteText)
        }
    }

    LaunchedEffect(callState) {
        if ((callState == Call.STATE_DISCONNECTED || callState == Call.STATE_DISCONNECTING) && noteText.isNotBlank() && phoneNumber.isNotEmpty()) {
            NoteManager.writeNote(context, contactName, phoneNumber, noteText)
        }
    }

    var isDisconnecting by remember { mutableStateOf(false) }
    val disconnectOffset by animateDpAsState(
        if (isDisconnecting) 120.dp else 0.dp,
        tween(600),
        label = "disconnectSlide"
    )
    val disconnectAlpha by animateFloatAsState(
        if (isDisconnecting) 0f else 1f,
        tween(600),
        label = "disconnectAlpha"
    )

    var wasRinging by remember { mutableStateOf(callState == Call.STATE_RINGING) }
    var screenEntered by remember { mutableStateOf(true) }

    // Smooth answer transition: when ringing → active, gently scale + fade the UI in.
    var callAnswered by remember { mutableStateOf(false) }
    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE && wasRinging && !callAnswered) {
            callAnswered = true
        }
    }
    val answerProgress by animateFloatAsState(
        targetValue = if (wasRinging && !callAnswered) 0f else 1f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "answerProgress"
    )
    val acceptScale = if (wasRinging) lerp(0.96f, 1f, answerProgress) else 1f
    val acceptAlpha = if (wasRinging) lerp(0.7f, 1f, answerProgress) else 1f

    LaunchedEffect(callState) {
        if (callState == Call.STATE_DISCONNECTED || callState == Call.STATE_DISCONNECTING) isDisconnecting = true
        if (callState == Call.STATE_RINGING) wasRinging = true
        // If call returns to active from holding (e.g. held call restored), sync isOnHold
        if (callState == Call.STATE_ACTIVE && isOnHold) isOnHold = false
    }

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            val start = System.currentTimeMillis()
            while (true) { callDuration = (System.currentTimeMillis() - start) / 1000; delay(1000) }
        }
    }

    val bgColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onSurface
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val overlayColor = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f)
    val controlBtnColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f)
    val controlBtnActiveColor = if (isDark) Color.White else Color.Black
    val controlBtnActiveFg = if (isDark) Color.Black else Color.White
    val controlBtnFg = onBgColor

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val driftX by infiniteTransition.animateFloat(-35f, 35f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "x")
    val driftY by infiniteTransition.animateFloat(-25f, 25f, infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "y")

    if (showMergeConfirm) {
        AlertDialog(
            onDismissRequest = { showMergeConfirm = false },
            icon = { Icon(Icons.Default.CallMerge, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Merge Calls") },
            text = {
                Text(
                    "This will merge your current call with ${heldCallName.ifBlank { "the held call" }} into a conference call.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    showMergeConfirm = false
                    CallService.mergeCalls()
                }) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = { showMergeConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddPersonSheet) {
        AddPersonSheet(
            context = context,
            contactsRepo = contactsRepo,
            callLogRepo = callLogRepo,
            onDismiss = { showAddPersonSheet = false },
            onPersonSelected = { number ->
                showAddPersonSheet = false
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    // Hold the current call and reflect that in UI
                    try {
                        call.hold()
                        isOnHold = true
                    } catch (_: Exception) {}
                    // Signal CallService to auto-merge once the 3rd call is answered
                    CallService.isAddingToCall = true
                    delay(500)
                    try {
                        val appContext = context.applicationContext
                        val telecomManager = appContext.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                        val uri = Uri.fromParts("tel", number, null)
                        if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
                            androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.CALL_PHONE)) {
                            telecomManager.placeCall(uri, android.os.Bundle())
                        } else {
                            CallService.isAddingToCall = false
                            isOnHold = false
                            try { call.unhold() } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {
                        CallService.isAddingToCall = false
                        isOnHold = false
                        try { call.unhold() } catch (_: Exception) {}
                    }
                }
            }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val dialpadOffsetY by animateFloatAsState(
        targetValue = if (showDialpad) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "dialpadSlide"
    )
    val dialpadAlpha by animateFloatAsState(
        targetValue = if (showDialpad) 1f else 0f,
        animationSpec = tween(220),
        label = "dialpadAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = disconnectOffset)
                .alpha(disconnectAlpha)
        ) {
            // Blurred background photo
            if (!photoUri.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = driftX; translationY = driftY; scaleX = 1.4f; scaleY = 1.4f }) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(80.dp).alpha(if (isDark) 0.35f else 0.2f), contentScale = ContentScale.Crop)
                }
            }

            if (isLandscape) {
                // ── LANDSCAPE: two-panel layout ─────────────────────────────
                val lsStatusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
                val lsNavBarHeight = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
                val frozenLsTop = remember { lsStatusBarHeight }
                val frozenLsBottom = remember { lsNavBarHeight }
                Row(
                    modifier = Modifier.fillMaxSize()
                        .padding(top = frozenLsTop, bottom = frozenLsBottom)
                        .scale(acceptScale).alpha(acceptAlpha)
                ) {
                    // Left panel: avatar + caller info
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(controlBtnColor)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(48.dp), tint = subtleColor)
                            if (!photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(photoUri).crossfade(300).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = contactName.ifEmpty { "" },
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                                color = onBgColor.copy(alpha = if (contactName.isEmpty()) 0f else 1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Text(
                            text = when {
                                isOnHold -> "On Hold"
                                callState == Call.STATE_ACTIVE -> formatDuration(callDuration)
                                callState == Call.STATE_DIALING -> "Calling"
                                callState == Call.STATE_RINGING -> "Incoming"
                                callState == Call.STATE_CONNECTING -> "Calling"
                                callState == Call.STATE_DISCONNECTING || isDisconnecting -> "Hanging up..."
                                else -> "Connecting..."
                            },
                            color = if (isOnHold) Color(0xFFFFB74D) else subtleColor,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        if (hasHeldCall) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CallMerge, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                    Text(text = if (heldCallName.isBlank()) "1 call on hold" else "$heldCallName on hold", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }

                    // Right panel: controls
                    if (effectiveCallState != Call.STATE_RINGING) {
                        Surface(modifier = Modifier.weight(1f).fillMaxHeight(), color = overlayColor) {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    AnimatedCallButton(icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause, label = "Hold", isActive = isOnHold, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) {
                                        isOnHold = !isOnHold
                                        if (isOnHold) call.hold() else call.unhold()
                                    }
                                    if (hasHeldCall) {
                                        AnimatedCallButton(icon = Icons.Default.CallMerge, label = "Merge", isActive = true, btnColor = controlBtnColor, activeBtnColor = Color(0xFF4CAF50), fgColor = controlBtnFg, activeFgColor = Color.White, onClick = { showMergeConfirm = true })
                                    } else {
                                        AnimatedCallButton(icon = Icons.Default.PersonAdd, label = "Add", isActive = false, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg, onClick = { showAddPersonSheet = true })
                                    }
                                    AnimatedCallButton(icon = Icons.Default.Dialpad, label = "Dialpad", isActive = showDialpad, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) { showDialpad = !showDialpad }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    AnimatedCallButton(icon = Icons.Default.EditNote, label = "Note", isActive = showNoteWindow, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) { showNoteWindow = !showNoteWindow }
                                    AnimatedCallButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, label = "Mute", isActive = isMuted, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) { CallService.setMuted(!isMuted) }
                                    AnimatedCallButton(icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown, label = "Speaker", isActive = isSpeakerOn, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) {
                                        CallService.setAudioRoute(if (isSpeakerOn) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER)
                                    }
                                    if (isBluetoothConnected) {
                                        AnimatedCallButton(
                                            icon = if (isBluetoothActive) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                            label = "Bluetooth",
                                            isActive = isBluetoothActive,
                                            btnColor = controlBtnColor,
                                            activeBtnColor = controlBtnActiveColor,
                                            fgColor = controlBtnFg,
                                            activeFgColor = controlBtnActiveFg
                                        ) {
                                            if (isBluetoothActive) CallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                                            else CallService.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                                        }
                                    }
                                }
                                AnimatedVisibility(visible = showNoteWindow, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Note — $contactName", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { if (phoneNumber.isNotEmpty()) NoteManager.writeNote(context, contactName, phoneNumber, noteText); showNoteWindow = false }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(value = noteText, onValueChange = { noteText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 140.dp), placeholder = { Text("Type your note...") }, shape = RoundedCornerShape(12.dp), minLines = 3, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                val endInteraction2 = remember { MutableInteractionSource() }
                                val endPressed2 by endInteraction2.collectIsPressedAsState()
                                val endRadius2 by animateDpAsState(if (endPressed2) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium), label = "endRadius2")
                                Surface(
                                    onClick = { if (noteText.isNotBlank() && phoneNumber.isNotEmpty()) NoteManager.writeNote(context, contactName, phoneNumber, noteText); try { call.disconnect() } catch (_: Exception) {} },
                                    modifier = Modifier.fillMaxWidth(0.8f).height(64.dp).scale(if (endPressed2) 0.96f else 1f),
                                    shape = RoundedCornerShape(endRadius2), color = Color(0xFFD32F2F), interactionSource = endInteraction2
                                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp)) } }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            NewSwipeToAnswer(
                                onAnswer = { if (!isPocketBlocked()) try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {} },
                                onDecline = { if (!isPocketBlocked()) try { call.disconnect() } catch (_: Exception) {} },
                                labelColor = subtleColor,
                                bgColor = overlayColor,
                                isPocketBlocked = isPocketBlocked
                            )
                        }
                    }
                }
            } else {
                // ── PORTRAIT: original layout ────────────────────────────────
                // Snapshot inset sizes once — never reread them — so window-flag
                // changes during the call (lock-screen → active) can't shift the layout.
                val statusBarHeight = with(LocalDensity.current) {
                    WindowInsets.statusBars.getTop(this).toDp()
                }
                val navBarHeight = with(LocalDensity.current) {
                    WindowInsets.navigationBars.getBottom(this).toDp()
                }
                val frozenStatusBarHeight = remember { statusBarHeight }
                val frozenNavBarHeight = remember { navBarHeight }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = frozenStatusBarHeight, bottom = frozenNavBarHeight)
                        .scale(acceptScale)
                        .alpha(acceptAlpha)
                ) {
                    // ── Top: caller info — absolutely top-anchored, never affected by bottom content ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .align(Alignment.TopCenter)
                            .padding(top = 130.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(controlBtnColor)) {
                            // Always render Icon as base layer so layout never shifts
                            Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(56.dp), tint = subtleColor)
                            if (!photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photoUri)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                        // Fixed height box so layout never shifts when name loads
                        Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = contactName.ifEmpty { "" },
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Medium),
                                color = onBgColor.copy(alpha = if (contactName.isEmpty()) 0f else 1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = when {
                                isOnHold -> "On Hold"
                                callState == Call.STATE_ACTIVE -> formatDuration(callDuration)
                                callState == Call.STATE_DIALING -> "Calling"
                                callState == Call.STATE_RINGING -> "Incoming"
                                callState == Call.STATE_CONNECTING -> "Calling"
                                callState == Call.STATE_DISCONNECTING || isDisconnecting -> "Hanging up..."
                                else -> "Connecting..."
                            },
                            color = if (isOnHold) Color(0xFFFFB74D) else subtleColor,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        if (hasHeldCall) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CallMerge, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (heldCallName.isBlank()) "1 call on hold" else "$heldCallName on hold",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }

                    // ── Bottom: controls — anchored to bottom ─────────────────
                    if (effectiveCallState != Call.STATE_RINGING) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)),
                            color = overlayColor
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                // Top row: Hold, Add Person, Dialpad
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    AnimatedCallButton(icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause, label = "Hold", isActive = isOnHold, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) {
                                        isOnHold = !isOnHold
                                        if (isOnHold) call.hold() else call.unhold()
                                    }

                                    if (hasHeldCall) {
                                        AnimatedCallButton(
                                            icon = Icons.Default.CallMerge,
                                            label = "Merge",
                                            isActive = true,
                                            btnColor = controlBtnColor,
                                            activeBtnColor = Color(0xFF4CAF50),
                                            fgColor = controlBtnFg,
                                            activeFgColor = Color.White,
                                            onClick = { showMergeConfirm = true }
                                        )
                                    } else {
                                        AnimatedCallButton(
                                            icon = Icons.Default.PersonAdd,
                                            label = "Add Person",
                                            isActive = false,
                                            btnColor = controlBtnColor,
                                            activeBtnColor = controlBtnActiveColor,
                                            fgColor = controlBtnFg,
                                            activeFgColor = controlBtnActiveFg,
                                            onClick = { showAddPersonSheet = true }
                                        )
                                    }

                                    AnimatedCallButton(
                                        icon = Icons.Default.Dialpad,
                                        label = "Dialpad",
                                        isActive = showDialpad,
                                        btnColor = controlBtnColor,
                                        activeBtnColor = controlBtnActiveColor,
                                        fgColor = controlBtnFg,
                                        activeFgColor = controlBtnActiveFg
                                    ) { showDialpad = !showDialpad }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Bottom row: Note, Mute, Speaker
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    AnimatedCallButton(
                                        icon = Icons.Default.EditNote,
                                        label = "Note",
                                        isActive = showNoteWindow,
                                        btnColor = controlBtnColor,
                                        activeBtnColor = controlBtnActiveColor,
                                        fgColor = controlBtnFg,
                                        activeFgColor = controlBtnActiveFg
                                    ) { showNoteWindow = !showNoteWindow }
                                    AnimatedCallButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, label = "Mute", isActive = isMuted, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) { CallService.setMuted(!isMuted) }
                                    AnimatedCallButton(icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown, label = "Speaker", isActive = isSpeakerOn, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) {
                                        CallService.setAudioRoute(if (isSpeakerOn) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER)
                                    }
                                }

                                // Bluetooth row — only visible when a BT device is connected
                                AnimatedVisibility(
                                    visible = isBluetoothConnected,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        AnimatedCallButton(
                                            icon = if (isBluetoothActive) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                            label = "Bluetooth",
                                            isActive = isBluetoothActive,
                                            btnColor = controlBtnColor,
                                            activeBtnColor = controlBtnActiveColor,
                                            fgColor = controlBtnFg,
                                            activeFgColor = controlBtnActiveFg
                                        ) {
                                            if (isBluetoothActive) {
                                                CallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                                            } else {
                                                CallService.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = showNoteWindow, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Note — $contactName",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row {
                                                        IconButton(onClick = {
                                                            if (phoneNumber.isNotEmpty()) {
                                                                NoteManager.writeNote(context, contactName, phoneNumber, noteText)
                                                            }
                                                            showNoteWindow = false
                                                        }, modifier = Modifier.size(32.dp)) {
                                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                OutlinedTextField(
                                                    value = noteText,
                                                    onValueChange = { noteText = it },
                                                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                                                    placeholder = { Text("Type your note...") },
                                                    shape = RoundedCornerShape(12.dp),
                                                    minLines = 4,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                    )
                                                )
                                                if (noteText.isNotBlank()) {
                                                    Text(
                                                        "Syncing...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                // ── Hangup Button with configurable width ──────────────
                                val endInteraction = remember { MutableInteractionSource() }
                                val endPressed by endInteraction.collectIsPressedAsState()
                                val endRadius by animateDpAsState(if (endPressed) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium), label = "endRadius")

                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    val isCircleHangup = hangupWidthFraction <= 0.1f
                                    Surface(
                                        onClick = {
                                            if (noteText.isNotBlank() && phoneNumber.isNotEmpty()) {
                                                NoteManager.writeNote(context, contactName, phoneNumber, noteText)
                                            }
                                            try { call.disconnect() } catch (e: Exception) {}
                                        },
                                        modifier = if (isCircleHangup) Modifier
                                            .size(76.dp)
                                            .scale(if (endPressed) 0.96f else 1f)
                                        else Modifier
                                            .fillMaxWidth(hangupWidthFraction.coerceIn(0.1f, 1.0f))
                                            .height(76.dp)
                                            .scale(if (endPressed) 0.96f else 1f),
                                        shape = if (isCircleHangup) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(endRadius),
                                        color = Color(0xFFD32F2F),
                                        interactionSource = endInteraction
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Ringing state — swipe to answer, also anchored to bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            NewSwipeToAnswer(
                                onAnswer = { if (!isPocketBlocked()) try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {} },
                                onDecline = { if (!isPocketBlocked()) try { call.disconnect() } catch (_: Exception) {} },
                                labelColor = subtleColor,
                                bgColor = overlayColor,
                                isPocketBlocked = isPocketBlocked
                            )
                        }
                    }
                } // end portrait Box
            } // end portrait
        }

        // ── Dialpad overlay — last child of main Box, never triggers layout shift ──
        if (showDialpad || dialpadAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dialpadAlpha }
                    .background(Color.Black.copy(alpha = 0.55f * dialpadAlpha)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = dialpadOffsetY * size.height }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                            Surface(shape = RoundedCornerShape(3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(width = 36.dp, height = 4.dp)) {}
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dialpad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showDialpad = false }) { Icon(Icons.Default.Close, null) }
                        }
                        if (dtmfInput.isNotEmpty()) {
                            Text(
                                text = dtmfInput,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        InCallDialPad(
                            onDigit = { digit ->
                                dtmfInput += digit
                                try { call.playDtmfTone(digit[0]); call.stopDtmfTone() } catch (_: Exception) {}
                            },
                            onBackspace = { if (dtmfInput.isNotEmpty()) dtmfInput = dtmfInput.dropLast(1) }
                        )
                    }
                }
            }
        }
    }
}

// ─── In-Call Dial Pad ──────────────────────────────────────────────────────────

@Composable
private fun InCallDialPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEach { (digit, letters) ->
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val keyRadius by animateDpAsState(if (isPressed) 14.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "keyR")
                    Surface(
                        onClick = { onDigit(digit) },
                        shape = RoundedCornerShape(keyRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.weight(1f).height(58.dp).scale(if (isPressed) 0.92f else 1f),
                        interactionSource = interaction
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(digit, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                            if (letters.isNotEmpty()) {
                                Text(letters, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // Backspace row
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
            Surface(
                onClick = onBackspace,
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(0.5f).height(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Backspace, null, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Add Person Bottom Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPersonSheet(
    context: android.content.Context,
    contactsRepo: IContactsRepository?,
    callLogRepo: ICallLogRepository?,
    onDismiss: () -> Unit,
    onPersonSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    var dialNumber by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            contacts = contactsRepo?.getContacts() ?: emptyList()
            callLogs = callLogRepo?.getCallLogs()?.distinctBy { it.number } ?: emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(width = 36.dp, height = 4.dp)) {}
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Add Person", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            if (selectedTab != 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(if (selectedTab == 0) "Search call logs..." else "Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("Call Logs" to Icons.Default.History, "Contacts" to Icons.Default.Person, "Dial Pad" to Icons.Default.Dialpad)
                tabs.forEachIndexed { index, (label, icon) ->
                    val selected = selectedTab == index
                    val tabColor by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        spring(stiffness = Spring.StiffnessMediumLow), label = "tabColor"
                    )
                    Surface(
                        onClick = { selectedTab = index; searchQuery = "" },
                        shape = RoundedCornerShape(50.dp),
                        color = tabColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, modifier = Modifier.size(16.dp),
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, fontSize = 11.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)) {
                when (selectedTab) {
                    0 -> {
                        val filtered = remember(callLogs, searchQuery) {
                            if (searchQuery.isBlank()) callLogs.take(50)
                            else callLogs.filter {
                                val name = it.name ?: ""
                                name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery)
                            }.take(50)
                        }
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.number }) { log ->
                                AddPersonRow(
                                    name = log.name?.takeIf { it != log.number } ?: log.number,
                                    subtitle = if (log.name != null && log.name != log.number) log.number else null,
                                    photoUri = log.photoUri,
                                    onClick = { onPersonSelected(log.number) }
                                )
                            }
                        }
                    }
                    1 -> {
                        val filtered = remember(contacts, searchQuery) {
                            if (searchQuery.isBlank()) contacts.take(100)
                            else contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumbers.any { n -> n.contains(searchQuery) } }.take(100)
                        }
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.id }) { contact ->
                                AddPersonRow(
                                    name = contact.name,
                                    subtitle = contact.phoneNumbers.firstOrNull(),
                                    photoUri = contact.photoUri,
                                    onClick = { contact.phoneNumbers.firstOrNull()?.let { onPersonSelected(it) } }
                                )
                            }
                        }
                    }
                    2 -> {
                        CompactDialPad(
                            number = dialNumber,
                            onNumberChange = { dialNumber = it },
                            onCall = { if (dialNumber.isNotEmpty()) onPersonSelected(dialNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPersonRow(
    name: String,
    subtitle: String?,
    photoUri: String?,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "rowScale")

    Surface(
        onClick = { isPressed = false; onClick() },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().scale(scale)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RivoAvatar(name = name, photoUri = photoUri, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CompactDialPad(
    number: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit
) {
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = number.ifEmpty { "Enter number" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (digit, letters) ->
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val keyRadius by animateDpAsState(if (isPressed) 14.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "keyR")
                    Surface(
                        onClick = { onNumberChange(number + digit) },
                        shape = RoundedCornerShape(keyRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.weight(1f).height(52.dp).scale(if (isPressed) 0.92f else 1f),
                        interactionSource = interaction
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(digit, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                            if (letters.isNotEmpty()) {
                                Text(letters, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = { if (number.isNotEmpty()) onNumberChange(number.dropLast(1)) },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Backspace, null, modifier = Modifier.size(22.dp))
                }
            }
            Surface(
                onClick = onCall,
                shape = RoundedCornerShape(22.dp),
                color = if (number.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.weight(2f).height(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Call, null, tint = if (number.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ─── Animated Call Button ───────────────────────────────────────────────────────

@Composable
fun AnimatedCallButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    btnColor: Color = Color.White.copy(0.12f),
    activeBtnColor: Color = Color.White,
    fgColor: Color = Color.White,
    activeFgColor: Color = Color.Black,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val radius by animateDpAsState(if (isPressed) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium), label = "btnRadius")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, modifier = Modifier.size(68.dp).scale(if (isPressed) 0.9f else 1f), shape = RoundedCornerShape(radius), color = if (isActive) activeBtnColor else btnColor, interactionSource = interaction) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (isActive) activeFgColor else fgColor, modifier = Modifier.size(26.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
            color = fgColor.copy(0.7f),
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NewSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit, labelColor: Color = Color.White.copy(0.6f), bgColor: Color = Color.White.copy(0.08f), isPocketBlocked: () -> Boolean = { false }) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val handleColor = if (isDark) Color.White else Color.Black.copy(0.85f)
    val handleFg = if (isDark) Color.Black else Color.White

    // pillWidth tracks the actual measured pill width so we can compute real edge travel
    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    val handleSizePx = with(density) { 72.dp.toPx() }
    // maxDrag = half of (pillWidth - handleSize) → handle reaches the pill edge
    val maxDrag = ((pillWidthPx - handleSizePx) / 2f).coerceAtLeast(with(density) { 120.dp.toPx() })

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(onClick = {}, shape = CircleShape, color = bgColor, modifier = Modifier.height(45.dp).width(140.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = labelColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", color = labelColor, style = MaterialTheme.typography.labelLarge)
            }
        }
        Box(
            modifier = Modifier
                .height(90.dp)
                .fillMaxWidth(0.85f)
                .clip(CircleShape)
                .background(bgColor)
                .onSizeChanged { pillWidthPx = it.width.toFloat() },
            contentAlignment = Alignment.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Decline", color = labelColor, style = MaterialTheme.typography.bodyLarge)
                Text("Answer", color = labelColor, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(handleColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        offsetX.value >= maxDrag * 0.90f -> onAnswer()
                                        offsetX.value <= -maxDrag * 0.90f -> onDecline()
                                        else -> offsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (!isPocketBlocked()) {
                                    change.consume()
                                    coroutineScope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)) }
                                } else {
                                    // In pocket: snap back to center and consume event to prevent accidental action
                                    change.consume()
                                    coroutineScope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.8f)) }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = when { offsetX.value > maxDrag * 0.5f -> Color(0xFF4CAF50); offsetX.value < -maxDrag * 0.5f -> Color(0xFFF44336); else -> handleFg },
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
