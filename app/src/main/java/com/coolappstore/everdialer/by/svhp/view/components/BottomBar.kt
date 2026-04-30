package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@Composable
fun BottomBar(navController: NavController, navigator: DestinationsNavigator) {
    val prefs        = koinInject<PreferenceManager>()
    val context      = LocalContext.current
    val settingsState by prefs.settingsChanged.collectAsState()

    val pillNav      = prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true)
    val iconOnly     = prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)
    val notesEnabled = prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)
    val labelStyle: TextStyle = MaterialTheme.typography.labelMedium

    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isFavoritesSelected = currentDestination?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true
    val isRecentsSelected   = currentDestination?.hierarchy?.any { it.route == RecentScreenDestination.route } == true
    val isContactsSelected  = currentDestination?.hierarchy?.any { it.route == ContactScreenDestination.route } == true
    val isNotesSelected     = currentDestination?.hierarchy?.any { it.route == NotesScreenDestination.route } == true

    fun doHaptic() {
        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
            performAppHaptic(
                context,
                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
            )
        }
    }

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    if (pillNav) {
        // ── Pill floating bar – rendered directly in the Scaffold bottomBar slot ──
        // fillMaxWidth + contentAlignment = Center guarantees true horizontal centering.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape         = RoundedCornerShape(50.dp),
                color         = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 12.dp,
                tonalElevation  = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    PillNavItem(
                        selected       = isFavoritesSelected,
                        selectedIcon   = Icons.Filled.Favorite,
                        unselectedIcon = Icons.Outlined.FavoriteBorder,
                        label          = "Favourites",
                        iconOnly       = iconOnly,
                        onClick        = { doHaptic(); navigate(FavoritesScreenDestination.route) }
                    )
                    PillNavItem(
                        selected       = isRecentsSelected,
                        selectedIcon   = Icons.Filled.History,
                        unselectedIcon = Icons.Outlined.History,
                        label          = "Calls",
                        iconOnly       = iconOnly,
                        onClick        = { doHaptic(); navigate(RecentScreenDestination.route) }
                    )
                    PillNavItem(
                        selected       = isContactsSelected,
                        selectedIcon   = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person,
                        label          = "Contacts",
                        iconOnly       = iconOnly,
                        onClick        = { doHaptic(); navigate(ContactScreenDestination.route) }
                    )
                    if (notesEnabled) {
                        PillNavItem(
                            selected       = isNotesSelected,
                            selectedIcon   = Icons.Filled.Note,
                            unselectedIcon = Icons.Outlined.Note,
                            label          = "Notes",
                            iconOnly       = iconOnly,
                            onClick        = { doHaptic(); navigate(NotesScreenDestination.route) }
                        )
                    }
                }
            }
        }
    } else {
        // ── Standard bottom navigation bar ──────────────────────────────────────
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                icon = {
                    val size by animateDpAsState(if (isFavoritesSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "favSize")
                    Icon(if (isFavoritesSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Favourites", Modifier.size(size))
                },
                label          = if (iconOnly) null else ({ Text("Favourites", style = labelStyle) }),
                alwaysShowLabel = !iconOnly,
                selected       = isFavoritesSelected,
                colors         = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                onClick        = { doHaptic(); navigate(FavoritesScreenDestination.route) }
            )
            NavigationBarItem(
                icon = {
                    val size by animateDpAsState(if (isRecentsSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "callSize")
                    Icon(if (isRecentsSelected) Icons.Filled.History else Icons.Outlined.History, "Calls", Modifier.size(size))
                },
                label          = if (iconOnly) null else ({ Text("Calls", style = labelStyle) }),
                alwaysShowLabel = !iconOnly,
                selected       = isRecentsSelected,
                colors         = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                onClick        = { doHaptic(); navigate(RecentScreenDestination.route) }
            )
            NavigationBarItem(
                icon = {
                    val size by animateDpAsState(if (isContactsSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "contactSize")
                    Icon(if (isContactsSelected) Icons.Filled.Person else Icons.Outlined.Person, "Contacts", Modifier.size(size))
                },
                label          = if (iconOnly) null else ({ Text("Contacts", style = labelStyle) }),
                alwaysShowLabel = !iconOnly,
                selected       = isContactsSelected,
                colors         = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                onClick        = { doHaptic(); navigate(ContactScreenDestination.route) }
            )
            if (notesEnabled) {
                NavigationBarItem(
                    icon = {
                        val size by animateDpAsState(if (isNotesSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "notesSize")
                        Icon(if (isNotesSelected) Icons.Filled.Note else Icons.Outlined.Note, "Notes", Modifier.size(size))
                    },
                    label          = if (iconOnly) null else ({ Text("Notes", style = labelStyle) }),
                    alwaysShowLabel = !iconOnly,
                    selected       = isNotesSelected,
                    colors         = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    onClick        = { doHaptic(); navigate(NotesScreenDestination.route) }
                )
            }
        }
    }
}

@Composable
private fun PillNavItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    iconOnly: Boolean,
    onClick: () -> Unit
) {
    val iconSize          = 24.dp
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (iconOnly) 16.dp else 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (iconOnly) {
            Icon(
                imageVector     = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                modifier        = Modifier.size(iconSize),
                tint            = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                  else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector     = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    modifier        = Modifier.size(iconSize),
                    tint            = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selected) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
