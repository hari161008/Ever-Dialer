package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * Shared animated container for the "Check for Updates" dialogs.
 * Provides a consistent pop-in/scale-fade entrance for every state.
 */
@Composable
fun UpdateDialogSurface(
    onDismissRequest: () -> Unit = {},
    dismissOnBack: Boolean = false,
    dismissOnClickOutside: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "updateDialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "updateDialogAlpha"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBack,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                content = content
            )
        }
    }
}

/** Rotating gradient ring with a pulsing center icon — shown while checking for updates. */
@Composable
fun UpdateCheckingDialog() {
    UpdateDialogSurface {
        val infinite = rememberInfiniteTransition(label = "checkingSpin")
        val rotation by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
            label = "spinAngle"
        )
        val pulse by infinite.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseScale"
        )

        val primary = MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                val stroke = 6.dp.toPx()
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(primary.copy(alpha = 0f), primary, primary.copy(alpha = 0f))),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(34.dp).scale(pulse)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Checking for updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Hang tight, this won't take long…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Animated success checkmark — shown when the app is already up to date. */
@Composable
fun UpdateUpToDateDialog(currentVersion: String, onDismiss: () -> Unit) {
    UpdateDialogSurface(onDismissRequest = onDismiss, dismissOnBack = true, dismissOnClickOutside = true) {
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }

        val circleScale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "checkCircleScale"
        )
        val checkScale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0f,
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            label = "checkIconScale",
        )

        val successColor = Color(0xFF4CAF50)

        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(circleScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(successColor.copy(alpha = 0.28f), successColor.copy(alpha = 0.06f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = successColor,
                modifier = Modifier.size(52.dp).scale(checkScale)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("You're up to date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Ever Dialer v$currentVersion is the latest version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = successColor)
        ) { Text("Nice!") }
    }
}

/**
 * Shown when a newer version is available.
 * [readyToInstall] = true when the APK for [latestVersion] is already downloaded —
 * in that case the primary action installs immediately without re-downloading.
 */
@Composable
fun UpdateAvailableDialog(
    currentVersion: String,
    latestVersion: String,
    readyToInstall: Boolean,
    onAction: () -> Unit,
    onDismiss: () -> Unit
) {
    UpdateDialogSurface(onDismissRequest = onDismiss, dismissOnBack = true, dismissOnClickOutside = false) {
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }

        val infinite = rememberInfiniteTransition(label = "availableFloat")
        val bob by infinite.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "iconBob"
        )

        val iconScale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0.4f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "availIconScale"
        )

        val accent = if (readyToInstall) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .size(84.dp)
                .scale(iconScale)
                .offset(y = bob.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.05f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (readyToInstall) Icons.Default.InstallMobile else Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(40.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Update available", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (readyToInstall) "The update has already been downloaded and is ready to install."
                else "A new version of Ever Dialer is ready to download.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Version chips: current -> latest
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VersionChip(label = "Current", version = "v$currentVersion", highlighted = false)
            val arrowAlpha by animateFloatAsState(
                targetValue = if (appeared) 1f else 0f,
                animationSpec = tween(400, delayMillis = 120),
                label = "arrowAlpha"
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp).alpha(arrowAlpha)
            )
            VersionChip(label = "New", version = "v$latestVersion", highlighted = true, accent = accent)
        }

        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Icon(
                if (readyToInstall) Icons.Default.InstallMobile else Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (readyToInstall) "Install Now" else "Download & Install")
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Not Now") }
    }
}

@Composable
private fun VersionChip(label: String, version: String, highlighted: Boolean, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                version,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) accent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Animated determinate progress dialog shown while the update APK downloads. */
@Composable
fun UpdateDownloadingDialog(latestVersion: String, progress: Float) {
    UpdateDialogSurface {
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            label = "downloadProgress"
        )

        val infinite = rememberInfiniteTransition(label = "downloadPulse")
        val pulse by infinite.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "downloadIconPulse"
        )

        val primary = MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Icon(
                Icons.Default.Downloading,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(34.dp).scale(pulse)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Downloading update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("v$latestVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                strokeCap = StrokeCap.Round
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(animatedProgress * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Installing automatically when done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Gentle shake + error icon — shown when the update check or download fails. */
@Composable
fun UpdateErrorDialog(message: String = "Could not check for updates. Please try again later.", onDismiss: () -> Unit) {
    UpdateDialogSurface(onDismissRequest = onDismiss, dismissOnBack = true, dismissOnClickOutside = true) {
        val shake = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    0f at 0
                    -10f at 60
                    10f at 120
                    -8f at 180
                    8f at 240
                    -4f at 300
                    4f at 360
                    0f at 420
                }
            )
        }

        val errorColor = MaterialTheme.colorScheme.error

        Box(
            modifier = Modifier
                .size(84.dp)
                .offset(x = shake.value.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(errorColor.copy(alpha = 0.22f), errorColor.copy(alpha = 0.05f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = errorColor, modifier = Modifier.size(44.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Update check failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }

        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) {
            Text("OK")
        }
    }
}
