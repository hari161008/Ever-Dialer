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
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository

class ContactsRepository(private val contentResolver: ContentResolver, private val context: Context) : IContactsRepository {

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
                            // SIM/device contacts have blank or local account type
                            val isLocalType = accountType.isBlank() ||
                                accountType.equals("com.android.local", ignoreCase = true) ||
                                accountType.equals("com.android.contacts", ignoreCase = true)
                            if (!isLocalType) return@any false
                            // If multiple SIMs, further disambiguate by slot
                            val slotNum = key.removePrefix("sim_").toIntOrNull() ?: 0
                            val slot = getSimSlotForAccount(accountName)
                            // slot == -1 means device storage (slot 0 by default), slotNum 0 == device
                            if (slot == -1) slotNum == 0 || slotNum == 1
                            else (slot + 1) == slotNum
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

    override fun getAvailableAccounts(): List<ContactAccount> {
        data class AccInfo(val type: String, val name: String, val contactIds: MutableSet<Long> = mutableSetOf())
        val accountMap = mutableMapOf<String, AccInfo>()

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
                val type = cursor.getString(typeIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: ""
                val key = buildAccountKey(type, name)
                accountMap.getOrPut(key) { AccInfo(type, name) }.contactIds.add(contactId)
            }
        }
        return accountMap.map { (key, info) ->
            ContactAccount(
                key          = key,
                displayName  = buildAccountDisplayName(info.type, info.name),
                accountType  = info.type,
                accountName  = info.name,
                contactCount = info.contactIds.size
            )
        }.filter { it.contactCount > 0 }.sortedByDescending { it.contactCount }
    }

    private fun buildAccountKey(type: String, name: String): String = when {
        type.contains("google", ignoreCase = true) -> "google_$name"
        type.contains("whatsapp", ignoreCase = true) -> "whatsapp"
        else -> {
            // SIM / local / device storage — assign per-SIM keys using SubscriptionManager
            val simSlot = getSimSlotForAccount(name)
            if (simSlot >= 0) "sim_${simSlot + 1}" else "sim_0"
        }
    }

    private fun buildAccountDisplayName(type: String, name: String): String = when {
        type.contains("google", ignoreCase = true) -> name.ifBlank { "Google" }
        type.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
        else -> {
            val simSlot = getSimSlotForAccount(name)
            when {
                simSlot == 0 -> "SIM 1"
                simSlot == 1 -> "SIM 2"
                simSlot > 1  -> "SIM ${simSlot + 1}"
                else         -> "Device Storage"
            }
        }
    }

    /** Returns 0-based SIM slot index if the account name matches a SIM subscription, else -1. */
    private fun getSimSlotForAccount(accountName: String): Int {
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
            // Match by display name or ICC ID that may be embedded in accountName
            subs.firstOrNull { sub ->
                sub.displayName?.toString()?.equals(accountName, ignoreCase = true) == true ||
                sub.iccId?.equals(accountName, ignoreCase = true) == true ||
                accountName.contains(sub.subscriptionId.toString())
            }?.simSlotIndex ?: -1
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
