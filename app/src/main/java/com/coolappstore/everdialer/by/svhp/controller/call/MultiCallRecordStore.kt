package com.coolappstore.everdialer.by.svhp.controller.call

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MultiCallRecord(
    val id: String = UUID.randomUUID().toString(),
    val number1: String,
    val name1: String? = null,
    val number2: String,
    val name2: String? = null,
    val startTime: Long,
    val endTime: Long = 0L,
    val isMerged: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("number1", number1)
            put("name1", name1 ?: "")
            put("number2", number2)
            put("name2", name2 ?: "")
            put("startTime", startTime)
            put("endTime", endTime)
            put("isMerged", isMerged)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): MultiCallRecord? {
            return try {
                MultiCallRecord(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    number1 = json.getString("number1"),
                    name1 = json.optString("name1").takeIf { it.isNotBlank() },
                    number2 = json.getString("number2"),
                    name2 = json.optString("name2").takeIf { it.isNotBlank() },
                    startTime = json.getLong("startTime"),
                    endTime = json.optLong("endTime", 0L),
                    isMerged = json.optBoolean("isMerged", false)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

object MultiCallRecordStore {
    private const val TAG = "EverDialer:MultiCall"
    private const val PREFS_NAME = "multi_call_records_store"
    private const val KEY_RECORDS = "records_json"
    private const val MAX_RECORDS = 200

    @Volatile
    private var activeSessionId: String? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun normalize(number: String?): String {
        return number?.filter { it.isDigit() } ?: ""
    }

    fun numbersMatch(num1: String?, num2: String?): Boolean {
        val d1 = normalize(num1)
        val d2 = normalize(num2)
        if (d1.isEmpty() || d2.isEmpty()) return false
        if (d1 == d2) return true
        val minLen = 7
        return if (d1.length >= minLen && d2.length >= minLen) {
            d1.takeLast(minLen) == d2.takeLast(minLen)
        } else false
    }

    @Synchronized
    fun getAllRecords(context: Context): List<MultiCallRecord> {
        val jsonStr = getPrefs(context).getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<MultiCallRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                MultiCallRecord.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load multi-call records", e)
            emptyList()
        }
    }

    @Synchronized
    private fun saveRecords(context: Context, records: List<MultiCallRecord>) {
        val trimmed = if (records.size > MAX_RECORDS) records.takeLast(MAX_RECORDS) else records
        val arr = JSONArray()
        trimmed.forEach { arr.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    /**
     * Records or updates a multi-call session (e.g. 1 call active, 1 on hold, or conference).
     * Returns the session ID.
     */
    @Synchronized
    fun recordMultiCall(
        context: Context,
        number1: String,
        name1: String?,
        number2: String,
        name2: String?,
        isMerged: Boolean,
        startTime: Long = System.currentTimeMillis()
    ): String {
        val existing = getAllRecords(context).toMutableList()
        val currentSessionId = activeSessionId

        val existingIndex = if (currentSessionId != null) {
            existing.indexOfFirst { it.id == currentSessionId }
        } else {
            existing.indexOfLast {
                (numbersMatch(it.number1, number1) && numbersMatch(it.number2, number2)) ||
                    (numbersMatch(it.number1, number2) && numbersMatch(it.number2, number1)) &&
                    (it.endTime == 0L || (System.currentTimeMillis() - it.startTime < 1_800_000L))
            }
        }

        val updatedRecord: MultiCallRecord
        if (existingIndex >= 0) {
            val old = existing[existingIndex]
            updatedRecord = old.copy(
                name1 = name1 ?: old.name1,
                name2 = name2 ?: old.name2,
                isMerged = isMerged || old.isMerged
            )
            existing[existingIndex] = updatedRecord
        } else {
            updatedRecord = MultiCallRecord(
                id = UUID.randomUUID().toString(),
                number1 = number1,
                name1 = name1,
                number2 = number2,
                name2 = name2,
                startTime = startTime,
                endTime = 0L,
                isMerged = isMerged
            )
            existing.add(updatedRecord)
        }

        activeSessionId = updatedRecord.id
        saveRecords(context, existing)
        Log.i(TAG, "Recorded multi-call session: ${updatedRecord.id} (merged=$isMerged, n1=$number1, n2=$number2)")
        return updatedRecord.id
    }

    /**
     * Marks the active or specified multi-call session as merged (conference).
     */
    @Synchronized
    fun markMerged(context: Context, sessionId: String? = null) {
        val targetId = sessionId ?: activeSessionId ?: return
        val records = getAllRecords(context).toMutableList()
        val idx = records.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            records[idx] = records[idx].copy(isMerged = true)
            saveRecords(context, records)
            Log.i(TAG, "Marked session $targetId as merged")
        }
    }

    /**
     * Ends the active multi-call session.
     */
    @Synchronized
    fun endMultiCall(
        context: Context,
        sessionId: String? = null,
        endTime: Long = System.currentTimeMillis()
    ) {
        val targetId = sessionId ?: activeSessionId
        if (targetId != null) {
            val records = getAllRecords(context).toMutableList()
            val idx = records.indexOfFirst { it.id == targetId }
            if (idx >= 0 && records[idx].endTime == 0L) {
                records[idx] = records[idx].copy(endTime = endTime)
                saveRecords(context, records)
                Log.i(TAG, "Ended multi-call session $targetId at $endTime")
            }
        }
        activeSessionId = null
    }

    /**
     * Gets the currently active multi-call record if one is ongoing.
     */
    @Synchronized
    fun getActiveRecord(context: Context): MultiCallRecord? {
        val targetId = activeSessionId
        val records = getAllRecords(context)
        if (targetId != null) {
            val found = records.firstOrNull { it.id == targetId }
            if (found != null && found.endTime == 0L) return found
        }
        return records.lastOrNull { it.endTime == 0L }
    }

    /**
     * Finds a multi-call record that occurred around the given timestamp.
     */
    @Synchronized
    fun findRecordForTime(
        context: Context,
        timestamp: Long,
        toleranceMs: Long = 600_000L
    ): MultiCallRecord? {
        val records = getAllRecords(context)
        return records.lastOrNull { rec ->
            val end = if (rec.endTime > 0L) rec.endTime else rec.startTime + 1_800_000L
            timestamp in (rec.startTime - toleranceMs)..(end + toleranceMs)
        }
    }

    /**
     * Finds a multi-call record matching two numbers and timestamp.
     */
    @Synchronized
    fun findRecordForNumbers(
        context: Context,
        number1: String,
        number2: String,
        timestamp: Long,
        toleranceMs: Long = 600_000L
    ): MultiCallRecord? {
        val records = getAllRecords(context)
        return records.lastOrNull { rec ->
            val numMatch = (numbersMatch(rec.number1, number1) && numbersMatch(rec.number2, number2)) ||
                (numbersMatch(rec.number1, number2) && numbersMatch(rec.number2, number1))
            if (!numMatch) return@lastOrNull false
            val end = if (rec.endTime > 0L) rec.endTime else rec.startTime + 1_800_000L
            timestamp in (rec.startTime - toleranceMs)..(end + toleranceMs)
        }
    }

    /**
     * Finds a multi-call record containing a given number around a timestamp.
     */
    @Synchronized
    fun findRecordForCall(
        context: Context,
        number: String,
        timestamp: Long,
        toleranceMs: Long = 600_000L
    ): MultiCallRecord? {
        val records = getAllRecords(context)
        return records.lastOrNull { rec ->
            val containsNum = numbersMatch(rec.number1, number) || numbersMatch(rec.number2, number)
            if (!containsNum) return@lastOrNull false
            val end = if (rec.endTime > 0L) rec.endTime else rec.startTime + 1_800_000L
            timestamp in (rec.startTime - toleranceMs)..(end + toleranceMs)
        }
    }
}
