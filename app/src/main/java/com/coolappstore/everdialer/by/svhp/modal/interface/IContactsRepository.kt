package com.coolappstore.everdialer.by.svhp.modal.`interface`

import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccountInfo
import com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget

interface IContactsRepository {
    fun getContacts(): List<Contact>
    fun getContacts(enabledAccountKeys: Set<String>): List<Contact>
    fun getContactById(contactId: String): Contact?
    fun getContactByNumber(number: String): Contact?
    fun toggleFavorite(contactId: String, isFavorite: Boolean)
    fun saveContact(
        contact: Contact,
        accountType: String? = null,
        accountName: String? = null,
        updateAllAccounts: Boolean = false,
        originalContact: Contact? = null
    )
    fun getContactAccounts(contactId: String): List<ContactAccountInfo>
    fun updateContactNote(contactId: String, note: String?)
    fun deleteContact(contactId: String)
    fun deleteRawContact(rawContactId: Long)
    fun getAvailableAccounts(excludedContactIds: Set<String> = emptySet()): List<ContactAccount>
    /** Destinations the user can save a brand-new contact to (Device, Google accounts, SIM cards, etc). */
    fun getSaveTargets(): List<ContactSaveTarget>
    /** Saves a contact's name + first phone number directly to a SIM card's contact storage. */
    fun saveContactToSim(contact: Contact, simSlotIndex: Int): Boolean
    /** Moves an existing contact to a different storage/account: creates it at the destination
     *  and removes only the raw contact(s) tied to its current account(s), leaving any other
     *  raw contacts merged into the same aggregate (e.g. from a different account) untouched. */
    fun moveContact(contact: Contact, target: ContactSaveTarget): Boolean
    /** Retrieves contact groups/labels from the system Contacts provider (Gmail, Exchange, etc.) */
    fun getSystemContactGroups(): List<com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup>
    /** Retrieves the member contact IDs for the specified system group row IDs without importing all groups */
    fun getSystemGroupMembers(groupRowIds: Set<Long>): Map<Long, List<String>>
    /** Finds the system group row ID for a group title and optional account */
    fun findSystemGroupId(groupName: String, accountType: String?, accountName: String?): Long?
    /** Creates or updates a contact group/label in the system Contacts provider with members */
    fun saveSystemContactGroup(group: com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup): String?
    /** Deletes a contact group/label and its memberships from the system Contacts provider */
    fun deleteSystemContactGroup(groupId: String): Boolean
}