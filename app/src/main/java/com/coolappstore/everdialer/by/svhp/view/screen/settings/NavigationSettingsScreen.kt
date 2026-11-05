package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.*
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorIndigo = Color(0xFF3F51B5)
private val ColorTeal   = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun NavigationSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var defaultTab by remember(settingsVer) {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_DEFAULT_TAB, "calls") ?: "calls")
    }

    var showTabSectionsDialog by remember { mutableStateOf(false) }

    var scrollIndication by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_INDICATION, true))
    }
    var pillNav by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true))
    }
    var slideToOpen by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SLIDE_TO_OPEN, true))
    }
    var iconOnlyNav by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false))
    }

    data class TabOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val allTabOptions = remember {
        buildList {
            add(TabOption("favorites",  "Favourites", Icons.Outlined.FavoriteBorder))
            add(TabOption("calls",      "Calls",      Icons.Outlined.History))
            add(TabOption("contacts",   "Contacts",   Icons.Outlined.Person))
            add(TabOption("sms",        "SMS",        Icons.Outlined.Chat))
            add(TabOption("groups",     "Groups",     Icons.Outlined.Group))
            if (com.coolappstore.everdialer.by.svhp.feature.AppFeatures.isCallRecorderAvailable) {
                add(TabOption("recordings", "Recordings", Icons.Outlined.FiberManualRecord))
            }
            add(TabOption("notes",      "Note",       Icons.Outlined.Note))
            add(TabOption("dialpad",    "Dialpad",    Icons.Outlined.Dialpad))
        }
    }

    fun isTabEnabled(key: String): Boolean {
        return when (key) {
            "favorites"  -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES, true)
            "calls"      -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS, true)
            "contacts"   -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS, true)
            "sms"        -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_SMS, true)
            "groups"     -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS, false)
            "recordings" -> com.coolappstore.everdialer.by.svhp.feature.AppFeatures.isCallRecorderAvailable && prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)
            "notes"      -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES, true)
            "dialpad"    -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD, false)
            else         -> true
        }
    }

    val availableTabOptions = remember(settingsVer) {
        allTabOptions.filter { option ->
            option.key != "dialpad" && isTabEnabled(option.key)
        }
    }

    LaunchedEffect(availableTabOptions, defaultTab) {
        if (availableTabOptions.isNotEmpty() && availableTabOptions.none { it.key == defaultTab }) {
            val fallback = availableTabOptions.first().key
            defaultTab = fallback
            prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, fallback)
        }
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Navigation",
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
                        "Navigation Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Tab Sections",
                            supporting = "Toggle and drag to reorder tabs in the navigation bar",
                            leadingIcon = Icons.Default.ViewWeek,
                            iconContainerColor = ColorIndigo,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("tab_sections", highlightedKey) { highlightedKey = null },
                            onClick = { showTabSectionsDialog = true }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoListItem(
                            headline = "Default Tab Section",
                            supporting = "Choose which tab opens when the app starts (currently: ${allTabOptions.firstOrNull { it.key == defaultTab }?.label ?: "Calls"})",
                            leadingIcon = Icons.Default.Tab,
                            iconContainerColor = ColorIndigo,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("default_tab_section", highlightedKey) { highlightedKey = null },
                            onClick = { showDefaultTabDialog = true }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Scroll Indication",
                            supporting = "Show a floating line indicator below tabs when horizontally scrollable",
                            leadingIcon = Icons.Outlined.LinearScale,
                            iconContainerColor = ColorIndigo,
                            checked = scrollIndication,
                            modifier = Modifier.settingsSearchHighlight("scroll_indication", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                scrollIndication = it
                                prefs.setBoolean(PreferenceManager.KEY_SCROLL_INDICATION, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Pill Style Navigation",
                            supporting = "Show a floating pill-style nav bar instead of the standard bottom bar",
                            leadingIcon = Icons.Outlined.ViewStream,
                            iconContainerColor = ColorTeal,
                            checked = pillNav,
                            modifier = Modifier.settingsSearchHighlight("pill_style_nav", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                pillNav = it
                                prefs.setBoolean(PreferenceManager.KEY_PILL_NAV, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Slide to Open",
                            supporting = "Drag the highlighted pill to slide across tabs and switch on release",
                            leadingIcon = Icons.Outlined.Swipe,
                            iconContainerColor = ColorTeal,
                            checked = slideToOpen,
                            modifier = Modifier.settingsSearchHighlight("slide_to_open", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                slideToOpen = it
                                prefs.setBoolean(PreferenceManager.KEY_SLIDE_TO_OPEN, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Icon-Only Bottom Bar",
                            supporting = "Removes text labels from navigation",
                            leadingIcon = Icons.Outlined.ViewStream,
                            iconContainerColor = ColorTeal,
                            checked = iconOnlyNav,
                            modifier = Modifier.settingsSearchHighlight("icon_only_bottom_bar", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                iconOnlyNav = it
                                prefs.setBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, it)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDefaultTabDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultTabDialog = false },
            icon = { Icon(Icons.Default.Tab, null, tint = ColorIndigo) },
            title = { Text("Default Tab Section") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose which tab opens when the app starts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    availableTabOptions.forEach { option ->
                        val isSelected = defaultTab == option.key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        defaultTab = option.key
                                        prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, option.key)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        defaultTab = option.key
                                        prefs.setString(PreferenceManager.KEY_DEFAULT_TAB, option.key)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultTabDialog = false }) { Text("Done") }
            }
        )
    }

    if (showTabSectionsDialog) {
        TabSectionsDialog(
            onDismissRequest = { showTabSectionsDialog = false }
        )
    }
}
