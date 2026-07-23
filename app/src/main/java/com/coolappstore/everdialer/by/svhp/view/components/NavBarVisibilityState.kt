package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
     * True while a tab screen is showing a single highlighted result it was opened into from
     * unified Search (e.g. Notes opened from a "Notes" search hit) rather than as the normal
     * bottom-nav tab. The bottom pill/nav bar and that screen's own search bar pill both stay
     * hidden for the lifetime of this highlighted view, matching the Settings-entry behaviour
     * above, since the user is viewing one specific search match rather than browsing the tab.
     */
    var hideForSearchResult by mutableStateOf(false)
}
