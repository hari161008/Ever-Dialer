package com.modiprincekr.thedialer.modal.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.util.UUID

@Immutable
@Serializable
data class AlternateNumber(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String = normalizeDigits(phoneNumber),
    val categoryTag: String = TAG_OTHER,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("phoneNumber", phoneNumber)
        put("normalizedNumber", normalizedNumber)
        put("categoryTag", categoryTag)
        put("note", note)
        put("createdAt", createdAt)
    }

    companion object {
        const val TAG_ALL = "All"
        const val TAG_SHOP = "Shop"
        const val TAG_DELIVERY = "Delivery"
        const val TAG_STUDIO = "Studio"
        const val TAG_WORK = "Work"
        const val TAG_OTHER = "Other"

        val PRESET_TAGS = listOf(TAG_SHOP, TAG_DELIVERY, TAG_STUDIO, TAG_WORK, TAG_OTHER)

        fun normalizeDigits(number: String): String {
            return number.filter { it.isDigit() }
        }

        fun fromJson(json: JSONObject): AlternateNumber? = try {
            val phone = json.getString("phoneNumber")
            AlternateNumber(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.getString("name"),
                phoneNumber = phone,
                normalizedNumber = json.optString("normalizedNumber").ifBlank { normalizeDigits(phone) },
                categoryTag = json.optString("categoryTag", TAG_OTHER),
                note = json.optString("note", ""),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }
}
