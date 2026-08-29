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
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
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
    var showFullScreenCallUIOnAnyApps by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_FULL_SCREEN_INCOMING_ON_ANY_APPS, false))
    }

    var incomingBgType by remember {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_INCOMING_BG_TYPE, "none") ?: "none")
    }
    var showContactPfp by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP, true))
    }

    var showBgOptionsPopup by remember { mutableStateOf(false) }
    var editorMediaState by remember {
        mutableStateOf<Triple<java.io.File, Boolean, String>?>(null)
    }

    val bgLabel = when (incomingBgType) {
        "wallpaper" -> "Device Wallpaper"
        "picture"   -> "Custom Picture"
        "video"     -> "Custom Video"
        else        -> "None (Default)"
    }

    val messageAppLabel = when (prefs.getString(PreferenceManager.KEY_DEFAULT_MESSAGE_APP, "sms")) {
        "whatsapp" -> "WhatsApp"
        "telegram" -> "Telegram"
        "ask"      -> "Always ask"
        else       -> "Messages / SMS"
    }

    if (showBgOptionsPopup) {
        com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundOptionsPopup(
            target = com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundTarget.INCOMING,
            currentType = incomingBgType,
            onDismiss = { showBgOptionsPopup = false },
            onSelectNone = {
                incomingBgType = "none"
                prefs.setString(PreferenceManager.KEY_INCOMING_BG_TYPE, "none")
                prefs.setString(PreferenceManager.KEY_INCOMING_BG_PATH, "")
            },
            onOpenEditor = { file, isVideo, bgType ->
                showBgOptionsPopup = false
                editorMediaState = Triple(file, isVideo, bgType)
            }
        )
    }

    editorMediaState?.let { (file, isVideo, bgType) ->
        com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundEditorDialog(
            target = com.coolappstore.everdialer.by.svhp.view.components.CustomBackgroundTarget.INCOMING,
            mediaFile = file,
            isVideo = isVideo,
            bgType = bgType,
            initialZoom = prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_ZOOM, 1f),
            initialPanX = prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_PAN_X, 0f),
            initialPanY = prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_PAN_Y, 0f),
            initialDim = prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_DIM, 0f),
            initialBlur = prefs.getFloat(PreferenceManager.KEY_INCOMING_BG_BLUR, 0f),
            onDismiss = { editorMediaState = null },
            onSaveSuccess = {
                incomingBgType = bgType
                editorMediaState = null
            }
        )
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

            // ── Show Full screen call UI on any apps ───────────────
            RivoAnimatedSection(delayMs = 0L) {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = "Show Full screen call UI on any apps",
                        supporting = "Open full screen incoming call UI over any app when a call rings",
                        leadingIcon = Icons.Default.Call,
                        iconContainerColor = Color(0xFF4CAF50),
                        checked = showFullScreenCallUIOnAnyApps,
                        onCheckedChange = {
                            showFullScreenCallUIOnAnyApps = it
                            prefs.setBoolean(PreferenceManager.KEY_SHOW_FULL_SCREEN_INCOMING_ON_ANY_APPS, it)
                        },
                        modifier = Modifier.settingsSearchHighlight("show_fullscreen_call_ui_on_any_apps", highlightedKey) { highlightedKey = null }
                    )
                }
            }

            // ── Custom Background & Contact Photo ───────────────
            RivoAnimatedSection(delayMs = 25L) {
                Column {
                    Text(
                        "Appearance & Background",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Choose Custom Background",
                            supporting = "Currently: $bgLabel",
                            leadingIcon = Icons.Outlined.Wallpaper,
                            iconContainerColor = Color(0xFF9C27B0),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("incoming_custom_background", highlightedKey) { highlightedKey = null },
                            onClick = { showBgOptionsPopup = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show Contact PFP",
                            supporting = "Display the caller's avatar photo over the incoming call screen",
                            leadingIcon = Icons.Outlined.AccountCircle,
                            iconContainerColor = Color(0xFF00BCD4),
                            checked = showContactPfp,
                            onCheckedChange = {
                                showContactPfp = it
                                prefs.setBoolean(PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP, it)
                            },
                            modifier = Modifier.settingsSearchHighlight("incoming_show_contact_pfp", highlightedKey) { highlightedKey = null }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 40L) {
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

