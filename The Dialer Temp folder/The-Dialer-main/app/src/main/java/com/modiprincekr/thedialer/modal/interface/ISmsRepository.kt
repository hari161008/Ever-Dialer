package com.modiprincekr.thedialer.modal.`interface`

import com.modiprincekr.thedialer.modal.data.SmsConversation
import com.modiprincekr.thedialer.modal.data.SmsMessage

interface ISmsRepository {
    suspend fun getConversations(): List<SmsConversation>
    suspend fun getMessagesForThread(threadId: Long): List<SmsMessage>
    suspend fun getOrCreateThreadId(address: String): Long
    suspend fun sendSms(address: String, body: String, subId: Int? = null): Boolean
    suspend fun deleteThread(threadId: Long): Boolean
    suspend fun deleteMessage(messageId: Long, isMms: Boolean = false): Boolean
    suspend fun markThreadAsRead(threadId: Long)
    suspend fun markThreadAsUnread(threadId: Long)
    suspend fun getMessagesByIds(messageIds: Set<Long>): List<SmsMessage>
    fun resolveContactInfo(address: String): Pair<String?, String?>
}

