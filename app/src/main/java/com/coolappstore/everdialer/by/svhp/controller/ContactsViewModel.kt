package com.coolappstore.everdialer.by.svhp.controller

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(
    application: Application,
    private val contactsRepo: IContactsRepository
) : AndroidViewModel(application) {

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    init {
        fetchContacts()
    }

    fun fetchContacts() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { contactsRepo.getContacts() }
                .onSuccess { _allContacts.value = it }
        }
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.toggleFavorite(contact.id, !contact.isFavorite)
            fetchContacts()
        }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.saveContact(contact)
            fetchContacts()
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContact(contactId)
            fetchContacts()
        }
    }
}