package com.coolappstore.everdialer.by.svhp.modal.repository

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.modal.data.MmsPart
import com.coolappstore.everdialer.by.svhp.modal.data.SmsConversation
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class SmsRepository(
    private val context: Context,
    private val contentResolver: ContentResolver
) : ISmsRepository {

    companion object {
        private val CONVERSATIONS_URI = Uri.parse("content://mms-sms/conversations?simple=true")
        private val CANONICAL_ADDRESSES_URI = Uri.parse("content://mms-sms/canonical-addresses")
        private val MMS_PART_URI = Uri.parse("content://mms/part")
        private const val PDU_HEADER_FROM = 137
        private const val PDU_HEADER_TO = 151
    }

    // In-memory cache for contact resolution: phone number -> (displayName, photoUri)
    private val contactLookupCache = ConcurrentHashMap<String, Pair<String, String?>>()

    private fun ensureContactCache() {
        if (contactLookupCache.isNotEmpty()) return
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                while (cursor.moveToNext()) {
                    val rawNum = if (numCol >= 0) cursor.getString(numCol) else null
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val photo = if (photoCol >= 0) cursor.getString(photoCol) else null
                    if (!rawNum.isNullOrBlank() && !name.isNullOrBlank()) {
                        val pair = Pair(name, photo)
                        contactLookupCache[rawNum] = pair
                        val normalized = rawNum.replace("[^0-9+]".toRegex(), "")
                        if (normalized.isNotBlank()) {
                            contactLookupCache[normalized] = pair
                            if (normalized.length >= 7) {
                                contactLookupCache[normalized.takeLast(7)] = pair
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    private fun hasSendSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    /**
     * Map all canonical recipient IDs to their raw phone numbers/addresses.
     */
    private fun getCanonicalAddresses(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        try {
            contentResolver.query(
                CANONICAL_ADDRESSES_URI,
                arrayOf("_id", "address"),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val addrIdx = cursor.getColumnIndex("address")
                while (cursor.moveToNext()) {
                    if (idIdx >= 0 && addrIdx >= 0) {
                        val id = cursor.getLong(idIdx)
                        val address = cursor.getString(addrIdx) ?: ""
                        map[id] = address
                    }
                }
            }
        } catch (_: Throwable) {
            // Permission or provider unavailable
        }
        return map
    }

    override suspend fun getConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) return@withContext emptyList()
        ensureContactCache()

        val canonicalMap = getCanonicalAddresses()
        val result = mutableListOf<SmsConversation>()

        try {
            val projection = arrayOf(
                Telephony.Threads._ID,
                Telephony.Threads.DATE,
                Telephony.Threads.MESSAGE_COUNT,
                Telephony.Threads.RECIPIENT_IDS,
                Telephony.Threads.SNIPPET,
                Telephony.Threads.READ,
                Telephony.Threads.TYPE
            )

            contentResolver.query(
                CONVERSATIONS_URI,
                projection,
                null,
                null,
                "${Telephony.Threads.DATE} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Threads._ID)
                val dateCol = cursor.getColumnIndex(Telephony.Threads.DATE)
                val msgCountCol = cursor.getColumnIndex(Telephony.Threads.MESSAGE_COUNT)
                val recipientIdsCol = cursor.getColumnIndex(Telephony.Threads.RECIPIENT_IDS)
                val snippetCol = cursor.getColumnIndex(Telephony.Threads.SNIPPET)
                val readCol = cursor.getColumnIndex(Telephony.Threads.READ)

                while (cursor.moveToNext()) {
                    val threadId = if (idCol >= 0) cursor.getLong(idCol) else continue
                    var date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    // Normalise timestamp: MMS dates in seconds vs SMS in ms
                    if (date > 0 && date < 1000000000000L) {
                        date *= 1000L
                    }

                    val rawRecipientIds = if (recipientIdsCol >= 0) cursor.getString(recipientIdsCol) ?: "" else ""
                    val recipientIds = rawRecipientIds.split(" ")
                        .mapNotNull { it.trim().toLongOrNull() }

                    val addresses = recipientIds.mapNotNull { canonicalMap[it] }.filter { it.isNotBlank() }
                    var primaryAddress = addresses.firstOrNull() ?: ""

                    // If canonical address table didn't have the address, resolve from latest SMS/MMS in this thread
                    if (primaryAddress.isBlank()) {
                        primaryAddress = resolveAddressForThread(threadId)
                    }

                    val isRead = if (readCol >= 0) cursor.getInt(readCol) == 1 else true
                    val snippet = if (snippetCol >= 0) cursor.getString(snippetCol) ?: "" else ""

                    // Resolve contact details
                    val contactInfo = resolveContact(primaryAddress)
                    val unreadCount = if (!isRead) 1 else 0

                    result.add(
                        SmsConversation(
                            threadId = threadId,
                            address = primaryAddress,
                            contactName = contactInfo?.first,
                            photoUri = contactInfo?.second,
                            snippet = snippet,
                            date = date,
                            unreadCount = unreadCount,
                            isRead = isRead,
                            recipientIds = recipientIds,
                            allAddresses = addresses
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            // Fallback for custom ROMs where mms-sms/conversations?simple=true is restricted
            return@withContext fallbackGetConversations()
        }

        if (result.isEmpty()) {
            return@withContext fallbackGetConversations()
        }

        result
    }

    /**
     * Fallback method using Telephony.Sms directly if simple=true is empty or restricted.
     */
    private fun fallbackGetConversations(): List<SmsConversation> {
        val conversationsMap = mutableMapOf<Long, SmsConversation>()
        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            )

            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val threadIdCol = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val readCol = cursor.getColumnIndex(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val threadId = cursor.getLong(threadIdCol)
                    if (!conversationsMap.containsKey(threadId)) {
                        val address = cursor.getString(addressCol) ?: ""
                        val body = cursor.getString(bodyCol) ?: ""
                        val date = cursor.getLong(dateCol)
                        val read = cursor.getInt(readCol)
                        val contact = resolveContact(address)

                        conversationsMap[threadId] = SmsConversation(
                            threadId = threadId,
                            address = address,
                            contactName = contact?.first,
                            photoUri = contact?.second,
                            snippet = body,
                            date = date,
                            unreadCount = if (read == 0) 1 else 0,
                            isRead = read == 1
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore
        }
        return conversationsMap.values.sortedByDescending { it.date }
    }

    private fun resolveAddressForThread(threadId: Long): String {
        try {
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val addr = cursor.getString(0)
                    if (!addr.isNullOrBlank()) return addr
                }
            }
        } catch (_: Throwable) {}
        return ""
    }

    override suspend fun getMessagesForThread(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) return@withContext emptyList()

        val messages = mutableListOf<SmsMessage>()

        // 1. Fetch SMS messages for thread
        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.STATUS
            )

            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val threadCol = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addrCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val readCol = cursor.getColumnIndex(Telephony.Sms.READ)
                val statusCol = cursor.getColumnIndex(Telephony.Sms.STATUS)

                while (cursor.moveToNext()) {
                    messages.add(
                        SmsMessage(
                            id = cursor.getLong(idCol),
                            threadId = cursor.getLong(threadCol),
                            address = cursor.getString(addrCol) ?: "",
                            body = cursor.getString(bodyCol) ?: "",
                            date = cursor.getLong(dateCol),
                            type = cursor.getInt(typeCol),
                            isRead = cursor.getInt(readCol) == 1,
                            isMms = false,
                            deliveryStatus = if (statusCol >= 0) cursor.getInt(statusCol) else -1
                        )
                    )
                }
            }
        } catch (_: Throwable) {}

        // 2. Fetch MMS messages for thread
        try {
            val mmsProjection = arrayOf(
                Telephony.Mms._ID,
                Telephony.Mms.THREAD_ID,
                Telephony.Mms.DATE,
                Telephony.Mms.MESSAGE_BOX,
                Telephony.Mms.READ,
                Telephony.Mms.SUBJECT
            )

            contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                mmsProjection,
                "${Telephony.Mms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Mms.DATE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Mms._ID)
                val threadCol = cursor.getColumnIndex(Telephony.Mms.THREAD_ID)
                val dateCol = cursor.getColumnIndex(Telephony.Mms.DATE)
                val boxCol = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX)
                val readCol = cursor.getColumnIndex(Telephony.Mms.READ)
                val subjCol = cursor.getColumnIndex(Telephony.Mms.SUBJECT)

                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idCol)
                    val rawDate = cursor.getLong(dateCol)
                    // MMS date is in seconds
                    val date = if (rawDate < 1000000000000L) rawDate * 1000L else rawDate
                    val box = cursor.getInt(boxCol)
                    val isRead = cursor.getInt(readCol) == 1
                    val subject = if (subjCol >= 0) cursor.getString(subjCol) else null

                    // Type mapping: box 1 = inbox, box 2 = sent
                    val msgType = if (box == Telephony.Mms.MESSAGE_BOX_INBOX) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX
                    } else {
                        Telephony.Sms.MESSAGE_TYPE_SENT
                    }

                    // Extract MMS sender/recipient address
                    val mmsAddress = getMmsAddress(mmsId)

                    // Extract MMS parts (text, image, audio, video)
                    val parts = getMmsParts(mmsId)
                    val textBody = parts.firstOrNull { it.contentType == "text/plain" && !it.text.isNullOrBlank() }?.text
                        ?: subject ?: ""

                    messages.add(
                        SmsMessage(
                            id = mmsId,
                            threadId = cursor.getLong(threadCol),
                            address = mmsAddress,
                            body = textBody,
                            date = date,
                            type = msgType,
                            isRead = isRead,
                            isMms = true,
                            subject = subject,
                            parts = parts
                        )
                    )
                }
            }
        } catch (_: Throwable) {}

        // Sort combined SMS + MMS messages chronologically and deduplicate
        val sorted = messages.sortedBy { it.date }
        val deduplicated = mutableListOf<SmsMessage>()
        for (msg in sorted) {
            val isDuplicate = deduplicated.any { existing ->
                existing.isMms == msg.isMms &&
                existing.type == msg.type &&
                existing.address == msg.address &&
                existing.body == msg.body &&
                kotlin.math.abs(existing.date - msg.date) < 3000L
            }
            if (!isDuplicate) {
                deduplicated.add(msg)
            }
        }
        deduplicated
    }

    private fun getMmsAddress(mmsId: Long): String {
        try {
            val uri = Telephony.Mms.CONTENT_URI.buildUpon()
                .appendPath(mmsId.toString())
                .appendPath("addr")
                .build()

            contentResolver.query(
                uri,
                arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE),
                "${Telephony.Mms.Addr.TYPE} = $PDU_HEADER_FROM",
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val addrIdx = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS)
                    if (addrIdx >= 0) {
                        val addr = cursor.getString(addrIdx)
                        if (!addr.isNullOrBlank() && !addr.contains("insert-address-token")) {
                            return addr
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return ""
    }

    private fun getMmsParts(mmsId: Long): List<MmsPart> {
        val parts = mutableListOf<MmsPart>()
        try {
            contentResolver.query(
                MMS_PART_URI,
                arrayOf(
                    Telephony.Mms.Part._ID,
                    Telephony.Mms.Part.MSG_ID,
                    Telephony.Mms.Part.CONTENT_TYPE,
                    Telephony.Mms.Part.TEXT
                ),
                "${Telephony.Mms.Part.MSG_ID} = ?",
                arrayOf(mmsId.toString()),
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Mms.Part._ID)
                val ctCol = cursor.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE)
                val textCol = cursor.getColumnIndex(Telephony.Mms.Part.TEXT)

                while (cursor.moveToNext()) {
                    val partId = cursor.getLong(idCol)
                    val contentType = cursor.getString(ctCol) ?: "*/*"
                    var text = cursor.getString(textCol)

                    // If text is in part body data file, read it
                    if (contentType == "text/plain" && text == null) {
                        text = readPartText(partId)
                    }

                    val uriString = if (contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.startsWith("audio/")) {
                        "content://mms/part/$partId"
                    } else null

                    parts.add(
                        MmsPart(
                            id = partId,
                            messageId = mmsId,
                            contentType = contentType,
                            uri = uriString,
                            text = text
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
        return parts
    }

    private fun readPartText(partId: Long): String? {
        val partUri = Uri.parse("content://mms/part/$partId")
        return try {
            contentResolver.openInputStream(partUri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, "UTF-8")).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun getOrCreateThreadId(address: String): Long = withContext(Dispatchers.IO) {
        try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (_: Throwable) {
            -1L
        }
    }

    override suspend fun sendSms(address: String, body: String, subId: Int?): Boolean = withContext(Dispatchers.IO) {
        if (address.isBlank() || body.isBlank()) return@withContext false

        return@withContext try {
            val smsManager: SmsManager = if (subId != null && subId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
            } else if (subId != null && subId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(address, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(address, null, body, null, null)
            }

            // Persist into Telephony.Sms.Sent
            val threadId = try {
                Telephony.Threads.getOrCreateThreadId(context, address)
            } catch (_: Throwable) {
                null
            }

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                if (threadId != null && threadId > 0) {
                    put(Telephony.Sms.THREAD_ID, threadId)
                }
                if (subId != null && subId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                }
            }

            try {
                contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            } catch (_: Throwable) {
                // Handled gracefully if app is not currently set as default SMS app
            }

            true
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun deleteThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            var count = 0
            // 1. Delete conversation via native conversation URI
            try {
                val convUri = ContentUris.withAppendedId(Uri.parse("content://sms/conversations"), threadId)
                count += contentResolver.delete(convUri, null, null)
            } catch (_: Throwable) {}

            // 2. Clean up any remaining SMS rows for thread
            try {
                count += contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString())
                )
            } catch (_: Throwable) {}

            // 3. Clean up any MMS rows for thread
            try {
                count += contentResolver.delete(
                    Telephony.Mms.CONTENT_URI,
                    "${Telephony.Mms.THREAD_ID} = ?",
                    arrayOf(threadId.toString())
                )
            } catch (_: Throwable) {}

            count > 0
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun deleteMessage(messageId: Long, isMms: Boolean): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = if (isMms) {
                ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, messageId)
            } else {
                ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)
            }
            val count = contentResolver.delete(uri, null, null)
            count > 0
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun markThreadAsRead(threadId: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            contentResolver.update(
                Telephony.Sms.Inbox.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )
        } catch (_: Throwable) {}

        try {
            val mmsValues = ContentValues().apply {
                put(Telephony.Mms.READ, 1)
                put(Telephony.Mms.SEEN, 1)
            }
            contentResolver.update(
                Telephony.Mms.Inbox.CONTENT_URI,
                mmsValues,
                "${Telephony.Mms.THREAD_ID} = ? AND ${Telephony.Mms.READ} = 0",
                arrayOf(threadId.toString())
            )
        } catch (_: Throwable) {}
    }

    override fun resolveContactInfo(address: String): Pair<String?, String?> {
        val result = resolveContact(address)
        return Pair(result?.first, result?.second)
    }

    override suspend fun getMessagesByIds(messageIds: Set<Long>): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty() || !hasReadSmsPermission()) return@withContext emptyList()
        val messages = mutableListOf<SmsMessage>()
        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.STATUS
            )
            val inClause = messageIds.joinToString(",")
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms._ID} IN ($inClause)",
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val threadCol = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addrCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val readCol = cursor.getColumnIndex(Telephony.Sms.READ)
                val statusCol = cursor.getColumnIndex(Telephony.Sms.STATUS)

                while (cursor.moveToNext()) {
                    messages.add(
                        SmsMessage(
                            id = cursor.getLong(idCol),
                            threadId = cursor.getLong(threadCol),
                            address = cursor.getString(addrCol) ?: "",
                            body = cursor.getString(bodyCol) ?: "",
                            date = cursor.getLong(dateCol),
                            type = cursor.getInt(typeCol),
                            isRead = cursor.getInt(readCol) == 1,
                            isMms = false,
                            deliveryStatus = if (statusCol >= 0) cursor.getInt(statusCol) else -1
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
        messages
    }

    /**
     * Fast contact name and photo lookup with thread-safe caching.
     */
    private fun resolveContact(phoneNumber: String): Pair<String, String?>? {
        if (phoneNumber.isBlank()) return null
        contactLookupCache[phoneNumber]?.let { return it }
        val normalized = phoneNumber.replace("[^0-9+]".toRegex(), "")
        if (normalized.isNotBlank()) {
            contactLookupCache[normalized]?.let { return it }
            if (normalized.length >= 7) {
                contactLookupCache[normalized.takeLast(7)]?.let { return it }
            }
        }

        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIdx = c.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val name = if (nameIdx >= 0) c.getString(nameIdx) else null
                    val photo = if (photoIdx >= 0) c.getString(photoIdx) else null
                    if (!name.isNullOrBlank()) {
                        val pair = Pair(name, photo)
                        contactLookupCache[phoneNumber] = pair
                        pair
                    } else null
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
