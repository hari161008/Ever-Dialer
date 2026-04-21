package com.grinch.rivo4.view.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ramcosta.composedestinations.generated.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
fun TopBar(navController: NavController, navigator: DestinationsNavigator) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else -16f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarTranslate"
    )

    LaunchedEffect(Unit) { visible = true }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .alpha(alpha)
            .offset(y = translateY.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { navigator.navigate(SearchScreenDestination) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Search in Ever Dialer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Surface(
                onClick = { navigator.navigate(SettingsScreenDestination) },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
