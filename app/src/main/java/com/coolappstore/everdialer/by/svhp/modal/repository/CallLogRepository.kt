package com.coolappstore.everdialer.by.svhp.modal.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SubscriptionManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry

class CallLogRepository(
    private val context: Context,
    private val contentResolver: ContentResolver
) : ICallLogRepository {

    // Persist across calls (not just within one getCallLogs() pass) so that repeated
    // refreshes — e.g. the content-observer-triggered refresh right after a call ends,
    // or the background refresh that runs every time the Calls tab is opened — don't
    // redo a PhoneLookup query for numbers we've already resolved. This was the main
    // reason the call history felt slow to update: previously these maps were declared
    // *inside* getCallLogs(), so every single refresh re-resolved every unique number
    // in the whole call history from scratch, even though almost none of it had changed.
    private val contactInfoCache = mutableMapOf<String, Triple<String?, String?, Long?>>()
    private val simSlotCache = mutableMapOf<String, Int>()

    private val contactsObserver = object : android.database.ContentObserver(
        android.os.Handler(android.os.Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean) {
            // A contact was added/renamed/deleted — the cached name/photo lookups above
            // are now stale, so drop them and let the next getCallLogs() pass re-resolve.
            contactInfoCache.clear()
        }
    }

    init {
        try {
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, contactsObserver
            )
        } catch (_: Exception) {}
    }

    override fun getCallLogs(): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->

            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val phoneAccountIdIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx) ?: "Unknown"
                val type = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                val duration = cursor.getLong(durationIdx)
                val phoneAccountId = if (phoneAccountIdIdx >= 0) cursor.getString(phoneAccountIdIdx) else null
                val simSlot = if (phoneAccountId.isNullOrBlank()) -1 else
                    simSlotCache.getOrPut(phoneAccountId) { getSimSlotForPhoneAccountId(phoneAccountId) }

                val (contactName, photoUri, contactId) =
                    contactInfoCache.getOrPut(number) {
                        getContactDataByNumber(number)
                    }

                val cachedName = cursor.getString(cachedNameIdx)
                val isCallerIdName = contactName == null && cachedName != null
                val displayName = contactName ?: cachedName ?: number

                val lastEntry = callLogs.lastOrNull()
                // Only merge consecutive rows into one grouped entry when they're the same
                // number AND fall on the same calendar day. Without the day check, e.g.
                // 2 calls to a contact yesterday immediately followed (in the query, since
                // there's nothing else in between for that number) by 1 call to the same
                // contact today would all collapse into a single "3 calls" entry stamped
                // with today's date — showing an inflated count for a day that only had 1
                // actual call.
                if (lastEntry != null && lastEntry.number == number && isSameCalendarDay(lastEntry.date, date)) {
                    val updatedEntry = lastEntry.copy(
                        types = lastEntry.types + type
                    )
                    callLogs[callLogs.size - 1] = updatedEntry
                } else {
                    callLogs.add(
                        CallLogEntry(
                            number = number,
                            name = displayName,
                            type = type,
                            date = date,
                            duration = duration,
                            photoUri = photoUri,
                            contactId = contactId?.toString(),
                            types = listOf(type),
                            isCallerIdName = isCallerIdName,
                            simSlot = simSlot
                        )
                    )
                }
            }
        }

        return callLogs
    }

    /** Resolves a call log's PHONE_ACCOUNT_ID (the telecom PhoneAccountHandle id) to a
     *  0-based SIM slot index via SubscriptionManager, or -1 if it can't be determined. */
    /** True if the two epoch-millis timestamps fall on the same calendar day (local timezone). */
    private fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
        val c1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR) &&
            c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun getSimSlotForPhoneAccountId(phoneAccountId: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return -1
        return try {
            val subId = phoneAccountId.toIntOrNull() ?: return -1
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager ?: return -1
            val info = sm.getActiveSubscriptionInfo(subId) ?: return -1
            info.simSlotIndex
        } catch (_: Exception) { -1 }
    }

    private fun getContactDataByNumber(
        number: String
    ): Triple<String?, String?, Long?> {

        if (number.isBlank() || number == "Unknown") {
            return Triple(null, null, null)
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        return try {
            contentResolver.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx =
                            cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                        val nameIdx =
                            cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                        val photoIdx =
                            cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                        val contactId = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx)
                        val photoUri = cursor.getString(photoIdx)

                        Triple(name, photoUri, contactId)
                    } else {
                        Triple(null, null, null)
                    }
                } ?: Triple(null, null, null)
        } catch (e: Exception) {
            Triple(null, null, null)
        }
    }
}