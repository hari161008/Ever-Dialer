package com.coolappstore.everdialer.by.svhp.controller

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.everdialer.by.svhp.controller.util.CallLogsCache
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallLogViewModel(
    application: Application,
    private val callLogRepo: ICallLogRepository,
    private val prefs: PreferenceManager
) : AndroidViewModel(application) {

    fun fetchCallLogs() {
        fetchLogs(forceRefresh = true)
    }

    private val _allCallLogs = MutableStateFlow<List<CallLogEntry>>(
        CallLogsCache.getInMemoryCache() ?: emptyList()
    )
    val allCallLogs: StateFlow<List<CallLogEntry>> = _allCallLogs.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallLogFilter.All)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow(prefs.getDefaultCallCategory())
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _isFullHistoryLoaded = MutableStateFlow(false)
    val isFullHistoryLoaded = _isFullHistoryLoaded.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    @Volatile private var currentLoadedLimit = 100

    // In-memory cache
    @Volatile private var cachedLogs: List<CallLogEntry> = CallLogsCache.getInMemoryCache() ?: emptyList()
    @Volatile private var isFetching = false
    @Volatile private var pendingRefresh = false
    @Volatile private var wasInCall = false
    private var debounceJob: Job? = null
    private var callEndRefreshJob: Job? = null

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(100)
                fetchLogsInternal()
            }
        }
    }

    // Saving/renaming/deleting a contact never touches the call log provider itself, so without
    // this, a call log entry kept showing "Unknown"/the old name forever after the contact was
    // saved — nothing was telling this ViewModel its cached names were now stale.
    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(300)
                fetchLogsInternal()
            }
        }
    }

    private var callLogObserverRegistered = false
    private var contactsObserverRegistered = false

    private fun ensureObservers() {
        try {
            if (!callLogObserverRegistered && androidx.core.content.ContextCompat.checkSelfPermission(
                    getApplication(),
                    android.Manifest.permission.READ_CALL_LOG
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                getApplication<Application>().contentResolver.registerContentObserver(
                    CallLog.Calls.CONTENT_URI,
                    true,
                    callLogObserver
                )
                callLogObserverRegistered = true
            }
        } catch (_: Throwable) {}
        try {
            if (!contactsObserverRegistered && androidx.core.content.ContextCompat.checkSelfPermission(
                    getApplication(),
                    android.Manifest.permission.READ_CONTACTS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                getApplication<Application>().contentResolver.registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI,
                    true,
                    contactsObserver
                )
                contactsObserverRegistered = true
            }
        } catch (_: Throwable) {}
    }

    init {
        ensureObservers()
        // Step 1: serve memory/disk cache immediately so UI is instant on cold/warm open
        viewModelScope.launch(Dispatchers.IO) {
            val diskCache = runCatching { CallLogsCache.read(application) }.getOrDefault(emptyList())
            if (diskCache.isNotEmpty()) {
                cachedLogs = diskCache
                if (_allCallLogs.value.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _allCallLogs.value = diskCache
                    }
                }
            }
            // Step 2: refresh from provider in background if needed
            val isNewLogic = prefs.getBoolean(PreferenceManager.KEY_NEW_CALL_LOGS_LOGIC, false)
            if (isNewLogic && diskCache.isNotEmpty()) {
                val latestDate = callLogRepo.getLatestCallDate()
                if (latestDate != null && diskCache.firstOrNull()?.date == latestDate) {
                    // Cache is up to date with latest call log; skip heavy reload on launch
                    return@launch
                }
            }
            fetchLogsInternal()
        }

        viewModelScope.launch {
            prefs.settingsChanged.collect {
                fetchLogs(forceRefresh = true)
            }
        }

        // Watching CallService's call-session state means we refetch both when a call starts
        // and when it ends (both incoming and outgoing, including call waiting).
        viewModelScope.launch {
            combine(
                CallService.currentCallSession,
                CallService.incomingCallSession
            ) { curr, inc -> curr != null || inc != null }
                .collect { inCall ->
                    if (inCall) {
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
                            // Telecom writes the finished call row asynchronously after disconnect.
                            // Fast sequence of retries with pendingRefresh protection ensures
                            // the row is picked up the moment Telecom commits it to the provider.
                            fetchLogsInternal()
                            delay(250)
                            fetchLogsInternal()
                            delay(350)
                            fetchLogsInternal()
                            delay(600)
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

    fun setCategoryId(id: String) {
        _selectedCategoryId.value = id
        val filter = when (id) {
            "all" -> CallLogFilter.All
            "favourites" -> CallLogFilter.Favourites
            "contacts" -> CallLogFilter.Contacts
            "unknown" -> CallLogFilter.Unknown
            "missed" -> CallLogFilter.Missed
            "incoming" -> CallLogFilter.Incoming
            "outgoing" -> CallLogFilter.Outgoing
            else -> CallLogFilter.All
        }
        _selectedFilter.value = filter
    }

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
        _selectedCategoryId.value = when (newFilter) {
            CallLogFilter.All -> "all"
            CallLogFilter.Favourites -> "favourites"
            CallLogFilter.Contacts -> "contacts"
            CallLogFilter.Unknown -> "unknown"
            CallLogFilter.Missed -> "missed"
            CallLogFilter.Incoming -> "incoming"
            CallLogFilter.Outgoing -> "outgoing"
        }
    }

    fun refreshLogs() {
        ensureObservers()
        fetchLogs(forceRefresh = true)
    }

    fun refreshIfStale() {
        val isNewLogic = prefs.getBoolean(PreferenceManager.KEY_NEW_CALL_LOGS_LOGIC, false)
        if (!isNewLogic) {
            refreshLogs()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val current = _allCallLogs.value
            if (current.isEmpty()) {
                fetchLogsInternal()
                return@launch
            }
            val latestDate = callLogRepo.getLatestCallDate()
            val cachedFirstDate = current.firstOrNull()?.date
            if (latestDate != null && latestDate != cachedFirstDate) {
                fetchLogsInternal()
            }
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            fetchLogsInternal()
        }
    }

    fun loadOlderHistory() {
        val isNewLogic = prefs.getBoolean(PreferenceManager.KEY_NEW_CALL_LOGS_LOGIC, false)
        if (isNewLogic || _isFullHistoryLoaded.value || _isLoadingMore.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            try {
                val nextLimit = currentLoadedLimit + 100
                val logs = callLogRepo.getCallLogs(limit = nextLimit)
                val prevCount = _allCallLogs.value.size
                currentLoadedLimit = nextLimit
                cachedLogs = logs
                withContext(Dispatchers.Main) {
                    if (logs.size == prevCount || logs.size < nextLimit) {
                        _isFullHistoryLoaded.value = true
                    }
                    _allCallLogs.value = logs
                }
            } catch (_: Exception) {
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun fetchLogsInternal() {
        ensureObservers()
        if (isFetching) {
            // Queue another fetch so concurrent calls/notifications are never dropped
            pendingRefresh = true
            return
        }
        isFetching = true
        try {
            do {
                pendingRefresh = false
                val isNewLogic = prefs.getBoolean(PreferenceManager.KEY_NEW_CALL_LOGS_LOGIC, false)
                val limit = if (!isNewLogic && !_isFullHistoryLoaded.value) currentLoadedLimit else null
                val result = callLogRepo.getCallLogs(limit = limit)
                val prevCached = cachedLogs
                cachedLogs = result
                if (isNewLogic) {
                    val changed = result.size != prevCached.size ||
                        result.zip(prevCached).any { (a, b) ->
                            a.number != b.number || a.date != b.date || a.type != b.type ||
                                a.name != b.name || a.photoUri != b.photoUri || a.count != b.count ||
                                a.callIds != b.callIds
                        }
                    if (changed || _allCallLogs.value.isEmpty()) {
                        CallLogsCache.write(getApplication(), result)
                        withContext(Dispatchers.Main) {
                            _allCallLogs.value = result
                        }
                    }
                    withContext(Dispatchers.Main) {
                        _isFullHistoryLoaded.value = true
                    }
                } else {
                    if (prevCached.isEmpty() || prevCached != result) {
                        CallLogsCache.write(getApplication(), result)
                    }
                    withContext(Dispatchers.Main) {
                        _isFullHistoryLoaded.value = true
                        if (_allCallLogs.value != result) {
                            _allCallLogs.value = result
                        }
                    }
                }
            } while (pendingRefresh)

            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        getApplication(),
                        android.Manifest.permission.READ_CALL_LOG
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    com.coolappstore.everdialer.by.svhp.controller.util.MissedCallBadgeManager.updateBadge(getApplication())
                }
            } catch (_: Throwable) {}
        } finally {
            isFetching = false
        }
    }
}
