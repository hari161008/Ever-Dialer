package com.coolappstore.everdialer.by.svhp.view.screen

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.VideoProfile
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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
import com.coolappstore.everdialer.by.svhp.controller.util.CallButtonPrefs
import com.coolappstore.everdialer.by.svhp.controller.util.NoteManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.SimSlotBadge
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService
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

class CallActivity : FragmentActivity() {

    private val contactsRepo: IContactsRepository by inject()
    private val callLogRepo: ICallLogRepository by inject()
    private val prefs: PreferenceManager by inject()

    companion object {
        /** FloatingCallService observes this to hide the bubble when CallActivity is visible. */
        val isInForeground = kotlinx.coroutines.flow.MutableStateFlow(false)
        /** Keep the activity alive while an auto-redial dialog or job is pending. */
        val keepAliveForRedial = kotlinx.coroutines.flow.MutableStateFlow(false)
    }

    // Pocket mode prevention
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var isPocketBlocked = false
    // Auto-speaker proximity tracking
    private var autoSpeakerActive = false

    // Note: the real near-ear screen-off (both the plain "Proximity Sensor" mode and the
    // "Device Orientation with Proximity Sensor" mode) is now owned entirely by CallService,
    // which runs continuously as a foreground service for the whole call regardless of which
    // Activity is on top — see CallService.updateProximityScreenOffGate(). It used to live here,
    // but the PROXIMITY_SCREEN_OFF_WAKE_LOCK it drives only reliably takes effect while the
    // acquiring app has a foregrounded window, so it would silently stop working the moment the
    // user switched to the main app during a call, and only "fix itself" once this call screen
    // was brought back on top. CallActivity no longer needs to track any of that state.

    private val proxSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    val maxRange = event.sensor.maximumRange
                    val isNear = event.values[0] < maxRange * 0.5f

                    // Pocket mode prevention
                    if (prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE_PREVENTION, false)) {
                        isPocketBlocked = isNear
                    }

                    // Auto speaker: near -> earpiece, far -> speaker
                    if (prefs.getBoolean(PreferenceManager.KEY_AUTO_SPEAKER, false)) {
                        val session = CallService.currentCallSession.value
                        if (session != null && (session.state == android.telecom.Call.STATE_ACTIVE)) {
                            if (isNear && autoSpeakerActive) {
                                // Near ear: switch to earpiece
                                CallService.setAudioRoute(android.telecom.CallAudioState.ROUTE_EARPIECE)
                                autoSpeakerActive = false
                            } else if (!isNear && !autoSpeakerActive) {
                                // Far from ear: switch to speaker
                                CallService.setAudioRoute(android.telecom.CallAudioState.ROUTE_SPEAKER)
                                autoSpeakerActive = true
                            }
                        }
                    }
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        // Prevent notification shade from being pulled down during a call
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        enableEdgeToEdge()
        // Register pocket mode / auto-speaker proximity listener. The real near-ear
        // screen-off is handled entirely by CallService now (see comment above).
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximitySensor?.let {
            sensorManager?.registerListener(proxSensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        val answeredFromNotif = intent?.getBooleanExtra("ANSWERED_FROM_NOTIFICATION", false) ?: false
        if (answeredFromNotif && !prefs.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true)) {
            finishAndRemoveTask()
            return
        }

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val heldSession by CallService.heldCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()
                val settingsVersion by prefs.settingsChanged.collectAsState()

                val call = session?.call
                val callState = session?.state

                LaunchedEffect(callState) {
                    // Real near-ear screen-off (plain + orientation-gated) is now handled
                    // entirely by CallService regardless of which Activity is on top — this
                    // effect only needs to worry about auto-closing the call screen.
                    if (session == null || callState == Call.STATE_DISCONNECTED) {
                        delay(800)
                        // Wait for any pending auto-redial dialog or job to complete before closing
                        while (keepAliveForRedial.value) {
                            delay(300)
                        }
                        finishAndRemoveTask()
                    }
                }


                if (call != null && session != null) {
                    val number = call.details?.handle?.schemeSpecificPart ?: ""
                    val simSlot = remember(call) { getSimSlotForAccountHandle(this@CallActivity, call.details?.accountHandle) }
                    val isDualSim = remember { prefs.getActiveSimCount() >= 2 }
                    // Stable initial values — number shown immediately, replaced by
                    // contact name in-place once async lookup completes (no layout shift
                    // because the composable tree is already present and sized).
                    // Start empty so the layout is stable from the first frame.
                    // contactName is filled by the async lookup; until then we
                    // show the number as a subtitle-style fallback (see the status
                    // text below the name), so there is no visible content gap.
                    var contactName by remember { mutableStateOf("") }
                    var photoUri by remember { mutableStateOf<String?>(null) }
                    var contactId by remember { mutableStateOf<String?>(null) }

                    val heldCall = heldSession?.call
                    val heldNumber = heldCall?.details?.handle?.schemeSpecificPart ?: ""
                    var heldContactName by remember(heldNumber) { mutableStateOf(heldNumber.ifEmpty { "Unknown" }) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            val contact = contactsRepo.getContactByNumber(number)
                            if (contact != null) {
                                val hideNames = prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false)
                                val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
                                val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet() else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
                                contactName = if (hideNames && contact.id in hiddenIds) number else contact.name
                                photoUri = if (hideNames && contact.id in hiddenIds) null else contact.photoUri
                                contactId = contact.id
                            } else {
                                contactName = number
                                contactId = null
                            }
                        } else {
                            contactName = "Unknown"
                            contactId = null
                        }
                    }

                    LaunchedEffect(heldNumber) {
                        if (heldNumber.isNotEmpty()) {
                            val c = contactsRepo.getContactByNumber(heldNumber)
                            if (c != null) {
                                val hideNames = prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_NAMES, false)
                                val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
                                val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet() else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
                                heldContactName = if (hideNames && c.id in hiddenIds) heldNumber else c.name
                            }
                        }
                    }

                    val answeredFromNotification = intent?.getBooleanExtra("ANSWERED_FROM_NOTIFICATION", false) ?: false
                    ExpressiveCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_ACTIVE,
                        contactName = contactName,
                        contactId = contactId,
                        phoneNumber = number,
                        photoUri = photoUri,
                        audioState = audioState,
                        hasHeldCall = heldSession != null && heldSession?.state != Call.STATE_DISCONNECTED && heldSession?.state != Call.STATE_DISCONNECTING,
                        heldCallName = heldContactName,
                        contactsRepo = contactsRepo,
                        callLogRepo = callLogRepo,
                        prefs = prefs,
                        isPocketBlocked = { isPocketBlocked },
                        skipIncomingScreen = answeredFromNotification,
                        simSlot = simSlot,
                        showSimBadge = isDualSim
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isInForeground.value = true
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val session = CallService.currentCallSession.value
        val isRinging = session?.state == Call.STATE_RINGING
        if (isRinging) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                com.coolappstore.everdialer.by.svhp.controller.util.silenceRingingCall(this)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(proxSensorListener)
        keepAliveForRedial.value = false
    }

    override fun onPause() {
        super.onPause()
        isInForeground.value = false
    }
}

private fun sanitizedPhoneForChatApps(number: String): String = number.filter { it.isDigit() || it == '+' }

/** Resolves a telecom PhoneAccountHandle (as reported on the live Call object) to a
 *  0-based SIM slot index via SubscriptionManager, or -1 if it can't be determined.
 *  This is how we can show a "SIM 1"/"SIM 2" badge on the in-call screen for an
 *  *incoming* call — unlike outgoing calls (where the app already knows which SIM
 *  it dialed out on), there was previously no way to tell which SIM an incoming
 *  call is arriving on. */
private fun getSimSlotForAccountHandle(context: Context, accountHandle: android.telecom.PhoneAccountHandle?): Int {
    if (accountHandle == null) return -1
    return try {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as? android.telephony.SubscriptionManager ?: return -1
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
        val phoneAccount = tm?.getPhoneAccount(accountHandle)
        val subId = phoneAccount?.extras?.getInt("android.telecom.extra.SUBSCRIPTION_ID", -1)?.takeIf { it != -1 }
            ?: accountHandle.id.toIntOrNull()
        if (subId != null && subId != -1) {
            val slot = sm.getActiveSubscriptionInfo(subId)?.simSlotIndex
            if (slot != null && slot in 0..1) return slot
        }
        val activeList = sm.activeSubscriptionInfoList
        if (!activeList.isNullOrEmpty()) {
            val match = activeList.firstOrNull { sub ->
                accountHandle.id.contains(sub.subscriptionId.toString()) ||
                        (sub.iccId != null && accountHandle.id.contains(sub.iccId))
            }
            if (match != null && match.simSlotIndex in 0..1) {
                return match.simSlotIndex
            }
            if (activeList.size == 1) {
                return activeList[0].simSlotIndex
            }
        }
        -1
    } catch (_: Exception) { -1 }
}

