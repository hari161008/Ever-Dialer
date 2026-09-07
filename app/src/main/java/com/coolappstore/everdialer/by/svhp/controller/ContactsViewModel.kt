package com.coolappstore.everdialer.by.svhp.controller

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.controller.util.ContactsCache
import com.coolappstore.everdialer.by.svhp.controller.util.ContactsHiderManager
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
import kotlinx.coroutines.withContext

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
    private var observerJob: Job? = null
    private var hasLoadedFromCache = false

    private val contactsContentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
            observerJob?.cancel()
            observerJob = viewModelScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(300)
                fetchContacts()
                fetchContactGroups()
                fetchAvailableAccounts()
            }
        }
    }

    init {
        _enabledAccountKeys.value = getEnabledAccountKeys()
        fetchContactGroups()
        loadCachedContactsThenRefresh()
        fetchAvailableAccounts()

        try {
            val resolver = getApplication<Application>().contentResolver
            resolver.registerContentObserver(android.provider.ContactsContract.Contacts.CONTENT_URI, true, contactsContentObserver)
            resolver.registerContentObserver(android.provider.ContactsContract.Groups.CONTENT_URI, true, contactsContentObserver)
            resolver.registerContentObserver(android.provider.ContactsContract.Data.CONTENT_URI, true, contactsContentObserver)
        } catch (_: Exception) {}
    }

    fun fetchContactGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            val rawLocalGroups = prefs.getContactGroups()
            val order = prefs.getContactsDisplayOrder()

            // 1. Fetch system groups from ContactsContract (Google, OEM accounts, etc.)
            val sysGroups = runCatching { contactsRepo.getSystemContactGroups() }.getOrDefault(emptyList())

            // Merge system groups into local groups if not already present
            val localGroupMap = rawLocalGroups.associateBy { it.id }.toMutableMap()
            for (sg in sysGroups) {
                if (isSystemGroupEligible(sg)) {
                    val existing = localGroupMap[sg.id]
                    if (existing == null) {
                        localGroupMap[sg.id] = sg
                    } else {
                        // Update account info or contacts if changed
                        localGroupMap[sg.id] = existing.copy(
                            accountType = sg.accountType ?: existing.accountType,
                            accountName = sg.accountName ?: existing.accountName,
                            targetLabel = sg.targetLabel ?: existing.targetLabel,
                            contactIds = if (existing.contactIds.isEmpty() && sg.contactIds.isNotEmpty()) sg.contactIds else existing.contactIds
                        )
                    }
                }
            }
            val mergedRawGroups = localGroupMap.values.toList()

            // 2. Filter groups
            val cleanGroups = mergedRawGroups.filter { group ->
                isLegitimateUserGroup(group, order)
            }

            if (cleanGroups.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _contactGroups.value = emptyList()
                    if (_selectedGroupId.value != null) {
                        _selectedGroupId.value = null
                        updateDisplayedContacts()
                    }
                }
                return@launch
            }

            // 3. Identify system-backed groups (e.g. Gmail / Google Contacts)
            val groupToRowIdMap = mutableMapOf<String, Long>()
            val systemBackedGroupIds = mutableSetOf<String>()

            for (g in cleanGroups) {
                if (g.id.startsWith("sys_group_")) {
                    systemBackedGroupIds.add(g.id)
                    g.id.removePrefix("sys_group_").toLongOrNull()?.let {
                        groupToRowIdMap[g.id] = it
                    }
                } else if (!g.accountType.isNullOrBlank() || !g.accountName.isNullOrBlank()) {
                    systemBackedGroupIds.add(g.id)
                    val rowId = runCatching { contactsRepo.findSystemGroupId(g.name, g.accountType, g.accountName) }.getOrNull()
                    if (rowId != null) {
                        groupToRowIdMap[g.id] = rowId
                    }
                }
            }

            // 4. Query which system groups still exist (DELETED = 0)
            val activeSystemRowIds = if (groupToRowIdMap.isNotEmpty()) {
                runCatching { contactsRepo.getActiveSystemGroupIds(groupToRowIdMap.values.toSet()) }.getOrDefault(emptySet())
            } else {
                emptySet()
            }

            // 5. Filter out any system-backed group that was deleted in Google Contacts / system provider
            // If activeSystemRowIds check returned empty (e.g. OEM query limitation), do not aggressively delete
            val survivingGroups = cleanGroups.filter { g ->
                if (g.id in systemBackedGroupIds) {
                    val rowId = groupToRowIdMap[g.id]
                    if (rowId == null) true
                    else if (activeSystemRowIds.isNotEmpty()) rowId in activeSystemRowIds
                    else true // Keep if activeSystemRowIds query was empty to prevent accidental wiping
                } else {
                    // Local-only group is independent of system provider
                    true
                }
            }

            // Remove any confirmed externally deleted groups from prefs and display order
            val deletedGroupIds = cleanGroups.map { it.id }.toSet() - survivingGroups.map { it.id }.toSet()
            for (delId in deletedGroupIds) {
                prefs.deleteContactGroup(delId)
            }

            if (survivingGroups.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _contactGroups.value = emptyList()
                    if (_selectedGroupId.value != null) {
                        _selectedGroupId.value = null
                        updateDisplayedContacts()
                    }
                }
                return@launch
            }

            // 6. Query system group memberships ONLY for surviving groups
            val survivingRowIds = survivingGroups.mapNotNull { groupToRowIdMap[it.id] }.toSet()
            val memberMap = if (survivingRowIds.isNotEmpty()) {
                runCatching { contactsRepo.getSystemGroupMembers(survivingRowIds) }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            // 7. Update contact memberships if any contact was added or removed
            var anyChanged = (survivingGroups.size != rawLocalGroups.size)
            val updatedList = survivingGroups.map { g ->
                val sysRowId = groupToRowIdMap[g.id]
                if (sysRowId != null && (memberMap.containsKey(sysRowId) || g.id.startsWith("sys_group_"))) {
                    val latestMemberIds = memberMap[sysRowId]?.distinct() ?: g.contactIds
                    if (latestMemberIds.toSet() != g.contactIds.toSet()) {
                        anyChanged = true
                        g.copy(contactIds = latestMemberIds)
                    } else {
                        g
                    }
                } else {
                    g
                }
            }

            if (anyChanged) {
                prefs.saveContactGroups(updatedList)
            }

            withContext(Dispatchers.Main) {
                _contactGroups.value = updatedList
                if (_selectedGroupId.value in deletedGroupIds) {
                    _selectedGroupId.value = null
                    updateDisplayedContacts()
                } else if (_selectedGroupId.value != null) {
                    updateDisplayedContacts()
                }
            }
        }
    }

    private fun isSystemGroupEligible(group: com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup): Boolean {
        val name = group.name.trim()
        if (name.isBlank()) return false
        if (name.startsWith("System Group:", ignoreCase = true)) return false
        if (name.equals("Starred in Android", ignoreCase = true)) return false
        if (name.equals("My Contacts", ignoreCase = true)) return false
        return true
    }

    private fun isLegitimateUserGroup(group: com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup, order: List<String>): Boolean {
        // 1. All groups created in Ever Dialer locally have UUID IDs (never start with sys_group_)
        if (!group.id.startsWith("sys_group_")) return true
        // 2. Groups created via Ever Dialer UI are tracked in display order
        if ("group_${group.id}" in order) return true
        // 3. Keep user groups from Gmail/Google Contacts or OEM system provider if eligible
        return isSystemGroupEligible(group)
    }

    fun cleanupAutoImportedGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            val raw = prefs.getContactGroups()
            val order = prefs.getContactsDisplayOrder()
            val clean = raw.filter { group ->
                isLegitimateUserGroup(group, order)
            }
            prefs.saveContactGroups(clean)
            fetchContactGroups()
        }
    }

    fun clearAllContactGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.saveContactGroups(emptyList())
            val order = prefs.getContactsDisplayOrder().filterNot { it.startsWith("group_") }
            prefs.setContactsDisplayOrder(order)
            withContext(Dispatchers.Main) {
                _contactGroups.value = emptyList()
                if (_selectedGroupId.value != null) {
                    _selectedGroupId.value = null
                    updateDisplayedContacts()
                }
            }
        }
    }

    fun addContactGroup(group: com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalGroup = group
            if (!group.accountType.isNullOrBlank() || !group.accountName.isNullOrBlank() || group.id.startsWith("sys_group_")) {
                val sysId = runCatching { contactsRepo.saveSystemContactGroup(group) }.getOrNull()
                if (sysId != null) {
                    finalGroup = group.copy(id = sysId)
                }
            }
            prefs.addContactGroup(finalGroup)
            fetchContactGroups()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                if (_selectedGroupId.value == group.id || _selectedGroupId.value == finalGroup.id) {
                    updateDisplayedContacts()
                }
            }
        }
    }

    fun deleteContactGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { contactsRepo.deleteSystemContactGroup(groupId) }
            prefs.deleteContactGroup(groupId)
            fetchContactGroups()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                if (_selectedGroupId.value == groupId) {
                    _selectedGroupId.value = null
                    updateDisplayedContacts()
                }
            }
        }
    }

    fun toggleContactGroupVisibility(groupId: String, isHidden: Boolean) {
        prefs.setContactGroupHidden(groupId, isHidden)
        fetchContactGroups()
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
                val backedUp = ContactsHiderManager.getBackedUpContacts(prefs).values.toList()
                (raw + backedUp).distinctBy { it.id }
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
        val hiddenIds = ContactsHiderManager.getHiddenIds(prefs)

        if (groupId != null) {
            val group = _contactGroups.value.find { it.id == groupId } ?: prefs.getContactGroups().find { it.id == groupId }
            val groupContactIds = group?.contactIds?.toSet() ?: emptySet()
            _displayedContacts.value = baseContacts.filter { it.id in groupContactIds && (hiddenIds.isEmpty() || it.id !in hiddenIds) }
        } else if (sessionKey != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val raw = contactsRepo.getContacts(setOf(sessionKey))
                val backedUp = ContactsHiderManager.getBackedUpContacts(prefs).values.toList()
                val merged = (raw + backedUp).distinctBy { it.id }
                val filtered = if (hiddenIds.isEmpty()) merged else merged.filter { it.id !in hiddenIds }
                _displayedContacts.value = filtered
            }
        } else {
            _displayedContacts.value = if (hiddenIds.isEmpty()) baseContacts else baseContacts.filter { it.id !in hiddenIds }
        }
    }

    fun updateHiddenContacts(newHiddenIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val hideEverywhere = ContactsHiderManager.isHideEverywhereEnabled(prefs)
            val updated = ContactsHiderManager.syncPhonebookState(
                context = getApplication(),
                prefs = prefs,
                contactsRepo = contactsRepo,
                allContacts = _allContacts.value,
                hiddenIds = newHiddenIds,
                hideEverywhere = hideEverywhere
            )
            prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, updated.joinToString(","))
            fetchContacts()
        }
    }

    fun setHideEverywhere(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_EVERYWHERE, enabled)
            prefs.setBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_IN_CONTACTS, enabled)
            val hiddenIds = ContactsHiderManager.getHiddenIds(prefs)
            val updated = ContactsHiderManager.syncPhonebookState(
                context = getApplication(),
                prefs = prefs,
                contactsRepo = contactsRepo,
                allContacts = _allContacts.value,
                hiddenIds = hiddenIds,
                hideEverywhere = enabled
            )
            prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, updated.joinToString(","))
            fetchContacts()
        }
    }

    fun unhideContact(contactId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentHidden = ContactsHiderManager.getHiddenIds(prefs).toMutableSet()
            currentHidden.remove(contactId)
            ContactsHiderManager.restoreToPhonebook(getApplication(), prefs, contactsRepo, contactId)
            prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, currentHidden.joinToString(","))
            fetchContacts()
        }
    }

    fun fetchAvailableAccounts() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch(Dispatchers.IO) {
            val hiddenIds = ContactsHiderManager.getHiddenIds(prefs)
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

    fun saveContact(
        contact: Contact,
        accountType: String? = null,
        accountName: String? = null,
        updateAllAccounts: Boolean = false,
        originalContact: Contact? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Optimistically update memory and cache for instant UI feedback
            if (contact.id.isNotEmpty() && contact.id != "0") {
                val current = _allContacts.value
                val updated = current.map { if (it.id == contact.id) contact else it }
                _allContacts.value = updated
                updateDisplayedContacts(updated)
                ContactsCache.write(getApplication(), updated)
            }
            contactsRepo.saveContact(contact, accountType, accountName, updateAllAccounts, originalContact)
            fetchContacts()
        }
    }

    fun getContactAccounts(contactId: String): List<com.coolappstore.everdialer.by.svhp.modal.data.ContactAccountInfo> =
        contactsRepo.getContactAccounts(contactId)

    fun updateContactNote(contactId: String, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            // Optimistically update memory and cache
            val current = _allContacts.value
            val updated = current.map { if (it.id == contactId) it.copy(note = note) else it }
            _allContacts.value = updated
            updateDisplayedContacts(updated)
            ContactsCache.write(getApplication(), updated)
            
            contactsRepo.updateContactNote(contactId, note)
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

    fun deleteContact(contactId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteContact(contactId)
            fetchContacts()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteRawContact(rawContactId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            contactsRepo.deleteRawContact(rawContactId)
            fetchContacts()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(contactsContentObserver)
        } catch (_: Exception) {}
    }
}
