package com.coolappstore.everdialer.by.svhp.controller.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.MainActivity

object MissedCallBadgeManager {
    const val CHANNEL_MISSED_CALLS_ID = "channel_missed_calls"
    const val MISSED_CALLS_NOTIF_ID = 4001

    fun getUnreadMissedCallsCount(context: Context): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return 0
        }
        return try {
            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(CallLog.Calls._ID)
            val selection = "${CallLog.Calls.TYPE} = ? AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)"
            val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use {
                it.count
            } ?: 0
        } catch (_: Throwable) {
            0
        }
    }

    private fun getLatestMissedCallInfo(context: Context): Pair<String, String>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER)
            val selection = "${CallLog.Calls.TYPE} = ? AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)"
            val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
            val sortOrder = "${CallLog.Calls.DATE} DESC"
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val num = if (numIdx >= 0) cursor.getString(numIdx) else ""
                    Pair(name?.ifBlank { num } ?: num, num)
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun updateBadge(context: Context, explicitCount: Int? = null) {
        val count = explicitCount ?: getUnreadMissedCallsCount(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_MISSED_CALLS_ID,
                "Missed Calls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications and home screen badges for missed calls"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }

        if (count > 0) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAV_TO_RECENTS", true)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                9901,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, MissedCallDismissReceiver::class.java)
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                9902,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val latestInfo = getLatestMissedCallInfo(context)
            val caller = latestInfo?.first?.ifBlank { "Unknown" } ?: "Unknown"
            val title = if (count == 1) "Missed call from $caller" else "$count Missed Calls"
            val contentText = if (count == 1) "Tap to view call history" else "Latest missed call from $caller"

            val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS_ID)
                .setSmallIcon(android.R.drawable.stat_notify_missed_call)
                .setContentTitle(title)
                .setContentText(contentText)
                .setNumber(count) // Tells launcher to display badge number on home screen app icon
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notification = builder.build()

            // Xiaomi MIUI badge reflection
            try {
                val extraNotificationField = notification.javaClass.getDeclaredField("extraNotification")
                val extraNotification = extraNotificationField.get(notification)
                val setMessageCountMethod = extraNotification.javaClass.getDeclaredMethod("setMessageCount", Int::class.javaPrimitiveType)
                setMessageCountMethod.invoke(extraNotification, count)
            } catch (_: Throwable) {}

            nm.notify(MISSED_CALLS_NOTIF_ID, notification)
            applyOemBadges(context, count)
        } else {
            nm.cancel(MISSED_CALLS_NOTIF_ID)
            applyOemBadges(context, 0)
        }
    }

    fun markMissedCallsAsRead(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            updateBadge(context, 0)
            return
        }
        try {
            val values = ContentValues().apply {
                put(CallLog.Calls.NEW, 0)
                put(CallLog.Calls.IS_READ, 1)
            }
            val where = "${CallLog.Calls.TYPE} = ? AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)"
            val args = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
            context.contentResolver.update(CallLog.Calls.CONTENT_URI, values, where, args)
        } catch (_: Throwable) {}
        updateBadge(context, 0)
    }

    private fun getLauncherActivityName(context: Context): String {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(context.packageName)
            }
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.name ?: MainActivity::class.java.name
        } catch (_: Throwable) {
            MainActivity::class.java.name
        }
    }

    private fun applyOemBadges(context: Context, count: Int) {
        val packageName = context.packageName
        val className = getLauncherActivityName(context)

        // 1. Samsung / LG / Generic Intent
        try {
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", packageName)
                putExtra("badge_count_class_name", className)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}

        // 2. Samsung ContentProvider
        try {
            val contentUri = Uri.parse("content://com.sec.badge/apps")
            val contentValues = ContentValues().apply {
                put("package", packageName)
                put("class", className)
                put("badgecount", count)
            }
            context.contentResolver.insert(contentUri, contentValues)
        } catch (_: Throwable) {}

        // 3. Sony
        try {
            val intent = Intent("com.sonyericsson.home.action.UPDATE_BADGE").apply {
                putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", packageName)
                putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", className)
                putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", if (count > 0) count.toString() else null)
                putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}

        // 4. Huawei
        try {
            val bundle = Bundle().apply {
                putString("package", packageName)
                putString("class", className)
                putInt("badgenumber", count)
            }
            context.contentResolver.call(
                Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                "change_badge",
                null,
                bundle
            )
        } catch (_: Throwable) {}

        // 5. Vivo
        try {
            val intent = Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM").apply {
                putExtra("packageName", packageName)
                putExtra("className", className)
                putExtra("notificationNum", count)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}

        // 6. Oppo
        try {
            val bundle = Bundle().apply {
                putInt("app_badge_count", count)
            }
            context.contentResolver.call(
                Uri.parse("content://com.android.badge/badge"),
                "setAppBadgeCount",
                null,
                bundle
            )
        } catch (_: Throwable) {}

        // 7. HTC
        try {
            val intent = Intent("com.htc.launcher.action.UPDATE_SHORTCUT").apply {
                putExtra("packagename", packageName)
                putExtra("count", count)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {}
    }
}
