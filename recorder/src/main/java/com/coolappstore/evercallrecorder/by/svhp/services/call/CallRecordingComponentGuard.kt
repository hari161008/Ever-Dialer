/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 */

package com.coolappstore.evercallrecorder.by.svhp.services.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger

/**
 * Keeps the manifest-declared call-monitoring components — the telephony state broadcast
 * receiver and the notification-listener service used for WhatsApp/Telegram call detection —
 * fully enabled/disabled in lockstep with what the user has actually turned on, so nothing
 * from this module sits resident in memory / drains battery for a feature that isn't in use:
 *
 * - [PhoneStateReceiver] is a plain manifest [android.content.BroadcastReceiver]. It costs
 *   nothing while idle (the OS only wakes it up for an actual PHONE_STATE broadcast), so it
 *   only needs to track the universal master switch ([AppPreferences.isCallRecordingEnabled]).
 * - [AppCallNotificationListenerService] is a *bound* [android.service.notification.NotificationListenerService].
 *   Unlike the receiver, once enabled the OS keeps it resident and bound for as long as
 *   notification access is granted — that is the actual "always running in the background"
 *   cost users notice. It is only needed to detect WhatsApp/Telegram VoIP calls, so it's kept
 *   disabled unless the master switch is on AND at least one of those app-call recording
 *   toggles ([AppPreferences.isAnyAppCallRecordingEnabled]) is actually turned on.
 */
object CallRecordingComponentGuard {

    private const val TAG = "SCR:ComponentGuard"

    /** Call at app startup, and again immediately after any of the relevant switches change. */
    fun sync(context: Context) {
        val appContext = context.applicationContext
        val prefs = AppPreferences(appContext)
        val recordingEnabled = prefs.isCallRecordingEnabled()
        val notificationListenerNeeded = recordingEnabled && prefs.isAnyAppCallRecordingEnabled()

        setComponentEnabled(appContext, PhoneStateReceiver::class.java, recordingEnabled)
        setComponentEnabled(appContext, AppCallNotificationListenerService::class.java, notificationListenerNeeded)

        if (!recordingEnabled) {
            try {
                appContext.stopService(Intent(appContext, RecordingForegroundService::class.java))
            } catch (_: Exception) {}
        }
    }

    private fun setComponentEnabled(context: Context, clazz: Class<*>, enabled: Boolean) {
        try {
            val componentName = ComponentName(context, clazz)
            val newState = if (enabled)
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to set enabled=$enabled for ${clazz.simpleName}: ${e.message}")
        }
    }
}
