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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

class CallActivity : ComponentActivity() {

    private val contactsRepo: IContactsRepository by inject()
    private val callLogRepo: ICallLogRepository by inject()
    private val prefs: PreferenceManager by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setupProximitySensor()
        enableEdgeToEdge()

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
                        finish()
                    }
                }

                if (call != null && session != null) {
                    val number = call.details?.handle?.schemeSpecificPart ?: ""
                    var contactName by remember { mutableStateOf(number.ifEmpty { "Unknown" }) }
                    var photoUri by remember { mutableStateOf<String?>(null) }

                    val heldCall = heldSession?.call
                    val heldNumber = heldCall?.details?.handle?.schemeSpecificPart ?: ""
                    var heldContactName by remember(heldNumber) { mutableStateOf(heldNumber.ifEmpty { "Unknown" }) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            contactsRepo.getContactByNumber(number)?.let {
                                contactName = it.name
                                photoUri = it.photoUri
                            }
                        }
                    }

                    LaunchedEffect(heldNumber) {
                        if (heldNumber.isNotEmpty()) {
                            contactsRepo.getContactByNumber(heldNumber)?.let {
                                heldContactName = it.name
                            }
                        }
                    }

                    ExpressiveCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_ACTIVE,
                        contactName = contactName,
                        phoneNumber = number,
                        photoUri = photoUri,
                        audioState = audioState,
                        hasHeldCall = heldSession != null,
                        heldCallName = heldContactName,
                        contactsRepo = contactsRepo,
                        callLogRepo = callLogRepo
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
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.requestDismissKeyguard(this, null)
    }

    override fun onDestroy() { super.onDestroy(); releaseProximityLock() }
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
    callLogRepo: ICallLogRepository? = null
) {
    val context = LocalView.current.context
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
    var isOnHold by remember { mutableStateOf(false) }
    var callDuration by remember { mutableLongStateOf(0L) }
    val isDark = isSystemInDarkTheme()
    var showNoteWindow by remember { mutableStateOf(false) }
    var showMergeConfirm by remember { mutableStateOf(false) }
    var showAddPersonSheet by remember { mutableStateOf(false) }

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
    var showAcceptAnim by remember { mutableStateOf(false) }
    val acceptScale by animateFloatAsState(if (showAcceptAnim) 1f else 0.85f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "acceptScale")
    val acceptAlpha by animateFloatAsState(if (showAcceptAnim) 1f else 0f, tween(400), label = "acceptAlpha")

    LaunchedEffect(callState) {
        if (callState == Call.STATE_DISCONNECTED || callState == Call.STATE_DISCONNECTING) isDisconnecting = true
        if (wasRinging && callState == Call.STATE_ACTIVE) showAcceptAnim = true
        if (callState == Call.STATE_RINGING) wasRinging = true
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

    // Add Person bottom sheet
    if (showAddPersonSheet) {
        AddPersonSheet(
            context = context,
            contactsRepo = contactsRepo,
            callLogRepo = callLogRepo,
            onDismiss = { showAddPersonSheet = false },
            onPersonSelected = { number ->
                showAddPersonSheet = false
                makeCall(context, number)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = disconnectOffset)
                .alpha(disconnectAlpha)
        ) {
            if (!photoUri.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = driftX; translationY = driftY; scaleX = 1.4f; scaleY = 1.4f }) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(80.dp).alpha(if (isDark) 0.35f else 0.2f), contentScale = ContentScale.Crop)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 80.dp)
                    .then(if (wasRinging && callState == Call.STATE_ACTIVE) Modifier.scale(acceptScale).alpha(acceptAlpha) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(controlBtnColor)) {
                    if (!photoUri.isNullOrEmpty()) {
                        AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(50.dp), tint = subtleColor)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(text = contactName, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium), color = onBgColor)
                Text(
                    text = when {
                        isOnHold -> "On Hold"
                        callState == Call.STATE_ACTIVE -> formatDuration(callDuration)
                        callState == Call.STATE_DIALING -> "Calling..."
                        callState == Call.STATE_RINGING -> "Incoming Call..."
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

                Spacer(modifier = Modifier.weight(1f))

                if (callState != Call.STATE_RINGING) {
                    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)), color = overlayColor) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                AnimatedCallButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, label = "Mute", isActive = isMuted, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) { CallService.setMuted(!isMuted) }
                                AnimatedCallButton(icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause, label = "Hold", isActive = isOnHold, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) {
                                    isOnHold = !isOnHold
                                    if (isOnHold) call.hold() else call.unhold()
                                }

                                // Show "Add Person" when no held call, "Merge" when there is one
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
                                    icon = Icons.Default.EditNote,
                                    label = "Note",
                                    isActive = showNoteWindow,
                                    btnColor = controlBtnColor,
                                    activeBtnColor = controlBtnActiveColor,
                                    fgColor = controlBtnFg,
                                    activeFgColor = controlBtnActiveFg
                                ) { showNoteWindow = !showNoteWindow }
                                AnimatedCallButton(icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown, label = "Speaker", isActive = isSpeakerOn, btnColor = controlBtnColor, activeBtnColor = controlBtnActiveColor, fgColor = controlBtnFg, activeFgColor = controlBtnActiveFg) {
                                    CallService.setAudioRoute(if (isSpeakerOn) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER)
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

                            val endInteraction = remember { MutableInteractionSource() }
                            val endPressed by endInteraction.collectIsPressedAsState()
                            val endRadius by animateDpAsState(if (endPressed) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium), label = "endRadius")

                            Surface(
                                onClick = {
                                    if (noteText.isNotBlank() && phoneNumber.isNotEmpty()) {
                                        NoteManager.writeNote(context, contactName, phoneNumber, noteText)
                                    }
                                    try { call.disconnect() } catch (e: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(76.dp).scale(if (endPressed) 0.96f else 1f),
                                shape = RoundedCornerShape(endRadius),
                                color = Color(0xFFD32F2F),
                                interactionSource = endInteraction
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                } else {
                    NewSwipeToAnswer(
                        onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                        onDecline = { try { call.disconnect() } catch (e: Exception) {} },
                        labelColor = subtleColor,
                        bgColor = overlayColor
                    )
                }
            }
        }
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
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Add Person", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            // Search bar (hidden for Dial tab)
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

            // Pill tabs
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

            // Tab content
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
                        // Compact dial pad
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
        // Number display
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

        // Bottom row: backspace + call
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
fun NewSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit, labelColor: Color = Color.White.copy(0.6f), bgColor: Color = Color.White.copy(0.08f)) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val maxDrag = with(density) { 100.dp.toPx() }
    val progress by remember { derivedStateOf { (kotlin.math.abs(offsetX.value) / maxDrag).coerceIn(0f, 1f) } }
    val fadeAlpha = 1f - progress
    val scaleFactor = 1f - (progress * 0.2f)
    val isDark = isSystemInDarkTheme()
    val handleColor = if (isDark) Color.White else Color.Black.copy(0.85f)
    val handleFg = if (isDark) Color.Black else Color.White

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(onClick = {}, shape = CircleShape, color = bgColor, modifier = Modifier.height(45.dp).width(140.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = labelColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", color = labelColor, style = MaterialTheme.typography.labelLarge)
            }
        }
        Box(modifier = Modifier.height(90.dp).fillMaxWidth(0.85f).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Decline", color = labelColor, style = MaterialTheme.typography.bodyLarge)
                Text("Answer", color = labelColor, style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(72.dp)
                    .graphicsLayer { alpha = fadeAlpha; scaleX = scaleFactor; scaleY = scaleFactor }
                    .clip(CircleShape)
                    .background(handleColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    when {
                                        offsetX.value > maxDrag * 0.6f -> onAnswer()
                                        offsetX.value < -maxDrag * 0.6f -> onDecline()
                                        else -> offsetX.animateTo(0f, spring(dampingRatio = 0.8f))
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)) }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = when { offsetX.value > 10 -> Color(0xFF4CAF50); offsetX.value < -10 -> Color(0xFFF44336); else -> handleFg },
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
