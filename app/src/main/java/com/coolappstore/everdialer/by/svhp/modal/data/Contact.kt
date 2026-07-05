package com.coolappstore.everdialer.by.svhp.modal.data

data class ContactEvent(
    val type: Int,
    val label: String?,
    val date: String
)

data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
)

data class ContactAccount(
    val key: String,
    val displayName: String,
    val accountType: String,
    val accountName: String,
    val contactCount: Int = 0
)

/**
 * A destination the user can choose to save a new contact to, shown in the
 * "Save contact to..." popup after tapping Save on a new contact.
 */
data class ContactSaveTarget(
    val label: String,
    val subLabel: String? = null,
    // null accountType/accountName = device/phone storage
    val accountType: String? = null,
    val accountName: String? = null,
    val isSim: Boolean = false,
    val simSlotIndex: Int = 0
)