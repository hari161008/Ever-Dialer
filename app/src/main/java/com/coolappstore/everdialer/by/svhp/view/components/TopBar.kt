package com.coolappstore.everdialer.by.svhp.view.components

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

/** The pill-shaped, non-editable "Search in Ever Dialer" bar — tapping it opens the single
 *  unified [SearchScreenDestination] (contacts, non-contacts, contact notes, recording notes).
 *  Shown at the top of the main tabs (via [TopBar]) as well as Settings, Notes, and Recordings,
 *  so every entry point into search looks and behaves identically. */
@Composable
fun SearchBarPill(navigator: DestinationsNavigator, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val searchSource = remember { MutableInteractionSource() }
    val searchPressed by searchSource.collectIsPressedAsState()
    val searchScale by animateFloatAsState(
        targetValue = if (searchPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "searchScale"
    )
    Surface(
        onClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
            }
            navigator.navigate(SearchScreenDestination)
        },
        modifier = modifier.height(52.dp).scale(searchScale),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = searchSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Search in Ever Dialer",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TopBar(navController: NavController, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val prefs = koinInject<PreferenceManager>()
    // In landscape, Settings is reachable from the NavigationRail, and each tab screen hosts
    // its own search bar inline within its scrollable content (so it scrolls away with the
    // rest of the list instead of staying pinned) — so the shared top bar renders nothing here.
    if (isLandscape) {
        // Reserve the same top inset the fixed bar used to occupy so page content lines up
        // the same as before, without pinning a non-scrolling search bar on screen.
        Spacer(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars))
        return
    }
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarAlpha"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else (-16).dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "topBarOffset"
    )
    LaunchedEffect(Unit) { visible = true }

    // Settings button press animation
    val settingsSource = remember { MutableInteractionSource() }
    val settingsPressed by settingsSource.collectIsPressedAsState()
    val settingsScale by animateFloatAsState(
        targetValue = if (settingsPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "settingsScale"
    )

    // Search bar press animation


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .alpha(alpha)
            .offset(y = offsetY),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search bar
            SearchBarPill(navigator = navigator, modifier = Modifier.weight(1f))

            // Settings button – coloured icon background
            Surface(
                onClick = {
                    if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                        performAppHaptic(context, prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light", prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f))
                    }
                    navigator.navigate(SettingsScreenDestination)
                },
                modifier = Modifier.size(52.dp).scale(settingsScale),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                interactionSource = settingsSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