private fun openSmsApp(context: Context, number: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {}
}

private fun openWhatsAppChat(context: Context, number: String) {
    val clean = sanitizedPhoneForChatApps(number)
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }
}

private fun openTelegramChat(context: Context, number: String) {
    val clean = sanitizedPhoneForChatApps(number)
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }
}

private fun openMessageApp(context: Context, number: String, appKey: String) {
    when (appKey) {
        "whatsapp" -> openWhatsAppChat(context, number)
        "telegram" -> openTelegramChat(context, number)
        else -> openSmsApp(context, number)
    }
}

private data class CallBackgroundConfig(
    val bgType: String = "none",
    val bgPath: String = "",
    val bgZoom: Float = 1f,
    val bgPanX: Float = 0f,
    val bgPanY: Float = 0f,
    val bgDim: Float = 0f,
    val bgBlur: Float = 0f,
    val bgVideoSpeed: Float = 1.0f,
    val bgFile: java.io.File? = null,
    val hasCustomBg: Boolean = false,
    val fontColorMode: String = "default",
    val customFontColorInt: Int = android.graphics.Color.WHITE,
    val showContactPfp: Boolean = true,
    val showPhoneNumber: Boolean = true,
    val elementsTheme: String = "auto"
)

@Composable
private fun CallBackgroundLayer(
    config: CallBackgroundConfig,
    photoUri: String?,
    isDark: Boolean,
    driftX: Float,
    driftY: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (config.hasCustomBg && config.bgFile != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (config.bgType == "video") {
                    com.coolappstore.everdialer.by.svhp.view.components.LoopingVideoPlayer(
                        videoFile = config.bgFile,
                        videoSpeed = config.bgVideoSpeed,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = config.bgZoom
                                scaleY = config.bgZoom
                                translationX = config.bgPanX
                                translationY = config.bgPanY
                            }
                            .then(if (config.bgBlur > 0f) Modifier.blur(config.bgBlur.dp) else Modifier)
                    )
                } else {
                    AsyncImage(
                        model = config.bgFile,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = config.bgZoom
                                scaleY = config.bgZoom
                                translationX = config.bgPanX
                                translationY = config.bgPanY
                            }
                            .then(if (config.bgBlur > 0f) Modifier.blur(config.bgBlur.dp) else Modifier)
                    )
                }

                if (config.bgDim > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = config.bgDim))
                    )
                }
            }
        } else {
            // Blurred background photo (Default)
            if (!photoUri.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = driftX
                            translationY = driftY
                            scaleX = 1.4f
                            scaleY = 1.4f
                        }
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .alpha(if (isDark) 0.35f else 0.2f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Top contrast gradient scrim for maximum text & detail legibility on custom backgrounds
        if (config.hasCustomBg) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.60f),
                                Color.Black.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    contactId: String? = null,
    phoneNumber: String = "",
    photoUri: String?,
    audioState: CallAudioState?,
    hasHeldCall: Boolean = false,
    heldCallName: String = "",
    contactsRepo: IContactsRepository? = null,
    callLogRepo: ICallLogRepository? = null,
    prefs: PreferenceManager? = null,
    isPocketBlocked: () -> Boolean = { false },
    skipIncomingScreen: Boolean = false,
    simSlot: Int = -1,
    showSimBadge: Boolean = false
) {
    val context = LocalView.current.context
    val ctx = context
    var showMessageAppPicker by remember { mutableStateOf(false) }
    val onMessageButtonClick: () -> Unit = {
        val pref = prefs?.getString(PreferenceManager.KEY_DEFAULT_MESSAGE_APP, "sms") ?: "sms"
        if (pref == "ask") {
            showMessageAppPicker = true
        } else {
            try { call.disconnect() } catch (_: Exception) {}
            openMessageApp(context, phoneNumber, pref)
        }
    }
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
    // Manual state for the optional "Record" Feature Button — toggles the bundled call
    // recorder's foreground service on/off for this call. Purely a manual trigger; the
    // recorder's own auto-record setting (Settings → Call Recording) is unaffected.
    var isManuallyRecording by remember { mutableStateOf(false) }
    var showNoteWindow by remember { mutableStateOf(false) }

    // ── Call-lock biometric ────────────────────────────────────────────────
    val callLockEnabled = remember {
        prefs?.shouldGateCallWithBiometric(phoneNumber) == true
    }
    var callBiometricUnlocked by remember { mutableStateOf(!callLockEnabled || skipIncomingScreen) }
    var showCallBiometricUnlock by remember { mutableStateOf(false) }
    var biometricGatesScreen by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Gate the incoming call screen behind biometric when call arrives ringing
    LaunchedEffect(callState) {
        if (callLockEnabled && !callBiometricUnlocked && !showCallBiometricUnlock) {
            if (callState == Call.STATE_RINGING) {
                biometricGatesScreen = true
                showCallBiometricUnlock = true
            }
        }
    }

    // ── Auto-redial state ────────────────────────────────────────────────────
    val autoRedialEnabled = remember { prefs?.getBoolean(PreferenceManager.KEY_AUTO_REDIAL_ENABLED, false) == true }
    var showRedialDialog by remember { mutableStateOf(false) }
    var redialReason by remember { mutableStateOf("") }
    var wasOutgoingCall by remember { mutableStateOf(false) }
    // Tracks whether the call ever reached STATE_ACTIVE (i.e. actually connected).
    // Auto-redial should only be offered for calls that never connected — not for
    // a completed call that the other party simply hung up on afterwards.
    var callWasConnected by remember { mutableStateOf(false) }
    var redialCountSelected by remember { mutableIntStateOf(3) }
    var redialJobActive by remember { mutableStateOf(false) }
    var redialRemaining by remember { mutableIntStateOf(0) }
    var redialCountdown by remember { mutableIntStateOf(0) }
    val redialScope = rememberCoroutineScope()

    // Track if this was an outgoing call (dialing state)
    // Using a single LaunchedEffect to avoid race between the two callState effects
    LaunchedEffect(callState) {
        if (callState == Call.STATE_DIALING || callState == Call.STATE_CONNECTING) {
            wasOutgoingCall = true
        } else if (callState == Call.STATE_ACTIVE) {
            callWasConnected = true
        } else if (callState == Call.STATE_DISCONNECTED && autoRedialEnabled && wasOutgoingCall &&
                   !callWasConnected && !redialJobActive) {
            val dc = call.details?.disconnectCause?.code ?: DisconnectCause.UNKNOWN
            // REJECTED=5, BUSY=4, REMOTE=2 (unanswered/remote end), MISSED=7
            // Only offered when the call never connected in the first place.
            val shouldOffer = dc == DisconnectCause.REJECTED || dc == DisconnectCause.BUSY ||
                              dc == DisconnectCause.REMOTE   || dc == DisconnectCause.MISSED ||
                              dc == DisconnectCause.ERROR    || dc == DisconnectCause.UNKNOWN
            if (shouldOffer) {
                redialReason = when (dc) {
                    DisconnectCause.REJECTED -> "Call was rejected"
                    DisconnectCause.BUSY     -> "Line was busy"
                    DisconnectCause.REMOTE,
                    DisconnectCause.MISSED   -> "Call was not answered"
                    DisconnectCause.ERROR    -> "Call failed"
                    else                     -> "Call ended"
                }
                // Signal the activity to stay alive until the dialog is resolved
                CallActivity.keepAliveForRedial.value = true
                showRedialDialog = true
            }
        }
    }


    var showMergeConfirm by remember { mutableStateOf(false) }
    var showAddPersonSheet by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }
    var dtmfInput by remember { mutableStateOf("") }

    // Track isAddingToCall from service so Merge button only shows when 3rd party answered
    var isAddingToCallState by remember { mutableStateOf(CallService.isAddingToCall) }
    LaunchedEffect(Unit) {
        while (true) {
            isAddingToCallState = CallService.isAddingToCall
            kotlinx.coroutines.delay(200)
        }
    }

    // Merge is only available when held call exists AND we are NOT still dialing the 3rd party
    val canShowMerge = hasHeldCall && !isAddingToCallState

    // Auto-dismiss merge confirm dialog if the held call disappears (3rd person hung up)
    LaunchedEffect(hasHeldCall) {
        if (!hasHeldCall) {
            showMergeConfirm = false
            showAddPersonSheet = false
            isAddingToCallState = false
            if (callState == Call.STATE_ACTIVE) {
                isOnHold = false
            }
        }
    }

    var callDuration by remember { mutableLongStateOf(0L) }
    val systemDarkTheme = isSystemInDarkTheme()
    // Respect the user's explicit Light/Dark theme choice (Settings → Interface), not just the
    // system's current mode, so the swipe pill's icon/handle always matches how the app actually
    // looks — otherwise on a device in light mode with the app forced to Dark (or vice versa) the
    // handle icon renders with the wrong, mismatched tint.
    val isDark = when (prefs?.getString(PreferenceManager.KEY_THEME_MODE, "auto")) {
        "light", "white" -> false
        "dark", "black" -> true
        else -> systemDarkTheme
    }

    // Hangup button width from prefs (0.1f .. 1.0f)
    val settingsVersion by (prefs?.settingsChanged ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
    val hangupWidthFraction = remember(settingsVersion) {
        prefs?.getFloat(PreferenceManager.KEY_HANGUP_WIDTH, 0.5f) ?: 0.5f
    }
    // Feature Buttons order/visibility from Settings → Appearance → Caller UI
    val activeButtonIds = remember(settingsVersion) {
        prefs?.let { CallButtonPrefs.getActiveActionIds(it) } ?: listOf(
            CallButtonPrefs.ID_HOLD, CallButtonPrefs.ID_ADD, CallButtonPrefs.ID_DIALPAD,
            CallButtonPrefs.ID_NOTE, CallButtonPrefs.ID_MUTE, CallButtonPrefs.ID_SPEAKER,
            CallButtonPrefs.ID_BLUETOOTH
        )
    }
    // Bluetooth is only actually shown while a headset is connected for this call — otherwise
    // it's dropped from the grid entirely (not just hidden in an empty slot), so the remaining
    // buttons reflow to fill the freed-up space instead of leaving a blank row/column behind.
    val displayedButtonIds = remember(activeButtonIds, isBluetoothConnected) {
        if (isBluetoothConnected) activeButtonIds
        else activeButtonIds.filter { it != CallButtonPrefs.ID_BLUETOOTH }
    }
    // Freeform layout — when enabled in Settings → Appearance → Caller UI, buttons are rendered
    // at the exact custom positions configured there instead of the fixed 3-per-row grid. The
    // real call screen only *renders* this saved layout; dragging/editing happens in Settings.
    val freeformEnabled = remember(settingsVersion) {
        prefs?.let { CallButtonPrefs.isFreeformEnabled(it) } ?: false
    }
    val freeformPositions = remember(settingsVersion) {
        prefs?.let { CallButtonPrefs.getFreeformPositions(it) } ?: emptyMap()
    }
    // Show Names / Element Size — configured in Settings → Appearance → Caller UI, applied to
    // every Feature Button (and Hang Up, in Freeform mode) via CompositionLocalProvider below.
    val showElementNames = remember(settingsVersion) {
        prefs?.let { CallButtonPrefs.isShowNamesEnabled(it) } ?: true
    }
    val elementSizeScale = remember(settingsVersion) {
        prefs?.let { CallButtonPrefs.getElementSize(it) } ?: CallButtonPrefs.ELEMENT_SIZE_DEFAULT
    }
    var showMoreMenu by remember { mutableStateOf(false) }
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
    // Settings → Appearance → Visual Effects → "Hangup Animation". When off, the call screen
    // should close immediately instead of smoothly sliding/fading away.
    val hangupAnimationEnabled = remember(settingsVersion) {
        prefs?.getBoolean(PreferenceManager.KEY_HANGUP_ANIMATION, true) ?: true
    }
    val disconnectOffset by animateDpAsState(
        if (isDisconnecting) 120.dp else 0.dp,
        if (hangupAnimationEnabled) tween(600) else snap(),
        label = "disconnectSlide"
    )
    val disconnectAlpha by animateFloatAsState(
        if (isDisconnecting) 0f else 1f,
        if (hangupAnimationEnabled) tween(600) else snap(),
        label = "disconnectAlpha"
    )

    var wasRinging by remember { mutableStateOf(callState == Call.STATE_RINGING && !skipIncomingScreen) }
    var screenEntered by remember { mutableStateOf(true) }

    // Smooth answer transition: when ringing → active, gently scale + fade the UI in.
    var callAnswered by remember { mutableStateOf(skipIncomingScreen || (callState == Call.STATE_ACTIVE && !wasRinging)) }
    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            callAnswered = true
        }
        if (callState == Call.STATE_RINGING && !skipIncomingScreen) {
            wasRinging = true
        }
        if (callState == Call.STATE_DISCONNECTED || callState == Call.STATE_DISCONNECTING) isDisconnecting = true
        // If call returns to active from holding (e.g. held call restored), sync isOnHold
        if (callState == Call.STATE_ACTIVE && isOnHold) isOnHold = false
        if (callState == Call.STATE_ACTIVE && wasRinging) {
            val showOngoingUI = prefs?.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true) ?: true
            if (!showOngoingUI) {
                (context as? Activity)?.finishAndRemoveTask()
            }
        }
    }

    val isIncomingMode = (callState == Call.STATE_RINGING || (wasRinging && !callAnswered)) && !skipIncomingScreen

    val answerProgress by animateFloatAsState(
        targetValue = if (wasRinging && !callAnswered) 0f else 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "answerProgress"
    )
    val acceptScale = if (wasRinging && callAnswered) lerp(0.97f, 1f, answerProgress) else 1f
    val acceptAlpha = if (wasRinging && callAnswered) lerp(0.88f, 1f, answerProgress) else 1f

    LaunchedEffect(Unit) {
        while (true) {
            val connectTime = call.details?.connectTimeMillis ?: 0L
            callDuration = if (connectTime > 0L) (System.currentTimeMillis() - connectTime) / 1000L else 0L
            delay(500)
        }
    }

    var resolvedContactId by remember(contactId, phoneNumber) { mutableStateOf(contactId) }
    var resolvedContact by remember(phoneNumber) { mutableStateOf<Contact?>(null) }

    LaunchedEffect(phoneNumber, contactsRepo) {
        if (phoneNumber.isNotEmpty() && contactsRepo != null) {
            val c = try { contactsRepo.getContactByNumber(phoneNumber) } catch (_: Exception) { null }
            resolvedContact = c
            if (c?.id != null) {
                resolvedContactId = c.id
            }
        }
    }

    // Prioritize contact-specific calling background if configured
    val getCustomPrefixFor = { targetPrefix: String ->
        if (prefs == null) null
        else {
            val cleanNum = phoneNumber.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
            val digitsOnly = cleanNum.filter { it.isDigit() }
            val last10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else ""
            val last7 = if (digitsOnly.length >= 7) digitsOnly.takeLast(7) else ""

            val candidates = mutableSetOf<String>()
            contactId?.let { if (it.isNotBlank()) candidates.add(it) }
            resolvedContactId?.let { if (it.isNotBlank()) candidates.add(it) }
            resolvedContact?.id?.let { if (it.isNotBlank()) candidates.add(it) }
            resolvedContact?.phoneNumbers?.forEach { pn ->
                val pNum = pn.trim()
                if (pNum.isNotBlank()) {
                    candidates.add(pNum)
                    val cDigits = pNum.filter { it.isDigit() }
                    if (cDigits.isNotBlank()) candidates.add(cDigits)
                    if (cDigits.length >= 10) candidates.add(cDigits.takeLast(10))
                    if (cDigits.length >= 7) candidates.add(cDigits.takeLast(7))
                }
            }
            if (phoneNumber.isNotBlank()) candidates.add(phoneNumber.trim())
            if (cleanNum.isNotBlank()) candidates.add(cleanNum)
            if (digitsOnly.isNotBlank()) candidates.add(digitsOnly)
            if (last10.isNotBlank()) candidates.add(last10)
            if (last7.isNotBlank()) candidates.add(last7)
            if (contactName.isNotBlank() && contactName != "Unknown" && contactName != phoneNumber) {
                candidates.add(contactName.trim())
            }

            var foundPrefix: String? = null
            for (cand in candidates) {
                val prefKey = "contact_${cand}_${targetPrefix}_bg_type"
                val type = prefs.getString(prefKey, null)
                if (!type.isNullOrEmpty() && type != "none") {
                    foundPrefix = "contact_${cand}_$targetPrefix"
                    break
                }
            }
            foundPrefix
        }
    }

    val incomingPrefix = remember(settingsVersion, phoneNumber, contactName, contactId, resolvedContactId, resolvedContact) {
        getCustomPrefixFor("incoming") ?: "incoming"
    }
    val ongoingPrefix = remember(settingsVersion, phoneNumber, contactName, contactId, resolvedContactId, resolvedContact) {
        getCustomPrefixFor("ongoing") ?: "ongoing"
    }

    fun loadBgConfig(prefix: String, isForIncoming: Boolean): CallBackgroundConfig {
        val defaultType = if (isForIncoming) prefs?.getString(PreferenceManager.KEY_INCOMING_BG_TYPE, "none") else prefs?.getString(PreferenceManager.KEY_ONGOING_BG_TYPE, "none")
        val defaultPath = if (isForIncoming) prefs?.getString(PreferenceManager.KEY_INCOMING_BG_PATH, "") else prefs?.getString(PreferenceManager.KEY_ONGOING_BG_PATH, "")
        val defaultZoom = if (isForIncoming) prefs?.getFloat(PreferenceManager.KEY_INCOMING_BG_ZOOM, 1f) else prefs?.getFloat(PreferenceManager.KEY_ONGOING_BG_ZOOM, 1f)
        val defaultPanX = if (isForIncoming) prefs?.getFloat(PreferenceManager.KEY_INCOMING_BG_PAN_X, 0f) else prefs?.getFloat(PreferenceManager.KEY_ONGOING_BG_PAN_X, 0f)
        val defaultPanY = if (isForIncoming) prefs?.getFloat(PreferenceManager.KEY_INCOMING_BG_PAN_Y, 0f) else prefs?.getFloat(PreferenceManager.KEY_ONGOING_BG_PAN_Y, 0f)
        val defaultDim = if (isForIncoming) prefs?.getFloat(PreferenceManager.KEY_INCOMING_BG_DIM, 0f) else prefs?.getFloat(PreferenceManager.KEY_ONGOING_BG_DIM, 0f)
        val defaultBlur = if (isForIncoming) prefs?.getFloat(PreferenceManager.KEY_INCOMING_BG_BLUR, 0f) else prefs?.getFloat(PreferenceManager.KEY_ONGOING_BG_BLUR, 0f)
        val defaultVideoSpeed = if (isForIncoming) prefs?.getFloat(PreferenceManager.KEY_INCOMING_BG_VIDEO_SPEED, 1.0f) else prefs?.getFloat(PreferenceManager.KEY_ONGOING_BG_VIDEO_SPEED, 1.0f)
        val defaultFontMode = if (isForIncoming) prefs?.getString(PreferenceManager.KEY_INCOMING_FONT_COLOR_MODE, "default") else prefs?.getString(PreferenceManager.KEY_ONGOING_FONT_COLOR_MODE, "default")
        val defaultFontColor = if (isForIncoming) prefs?.getInt(PreferenceManager.KEY_INCOMING_FONT_COLOR, android.graphics.Color.WHITE) else prefs?.getInt(PreferenceManager.KEY_ONGOING_FONT_COLOR, android.graphics.Color.WHITE)

        val bgType = prefs?.getString("${prefix}_bg_type", defaultType ?: "none") ?: (defaultType ?: "none")
        val bgPath = prefs?.getString("${prefix}_bg_path", defaultPath ?: "") ?: (defaultPath ?: "")
        val bgZoom = prefs?.getFloat("${prefix}_bg_zoom", defaultZoom ?: 1f) ?: (defaultZoom ?: 1f)
        val bgPanX = prefs?.getFloat("${prefix}_bg_pan_x", defaultPanX ?: 0f) ?: (defaultPanX ?: 0f)
        val bgPanY = prefs?.getFloat("${prefix}_bg_pan_y", defaultPanY ?: 0f) ?: (defaultPanY ?: 0f)
        val bgDim = prefs?.getFloat("${prefix}_bg_dim", defaultDim ?: 0f) ?: (defaultDim ?: 0f)
        val bgBlur = prefs?.getFloat("${prefix}_bg_blur", defaultBlur ?: 0f) ?: (defaultBlur ?: 0f)
        val bgVideoSpeed = prefs?.getFloat("${prefix}_bg_video_speed", defaultVideoSpeed ?: 1.0f) ?: (defaultVideoSpeed ?: 1.0f)
        val fontColorMode = prefs?.getString("${prefix}_font_color_mode", defaultFontMode ?: "default") ?: (defaultFontMode ?: "default")
        val customFontColorInt = prefs?.getInt("${prefix}_font_color", defaultFontColor ?: android.graphics.Color.WHITE) ?: (defaultFontColor ?: android.graphics.Color.WHITE)
        val showContactPfp = if (isForIncoming) prefs?.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP, true) ?: true
                             else prefs?.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, true) ?: true
        val showPhoneNumber = if (isForIncoming) prefs?.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_PHONE_NUMBER, true) ?: true
                              else prefs?.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_PHONE_NUMBER, true) ?: true
        val defaultElementsTheme = if (isForIncoming) prefs?.getString(PreferenceManager.KEY_INCOMING_ELEMENTS_THEME, "auto") ?: "auto" else "auto"
        val elementsTheme = prefs?.getString("${prefix}_elements_theme", defaultElementsTheme) ?: defaultElementsTheme

        val bgFile = if (bgPath.isNotEmpty()) java.io.File(bgPath) else null
        val hasCustomBg = (bgType == "wallpaper" || bgType == "picture" || bgType == "video") && bgFile != null && bgFile.exists()

        return CallBackgroundConfig(
            bgType = bgType,
            bgPath = bgPath,
            bgZoom = bgZoom,
            bgPanX = bgPanX,
            bgPanY = bgPanY,
            bgDim = bgDim,
            bgBlur = bgBlur,
            bgVideoSpeed = bgVideoSpeed,
            bgFile = bgFile,
            hasCustomBg = hasCustomBg,
            fontColorMode = fontColorMode,
            customFontColorInt = customFontColorInt,
            showContactPfp = showContactPfp,
            showPhoneNumber = showPhoneNumber,
            elementsTheme = elementsTheme
        )
    }

    val incomingBgConfig = remember(settingsVersion, incomingPrefix) {
        loadBgConfig(incomingPrefix, isForIncoming = true)
    }
    val ongoingBgConfig = remember(settingsVersion, ongoingPrefix) {
        loadBgConfig(ongoingPrefix, isForIncoming = false)
    }

    val currentBgConfig = if (isIncomingMode) incomingBgConfig else ongoingBgConfig
    val hasCustomBg = currentBgConfig.hasCustomBg
    val fontColorMode = currentBgConfig.fontColorMode
    val customFontColorInt = currentBgConfig.customFontColorInt
    val showContactPfp = currentBgConfig.showContactPfp
    val showPhoneNumber = currentBgConfig.showPhoneNumber
    val showIncomingMuteButton = remember(settingsVersion) { prefs?.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_MUTE_BUTTON, false) ?: false }

    val isIncomingElementsDark = when (incomingBgConfig.elementsTheme) {
        "light" -> false
        "dark" -> true
        else -> isDark
    }
    val incomingElemBgColor = if (isIncomingElementsDark) Color(0xFF23262D) else colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f)
    val incomingElemFgColor = if (isIncomingElementsDark) Color(0xFFE2E2E6) else MaterialTheme.colorScheme.onPrimaryContainer

    val effectiveOnBgColor = if (fontColorMode == "custom") Color(customFontColorInt)
        else if (hasCustomBg) Color.White
        else MaterialTheme.colorScheme.onSurface
    val effectiveSubtleColor = if (fontColorMode == "custom") Color(customFontColorInt).copy(alpha = 0.85f)
        else if (hasCustomBg) Color.White.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    val textShadow = if (hasCustomBg || fontColorMode == "custom") androidx.compose.ui.graphics.Shadow(
        color = Color.Black.copy(alpha = 0.80f),
        blurRadius = 8f,
        offset = androidx.compose.ui.geometry.Offset(0f, 2f)
    ) else null

    val bgColor = colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.035f)
    val onBgColor = effectiveOnBgColor
    val subtleColor = effectiveSubtleColor
    val overlayColor = if (hasCustomBg) Color.Black.copy(0.35f) else (if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f))
    val controlBtnColor = if (hasCustomBg) Color.Black.copy(0.45f) else (if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f))
    val controlBtnActiveColor = if (isDark || hasCustomBg) Color.White else Color.Black
    val controlBtnActiveFg = if (isDark || hasCustomBg) Color.Black else Color.White
    val controlBtnFg = onBgColor

    // ── Feature Buttons — renders one configurable ongoing-call button by id, honoring the
    // order/visibility chosen in Settings → Appearance → Caller UI → Feature Buttons ──
    @Composable
    fun RenderFeatureButton(id: String) {
        when (id) {
            CallButtonPrefs.ID_HOLD -> AnimatedCallButton(
                icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                label = "Hold", isActive = isOnHold,
                btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
            ) {
                isOnHold = !isOnHold
                if (isOnHold) call.hold() else call.unhold()
            }
            CallButtonPrefs.ID_ADD -> if (canShowMerge) {
                AnimatedCallButton(
                    icon = Icons.Default.CallMerge, label = "Merge", isActive = true,
                    btnColor = controlBtnColor, activeBtnColor = Color(0xFF4CAF50),
                    fgColor = controlBtnFg, activeFgColor = Color.White,
                    onClick = { showMergeConfirm = true }
                )
            } else {
                AnimatedCallButton(
                    icon = Icons.Default.PersonAdd, label = "Add Person", isActive = false,
                    btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                    fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg,
                    onClick = { showAddPersonSheet = true }
                )
            }
            CallButtonPrefs.ID_DIALPAD -> AnimatedCallButton(
                icon = Icons.Default.Dialpad, label = "Dialpad", isActive = showDialpad,
                btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
            ) { showDialpad = !showDialpad }
            CallButtonPrefs.ID_NOTE -> AnimatedCallButton(
                icon = Icons.Default.EditNote, label = "Note", isActive = showNoteWindow,
                btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
            ) { showNoteWindow = !showNoteWindow }
            CallButtonPrefs.ID_MUTE -> AnimatedCallButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = "Mute", isActive = isMuted,
                btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
            ) { CallService.setMuted(!isMuted) }
            CallButtonPrefs.ID_SPEAKER -> AnimatedCallButton(
                icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown,
                label = "Speaker", isActive = isSpeakerOn,
                btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
            ) {
                CallService.setAudioRoute(if (isSpeakerOn) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER)
            }
            // displayedButtonIds already excludes Bluetooth entirely while no headset is
            // connected, so this branch only ever runs while it actually is connected — no
            // reserved blank slot needed anymore, the grid simply reflows around its absence.
            CallButtonPrefs.ID_BLUETOOTH -> if (isBluetoothConnected) {
                AnimatedCallButton(
                    icon = if (isBluetoothActive) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    label = "Bluetooth", isActive = isBluetoothActive,
                    btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                    fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
                ) {
                    if (isBluetoothActive) CallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                    else CallService.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                }
            }
            CallButtonPrefs.ID_RECORD -> AnimatedCallButton(
                icon = Icons.Default.FiberManualRecord,
                label = if (isManuallyRecording) "Stop" else "Record",
                isActive = isManuallyRecording,
                btnColor = controlBtnColor, activeBtnColor = Color(0xFFE53935),
                fgColor = controlBtnFg, activeFgColor = Color.White
            ) {
                val action = if (isManuallyRecording) {
                    RecordingForegroundService.ACTION_STOP_RECORDING
                } else {
                    RecordingForegroundService.ACTION_MANUAL_START
                }
                try {
                    context.startService(
                        Intent(context, RecordingForegroundService::class.java).setAction(action)
                    )
                } catch (_: Exception) {
                    // Recording permissions/service unavailable on this device — ignore, the
                    // button simply won't toggle recording state below.
                }
                isManuallyRecording = !isManuallyRecording
            }
            // Only reached when Freeform is on — Hang Up is otherwise rendered separately as the
            // fixed, dedicated end-call action below/beside the grid.
            CallButtonPrefs.ID_HANGUP -> AnimatedCallButton(
                icon = Icons.Default.CallEnd, label = "Hang Up", isActive = true,
                btnColor = Color(0xFFD32F2F), activeBtnColor = Color(0xFFD32F2F),
                fgColor = Color.White, activeFgColor = Color.White,
                labelColor = controlBtnFg
            ) {
                if (noteText.isNotBlank() && phoneNumber.isNotEmpty()) {
                    NoteManager.writeNote(context, contactName, phoneNumber, noteText)
                }
                try { call.disconnect() } catch (_: Exception) {}
            }
            CallButtonPrefs.ID_MORE -> Box {
                AnimatedCallButton(
                    icon = Icons.Default.MoreHoriz, label = "More", isActive = showMoreMenu,
                    btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor,
                    fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg
                ) { showMoreMenu = true }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (showNoteWindow) "Hide Note" else "Note") },
                        leadingIcon = { Icon(Icons.Default.EditNote, null) },
                        onClick = { showMoreMenu = false; showNoteWindow = !showNoteWindow }
                    )
                    if (isBluetoothConnected) {
                        DropdownMenuItem(
                            text = { Text(if (isBluetoothActive) "Switch to Earpiece" else "Switch to Bluetooth") },
                            leadingIcon = { Icon(Icons.Default.Bluetooth, null) },
                            onClick = {
                                showMoreMenu = false
                                if (isBluetoothActive) CallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                                else CallService.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                            }
                        )
                    }
                }
            }
        }
    }

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
                    // Signal CallService FIRST so it knows the next outgoing call is an "add to call"
                    CallService.isAddingToCall = true
                    // Hold the current call and reflect that in UI.
                    var holdSucceeded = false
                    try {
                        call.hold()
                        isOnHold = true
                        holdSucceeded = true
                    } catch (e: Exception) {
                        android.util.Log.e("EverDialerCall", "Add call: hold() on current call threw", e)
                    }
                    if (!holdSucceeded) {
                        CallService.isAddingToCall = false
                        isOnHold = false
                        android.widget.Toast.makeText(context, "Couldn't hold the current call, so the second call wasn't placed", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    // The old code used a blind delay(300) here and assumed the current call had
                    // actually reached STATE_HOLDING by then. On slower networks/modems that
                    // transition can take noticeably longer than 300ms, and placing a second
                    // outgoing call before the first one is confirmed HELD is a common reason
                    // Telecom silently drops the second call — it never even reaches the
                    // radio/telephony layer, so nothing appears to happen. Actively wait for the
                    // confirmed state instead of guessing a fixed delay, with a bounded timeout so
                    // this can't hang forever if hold silently never completes.
                    var actuallyHeld = call.state == Call.STATE_HOLDING
                    if (!actuallyHeld) {
                        val waitStart = System.currentTimeMillis()
                        while (System.currentTimeMillis() - waitStart < 3000) {
                            delay(100)
                            if (call.state == Call.STATE_HOLDING) { actuallyHeld = true; break }
                            // If the call disconnected or moved to a terminal state while we were
                            // waiting for hold, stop waiting — there's nothing left to add to.
                            if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) break
                        }
                    }
                    if (!actuallyHeld) {
                        android.util.Log.w("EverDialerCall", "Add call: current call never reached STATE_HOLDING (state=${call.state}), aborting second call")
                        CallService.isAddingToCall = false
                        isOnHold = call.state == Call.STATE_HOLDING
                        android.widget.Toast.makeText(context, "Couldn't add the call — the current call didn't hold in time", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    try {
                        android.util.Log.d("EverDialerCall", "Add call: placing second call to $number")
                        makeCall(context, number)
                    } catch (e: Exception) {
                        android.util.Log.e("EverDialerCall", "Add call: makeCall() for second party threw", e)
                        CallService.isAddingToCall = false
                        isOnHold = false
                        try { call.unhold() } catch (_: Exception) {}
                        android.widget.Toast.makeText(context, "Couldn't place the second call", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Guard against extreme system "Display size" / font-scale accessibility settings pushing
    // critical controls (like End Call) off screen — clamp the effective density used for this
    // screen's layout (not the system's global text rendering) to a sane range.
    val rawDensity = LocalDensity.current
    val clampedCallDensity = remember(rawDensity.density, rawDensity.fontScale) {
        androidx.compose.ui.unit.Density(
            density = rawDensity.density,
            fontScale = rawDensity.fontScale.coerceIn(0.85f, 1.30f)
        )
    }

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
    androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides clampedCallDensity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .offset(y = disconnectOffset)
            .alpha(disconnectAlpha)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (wasRinging && !skipIncomingScreen && answerProgress < 1f) {
                CallBackgroundLayer(
                    config = incomingBgConfig,
                    photoUri = photoUri,
                    isDark = isDark,
                    driftX = driftX,
                    driftY = driftY,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - answerProgress }
                )
            }
            if (!wasRinging || skipIncomingScreen || answerProgress > 0f) {
                CallBackgroundLayer(
                    config = ongoingBgConfig,
                    photoUri = photoUri,
                    isDark = isDark,
                    driftX = driftX,
                    driftY = driftY,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (wasRinging && !skipIncomingScreen && answerProgress < 1f) {
                                Modifier.graphicsLayer { alpha = answerProgress }
                            } else Modifier
                        )
                )
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
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (showContactPfp) {
                                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(controlBtnColor)) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(48.dp), tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else subtleColor)
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
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = contactName.ifEmpty { "" },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        shadow = textShadow
                                    ),
                                    color = onBgColor.copy(alpha = if (contactName.isEmpty()) 0f else 1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                if (showSimBadge && simSlot in 0..1) {
                                    SimSlotBadge(slot = simSlot, modifier = Modifier.size(width = 16.dp, height = 19.dp), shape = RoundedCornerShape(percent = 25))
                                }
                                Text(
                                    text = when {
                                        isOnHold -> "On Hold"
                                        callState == Call.STATE_ACTIVE -> formatDuration(callDuration)
                                        callState == Call.STATE_DIALING -> "Calling"
                                        callState == Call.STATE_RINGING -> "Incoming"
                                        callState == Call.STATE_CONNECTING -> "Calling"
                                        callState == Call.STATE_DISCONNECTING || isDisconnecting || callState == Call.STATE_DISCONNECTED -> {
                                            if (isIncomingMode) "Declined" else "Hanging up..."
                                        }
                                        else -> "Connecting..."
                                    },
                                    color = if (isOnHold) Color(0xFFFFB74D) else subtleColor,
                                    style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow)
                                )
                            }
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
                        if (!isIncomingMode) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showNoteWindow,
                                enter = fadeIn(tween(250, easing = FastOutSlowInEasing)) +
                                        scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        ),
                                exit = fadeOut(tween(200, easing = FastOutLinearInEasing)) +
                                       scaleOut(
                                           targetScale = 0.92f,
                                           animationSpec = tween(200)
                                       ),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .imePadding()
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .zIndex(20f)
                            ) {
                                FloatingCallNoteBox(
                                    contactName = contactName,
                                    noteText = noteText,
                                    onNoteChange = { noteText = it },
                                    onClose = { showNoteWindow = false }
                                )
                            }
                        }
                    }

                    // Right panel: controls
                    if (!isIncomingMode) {
                        Surface(modifier = Modifier.weight(1f).fillMaxHeight(), color = overlayColor) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Scrollable area: feature buttons. Independent of
                                // display/font scale — this area shrinks and scrolls internally,
                                // it never pushes the End Call button below the visible screen.
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    CompositionLocalProvider(
                                        LocalCallButtonElementSize provides elementSizeScale,
                                        LocalCallButtonShowNames provides showElementNames
                                    ) {
                                        FeatureButtonsLayout(
                                            activeButtonIds = if (freeformEnabled) displayedButtonIds + CallButtonPrefs.ID_HANGUP else displayedButtonIds,
                                            freeformEnabled = freeformEnabled,
                                            freeformPositions = freeformPositions,
                                            rowSpacing = 16.dp
                                        ) { id -> RenderFeatureButton(id) }
                                    }
                                }
                                // Fixed footer: End Call button — always visible, never affected
                                // by how tall the scrollable content above is, regardless of
                                // system font/display scale. Hidden in Freeform mode, where Hang
                                // Up is instead a draggable tile inside FeatureButtonsLayout above.
                                if (!freeformEnabled) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val endInteraction2 = remember { MutableInteractionSource() }
                                    val endPressed2 by endInteraction2.collectIsPressedAsState()
                                    val endRadius2 by animateDpAsState(if (endPressed2) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium), label = "endRadius2")
                                    Surface(
                                        onClick = { if (noteText.isNotBlank() && phoneNumber.isNotEmpty()) NoteManager.writeNote(context, contactName, phoneNumber, noteText); try { call.disconnect() } catch (_: Exception) {} },
                                        modifier = Modifier.fillMaxWidth(0.8f).heightIn(min = 56.dp).scale(if (endPressed2) 0.96f else 1f),
                                        shape = RoundedCornerShape(endRadius2), color = Color(0xFFD32F2F), interactionSource = endInteraction2
                                    ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) { Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp)) } }
                                }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            NewSwipeToAnswer(
                                onAnswer = {
                                    if (!isPocketBlocked()) {
                                        if (callBiometricUnlocked) {
                                            try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {}
                                            if (prefs?.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true) == false) {
                                                (context as? Activity)?.finishAndRemoveTask()
                                            }
                                        } else {
                                            pendingAction = {
                                                try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {}
                                                if (prefs?.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true) == false) {
                                                    (context as? Activity)?.finishAndRemoveTask()
                                                }
                                            }
                                            showCallBiometricUnlock = true
                                        }
                                    }
                                },
                                onDecline = {
                                    if (!isPocketBlocked()) {
                                        if (callBiometricUnlocked) {
                                            try { call.disconnect() } catch (_: Exception) {}
                                        } else {
                                            pendingAction = { try { call.disconnect() } catch (_: Exception) {} }
                                            showCallBiometricUnlock = true
                                        }
                                    }
                                },
                                onMessage = onMessageButtonClick,
                                onMute = { com.coolappstore.everdialer.by.svhp.controller.util.silenceRingingCall(context) },
                                showMuteButton = showIncomingMuteButton,
                                labelColor = incomingElemFgColor,
                                bgColor = incomingElemBgColor,
                                isPocketBlocked = isPocketBlocked,
                                isDark = isIncomingElementsDark
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
                    val isIncomingRinging = isIncomingMode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .align(Alignment.TopCenter)
                            .padding(top = if (isIncomingRinging) (if (showContactPfp) 130.dp else 180.dp) else (if (showContactPfp) 100.dp else 140.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        val callAvatarSize = 240.dp
                        val callAvatarIconSize = 100.dp
                        val callNameStyle = if (isIncomingRinging)
                            MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium)
                        val callNameBoxHeight = if (isIncomingRinging) 64.dp else 50.dp

                        if (showContactPfp) {
                            Box(modifier = Modifier.size(callAvatarSize).clip(CircleShape).background(controlBtnColor)) {
                                // Always render Icon as base layer so layout never shifts
                                Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(callAvatarIconSize), tint = if (hasCustomBg) Color.White.copy(alpha = 0.75f) else subtleColor)
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

                            Spacer(modifier = Modifier.height(if (isIncomingRinging) 28.dp else 20.dp))
                        }

                        // Fixed height box so layout never shifts when name loads
                        Box(modifier = Modifier.fillMaxWidth().height(callNameBoxHeight), contentAlignment = Alignment.Center) {
                            Text(
                                text = contactName.ifEmpty { "" },
                                style = callNameStyle.copy(shadow = textShadow),
                                color = onBgColor.copy(alpha = if (contactName.isEmpty()) 0f else 1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Show the number this specific call is on — resolved once for this
                        // call session, so it's always the single correct number even when
                        // the matched contact has multiple saved numbers. Hidden if it would
                        // just duplicate the name shown above (e.g. unknown callers), or if disabled in settings.
                        if (showPhoneNumber && phoneNumber.isNotBlank() && phoneNumber != contactName) {
                            Text(
                                text = phoneNumber,
                                style = (if (isIncomingRinging) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium).copy(
                                    fontWeight = FontWeight.Bold,
                                    shadow = textShadow
                                ),
                                color = onBgColor.copy(alpha = if (contactName.isEmpty()) 0f else 1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            if (showSimBadge && simSlot in 0..1) {
                                SimSlotBadge(slot = simSlot, modifier = Modifier.size(width = if (isIncomingRinging) 18.dp else 20.dp, height = if (isIncomingRinging) 21.dp else 23.dp), shape = RoundedCornerShape(percent = 25))
                            }
                            Text(
                                text = when {
                                    isOnHold -> "On Hold"
                                    callState == Call.STATE_ACTIVE -> formatDuration(callDuration)
                                    callState == Call.STATE_DIALING -> "Calling"
                                    callState == Call.STATE_RINGING -> "Incoming"
                                    callState == Call.STATE_CONNECTING -> "Calling"
                                    callState == Call.STATE_DISCONNECTING || isDisconnecting || callState == Call.STATE_DISCONNECTED -> {
                                        if (isIncomingMode) "Declined" else "Hanging up..."
                                    }
                                    else -> "Connecting..."
                                },
                                color = if (isOnHold) Color(0xFFFFB74D) else subtleColor,
                                style = (if (isIncomingRinging) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge).copy(shadow = textShadow)
                            )
                        }

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
                    if (!isIncomingMode) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)),
                            color = overlayColor
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                // Feature Buttons — order & visibility from Settings → Appearance → Caller UI
                                CompositionLocalProvider(
                                    LocalCallButtonElementSize provides elementSizeScale,
                                    LocalCallButtonShowNames provides showElementNames
                                ) {
                                    FeatureButtonsLayout(
                                        activeButtonIds = if (freeformEnabled) displayedButtonIds + CallButtonPrefs.ID_HANGUP else displayedButtonIds,
                                        freeformEnabled = freeformEnabled,
                                        freeformPositions = freeformPositions,
                                        rowSpacing = 20.dp
                                    ) { id -> RenderFeatureButton(id) }
                                }

                                if (!freeformEnabled) {
                                Spacer(modifier = Modifier.height(20.dp))

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
                        }
                    } else {
                        // Ringing state — swipe to answer, also anchored to bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            NewSwipeToAnswer(
                                onAnswer = {
                                    if (!isPocketBlocked()) {
                                        if (callBiometricUnlocked) {
                                            try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {}
                                            if (prefs?.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true) == false) {
                                                (context as? Activity)?.finishAndRemoveTask()
                                            }
                                        } else {
                                            pendingAction = {
                                                try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: Exception) {}
                                                if (prefs?.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true) == false) {
                                                    (context as? Activity)?.finishAndRemoveTask()
                                                }
                                            }
                                            showCallBiometricUnlock = true
                                        }
                                    }
                                },
                                onDecline = {
                                    if (!isPocketBlocked()) {
                                        if (callBiometricUnlocked) {
                                            try { call.disconnect() } catch (_: Exception) {}
                                        } else {
                                            pendingAction = { try { call.disconnect() } catch (_: Exception) {} }
                                            showCallBiometricUnlock = true
                                        }
                                    }
                                },
                                onMessage = onMessageButtonClick,
                                onMute = { com.coolappstore.everdialer.by.svhp.controller.util.silenceRingingCall(context) },
                                showMuteButton = showIncomingMuteButton,
                                labelColor = incomingElemFgColor,
                                bgColor = incomingElemBgColor,
                                isPocketBlocked = isPocketBlocked,
                                isDark = isIncomingElementsDark
                            )
                        }
                    }

                    // Floating Notes Box centered on screen, adapts to keyboard
                    if (!isIncomingMode) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showNoteWindow,
                            enter = fadeIn(tween(250, easing = FastOutSlowInEasing)) +
                                    scaleIn(
                                        initialScale = 0.88f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                                    ),
                            exit = fadeOut(tween(200, easing = FastOutLinearInEasing)) +
                                   scaleOut(
                                       targetScale = 0.88f,
                                       animationSpec = tween(200)
                                   ),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .imePadding()
                                .padding(horizontal = 24.dp)
                                .zIndex(20f)
                        ) {
                            FloatingCallNoteBox(
                                contactName = contactName,
                                noteText = noteText,
                                onNoteChange = { noteText = it },
                                onClose = { showNoteWindow = false },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

        // ── Call biometric — direct prompt, no overlay ────────────────────
        if (showCallBiometricUnlock && prefs != null) {
            val biometricType = prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""
            val callActivity   = LocalContext.current as? FragmentActivity
            fun onBiometricFail() {
                showCallBiometricUnlock = false
                pendingAction = null
                // Don't disconnect — let the call keep ringing so the user can retry
            }
            when (biometricType) {
                "system" -> {
                    LaunchedEffect(showCallBiometricUnlock) {
                        val activity = callActivity ?: run { onBiometricFail(); return@LaunchedEffect }
                        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
                        val prompt = androidx.biometric.BiometricPrompt(
                            activity, executor,
                            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                    callBiometricUnlocked = true
                                    biometricGatesScreen = false
                                    showCallBiometricUnlock = false
                                    pendingAction?.invoke(); pendingAction = null
                                }
                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onBiometricFail() }
                                override fun onAuthenticationFailed() { /* keep prompt open */ }
                            }
                        )
                        prompt.authenticate(
                            androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Ever Dialer")
                                .setSubtitle("Verify your identity to access this call")
                                .setNegativeButtonText("Cancel")
                                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                .build()
                        )
                    }
                }
                "pin" -> {
                    com.coolappstore.everdialer.by.svhp.view.screen.settings.PinSetupDialog(
                        title = "Enter PIN", isVerify = true,
                        expectedPin = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PIN, "") ?: "",
                        showCloseButton = !biometricGatesScreen,
                        onConfirm = {
                            callBiometricUnlocked = true; biometricGatesScreen = false
                            showCallBiometricUnlock = false
                            pendingAction?.invoke(); pendingAction = null
                        },
                        onDismiss = { onBiometricFail() }
                    )
                }
                "password" -> {
                    com.coolappstore.everdialer.by.svhp.view.screen.settings.PasswordSetupDialog(
                        title = "Enter Password", isVerify = true,
                        expectedPassword = prefs.getString(PreferenceManager.KEY_BIOMETRICS_PASSWORD, "") ?: "",
                        showCloseButton = !biometricGatesScreen,
                        onConfirm = {
                            callBiometricUnlocked = true; biometricGatesScreen = false
                            showCallBiometricUnlock = false
                            pendingAction?.invoke(); pendingAction = null
                        },
                        onDismiss = { onBiometricFail() }
                    )
                }
            }
        }

        // ── Auto-redial dialog ────────────────────────────────────────────────
        if (showRedialDialog && phoneNumber.isNotEmpty()) {
            AutoRedialDialog(
                reason = redialReason,
                phoneNumber = phoneNumber,
                context = LocalContext.current,
                onConfirm = { count ->
                    showRedialDialog = false
                    redialRemaining = count
                    redialJobActive = true
                    redialCountSelected = count
                    // keepAliveForRedial remains true during the redial job
                    redialScope.launch {
                        var remaining = count
                        while (remaining > 0 && redialJobActive) {
                            for (i in 5 downTo 1) {
                                redialCountdown = i
                                kotlinx.coroutines.delay(1000)
                                if (!redialJobActive) break
                            }
                            if (!redialJobActive) break
                            redialCountdown = 0
                            com.coolappstore.everdialer.by.svhp.controller.util.makeCall(ctx, phoneNumber)
                            remaining--
                            redialRemaining = remaining
                            // Wait for the new call to connect/disconnect before deciding to redial again
                            kotlinx.coroutines.delay(35_000)
                        }
                        redialJobActive = false
                        // Allow the activity to close now that all redial attempts are done
                        CallActivity.keepAliveForRedial.value = false
                    }
                },
                onDismiss = {
                    showRedialDialog = false
                    // Allow the activity to close since user dismissed the dialog
                    CallActivity.keepAliveForRedial.value = false
                }
            )
        }

        // ── Auto-redial countdown overlay ────────────────────────────────────
        if (redialJobActive && redialCountdown > 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    modifier = Modifier.padding(24.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Redialing in ${redialCountdown}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${redialRemaining} attempt${if (redialRemaining != 1) "s" else ""} remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { redialJobActive = false; CallActivity.keepAliveForRedial.value = false }) { Text("Cancel") }
                    }
                }
            }
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

    if (showMessageAppPicker) {
        Dialog(onDismissRequest = { showMessageAppPicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        "Reply with",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    val quickReplyOptions = listOf(
                        Triple("sms", "Messages / SMS", Color(0xFF2196F3)),
                        Triple("whatsapp", "WhatsApp", Color(0xFF25D366)),
                        Triple("telegram", "Telegram", Color(0xFF29B6F6))
                    )
                    quickReplyOptions.forEach { (key, label, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMessageAppPicker = false
                                    try { call.disconnect() } catch (_: Exception) {}
                                    openMessageApp(context, phoneNumber, key)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        when (key) { "whatsapp" -> Icons.Default.Chat; "telegram" -> Icons.Default.Send; else -> Icons.Default.Sms },
                                        null, tint = color, modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
    }
    }
}

// ─── In-Call Dial Pad ──────────────────────────────────────────────────────────

/**
 * Renders the ongoing call's Feature Buttons either as the fixed 3-per-row grid, or — when
 * Freeform is turned on in Settings → Appearance → Caller UI — at the exact custom positions
 * saved there. Positions are read-only here; editing them happens in the Settings preview.
 */
@Composable
private fun FeatureButtonsLayout(
    activeButtonIds: List<String>,
    freeformEnabled: Boolean,
    freeformPositions: Map<String, Pair<Float, Float>>,
    rowSpacing: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable (String) -> Unit
) {
    if (!freeformEnabled) {
        activeButtonIds.chunked(3).forEachIndexed { rowIndex, rowIds ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(rowSpacing))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowIds.forEach { id -> content(id) }
            }
        }
        return
    }

    val density = LocalDensity.current
    val rows = if (activeButtonIds.isEmpty()) 1 else ((activeButtonIds.size + 2) / 3)
    val areaHeight = (rows * 96).dp.coerceAtLeast(120.dp)
    val tileWidthPx = with(density) { 76.dp.toPx() }
    val tileHeightPx = with(density) { 88.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(areaHeight)) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        activeButtonIds.forEachIndexed { index, id ->
            val (fx, fy) = freeformPositions[id] ?: CallButtonPrefs.defaultFreeformFraction(id, index, activeButtonIds.size)
            Box(
                modifier = Modifier.offset {
                    val cx = fx * containerWidthPx - tileWidthPx / 2f
                    val cy = fy * containerHeightPx - tileHeightPx / 2f
                    IntOffset(cx.roundToInt(), cy.roundToInt())
                }
            ) {
                content(id)
            }
        }
    }
}

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

