package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.coolappstore.everdialer.by.svhp.controller.FakeCallReceiver
import com.coolappstore.everdialer.by.svhp.modal.data.FakeCallEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * Persists and schedules "Fake Call" entries.
 *
 * Entries are stored as JSON in [PreferenceManager.KEY_FAKE_CALLS]. Each entry is backed by an
 * [AlarmManager] alarm that fires [FakeCallReceiver], which places a genuine self-managed
 * Telecom call via [com.coolappstore.everdialer.by.svhp.controller.FakeCallConnectionService] —
 * so the existing real incoming-call UI and notification are used, unmodified.
 */
object FakeCallManager {

    const val ACTION_TRIGGER = "com.coolappstore.everdialer.by.svhp.FAKE_CALL_TRIGGER"
    const val ACTION_BOOT    = "com.coolappstore.everdialer.by.svhp.FAKE_CALL_RESCHEDULE"
    const val EXTRA_ID = "fake_call_id"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Persistence ─────────────────────────────────────────────────────────

    fun loadEntries(prefs: PreferenceManager): List<FakeCallEntry> {
        val raw = prefs.getString(PreferenceManager.KEY_FAKE_CALLS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<FakeCallEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(prefs: PreferenceManager, entries: List<FakeCallEntry>) {
        prefs.setString(PreferenceManager.KEY_FAKE_CALLS, json.encodeToString(entries))
    }

    fun findEntry(prefs: PreferenceManager, id: String): FakeCallEntry? =
        loadEntries(prefs).find { it.id == id }

    // ── CRUD ────────────────────────────────────────────────────────────────

    fun addEntry(context: Context, prefs: PreferenceManager, entry: FakeCallEntry) {
        val triggerAt = if (entry.enabled) scheduleAlarm(context, entry) else 0L
        val updated = entry.copy(triggerAt = triggerAt)
        saveEntries(prefs, loadEntries(prefs) + updated)
    }

    fun removeEntry(context: Context, prefs: PreferenceManager, id: String) {
        cancelAlarm(context, id)
        saveEntries(prefs, loadEntries(prefs).filterNot { it.id == id })
    }

    fun setEnabled(context: Context, prefs: PreferenceManager, id: String, enabled: Boolean) {
        val updated = loadEntries(prefs).map { entry ->
            if (entry.id != id) return@map entry
            if (enabled) {
                entry.copy(enabled = true, triggerAt = scheduleAlarm(context, entry))
            } else {
                cancelAlarm(context, id)
                entry.copy(enabled = false, triggerAt = 0L)
            }
        }
        saveEntries(prefs, updated)
    }

    /** Re-arms the alarm for [id] at the next matching time, used after a fake call has rung. */
    fun rescheduleNext(context: Context, prefs: PreferenceManager, id: String) {
        val updated = loadEntries(prefs).map { entry ->
            if (entry.id != id) return@map entry
            val nextTrigger = computeNextTrigger(entry.hour, entry.minute, entry.days, from = System.currentTimeMillis() + 60_000L)
            scheduleAlarm(context, entry, nextTrigger)
            entry.copy(triggerAt = nextTrigger)
        }
        saveEntries(prefs, updated)
    }

    /** Re-arms every enabled entry's alarm — call after device boot. */
    fun rescheduleAll(context: Context, prefs: PreferenceManager) {
        val updated = loadEntries(prefs).map { entry ->
            if (!entry.enabled) return@map entry
            entry.copy(triggerAt = scheduleAlarm(context, entry))
        }
        saveEntries(prefs, updated)
    }

    // ── Alarm scheduling ────────────────────────────────────────────────────

    fun scheduleAlarm(context: Context, entry: FakeCallEntry, triggerAtOverride: Long? = null): Long {
        val triggerAt = triggerAtOverride ?: computeNextTrigger(entry.hour, entry.minute, entry.days)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(context, entry.id)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        return triggerAt
    }

    fun cancelAlarm(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context, id))
    }

    private fun alarmPendingIntent(context: Context, id: String): PendingIntent {
        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            action = ACTION_TRIGGER
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ── Time math ───────────────────────────────────────────────────────────

    /**
     * Computes the next epoch millis at which [hour]:[minute] occurs.
     * If [days] is empty, returns the next occurrence (today if still upcoming, else tomorrow).
     * If [days] is non-empty (Calendar.DAY_OF_WEEK values, 1=Sun..7=Sat), returns the soonest
     * matching day strictly after [from].
     */
    fun computeNextTrigger(hour: Int, minute: Int, days: Set<Int>, from: Long = System.currentTimeMillis()): Long {
        val base = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (days.isEmpty()) {
            if (base.timeInMillis <= from) base.add(Calendar.DAY_OF_MONTH, 1)
            return base.timeInMillis
        }

        for (offset in 0..7) {
            val candidate = base.clone() as Calendar
            candidate.add(Calendar.DAY_OF_MONTH, offset)
            val dow = candidate.get(Calendar.DAY_OF_WEEK)
            if (dow in days && candidate.timeInMillis > from) {
                return candidate.timeInMillis
            }
        }

        // Fallback — shouldn't be reachable since the loop covers a full week.
        base.add(Calendar.DAY_OF_MONTH, 7)
        return base.timeInMillis
    }
}
