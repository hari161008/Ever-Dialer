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
import com.coolappstore.everdialer.by.svhp.controller.util.ContactsCache
import com.coolappstore.everdialer.by.svhp.modal.data.SmsConversation
import com.coolappstore.everdialer.by.svhp.modal.data.SmsMessage
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(
    private val context: Context,
    private val contentResolver: ContentResolver
) : ISmsRepository {

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    private fun hasSendSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    override suspend fun getConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) return@withContext emptyList()

        val conversationsMap = mutableMapOf<Long, SmsConversation>()
        val unreadCounts = mutableMapOf<Long, Int>()

        // Fast in-memory cache for contact phone numbers
        val contacts = ContactsCache.read(context)
        val contactLookup = mutableMapOf<String, Pair<String, String?>>()
        contacts.forEach { contact ->
            contact.phoneNumbers.forEach { num ->
                val normalized = num.filter { it.isDigit() }
                if (normalized.isNotBlank()) {
                    contactLookup[normalized] = Pair(contact.name, contact.photoUri)
                }
            }
        }

        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE
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
                    val read = cursor.getInt(readCol)
                    if (read == 0) {
                        unreadCounts[threadId] = (unreadCounts[threadId] ?: 0) + 1
                    }

                    if (!conversationsMap.containsKey(threadId)) {
                        val address = cursor.getString(addressCol) ?: ""
                        val body = cursor.getString(bodyCol) ?: ""
                        val date = cursor.getLong(dateCol)

                        // Resolve contact details
                        var contactName: String? = null
                        var photoUri: String? = null

                        val normAddr = address.filter { it.isDigit() }
                        val matched = contactLookup.entries.firstOrNull { (k, _) ->
                            k.endsWith(normAddr) || normAddr.endsWith(k)
                        }?.value

                        if (matched != null) {
                            contactName = matched.first
                            photoUri = matched.second
                        } else if (address.isNotBlank()) {
                            resolveContactFromPhoneLookup(address)?.let { resolved ->
                                contactName = resolved.first
                                photoUri = resolved.second
                            }
                        }

                        conversationsMap[threadId] = SmsConversation(
                            threadId = threadId,
                            address = address,
                            contactName = contactName,
                            photoUri = photoUri,
                            snippet = body,
                            date = date,
                            unreadCount = 0,
                            isRead = read == 1
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            return@withContext emptyList()
        }

        conversationsMap.values.map { conv ->
            val unread = unreadCounts[conv.threadId] ?: 0
            conv.copy(
                unreadCount = unread,
                isRead = unread == 0
            )
        }.sortedByDescending { it.date }
    }

    override suspend fun getMessagesForThread(threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) return@withContext emptyList()

        val messages = mutableListOf<SmsMessage>()
        try {
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
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

                while (cursor.moveToNext()) {
                    messages.add(
                        SmsMessage(
                            id = cursor.getLong(idCol),
                            threadId = cursor.getLong(threadCol),
                            address = cursor.getString(addrCol) ?: "",
                            body = cursor.getString(bodyCol) ?: "",
                            date = cursor.getLong(dateCol),
                            type = cursor.getInt(typeCol),
                            isRead = cursor.getInt(readCol) == 1
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            // Ignore
        }
        messages
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
                // If not default SMS app, insert might fail on some Android versions, which is handled gracefully
            }

            true
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun deleteThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val count = contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString())
            )
            count > 0
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun deleteMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)
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
        } catch (_: Throwable) {
            // Ignore if restricted
        }
    }

    private fun resolveContactFromPhoneLookup(phoneNumber: String): Pair<String, String?>? {
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
                    if (!name.isNullOrBlank()) Pair(name, photo) else null
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
