package com.coolappstore.everdialer.by.svhp.modal.repository
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactEvent
import com.coolappstore.everdialer.by.svhp.modal.data.ContactSaveTarget
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch

class ContactsRepository(private val contentResolver: ContentResolver, private val context: Context) : IContactsRepository {

    /** Distinct, human-readable source labels (Google account(s), SIM, phone storage, etc.)
     *  for every contact_id, built from the raw contacts table in one bulk query. */
    private fun buildSourceAccountsByContactId(): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        val seenAccountKeyByContactId = mutableMapOf<String, MutableSet<String>>()
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME
            ),
            "${ContactsContract.RawContacts.DELETED} = 0",
            null, null
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val typeIdx      = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val nameIdx      = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                val contactId = if (contactIdIdx >= 0) cursor.getString(contactIdIdx) ?: continue else continue
                val type = cursor.getString(typeIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: ""
                val key = buildAccountKey(type, name)
                val seen = seenAccountKeyByContactId.getOrPut(contactId) { mutableSetOf() }
                if (seen.add(key)) {
                    result.getOrPut(contactId) { mutableListOf() }.add(buildAccountDisplayName(type, name))
                }
            }
        }
        return result
    }

    /** Source-account labels for a single contact_id (see [buildSourceAccountsByContactId]). */
    private fun getSourceAccountsForContact(contactId: String): List<String> {
        val labels = mutableListOf<String>()
        val seenKeys = mutableSetOf<String>()
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContract.RawContacts.ACCOUNT_NAME),
            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                val type = cursor.getString(typeIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: ""
                val key = buildAccountKey(type, name)
                if (seenKeys.add(key)) labels.add(buildAccountDisplayName(type, name))
            }
        }
        return labels
    }

    override fun getContacts(): List<Contact> = getContacts(emptySet())

    override fun getContacts(enabledAccountKeys: Set<String>): List<Contact> = try {
        getContactsInternal(enabledAccountKeys)
    } catch (_: SecurityException) {
        // READ_CONTACTS not granted yet (e.g. right after a fresh install, before the user
        // answers the permission prompt) — fail safe instead of crashing.
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun extractPhoneNumberFromSyncData(data1: String?, data3: String?): String? {
        if (!data1.isNullOrBlank() && data1.contains("@")) {
            val candidate = data1.substringBefore("@").filter { it.isDigit() || it == '+' }
            if (candidate.length >= 7) return candidate
        }
        if (!data3.isNullOrBlank()) {
            val digits = data3.filter { it.isDigit() || it == '+' }
            if (digits.length >= 7) return digits
        }
        return null
    }

    private fun getContactsInternal(enabledAccountKeys: Set<String>): List<Contact> {
        // Build list of contact IDs allowed by the enabled account filter
        val allowedContactIds: Set<String>? = if (enabledAccountKeys.isNotEmpty()) {
            buildAllowedContactIds(enabledAccountKeys)
        } else null // null = no filter, show all

        // Build contact_id -> distinct source-account labels (Google account(s), SIM, phone
        // storage, etc.) once up front, so ContactDetails can show the user where a contact is
        // actually stored/synced without a separate per-contact query.
        val sourceAccountsByContactId = buildSourceAccountsByContactId()

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

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue

                // Apply account filter: skip contacts not from allowed contact IDs
                if (allowedContactIds != null && id !in allowedContactIds) continue

                val mimeType = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx) ?: continue
                val isStarred = cursor.getInt(starredIdx) == 1

                val contact = contactsMap.getOrPut(id) {
                    Contact(
                        id = id,
                        name = cursor.getString(nameIdx) ?: "Unknown",
                        photoUri = cursor.getString(photoIdx),
                        isFavorite = isStarred,
                        sourceAccounts = sourceAccountsByContactId[id]?.toList() ?: emptyList()
                    )
                }

                when {
                    mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        contactsMap[id] = contact.copy(phoneNumbers = (contact.phoneNumbers + data1).distinct())
                    }
                    mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        contactsMap[id] = contact.copy(emails = (contact.emails + data1).distinct())
                    }
                    mimeType == ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        contactsMap[id] = contact.copy(addresses = (contact.addresses + data1).distinct())
                    }
                    mimeType == ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                        val type = cursor.getInt(data2Idx)
                        val label = cursor.getString(data3Idx)
                        val event = ContactEvent(type, label, data1)
                        contactsMap[id] = contact.copy(events = (contact.events + event).distinct())
                    }
                    mimeType?.contains("whatsapp", ignoreCase = true) == true ||
                    mimeType?.contains("tachyon", ignoreCase = true) == true -> {
                        val extracted = extractPhoneNumberFromSyncData(data1, cursor.getString(data3Idx))
                        if (!extracted.isNullOrBlank()) {
                            contactsMap[id] = contact.copy(phoneNumbers = (contact.phoneNumbers + extracted).distinct())
                        }
                    }
                }
            }
        }
        return contactsMap.values.toList()
            .filter { it.phoneNumbers.isNotEmpty() }
            .sortedBy { it.name }
    }

    private fun matchesAccountFilter(key: String, accountType: String, accountName: String, enabledKeys: Set<String>): Boolean {
        if (enabledKeys.isEmpty()) return false
        if (key in enabledKeys) return true
        return enabledKeys.any { targetKey ->
            when {
                targetKey == key -> true
                targetKey == "sim_0" && (key == "sim_0" || accountType.isBlank() || accountType.equals("com.android.local", true) || accountType.equals("com.android.contacts", true)) -> true
                targetKey.startsWith("sim_") && key == targetKey -> true
                targetKey.startsWith("google_") -> {
                    val email = targetKey.removePrefix("google_")
                    accountType.contains("google", ignoreCase = true) && accountName.equals(email, ignoreCase = true)
                }
                targetKey.startsWith("whatsapp") -> {
                    accountType.contains("whatsapp", ignoreCase = true) || accountName.contains("whatsapp", ignoreCase = true)
                }
                targetKey.startsWith("acc:") -> {
                    val parts = targetKey.removePrefix("acc:").split(":", limit = 2)
                    if (parts.size == 2) {
                        accountType.equals(parts[0], ignoreCase = true) && accountName.equals(parts[1], ignoreCase = true)
                    } else false
                }
                else -> false
            }
        }
    }

    /**
     * Returns aggregate contact IDs for accounts matching the enabled account keys.
     */
    private fun buildAllowedContactIds(enabledKeys: Set<String>): Set<String> {
        val allowed = mutableSetOf<String>()

        val rcProjection = arrayOf(
            ContactsContract.RawContacts.CONTACT_ID,
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
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                val contactId = if (contactIdIdx >= 0) cursor.getString(contactIdIdx) ?: continue else continue
                val accountType = cursor.getString(typeIdx) ?: ""
                val accountName = cursor.getString(nameIdx) ?: ""
                val key = buildAccountKey(accountType, accountName)

                if (matchesAccountFilter(key, accountType, accountName, enabledKeys)) {
                    allowed.add(contactId)
                }
            }
        }
        return allowed
    }

    override fun getContactById(contactId: String): Contact? = try {
        getContactByIdInternal(contactId)
    } catch (_: Exception) {
        null
    }

    private fun getContactByIdInternal(contactId: String): Contact? {
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
                    isFavorite = isStarred,
                    sourceAccounts = getSourceAccountsForContact(id)
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
        try {
            val contentValue = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
            }
            val updateUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendPath(contactId)
                .build()
            contentResolver.update(updateUri, contentValue, null, null)
        } catch (_: Exception) {}
    }

    private fun loadPhotoBytes(uriString: String?): ByteArray? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream) ?: return null
                val maxDim = 720
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val targetW = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt()
                    val targetH = if (ratio >= 1f) (maxDim / ratio).toInt() else maxDim
                    android.graphics.Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                } else bitmap
                val baos = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                baos.toByteArray()
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error reading photo bytes", e)
            null
        }
    }

    override fun saveContact(contact: Contact, accountType: String?, accountName: String?) {
        val ops = ArrayList<ContentProviderOperation>()
        
        if (contact.id.isEmpty() || contact.id == "0") {
            val rawContactIndex = ops.size
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build())

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build())

            contact.phoneNumbers.filter { it.isNotBlank() }.forEach { number ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build())
            }

            contact.emails.filter { it.isNotBlank() }.forEach { email ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                    .build())
            }

            contact.addresses.filter { it.isNotBlank() }.forEach { address ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                    .build())
            }

            val photoBytes = loadPhotoBytes(contact.photoUri)
            if (photoBytes != null) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                    .build())
            }
        } else {
            val rawContactIds = getRawContactIdsForContact(contact.id)
            val targetRawContactId = rawContactIds.firstOrNull() ?: contact.id.toLongOrNull()

            if (targetRawContactId != null) {
                val rawIdStrings = if (rawContactIds.isNotEmpty()) rawContactIds.map { it.toString() } else listOf(targetRawContactId.toString())

                // Delete existing Name, Phone, Email, Address rows for all associated raw contacts
                // using RAW_CONTACT_ID instead of CONTACT_ID (which is invalid in Data table queries)
                rawIdStrings.forEach { rawId ->
                    ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(rawId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        )
                        .build())
                    ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(rawId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        )
                        .build())
                    ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(rawId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        )
                        .build())
                    ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(rawId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        )
                        .build())
                }

                // Insert updated StructuredName
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, targetRawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .build())

                // Insert updated Phone numbers
                contact.phoneNumbers.filter { it.isNotBlank() }.forEach { number ->
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, targetRawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build())
                }

                // Insert updated Emails
                contact.emails.filter { it.isNotBlank() }.forEach { email ->
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, targetRawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build())
                }

                // Insert updated Addresses
                contact.addresses.filter { it.isNotBlank() }.forEach { address ->
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, targetRawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                        .build())
                }

                // Handle Photo update/deletion
                if (contact.photoUri == null) {
                    rawIdStrings.forEach { rawId ->
                        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                arrayOf(rawId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            )
                            .build())
                    }
                } else if (!contact.photoUri.startsWith("content://com.android.contacts")) {
                    val photoBytes = loadPhotoBytes(contact.photoUri)
                    if (photoBytes != null) {
                        rawIdStrings.forEach { rawId ->
                            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                .withSelection(
                                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                    arrayOf(rawId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                )
                                .build())
                        }
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, targetRawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build())
                    }
                }
            }
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error saving contact", e)
        }
    }

    override fun deleteContact(contactId: String) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            contentResolver.delete(uri, null, null)
        } catch (_: Exception) {}
    }

    override fun getAvailableAccounts(excludedContactIds: Set<String>): List<ContactAccount> = try {
        getAvailableAccountsInternal(excludedContactIds)
    } catch (_: Exception) {
        emptyList()
    }

    private fun getAvailableAccountsInternal(excludedContactIds: Set<String>): List<ContactAccount> {
        data class AccInfo(val type: String, val name: String, val contactIds: MutableSet<Long> = mutableSetOf())
        val accountMap = mutableMapOf<String, AccInfo>()

        // Only contacts with at least one phone number are ever shown in the contacts list
        // (see the `.filter { it.phoneNumbers.isNotEmpty() }` in getContacts()). Counting here
        // must use the exact same criteria, otherwise the "N contacts" badge shown on each
        // account button won't match how many contacts actually appear once that account is
        // selected.
        val contactIdsWithPhoneNumber = mutableSetOf<Long>()
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            if (idIdx >= 0) {
                while (cursor.moveToNext()) {
                    contactIdsWithPhoneNumber.add(cursor.getLong(idIdx))
                }
            }
        }

        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME
            ),
            "${ContactsContract.RawContacts.DELETED} = 0",
            null, null
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val typeIdx      = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val nameIdx      = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                val contactId = if (contactIdIdx >= 0) cursor.getLong(contactIdIdx) else continue
                if (contactId !in contactIdsWithPhoneNumber) continue
                if (contactId.toString() in excludedContactIds) continue
                val type = cursor.getString(typeIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: ""
                val key = buildAccountKey(type, name)
                accountMap.getOrPut(key) { AccInfo(type, name) }.contactIds.add(contactId)
            }
        }

        val fromRawContacts = accountMap.map { (key, info) ->
            ContactAccount(
                key          = key,
                displayName  = buildAccountDisplayName(info.type, info.name),
                accountType  = info.type,
                accountName  = info.name,
                contactCount = info.contactIds.size
            )
        }.filter { it.contactCount > 0 }

        // Always surface a button for every active SIM slot — even one that currently holds
        // zero contacts — so the user can still tap into "SIM 1"/"SIM 2" (e.g. to save a new
        // contact there). Without this, a SIM with 0 contacts would simply be missing from the
        // list below since entries with contactCount == 0 are otherwise filtered out above.
        val merged = LinkedHashMap<String, ContactAccount>()
        fromRawContacts.forEach { merged[it.key] = it }
        try {
            val simCount = getActiveSimCount()
            for (slot in 0 until simCount) {
                val key = "sim_${slot + 1}"
                merged.putIfAbsent(
                    key,
                    ContactAccount(
                        key = key,
                        displayName = if (simCount > 1) "SIM ${slot + 1}" else "SIM Card",
                        accountType = "sim",
                        accountName = "",
                        contactCount = 0
                    )
                )
            }
        } catch (_: Exception) { }

        return merged.values.sortedByDescending { it.contactCount }
    }

    override fun getSaveTargets(): List<ContactSaveTarget> {
        val targets = mutableListOf<ContactSaveTarget>()
        targets.add(ContactSaveTarget(label = "Device", subLabel = "This phone only"))

        try {
            getAvailableAccounts()
                .filter { !it.key.startsWith("sim_") && !it.key.equals("whatsapp", ignoreCase = true) }
                .forEach { acc ->
                    targets.add(
                        ContactSaveTarget(
                            label = acc.displayName,
                            subLabel = acc.accountName.ifBlank { null },
                            accountType = acc.accountType,
                            accountName = acc.accountName
                        )
                    )
                }
        } catch (_: Exception) { }

        try {
            val simCount = getActiveSimCount()
            for (slot in 0 until simCount) {
                targets.add(
                    ContactSaveTarget(
                        label = if (simCount > 1) "SIM ${slot + 1}" else "SIM Card",
                        subLabel = "Name & number only",
                        isSim = true,
                        simSlotIndex = slot
                    )
                )
            }
        } catch (_: Exception) { }

        return targets
    }

    override fun saveContactToSim(contact: Contact, simSlotIndex: Int): Boolean {
        val number = contact.phoneNumbers.firstOrNull { it.isNotBlank() } ?: return false
        return try {
            val values = ContentValues().apply {
                put("tag", contact.name)
                put("number", number)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    put("subscription_id", getSubscriptionIdForSlot(simSlotIndex))
                }
            }
            val uri = contentResolver.insert(Uri.parse("content://icc/adn"), values)
            uri != null
        } catch (_: Exception) {
            false
        }
    }

    override fun moveContact(contact: Contact, target: ContactSaveTarget): Boolean {
        return try {
            if (target.isSim) {
                val saved = saveContactToSim(contact, target.simSlotIndex)
                if (saved) deleteRawContactsForContact(contact.id)
                saved
            } else {
                // Create the copy at the destination first, then remove only the raw contact(s)
                // that lived under the original account(s) — not the aggregate — so we don't risk
                // deleting the copy we just made if Android merges the two by matching name/number.
                val originalRawIds = getRawContactIdsForContact(contact.id)
                saveContact(contact.copy(id = ""), target.accountType, target.accountName)
                originalRawIds.forEach { deleteRawContact(it) }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getRawContactIdsForContact(contactId: String): List<Long> {
        val ids = mutableListOf<Long>()
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
            while (cursor.moveToNext()) ids.add(cursor.getLong(idIdx))
        }
        return ids
    }

    private fun deleteRawContactsForContact(contactId: String) {
        getRawContactIdsForContact(contactId).forEach { deleteRawContact(it) }
    }

    private fun deleteRawContact(rawContactId: Long) {
        try {
            val uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                .appendPath(rawContactId.toString())
                .build()
            contentResolver.delete(uri, null, null)
        } catch (_: Exception) {}
    }

    /** Resolves the subscription_id for a given 0-based SIM slot index, or -1 if unknown. */
    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return -1
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return -1
            val subs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sm.activeSubscriptionInfoList ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                SubscriptionManager.from(context).activeSubscriptionInfoList ?: emptyList()
            }
            subs.firstOrNull { it.simSlotIndex == slotIndex }?.subscriptionId ?: -1
        } catch (_: Exception) { -1 }
    }

    private fun buildAccountKey(type: String, name: String): String {
        val simSlot = getSimSlotForAccount(type, name)
        if (simSlot >= 0) return "sim_${simSlot + 1}"
        val isLocal = type.isBlank() ||
            type.equals("com.android.local", ignoreCase = true) ||
            type.equals("com.android.contacts", ignoreCase = true) ||
            type.equals("phone", ignoreCase = true) ||
            type.equals("device", ignoreCase = true)
        if (isLocal) return "sim_0"
        if (type.equals("com.google", ignoreCase = true)) return "google_$name"
        if (type.contains("whatsapp", ignoreCase = true)) {
            return if (name.isNotBlank() && !name.equals("WhatsApp", ignoreCase = true)) "whatsapp_$name" else "whatsapp"
        }
        return "acc:${type}:${name}"
    }

    private fun buildAccountDisplayName(type: String, name: String): String {
        val simSlot = getSimSlotForAccount(type, name)
        if (simSlot >= 0) {
            return when {
                simSlot == 0 -> "SIM 1"
                simSlot == 1 -> "SIM 2"
                else         -> "SIM ${simSlot + 1}"
            }
        }
        val isLocal = type.isBlank() ||
            type.equals("com.android.local", ignoreCase = true) ||
            type.equals("com.android.contacts", ignoreCase = true) ||
            type.equals("phone", ignoreCase = true) ||
            type.equals("device", ignoreCase = true)
        if (isLocal) return "Device Storage"

        if (type.equals("com.google", ignoreCase = true)) {
            return if (name.isNotBlank()) name else "Google"
        }
        if (type.contains("whatsapp", ignoreCase = true)) {
            return if (type.contains("w4b", ignoreCase = true) || name.contains("business", ignoreCase = true)) {
                "WhatsApp Business"
            } else {
                "WhatsApp"
            }
        }

        // Try getting installed app label from PackageManager for Exchange, Meet, Telegram, etc.
        val appLabel = runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(type, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrNull()

        val baseLabel = when {
            !appLabel.isNullOrBlank() -> appLabel
            type.contains("tachyon", ignoreCase = true) || type.contains("meet", ignoreCase = true) -> "Google Meet"
            type.contains("exchange", ignoreCase = true) -> "Exchange"
            type.contains("outlook", ignoreCase = true) -> "Outlook"
            type.contains("telegram", ignoreCase = true) -> "Telegram"
            type.contains("signal", ignoreCase = true) -> "Signal"
            else -> type.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        return if (name.isNotBlank() && !name.equals(baseLabel, ignoreCase = true) && !name.equals(type, ignoreCase = true)) {
            "$baseLabel ($name)"
        } else {
            baseLabel
        }
    }

    /** Returns 0-based SIM slot index if this raw contact's account is actually a SIM account
     *  and its name matches a currently active subscription, else -1 (treated as Device Storage). */
    private fun getSimSlotForAccount(accountType: String, accountName: String): Int {
        // Only raw contacts whose account type explicitly denotes SIM/ICC storage should ever be
        // considered for a SIM slot. Plain device/local contacts (accountType blank/null on AOSP,
        // or OEM-specific non-SIM types) must never be guessed into a SIM bucket just because their
        // account name happens to contain a digit that matches a subscription id.
        val isSimAccountType = accountType.contains("sim", ignoreCase = true) ||
            accountType.contains("icc", ignoreCase = true)
        if (!isSimAccountType) return -1
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return -1
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return -1
            val subs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sm.activeSubscriptionInfoList ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                SubscriptionManager.from(context).activeSubscriptionInfoList ?: emptyList()
            }
            if (subs.isEmpty()) return -1
            // Match by display name or ICC ID that may be embedded in accountName
            val matched = subs.firstOrNull { sub ->
                (accountName.isNotBlank() && sub.displayName?.toString()?.equals(accountName, ignoreCase = true) == true) ||
                (accountName.isNotBlank() && sub.iccId?.let { accountName.equals(it, ignoreCase = true) } == true)
            }
            // If we couldn't disambiguate which specific SIM this account belongs to but we do
            // know it's a SIM account and there's exactly one active SIM, it must be that one.
            matched?.simSlotIndex ?: if (subs.size == 1) subs[0].simSlotIndex else -1
        } catch (_: Exception) { -1 }
    }

    /** Returns how many active SIM cards are present (max 2 for dual-SIM). */
    private fun getActiveSimCount(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return 1
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return 1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sm.activeSubscriptionInfoList?.size ?: 1
            } else {
                @Suppress("DEPRECATION")
                SubscriptionManager.from(context).activeSubscriptionInfoList?.size ?: 1
            }
        } catch (_: Exception) { 1 }
    }


    override fun getContactByNumber(number: String): Contact? = try {
        getContactByNumberInternal(number)
    } catch (_: Exception) {
        null
    }

    private fun getContactByNumberInternal(number: String): Contact? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.STARRED,
            ContactsContract.PhoneLookup.NUMBER
        )

        // PhoneLookup's own fuzzy matching isn't a genuine match — on some OEMs/Android versions
        // a short, unsaved number (e.g. a 3-digit short code like "787" or "875") can incorrectly
        // match a much longer saved contact number that merely *contains* or *starts with* those
        // same digits (e.g. a saved "7875XXXXXX"), pointing call logs at the wrong contact.
        // Guard against that here with numbersLikelyMatch: exact digit match always passes, and a
        // suffix match (needed so a contact saved with a country code, e.g. "+917875551234",
        // still matches the plain call-log number "7875551234") is only trusted when both numbers
        // are long enough to be real phone numbers — never for short codes.
        val queriedDigits = number.filter { it.isDigit() }

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            // A contact with 2+ saved numbers can produce multiple rows here (one per raw
            // number PhoneLookup fuzzy-matched against). Checking only the first row meant that
            // if that particular row's number wasn't an exact/suffix match, the lookup bailed
            // out entirely — even when a later row for the same contact was a perfect match.
            // Walk every row and accept the first genuine match.
            while (cursor.moveToNext()) {
                val matchedRaw = cursor.getString(4) ?: ""
                val isMatch = numbersLikelyMatch(queriedDigits, matchedRaw)

                if (!isMatch) continue

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

        // Fallback: PhoneLookup's own built-in fuzzy matching relies on Android's internal
        // PHONE_NUMBERS_EQUAL comparison, which on many OEMs/Android versions simply fails to
        // bridge a number saved WITH a country code (e.g. "+917875551234") against the same
        // number dialed/received WITHOUT one ("7875551234"), or vice versa — especially when
        // the device's detected region doesn't match the number's country. In that case
        // PhoneLookup returns *zero* rows at all, so no amount of row-walking above helps; the
        // contact is wrongly treated as unknown everywhere this lookup is used (contact info
        // page, call logs, call recording logs, incoming/ongoing call UI). Recover by manually
        // scanning every saved phone number directly and applying the same trusted
        // exact-or-suffix comparison ourselves.
        return getContactByNumberFallbackScan(queriedDigits)
    }

    /** Manual scan over every saved phone number, used when PhoneLookup itself fails to
     *  surface a genuine match (see [getContactByNumber]). */
    private fun getContactByNumberFallbackScan(queriedDigits: String): Contact? {
        if (queriedDigits.isEmpty()) return null
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val savedNumber = cursor.getString(numberIdx) ?: continue
                if (!numbersLikelyMatch(queriedDigits, savedNumber)) continue
                return Contact(
                    id = cursor.getString(idIdx) ?: continue,
                    name = cursor.getString(nameIdx),
                    photoUri = cursor.getString(photoIdx),
                    isFavorite = cursor.getInt(starredIdx) == 1,
                    phoneNumbers = listOf(savedNumber)
                )
            }
        }
        return null
    }

    override fun getSystemContactGroups(): List<com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup> {
        val groups = mutableListOf<com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup>()
        val groupIdToGroupMap = mutableMapOf<Long, com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup>()

        val groupProjection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.ACCOUNT_TYPE,
            ContactsContract.Groups.ACCOUNT_NAME
        )
        try {
            contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                groupProjection,
                "${ContactsContract.Groups.DELETED} = 0",
                null,
                "${ContactsContract.Groups.TITLE} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
                val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
                val typeIdx = cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME)

                while (cursor.moveToNext()) {
                    val rowId = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else ""
                    val accType = if (typeIdx >= 0) cursor.getString(typeIdx) else null
                    val accName = if (nameIdx >= 0) cursor.getString(nameIdx) else null

                    if (title.isNotBlank()) {
                        val g = com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup(
                            id = "sys_group_$rowId",
                            name = title,
                            contactIds = emptyList(),
                            accountType = accType,
                            accountName = accName,
                            targetLabel = if (!accName.isNullOrBlank()) "$accName (${buildAccountDisplayName(accType ?: "", accName)})" else null
                        )
                        groupIdToGroupMap[rowId] = g
                    }
                }
            }

            if (groupIdToGroupMap.isNotEmpty()) {
                val memberMap = mutableMapOf<Long, MutableSet<String>>()
                val memberProjection = arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
                )
                val memberSelection = "${ContactsContract.Data.MIMETYPE} = ?"
                val memberArgs = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)

                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    memberProjection,
                    memberSelection,
                    memberArgs,
                    null
                )?.use { cursor ->
                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                    val groupRowIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)

                    while (cursor.moveToNext()) {
                        val contactId = if (contactIdIdx >= 0) cursor.getString(contactIdIdx) else null
                        val gRowId = if (groupRowIdIdx >= 0) cursor.getLong(groupRowIdIdx) else 0L
                        if (contactId != null && gRowId != 0L) {
                            memberMap.getOrPut(gRowId) { mutableSetOf() }.add(contactId)
                        }
                    }
                }

                groupIdToGroupMap.forEach { (rowId, g) ->
                    val contactIds = memberMap[rowId]?.toList() ?: emptyList()
                    groups.add(g.copy(contactIds = contactIds))
                }
            }
        } catch (_: Exception) {}

        return groups
    }

    override fun saveSystemContactGroup(group: com.coolappstore.everdialer.by.svhp.modal.data.ContactGroup): String? {
        val ops = ArrayList<ContentProviderOperation>()
        var targetGroupId: Long? = null

        // 1. Determine or create the group in ContactsContract.Groups
        if (group.id.startsWith("sys_group_")) {
            targetGroupId = group.id.removePrefix("sys_group_").toLongOrNull()
        }

        try {
            if (targetGroupId == null) {
                val groupValues = ContentValues().apply {
                    put(ContactsContract.Groups.TITLE, group.name)
                    put(ContactsContract.Groups.GROUP_VISIBLE, 1)
                    if (!group.accountType.isNullOrBlank()) {
                        put(ContactsContract.Groups.ACCOUNT_TYPE, group.accountType)
                    }
                    if (!group.accountName.isNullOrBlank()) {
                        put(ContactsContract.Groups.ACCOUNT_NAME, group.accountName)
                    }
                }
                val newGroupUri = contentResolver.insert(ContactsContract.Groups.CONTENT_URI, groupValues)
                targetGroupId = newGroupUri?.lastPathSegment?.toLongOrNull()
            } else {
                // Update group title if existing
                val updateValues = ContentValues().apply {
                    put(ContactsContract.Groups.TITLE, group.name)
                }
                contentResolver.update(
                    ContactsContract.Groups.CONTENT_URI,
                    updateValues,
                    "${ContactsContract.Groups._ID} = ?",
                    arrayOf(targetGroupId.toString())
                )
            }

            if (targetGroupId != null) {
                // 2. Remove previous group memberships
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?",
                            arrayOf(
                                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                                targetGroupId.toString()
                            )
                        )
                        .build()
                )

                // 3. For each contact, find raw contact id matching account or first raw contact
                for (contactId in group.contactIds) {
                    val rawContactIds = getRawContactIdsForContact(contactId)
                    val targetRawId = rawContactIds.firstOrNull() ?: contactId.toLongOrNull()
                    if (targetRawId != null) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, targetRawId)
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                                    targetGroupId
                                )
                                .build()
                        )
                    }
                }

                if (ops.isNotEmpty()) {
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                }

                return "sys_group_$targetGroupId"
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error saving system contact group", e)
        }
        return null
    }

    override fun deleteSystemContactGroup(groupId: String): Boolean {
        val targetId = if (groupId.startsWith("sys_group_")) {
            groupId.removePrefix("sys_group_").toLongOrNull()
        } else {
            groupId.toLongOrNull()
        } ?: return false

        return try {
            // 1. Delete group memberships first
            contentResolver.delete(
                ContactsContract.Data.CONTENT_URI,
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?",
                arrayOf(
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    targetId.toString()
                )
            )
            // 2. Delete group from ContactsContract.Groups
            val deletedRows = contentResolver.delete(
                ContactsContract.Groups.CONTENT_URI,
                "${ContactsContract.Groups._ID} = ?",
                arrayOf(targetId.toString())
            )
            deletedRows > 0
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error deleting system contact group", e)
            false
        }
    }
}
