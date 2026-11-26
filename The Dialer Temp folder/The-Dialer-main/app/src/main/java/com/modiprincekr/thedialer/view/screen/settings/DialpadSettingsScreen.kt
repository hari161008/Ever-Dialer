package com.modiprincekr.thedialer.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.components.RivoAnimatedSection
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.RivoListItem
import com.modiprincekr.thedialer.view.components.RivoSwitchListItem
import com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar
import com.modiprincekr.thedialer.view.components.rivoHeadingColor
import com.modiprincekr.thedialer.view.components.settingsSearchHighlight
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.SpeedDialScreenDestination
import com.ramcosta.composedestinations.generated.destinations.UssdCodesScreenDestination
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
    val settingsState by prefs.settingsChanged.collectAsState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var dialpadMemory by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, true)) }
    var showCallLogsInDialpadSearchList by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALL_LOGS_IN_DIALPAD_SEARCH_LIST, true)) }
    var openDialpadDefault by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) }
    var dialpadHeightScale by remember { mutableStateOf(prefs.getDialpadHeightScale()) }
    var dialpadButtonStyle by remember { mutableStateOf(prefs.getDialpadButtonStyle()) }
    var showButtonStyleDialog by remember { mutableStateOf(false) }

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
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        // Adjust dialpad height
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Adjust dialpad height",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val percent = (dialpadHeightScale * 100).toInt()
                                    val subtitle = "$percent%" + if (percent == 100) " (Default)" else ""
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (dialpadHeightScale != 1.0f) {
                                    TextButton(
                                        onClick = {
                                            dialpadHeightScale = 1.0f
                                            prefs.setDialpadHeightScale(1.0f)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Slider(
                                value = dialpadHeightScale,
                                onValueChange = {
                                    dialpadHeightScale = ((it * 100).toInt()) / 100f
                                    prefs.setDialpadHeightScale(dialpadHeightScale)
                                },
                                valueRange = 0.75f..1.15f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        // Button style
                        val buttonStyleHeadline = when (dialpadButtonStyle) {
                            PreferenceManager.DIALPAD_BUTTON_STYLE_CIRCLE -> "Circle buttons"
                            PreferenceManager.DIALPAD_BUTTON_STYLE_GRID -> "Grid buttons"
                            else -> "Squircle buttons"
                        }
                        RivoListItem(
                            headline = "Button style",
                            supporting = buttonStyleHeadline,
                            leadingIcon = Icons.Outlined.Lens,
                            iconContainerColor = ColorPurple,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("dialpad_button_style", highlightedKey) { highlightedKey = null },
                            onClick = { showButtonStyleDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 20L) {
                Column {
                    Text(
                        "Speed Dial",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Speed Dial",
                            supporting = "Assign contacts to keys 2–9 and voicemail to key 1 for quick calling via long press",
                            leadingIcon = Icons.Outlined.Speed,
                            iconContainerColor = ColorTeal,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("speed_dial", highlightedKey) { highlightedKey = null },
                            onClick = { navigator.navigate(SpeedDialScreenDestination()) }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 30L) {
                Column {
                    Text(
                        "Shortcuts & Codes",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "USSD & Secret Codes",
                            supporting = "Carrier codes, balance checks, IMEI, and hardware test diagnostics",
                            leadingIcon = Icons.Outlined.Numbers,
                            iconContainerColor = ColorPurple,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("ussd_codes", highlightedKey) { highlightedKey = null },
                            onClick = { navigator.navigate(UssdCodesScreenDestination()) }
                        )
                    }
                }
            }
        }
    }

    if (showButtonStyleDialog) {
        AlertDialog(
            onDismissRequest = { showButtonStyleDialog = false },
            icon = { Icon(Icons.Outlined.Lens, null, tint = ColorPurple) },
            title = { Text("Button Style") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose the shape and appearance of dialpad buttons.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        Triple(
                            PreferenceManager.DIALPAD_BUTTON_STYLE_CIRCLE,
                            "Circle buttons",
                            "Classic round dialpad keys"
                        ),
                        Triple(
                            PreferenceManager.DIALPAD_BUTTON_STYLE_SQUIRCLE,
                            "Squircle buttons",
                            "Smooth rounded square dialpad keys"
                        ),
                        Triple(
                            PreferenceManager.DIALPAD_BUTTON_STYLE_GRID,
                            "Grid buttons",
                            "Continuous hairline grid layout without individual button bubbles"
                        )
                    ).forEach { (key, title, subtitle) ->
                        val isSelected = dialpadButtonStyle == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dialpadButtonStyle = key
                                    prefs.setDialpadButtonStyle(key)
                                    showButtonStyleDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        dialpadButtonStyle = key
                                        prefs.setDialpadButtonStyle(key)
                                        showButtonStyleDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showButtonStyleDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
