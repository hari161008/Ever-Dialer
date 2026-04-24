package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
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
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val iconOnly      = prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)
    val notesEnabled  = prefs.getBoolean(PreferenceManager.KEY_NOTES_ENABLED, true)

    // Inherit font family + size from the current MaterialTheme typography
    val labelStyle: TextStyle = MaterialTheme.typography.labelMedium

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isFavoritesSelected = currentDestination?.hierarchy?.any { it.route == FavoritesScreenDestination.route } == true
    val isRecentsSelected   = currentDestination?.hierarchy?.any { it.route == RecentScreenDestination.route } == true
    val isContactsSelected  = currentDestination?.hierarchy?.any { it.route == ContactScreenDestination.route } == true
    val isNotesSelected     = currentDestination?.hierarchy?.any { it.route == NotesScreenDestination.route } == true

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        NavigationBarItem(
            icon = {
                val size by animateDpAsState(if (isFavoritesSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "favSize")
                Icon(if (isFavoritesSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Favourites", Modifier.size(size))
            },
            label = if (iconOnly) null else ({ Text("Favourites", style = labelStyle) }),
            alwaysShowLabel = !iconOnly,
            selected = isFavoritesSelected,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            onClick = {
                navController.navigate(FavoritesScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                val size by animateDpAsState(if (isRecentsSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "callSize")
                Icon(if (isRecentsSelected) Icons.Filled.History else Icons.Outlined.History, "Calls", Modifier.size(size))
            },
            label = if (iconOnly) null else ({ Text("Calls", style = labelStyle) }),
            alwaysShowLabel = !iconOnly,
            selected = isRecentsSelected,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            onClick = {
                navController.navigate(RecentScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = {
                val size by animateDpAsState(if (isContactsSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "contactSize")
                Icon(if (isContactsSelected) Icons.Filled.Person else Icons.Outlined.Person, "Contacts", Modifier.size(size))
            },
            label = if (iconOnly) null else ({ Text("Contacts", style = labelStyle) }),
            alwaysShowLabel = !iconOnly,
            selected = isContactsSelected,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            onClick = {
                navController.navigate(ContactScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }
        )
        if (notesEnabled) {
            NavigationBarItem(
                icon = {
                    val size by animateDpAsState(if (isNotesSelected) 27.dp else 22.dp, spring(stiffness = Spring.StiffnessMedium), label = "notesSize")
                    Icon(if (isNotesSelected) Icons.Filled.Note else Icons.Outlined.Note, "Notes", Modifier.size(size))
                },
                label = if (iconOnly) null else ({ Text("Notes", style = labelStyle) }),
                alwaysShowLabel = !iconOnly,
                selected = isNotesSelected,
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = {
                    navController.navigate(NotesScreenDestination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                }
            )
        }
    }
}
