package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ─── Shared pop-in wrapper ────────────────────────────────────────────────────

@Composable
private fun LaunchDialogSurface(
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.78f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ldScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "ldAlpha"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().scale(scale).alpha(alpha)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

// ─── Android 14 Restricted Settings Dialog ───────────────────────────────────

@Composable
fun Android14WelcomeDialog(
    onAppInfo: () -> Unit,
    onContinue: () -> Unit
) {
    LaunchDialogSurface {
        // Animated header strip
        val primary = MaterialTheme.colorScheme.primary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary

        val infinite = rememberInfiniteTransition(label = "w14hdr")
        val orbX by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "orbX"
        )
        val orbY by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "orbY"
        )
        val ring by infinite.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(8000, easing = LinearEasing)),
            label = "ring"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.85f), primaryContainer.copy(alpha = 0.6f))
                    )
                )
        ) {
            // Blurry orbs
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(secondary.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(size.width * 0.15f + orbX * size.width * 0.25f, size.height * 0.2f + orbY * size.height * 0.5f),
                        radius = size.minDimension * 0.45f
                    ),
                    center = Offset(size.width * 0.15f + orbX * size.width * 0.25f, size.height * 0.2f + orbY * size.height * 0.5f),
                    radius = size.minDimension * 0.45f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(tertiary.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width * 0.75f - orbX * size.width * 0.2f, size.height * 0.7f),
                        radius = size.minDimension * 0.38f
                    ),
                    center = Offset(size.width * 0.75f - orbX * size.width * 0.2f, size.height * 0.7f),
                    radius = size.minDimension * 0.38f
                )
                // Dashed ring
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.3f
                drawArc(
                    color = Color.White.copy(alpha = 0.22f),
                    startAngle = ring,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Center icon
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Welcome to Ever Dialer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Body
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp).padding(top = 1.dp)
                    )
                    Text(
                        "Android 14+ requires \"Allow restricted settings\" to set this as default dialer when installed outside Play Store.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 18.sp
                    )
                }
            }

            // Steps
            StepRow(
                number = "1",
                icon = Icons.Default.TouchApp,
                text = "Long-press Ever Dialer icon → tap App info"
            )
            StepRow(
                number = "2",
                icon = Icons.Default.MoreVert,
                text = "Tap the ⋮ menu (top-right corner)"
            )
            StepRow(
                number = "3",
                icon = Icons.Default.LockOpen,
                text = "Tap \"Allow restricted settings\""
            )
            StepRow(
                number = "4",
                icon = Icons.Default.Celebration,
                text = "Return here and set as default dialer. Enjoy!"
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAppInfo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) { Text("App Info") }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun StepRow(number: String, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            lineHeight = 17.sp
        )
    }
}

// ─── Telegram Join Dialog ─────────────────────────────────────────────────────

@Composable
fun TelegramJoinDialog(
    onJoin: () -> Unit,
    onSkip: () -> Unit
) {
    LaunchDialogSurface {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer

        val infinite = rememberInfiniteTransition(label = "tghdr")
        val wave by infinite.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wave"
        )
        val pulse by infinite.animateFloat(
            0.92f, 1.08f,
            infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "tgPulse"
        )
        val shimmer by infinite.animateFloat(
            -1f, 2f,
            infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "shimmer"
        )

        // ── Banner ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.9f),
                            secondary.copy(alpha = 0.7f),
                            tertiary.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            // Animated orbs
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Large soft orb
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(secondary.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(size.width * (0.1f + wave * 0.3f), size.height * 0.3f),
                        radius = size.minDimension * 0.55f
                    ),
                    center = Offset(size.width * (0.1f + wave * 0.3f), size.height * 0.3f),
                    radius = size.minDimension * 0.55f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(tertiary.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(size.width * (0.85f - wave * 0.2f), size.height * 0.75f),
                        radius = size.minDimension * 0.4f
                    ),
                    center = Offset(size.width * (0.85f - wave * 0.2f), size.height * 0.75f),
                    radius = size.minDimension * 0.4f
                )

                // Shimmer streak
                val shimmerX = shimmer * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                        start = Offset(shimmerX - 80f, 0f),
                        end = Offset(shimmerX + 80f, size.height)
                    )
                )

                // Dot pattern
                val dotSpacing = 28.dp.toPx()
                val dotR = 2.dp.toPx()
                var dx = dotSpacing
                while (dx < size.width) {
                    var dy = dotSpacing
                    while (dy < size.height) {
                        drawCircle(Color.White.copy(alpha = 0.08f), radius = dotR, center = Offset(dx, dy))
                        dy += dotSpacing
                    }
                    dx += dotSpacing
                }
            }

            // Pill tags at top
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Updates", "Support", "Features").forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Center content
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Telegram icon placeholder (stylised circle)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Join the Community",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Stay in the loop with every release",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Body
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Support the developer by joining the Telegram channel & group. It's the best way to stay updated, request features, and get help.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Feature chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Icons.Outlined.Announcement to "Announcements",
                    Icons.Outlined.BugReport to "Bug Fixes",
                    Icons.Outlined.Star to "Rate",
                ).forEach { (icon, label) ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Navigate hint
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        "Links also available in Settings → About",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join Telegram", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
