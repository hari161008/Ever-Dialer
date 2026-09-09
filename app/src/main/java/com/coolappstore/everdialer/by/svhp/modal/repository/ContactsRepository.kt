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
import com.coolappstore.everdialer.by.svhp.modal.data.ContactPhone
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccount
import com.coolappstore.everdialer.by.svhp.modal.data.ContactAccountInfo
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
                        val type = cursor.getInt(data2Idx)
                        val label = cursor.getString(data3Idx)
                        val phone = ContactPhone(data1, type, label)
                        contactsMap[id] = contact.copy(
                            phoneNumbers = (contact.phoneNumbers + data1).distinct(),
                            phones = (contact.phones + phone).distinctBy { it.number }
                        )
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
                    mimeType == ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            val existingNote = contact.note
                            val updatedNote = if (existingNote.isNullOrBlank()) data1 else "$existingNote\n$data1"
                            contactsMap[id] = contact.copy(note = updatedNote)
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
                        val type = cursor.getInt(data2Idx)
                        val label = cursor.getString(data3Idx)
                        val phone = ContactPhone(data1, type, label)
                        currentContact.copy(
                            phoneNumbers = (currentContact.phoneNumbers + data1).distinct(),
                            phones = (currentContact.phones + phone).distinctBy { it.number }
                        )
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
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                        if (data1.isNotBlank()) {
                            val existingNote = currentContact.note
                            val updatedNote = if (existingNote.isNullOrBlank()) data1 else "$existingNote\n$data1"
                            currentContact.copy(note = updatedNote)
                        } else currentContact
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

    private data class RawContactRecord(
        val id: Long,
        val accountType: String?,
        val accountName: String?,
        val isReadOnly: Boolean = false
    )

    private fun isKnownSyncAdapter(accountType: String?): Boolean {
        if (accountType.isNullOrBlank()) return false
        val lower = accountType.lowercase()
        return lower.contains("whatsapp") ||
                lower.contains("telegram") ||
                lower.contains("securesms") ||
                lower.contains("signal") ||
                lower.contains("facebook") ||
                lower.contains("katana") ||
                lower.contains("orca") ||
                lower.contains("truecaller") ||
                lower.contains("skype") ||
                lower.contains("viber") ||
                lower.contains("tachyon") ||
                lower.contains("duo") ||
                lower.contains("meet") ||
                lower.contains("imo") ||
                lower.contains("line") ||
                lower.contains("wechat") ||
                lower.contains("twitter") ||
                lower.contains("instagram") ||
                lower.contains("linkedin") ||
                lower.contains("zoom") ||
                lower.contains("teams")
    }

    private fun getRawContactsForContact(contactId: String): List<RawContactRecord> {
        val list = mutableListOf<RawContactRecord>()
        val seenIds = mutableSetOf<Long>()
        val projection = arrayOf(
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.RawContacts.ACCOUNT_NAME
        )
        try {
            // 1. Query RawContacts by aggregate CONTACT_ID
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                projection,
                "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    if (seenIds.add(id)) {
                        val type = if (typeIdx >= 0) cursor.getString(typeIdx) else null
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        val isRo = isKnownSyncAdapter(type)
                        list.add(RawContactRecord(id, type, name, isRo))
                    }
                }
            }

            // 2. Also search Data table for any raw contacts associated with this aggregate contact
            val dataRawIds = mutableListOf<Long>()
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                val rawIdIdx = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)
                while (cursor.moveToNext()) {
                    if (rawIdIdx >= 0) {
                        val rawId = cursor.getLong(rawIdIdx)
                        if (rawId > 0 && !seenIds.contains(rawId)) {
                            dataRawIds.add(rawId)
                        }
                    }
                }
            }

            if (dataRawIds.isNotEmpty()) {
                val placeholders = dataRawIds.joinToString(",") { "?" }
                contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    projection,
                    "${ContactsContract.RawContacts._ID} IN ($placeholders) AND ${ContactsContract.RawContacts.DELETED} = 0",
                    dataRawIds.map { it.toString() }.toTypedArray(),
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                    val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

                    while (cursor.moveToNext()) {
                        val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                        if (seenIds.add(id)) {
                            val type = if (typeIdx >= 0) cursor.getString(typeIdx) else null
                            val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                            val isRo = isKnownSyncAdapter(type)
                            list.add(RawContactRecord(id, type, name, isRo))
                        }
                    }
                }
            }

            // 3. Fallback: in case contactId is actually a raw contact id
            if (list.isEmpty()) {
                val rawId = contactId.toLongOrNull()
                if (rawId != null) {
                    contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        projection,
                        "${ContactsContract.RawContacts._ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                        arrayOf(rawId.toString()),
                        null
                    )?.use { cursor ->
                        val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                        val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                        val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

                        while (cursor.moveToNext()) {
                            val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                            if (seenIds.add(id)) {
                                val type = if (typeIdx >= 0) cursor.getString(typeIdx) else null
                                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                                val isRo = isKnownSyncAdapter(type)
                                list.add(RawContactRecord(id, type, name, isRo))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error getting raw contacts for contact $contactId", e)
        }
        return list
    }

    override fun getContactAccounts(contactId: String): List<ContactAccountInfo> {
        val rawRecords = getRawContactsForContact(contactId)
        return rawRecords.map { record ->
            val type = record.accountType ?: ""
            val name = record.accountName ?: ""
            val simSlot = getSimSlotForAccount(type, name)
            ContactAccountInfo(
                rawContactId = record.id,
                accountType = record.accountType,
                accountName = record.accountName,
                displayName = buildAccountDisplayName(type, name),
                isReadOnly = record.isReadOnly,
                isSim = simSlot >= 0,
                simSlotIndex = simSlot
            )
        }
    }

    private data class ExistingDataRow(val id: Long, val value1: String?, val type: Int? = null)

    private fun updateStructuredName(ops: ArrayList<ContentProviderOperation>, rawId: Long, displayName: String) {
        var existingDataId: Long? = null
        var existingDisplayName: String? = null
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME
                ),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
                    if (idIdx >= 0) existingDataId = cursor.getLong(idIdx)
                    if (nameIdx >= 0) existingDisplayName = cursor.getString(nameIdx)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying StructuredName for rawId $rawId", e)
        }

        if (existingDataId != null) {
            if (existingDisplayName != displayName) {
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingDataId.toString()))
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName)
                        .build()
                )
            }
        } else {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, displayName)
                    .build()
            )
        }
    }

    private fun updatePhoneNumbers(ops: ArrayList<ContentProviderOperation>, rawId: Long, phoneNumbers: List<String>) {
        updatePhoneNumbersWithPhones(ops, rawId, phoneNumbers.map { ContactPhone(it, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) })
    }

    private fun updatePhoneNumbersWithPhones(ops: ArrayList<ContentProviderOperation>, rawId: Long, phones: List<ContactPhone>) {
        val cleanPhones = phones.filter { it.number.isNotBlank() }.distinctBy { it.number.trim() }
        data class ExistingPhoneRow(val id: Long, val number: String?, val type: Int?, val label: String?)
        val existingRows = mutableListOf<ExistingPhoneRow>()
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
                ),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                while (cursor.moveToNext()) {
                    if (idIdx >= 0) {
                        existingRows.add(
                            ExistingPhoneRow(
                                id = cursor.getLong(idIdx),
                                number = if (numIdx >= 0) cursor.getString(numIdx) else null,
                                type = if (typeIdx >= 0) cursor.getInt(typeIdx) else null,
                                label = if (labelIdx >= 0) cursor.getString(labelIdx) else null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying phone numbers for rawId $rawId", e)
        }

        val minCount = minOf(existingRows.size, cleanPhones.size)
        for (i in 0 until minCount) {
            val existing = existingRows[i]
            val newPhone = cleanPhones[i]
            val numChanged = existing.number?.trim() != newPhone.number.trim()
            val typeChanged = existing.type != newPhone.type
            val labelChanged = existing.label != newPhone.label
            if (numChanged || typeChanged || labelChanged) {
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existing.id.toString()))
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone.number.trim())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, newPhone.type)
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, if (newPhone.type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) newPhone.label else null)
                        .build()
                )
            }
        }

        if (cleanPhones.size > existingRows.size) {
            for (i in existingRows.size until cleanPhones.size) {
                val newPhone = cleanPhones[i]
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone.number.trim())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, newPhone.type)
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, if (newPhone.type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) newPhone.label else null)
                        .build()
                )
            }
        }

        if (existingRows.size > cleanPhones.size) {
            for (i in cleanPhones.size until existingRows.size) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingRows[i].id.toString()))
                        .build()
                )
            }
        }
    }

    private fun updateEmails(ops: ArrayList<ContentProviderOperation>, rawId: Long, emails: List<String>) {
        val cleanEmails = emails.filter { it.isNotBlank() }.map { it.trim() }.distinct()
        val existingRows = mutableListOf<ExistingDataRow>()
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                    ContactsContract.CommonDataKinds.Email.TYPE
                ),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
                while (cursor.moveToNext()) {
                    if (idIdx >= 0) {
                        existingRows.add(
                            ExistingDataRow(
                                id = cursor.getLong(idIdx),
                                value1 = if (addrIdx >= 0) cursor.getString(addrIdx) else null,
                                type = if (typeIdx >= 0) cursor.getInt(typeIdx) else null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying emails for rawId $rawId", e)
        }

        val minCount = minOf(existingRows.size, cleanEmails.size)
        for (i in 0 until minCount) {
            val existing = existingRows[i]
            val newEmail = cleanEmails[i]
            if (!existing.value1.equals(newEmail, ignoreCase = true)) {
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existing.id.toString()))
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, newEmail)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build()
                )
            }
        }

        if (cleanEmails.size > existingRows.size) {
            for (i in existingRows.size until cleanEmails.size) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, cleanEmails[i])
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build()
                )
            }
        }

        if (existingRows.size > cleanEmails.size) {
            for (i in cleanEmails.size until existingRows.size) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingRows[i].id.toString()))
                        .build()
                )
            }
        }
    }

    private fun updateAddresses(ops: ArrayList<ContentProviderOperation>, rawId: Long, addresses: List<String>) {
        val cleanAddresses = addresses.filter { it.isNotBlank() }.map { it.trim() }.distinct()
        val existingRows = mutableListOf<ExistingDataRow>()
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                    ContactsContract.CommonDataKinds.StructuredPostal.TYPE
                ),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
                while (cursor.moveToNext()) {
                    if (idIdx >= 0) {
                        existingRows.add(
                            ExistingDataRow(
                                id = cursor.getLong(idIdx),
                                value1 = if (addrIdx >= 0) cursor.getString(addrIdx) else null,
                                type = if (typeIdx >= 0) cursor.getInt(typeIdx) else null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying addresses for rawId $rawId", e)
        }

        val minCount = minOf(existingRows.size, cleanAddresses.size)
        for (i in 0 until minCount) {
            val existing = existingRows[i]
            val newAddr = cleanAddresses[i]
            if (existing.value1?.trim() != newAddr) {
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existing.id.toString()))
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, newAddr)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                        .build()
                )
            }
        }

        if (cleanAddresses.size > existingRows.size) {
            for (i in existingRows.size until cleanAddresses.size) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, cleanAddresses[i])
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                        .build()
                )
            }
        }

        if (existingRows.size > cleanAddresses.size) {
            for (i in cleanAddresses.size until existingRows.size) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingRows[i].id.toString()))
                        .build()
                )
            }
        }
    }

    private fun updateNote(ops: ArrayList<ContentProviderOperation>, rawId: Long, note: String?) {
        var existingId: Long? = null
        var existingNote: String? = null
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Note.NOTE),
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                    val noteIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
                    if (idIdx >= 0) existingId = cursor.getLong(idIdx)
                    if (noteIdx >= 0) existingNote = cursor.getString(noteIdx)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying note for rawId $rawId", e)
        }

        val cleanNote = note?.trim()
        if (cleanNote.isNullOrBlank()) {
            if (existingId != null) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingId.toString()))
                        .build()
                )
            }
        } else {
            if (existingId != null) {
                if (existingNote?.trim() != cleanNote) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(existingId.toString()))
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, cleanNote)
                            .build()
                    )
                }
            } else {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, cleanNote)
                        .build()
                )
            }
        }
    }

    private fun ensureSyncAdapterAggregationAutomatic(allRawContacts: List<RawContactRecord>) {
        val readOnlyIds = allRawContacts.filter { it.isReadOnly }.map { it.id }
        val writableIds = allRawContacts.filter { !it.isReadOnly }.map { it.id }
        if (readOnlyIds.isEmpty() || writableIds.isEmpty()) return

        val ops = ArrayList<ContentProviderOperation>()
        for (roId in readOnlyIds) {
            for (wId in writableIds) {
                val id1 = minOf(roId, wId)
                val id2 = maxOf(roId, wId)
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                        .withValue(ContactsContract.AggregationExceptions.TYPE, ContactsContract.AggregationExceptions.TYPE_AUTOMATIC)
                        .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, id1)
                        .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, id2)
                        .build()
                )
            }
        }
        try {
            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
        } catch (e: Exception) {
            android.util.Log.w("ContactsRepo", "Error resetting sync adapter aggregation to automatic", e)
        }
    }

    private fun setAggregationKeepTogether(rawContactIds: List<Long>) {
        if (rawContactIds.size < 2) return
        val distinctIds = rawContactIds.distinct()
        val ops = ArrayList<ContentProviderOperation>()
        for (i in 0 until distinctIds.size) {
            for (j in i + 1 until distinctIds.size) {
                val id1 = minOf(distinctIds[i], distinctIds[j])
                val id2 = maxOf(distinctIds[i], distinctIds[j])
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                        .withValue(ContactsContract.AggregationExceptions.TYPE, ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
                        .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, id1)
                        .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, id2)
                        .build()
                )
            }
        }
        try {
            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error setting aggregation exceptions", e)
        }
    }

    private fun updateContactOnSim(
        oldName: String,
        oldNumber: String,
        newName: String,
        newNumber: String,
        simSlotIndex: Int
    ): Boolean {
        if (newName.isBlank() || newNumber.isBlank()) return false
        return try {
            val values = ContentValues().apply {
                put("tag", oldName.ifBlank { newName })
                put("number", oldNumber.ifBlank { newNumber })
                put("newTag", newName)
                put("newNumber", newNumber)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subId = getSubscriptionIdForSlot(simSlotIndex)
                    if (subId >= 0) put("subscription_id", subId)
                }
            }
            val uri = Uri.parse("content://icc/adn")
            val rows = contentResolver.update(uri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            android.util.Log.w("ContactsRepo", "Failed to update contact on SIM", e)
            false
        }
    }

    override fun saveContact(
        contact: Contact,
        accountType: String?,
        accountName: String?,
        updateAllAccounts: Boolean,
        originalContact: Contact?
    ) {
        val ops = ArrayList<ContentProviderOperation>()

        if (contact.id.isEmpty() || contact.id == "0" || contact.id == "null") {
            val rawContactIndex = ops.size
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build())

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.name)
                .build())

            val phonesToInsert = if (contact.phones.isNotEmpty()) {
                contact.phones.filter { it.number.isNotBlank() }.distinctBy { it.number.trim() }
            } else {
                contact.phoneNumbers.filter { it.isNotBlank() }.distinct().map { ContactPhone(it.trim(), ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) }
            }
            phonesToInsert.forEach { phone ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number.trim())
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                    .apply {
                        if (phone.type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM && !phone.label.isNullOrBlank()) {
                            withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                        }
                    }
                    .build())
            }

            contact.emails.filter { it.isNotBlank() }.distinct().forEach { email ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.trim())
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                    .build())
            }

            contact.addresses.filter { it.isNotBlank() }.distinct().forEach { address ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.trim())
                    .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                    .build())
            }

            if (!contact.note.isNullOrBlank()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.note.trim())
                    .build())
            }

            contact.events.forEach { event ->
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.date)
                    .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.type)
                    .apply {
                        if (!event.label.isNullOrBlank()) {
                            withValue(ContactsContract.CommonDataKinds.Event.LABEL, event.label)
                        }
                    }
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
            val allRawContacts = getRawContactsForContact(contact.id)
            val writableRawContacts = allRawContacts.filter { !it.isReadOnly }

            if (writableRawContacts.isEmpty()) {
                // If raw contact wasn't found or was only read-only sync adapter (e.g. WhatsApp), insert a new writable raw contact
                val rawContactIndex = ops.size
                val defaultAccount = getAvailableAccounts().firstOrNull { !it.key.startsWith("sim_") && !it.key.equals("whatsapp", ignoreCase = true) }
                val targetAccType = accountType ?: defaultAccount?.accountType
                val targetAccName = accountName ?: defaultAccount?.accountName

                ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, targetAccType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, targetAccName)
                    .build())

                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.name)
                    .build())

                val fallbackPhones = if (contact.phones.isNotEmpty()) {
                    contact.phones.filter { it.number.isNotBlank() }.distinctBy { it.number.trim() }
                } else {
                    contact.phoneNumbers.filter { it.isNotBlank() }.distinct().map { ContactPhone(it.trim(), ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) }
                }
                fallbackPhones.forEach { phone ->
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number.trim())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                        .apply {
                            if (phone.type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM && !phone.label.isNullOrBlank()) {
                                withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                            }
                        }
                        .build())
                }

                contact.emails.filter { it.isNotBlank() }.distinct().forEach { email ->
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.trim())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build())
                }

                contact.addresses.filter { it.isNotBlank() }.distinct().forEach { address ->
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.trim())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
                        .build())
                }

                if (!contact.note.isNullOrBlank()) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contact.note.trim())
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
                // Determine target raw contacts to update
                val targetRawContacts = if (updateAllAccounts || (accountType == null && accountName == null)) {
                    writableRawContacts
                } else {
                    val matching = writableRawContacts.filter { raw ->
                        val tMatch = (accountType == null && raw.accountType == null) ||
                            (accountType != null && raw.accountType?.equals(accountType, ignoreCase = true) == true)
                        val nMatch = (accountName == null && raw.accountName == null) ||
                            (accountName != null && raw.accountName?.equals(accountName, ignoreCase = true) == true)
                        tMatch && nMatch
                    }
                    if (matching.isNotEmpty()) matching else writableRawContacts
                }

                val primaryRawContactId = targetRawContacts.firstOrNull {
                    val type = it.accountType?.lowercase() ?: ""
                    type.contains("google") || type.contains("exchange") || type.contains("outlook") || type.contains("corporate") || type.contains("eas")
                }?.id ?: targetRawContacts.first().id

                // Always update StructuredName in place across all writable raw contacts so display name remains in sync
                // without destroying row IDs (Data._ID), which preserves sync adapter anchors (e.g. WhatsApp, Google Sync).
                val allWritableIds = writableRawContacts.map { it.id }.distinct()
                allWritableIds.forEach { rawId ->
                    updateStructuredName(ops, rawId, contact.name)
                }

                // Update data rows (phones, emails, addresses, notes) across target raw contacts in place
                targetRawContacts.forEach { rawRecord ->
                    val rawId = rawRecord.id
                    val type = rawRecord.accountType ?: ""
                    val name = rawRecord.accountName ?: ""
                    val simSlot = getSimSlotForAccount(type, name)
                    val isSim = simSlot >= 0

                    if (isSim) {
                        updateContactOnSim(
                            oldName = originalContact?.name ?: contact.name,
                            oldNumber = originalContact?.phoneNumbers?.firstOrNull() ?: "",
                            newName = contact.name,
                            newNumber = contact.phoneNumbers.firstOrNull() ?: "",
                            simSlotIndex = simSlot
                        )
                    }

                    // In-place phone update preserves Data._ID for WhatsApp / sync adapters
                    val phonesToUpdate = if (contact.phones.isNotEmpty()) contact.phones else contact.phoneNumbers.map { ContactPhone(it, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) }
                    updatePhoneNumbersWithPhones(ops, rawId, phonesToUpdate)

                    // SIM records only store name and phone number; only non-SIM stores emails, addresses, and notes
                    if (!isSim) {
                        updateEmails(ops, rawId, contact.emails)
                        updateAddresses(ops, rawId, contact.addresses)
                        updateNote(ops, rawId, contact.note)
                    }
                }

                // Handle Photo update
                if (contact.photoUri == null) {
                    allWritableIds.forEach { rawId ->
                        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            ).build())
                    }
                } else if (!contact.photoUri.startsWith("content://com.android.contacts")) {
                    val photoBytes = loadPhotoBytes(contact.photoUri)
                    if (photoBytes != null) {
                        allWritableIds.forEach { rawId ->
                            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                .withSelection(
                                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                                    arrayOf(rawId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                ).build())
                        }
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, primaryRawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                            .build())
                    }
                }
            }
        }

        try {
            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
            clearNumberLookupCache()

            // CRITICAL: Ensure all writable raw contacts associated with this contact stay unified
            // with TYPE_KEEP_TOGETHER so Android's ContactAggregator never splits them.
            // Never include read-only sync adapters (like WhatsApp) in TYPE_KEEP_TOGETHER!
            // Reset sync adapter raw contacts to TYPE_AUTOMATIC so WhatsApp auto-links seamlessly.
            val finalContactId = contact.id.ifEmpty { "0" }
            if (finalContactId != "0" && finalContactId != "null") {
                val allRaw = getRawContactsForContact(finalContactId)
                val writableRawIds = allRaw.filter { !it.isReadOnly }.map { it.id }.distinct()
                if (writableRawIds.size > 1) {
                    setAggregationKeepTogether(writableRawIds)
                }
                ensureSyncAdapterAggregationAutomatic(allRaw)
            }

            // Explicitly notify content observers so external apps (e.g. WhatsApp, Google Contacts)
            // immediately detect the changes and refresh their contact caches.
            try {
                contentResolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.RawContacts.CONTENT_URI, null)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error saving contact", e)
        }
    }

    override fun updateContactNote(contactId: String, note: String?) {
        val ops = ArrayList<ContentProviderOperation>()
        val allRawContacts = getRawContactsForContact(contactId)
        val writableRawContacts = allRawContacts.filter { !it.isReadOnly }
        val targetRawContactId = writableRawContacts.firstOrNull()?.id ?: return

        updateNote(ops, targetRawContactId, note)

        try {
            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                clearNumberLookupCache()
                try {
                    contentResolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
                    contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error updating contact note", e)
        }
    }

    override fun deleteContact(contactId: String) {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            contentResolver.delete(uri, null, null)
            clearNumberLookupCache()
            try {
                contentResolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.RawContacts.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    override fun deletePhoneNumberFromContact(contactId: String, phoneNumber: String): Boolean {
        return try {
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            val cleanTarget = phoneNumber.trim()

            val rowsToDelete = mutableListOf<Long>()
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val num = if (numIdx >= 0) cursor.getString(numIdx) else null
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else -1L
                    if (id > 0 && num != null && numbersLikelyMatch(num, cleanTarget)) {
                        rowsToDelete.add(id)
                    }
                }
            }

            var deletedCount = 0
            for (rowId in rowsToDelete) {
                deletedCount += contentResolver.delete(
                    ContactsContract.Data.CONTENT_URI,
                    "${ContactsContract.Data._ID} = ?",
                    arrayOf(rowId.toString())
                )
            }
            if (deletedCount > 0) {
                clearNumberLookupCache()
                try {
                    contentResolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
                    contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
                } catch (_: Exception) {}
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error deleting phone number $phoneNumber from contact $contactId", e)
            false
        }
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
        return getRawContactsForContact(contactId).map { it.id }
    }

    private fun deleteRawContactsForContact(contactId: String) {
        getRawContactIdsForContact(contactId).forEach { deleteRawContact(it) }
    }

    override fun deleteRawContact(rawContactId: Long) {
        try {
            val uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                .appendPath(rawContactId.toString())
                .build()
            contentResolver.delete(uri, null, null)
            clearNumberLookupCache()
            try {
                contentResolver.notifyChange(ContactsContract.Contacts.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.RawContacts.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
            } catch (_: Exception) {}
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


    private val numberLookupCache = java.util.concurrent.ConcurrentHashMap<String, Contact>()
    private val unknownNumberCache = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    fun clearNumberLookupCache() {
        numberLookupCache.clear()
        unknownNumberCache.clear()
    }

    override fun getContactByNumber(number: String): Contact? {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return null
        numberLookupCache[trimmed]?.let { return it }
        if (unknownNumberCache.contains(trimmed)) return null

        return try {
            val contact = getContactByNumberInternal(trimmed)
            if (contact != null) {
                numberLookupCache[trimmed] = contact
            } else {
                unknownNumberCache.add(trimmed)
            }
            contact
        } catch (_: Exception) {
            null
        }
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
            val cursor = try {
                contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    groupProjection,
                    "(${ContactsContract.Groups.DELETED} = 0 OR ${ContactsContract.Groups.DELETED} IS NULL)",
                    null,
                    "${ContactsContract.Groups.TITLE} ASC"
                )
            } catch (_: Exception) {
                contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    groupProjection,
                    null,
                    null,
                    "${ContactsContract.Groups.TITLE} ASC"
                )
            }
            cursor?.use { c ->
                val idIdx = c.getColumnIndex(ContactsContract.Groups._ID)
                val titleIdx = c.getColumnIndex(ContactsContract.Groups.TITLE)
                val typeIdx = c.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE)
                val nameIdx = c.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME)
                val deletedIdx = c.getColumnIndex(ContactsContract.Groups.DELETED)

                while (c.moveToNext()) {
                    if (deletedIdx >= 0 && !c.isNull(deletedIdx) && c.getInt(deletedIdx) != 0) {
                        continue
                    }
                    val rowId = if (idIdx >= 0) c.getLong(idIdx) else continue
                    val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "" else ""
                    val accType = if (typeIdx >= 0) c.getString(typeIdx) else null
                    val accName = if (nameIdx >= 0) c.getString(nameIdx) else null

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

    override fun getSystemGroupMembers(groupRowIds: Set<Long>): Map<Long, List<String>> {
        if (groupRowIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<Long, MutableList<String>>()
        val inClause = groupRowIds.joinToString(",") { it.toString() }
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} IN ($inClause)",
                arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val contactIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val groupRowIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
                while (cursor.moveToNext()) {
                    val contactId = if (contactIdIdx >= 0) cursor.getString(contactIdIdx) else null
                    val rowId = if (groupRowIdIdx >= 0) cursor.getLong(groupRowIdIdx) else null
                    if (contactId != null && rowId != null) {
                        result.getOrPut(rowId) { mutableListOf() }.add(contactId)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error querying group members", e)
        }
        return result
    }

    override fun getActiveSystemGroupIds(groupRowIds: Set<Long>): Set<Long> {
        if (groupRowIds.isEmpty()) return emptySet()
        val activeIds = mutableSetOf<Long>()
        val inClause = groupRowIds.joinToString(",") { it.toString() }
        try {
            val cursor = try {
                contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.DELETED),
                    "${ContactsContract.Groups._ID} IN ($inClause) AND (${ContactsContract.Groups.DELETED} = 0 OR ${ContactsContract.Groups.DELETED} IS NULL)",
                    null,
                    null
                )
            } catch (_: Exception) {
                contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    arrayOf(ContactsContract.Groups._ID),
                    "${ContactsContract.Groups._ID} IN ($inClause)",
                    null,
                    null
                )
            }
            cursor?.use { c ->
                val idIdx = c.getColumnIndex(ContactsContract.Groups._ID)
                val delIdx = c.getColumnIndex(ContactsContract.Groups.DELETED)
                while (c.moveToNext()) {
                    if (delIdx >= 0 && !c.isNull(delIdx) && c.getInt(delIdx) != 0) {
                        continue
                    }
                    if (idIdx >= 0) {
                        activeIds.add(c.getLong(idIdx))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error checking active system group IDs", e)
        }
        return activeIds
    }

    override fun findSystemGroupId(groupName: String, accountType: String?, accountName: String?): Long? {
        val projection = arrayOf(ContactsContract.Groups._ID)
        val selection = StringBuilder("${ContactsContract.Groups.TITLE} = ? AND (${ContactsContract.Groups.DELETED} = 0 OR ${ContactsContract.Groups.DELETED} IS NULL)")
        val args = mutableListOf(groupName)
        if (!accountType.isNullOrBlank()) {
            selection.append(" AND ${ContactsContract.Groups.ACCOUNT_TYPE} = ?")
            args.add(accountType)
        }
        if (!accountName.isNullOrBlank()) {
            selection.append(" AND ${ContactsContract.Groups.ACCOUNT_NAME} = ?")
            args.add(accountName)
        }
        return try {
            val cursor = try {
                contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    projection,
                    selection.toString(),
                    args.toTypedArray(),
                    null
                )
            } catch (_: Exception) {
                contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    projection,
                    "${ContactsContract.Groups.TITLE} = ?",
                    arrayOf(groupName),
                    null
                )
            }
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idIdx = c.getColumnIndex(ContactsContract.Groups._ID)
                    if (idIdx >= 0) c.getLong(idIdx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
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

                try {
                    contentResolver.notifyChange(ContactsContract.Groups.CONTENT_URI, null)
                    contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
                } catch (_: Exception) {}

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
            try {
                contentResolver.notifyChange(ContactsContract.Groups.CONTENT_URI, null)
                contentResolver.notifyChange(ContactsContract.Data.CONTENT_URI, null)
            } catch (_: Exception) {}
            deletedRows > 0
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepo", "Error deleting system contact group", e)
            false
        }
    }
}
