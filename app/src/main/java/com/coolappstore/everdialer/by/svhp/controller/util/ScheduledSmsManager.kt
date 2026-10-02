package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.coolappstore.everdialer.by.svhp.controller.sms.ScheduledSmsReceiver
import com.coolappstore.everdialer.by.svhp.modal.data.ScheduledSmsEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages scheduling, cancelling, and persisting scheduled SMS messages.
 */
object ScheduledSmsManager {
    const val ACTION_TRIGGER = "com.coolappstore.everdialer.by.svhp.SCHEDULED_SMS_TRIGGER"
    const val ACTION_BOOT    = "com.coolappstore.everdialer.by.svhp.SCHEDULED_SMS_RESCHEDULE"
    const val EXTRA_ID       = "scheduled_sms_id"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun loadEntries(prefs: PreferenceManager): List<ScheduledSmsEntry> {
        val raw = prefs.getString(PreferenceManager.KEY_SCHEDULED_SMS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ScheduledSmsEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(prefs: PreferenceManager, entries: List<ScheduledSmsEntry>) {
        prefs.setString(PreferenceManager.KEY_SCHEDULED_SMS, json.encodeToString(entries))
    }

    fun findEntry(prefs: PreferenceManager, id: String): ScheduledSmsEntry? =
        loadEntries(prefs).find { it.id == id }

    fun getEntriesForThreadOrAddress(prefs: PreferenceManager, threadId: Long, address: String): List<ScheduledSmsEntry> {
        return loadEntries(prefs).filter {
            (threadId > 0 && it.threadId == threadId) ||
            (it.address.isNotBlank() && it.address == address)
        }.sortedBy { it.scheduledTime }
    }

    fun addEntry(context: Context, prefs: PreferenceManager, entry: ScheduledSmsEntry) {
        val current = loadEntries(prefs).filterNot { it.id == entry.id }
        saveEntries(prefs, current + entry)
        scheduleAlarm(context, entry)
    }

    fun removeEntry(context: Context, prefs: PreferenceManager, id: String) {
        cancelAlarm(context, id)
        saveEntries(prefs, loadEntries(prefs).filterNot { it.id == id })
    }

    fun scheduleAlarm(context: Context, entry: ScheduledSmsEntry) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(context, entry.id)
        val triggerAt = entry.scheduledTime
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
            return
        } catch (_: Exception) {}
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

    fun cancelAlarm(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context, id))
    }

    private fun alarmPendingIntent(context: Context, id: String): PendingIntent {
        val intent = Intent(context, ScheduledSmsReceiver::class.java).apply {
            action = ACTION_TRIGGER
            putExtra(EXTRA_ID, id)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun rescheduleAll(context: Context, prefs: PreferenceManager) {
        val now = System.currentTimeMillis()
        val entries = loadEntries(prefs)
        for (entry in entries) {
            if (entry.scheduledTime > now) {
                scheduleAlarm(context, entry)
            }
        }
    }
}
