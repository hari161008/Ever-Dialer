package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ActionButtonsSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CustomBackgroundPickerScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun CallerUIScreen(navigator: DestinationsNavigator, highlightKey: String? = null) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var showOngoingCallUIWhenAnswered by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, true))
    }
    var showOngoingCallUIOnLockscreenWhenAnswered by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_ON_LOCKSCREEN_WHEN_ANSWERED, true))
    }

    val ongoingBgType = remember(settingsVersion) {
        prefs.getString(PreferenceManager.KEY_ONGOING_BG_TYPE, "none") ?: "none"
    }
    var showContactPfp by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, true))
    }
    var showForNoPfp by remember(settingsVersion) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_SHOW_FOR_NO_PFP, true))
    }
    var showPhoneNumber by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ONGOING_SHOW_PHONE_NUMBER, true))
    }

    val bgLabel = when (ongoingBgType) {
        "wallpaper" -> "Device Wallpaper"
        "picture"   -> "Custom Picture"
        "video"     -> "Custom Video"
        else        -> "None (Default)"
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar(
                title = "Ongoing Call UI",
                onBackClick = { navigator.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    com.coolappstore.everdialer.by.svhp.view.components.SettingsSearchEntryPoint(navigator = navigator)
                }

                // ── Show ongoing call UI when the call is answered ───────
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        Column {
                            Text(
                                "Full Screen Display",
                                style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Show ongoing call UI when the call is answered when phone is unlocked",
                                    supporting = "Display the full screen in-call screen when a call is answered",
                                    leadingIcon = Icons.Outlined.Call,
                                    iconContainerColor = Color(0xFF2196F3),
                                    checked = showOngoingCallUIWhenAnswered,
                                    modifier = Modifier.settingsSearchHighlight("show_ongoing_call_ui_when_answered", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        showOngoingCallUIWhenAnswered = it
                                        prefs.setBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_WHEN_ANSWERED, it)
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Show ongoing call UI screen in lockscreen when the call is answered",
                                    supporting = "Display the full screen in-call screen on lock screen after answering",
                                    leadingIcon = Icons.Outlined.Lock,
                                    iconContainerColor = Color(0xFF3F51B5),
                                    checked = showOngoingCallUIOnLockscreenWhenAnswered,
                                    modifier = Modifier.settingsSearchHighlight("show_ongoing_call_ui_on_lockscreen_when_answered", highlightedKey) { highlightedKey = null },
                                    onCheckedChange = {
                                        showOngoingCallUIOnLockscreenWhenAnswered = it
                                        prefs.setBoolean(PreferenceManager.KEY_SHOW_ONGOING_CALL_UI_ON_LOCKSCREEN_WHEN_ANSWERED, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Custom Background & Contact Photo ───────────────
                item {
                    RivoAnimatedSection(delayMs = 20L) {
                        Column {
                            Text(
                                "Appearance & Background",
                                style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
                            RivoExpressiveCard {
                                com.coolappstore.everdialer.by.svhp.view.components.RivoListItem(
                                    headline = "Background And Contact PFP Customisation",
                                    supporting = "Currently: $bgLabel",
                                    leadingIcon = Icons.Outlined.Wallpaper,
                                    iconContainerColor = Color(0xFF9C27B0),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("ongoing_custom_background", highlightedKey) { highlightedKey = null },
                                    onClick = { navigator.navigate(CustomBackgroundPickerScreenDestination(isIncoming = false)) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Show Contact PFP",
                                    supporting = "Display the contact avatar photo on the ongoing call screen",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = Color(0xFF00BCD4),
                                    checked = showContactPfp,
                                    onCheckedChange = {
                                        showContactPfp = it
                                        prefs.setBoolean(PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP, it)
                                    },
                                    modifier = Modifier.settingsSearchHighlight("ongoing_show_contact_pfp", highlightedKey) { highlightedKey = null }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                com.coolappstore.everdialer.by.svhp.view.components.RivoCheckboxListItem(
                                    headline = "Show PFP for non-contacts or non-PFP contacts",
                                    supporting = "Display custom contact PFP or default avatar for callers without a contact photo",
                                    leadingIcon = Icons.Outlined.Face,
                                    iconContainerColor = Color(0xFF4CAF50),
                                    checked = showForNoPfp,
                                    onCheckedChange = {
                                        showForNoPfp = it
                                        prefs.setBoolean(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_SHOW_FOR_NO_PFP, it)
                                    },
                                    modifier = Modifier.settingsSearchHighlight("ongoing_custom_pfp_show_for_no_pfp", highlightedKey) { highlightedKey = null }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                RivoSwitchListItem(
                                    headline = "Show Phone Number",
                                    supporting = "Display the phone number on the ongoing call screen",
                                    leadingIcon = Icons.Outlined.Call,
                                    iconContainerColor = Color(0xFF4CAF50),
                                    checked = showPhoneNumber,
                                    onCheckedChange = {
                                        showPhoneNumber = it
                                        prefs.setBoolean(PreferenceManager.KEY_ONGOING_SHOW_PHONE_NUMBER, it)
                                    },
                                    modifier = Modifier.settingsSearchHighlight("ongoing_show_phone_number", highlightedKey) { highlightedKey = null }
                                )
                            }
                        }
                    }
                }

                // ── Action Buttons ─────────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 40L) {
                        Column {
                            Text(
                                "Action Buttons",
                                style = MaterialTheme.typography.labelLarge,
                                color = rivoHeadingColor(),
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Action Buttons",
                                    supporting = "Customize ongoing call button layout, order, sizing, and visibility",
                                    leadingIcon = Icons.Default.Widgets,
                                    iconContainerColor = Color(0xFF673AB7),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    modifier = Modifier.settingsSearchHighlight("action_buttons", highlightedKey) { highlightedKey = null },
                                    onClick = {
                                        navigator.navigate(ActionButtonsSettingsScreenDestination())
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
