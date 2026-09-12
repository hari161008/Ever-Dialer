package com.coolappstore.everdialer.by.svhp.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class ContactEvent(
    val type: Int,
    val label: String?,
    val date: String
)

@Serializable
data class ContactPhone(
    val number: String,
    val type: Int = android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
    val label: String? = null
)

fun getPhoneTypeLabel(type: Int, customLabel: String? = null): String {
    return when (type) {
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel?.ifBlank { "Custom" } ?: "Custom"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Pager"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK -> "Callback"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_CAR -> "Car"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> "Company Main"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_ISDN -> "ISDN"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_RADIO -> "Radio"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_TELEX -> "Telex"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD -> "TTY/TDD"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> "Work Pager"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> "Assistant"
        android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> "MMS"
        else -> customLabel?.ifBlank { "Phone" } ?: "Phone"
    }
}

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String> = emptyList(),
    val phones: List<ContactPhone> = emptyList(),
    val emails: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    // Human-readable labels for every account this contact is stored in/synced with, e.g.
    // "jane@gmail.com", "SIM 1", "Device Storage" — a contact merged across multiple sources
    // (e.g. saved on the SIM and also synced to a Google account) will show more than one.
    val sourceAccounts: List<String> = emptyList(),
    // Custom note/description synced with Microsoft Exchange / Google Contacts / ContactsContract
    val note: String? = null,
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

/**
 * Details of a specific account/storage location an existing contact is stored in.
 */
@Serializable
data class ContactAccountInfo(
    val rawContactId: Long,
    val accountType: String?,
    val accountName: String?,
    val displayName: String,
    val isReadOnly: Boolean,
    val isSim: Boolean,
    val simSlotIndex: Int = -1,
    val description: String? = null
)