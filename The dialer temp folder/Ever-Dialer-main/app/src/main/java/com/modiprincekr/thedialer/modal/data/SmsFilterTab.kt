package com.modiprincekr.thedialer.modal.data

enum class SmsFilterTab(val id: String, val displayName: String) {
    ALL("all", "All"),
    FAVOURITES("favourites", "Favourites"),
    STARRED("starred", "Starred"),
    CONTACTS("contacts", "Contacts"),
    UNKNOWN("unknown", "Unknown"),
    PAYMENTS("payments", "Payments"),
    RECHARGE("recharge", "Recharge"),
    OTP("otp", "OTP");

    companion object {
        val DEFAULT_ORDER: List<SmsFilterTab> = listOf(
            ALL,
            FAVOURITES,
            STARRED,
            CONTACTS,
            UNKNOWN,
            PAYMENTS,
            RECHARGE,
            OTP
        )

        fun fromId(id: String): SmsFilterTab {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: ALL
        }

        fun parseOrder(saved: String?): List<SmsFilterTab> {
            if (saved.isNullOrBlank()) return DEFAULT_ORDER
            val savedTabs = saved.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { id -> entries.find { it.id.equals(id, ignoreCase = true) } }
                .distinct()
            val missingTabs = DEFAULT_ORDER.filter { it !in savedTabs }
            return savedTabs + missingTabs
        }

        fun serializeOrder(order: List<SmsFilterTab>): String {
            return order.joinToString(",") { it.id }
        }
    }
}
