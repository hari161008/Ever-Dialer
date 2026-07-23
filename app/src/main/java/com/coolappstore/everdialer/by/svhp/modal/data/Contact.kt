package com.coolappstore.everdialer.by.svhp.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class ContactEvent(
    val type: Int,
    val label: String?,
    val date: String
)

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    // Human-readable labels for every account this contact is stored in/synced with, e.g.
    // "jane@gmail.com", "SIM 1", "Device Storage" — a contact merged across multiple sources
    // (e.g. saved on the SIM and also synced to a Google account) will show more than one.
    val sourceAccounts: List<String> = emptyList(),
)

@Serializable
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