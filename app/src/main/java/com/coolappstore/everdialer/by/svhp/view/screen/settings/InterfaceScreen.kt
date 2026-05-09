package com.coolappstore.everdialer.by.svhp.view.screen.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import android.os.Build
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoListItem
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val ColorPurple = Color(0xFF9C27B0)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)
private val ColorIndigo = Color(0xFF3F51B5)

data class ThemeOption(val key: String, val label: String)

private val themeOptions = listOf(
    ThemeOption("auto",    "Auto"),
    ThemeOption("light",   "Light"),
    ThemeOption("dark",    "Dark"),
    ThemeOption("white",   "White"),
    ThemeOption("black",   "Black"),
    ThemeOption("auto_bw", "Auto B/W")
)

private fun triggerRestartPrompt(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: android.content.Context
) {
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = "Restart required to apply theme changes fully.",
            actionLabel = "Restart",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            (context as? Activity)?.recreate()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InterfaceScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var themeMode           by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto") }
    var dynamicColors       by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)) }
    var showFirstLetter     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)) }
    var colorfulAvatars     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)) }
    var showPicture         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)) }
    var iconOnlyNav         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)) }
    var pillNav             by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true)) }
    var customPrimaryColor  by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }
    var showIncomingCallUI  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_INCOMING_CALL_UI, true)) }
    var showCallerUI        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALLER_UI, true)) }
    var openDialpadDefault  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) }
    var scrollAnimation     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, true)) }
    var liquidGlass         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false)) }
    var blurEffects         by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false)) }

    // Call UI section checkboxes dialog
    var showCallUIDialog   by remember { mutableStateOf(false) }
    var callUIShowToday    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY, true)) }
    var callUIShowMissed   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED, true)) }
    var callUIShowOutgoing by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, true)) }
    var callUIShowCallTime by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, true)) }

    // Default Tab dialog
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var defaultTab           by remember { mutableStateOf(prefs.getString(PreferenceManager.KEY_DEFAULT_TAB, "calls") ?: "calls") }
    data class TabOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tabOptions = listOf(
        TabOption("favorites", "Favourites", Icons.Outlined.FavoriteBorder),
        TabOption("calls",     "Calls",      Icons.Outlined.History),
        TabOption("contacts",  "Contacts",   Icons.Outlined.Person),
        TabOption("notes",     "Note",       Icons.Outlined.Note)
    )

    var hexInput by remember { mutableStateOf(String.format("%06X", 0xFFFFFF and customPrimaryColor)) }
    var hexError by remember { mutableStateOf(false) }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60), Color(0xFF436916),
        Color(0xFF984061), Color(0xFF006874), Color(0xFF705D00), Color(0xFFBF0031),
        Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B)
    )

    fun applyHexColor(hex: String) {
        val cleaned = hex.trimStart('#').uppercase()
        if (cleaned.length == 6) {
            try {
                val colorInt = android.graphics.Color.parseColor("#$cleaned")
                customPrimaryColor = colorInt
                prefs.setInt("custom_primary_color", colorInt)
                hexError = false
                triggerRestartPrompt(scope, snackbarHostState, context)
            } catch (_: Exception) { hexError = true }
        } else {
            hexError = true
        }
    }

    // ── Call UI Dialog ────────────────────────────────────────────────────────
    if (showCallUIDialog) {
        AlertDialog(
            onDismissRequest = { showCallUIDialog = false },
            icon = { Icon(Icons.Default.Dashboard, null, tint = ColorBlue) },
            title = { Text("Call UI Elements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Toggle which stat cards appear in the Calls home screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        Triple("Today", callUIShowToday) { v: Boolean ->
                            callUIShowToday = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_TODAY, v)
                        },
                        Triple("Missed", callUIShowMissed) { v: Boolean ->
                            callUIShowMissed = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_MISSED, v)
                        },
                        Triple("Outgoing", callUIShowOutgoing) { v: Boolean ->
                            callUIShowOutgoing = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_OUTGOING, v)
                        },
                        Triple("Call Time", callUIShowCallTime) { v: Boolean ->
                            callUIShowCallTime = v
                            prefs.setBoolean(PreferenceManager.KEY_CALL_UI_SHOW_CALL_TIME, v)
                        }
                    ).forEach { (label, checked, onChange) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = onChange,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCallUIDialog = false }) { Text("Done") }
            }
        )
    }

    // ── Default Tab Dialog ─────────────────────────────────────────────────
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
                    tabOptions.forEach { option ->
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Interface", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── App Theme ────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        Column {
                            Text("App Theme", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Color Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(12.dp))
                                    themeOptions.chunked(3).forEach { rowItems ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            rowItems.forEach { option ->
                                                val selected = themeMode == option.key
                                                Surface(
                                                    onClick = {
                                                        themeMode = option.key
                                                        prefs.setString(PreferenceManager.KEY_THEME_MODE, option.key)
                                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                                    },
                                                    shape = RoundedCornerShape(50),
                                                    color = if (selected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.weight(1f).height(38.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(option.label, style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Theme Colors ──────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 60L) {
                        Column {
                            Text("Theme Colors", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Dynamic Colors",
                                    supporting = "Wallpaper based app color theming",
                                    leadingIcon = Icons.Outlined.Palette,
                                    iconContainerColor = ColorPurple,
                                    checked = dynamicColors,
                                    onCheckedChange = {
                                        dynamicColors = it
                                        prefs.setBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, it)
                                        triggerRestartPrompt(scope, snackbarHostState, context)
                                    }
                                )
                                if (!dynamicColors) {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Primary Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(12.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(presetColors) { color ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .border(
                                                            width = if (customPrimaryColor == color.toArgb()) 3.dp else 0.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            customPrimaryColor = color.toArgb()
                                                            prefs.setInt("custom_primary_color", color.toArgb())
                                                            hexInput = String.format("%06X", 0xFFFFFF and color.toArgb())
                                                            hexError = false
                                                            triggerRestartPrompt(scope, snackbarHostState, context)
                                                        }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                                    try { Color(android.graphics.Color.parseColor("#${hexInput.trimStart('#')}")) }
                                                    catch (_: Exception) { Color.Gray }
                                                )
                                            )
                                            OutlinedTextField(
                                                value = hexInput,
                                                onValueChange = { v ->
                                                    hexInput = v.trimStart('#').uppercase().take(6)
                                                    hexError = false
                                                },
                                                label = { Text("Hex Color") },
                                                prefix = { Text("#") },
                                                isError = hexError,
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = {
                                                    applyHexColor(hexInput)
                                                    keyboardController?.hide()
                                                }),
                                                shape = RoundedCornerShape(12.dp),
                                                supportingText = if (hexError) {{ Text("Enter a valid 6-digit hex code") }} else null
                                            )
                                            Button(onClick = {
                                                applyHexColor(hexInput)
                                                keyboardController?.hide()
                                            }, shape = RoundedCornerShape(12.dp)) {
                                                Text("Apply")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Liquid Glass ─────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 80L) {
                        Column {
                            Text("Visual Effects", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                RivoExpressiveCard {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Lens,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                "Not supported on this device",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Blur and Liquid Glass require Android 12 or higher",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            } else {
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Material Liquid You Glass",
                                    supporting = "Apply a liquid glass refraction effect to navigation and menus",
                                    leadingIcon = Icons.Outlined.Lens,
                                    iconContainerColor = Color(0xFF00BCD4),
                                    checked = liquidGlass,
                                    onCheckedChange = {
                                        liquidGlass = it
                                        prefs.setBoolean(PreferenceManager.KEY_LIQUID_GLASS, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Elements to have liquid glass effect",
                                    supporting = "Choose which UI elements use the liquid glass effect",
                                    leadingIcon = Icons.Outlined.Layers,
                                    iconContainerColor = Color(0xFF0097A7),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {
                                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.LiquidGlassElementsScreenDestination)
                                    }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Material Blur Effects",
                                    supporting = "Apply a background blur effect to navigation and menus",
                                    leadingIcon = Icons.Outlined.BlurOn,
                                    iconContainerColor = Color(0xFF5C6BC0),
                                    checked = blurEffects,
                                    onCheckedChange = {
                                        blurEffects = it
                                        prefs.setBoolean(PreferenceManager.KEY_BLUR_EFFECTS, it)
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Elements to have blur effect",
                                    supporting = "Choose which UI elements use the blur effect",
                                    leadingIcon = Icons.Outlined.Layers,
                                    iconContainerColor = Color(0xFF3949AB),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {
                                        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.BlurEffectsElementsScreenDestination)
                                    }
                                )
                            }
                            }
                        }
                    }
                }

                // ── Call UI ───────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 100L) {
                        Column {
                            Text("Call UI", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoListItem(
                                    headline = "Incoming Call UI",
                                    supporting = "Customize the incoming call screen appearance",
                                    leadingIcon = Icons.Outlined.CallReceived,
                                    iconContainerColor = ColorGreen,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = {}
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // ── Caller UI → separate page ─────────────────────────
                                RivoListItem(
                                    headline = "Caller UI",
                                    supporting = "Customize the in-call screen layout and controls",
                                    leadingIcon = Icons.Outlined.Person,
                                    iconContainerColor = ColorBlue,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.CallerUIScreenDestination) }
                                )

                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Calls Section Elements",
                                    supporting = "Toggle Today, Missed, Outgoing, Call Time cards",
                                    leadingIcon = Icons.Default.Dashboard,
                                    iconContainerColor = ColorOrange,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showCallUIDialog = true }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoListItem(
                                    headline = "Default Tab Section",
                                    supporting = "Choose which tab opens when the app starts (currently: ${tabOptions.firstOrNull { it.key == defaultTab }?.label ?: "Calls"})",
                                    leadingIcon = Icons.Default.Tab,
                                    iconContainerColor = ColorIndigo,
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { showDefaultTabDialog = true }
                                )
                            }
                        }
                    }
                }

                // ── Animations ───────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 115L) {
                        Column {
                            Text("Animations", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Scroll Animation",
                                    supporting = "Fade-in animation for list items as you scroll",
                                    leadingIcon = Icons.Outlined.Animation,
                                    iconContainerColor = ColorBlue,
                                    checked = scrollAnimation,
                                    onCheckedChange = {
                                        scrollAnimation = it
                                        prefs.setBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── UI Element Visibility ────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 130L) {
                        Column {
                            Text("UI Elements", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Pill Style Navigation",
                                    supporting = "Show a floating pill-style nav bar instead of the standard bottom bar",
                                    leadingIcon = Icons.Outlined.ViewStream,
                                    iconContainerColor = ColorTeal,
                                    checked = pillNav,
                                    onCheckedChange = {
                                        pillNav = it
                                        prefs.setBoolean(PreferenceManager.KEY_PILL_NAV, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Avatars ──────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 160L) {
                        Column {
                            Text("Avatars", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Show First Letter in Avatar",
                                    supporting = "Displays letter when picture is missing",
                                    leadingIcon = Icons.Outlined.TextFields,
                                    iconContainerColor = ColorAmber,
                                    checked = showFirstLetter,
                                    onCheckedChange = { showFirstLetter = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Use Colorful Avatars",
                                    supporting = "Random colors based on contact name",
                                    leadingIcon = Icons.Outlined.ColorLens,
                                    iconContainerColor = ColorBlue,
                                    checked = colorfulAvatars,
                                    onCheckedChange = { colorfulAvatars = it; prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Show Picture in Avatar",
                                    supporting = "Shows the contact picture if available",
                                    leadingIcon = Icons.Outlined.AccountCircle,
                                    iconContainerColor = ColorGreen,
                                    checked = showPicture,
                                    onCheckedChange = { showPicture = it; prefs.setBoolean(PreferenceManager.KEY_SHOW_PICTURE, it) }
                                )
                            }
                        }
                    }
                }

                // ── Navigation ───────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 220L) {
                        Column {
                            Text("Navigation", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                            RivoExpressiveCard {
                                RivoSwitchListItem(
                                    headline = "Icon-Only Bottom Bar",
                                    supporting = "Removes text labels from navigation",
                                    leadingIcon = Icons.Outlined.ViewStream,
                                    iconContainerColor = ColorTeal,
                                    checked = iconOnlyNav,
                                    onCheckedChange = { iconOnlyNav = it; prefs.setBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, it) }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                RivoSwitchListItem(
                                    headline = "Open Dialpad by Default",
                                    supporting = "Show dialpad automatically when app starts",
                                    leadingIcon = Icons.Outlined.Dialpad,
                                    iconContainerColor = ColorAmber,
                                    checked = openDialpadDefault,
                                    onCheckedChange = {
                                        openDialpadDefault = it
                                        prefs.setBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, it)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── App Icon ─────────────────────────────────────────
                item {
                    Column {
                        Text(
                            "App Icon",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        RivoExpressiveCard {
                            RivoListItem(
                                headline = "App Icon",
                                supporting = "Choose the app icon displayed on your home screen",
                                leadingIcon = Icons.Outlined.Apps,
                                iconContainerColor = ColorIndigo,
                                onClick = {
                                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.AppIconScreenDestination)
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }


        }
    }
}
