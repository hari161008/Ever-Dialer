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

    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            fetchLogs()
        }
    }

    init {
        getApplication<Application>().contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            callLogObserver
        )
        fetchLogs()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(callLogObserver)
    }

    fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter
    }

    fun refreshLogs() {
        fetchLogs()
    }

    private fun fetchLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = callLogRepo.getCallLogs()
            _allCallLogs.value = result
        }
    }
}
