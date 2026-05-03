package com.coolappstore.everdialer.by.svhp.modal.repository
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactEvent
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository

class ContactsRepository(private val contentResolver: ContentResolver) : IContactsRepository {

    override fun getContacts(): List<Contact> = getContacts(emptySet())

    override fun getContacts(enabledAccountKeys: Set<String>): List<Contact> {
        // Build list of raw contact IDs allowed by the enabled account filter
        val allowedRawContactIds: Set<Long>? = if (enabledAccountKeys.isNotEmpty()) {
            buildAllowedRawContactIds(enabledAccountKeys)
        } else null // null = no filter, show all

        val contactsMap = mutableMapOf<String, Contact>()

        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_URI,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.STARRED,
            ContactsContract.Data.RAW_CONTACT_ID
        )

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Data.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val data3Idx = cursor.getColumnIndex(ContactsContract.Data.DATA3)
            val starredIdx = cursor.getColumnIndex(ContactsContract.Data.STARRED)
            val rawIdIdx = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val mimeType = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx) ?: continue

                // Apply account filter: skip contacts not from allowed raw contact IDs
                if (allowedRawContactIds != null) {
                    val rawId = cursor.getLong(rawIdIdx)
                    if (rawId !in allowedRawContactIds) continue
                }

                val isStarred = cursor.getInt(starredIdx) == 1

                val contact = contactsMap.getOrPut(id) {
                    Contact(
                        id = id,
                        name = cursor.getString(nameIdx) ?: "Unknown",
                        photoUri = cursor.getString(photoIdx),
                        isFavorite = isStarred
                    )
                }

                when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        contactsMap[id] = contact.copy(phoneNumbers = (contact.phoneNumbers + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        contactsMap[id] = contact.copy(emails = (contact.emails + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        contactsMap[id] = contact.copy(addresses = (contact.addresses + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val type = cursor.getInt(data2Idx)
                        val label = cursor.getString(data3Idx)
                        val event = ContactEvent(type, label, data1)
                        contactsMap[id] = contact.copy(events = (contact.events + event).distinct())
                    }
                }
            }
        }
        return contactsMap.values.toList()
            .filter { it.phoneNumbers.isNotEmpty() }
            .sortedBy { it.name }
    }

    /**
     * Returns raw contact IDs for accounts matching the enabled account keys.
     * Key format matches what ContactsToDisplayDialog produces:
     *   "google_<email>"  → account type "com.google", name == email
     *   "sim_<subId>"     → account type "com.android.local" or null (device/SIM contacts)
     *   "whatsapp"        → account type contains "whatsapp"
     */
    private fun buildAllowedRawContactIds(enabledKeys: Set<String>): Set<Long> {
        val allowed = mutableSetOf<Long>()

        val rcProjection = arrayOf(
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.RawContacts.ACCOUNT_NAME
        )
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            rcProjection,
            "${ContactsContract.RawContacts.DELETED} = 0",
            null,
            null
        )?.use { cursor ->
            val idIdx   = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
            val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                val rawId = cursor.getLong(idIdx)
                val accountType = cursor.getString(typeIdx) ?: ""
                val accountName = cursor.getString(nameIdx) ?: ""

                val matchesAny = enabledKeys.any { key ->
                    when {
                        key.startsWith("google_") -> {
                            val email = key.removePrefix("google_")
                            accountType.equals("com.google", ignoreCase = true) &&
                                accountName.equals(email, ignoreCase = true)
                        }
                        key.startsWith("sim_") -> {
                            // SIM/device contacts typically have null/empty account type
                            // or account type like "com.android.local"
                            accountType.isBlank() ||
                                accountType.equals("com.android.local", ignoreCase = true) ||
                                accountType.equals("com.android.contacts", ignoreCase = true)
                        }
                        key == "whatsapp" -> {
                            accountType.contains("whatsapp", ignoreCase = true) ||
                                accountName.contains("whatsapp", ignoreCase = true)
                        }
                        else -> false
                    }
                }
                if (matchesAny) allowed.add(rawId)
            }
        }
        return allowed
    }

    override fun getContactById(contactId: String): Contact? {
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_URI,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.STARRED
        )

        var contact: Contact? = null

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            "${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Idx = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val data3Idx = cursor.getColumnIndex(ContactsContract.Data.DATA3)
            val starredIdx = cursor.getColumnIndex(ContactsContract.Data.STARRED)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val mimeType = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx) ?: continue
                val isStarred = cursor.getInt(starredIdx) == 1

                val currentContact = contact ?: Contact(
                    id = id,
                    name = cursor.getString(nameIdx) ?: "Unknown",
                    photoUri = cursor.getString(photoIdx),
                    isFavorite = isStarred
                )

                contact = when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        currentContact.copy(phoneNumbers = (currentContact.phoneNumbers + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        currentContact.copy(emails = (currentContact.emails + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        currentContact.copy(addresses = (currentContact.addresses + data1).distinct())
                    }
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val type = cursor.getInt(data2Idx)
                        val label = cursor.getString(data3Idx)
                        val event = ContactEvent(type, label, data1)
                        currentContact.copy(events = (currentContact.events + event).distinct())
                    }
                    else -> currentContact
                }
            }
        }
        return contact
    }

    override fun toggleFavorite(contactId: String, isFavorite: Boolean) {
        val contentValue = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }
        val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendPath(contactId)
            .build()
        contentResolver.update(updateUri, contentValue, null, null)
    }

    override fun saveContact(contact: Contact) {
        val ops = ArrayList<ContentProviderOperation>()
        
        if (contact.id.isEmpty() || contact.id == "0") {
            
            val rawContactIndex = ops.size
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build())

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build())

            contact.phoneNumbers.forEach { number ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build())
            }
        } else {
            
            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(contact.id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build())
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun deleteContact(contactId: String) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
        contentResolver.delete(uri, null, null)
    }

    override fun getContactByNumber(number: String): Contact? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.STARRED
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val photoUri = cursor.getString(2)
                val starred = cursor.getInt(3) == 1
                return Contact(
                    id = id,
                    name = name,
                    photoUri = photoUri,
                    isFavorite = starred,
                    phoneNumbers = listOf(number)
                )
            }
        }
        return null
    }
}
