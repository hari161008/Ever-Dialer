package com.coolappstore.everdialer.by.svhp.controller.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.APP_VERSION
import com.coolappstore.everdialer.by.svhp.GITHUB_API_RELEASES
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

object UpdateNotificationManager {

    const val ACTION_UPDATE_CHECK_TRIGGER = "com.coolappstore.everdialer.by.svhp.UPDATE_CHECK_TRIGGER"
    const val CHANNEL_UPDATES_ID = "app_updates_channel"
    private const val REQUEST_CODE_ALARM = 4135
    private const val REQUEST_CODE_NOTIF = 4136
    private const val NOTIFICATION_ID_UPDATE = 4137

    /**
     * Schedules the next periodic background check for updates based on the user's configured interval.
     * Cancels existing scheduled alarms if Notify Updates is disabled.
     */
    fun scheduleUpdateCheck(context: Context) {
        val prefs = PreferenceManager(context)
        if (!prefs.isNotifyUpdatesEnabled()) {
            cancelUpdateCheck(context)
            return
        }

        val intervalValue = prefs.getNotifyUpdatesValue().coerceAtLeast(1)
        val intervalUnit = prefs.getNotifyUpdatesUnit() // "hours" or "days"

        val durationMillis = when (intervalUnit.lowercase(Locale.ROOT)) {
            "days" -> intervalValue * 24 * 60 * 60 * 1000L
            else -> intervalValue * 60 * 60 * 1000L // default "hours"
        }

        val triggerAt = System.currentTimeMillis() + durationMillis
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = getAlarmPendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * Cancels any scheduled background update checks.
     */
    fun cancelUpdateCheck(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(getAlarmPendingIntent(context))
    }

    private fun getAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, UpdateNotificationReceiver::class.java).apply {
            action = ACTION_UPDATE_CHECK_TRIGGER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)
    }

    /**
     * Asynchronously checks GitHub for newer releases and sends a notification with version details
     * if an update is found. Clicking the notification opens the Updates screen.
     */
    fun checkUpdatesAndNotify(context: Context) {
        val prefs = PreferenceManager(context)
        if (!prefs.isNotifyUpdatesEnabled()) {
            cancelUpdateCheck(context)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!prefs.isNotifyUpdatesEnabled()) {
                    cancelUpdateCheck(context)
                    return@launch
                }

                val latest = fetchLatestRelease(GITHUB_API_RELEASES)
                if (latest != null && isNewerVersion(latest.tagName, APP_VERSION)) {
                    postUpdateNotification(context, latest)
                }
            } catch (e: Throwable) {
                android.util.Log.e("UpdateNotifManager", "Failed checking updates in background", e)
            } finally {
                if (prefs.isNotifyUpdatesEnabled()) {
                    scheduleUpdateCheck(context)
                } else {
                    cancelUpdateCheck(context)
                }
            }
        }
    }

    private fun postUpdateNotification(context: Context, latest: ReleaseInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new Ever Dialer versions and updates"
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }

        // Tap action: open MainActivity and navigate to Updates screen
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.coolappstore.everdialer.OPEN_UPDATES"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_NOTIF,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Update Available • v${latest.tagName}"
        val summary = "New version v${latest.tagName} is available (Current: v$APP_VERSION). Tap to view and install."
        val expandedDetails = buildString {
            append("New version v").append(latest.tagName).append(" is ready to install.")
            append("\nCurrently installed: v").append(APP_VERSION)
            if (!latest.releaseNotes.isNullOrBlank()) {
                append("\n\nWhat's New:\n").append(latest.releaseNotes.take(300))
                if (latest.releaseNotes.length > 300) append("…")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES_ID)
            .setSmallIcon(R.drawable.ic_notif_call_default)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedDetails))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIFICATION_ID_UPDATE, notification)
    }
}
