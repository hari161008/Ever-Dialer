package com.modiprincekr.thedialer.controller

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.modiprincekr.thedialer.modal.data.SmsConversation
import com.modiprincekr.thedialer.modal.data.SmsFilterTab
import com.modiprincekr.thedialer.modal.data.SmsMessage
import com.modiprincekr.thedialer.modal.`interface`.ISmsRepository
import com.modiprincekr.thedialer.controller.sms.SmsEventBus
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.controller.util.SmsCache
import com.modiprincekr.thedialer.controller.util.numbersLikelyMatch
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
    private val smsRepository: ISmsRepository,
    private val prefs: PreferenceManager? = null
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

    private val _selectedFilter = MutableStateFlow<SmsFilterTab>(
        prefs?.getSmsSelectedFilter() ?: SmsFilterTab.ALL
    )
    val selectedFilter: StateFlow<SmsFilterTab> = _selectedFilter.asStateFlow()

    private val _filterOrder = MutableStateFlow<List<SmsFilterTab>>(
        prefs?.getSmsFilterOrder() ?: SmsFilterTab.DEFAULT_ORDER
    )
    val filterOrder: StateFlow<List<SmsFilterTab>> = _filterOrder.asStateFlow()

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

    private val _isThreadLoading = MutableStateFlow(false)
    val isThreadLoading: StateFlow<Boolean> = _isThreadLoading.asStateFlow()

    private val _starredMessages = MutableStateFlow<List<com.modiprincekr.thedialer.modal.data.StarredMessage>>(emptyList())
    val starredMessages: StateFlow<List<com.modiprincekr.thedialer.modal.data.StarredMessage>> = _starredMessages.asStateFlow()

    private var activeThreadId: Long? = null
    private var debounceJob: Job? = null

    fun isSmsEnabled(): Boolean {
        return prefs?.getBoolean(PreferenceManager.KEY_SMS_ENABLED, false) ?: false
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
                    withContext(Dispatchers.Main) {
                        if (_currentThreadMessages.value != msgs) {
                            _currentThreadMessages.value = msgs
                        }
                        SmsCache.setThreadMessages(tid, msgs)
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
        prefs?.let { p ->
            viewModelScope.launch {
                p.settingsChanged.collect {
                    _filterOrder.value = p.getSmsFilterOrder()
                    _selectedFilter.value = p.getSmsSelectedFilter()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            SmsEventBus.newSmsEvent.collect { eventThreadId ->
                if (!isSmsEnabled()) return@collect
                activeThreadId?.let { tid ->
                    if (eventThreadId == null || eventThreadId == tid) {
                        val msgs = smsRepository.getMessagesForThread(tid)
                        withContext(Dispatchers.Main) {
                            if (_currentThreadMessages.value != msgs) {
                                _currentThreadMessages.value = msgs
                            }
                            SmsCache.setThreadMessages(tid, msgs)
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

    fun prepareThread(threadId: Long, address: String? = null) {
        activeThreadId = if (threadId > 0) threadId else null
        val cached = (if (threadId > 0) SmsCache.getThreadMessages(threadId) else null)
            ?: (if (!address.isNullOrBlank()) SmsCache.getThreadMessagesByAddress(address) else null)
        if (!cached.isNullOrEmpty()) {
            _currentThreadMessages.value = cached
            _isThreadLoading.value = false
        } else {
            _currentThreadMessages.value = emptyList()
            _isThreadLoading.value = true
        }
    }

    fun loadThreadMessages(threadId: Long, address: String? = null) {
        activeThreadId = threadId
        val cached = SmsCache.getThreadMessages(threadId)
            ?: (if (!address.isNullOrBlank()) SmsCache.getThreadMessagesByAddress(address) else null)
        if (!cached.isNullOrEmpty()) {
            if (_currentThreadMessages.value != cached) {
                _currentThreadMessages.value = cached
            }
            _isThreadLoading.value = false
        } else if (_currentThreadMessages.value.isEmpty()) {
            _isThreadLoading.value = true
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val msgs = smsRepository.getMessagesForThread(threadId)
                withContext(Dispatchers.Main) {
                    if (_currentThreadMessages.value != msgs) {
                        _currentThreadMessages.value = msgs
                    }
                    SmsCache.setThreadMessages(threadId, msgs, address)
                    _isThreadLoading.value = false
                }
                smsRepository.markThreadAsRead(threadId)
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _isThreadLoading.value = false
                }
            }
        }
    }

    fun clearActiveThread() {
        activeThreadId = null
        _isThreadLoading.value = false
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
                    withContext(Dispatchers.Main) {
                        _currentThreadMessages.value = msgs
                        SmsCache.setThreadMessages(tid, msgs, address)
                    }
                }
                launch(Dispatchers.IO) {
                    fetchConversations()
                }
            }
            onResult?.invoke(success)
        }
    }

    fun deleteThread(threadId: Long, onResult: ((Boolean) -> Unit)? = null) {
        SmsCache.removeThreadMessages(threadId)
        if (activeThreadId == threadId) {
            _currentThreadMessages.value = emptyList()
        }
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
        val current = _currentThreadMessages.value.filter { it.id != messageId }
        _currentThreadMessages.value = current
        SmsCache.setThreadMessages(threadId, current)
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

    fun setFilter(tab: SmsFilterTab) {
        _selectedFilter.value = tab
        prefs?.setSmsSelectedFilter(tab)
    }

    fun setFilterOrder(newOrder: List<SmsFilterTab>) {
        _filterOrder.value = newOrder
        prefs?.setSmsFilterOrder(newOrder)
    }

    fun moveFilterTab(fromIndex: Int, toIndex: Int) {
        val current = _filterOrder.value
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val updated = current.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        setFilterOrder(updated)
    }

    fun resetFilterOrder() {
        setFilterOrder(SmsFilterTab.DEFAULT_ORDER)
    }

    fun filterConversations(
        conversations: List<SmsConversation>,
        filter: SmsFilterTab,
        favoriteNumbers: Set<String> = emptySet(),
        favoriteNames: Set<String> = emptySet()
    ): List<SmsConversation> {
        val filtered = when (filter) {
            SmsFilterTab.ALL -> conversations
            SmsFilterTab.FAVOURITES -> conversations.filter { conv ->
                favoriteNames.contains(conv.contactName?.trim()?.lowercase()) ||
                favoriteNumbers.any { numbersLikelyMatch(conv.address, it) }
            }
            SmsFilterTab.STARRED -> emptyList()
            SmsFilterTab.CONTACTS -> conversations.filter { !it.contactName.isNullOrBlank() }
            SmsFilterTab.UNKNOWN -> conversations.filter { it.contactName.isNullOrBlank() }
            SmsFilterTab.PAYMENTS -> conversations.filter { conv ->
                isPaymentMessage(conv.snippet)
            }
            SmsFilterTab.RECHARGE -> conversations.filter { conv ->
                isRechargeMessage(conv.snippet)
            }
            SmsFilterTab.OTP -> conversations.filter { conv ->
                isOtpMessage(conv.snippet)
            }
        }
        return filtered.sortedByDescending { it.date }
    }

    companion object {
        val OTP_REGEX = Regex(
            """\b(otp|passcode|one[- ]time (?:password|passcode|code)|(?:verification|authentication|auth|security|secret|login|access|confirmation)[- ](?:code|pin|password)|your\s+(?:\w+\s+)?code\s+is|is\s+your\s+(?:\w+\s+)?(?:code|pin)|\d{4,8}\s+is\s+your|use\s+code\s+\d{4,8}|enter\s+(?:this\s+)?otp)\b|(?:\b(?:code|pin|otp)\s*[:=]\s*\d{4,8}\b)""",
            RegexOption.IGNORE_CASE
        )

        val PAYMENTS_REGEX = Regex(
            """\b(debited|credited|upi|account|spent|paid|received|balance|inr)\b|\ba/c\b|\brs(?:\.|\b)|₹""",
            RegexOption.IGNORE_CASE
        )

        val RECHARGE_REGEX = Regex(
            """\b(recharge[s|d]?|validity|plan[s]?|expire[ds]?|quota|renewal|renew)\b|\bdata\s*pack\b|\b\d*(?:\.\d+)?\s*gb\s*/\s*day\b|gb/day""",
            RegexOption.IGNORE_CASE
        )

        fun isPaymentMessage(text: String): Boolean = PAYMENTS_REGEX.containsMatchIn(text)
        fun isRechargeMessage(text: String): Boolean = RECHARGE_REGEX.containsMatchIn(text)
        fun isOtpMessage(text: String): Boolean = OTP_REGEX.containsMatchIn(text)
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
                com.modiprincekr.thedialer.modal.data.StarredMessage(
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
