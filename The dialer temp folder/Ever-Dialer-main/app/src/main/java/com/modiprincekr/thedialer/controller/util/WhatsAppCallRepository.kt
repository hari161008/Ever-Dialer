package com.modiprincekr.thedialer.controller.util

import android.content.Context
import android.content.SharedPreferences
import com.modiprincekr.thedialer.modal.data.CallLogEntry
import com.modiprincekr.thedialer.modal.data.WhatsAppCallRecord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import kotlin.math.abs

object WhatsAppCallRepository {
    private const val PREFS_NAME = "whatsapp_call_logs_store"
    private const val KEY_LOGS = "whatsapp_call_logs_json"
    private const val MAX_LOGS = 500

    private val _callLogsChanged = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val callLogsChanged = _callLogsChanged.asSharedFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getAllRecords(context: Context): List<WhatsAppCallRecord> {
        val jsonStr = getPrefs(context).getString(KEY_LOGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<WhatsAppCallRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                WhatsAppCallRecord.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addCallRecord(context: Context, record: WhatsAppCallRecord) {
        val current = getAllRecords(context).toMutableList()
        // Deduplicate rapid duplicate notification updates within a 5-second window
        val existingIndex = current.indexOfFirst {
            val sameCaller = it.callerName.equals(record.callerName, ignoreCase = true) ||
                    (!it.phoneNumber.isNullOrBlank() && !record.phoneNumber.isNullOrBlank() &&
                     numbersLikelyMatch(it.phoneNumber, record.phoneNumber))
            sameCaller && abs(it.timestamp - record.timestamp) <= 5000L
        }

        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val updated = existing.copy(
                durationSeconds = maxOf(existing.durationSeconds, record.durationSeconds),
                callType = if (record.callType != android.provider.CallLog.Calls.MISSED_TYPE) record.callType else existing.callType,
                isVideo = existing.isVideo || record.isVideo,
                phoneNumber = existing.phoneNumber ?: record.phoneNumber,
                contactId = existing.contactId ?: record.contactId,
                photoUri = existing.photoUri ?: record.photoUri
            )
            current[existingIndex] = updated
        } else {
            current.add(0, record)
        }

        val trimmed = if (current.size > MAX_LOGS) current.take(MAX_LOGS) else current
        val arr = JSONArray()
        for (item in trimmed) {
            arr.put(item.toJson())
        }
        getPrefs(context).edit().putString(KEY_LOGS, arr.toString()).apply()
        _callLogsChanged.tryEmit(Unit)
    }

    @Synchronized
    fun deleteByNumberAndDates(context: Context, number: String, dates: List<Long>) {
        val current = getAllRecords(context)
        val filtered = current.filterNot { record ->
            val numMatches = record.callerName.equals(number, ignoreCase = true) ||
                    (record.phoneNumber != null && numbersLikelyMatch(record.phoneNumber, number))
            val dateMatches = dates.isEmpty() || dates.any { abs(it - record.timestamp) < 5000L }
            numMatches && dateMatches
        }
        val arr = JSONArray()
        for (item in filtered) {
            arr.put(item.toJson())
        }
        getPrefs(context).edit().putString(KEY_LOGS, arr.toString()).apply()
        _callLogsChanged.tryEmit(Unit)
    }

    @Synchronized
    fun clearLogs(context: Context) {
        getPrefs(context).edit().remove(KEY_LOGS).apply()
        _callLogsChanged.tryEmit(Unit)
    }

    fun getCallLogEntries(context: Context): List<CallLogEntry> {
        val records = getAllRecords(context)
        return records.map { record ->
            CallLogEntry(
                number = record.phoneNumber ?: record.callerName,
                name = record.callerName,
                type = record.callType,
                date = record.timestamp,
                duration = record.durationSeconds,
                photoUri = record.photoUri,
                contactId = record.contactId,
                types = listOf(record.callType),
                isCallerIdName = false,
                simSlot = -1,
                callIds = emptyList(),
                dates = listOf(record.timestamp),
                isWhatsApp = true,
                isVideo = record.isVideo,
                sourcePackage = record.sourcePackage
            )
        }
    }
}
