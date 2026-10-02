package com.coolappstore.everdialer.by.svhp.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class MmsPart(
    val id: Long,
    val messageId: Long,
    val contentType: String,
    val uri: String? = null,
    val text: String? = null
) {
    val isImage: Boolean
        get() = contentType.startsWith("image/")
    val isAudio: Boolean
        get() = contentType.startsWith("audio/")
    val isVideo: Boolean
        get() = contentType.startsWith("video/")
}

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
    val subId: Int? = null,
    val recipientIds: List<Long> = emptyList(),
    val allAddresses: List<String> = emptyList()
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
    val isMms: Boolean = false,
    val subject: String? = null,
    val parts: List<MmsPart> = emptyList(),
    val subId: Int? = null,
    val deliveryStatus: Int = -1 // -1 = none, 0 = complete/received, 64 = pending, 128 = failed
) {
    val isOutgoing: Boolean
        get() = type == android.provider.Telephony.Sms.MESSAGE_TYPE_SENT ||
                type == android.provider.Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                type == android.provider.Telephony.Sms.MESSAGE_TYPE_FAILED ||
                type == android.provider.Telephony.Sms.MESSAGE_TYPE_QUEUED
}

@Serializable
data class ScheduledSmsEntry(
    val id: String,
    val threadId: Long,
    val address: String,
    val body: String,
    val subId: Int? = null,
    val scheduledTime: Long
)
