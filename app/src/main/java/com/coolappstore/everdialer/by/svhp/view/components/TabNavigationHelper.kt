package com.coolappstore.everdialer.by.svhp.view.components

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecordingsScreenDestination

object TabNavigationHelper {

    fun routeForTabKey(key: String): String? = when (key) {
        "favorites"  -> FavoritesScreenDestination.route
        "calls"      -> RecentScreenDestination.route
        "contacts"   -> ContactScreenDestination.route
        "groups"     -> GroupsScreenDestination.route
        "recordings" -> RecordingsScreenDestination.route
        "notes"      -> NotesScreenDestination.route
        "dialpad"    -> DialPadScreenDestination.route
        else         -> null
    }

    fun tabKeyForRoute(route: String?): String? {
        if (route == null) return null
        val base = route.substringBefore("?").substringBefore("/")
        return when {
            base.contains(FavoritesScreenDestination.route, ignoreCase = true) -> "favorites"
            base.contains(RecentScreenDestination.route, ignoreCase = true)    -> "calls"
            base.contains(ContactScreenDestination.route, ignoreCase = true)   -> "contacts"
            base.contains(GroupsScreenDestination.route, ignoreCase = true)    -> "groups"
            base.contains(RecordingsScreenDestination.route, ignoreCase = true)-> "recordings"
            base.contains(NotesScreenDestination.route, ignoreCase = true)     -> "notes"
            base.contains(DialPadScreenDestination.route, ignoreCase = true)   -> "dialpad"
            else -> null
        }
    }

    fun isTabKeyVisible(key: String, prefs: PreferenceManager): Boolean = when (key) {
        "favorites"  -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES, true)
        "calls"      -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS, true)
        "contacts"   -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS, true)
        "groups"     -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS, false)
        "recordings" -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)
        "notes"      -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES, true)
        "dialpad"    -> prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD, false)
        else         -> false
    }

    /**
     * Returns the ordered list of routes for currently visible tabs based on user preferences.
     */
    fun getVisibleTabRoutes(prefs: PreferenceManager): List<String> {
        val configuredOrder = prefs.getTabOrder()
        return configuredOrder
            .filter { isTabKeyVisible(it, prefs) }
            .mapNotNull { routeForTabKey(it) }
    }

    /**
     * Navigates to a tab route using the standard bottom-bar / rail navigation pattern.
     */
    fun navigateToTab(navController: NavController, targetRoute: String) {
        if (targetRoute.contains(NotesScreenDestination.route, ignoreCase = true)) {
            navController.enterNotesTab()
            return
        }
        val currentDestination = navController.currentBackStackEntry?.destination
        val alreadyOnRoute = currentDestination?.hierarchy?.any {
            it.route?.substringBefore("?")?.substringBefore("/") ==
                targetRoute.substringBefore("?").substringBefore("/")
        } == true
        if (alreadyOnRoute) return

        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * Navigates to the adjacent tab (forward or backward in user's configured tab order).
     * @param goForward true to navigate to the tab to the right (next index), false to the left (prev index).
     */
    fun navigateAdjacentTab(
        navController: NavController,
        currentRoute: String,
        goForward: Boolean,
        prefs: PreferenceManager
    ) {
        val visibleRoutes = getVisibleTabRoutes(prefs)
        if (visibleRoutes.size <= 1) return

        val currentBase = currentRoute.substringBefore("?").substringBefore("/")
        val currentIdx = visibleRoutes.indexOfFirst {
            val base = it.substringBefore("?").substringBefore("/")
            currentBase.contains(base, ignoreCase = true) || base.contains(currentBase, ignoreCase = true)
        }
        if (currentIdx == -1) return

        val targetIdx = if (goForward) {
            (currentIdx + 1) % visibleRoutes.size
        } else {
            (currentIdx - 1 + visibleRoutes.size) % visibleRoutes.size
        }

        val targetRoute = visibleRoutes[targetIdx]
        navigateToTab(navController, targetRoute)
    }
}
