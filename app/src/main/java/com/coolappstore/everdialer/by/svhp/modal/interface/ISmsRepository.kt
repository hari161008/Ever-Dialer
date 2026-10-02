package com.coolappstore.everdialer.by.svhp.modal.`interface`

import com.coolappstore.everdialer.by.svhp.modal.data.SmsConversation
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage

interface ISmsRepository {
    suspend fun getConversations(): List<SmsConversation>
    suspend fun getMessagesForThread(threadId: Long): List<SmsMessage>
    suspend fun getOrCreateThreadId(address: String): Long
    suspend fun sendSms(address: String, body: String, subId: Int? = null): Boolean
    suspend fun deleteThread(threadId: Long): Boolean
    suspend fun deleteMessage(messageId: Long, isMms: Boolean = false): Boolean
    suspend fun markThreadAsRead(threadId: Long)
}
