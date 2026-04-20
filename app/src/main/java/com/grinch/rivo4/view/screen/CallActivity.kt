package com.grinch.rivo4.view.screen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grinch.rivo4.controller.CallService
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.math.roundToInt
import com.grinch.rivo4.controller.ContactsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CallActivity : ComponentActivity() {

    private val contactsRepo: IContactsRepository by inject()
    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setupProximitySensor()
        enableEdgeToEdge()

        setContent {
            Rivo4Theme {
                val session by CallService.currentCallSession.collectAsState()
                val audioState by CallService.audioState.collectAsState()

                val call = session?.call
                val callState = session?.state

                LaunchedEffect(callState) {
                    when (callState) {
                        Call.STATE_ACTIVE, Call.STATE_DIALING -> acquireProximityLock()
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
                    var isUnknown by remember { mutableStateOf(true) }

                    LaunchedEffect(number) {
                        if (number.isNotEmpty()) {
                            contactsRepo.getContactByNumber(number)?.let {
                                contactName = it.name
                                photoUri = it.photoUri
                                isUnknown = false
                            }
                        }
                    }

                    ExpressiveCallScreen(
                        call = call,
                        callState = session?.state ?: Call.STATE_ACTIVE,
                        contactName = contactName,
                        photoUri = photoUri,
                        audioState = audioState
                    )
                }
            }
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

@Composable
fun ExpressiveCallScreen(
    call: Call,
    callState: Int,
    contactName: String,
    photoUri: String?,
    audioState: CallAudioState?
) {
    val context = LocalView.current.context
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
    var isOnHold by remember { mutableStateOf(false) }
    var callDuration by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val driftX by infiniteTransition.animateFloat(-35f, 35f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "x")
    val driftY by infiniteTransition.animateFloat(-25f, 25f, infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "y")

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            val start = System.currentTimeMillis()
            while (true) {
                callDuration = (System.currentTimeMillis() - start) / 1000
                delay(1000)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Animated Background
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = driftX; translationY = driftY; scaleX = 1.4f; scaleY = 1.4f }) {
            if (!photoUri.isNullOrEmpty()) {
                AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize().blur(100.dp).alpha(0.45f), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color.Black))))
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Profile Area
            Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
                if (!photoUri.isNullOrEmpty()) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center).size(50.dp), tint = Color.White.copy(0.6f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = contactName, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium), color = Color.White)
            Text(
                text = if (isOnHold) "On Hold" else if (callState == Call.STATE_ACTIVE) formatDuration(callDuration) else "Incoming Call...",
                color = if (isOnHold) Color(0xFFFFB74D) else Color.White.copy(0.6f),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (callState != Call.STATE_RINGING) {
                // Ongoing Call Controls (Frosted Glass)
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            AnimatedCallButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, label = "Mute", isActive = isMuted) {
                                CallService.setMuted(!isMuted)
                            }
                            AnimatedCallButton(icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause, label = "Hold", isActive = isOnHold) {
                                isOnHold = !isOnHold
                                if (isOnHold) call.hold() else call.unhold()
                            }
                            AnimatedCallButton(icon = Icons.Default.EditNote, label = "Note") {
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Note for $contactName: ") }
                                context.startActivity(Intent.createChooser(intent, "Save Note"))
                            }
                            AnimatedCallButton(icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown, label = "Speaker", isActive = isSpeakerOn) {
                                CallService.setAudioRoute(if (isSpeakerOn) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER)
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))

                        // End Call Button with Shape Animation
                        val endInteraction = remember { MutableInteractionSource() }
                        val endPressed by endInteraction.collectIsPressedAsState()
                        val endRadius by animateDpAsState(if (endPressed) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium))

                        Surface(
                            onClick = { try { call.disconnect() } catch (e: Exception) {} },
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
                // Incoming Call - New Swipe Design
                NewSwipeToAnswer(
                    onAnswer = { try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {} },
                    onDecline = { try { call.disconnect() } catch (e: Exception) {} }
                )
            }
        }
    }
}

@Composable
fun AnimatedCallButton(icon: ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val radius by animateDpAsState(if (isPressed) 16.dp else 32.dp, spring(stiffness = Spring.StiffnessMedium))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(68.dp).scale(if (isPressed) 0.9f else 1f),
            shape = RoundedCornerShape(radius),
            color = if (isActive) Color.White else Color.White.copy(0.12f),
            interactionSource = interaction
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (isActive) Color.Black else Color.White, modifier = Modifier.size(26.dp))
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun NewSwipeToAnswer(onAnswer: () -> Unit, onDecline: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val maxDrag = with(density) { 100.dp.toPx() }

    // --- حساب التأثيرات بناءً على السحب ---
    val progress by remember {
        derivedStateOf { (kotlin.math.abs(offsetX.value) / maxDrag).coerceIn(0f, 1f) }
    }

    // يتلاشى الزر كلما اقترب من الحافة (Answer أو Decline)
    val fadeAlpha = 1f - progress
    // يصغر الزر قليلاً عند السحب ليعطي طابع Android 16
    val scaleFactor = 1f - (progress * 0.2f)

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Message Button
        Surface(
            onClick = { /* Handle message */ },
            shape = CircleShape,
            color = Color.White.copy(0.1f),
            modifier = Modifier.height(45.dp).width(140.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelLarge)
            }
        }

        // Main Swipe Pill
        Box(
            modifier = Modifier
                .height(90.dp)
                .fillMaxWidth(0.85f)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f)),
            contentAlignment = Alignment.Center
        ) {
            // Background Labels
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Decline", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodyLarge)
                Text("Answer", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodyLarge)
            }

            // Draggable Handle with FADE EFFECT
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(72.dp)
                    // تطبيق التأثيرات هنا
                    .graphicsLayer {
                        alpha = fadeAlpha
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    .clip(CircleShape)
                    .background(Color.White)
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
                    tint = if (offsetX.value > 10) Color(0xFF4CAF50) else if (offsetX.value < -10) Color(0xFFF44336) else Color.DarkGray,
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
