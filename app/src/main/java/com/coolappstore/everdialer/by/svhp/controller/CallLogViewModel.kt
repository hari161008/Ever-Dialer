package com.coolappstore.everdialer.by.svhp.controller

import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallLogViewModel(
    private val callLogRepo: ICallLogRepository
) : ViewModel() {

    private val _allCallLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val allCallLogs: StateFlow<List<CallLogEntry>> = _allCallLogs.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallLogFilter.All)
    val selectedFilter = _selectedFilter.asStateFlow()

    init {
        fetchLogs()
    }

    public fun setFilter(newFilter: CallLogFilter) {
        _selectedFilter.value = newFilter;
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