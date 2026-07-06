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
import kotlinx.coroutines.Job
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedAccountKey = MutableStateFlow<String?>(null)
    val selectedAccountKey: StateFlow<String?> = _selectedAccountKey.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val availableAccounts: StateFlow<List<ContactAccount>> = _availableAccounts.asStateFlow()

    // Tracks the in-flight fetch job so a fast follow-up filter change (e.g. tapping SIM, then
    // Gmail, then WhatsApp in quick succession) cancels the older, still-running fetch instead
    // of letting it race the newer one and potentially overwrite fresh results with stale ones
    // — or leave the loading spinner stuck if it never completes.
    private var fetchJob: Job? = null

    init {
        fetchContacts()
        fetchAvailableAccounts()
    }

    fun fetchContacts() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            fetchJob?.cancel()
            _isLoading.value = false
            return
        }
        fetchJob?.cancel()
        _isLoading.value = true
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sessionKey = _selectedAccountKey.value
                val raw = if (sessionKey != null) {
                    contactsRepo.getContacts(setOf(sessionKey))
                } else {
                    val enabledKeys = getEnabledAccountKeys()
                    if (enabledKeys.isEmpty()) contactsRepo.getContacts()
                    else contactsRepo.getContacts(enabledKeys)
                }
                // Filter out hidden contacts from the main list
                val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
                val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet()
                               else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
                if (hiddenIds.isEmpty()) raw else raw.filter { it.id !in hiddenIds }
            }.onSuccess {
                _allContacts.value = it
                _isLoading.value = false
            }.onFailure {
                _isLoading.value = false
            }
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

    fun saveContact(contact: Contact, accountType: String? = null, accountName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.saveContact(contact, accountType, accountName)
            fetchContacts()
        }
    }

    fun saveContactToSim(contact: Contact, simSlotIndex: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = contactsRepo.saveContactToSim(contact, simSlotIndex)
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun getSaveTargets() = contactsRepo.getSaveTargets()

    /** Moves [contact] to a different account/storage (e.g. Google, a SIM, or device storage). */
    fun moveContact(contact: Contact, target: com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = runCatching { contactsRepo.moveContact(contact, target) }.getOrDefault(false)
            fetchContacts()
            fetchAvailableAccounts()
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(success) }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContact(contactId)
            fetchContacts()
        }
    }
}
