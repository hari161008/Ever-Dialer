package com.modiprincekr.thedialer.modal.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.util.UUID

@Immutable
@Serializable
data class WhatsAppCallRecord(
    val id: String = UUID.randomUUID().toString(),
    val callerName: String,
    val phoneNumber: String? = null,
    val timestamp: Long,
    val durationSeconds: Long = 0L,
    val callType: Int, // android.provider.CallLog.Calls.INCOMING_TYPE, OUTGOING_TYPE, MISSED_TYPE
    val isVideo: Boolean = false,
    val sourcePackage: String = "com.whatsapp",
    val photoUri: String? = null,
    val contactId: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("callerName", callerName)
        put("phoneNumber", phoneNumber ?: "")
        put("timestamp", timestamp)
        put("durationSeconds", durationSeconds)
        put("callType", callType)
        put("isVideo", isVideo)
        put("sourcePackage", sourcePackage)
        put("photoUri", photoUri ?: "")
        put("contactId", contactId ?: "")
    }

    companion object {
        fun fromJson(json: JSONObject): WhatsAppCallRecord? {
            return try {
                WhatsAppCallRecord(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    callerName = json.getString("callerName"),
                    phoneNumber = json.optString("phoneNumber").takeIf { it.isNotBlank() },
                    timestamp = json.getLong("timestamp"),
                    durationSeconds = json.optLong("durationSeconds", 0L),
                    callType = json.getInt("callType"),
                    isVideo = json.optBoolean("isVideo", false),
                    sourcePackage = json.optString("sourcePackage", "com.whatsapp"),
                    photoUri = json.optString("photoUri").takeIf { it.isNotBlank() },
                    contactId = json.optString("contactId").takeIf { it.isNotBlank() }
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
