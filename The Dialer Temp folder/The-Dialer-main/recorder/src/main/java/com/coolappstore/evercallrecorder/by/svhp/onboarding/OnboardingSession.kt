/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.onboarding

/**
 * Process-lifetime (in-memory only, never persisted to disk) flag tracking whether the user has
 * tapped "Skip" on the permissions screen.
 *
 * This is a plain Kotlin `object`, so it lives exactly as long as the app's process: it survives
 * Activity/ViewModel recreation (rotation, backgrounding while the OS keeps the process alive),
 * but is always reset back to `false` the moment the process is actually killed and restarted -
 * meaning a real app close + reopen will show the permissions screen again if anything is still
 * missing, exactly like a first-time run.
 */
object OnboardingSession {
    @Volatile
    var skipped: Boolean = false
}
