package com.modiprincekr.thedialer.view.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ramcosta.composedestinations.generated.destinations.NotesScreenDestination

/**
 * Shared flag that lets a tab screen (e.g. the Recordings tab while it's showing the
 * bundled Ever Call Recorder's disclaimer/permissions onboarding) temporarily hide the
 * bottom navigation bar (pill or standard), since those onboarding screens have their
 * own bottom-anchored "Continue" button that the nav bar would otherwise cover.
 */
object NavBarVisibilityState {
    var hideForOnboarding by mutableStateOf(false)

    /**
     * True while the Recordings tab's own selection pill (Favourite / Recover / Assign
     * contact / Recordings / Share, shown on long-pressing a recording) is visible, so the
     * bottom nav pill can smoothly slide away instead of overlapping it.
     */
    var hideForSelectionMode by mutableStateOf(false)

    /**
     * True while the recordings list is showing as a screen pushed from Settings → Call
     * Recording, rather than as the "Recordings" bottom-nav tab itself. Both routes render the
     * exact same single screen, but the bottom pill/nav bar should stay hidden in the
     * Settings-pushed case since the user isn't switching tabs there — they're drilling into a
     * detail screen and expect a normal "back" flow.
     */
    var hideForSettingsEntry by mutableStateOf(false)

    /**
     * True while the dialpad overlay is open, so the bottom bar (pill or standard navigation
     * bar) smoothly hides and doesn't overlap or show beneath the dialpad.
     */
    var hideForDialpad by mutableStateOf(false)

    /**
     * True while a tab screen is showing a single highlighted result it was opened into from
     * unified Search (e.g. Notes opened from a "Notes" search hit) rather than as the normal
     * bottom-nav tab. The bottom pill/nav bar and that screen's own search bar pill both stay
     * hidden for the lifetime of this highlighted view, matching the Settings-entry behaviour
     * above, since the user is viewing one specific search match rather than browsing the tab.
     */
    var hideForSearchResult by mutableStateOf(false)
}

/**
 * Navigates to the Notes tab as a normal bottom-nav tab entry (tab tap, nav rail, swipe-to-Notes)
 * — always landing on the plain Notes view with its search bar and the bottom nav pill visible.
 *
 * Uses the exact same popUpTo(start destination, saveState = true) + launchSingleTop pattern every
 * other tab uses, so the back stack stays shallow and switching to/from other tabs keeps working
 * normally. The one difference is restoreState = false: Notes always pushes a brand new instance
 * with highlightQuery = null explicitly, instead of risking a restored saved instance that could
 * still be carrying a stale highlightQuery (and hidden chrome) from an earlier unified-Search visit.
 */
fun NavController.enterNotesTab() {
    val alreadyShowingNormalNotes =
        currentDestination?.hierarchy?.any { it.route == NotesScreenDestination.route } == true &&
            !NavBarVisibilityState.hideForSearchResult
    NavBarVisibilityState.hideForSearchResult = false
    if (alreadyShowingNormalNotes) return

    navigate(NotesScreenDestination(highlightQuery = null).route) {
        // Same popUpTo(start destination, saveState = true) pattern every other tab uses, so the
        // back stack never grows unbounded and other tabs keep behaving normally. restoreState is
        // deliberately left false (unlike other tabs) so Notes never restores a previously-saved
        // instance that could still be carrying a stale highlightQuery from an earlier Search visit.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = false
    }
}
