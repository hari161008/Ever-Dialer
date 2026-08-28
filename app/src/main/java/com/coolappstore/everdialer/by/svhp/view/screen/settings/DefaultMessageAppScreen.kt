package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private data class MessageAppOption(val key: String, val label: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

private val messageAppOptions = listOf(
    MessageAppOption("sms", "Messages / SMS", "Use the system's default messaging app", Icons.Outlined.Sms, Color(0xFF2196F3)),
    MessageAppOption("whatsapp", "WhatsApp", "Open a WhatsApp chat with this number", Icons.Outlined.Chat, Color(0xFF25D366)),
    MessageAppOption("telegram", "Telegram", "Open a Telegram chat with this number", Icons.Default.Send, Color(0xFF29B6F6)),
    MessageAppOption("ask", "Always ask", "Show a popup to choose every time", Icons.Outlined.HelpOutline, Color(0xFF9C27B0))
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DefaultMessageAppScreen(navigator: DestinationsNavigator) {
    val prefs: PreferenceManager = koinInject()
    var selected by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_DEFAULT_MESSAGE_APP, "sms") ?: "sms") }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Default Message", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item {
                RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        Text(
                            "Message App",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            Column {
                                messageAppOptions.forEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selected = option.key
                                                prefs.setString(PreferenceManager.KEY_DEFAULT_MESSAGE_APP, option.key)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = option.color.copy(alpha = 0.15f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(option.icon, null, tint = option.color, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = selected == option.key,
                                            onClick = {
                                                selected = option.key
                                                prefs.setString(PreferenceManager.KEY_DEFAULT_MESSAGE_APP, option.key)
                                            }
                                        )
                                    }
                                    if (index != messageAppOptions.lastIndex) {
                                        HorizontalDivider(
                                            Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                RivoAnimatedSection(delayMs = 60L) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "This controls the Message button on the incoming call screen. " +
                                    "\"Always ask\" shows a quick popup to pick an app each time " +
                                    "instead of opening one automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
