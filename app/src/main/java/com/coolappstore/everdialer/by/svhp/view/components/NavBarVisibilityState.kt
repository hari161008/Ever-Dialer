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
}
