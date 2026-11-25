/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the App Lock gate is currently satisfied for this app session.
 *
 * This is intentionally separate from [AppPreferences] (which only remembers whether App Lock
 * is *configured*): [isUnlocked] is in-memory only, so the app re-locks every time the process
 * is recreated, and [AppNavigationScreen][com.coolappstore.evercallrecorder.by.svhp.AppNavigationScreen]
 * re-locks it whenever the activity goes to the background (see [lock]).
 *
 * Because this is a `ViewModel`, the unlocked state survives configuration changes (e.g. screen
 * rotation) without forcing the user to re-authenticate.
 */
class AppLockViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application.applicationContext)

    // Start unlocked unless App Lock is configured - first launch with no lock set up should
    // never show the gate.
    private val _isUnlocked = MutableStateFlow(!preferences.isAppLockEnabled())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    /** Marks the session as authenticated; called after a successful PIN/password/biometric check. */
    fun unlock() { _isUnlocked.value = true }

    /**
     * Re-arms the gate so the next time the app is shown it requires authentication again.
     * No-ops when App Lock isn't currently enabled, so disabling it never leaves a stray lock
     * screen behind.
     */
    fun lock() {
        if (preferences.isAppLockEnabled()) _isUnlocked.value = false
    }
}
