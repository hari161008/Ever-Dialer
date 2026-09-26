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
            // Step 2: refresh from provider in background
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

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
    }

    fun refreshLogs() {
        ensureObservers()
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
        viewModelScope.launch(Dispatchers.IO) {
            fetchLogsInternal()
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
                val result = callLogRepo.getCallLogs()
                cachedLogs = result
                CallLogsCache.write(getApplication(), result)
                withContext(Dispatchers.Main) {
                    _allCallLogs.value = result
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
