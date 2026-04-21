package com.grinch.rivo4.view.screen.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.APP_VERSION
import com.grinch.rivo4.DISCORD_URL
import com.grinch.rivo4.GITHUB_URL
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AboutAppScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "logoScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "logoAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Ever Dialer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern logo container
            Surface(
                modifier = Modifier.size(110.dp).scale(scale).alpha(alpha),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Ever Dialer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(alpha)
            )
            Text(
                "Modern. Fast. Reliable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Version badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "v$APP_VERSION",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            RivoExpressiveCard {
                RivoListItem(
                    headline = "Developer",
                    supporting = "Hama (MoHamed-B-M)",
                    leadingIcon = Icons.Outlined.Person,
                    onClick = { openLink(context, GITHUB_URL) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                RivoListItem(
                    headline = "Source Code",
                    supporting = "GitHub Repository",
                    leadingIcon = Icons.Outlined.Code,
                    onClick = { openLink(context, GITHUB_URL) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                RivoListItem(
                    headline = "Community",
                    supporting = "Join our Discord server",
                    leadingIcon = Icons.Outlined.Groups,
                    onClick = { openLink(context, DISCORD_URL) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                RivoListItem(
                    headline = "App Version",
                    supporting = APP_VERSION,
                    leadingIcon = Icons.Default.Info,
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Open source badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            "Free & Open Source",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Built with ❤ using Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
