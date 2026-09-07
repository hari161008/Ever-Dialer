package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ContactsHiderManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun getHiddenIds(prefs: PreferenceManager): Set<String> {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun isHideEverywhereEnabled(prefs: PreferenceManager): Boolean {
        return prefs.getBoolean(
            PreferenceManager.KEY_CONTACTS_HIDER_HIDE_EVERYWHERE,
            prefs.getBoolean(PreferenceManager.KEY_CONTACTS_HIDER_HIDE_IN_CONTACTS, false)
        )
    }

    fun getBackedUpContacts(prefs: PreferenceManager): Map<String, Contact> {
        val raw = prefs.getString(PreferenceManager.KEY_CONTACTS_HIDER_BACKED_UP_DATA, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, Contact>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveBackedUpContacts(prefs: PreferenceManager, map: Map<String, Contact>) {
        try {
            val encoded = json.encodeToString(map)
            prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_BACKED_UP_DATA, encoded)
        } catch (_: Exception) {}
    }

    private fun persistContactPhotoLocally(context: Context, contactId: String, photoUriStr: String?): String? {
        if (photoUriStr.isNullOrBlank()) return null
        if (photoUriStr.startsWith("file://")) return photoUriStr
        return try {
            val photoFile = File(context.filesDir, "hidden_contact_${contactId}.jpg")
            val uri = Uri.parse(photoUriStr)
            context.contentResolver.openInputStream(uri)?.use { input ->
                photoFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (photoFile.exists()) Uri.fromFile(photoFile).toString() else photoUriStr
        } catch (_: Exception) {
            photoUriStr
        }
    }

    fun hideFromPhonebook(
        context: Context,
        prefs: PreferenceManager,
        contactsRepo: IContactsRepository,
        contact: Contact
    ) {
        val backupMap = getBackedUpContacts(prefs).toMutableMap()
        val localPhoto = persistContactPhotoLocally(context, contact.id, contact.photoUri)
        val preparedContact = contact.copy(photoUri = localPhoto)
        backupMap[contact.id] = preparedContact
        saveBackedUpContacts(prefs, backupMap)

        contactsRepo.deleteContact(contact.id)
    }

    fun restoreToPhonebook(
        context: Context,
        prefs: PreferenceManager,
        contactsRepo: IContactsRepository,
        contactId: String
    ): String? {
        val backupMap = getBackedUpContacts(prefs).toMutableMap()
        val backedUp = backupMap[contactId] ?: return null

        // Re-insert into ContactsContract
        contactsRepo.saveContact(contact = backedUp.copy(id = ""))

        // Clean up local photo file if it exists
        try {
            val photoFile = File(context.filesDir, "hidden_contact_${contactId}.jpg")
            if (photoFile.exists()) photoFile.delete()
        } catch (_: Exception) {}

        backupMap.remove(contactId)
        saveBackedUpContacts(prefs, backupMap)

        // Attempt to find new contact ID
        return findNewContactId(context, backedUp.phoneNumbers.firstOrNull(), backedUp.name)
    }

    private fun findNewContactId(context: Context, phoneNumber: String?, name: String?): String? {
        if (!phoneNumber.isNullOrBlank()) {
            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
                )
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup._ID),
                    null, null, null
                )?.use { c ->
                    if (c.moveToFirst()) return c.getString(0)
                }
            } catch (_: Exception) {}
        }
        if (!name.isNullOrBlank()) {
            try {
                context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(ContactsContract.Contacts._ID),
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?",
                    arrayOf(name),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) return c.getString(0)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Synchronizes phonebook state based on current hidden IDs and hideEverywhere flag.
     */
    fun syncPhonebookState(
        context: Context,
        prefs: PreferenceManager,
        contactsRepo: IContactsRepository,
        allContacts: List<Contact>,
        hiddenIds: Set<String>,
        hideEverywhere: Boolean
    ): Set<String> {
        val updatedHiddenIds = hiddenIds.toMutableSet()
        val backupMap = getBackedUpContacts(prefs).toMutableMap()

        if (hideEverywhere) {
            // 1. Hide contacts in hiddenIds that are still in system contacts
            val contactsToHide = allContacts.filter { it.id in hiddenIds && !backupMap.containsKey(it.id) }
            for (contact in contactsToHide) {
                val localPhoto = persistContactPhotoLocally(context, contact.id, contact.photoUri)
                backupMap[contact.id] = contact.copy(photoUri = localPhoto)
                contactsRepo.deleteContact(contact.id)
            }
            saveBackedUpContacts(prefs, backupMap)

            // 2. Restore contacts that were previously backed up but removed from hiddenIds
            val removedIds = backupMap.keys.filter { it !in hiddenIds }
            for (id in removedIds) {
                restoreToPhonebook(context, prefs, contactsRepo, id)
            }
        } else {
            // Hide everywhere turned OFF: restore ALL backed-up contacts to ContactsContract
            for ((id, backedUp) in backupMap.toList()) {
                contactsRepo.saveContact(contact = backedUp.copy(id = ""))
                try {
                    val photoFile = File(context.filesDir, "hidden_contact_${id}.jpg")
                    if (photoFile.exists()) photoFile.delete()
                } catch (_: Exception) {}
                val newId = findNewContactId(context, backedUp.phoneNumbers.firstOrNull(), backedUp.name)
                if (newId != null && updatedHiddenIds.contains(id)) {
                    updatedHiddenIds.remove(id)
                    updatedHiddenIds.add(newId)
                }
            }
            saveBackedUpContacts(prefs, emptyMap())
            prefs.setString(PreferenceManager.KEY_CONTACTS_HIDER_IDS, updatedHiddenIds.joinToString(","))
        }
        return updatedHiddenIds
    }
}
