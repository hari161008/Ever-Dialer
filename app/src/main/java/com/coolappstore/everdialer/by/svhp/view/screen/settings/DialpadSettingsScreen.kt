package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorBlue   = Color(0xFF2196F3)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorPurple = Color(0xFF9C27B0)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun DialpadSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var dialpadMemory by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, true)) }
    var showCallLogsInDialpadSearchList by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALL_LOGS_IN_DIALPAD_SEARCH_LIST, true)) }
    var openDialpadDefault by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) }
    var circleButtons by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, false)) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Dialpad",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Dialpad Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Dialpad Memory",
                            supporting = "Keep the typed number in the Dialpad after closing it or calling",
                            leadingIcon = Icons.Outlined.Dialpad,
                            iconContainerColor = ColorBlue,
                            checked = dialpadMemory,
                            modifier = Modifier.settingsSearchHighlight("dialpad_memory", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                dialpadMemory = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show call logs in the dialpad search list",
                            supporting = "Show recent call logs in the search list when nothing is typed or searched",
                            leadingIcon = Icons.Outlined.History,
                            iconContainerColor = ColorTeal,
                            checked = showCallLogsInDialpadSearchList,
                            modifier = Modifier.settingsSearchHighlight("show_call_logs_in_dialpad_search_list", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showCallLogsInDialpadSearchList = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_CALL_LOGS_IN_DIALPAD_SEARCH_LIST, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Open Dialpad by Default",
                            supporting = "Show dialpad automatically when app starts",
                            leadingIcon = Icons.Outlined.Dialpad,
                            iconContainerColor = ColorAmber,
                            checked = openDialpadDefault,
                            modifier = Modifier.settingsSearchHighlight("open_dialpad_default", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                openDialpadDefault = it
                                prefs.setBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, it)
                            }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 40L) {
                Column {
                    Text(
                        "Buttons Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Circle buttons",
                            supporting = "Use circular buttons on the dialpad",
                            leadingIcon = Icons.Outlined.Lens,
                            iconContainerColor = ColorPurple,
                            checked = circleButtons,
                            modifier = Modifier.settingsSearchHighlight("circle_buttons", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                circleButtons = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, it)
                            }
                        )
                    }
                }
            }
        }
    }
}
