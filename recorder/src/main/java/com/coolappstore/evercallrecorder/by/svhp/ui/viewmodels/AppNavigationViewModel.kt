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
import com.coolappstore.evercallrecorder.by.svhp.onboarding.OnboardingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The "Brain" of the top-level navigation routing.
 *
 * Owns the [onboardingStatus] `StateFlow` that [AppNavigationScreen] observes to decide
 * which of three destinations to show (disclaimer → permissions → settings).
 *
 */
class AppNavigationViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Application context - safe to store in a ViewModel because it lives as long as the app
     * process, unlike an Activity context which is destroyed and recreated on every rotation.
     */
    private val appContext = application.applicationContext

    /**
     * Read and Manage AppPreferences
     */
    private val preferences = AppPreferences(appContext)

    // ------ Internal mutable state

    /**
     * Backing store for [onboardingStatus].
     * Updated by [refresh]; never exposed directly so external code cannot push arbitrary values.
     */
    private val _onboardingStatus = MutableStateFlow(
        OnboardingStatus.getStatus(appContext, preferences)
    )

    // ------ Public state (AppNavigationScreen watches this)

    /**
     * The current onboarding progress - a "Snapshot" of every permission and setup step.
     *
     * [AppNavigationScreen] uses `collectAsState()` to observe this flow; whenever a permission
     * is granted or the disclaimer is accepted, [refresh] pushes a new [OnboardingStatus.Status]
     * which triggers a refresh (recompose) and the router advances to the correct screen.
     */
    val onboardingStatus: StateFlow<OnboardingStatus.Status> = _onboardingStatus.asStateFlow()

    // ------ Refresh

    /**
     * Re-reads all permission and setup states from the system and updates [onboardingStatus].
     *
     * Should be called when the user returns to the app after granting a permission in the system
     * Settings app, or immediately after accepting the disclaimer / granting a permission
     * in-app, so the router advances to the next screen without delay.
     */
    fun refresh() {
        _onboardingStatus.update { OnboardingStatus.getStatus(appContext, preferences) }
    }
}
