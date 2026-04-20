package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.PATREON_URL
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val prefs: PreferenceManager = koinInject()

    // حالة الـ Haptic Feedback
    var hapticEnabled by remember {
        mutableStateOf(prefs.getBoolean("haptic_feedback", true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // المسافة بين الكروت
        ) {
            // Support Card (Primary Color)
            item {
                RivoExpressiveCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    RivoListItem(
                        headline = "Support Pdialer",
                        supporting = "Help us keep it open source",
                        leadingIcon = Icons.Default.Favorite,
                        onClick = { openLink(context, PATREON_URL) }
                    )
                }
            }

            // Appearance & Sound Group
            item {
                Text(
                    "Personalization",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Interface",
                        supporting = "Themes, colors, and layout",
                        leadingIcon = Icons.Outlined.Palette,
                        onClick = { navigator.navigate(InterfaceScreenDestination) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // خيار الـ Haptic Feedback الجديد
                    RivoSwitchListItem(
                        headline = "Haptic Feedback",
                        supporting = "Vibrate on touch and gestures",
                        leadingIcon = Icons.Outlined.Vibration,
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            prefs.setBoolean("haptic_feedback", it)
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    RivoListItem(
                        headline = "Sound & Vibration",
                        supporting = "Ringtones and dialpad sounds",
                        leadingIcon = Icons.Outlined.VolumeUp,
                        onClick = { navigator.navigate(SoundVibrationScreenDestination) }
                    )
                }
            }

            // Calls Group
            item {
                Text(
                    "Calls & System",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                RivoExpressiveCard {
                    RivoListItem(
                        headline = "Call Accounts",
                        supporting = "SIM cards and calling accounts",
                        leadingIcon = Icons.Outlined.SimCard,
                        onClick = { navigator.navigate(CallAccountsScreenDestination) }
                    )
                }
            }

            // About Group
            item {
                RivoExpressiveCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)) {
                    RivoListItem(
                        headline = "About App",
                        supporting = "Version and developer info",
                        leadingIcon = Icons.Outlined.Info,
                        onClick = { navigator.navigate(AboutAppScreenDestination) }
                    )
                }
            }
        }
    }
}
