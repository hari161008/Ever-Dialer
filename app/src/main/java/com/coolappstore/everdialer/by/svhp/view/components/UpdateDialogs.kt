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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                content = content
            )
        }
    }
}

/** Rotating gradient squircle with a pulsing center icon — shown while checking for updates. */
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
            initialValue = 0.88f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseScale"
        )

        val primary = MaterialTheme.colorScheme.primary

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(rotation)
                        .scale(pulse)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Checking for Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Connecting to update repository…",
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

        val checkScale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0.4f,
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            label = "checkIconScale",
        )

        val successColor = MaterialTheme.colorScheme.primary

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = successColor,
                    modifier = Modifier.size(40.dp).scale(checkScale)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("You're Up to Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Ever Dialer v$currentVersion is the latest version available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = successColor)
        ) { Text("Great!") }
    }
}

/**
 * Modern Material You / Material Expressive update popup shown when an update is available.
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

        val iconScale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0.5f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "availIconScale"
        )

        val accent = MaterialTheme.colorScheme.primary

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp).scale(iconScale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Update Available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "A newer build of Ever Dialer is ready to download & explore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Version chips: current -> latest
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            VersionChip(label = "Installed", version = "v$currentVersion", highlighted = false, modifier = Modifier.weight(1f))
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(20.dp)
            )
            
            VersionChip(label = "Latest", version = "v$latestVersion", highlighted = true, accent = accent, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("View Update", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VersionChip(
    label: String,
    version: String,
    highlighted: Boolean,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        val primary = MaterialTheme.colorScheme.primary

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Downloading,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Downloading Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("v$latestVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                strokeCap = StrokeCap.Round,
                color = primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(animatedProgress * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Installing when ready…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(76.dp).offset(x = shake.value.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Update Check Failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("OK")
        }
    }
}
