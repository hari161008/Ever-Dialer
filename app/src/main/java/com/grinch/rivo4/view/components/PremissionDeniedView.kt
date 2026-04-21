package com.grinch.rivo4.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDeniedView(
    icon: ImageVector,
    title: String,
    description: String,
    onGrantClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "permScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "permAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
            .alpha(alpha),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(120.dp).scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(56.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(36.dp))
        Button(
            onClick = onGrantClick,
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Grant Permission", fontWeight = FontWeight.SemiBold)
        }
    }
}
