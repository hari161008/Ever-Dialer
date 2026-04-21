package com.grinch.rivo4.view.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grinch.rivo4.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@Composable
fun BottomBar(navController: NavController, navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val iconOnly = prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isContactsSelected = currentDestination?.hierarchy?.any { it.route == ContactScreenDestination.route } == true
    val isRecentsSelected = currentDestination?.hierarchy?.any { it.route == RecentScreenDestination.route } == true

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = {
                val size by animateDpAsState(
                    targetValue = if (isContactsSelected) 26.dp else 22.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "contactsIconSize"
                )
                Icon(
                    if (isContactsSelected) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Contacts",
                    modifier = Modifier.size(size)
                )
            },
            label = if (iconOnly) null else ({ Text("Contacts") }),
            alwaysShowLabel = !iconOnly,
            selected = isContactsSelected,
            onClick = {
                navController.navigate(ContactScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = {
                val size by animateDpAsState(
                    targetValue = if (isRecentsSelected) 26.dp else 22.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "recentsIconSize"
                )
                Icon(
                    if (isRecentsSelected) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "Recents",
                    modifier = Modifier.size(size)
                )
            },
            label = if (iconOnly) null else ({ Text("Recents") }),
            alwaysShowLabel = !iconOnly,
            selected = isRecentsSelected,
            onClick = {
                navController.navigate(RecentScreenDestination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
