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
                                accountType.equals("com.android.contacts", ignoreCase = true) ||
                                accountType.contains("sim", ignoreCase = true) ||
                                accountType.contains("icc", ignoreCase = true)
                            if (!isLocalType) return@any false
                            val slotNum = key.removePrefix("sim_").toIntOrNull() ?: 0
                            val slot = getSimSlotForAccount(accountType, accountName)
                            // slot == -1 means genuine device/local storage (maps to key "sim_0");
                            // slot 0/1/... map to "sim_1"/"sim_2"/... — a raw contact can only ever
                            // satisfy exactly one of these, never fall through into SIM1/SIM2 by default.
                            if (slot == -1) slotNum == 0
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

    private fun buildAccountKey(type: String, name: String): String = when {
        type.contains("google", ignoreCase = true) -> "google_$name"
        type.contains("whatsapp", ignoreCase = true) -> "whatsapp"
        else -> {
            // SIM / local / device storage — assign per-SIM keys using SubscriptionManager
            val simSlot = getSimSlotForAccount(type, name)
            if (simSlot >= 0) "sim_${simSlot + 1}" else "sim_0"
        }
    }

    private fun buildAccountDisplayName(type: String, name: String): String = when {
        type.contains("google", ignoreCase = true) -> name.ifBlank { "Google" }
        type.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
        else -> {
            val simSlot = getSimSlotForAccount(type, name)
            when {
                simSlot == 0 -> "SIM 1"
                simSlot == 1 -> "SIM 2"
                simSlot > 1  -> "SIM ${simSlot + 1}"
                else         -> "Device Storage"
            }
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
