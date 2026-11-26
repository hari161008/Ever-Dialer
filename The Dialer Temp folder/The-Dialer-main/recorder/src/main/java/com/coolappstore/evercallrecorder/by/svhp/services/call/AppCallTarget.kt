/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.services.call

import com.coolappstore.evercallrecorder.by.svhp.R

/**
 * Messaging apps whose VoIP calls [AppCallNotificationListenerService] knows how to detect, so they
 * can be auto-recorded the same way normal telephony calls are (see "Record calls from apps" in Settings).
 *
 * Detection relies on the *ongoing call* notification these apps post while a voice/video call is active
 * (see [AppCallNotificationListenerService] for how it is recognised). [packageNames] lists every package
 * id known to post that notification for the given app, so renamed/business variants are covered too.
 *
 * @property key             Stable identifier used for logging/preferences plumbing.
 * @property displayNameResId String resource for the name shown in the Settings UI.
 * @property packageNames    All known package ids for this app (main + known variants).
 */
enum class AppCallTarget(
    val key: String,
    val displayNameResId: Int,
    val packageNames: List<String>
) {
    WHATSAPP(
        key = "whatsapp",
        displayNameResId = R.string.app_call_target_whatsapp,
        packageNames = listOf("com.whatsapp", "com.whatsapp.w4b")
    ),
    TELEGRAM(
        key = "telegram",
        displayNameResId = R.string.app_call_target_telegram,
        packageNames = listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")
    );

    companion object {
        /**
         * Finds the [AppCallTarget] (if any) that owns [packageName].
         * @param packageName The package name from a [android.service.notification.StatusBarNotification].
         */
        fun fromPackageName(packageName: String): AppCallTarget? =
            entries.firstOrNull { packageName in it.packageNames }
    }
}
