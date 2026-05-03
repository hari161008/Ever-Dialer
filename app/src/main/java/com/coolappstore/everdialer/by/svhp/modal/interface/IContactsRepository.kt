package com.coolappstore.everdialer.by.svhp.modal.`interface`

import com.coolappstore.everdialer.by.svhp.modal.data.Contact

interface IContactsRepository {
    fun getContacts(): List<Contact>
    fun getContacts(enabledAccountKeys: Set<String>): List<Contact>
    fun getContactById(contactId: String): Contact?
    fun getContactByNumber(number: String): Contact?
    fun toggleFavorite(contactId: String, isFavorite: Boolean)
    fun saveContact(contact: Contact)
    fun deleteContact(contactId: String)
}