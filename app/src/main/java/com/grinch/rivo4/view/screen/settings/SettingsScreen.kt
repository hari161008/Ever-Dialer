package com.grinch.rivo4.view.screen.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.APP_VERSION
import com.grinch.rivo4.PATREON_URL
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.openLink
import com.grinch.rivo4.view.components.RivoAnimatedSection
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

// Semantic icon colors
private val ColorPurple   = Color(0xFF9C27B0)
private val ColorOrange   = Color(0xFFFF9800)
private val ColorBlue     = Color(0xFF2196F3)
private val ColorGreen    = Color(0xFF4CAF50)
private val ColorRed      = Color(0xFFE91E63)
private val ColorTeal     = Color(0xFF009688)
private val ColorIndigo   = Color(0xFF3F51B5)
private val ColorBluGrey  = Color(0xFF607D8B)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val prefs: PreferenceManager = koinInject()

    var hapticEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_feedback", true)) }
    var blockUnknown by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, false)) }
    var blockHidden by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, false)) }

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "settingsAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

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
            modifier = Modifier.fillMaxSize().padding(padding).alpha(alpha),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Support card ──────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 0L) {
                    RivoExpressiveCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        RivoListItem(
                            headline = "Support Ever Dialer",
                            supporting = "Help keep it open source & free",
                            leadingIcon = Icons.Default.Favorite,
                            iconContainerColor = ColorRed,
                            onClick = { openLink(context, PATREON_URL) }
                        )
                    }
                }
            }

            // ── Personalization ───────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 80L) {
                    SectionLabel("Personalization")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Interface",
                            supporting = "Themes, colors, and layout",
                            leadingIcon = Icons.Outlined.Palette,
                            iconContainerColor = ColorPurple,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { navigator.navigate(InterfaceScreenDestination) }
                        )
                        Divider()
                        RivoSwitchListItem(
                            headline = "Haptic Feedback",
                            supporting = "Vibrate on touch and gestures",
                            leadingIcon = Icons.Outlined.Vibration,
                            iconContainerColor = ColorOrange,
                            checked = hapticEnabled,
                            onCheckedChange = {
                                hapticEnabled = it
                                prefs.setBoolean("haptic_feedback", it)
                            }
                        )
                        Divider()
                        RivoListItem(
                            headline = "Sound & Vibration",
                            supporting = "Ringtones and dialpad tones",
                            leadingIcon = Icons.Outlined.VolumeUp,
                            iconContainerColor = ColorBlue,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { navigator.navigate(SoundVibrationScreenDestination) }
                        )
                    }
                }
            }

            // ── Calls & System ────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 160L) {
                    SectionLabel("Calls & System")
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Call Accounts",
                            supporting = "SIM cards and calling accounts",
                            leadingIcon = Icons.Outlined.SimCard,
                            iconContainerColor = ColorGreen,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { navigator.navigate(CallAccountsScreenDestination) }
                        )
                    }
                }
            }

            // ── Privacy ───────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 240L) {
                    SectionLabel("Privacy")
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Block Unknown Callers",
                            supporting = "Silence calls from unidentified numbers",
                            leadingIcon = Icons.Outlined.Block,
                            iconContainerColor = ColorRed,
                            checked = blockUnknown,
                            onCheckedChange = {
                                blockUnknown = it
                                prefs.setBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, it)
                            }
                        )
                        Divider()
                        RivoSwitchListItem(
                            headline = "Block Hidden Numbers",
                            supporting = "Silence private or withheld numbers",
                            leadingIcon = Icons.Outlined.VisibilityOff,
                            iconContainerColor = ColorIndigo,
                            checked = blockHidden,
                            onCheckedChange = {
                                blockHidden = it
                                prefs.setBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, it)
                            }
                        )
                    }
                }
            }

            // ── App info ──────────────────────────────────────────────
            item {
                RivoAnimatedSection(delayMs = 320L) {
                    RivoExpressiveCard(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        RivoListItem(
                            headline = "About Ever Dialer",
                            supporting = "Version $APP_VERSION · Developer info",
                            leadingIcon = Icons.Outlined.Info,
                            iconContainerColor = ColorBluGrey,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { navigator.navigate(AboutAppScreenDestination) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
