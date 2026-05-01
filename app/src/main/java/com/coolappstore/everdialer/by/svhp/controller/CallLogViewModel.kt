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

class CallLogViewModel(
    application: Application,
    private val callLogRepo: ICallLogRepository
) : AndroidViewModel(application) {

    private val _allCallLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val allCallLogs: StateFlow<List<CallLogEntry>> = _allCallLogs.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallLogFilter.All)
    val selectedFilter = _selectedFilter.asStateFlow()

    // In-memory cache to avoid redundant IO on every observer change
    @Volatile private var cachedLogs: List<CallLogEntry> = emptyList()
    @Volatile private var isFetching = false
    private var debounceJob: Job? = null

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Debounce rapid successive changes (e.g., bulk delete)
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
        fetchLogs(forceRefresh = false)
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
        // Serve cached result immediately while a fetch is in-flight to keep UI smooth
        if (!forceRefresh && cachedLogs.isNotEmpty()) {
            _allCallLogs.value = cachedLogs
            return
        }
        if (isFetching) return
        isFetching = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = callLogRepo.getCallLogs()
                cachedLogs = result
                _allCallLogs.value = result
            } finally {
                isFetching = false
            }
        }
    }
}
