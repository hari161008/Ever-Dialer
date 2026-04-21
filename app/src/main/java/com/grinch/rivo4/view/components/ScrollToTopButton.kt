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
import androidx.compose.ui.unit.dp

@Composable
fun ScrollToTopButton(visible: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f)
        ) {
            SmallFloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Scroll to top")
            }
        }
    }
}
