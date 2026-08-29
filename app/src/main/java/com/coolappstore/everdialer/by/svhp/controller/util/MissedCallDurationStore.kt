package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import java.util.concurrent.ConcurrentHashMap

object MissedCallDurationStore {
    private const val PREFS_NAME = "missed_call_ringing_durations"
    private val memoryCache = ConcurrentHashMap<String, Long>()

    private fun normalize(number: String): String {
        return number.filter { it.isDigit() }.takeLast(10)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDuration(context: Context, number: String, dateMillis: Long, durationSec: Long) {
        if (durationSec <= 0) return
        val digits = normalize(number)
        if (digits.isBlank()) return

        val keyMinute = "${digits}_${dateMillis / 60000L}"
        val keyExact = "${digits}_$dateMillis"
        val keyLatest = "${digits}_latest"

        memoryCache[keyMinute] = durationSec
        memoryCache[keyExact] = durationSec
        memoryCache[keyLatest] = durationSec

        try {
            getPrefs(context).edit()
                .putLong(keyMinute, durationSec)
                .putLong(keyExact, durationSec)
                .putString(keyLatest, "$dateMillis:$durationSec")
                .apply()
        } catch (_: Throwable) {}
    }

    fun getDuration(context: Context, number: String, dateMillis: Long): Long {
        val digits = normalize(number)
        if (digits.isBlank()) return 0L

        val keyExact = "${digits}_$dateMillis"
        memoryCache[keyExact]?.let { if (it > 0) return it }

        val keyMinute = "${digits}_${dateMillis / 60000L}"
        memoryCache[keyMinute]?.let { if (it > 0) return it }

        // Try minute before and minute after in case boundary fell on minute flip
        memoryCache["${digits}_${(dateMillis / 60000L) - 1}"]?.let { if (it > 0) return it }
        memoryCache["${digits}_${(dateMillis / 60000L) + 1}"]?.let { if (it > 0) return it }

        try {
            val prefs = getPrefs(context)
            val exact = prefs.getLong(keyExact, 0L)
            if (exact > 0) {
                memoryCache[keyExact] = exact
                return exact
            }
            val minute = prefs.getLong(keyMinute, 0L)
            if (minute > 0) {
                memoryCache[keyMinute] = minute
                return minute
            }
            val minPrev = prefs.getLong("${digits}_${(dateMillis / 60000L) - 1}", 0L)
            if (minPrev > 0) return minPrev
            val minNext = prefs.getLong("${digits}_${(dateMillis / 60000L) + 1}", 0L)
            if (minNext > 0) return minNext

            // Check latest entry for this number if within 3 minutes
            val latestStr = prefs.getString("${digits}_latest", null)
            if (!latestStr.isNullOrBlank()) {
                val parts = latestStr.split(":")
                if (parts.size == 2) {
                    val savedDate = parts[0].toLongOrNull() ?: 0L
                    val savedDur = parts[1].toLongOrNull() ?: 0L
                    if (savedDur > 0 && kotlin.math.abs(savedDate - dateMillis) <= 180000L) {
                        return savedDur
                    }
                }
            }
        } catch (_: Throwable) {}

        return 0L
    }

    fun updateProviderDuration(context: Context, number: String, durationSec: Long) {
        if (durationSec <= 0) return
        val handler = Handler(Looper.getMainLooper())
        val updateWork = Runnable {
            try {
                val values = ContentValues().apply {
                    put(CallLog.Calls.DURATION, durationSec)
                }
                val where = "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DURATION} = 0 AND ${CallLog.Calls.DATE} >= ?"
                val args = arrayOf(
                    number,
                    CallLog.Calls.MISSED_TYPE.toString(),
                    (System.currentTimeMillis() - 90000L).toString()
                )
                context.contentResolver.update(CallLog.Calls.CONTENT_URI, values, where, args)
            } catch (_: Throwable) {}
        }

        // Run immediately and retry shortly after as telecom writes call log asynchronously
        updateWork.run()
        handler.postDelayed(updateWork, 600)
        handler.postDelayed(updateWork, 1500)
    }
}
