package com.grinch.rivo4.view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun ScrollToTopButton(visible: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.6f)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "arrowPulse")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    tween(800, easing = EaseInOut), RepeatMode.Reverse
                ),
                label = "arrowFloat"
            )

            SmallFloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(2.dp),
                modifier = Modifier.offset(y = offsetY.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
            }
        }
    }
}
