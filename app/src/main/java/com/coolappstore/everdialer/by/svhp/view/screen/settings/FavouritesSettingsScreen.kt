package com.coolappstore.everdialer.by.svhp.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FormatListBulleted
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
import com.coolappstore.everdialer.by.svhp.view.components.SettingsPillTopAppBar
import com.coolappstore.everdialer.by.svhp.view.components.rivoHeadingColor
import com.coolappstore.everdialer.by.svhp.view.components.settingsSearchHighlight
import com.coolappstore.everdialer.by.svhp.view.theme.SettingsTransitionStyle
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PfpShapeChooserDialog
import com.coolappstore.everdialer.by.svhp.view.theme.expressive.PfpShapeRegistry
import com.coolappstore.everdialer.by.svhp.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun FavouritesSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var favoritesInList by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FAVORITES_IN_LIST, false))
    }

    var favoritesPfpShape by remember(settingsVer) {
        mutableStateOf(prefs.getString(PreferenceManager.KEY_FAVORITES_PFP_SHAPE, "circle") ?: "circle")
    }
    var animateFavoritesShapeRotation by remember(settingsVer) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FAVORITES_PFP_SHAPE_ANIMATE, false))
    }
    var showPfpShapeDialog by remember { mutableStateOf(false) }

    if (showPfpShapeDialog) {
        PfpShapeChooserDialog(
            currentShapeKey = favoritesPfpShape,
            animateRotation = animateFavoritesShapeRotation,
            onAnimateRotationChange = {
                animateFavoritesShapeRotation = it
                prefs.setBoolean(PreferenceManager.KEY_FAVORITES_PFP_SHAPE_ANIMATE, it)
            },
            onSelectShape = {
                favoritesPfpShape = it
                prefs.setString(PreferenceManager.KEY_FAVORITES_PFP_SHAPE, it)
            },
            onDismiss = { showPfpShapeDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Favourites",
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
                        "Layout Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Show favourites in list",
                            supporting = "Display favourite contacts in a vertical list instead of a grid",
                            leadingIcon = Icons.Outlined.FormatListBulleted,
                            iconContainerColor = Color(0xFFE91E63),
                            checked = favoritesInList,
                            modifier = Modifier.settingsSearchHighlight("favorites_in_list", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                favoritesInList = it
                                prefs.setBoolean(PreferenceManager.KEY_FAVORITES_IN_LIST, it)
                            }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 20L) {
                Column {
                    Text(
                        "Profile Picture",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "PFP Shape",
                            supporting = PfpShapeRegistry.getDisplayLabel(favoritesPfpShape),
                            leadingIcon = if (favoritesPfpShape.contains("square")) Icons.Outlined.CropSquare else Icons.Outlined.AccountCircle,
                            iconContainerColor = Color(0xFF9C27B0),
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("favorites_pfp_shape", highlightedKey) { highlightedKey = null },
                            onClick = { showPfpShapeDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 20L) {
                SwipeActionConfigSection(sectionKey = "favourites")
            }
        }
    }
}
