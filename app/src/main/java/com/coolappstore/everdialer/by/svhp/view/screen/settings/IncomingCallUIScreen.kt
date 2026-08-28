package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.DefaultMessageAppScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun IncomingCallUIScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs: PreferenceManager = koinInject()
    var highlightedKey by remember { mutableStateOf(highlightKey) }
    val messageAppLabel = when (prefs.getString(PreferenceManager.KEY_DEFAULT_MESSAGE_APP, "sms")) {
        "whatsapp" -> "WhatsApp"
        "telegram" -> "Telegram"
        "ask"      -> "Always ask"
        else       -> "Messages / SMS"
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Incoming Call UI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsBackIconButton(onClick = { navigator.navigateUp() })
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)

            RivoAnimatedSection(delayMs = 0L) {
                    Column {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            // ── Default Message → separate page ───────────────────
                            RivoListItem(
                                headline = "Default Message",
                                supporting = "Currently: $messageAppLabel",
                                leadingIcon = Icons.Default.Send,
                                iconContainerColor = Color(0xFF29B6F6),
                                trailingIcon = Icons.Default.ChevronRight,
                                modifier = Modifier.settingsSearchHighlight("default_message_link", highlightedKey) { highlightedKey = null },
                                onClick = { navigator.navigate(DefaultMessageAppScreenDestination) }
                            )
                        }
                    }
            }

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
                                "Choose which app the Message quick action opens on the " +
                                    "incoming call screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
            }
        }
    }
}
