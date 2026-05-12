package com.coolappstore.everdialer.by.svhp.controller

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CallLogViewModel(
    application: Application,
    private val callLogRepo: ICallLogRepository
) : AndroidViewModel(application) {

    private val _allCallLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val allCallLogs: StateFlow<List<CallLogEntry>> = _allCallLogs.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallLogFilter.All)
    val selectedFilter = _selectedFilter.asStateFlow()

    // In-memory cache
    @Volatile private var cachedLogs: List<CallLogEntry> = emptyList()
    @Volatile private var isFetching = false
    private var debounceJob: Job? = null

    // Disk cache file
    private val cacheFile: File by lazy {
        File(application.cacheDir, "call_logs_cache.json")
    }

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                delay(300)
                fetchLogs(forceRefresh = true)
            }
        }
    }

    init {
        getApplication<Application>().contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            callLogObserver
        )
        // Step 1: serve disk cache immediately so UI is instant
        viewModelScope.launch(Dispatchers.IO) {
            val diskCache = loadFromDisk()
            if (diskCache.isNotEmpty()) {
                cachedLogs = diskCache
                withContext(Dispatchers.Main) {
                    _allCallLogs.value = diskCache
                }
            }
            // Step 2: refresh from provider in background
            fetchLogsInternal()
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(callLogObserver)
    }

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
    }

    fun refreshLogs() {
        fetchLogs(forceRefresh = true)
    }

    private fun fetchLogs(forceRefresh: Boolean = false) {
        if (!forceRefresh && cachedLogs.isNotEmpty()) {
            _allCallLogs.value = cachedLogs
            return
        }
        if (isFetching) return
        viewModelScope.launch(Dispatchers.IO) {
            fetchLogsInternal()
        }
    }

    private suspend fun fetchLogsInternal() {
        if (isFetching) return
        isFetching = true
        try {
            val result = callLogRepo.getCallLogs()
            // Only push an update to the UI if the data actually changed.
            // This prevents a visible "refresh flicker" when the disk cache
            // and the freshly-fetched data are identical (the common case on
            // every app open after the first one).
            val changed = result.size != cachedLogs.size ||
                result.zip(cachedLogs).any { (a, b) ->
                    a.number != b.number || a.date != b.date || a.type != b.type
                }
            cachedLogs = result
            saveToDisk(result)
            if (changed) {
                withContext(Dispatchers.Main) {
                    _allCallLogs.value = result
                }
            }
        } finally {
            isFetching = false
        }
    }

    // ── Disk cache helpers ────────────────────────────────────────────────────

    private fun saveToDisk(logs: List<CallLogEntry>) {
        try {
            val arr = JSONArray()
            logs.forEach { e ->
                val obj = JSONObject()
                obj.put("number", e.number)
                obj.put("name", e.name ?: "")
                obj.put("type", e.type)
                obj.put("date", e.date)
                obj.put("duration", e.duration)
                obj.put("photoUri", e.photoUri ?: "")
                obj.put("contactId", e.contactId ?: "")
                val typesArr = JSONArray()
                e.types.forEach { typesArr.put(it) }
                obj.put("types", typesArr)
                arr.put(obj)
            }
            cacheFile.writeText(arr.toString())
        } catch (_: Exception) {}
    }

    private fun loadFromDisk(): List<CallLogEntry> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            val arr = JSONArray(cacheFile.readText())
            val list = mutableListOf<CallLogEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val typesArr = obj.optJSONArray("types")
                val types = mutableListOf<Int>()
                if (typesArr != null) {
                    for (j in 0 until typesArr.length()) types.add(typesArr.getInt(j))
                }
                list.add(
                    CallLogEntry(
                        number = obj.getString("number"),
                        name = obj.getString("name").ifEmpty { null },
                        type = obj.getInt("type"),
                        date = obj.getLong("date"),
                        duration = obj.getLong("duration"),
                        photoUri = obj.getString("photoUri").ifEmpty { null },
                        contactId = obj.getString("contactId").ifEmpty { null },
                        types = types
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
