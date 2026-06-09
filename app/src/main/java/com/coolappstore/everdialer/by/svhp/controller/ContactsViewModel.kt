package com.coolappstore.everdialer.by.svhp.controller

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(
    application: Application,
    private val contactsRepo: IContactsRepository,
    private val prefs: PreferenceManager
) : AndroidViewModel(application) {

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    val allContacts: StateFlow<List<Contact>> = _allContacts.asStateFlow()

    private val _selectedAccountKey = MutableStateFlow<String?>(null)
    val selectedAccountKey: StateFlow<String?> = _selectedAccountKey.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val availableAccounts: StateFlow<List<ContactAccount>> = _availableAccounts.asStateFlow()

    init {
        fetchContacts()
        fetchAvailableAccounts()
    }

    fun fetchContacts() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sessionKey = _selectedAccountKey.value
                if (sessionKey != null) {
                    contactsRepo.getContacts(setOf(sessionKey))
                } else {
                    val enabledKeys = getEnabledAccountKeys()
                    if (enabledKeys.isEmpty()) contactsRepo.getContacts()
                    else contactsRepo.getContacts(enabledKeys)
                }
            }.onSuccess { _allContacts.value = it }
        }
    }

    fun fetchAvailableAccounts() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { contactsRepo.getAvailableAccounts() }
                .onSuccess { _availableAccounts.value = it }
        }
    }

    fun setAccountFilter(key: String?) {
        _selectedAccountKey.value = key
        fetchContacts()
    }

    private fun getEnabledAccountKeys(): Set<String> {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, null)
        return if (raw.isNullOrBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
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
