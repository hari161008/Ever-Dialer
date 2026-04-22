package com.grinch.rivo4.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RivoLoadingIndicatorView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            // Pulsing dots row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    PulsingDot(delayMs = index * 150L)
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(delayMs: Long) {
    var active by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        active = true
    }
    val infiniteTransition = rememberInfiniteTransition(label = "dot$delayMs")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "dotScale$delayMs"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "dotAlpha$delayMs"
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(8.dp).scale(scale).alpha(if (active) alpha else 0f)
    ) {}
}
