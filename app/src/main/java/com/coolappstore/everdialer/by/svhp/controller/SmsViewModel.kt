package com.coolappstore.everdialer.by.svhp.controller

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.everdialer.by.svhp.modal.data.SmsConversation
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import com.coolappstore.everdialer.by.svhp.controller.sms.SmsEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmsViewModel(
    application: Application,
    private val smsRepository: ISmsRepository
) : AndroidViewModel(application) {

    private val _conversations = MutableStateFlow<List<SmsConversation>>(emptyList())
    val conversations: StateFlow<List<SmsConversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val searchQuery = MutableStateFlow("")

    val filteredConversations: StateFlow<List<SmsConversation>> = combine(
        _conversations,
        searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter { conv ->
                conv.contactName?.lowercase()?.contains(q) == true ||
                conv.address.lowercase().contains(q) ||
                conv.snippet.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentThreadMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val currentThreadMessages: StateFlow<List<SmsMessage>> = _currentThreadMessages.asStateFlow()

    private val _starredMessages = MutableStateFlow<List<com.coolappstore.everdialer.by.svhp.modal.data.StarredMessage>>(emptyList())
    val starredMessages: StateFlow<List<com.coolappstore.everdialer.by.svhp.modal.data.StarredMessage>> = _starredMessages.asStateFlow()

    private var activeThreadId: Long? = null
    private var debounceJob: Job? = null

    private val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Immediately reload active thread messages without waiting for full conversation scan
            activeThreadId?.let { tid ->
                viewModelScope.launch(Dispatchers.IO) {
                    val msgs = smsRepository.getMessagesForThread(tid)
                    if (_currentThreadMessages.value != msgs) {
                        _currentThreadMessages.value = msgs
                    }
                    smsRepository.markThreadAsRead(tid)
                }
            }
            // Debounce full conversation list refresh
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(300)
                fetchConversations()
            }
        }
    }

    init {
        registerObservers()
        refreshConversations()
        viewModelScope.launch(Dispatchers.IO) {
            SmsEventBus.newSmsEvent.collect { eventThreadId ->
                activeThreadId?.let { tid ->
                    if (eventThreadId == null || eventThreadId == tid) {
                        val msgs = smsRepository.getMessagesForThread(tid)
                        if (_currentThreadMessages.value != msgs) {
                            _currentThreadMessages.value = msgs
                        }
                        smsRepository.markThreadAsRead(tid)
                    }
                }
                fetchConversations()
            }
        }
    }

    private fun registerObservers() {
        val cr = getApplication<Application>().contentResolver
        try {
            cr.registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver)
        } catch (_: Throwable) {}
        try {
            cr.registerContentObserver(Telephony.Mms.CONTENT_URI, true, smsObserver)
        } catch (_: Throwable) {}
        try {
            cr.registerContentObserver(Uri.parse("content://mms-sms/conversations"), true, smsObserver)
        } catch (_: Throwable) {}
    }

    fun refreshConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchConversations()
        }
    }

    private suspend fun fetchConversations() {
        if (_conversations.value.isEmpty()) {
            _isLoading.value = true
        }
        try {
            val list = smsRepository.getConversations()
            _conversations.value = list
        } finally {
            _isLoading.value = false
        }
    }

    fun loadThreadMessages(threadId: Long) {
        activeThreadId = threadId
        viewModelScope.launch(Dispatchers.IO) {
            val msgs = smsRepository.getMessagesForThread(threadId)
            if (_currentThreadMessages.value != msgs) {
                _currentThreadMessages.value = msgs
            }
            smsRepository.markThreadAsRead(threadId)
        }
    }

    fun clearActiveThread() {
        activeThreadId = null
        _currentThreadMessages.value = emptyList()
    }

    suspend fun getOrCreateThreadId(address: String): Long {
        return smsRepository.getOrCreateThreadId(address)
    }

    fun sendMessage(address: String, body: String, subId: Int? = null, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = smsRepository.sendSms(address, body, subId)
            if (success) {
                activeThreadId?.let { tid ->
                    val msgs = smsRepository.getMessagesForThread(tid)
                    _currentThreadMessages.value = msgs
                }
                launch(Dispatchers.IO) {
                    fetchConversations()
                }
            }
            onResult?.invoke(success)
        }
    }

    fun deleteThread(threadId: Long, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = smsRepository.deleteThread(threadId)
            if (success) {
                if (activeThreadId == threadId) {
                    clearActiveThread()
                }
                fetchConversations()
            }
            onResult?.invoke(success)
        }
    }

    fun deleteMessage(messageId: Long, threadId: Long, isMms: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = smsRepository.deleteMessage(messageId, isMms)
            if (success) {
                loadThreadMessages(threadId)
                fetchConversations()
            }
        }
    }

    fun markThreadAsRead(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            smsRepository.markThreadAsRead(threadId)
            fetchConversations()
        }
    }

    fun refreshStarredMessages(starredIds: Set<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (starredIds.isEmpty()) {
                _starredMessages.value = emptyList()
                return@launch
            }
            val msgs = smsRepository.getMessagesByIds(starredIds)
            val starredList = msgs.map { msg ->
                val contactInfo = smsRepository.resolveContactInfo(msg.address)
                com.coolappstore.everdialer.by.svhp.modal.data.StarredMessage(
                    message = msg,
                    contactName = contactInfo.first,
                    photoUri = contactInfo.second
                )
            }
            _starredMessages.value = starredList
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(smsObserver)
        } catch (_: Throwable) {}
    }
}
