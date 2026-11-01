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
import com.coolappstore.everdialer.by.svhp.controller.util.SmsCache
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
import kotlinx.coroutines.withContext

class SmsViewModel(
    application: Application,
    private val smsRepository: ISmsRepository
) : AndroidViewModel(application) {

    // Seed immediately from memory cache if available (0ms to render)
    private val _conversations = MutableStateFlow<List<SmsConversation>>(
        SmsCache.getInMemoryCache() ?: emptyList()
    )
    val conversations: StateFlow<List<SmsConversation>> = _conversations.asStateFlow()

    private val _hasInitialLoaded = MutableStateFlow(!SmsCache.getInMemoryCache().isNullOrEmpty())
    val hasInitialLoaded: StateFlow<Boolean> = _hasInitialLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(SmsCache.getInMemoryCache().isNullOrEmpty())
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @Volatile
    private var isFetching = false

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

    private val prefs: com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager by lazy {
        org.koin.core.context.GlobalContext.get().get()
    }

    fun isSmsEnabled(): Boolean {
        return prefs.getBoolean(com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager.KEY_SMS_ENABLED, false)
    }

    fun clearConversations() {
        _conversations.value = emptyList()
        _isLoading.value = false
        _hasInitialLoaded.value = true
    }

    private val smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (!isSmsEnabled()) return
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
        if (isSmsEnabled()) {
            // Serve memory/disk cache immediately so UI renders in 0ms on open
            val cached = SmsCache.getInMemoryCache()
            if (!cached.isNullOrEmpty()) {
                _conversations.value = cached
                _hasInitialLoaded.value = true
                _isLoading.value = false
            }
            viewModelScope.launch(Dispatchers.IO) {
                if (_conversations.value.isEmpty()) {
                    val diskCache = SmsCache.read(application)
                    if (diskCache.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (_conversations.value.isEmpty()) {
                                _conversations.value = diskCache
                                _hasInitialLoaded.value = true
                                _isLoading.value = false
                            }
                        }
                    }
                }
                fetchConversations()
            }
        } else {
            _conversations.value = emptyList()
            _hasInitialLoaded.value = true
            _isLoading.value = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            SmsEventBus.newSmsEvent.collect { eventThreadId ->
                if (!isSmsEnabled()) return@collect
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
        if (!isSmsEnabled()) {
            _conversations.value = emptyList()
            _isLoading.value = false
            _hasInitialLoaded.value = true
            return
        }
        if (isFetching) return
        isFetching = true
        if (_conversations.value.isEmpty()) {
            _isLoading.value = true
        }
        try {
            val list = smsRepository.getConversations()
            if (list.isNotEmpty() || _conversations.value.isEmpty()) {
                if (_conversations.value != list) {
                    _conversations.value = list
                }
                SmsCache.write(getApplication(), list)
            }
        } finally {
            _hasInitialLoaded.value = true
            _isLoading.value = false
            isFetching = false
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
        // Optimistically remove from state immediately for zero-delay UI update
        val updated = _conversations.value.filter { it.threadId != threadId }
        _conversations.value = updated
        SmsCache.write(getApplication(), updated)
        viewModelScope.launch(Dispatchers.IO) {
            val success = smsRepository.deleteThread(threadId)
            if (success) {
                if (activeThreadId == threadId) {
                    clearActiveThread()
                }
                debounceJob?.cancel()
                fetchConversations()
            } else {
                // If deletion failed, re-fetch to restore state
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
        val current = _conversations.value
        val updated = current.map { conv ->
            if (conv.threadId == threadId) conv.copy(isRead = true, unreadCount = 0) else conv
        }
        if (current != updated) {
            _conversations.value = updated
            SmsCache.write(getApplication(), updated)
        }
        viewModelScope.launch(Dispatchers.IO) {
            smsRepository.markThreadAsRead(threadId)
            fetchConversations()
        }
    }

    fun markThreadAsUnread(threadId: Long) {
        val current = _conversations.value
        val updated = current.map { conv ->
            if (conv.threadId == threadId) conv.copy(isRead = false, unreadCount = 1) else conv
        }
        if (current != updated) {
            _conversations.value = updated
            SmsCache.write(getApplication(), updated)
        }
        viewModelScope.launch(Dispatchers.IO) {
            smsRepository.markThreadAsUnread(threadId)
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
