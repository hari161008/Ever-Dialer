package com.coolappstore.everdialer.by.svhp.controller.sms

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SmsEventBus {
    private val _newSmsEvent = MutableSharedFlow<Long?>(extraBufferCapacity = 10)
    val newSmsEvent = _newSmsEvent.asSharedFlow()

    fun notifyNewSms(threadId: Long? = null) {
        _newSmsEvent.tryEmit(threadId)
    }
}
