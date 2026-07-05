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
    fun getAvailableAccounts(): List<ContactAccount>
    /** Destinations the user can save a brand-new contact to (Device, Google accounts, SIM cards, etc). */
    fun getSaveTargets(): List<ContactSaveTarget>
    /** Saves a contact's name + first phone number directly to a SIM card's contact storage. */
    fun saveContactToSim(contact: Contact, simSlotIndex: Int): Boolean
}