/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.system.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * PermissionChecks centralises all runtime permission queries used throughout the app.
 */
object PermissionChecks {

    /**
     * Returns true if the app is allowed to post notifications.
     *
     * @param context The app context.
     * @return true if the app can post notifications.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns true if [Manifest.permission.READ_PHONE_STATE] is granted.
     *
     * This permission is required to:
     *  - Receive [android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER] in broadcasts.
     *  - Call [android.telephony.TelephonyManager.getCallState] programmatically.
     *
     * @param context The app context.
     * @return true if the phone-state permission is currently granted.
     */
    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns true if [Manifest.permission.READ_CALL_LOG] is granted.
     *
     * Required to read call logs and receive deprecated phone number in phone state broadcast intents.
     *
     * @param context The app context.
     * @return true if the read-call-log permission is currently granted.
     */
    fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns true if [Manifest.permission.READ_CONTACTS] is granted.
     *
     * Required for:
     *  - [ContactLookup.isKnownContact] queries against the Contacts provider.
     *  - Loading the contact list in the picker dialog.
     *
     * @param context The app context.
     * @return true if the contacts permission is currently granted.
     */
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns true if the app is exempt from Android's battery-optimisation restrictions.
     *
     * @param context The app context.
     * @return true if the app is on the battery-optimisation whitelist (or if the API is unavailable).
     */
    fun hasBatteryExemption(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Returns true if the user has granted this app Notification Access (i.e. it is an enabled
     * [android.service.notification.NotificationListenerService] listener).
     *
     * Required by [com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallNotificationListenerService]
     * to detect ongoing calls from messaging apps (see "Record calls from apps" in Settings).
     *
     * @param context The app context.
     * @return true if this app's package is among the system's enabled notification listeners.
     */
    fun hasNotificationListenerPermission(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return context.packageName in enabledPackages
    }
}
