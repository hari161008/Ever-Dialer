package com.coolappstore.everdialer.by.svhp.controller

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.controller.util.ContactsCache
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

    private val _displayedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val displayedContacts: StateFlow<List<Contact>> = _displayedContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedAccountKey = MutableStateFlow<String?>(null)
    val selectedAccountKey: StateFlow<String?> = _selectedAccountKey.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _contactGroups = MutableStateFlow<List<com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup>>(emptyList())
    val contactGroups: StateFlow<List<com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup>> = _contactGroups.asStateFlow()

    private val _availableAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    val availableAccounts: StateFlow<List<ContactAccount>> = _availableAccounts.asStateFlow()

    private val _enabledAccountKeys = MutableStateFlow<Set<String>?>(null)
    val enabledAccountKeys: StateFlow<Set<String>?> = _enabledAccountKeys.asStateFlow()

    private var fetchJob: Job? = null
    private var hasLoadedFromCache = false

    init {
        _enabledAccountKeys.value = getEnabledAccountKeys()
        fetchContactGroups()
        loadCachedContactsThenRefresh()
        fetchAvailableAccounts()
    }

    fun fetchContactGroups() {
        _contactGroups.value = prefs.getContactGroups()
    }

    fun addContactGroup(group: com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup) {
        prefs.addContactGroup(group)
        fetchContactGroups()
        if (_selectedGroupId.value == group.id) {
            updateDisplayedContacts()
        }
    }

    fun deleteContactGroup(groupId: String) {
        prefs.deleteContactGroup(groupId)
        fetchContactGroups()
        if (_selectedGroupId.value == groupId) {
            _selectedGroupId.value = null
            updateDisplayedContacts()
        }
    }

    fun reorderContactGroups(groups: List<com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup>) {
        prefs.saveContactGroups(groups)
        _contactGroups.value = groups
    }

    /** Shows the last-known contacts list from disk immediately (near-instant even for a few
     *  thousand contacts, unlike a live ContentResolver query) so the Contacts tab and unified
     *  Search have something to render and filter against right away, then kicks off a real
     *  fetch in the background to replace it with up-to-date data and refresh the cache. */
    private fun loadCachedContactsThenRefresh() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            _isLoading.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val cached = runCatching { ContactsCache.read(ctx) }.getOrDefault(emptyList())
            if (cached.isNotEmpty() && !hasLoadedFromCache) {
                _allContacts.value = cached
                updateDisplayedContacts(cached)
                _isLoading.value = false
            }
            fetchContacts()
        }
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
        // Only show the blocking loading state if we don't already have a cached list on
        // screen — otherwise this becomes a silent background refresh.
        if (_allContacts.value.isEmpty()) _isLoading.value = true
        val enabledKeys = getEnabledAccountKeys()
        val isUnfiltered = enabledKeys == null
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val raw = if (enabledKeys == null) {
                    contactsRepo.getContacts()
                } else if (enabledKeys.isEmpty()) {
                    emptyList()
                } else {
                    contactsRepo.getContacts(enabledKeys)
                }
                // Filter out hidden contacts from the main list
                val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
                val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet()
                               else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
                if (hiddenIds.isEmpty()) raw else raw.filter { it.id !in hiddenIds }
            }.onSuccess { contacts ->
                hasLoadedFromCache = true
                _allContacts.value = contacts
                updateDisplayedContacts(contacts)
                _isLoading.value = false
                if (isUnfiltered) ContactsCache.write(ctx, contacts)
            }.onFailure {
                _isLoading.value = false
            }
        }
    }

    private fun updateDisplayedContacts(baseContacts: List<Contact> = _allContacts.value) {
        val groupId = _selectedGroupId.value
        val sessionKey = _selectedAccountKey.value
        if (groupId != null) {
            val group = prefs.getContactGroups().find { it.id == groupId }
            val groupContactIds = group?.contactIds?.toSet() ?: emptySet()
            _displayedContacts.value = baseContacts.filter { it.id in groupContactIds }
        } else if (sessionKey != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val raw = contactsRepo.getContacts(setOf(sessionKey))
                val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
                val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet()
                               else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
                val filtered = if (hiddenIds.isEmpty()) raw else raw.filter { it.id !in hiddenIds }
                _displayedContacts.value = filtered
            }
        } else {
            _displayedContacts.value = baseContacts
        }
    }

    fun fetchAvailableAccounts() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch(Dispatchers.IO) {
            val hiddenIdsRaw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
            val hiddenIds = if (hiddenIdsRaw.isBlank()) emptySet()
                           else hiddenIdsRaw.split(",").filter { it.isNotBlank() }.toSet()
            runCatching { contactsRepo.getAvailableAccounts(hiddenIds) }
                .onSuccess { _availableAccounts.value = it }
        }
    }

    fun setAccountFilter(key: String?) {
        _selectedGroupId.value = null
        _selectedAccountKey.value = key
        updateDisplayedContacts()
    }

    fun setGroupFilter(groupId: String?) {
        _selectedAccountKey.value = null
        _selectedGroupId.value = groupId
        updateDisplayedContacts()
    }

    fun clearFilters() {
        _selectedAccountKey.value = null
        _selectedGroupId.value = null
        updateDisplayedContacts()
    }

    fun getEnabledAccountKeys(): Set<String>? {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, null)
        if (raw == null) return null
        if (raw == "__NONE__" || raw.isBlank()) return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun setEnabledAccountKeys(keys: Set<String>?) {
        _enabledAccountKeys.value = keys
        if (keys == null) {
            prefs.setString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, null)
        } else if (keys.isEmpty()) {
            prefs.setString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, "__NONE__")
        } else {
            prefs.setString(PreferenceManager.KEY_CONTACTS_DISPLAY_ACCOUNTS, keys.joinToString(","))
        }
        fetchContacts()
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

    fun getContactById(contactId: String): Contact? = contactsRepo.getContactById(contactId)

    fun deleteContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContact(contactId)
            fetchContacts()
        }
    }
}