// Ongoing call screen — Feature Button appearance, configured in Settings → Appearance →
// Caller UI. Provided via CompositionLocalProvider around the Feature Buttons layout so
// AnimatedCallButton (used for every Feature Button + Hang Up in Freeform mode) can read it
// without threading extra parameters through every call site.
val LocalCallButtonElementSize = compositionLocalOf { CallButtonPrefs.ELEMENT_SIZE_DEFAULT }
val LocalCallButtonShowNames = compositionLocalOf { true }

@Composable
fun AnimatedCallButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    btnColor: Color = Color.White.copy(0.12f),
    activeBtnColor: Color = Color.White,
    fgColor: Color = Color.White,
    activeFgColor: Color = Color.Black,
    labelColor: Color? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val sizeScale = LocalCallButtonElementSize.current
    val showLabel = LocalCallButtonShowNames.current
    val btnSize = (68.dp * sizeScale)
    val iconSize = (26.dp * sizeScale)
    val radius by animateDpAsState(if (isPressed) 16.dp * sizeScale else 32.dp * sizeScale, spring(stiffness = Spring.StiffnessMedium), label = "btnRadius")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, modifier = Modifier.size(btnSize).scale(if (isPressed) 0.9f else 1f), shape = RoundedCornerShape(radius), color = if (isActive) activeBtnColor else btnColor, interactionSource = interaction) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (isActive) activeFgColor else fgColor, modifier = Modifier.size(iconSize))
            }
        }
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                color = (labelColor ?: fgColor).copy(alpha = if (labelColor != null) labelColor.alpha * 0.7f else 0.7f),
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NewSwipeToAnswer(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit = {},
    onMute: () -> Unit = {},
    showMuteButton: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f),
    bgColor: Color = colorLerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.55f),
    isPocketBlocked: () -> Boolean = { false },
    isDark: Boolean = isSystemInDarkTheme()
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val handleColor = MaterialTheme.colorScheme.surface
    val handleFg = MaterialTheme.colorScheme.onSurface

    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    val handleSizePx = with(density) { 72.dp.toPx() }
    val edgeGapPx    = with(density) { 9.dp.toPx() }
    val maxDrag = ((pillWidthPx - handleSizePx) / 2f - edgeGapPx)
        .coerceAtLeast(with(density) { 100.dp.toPx() })

    // Flash-fill progress for confirmed answer/decline (purely cosmetic, fired AFTER action)
    val answerFlash  = remember { Animatable(0f) }
    val declineFlash = remember { Animatable(0f) }

    // Entrance reveal — pill starts narrow (just the handle) and expands outward to both
    // sides fast and smoothly, revealing the Decline/Answer labels as it grows.
    var pillRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        pillRevealed = true
    }
    val pillWidthFraction by animateFloatAsState(
        targetValue = if (pillRevealed) 0.8f else 0.26f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "pillWidthFraction"
    )
    val pillRevealAlpha = ((pillWidthFraction - 0.26f) / (0.8f - 0.26f)).coerceIn(0f, 1f)

    // The hint only starts shortly after the entrance reveal has fully finished, so the
    // two animations never run at the same time.
    var pulseActive by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120 + 900 + 300)
        pulseActive = true
    }

    // Hint bar — a subtle translucent capsule that grows outward from the phone-button
    // handle to the pill's edges, then retreats back into the handle, repeating on its
    // own as a recurring hint (no fading — visibility is just its width). The instant
    // the drag coming out of the handle gets close to either end of the pill, this
    // same retreat fires immediately and overrides the cycle, so it clears out of the
    // way of the answer/decline fill instead of waiting for its own timer.
    val pillInset = 98.dp * 0.075f
    val hintHeight = 98.dp - (pillInset * 2)

    var pulseCycleOn by remember { mutableStateOf(false) }
    LaunchedEffect(pulseActive) {
        if (pulseActive) {
            while (true) {
                pulseCycleOn = true
                kotlinx.coroutines.delay(700 + 1400) // grow duration + hold
                pulseCycleOn = false
                kotlinx.coroutines.delay(700 + 500)  // retreat duration + gap before next cycle
            }
        }
    }

    val hintDragFraction = if (maxDrag > 0f) (offsetX.value / maxDrag).coerceIn(-1f, 1f) else 0f
    val hintNearEnd = kotlin.math.abs(hintDragFraction) > 0.6f
    val hintTarget = if (pulseActive && pulseCycleOn && !hintNearEnd) 1f else 0f
    val hintSpec = if (hintNearEnd) tween<Float>(180, easing = FastOutSlowInEasing) else tween<Float>(700, easing = FastOutSlowInEasing)
    val pulseGrowth by animateFloatAsState(targetValue = hintTarget, animationSpec = hintSpec, label = "pulseGrowth")
    val pulseAlpha by animateFloatAsState(targetValue = hintTarget * 0.09f, animationSpec = hintSpec, label = "pulseAlpha")
    val pulseColor = if (isDark) Color(0xFF212121) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Centered Quick Action Pills (Message, and optional Mute ringer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Message quick-reply pill
            Surface(
                onClick = onMessage,
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.height(45.dp).width(if (showMuteButton) 130.dp else 140.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ChatBubble, null, tint = labelColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Message", color = labelColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            if (showMuteButton) {
                Spacer(Modifier.width(16.dp))
                // Mute ringer pill
                Surface(
                    onClick = onMute,
                    shape = CircleShape,
                    color = bgColor,
                    modifier = Modifier.height(45.dp).width(130.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.VolumeOff, null, tint = labelColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mute", color = labelColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Swipe pill, with its recurring hint pulse layered above the solid background
        // but below the labels/handle, so it's actually visible on top of the pill.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .height(98.dp)
                    .fillMaxWidth(pillWidthFraction)
                    .clip(CircleShape)
                    .background(bgColor)
                    .onSizeChanged { pillWidthPx = it.width.toFloat() },
                contentAlignment = Alignment.Center
            ) {
            // Hint pulse — grows outward from the center (from behind the phone icon
            // handle) up to the pill's own width, inset by a small fixed margin on
            // every side (same gap as top/bottom), and fades, mirroring the entrance
            // reveal. Sized in absolute dp off the pill's real measured width, so it
            // always stays fully inside the pill's own footprint. Drawn as the first
            // child here so it layers on top of the pill's solid background.
            val hintFullWidth = with(density) { pillWidthPx.toDp() } - (pillInset * 2)
            Box(
                modifier = Modifier
                    .height(hintHeight)
                    .width((hintFullWidth.coerceAtLeast(0.dp)) * pulseGrowth)
                    .clip(CircleShape)
                    .background(pulseColor)
                    .alpha(pulseAlpha)
            )

            // Green answer fill — grows from right on confirm
            if (answerFlash.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(answerFlash.value)
                        .clip(CircleShape)
                        .background(Color(0xFF43A047))
                        .align(Alignment.CenterEnd)
                )
            }
            // Red decline fill — grows from left on confirm
            if (declineFlash.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(declineFlash.value)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .align(Alignment.CenterStart)
                )
            }

            // Decline / Answer labels
            val labelFade = (1f - maxOf(answerFlash.value, declineFlash.value) * 1.8f).coerceIn(0f, 1f) * pillRevealAlpha
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp).alpha(labelFade),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Decline", color = labelColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Answer",  color = labelColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }

            // Draggable handle — the pill itself stays a fixed neutral color while
            // dragging; only the phone icon glows green/red as feedback. The icon is
            // green by default and blends to red only as the user drags toward decline.
            val dragFraction = if (maxDrag > 0f) (offsetX.value / maxDrag).coerceIn(-1f, 1f) else 0f
            val iconTint = if (dragFraction < -0.45f)
                colorLerp(Color(0xFF4CAF50), Color(0xFFF44336), ((-dragFraction - 0.45f) / 0.55f).coerceIn(0f, 1f))
            else
                Color(0xFF4CAF50)
            // Answer (positive drag): phone icon rotates clockwise up to 90° (stands vertical).
            // Decline (negative drag): phone icon rotates anticlockwise up to 135°.
            val iconRotation = if (dragFraction >= 0f) dragFraction * 90f else dragFraction * 135f
            val handleAlpha = (1f - maxOf(answerFlash.value, declineFlash.value) * 2f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(handleColor)
                    .alpha(handleAlpha)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        offsetX.value >= maxDrag * 0.88f -> {
                                            // Animate fill then fire action
                                            launch { answerFlash.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                            kotlinx.coroutines.delay(180)
                                            onAnswer()
                                        }
                                        offsetX.value <= -maxDrag * 0.88f -> {
                                            launch { declineFlash.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                                            kotlinx.coroutines.delay(180)
                                            onDecline()
                                        }
                                        else -> offsetX.animateTo(
                                            0f,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (!isPocketBlocked()) {
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag))
                                    }
                                } else {
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoRedialDialog(
    reason: String,
    phoneNumber: String,
    context: android.content.Context,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(3, 5, 10, 15)
    var selectedCount by remember { mutableIntStateOf(3) }
    var expanded by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220),
        label = "dialogAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().scale(scale).alpha(alpha)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2196F3).copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Replay, null, tint = Color(0xFF2196F3), modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text("Auto Redial?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    "Automatically redial $phoneNumber until someone answers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Count picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Redial attempts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = "$selectedCount times",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count times") },
                                    onClick = { selectedCount = count; expanded = false },
                                    trailingIcon = if (selectedCount == count) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedCount) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Redial")
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingCallNoteBox(
    contactName: String,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Call Note",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (contactName.isNotBlank()) {
                            Text(
                                text = contactName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Note",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 260.dp),
                placeholder = {
                    Text(
                        "Add a note for this call...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(20.dp),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
