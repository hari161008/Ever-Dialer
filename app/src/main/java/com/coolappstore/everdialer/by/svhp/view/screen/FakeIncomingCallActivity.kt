package com.coolappstore.everdialer.by.svhp.view.screen

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.data.FakeCallEntry
import com.coolappstore.everdialer.by.svhp.view.theme.Rivo4Theme
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

/**
 * Fake incoming call screen. Mimics the system incoming-call UI without placing
 * any real call. On "Answer", shows the ongoing-call screen UI; on "Decline" or
 * after the activity is dismissed, everything is silently cleaned up.
 */
class FakeIncomingCallActivity : ComponentActivity() {

    private val prefs: PreferenceManager by inject()
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen, turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val entryId = intent.getStringExtra(FakeCallManager.EXTRA_ID)
        val entry = if (entryId != null) FakeCallManager.findEntry(prefs, entryId) else null

        // Acquire wake lock
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "EverDialer:FakeCallScreenWL"
            ).also { it.acquire(120_000L) }
        } catch (_: Exception) {}

        startRingtone()
        startVibration()

        setContent {
            Rivo4Theme {
                var state by remember { mutableStateOf(FakeCallState.Ringing) }

                when (state) {
                    FakeCallState.Ringing -> FakeRingingScreen(
                        entry = entry,
                        onAnswer = {
                            stopRingtone()
                            stopVibration()
                            state = FakeCallState.Ongoing
                        },
                        onDecline = {
                            dismissAndFinish(entryId)
                        }
                    )
                    FakeCallState.Ongoing -> FakeOngoingCallScreen(
                        entry = entry,
                        onEnd = { dismissAndFinish(entryId) }
                    )
                }
            }
        }
    }

    private fun dismissAndFinish(entryId: String?) {
        stopRingtone()
        stopVibration()
        entryId?.let {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(FakeCallManager.notificationId(it))
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        stopVibration()
        try { wakeLock?.release() } catch (_: Exception) {}
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@FakeIncomingCallActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {}
    }

    private fun stopRingtone() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun stopVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.cancel()
            } else {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.cancel()
            }
        } catch (_: Exception) {}
    }
}

private enum class FakeCallState { Ringing, Ongoing }

// ─── Ringing Screen ───────────────────────────────────────────────────────────

@Composable
private fun FakeRingingScreen(
    entry: FakeCallEntry?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val displayName = entry?.displayName ?: "Unknown"
    val phoneNumber = entry?.phoneNumber ?: ""

    val infinite = rememberInfiniteTransition(label = "ring")
    val pulse1 by infinite.animateFloat(
        1f, 1.5f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "p1"
    )
    val pulse2 by infinite.animateFloat(
        1f, 1.9f,
        infiniteRepeatable(tween(1200, 300, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "p2"
    )
    val pulse1Alpha by infinite.animateFloat(
        0.45f, 0f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pa1"
    )
    val pulse2Alpha by infinite.animateFloat(
        0.3f, 0f,
        infiniteRepeatable(tween(1200, 300, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pa2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF1B2838), Color(0xFF0D1B2A))
                )
            )
    ) {
        // Stars / particles background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val rng = java.util.Random(42)
            repeat(80) {
                val x = rng.nextFloat() * size.width
                val y = rng.nextFloat() * size.height
                drawCircle(Color.White.copy(alpha = rng.nextFloat() * 0.3f), radius = rng.nextFloat() * 1.5f + 0.5f, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                "Incoming Call",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.65f),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(20.dp))

            // Avatar with ripple
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Box(modifier = Modifier.size(160.dp).scale(pulse2).clip(CircleShape).background(Color.White.copy(alpha = pulse2Alpha)))
                Box(modifier = Modifier.size(130.dp).scale(pulse1).clip(CircleShape).background(Color.White.copy(alpha = pulse1Alpha)))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(phoneNumber, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))

            Spacer(Modifier.weight(2f))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(
                        onClick = onDecline,
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, "Decline", modifier = Modifier.size(30.dp))
                    }
                    Text("Decline", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                }

                // Answer
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(
                        onClick = onAnswer,
                        containerColor = Color(0xFF43A047),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(Icons.Default.Phone, "Answer", modifier = Modifier.size(30.dp))
                    }
                    Text("Answer", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ─── Ongoing call screen ──────────────────────────────────────────────────────

@Composable
private fun FakeOngoingCallScreen(
    entry: FakeCallEntry?,
    onEnd: () -> Unit
) {
    val displayName = entry?.displayName ?: "Unknown"
    val phoneNumber = entry?.phoneNumber ?: ""
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val timerStr = remember(elapsedSeconds) {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        "%02d:%02d".format(m, s)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1B2838), Color(0xFF0D1B2A)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(phoneNumber, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.65f))
            Spacer(Modifier.height(10.dp))
            Text(timerStr, style = MaterialTheme.typography.titleMedium, color = Color(0xFF80CBC4), fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.weight(2f))

            // End call
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    onClick = onEnd,
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.CallEnd, "End", modifier = Modifier.size(32.dp))
                }
                Text("End Call", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}


