package com.coolappstore.everdialer.by.svhp.controller

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
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
    @Volatile private var wasInCall = false
    private var debounceJob: Job? = null
    private var callEndRefreshJob: Job? = null

    // Disk cache file
    private val cacheFile: File by lazy {
        File(application.cacheDir, "call_logs_cache.json")
    }

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                fetchLogs(forceRefresh = true)
            }
        }
    }

    // Saving/renaming/deleting a contact never touches the call log provider itself, so without
    // this, a call log entry kept showing "Unknown"/the old name forever after the contact was
    // saved — nothing was telling this ViewModel its cached names (via CallLogRepository's own
    // contactInfoCache) were now stale. Shares the same debounce job as the call log observer
    // since either one just means "re-fetch and re-resolve names".
    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                delay(300)
                fetchLogs(forceRefresh = true)
            }
        }
    }

    init {
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            )
        } catch (_: Exception) {}
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver
            )
        } catch (_: Exception) {}
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

        // The call log provider writes the finished call's row right around when the call
        // actually ends, but its own ContentObserver notification can lag by a bit. Watching
        // CallService's own call-session state instead means we refetch the instant the call
        // ends — while the user is still on the in-call screen / navigating back — so Recents
        // is already showing the finished call by the time they look, with no pull-to-refresh
        // and no visible loading animation needed.
        //
        // Bug fix: this previously only ever refetched when a call *ended* (session transitioned
        // to null). That meant an incoming call that was missed/rejected super quickly, or a
        // call log row that the provider already had queued up by the time the call actually
        // starts ringing/dialing, wouldn't show up until something else (like scrolling, which
        // happens to force a recomposition) nudged the list to catch up. Now also refetch right
        // when a call *starts* — both incoming and outgoing — so Recents stays current through
        // the whole lifecycle of a call, not just after it hangs up.
        viewModelScope.launch {
            CallService.currentCallSession.collect { session ->
                if (session != null) {
                    if (!wasInCall) {
                        wasInCall = true
                        callEndRefreshJob?.cancel()
                        callEndRefreshJob = viewModelScope.launch(Dispatchers.IO) {
                            fetchLogsInternal()
                        }
                    }
                } else if (wasInCall) {
                    wasInCall = false
                    callEndRefreshJob?.cancel()
                    callEndRefreshJob = viewModelScope.launch(Dispatchers.IO) {
                        // Fire right away, then a couple more times shortly after as a safety
                        // net in case the provider hadn't finished writing the row on the
                        // first (or second) pass yet.
                        fetchLogsInternal()
                        delay(400)
                        fetchLogsInternal()
                        delay(800)
                        fetchLogsInternal()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(callLogObserver)
        } catch (_: Exception) {}
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(contactsObserver)
        } catch (_: Exception) {}
    }

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
    }

    fun refreshLogs() {
        fetchLogs(forceRefresh = true)
    }

    fun deleteCallLog(entry: CallLogEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepo.deleteCallLog(entry)
            fetchLogs(forceRefresh = true)
        }
    }

    fun deleteCallLogs(entries: Collection<CallLogEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            callLogRepo.deleteCallLogs(entries)
            fetchLogs(forceRefresh = true)
        }
    }

    fun deleteCallLogsByKeys(keys: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentLogs = _allCallLogs.value
            val entriesToDelete = currentLogs.filter { "${it.number}|${it.date}" in keys }
            if (entriesToDelete.isNotEmpty()) {
                callLogRepo.deleteCallLogs(entriesToDelete)
            } else {
                // Fallback if entries not found in memory
                keys.forEach { key ->
                    val parts = key.split("|", limit = 2)
                    if (parts.size == 2) {
                        try {
                            getApplication<Application>().contentResolver.delete(
                                CallLog.Calls.CONTENT_URI,
                                "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
                                arrayOf(parts[0], parts[1])
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
            fetchLogs(forceRefresh = true)
        }
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
                    a.number != b.number || a.date != b.date || a.type != b.type ||
                        a.name != b.name || a.photoUri != b.photoUri || a.count != b.count ||
                        a.callIds != b.callIds
                }
            cachedLogs = result
            if (changed) {
                // Only touch disk when the data actually changed. This method gets called
                // several times back-to-back (call-start, call-end + 2 retries, the call-log
                // ContentObserver, and the screen's own onResume refresh can all land within a
                // second of each other), and on a large call history serializing and writing the
                // full JSON every single time - even when nothing changed - was pure wasted I/O.
                saveToDisk(result)
                withContext(Dispatchers.Main) {
                    _allCallLogs.value = result
                }
            }
            com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.updateBadge(getApplication())
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
                obj.put("isCallerIdName", e.isCallerIdName)
                obj.put("simSlot", e.simSlot)
                val typesArr = JSONArray()
                e.types.forEach { typesArr.put(it) }
                obj.put("types", typesArr)
                val callIdsArr = JSONArray()
                e.callIds.forEach { callIdsArr.put(it) }
                obj.put("callIds", callIdsArr)
                val datesArr = JSONArray()
                e.dates.forEach { datesArr.put(it) }
                obj.put("dates", datesArr)
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
                val callIdsArr = obj.optJSONArray("callIds")
                val callIds = mutableListOf<Long>()
                if (callIdsArr != null) {
                    for (j in 0 until callIdsArr.length()) callIds.add(callIdsArr.getLong(j))
                }
                val datesArr = obj.optJSONArray("dates")
                val dates = mutableListOf<Long>()
                if (datesArr != null) {
                    for (j in 0 until datesArr.length()) dates.add(datesArr.getLong(j))
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
                        types = types,
                        isCallerIdName = obj.optBoolean("isCallerIdName", false),
                        simSlot = obj.optInt("simSlot", -1),
                        callIds = callIds,
                        dates = dates
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
