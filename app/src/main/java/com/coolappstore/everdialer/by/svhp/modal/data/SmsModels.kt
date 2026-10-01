package com.coolappstore.everdialer.by.svhp.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val photoUri: String?,
    val snippet: String,
    val date: Long,
    val unreadCount: Int = 0,
    val isRead: Boolean = true,
    val subId: Int? = null
)

@Serializable
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int, // 1 = inbox, 2 = sent, 3 = draft, 4 = outbox, 5 = failed, 6 = queued
    val isRead: Boolean = true,
    val subId: Int? = null
) {
    val isOutgoing: Boolean
        get() = type == android.provider.Telephony.Sms.MESSAGE_TYPE_SENT ||
                type == android.provider.Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                type == android.provider.Telephony.Sms.MESSAGE_TYPE_FAILED ||
                type == android.provider.Telephony.Sms.MESSAGE_TYPE_QUEUED
}
