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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentThreadMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val currentThreadMessages: StateFlow<List<SmsMessage>> = _currentThreadMessages.asStateFlow()

    private var activeThreadId: Long? = null
    private var debounceJob: Job? = null

    private val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(150)
                fetchConversations()
                activeThreadId?.let { tid ->
                    loadThreadMessages(tid)
                }
            }
        }
    }

    init {
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                smsObserver
            )
        } catch (_: Throwable) {
            // Ignore if permission not yet granted
        }
        refreshConversations()
    }

    fun refreshConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchConversations()
        }
    }

    private suspend fun fetchConversations() {
        _isLoading.value = true
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
            _currentThreadMessages.value = msgs
            smsRepository.markThreadAsRead(threadId)
        }
    }

    fun clearActiveThread() {
        activeThreadId = null
        _currentThreadMessages.value = emptyList()
    }

    fun sendMessage(address: String, body: String, subId: Int? = null, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = smsRepository.sendSms(address, body, subId)
            if (success) {
                activeThreadId?.let { tid ->
                    val msgs = smsRepository.getMessagesForThread(tid)
                    _currentThreadMessages.value = msgs
                }
                fetchConversations()
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

    fun deleteMessage(messageId: Long, threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = smsRepository.deleteMessage(messageId)
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

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(smsObserver)
        } catch (_: Throwable) {
            // Ignore
        }
    }
}
