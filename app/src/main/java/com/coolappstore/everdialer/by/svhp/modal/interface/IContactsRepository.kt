package com.coolappstore.everdialer.by.svhp.modal.`interface`

import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget

interface IContactsRepository {
    fun getContacts(): List<Contact>
    fun getContacts(enabledAccountKeys: Set<String>): List<Contact>
    fun getContactById(contactId: String): Contact?
    fun getContactByNumber(number: String): Contact?
    fun toggleFavorite(contactId: String, isFavorite: Boolean)
    fun saveContact(contact: Contact, accountType: String? = null, accountName: String? = null)
    fun deleteContact(contactId: String)
    fun getAvailableAccounts(excludedContactIds: Set<String> = emptySet()): List<ContactAccount>
    /** Destinations the user can save a brand-new contact to (Device, Google accounts, SIM cards, etc). */
    fun getSaveTargets(): List<ContactSaveTarget>
    /** Saves a contact's name + first phone number directly to a SIM card's contact storage. */
    fun saveContactToSim(contact: Contact, simSlotIndex: Int): Boolean
    /** Moves an existing contact to a different storage/account: creates it at the destination
     *  and removes only the raw contact(s) tied to its current account(s), leaving any other
     *  raw contacts merged into the same aggregate (e.g. from a different account) untouched. */
    fun moveContact(contact: Contact, target: ContactSaveTarget): Boolean
}