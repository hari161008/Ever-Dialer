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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.components.RivoAnimatedSection
import com.coolappstore.everdialer.by.svhp.view.components.RivoExpressiveCard
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import com.coolappstore.everdialer.by.svhp.view.components.RivoSwitchListItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollToTopButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val ColorPurple = Color(0xFF9C27B0)
private val ColorIndigo = Color(0xFF3F51B5)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorBlue   = Color(0xFF2196F3)
private val ColorGreen  = Color(0xFF4CAF50)
private val ColorOrange = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InterfaceScreen(navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    var dynamicColors by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)) }
    var amoledMode by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)) }
    var showFirstLetter by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)) }
    var colorfulAvatars by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)) }
    var showPicture by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)) }
    var iconOnlyNav by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)) }
    var customPrimaryColor by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60), Color(0xFF436916),
        Color(0xFF984061), Color(0xFF006874), Color(0xFF705D00), Color(0xFFBF0031)
    )

    fun showRestartPrompt() {
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
                // ── Theme ───────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 0L) {
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Material You Theming",
                                supporting = "Wallpaper based app color theming",
                                leadingIcon = Icons.Outlined.Palette,
                                iconContainerColor = ColorPurple,
                                checked = dynamicColors,
                                onCheckedChange = {
                                    dynamicColors = it
                                    prefs.setBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, it)
                                    showRestartPrompt()
                                }
                            )

                            if (!dynamicColors) {
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Primary Color", style = MaterialTheme.typography.labelLarge)
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
                                                        showRestartPrompt()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline = "Amoled Dark Mode",
                                supporting = "Uses pitch black for UI elements",
                                leadingIcon = Icons.Outlined.DarkMode,
                                iconContainerColor = ColorIndigo,
                                checked = amoledMode,
                                onCheckedChange = {
                                    amoledMode = it
                                    prefs.setBoolean(PreferenceManager.KEY_AMOLED_MODE, it)
                                    showRestartPrompt()
                                }
                            )
                        }
                    }
                }

                // ── Avatars ──────────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 100L) {
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Show First Letter in Avatar",
                                supporting = "Displays letter when picture is missing",
                                leadingIcon = Icons.Outlined.TextFields,
                                iconContainerColor = ColorAmber,
                                checked = showFirstLetter,
                                onCheckedChange = {
                                    showFirstLetter = it
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, it)
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline = "Use Colorful Avatars",
                                supporting = "Random colors based on contact name",
                                leadingIcon = Icons.Outlined.ColorLens,
                                iconContainerColor = ColorBlue,
                                checked = colorfulAvatars,
                                onCheckedChange = {
                                    colorfulAvatars = it
                                    prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it)
                                }
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            RivoSwitchListItem(
                                headline = "Show Picture in Avatar",
                                supporting = "Shows the contact picture if available",
                                leadingIcon = Icons.Outlined.AccountCircle,
                                iconContainerColor = ColorGreen,
                                checked = showPicture,
                                onCheckedChange = {
                                    showPicture = it
                                    prefs.setBoolean(PreferenceManager.KEY_SHOW_PICTURE, it)
                                }
                            )
                        }
                    }
                }

                // ── Navigation ───────────────────────────────────────
                item {
                    RivoAnimatedSection(delayMs = 200L) {
                        RivoExpressiveCard {
                            RivoSwitchListItem(
                                headline = "Icon-Only Bottom Bar",
                                supporting = "Removes text labels from navigation",
                                leadingIcon = Icons.Outlined.ViewStream,
                                iconContainerColor = ColorTeal,
                                checked = iconOnlyNav,
                                onCheckedChange = {
                                    iconOnlyNav = it
                                    prefs.setBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, it)
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            ScrollToTopButton(
                visible = showButton,
                onClick = { scope.launch { listState.animateScrollToItem(0) } }
            )
        }
    }
}
