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

    data class MissedCallerEntry(
        val key: String,
        val number: String,
        val name: String?,
        var count: Int,
        var timestamp: Long
    )

    fun getNotifIdForKey(key: String): Int {
        return 4000 + (kotlin.math.abs(key.hashCode()) % 40000) + 1
    }

    fun getActiveMissedCalls(context: Context): MutableMap<String, MissedCallerEntry> {
        val prefs = PreferenceManager(context)
        val jsonStr = prefs.getString("active_missed_calls_map_json", "") ?: ""
        val map = mutableMapOf<String, MissedCallerEntry>()
        if (jsonStr.isBlank()) return map
        try {
            val arr = org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val key = obj.getString("key")
                val number = obj.optString("number", "")
                val name = if (obj.has("name") && !obj.isNull("name")) obj.getString("name") else null
                val count = obj.optInt("count", 1)
                val timestamp = obj.optLong("timestamp", 0L)
                map[key] = MissedCallerEntry(key, number, name, count, timestamp)
            }
        } catch (_: Throwable) {}
        return map
    }

    fun saveActiveMissedCalls(context: Context, map: Map<String, MissedCallerEntry>) {
        val prefs = PreferenceManager(context)
        val arr = org.json.JSONArray()
        map.values.forEach { entry ->
            val obj = org.json.JSONObject().apply {
                put("key", entry.key)
                put("number", entry.number)
                if (entry.name != null) put("name", entry.name)
                put("count", entry.count)
                put("timestamp", entry.timestamp)
            }
            arr.put(obj)
        }
        prefs.setString("active_missed_calls_map_json", arr.toString())
    }

    private fun lookupContactName(context: Context, number: String): String? {
        if (number.isBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun dismissCallerNotification(context: Context, callerKey: String) {
        val activeMap = getActiveMissedCalls(context)
        activeMap.remove(callerKey)
        saveActiveMissedCalls(context, activeMap)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(getNotifIdForKey(callerKey))
        val remainingTotal = activeMap.values.sumOf { it.count }
        applyOemBadges(context, remainingTotal)
    }

    fun clearAllMissedCallNotifications(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val activeMap = getActiveMissedCalls(context)
        activeMap.keys.forEach { key ->
            nm?.cancel(getNotifIdForKey(key))
        }
        activeMap.clear()
        saveActiveMissedCalls(context, activeMap)
        val prefs = PreferenceManager(context)
        prefs.setInt("active_ever_dialer_missed_count", 0)
        nm?.cancel(MISSED_CALLS_NOTIF_ID)
    }

    fun updateBadge(context: Context, explicitCount: Int? = null) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        try {
            val count = explicitCount ?: getUnreadMissedCallsCount(context)

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

            val prefs = PreferenceManager(context)
            val showNotif = prefs.getBoolean(PreferenceManager.KEY_MISSED_CALL_NOTIFICATION, true)
            val provider = prefs.getString(
                PreferenceManager.KEY_MISSED_CALL_NOTIFICATION_PROVIDER,
                PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM
            ) ?: PreferenceManager.MISSED_CALL_PROVIDER_SYSTEM

            if (!showNotif) {
                clearAllMissedCallNotifications(context)
                cancelSystemMissedCallNotification(context)
                markCallLogMissedAsReadSilently(context)
                applyOemBadges(context, 0)
                return
            }

            if (provider == PreferenceManager.MISSED_CALL_PROVIDER_EVER_DIALER) {
                cancelSystemMissedCallNotification(context)

                val unreadCalls = mutableListOf<Triple<String, String?, Long>>()
                val uri = CallLog.Calls.CONTENT_URI
                val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE)
                val selection = "${CallLog.Calls.TYPE} = ? AND (${CallLog.Calls.NEW} = 1 OR ${CallLog.Calls.IS_READ} = 0)"
                val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
                val sortOrder = "${CallLog.Calls.DATE} ASC"
                context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                    while (cursor.moveToNext()) {
                        val num = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                        unreadCalls.add(Triple(num, name, date))
                    }
                }

                if (unreadCalls.isNotEmpty()) {
                    markCallLogMissedAsReadSilently(context)
                }

                val activeMap = getActiveMissedCalls(context)

                for ((num, cachedName, date) in unreadCalls) {
                    val resolvedName = if (cachedName.isNullOrBlank() && num.isNotBlank()) {
                        lookupContactName(context, num)
                    } else cachedName

                    val key = if (num.isNotBlank()) {
                        android.telephony.PhoneNumberUtils.normalizeNumber(num).ifBlank { num.trim() }
                    } else "unknown_caller"

                    val existing = activeMap[key]
                    if (existing != null) {
                        existing.count++
                        if (date > existing.timestamp) existing.timestamp = date
                        if (existing.name.isNullOrBlank() && !resolvedName.isNullOrBlank()) {
                            activeMap[key] = existing.copy(name = resolvedName)
                        }
                    } else {
                        activeMap[key] = MissedCallerEntry(key, num, resolvedName, 1, date)
                    }
                }

                saveActiveMissedCalls(context, activeMap)
                val totalCount = activeMap.values.sumOf { it.count }

                nm.cancel(MISSED_CALLS_NOTIF_ID)

                if (totalCount > 0) {
                    activeMap.values.forEach { entry ->
                        val notifId = getNotifIdForKey(entry.key)
                        val displayName = entry.name?.ifBlank { null }
                            ?: entry.number.ifBlank { "Unknown" }

                        val title = if (entry.count >= 2) {
                            "Missed call from $displayName (${entry.count})"
                        } else {
                            "Missed call from $displayName"
                        }

                        val contentText = if (!entry.name.isNullOrBlank() && entry.name != entry.number && entry.number.isNotBlank()) {
                            if (entry.count >= 2) "${entry.number} • ${entry.count} missed calls" else entry.number
                        } else if (entry.count >= 2) {
                            "${entry.count} missed calls"
                        } else {
                            "Tap to view call history"
                        }

                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("NAV_TO_RECENTS", true)
                            putExtra("EXTRA_MISSED_CALLER_KEY", entry.key)
                        }
                        val openPendingIntent = PendingIntent.getActivity(
                            context,
                            notifId,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val dismissIntent = Intent(context, MissedCallDismissReceiver::class.java).apply {
                            putExtra(MissedCallDismissReceiver.EXTRA_CALLER_KEY, entry.key)
                        }
                        val dismissPendingIntent = PendingIntent.getBroadcast(
                            context,
                            notifId + 50000,
                            dismissIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val builder = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS_ID)
                            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
                            .setContentTitle(title)
                            .setContentText(contentText)
                            .setNumber(entry.count)
                            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                            .setAutoCancel(true)
                            .setContentIntent(openPendingIntent)
                            .setDeleteIntent(dismissPendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setWhen(entry.timestamp)
                            .setShowWhen(true)

                        if (entry.number.isNotBlank()) {
                            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${entry.number}")).apply {
                                setClass(context, MainActivity::class.java)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            val callPendingIntent = PendingIntent.getActivity(
                                context,
                                notifId + 100000,
                                callIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            builder.addAction(
                                android.R.drawable.stat_notify_missed_call,
                                "Call back",
                                callPendingIntent
                            )

                            val msgIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${entry.number}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            val msgPendingIntent = PendingIntent.getActivity(
                                context,
                                notifId + 150000,
                                msgIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            builder.addAction(
                                android.R.drawable.sym_action_chat,
                                "Message",
                                msgPendingIntent
                            )
                        }

                        val notification = builder.build()
                        try {
                            val extraNotificationField = notification.javaClass.getDeclaredField("extraNotification")
                            val extraNotification = extraNotificationField.get(notification)
                            val setMessageCountMethod = extraNotification.javaClass.getDeclaredMethod("setMessageCount", Int::class.javaPrimitiveType)
                            setMessageCountMethod.invoke(extraNotification, entry.count)
                        } catch (_: Throwable) {}

                        nm.notify(notifId, notification)
                    }
                }
                applyOemBadges(context, totalCount)
            } else {
                clearAllMissedCallNotifications(context)
                applyOemBadges(context, count)
            }
        } catch (_: Throwable) {}
    }

    fun markMissedCallsAsRead(context: Context) {
        clearAllMissedCallNotifications(context)
        markCallLogMissedAsReadSilently(context)
        cancelSystemMissedCallNotification(context)
        applyOemBadges(context, 0)
    }

    fun markCallLogMissedAsReadSilently(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
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
    }

    fun cancelSystemMissedCallNotification(context: Context) {
        val cancelAction = {
            try {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tm?.cancelMissedCallsNotification()
                }
            } catch (_: Throwable) {}
        }
        cancelAction()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({ cancelAction() }, 300)
        handler.postDelayed({ cancelAction() }, 800)
        handler.postDelayed({ cancelAction() }, 1500)
        handler.postDelayed({ cancelAction() }, 2500)
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
