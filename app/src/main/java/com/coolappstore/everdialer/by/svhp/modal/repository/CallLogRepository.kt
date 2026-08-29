package com.coolappstore.everdialer.by.svhp.modal.repository

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SubscriptionManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.normalizeNumberDigits
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry

/**
 * Rebuilt from scratch to fix three separate classes of bugs that were all previously living in
 * this one class:
 *
 * 1. WRONG COUNTS ("1 missed call showing as 4"): some OEM/telecom stacks write more than one
 *    physical row into the CallLog provider for what is really a single call event (this is a
 *    known behavior on several devices, especially for missed/rejected/spam-screened calls where
 *    the telecom stack and the carrier's spam-check both log a row). The old code trusted every
 *    row in the provider as a distinct real call, so those duplicate rows got grouped together
 *    with the real call and inflated the on-screen count. This version explicitly recognizes and
 *    collapses exact-duplicate rows (same number + same type + same timestamp) before any
 *    grouping happens, so a duplicated single event can never masquerade as multiple calls.
 *
 * 2. SLOW LOADING: the old code resolved contact info number-by-number, issuing one
 *    PhoneLookup ContentResolver query per unique number, and for every number that query missed
 *    it ran a *second* query that linearly scanned the entire contacts phone table. On a call
 *    history with many unique numbers this was dozens-to-hundreds of extra IPC round trips and
 *    repeated full-table scans. This version reads the whole contacts phone table exactly once
 *    per refresh into an in-memory index (keyed by exact digits and by a last-7-digit suffix
 *    bucket) and then resolves every call log number as a plain in-memory map lookup.
 *
 * 3. MULTI-NUMBER CONTACTS: because the index above is built directly from every row of
 *    ContactsContract.CommonDataKinds.Phone (not from PhoneLookup's own fuzzy matching), every
 *    number saved against a contact - home, mobile, work, with or without a country code - is
 *    indexed independently, so a call from any one of a contact's saved numbers resolves to that
 *    contact correctly and consistently.
 */
class CallLogRepository(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val prefs: PreferenceManager
) : ICallLogRepository {

    private data class ContactMatch(
        val contactId: Long,
        val name: String?,
        val photoUri: String?,
        // The saved number's own digits, kept so a suffix-bucket candidate can be re-verified
        // with the real numbersLikelyMatch rule (bucketing only guarantees the *last 7* digits
        // match - it does not by itself guarantee the full suffix-match rule holds when the two
        // numbers have different lengths beyond that).
        val savedDigits: String
    )

    private data class RawCall(
        val number: String,
        val digits: String,
        val cachedName: String?,
        val type: Int,
        val date: Long,
        val duration: Long,
        val simSlot: Int
    )

    // The suffix length used to bucket saved numbers for fast lookup. Must match
    // numbersLikelyMatch's own trusted-suffix threshold so we never bucket (and therefore never
    // match) two numbers that numbersLikelyMatch itself wouldn't consider a match.
    private val suffixBucketLen = 7

    // Only the SIM-slot lookup is worth keeping across refreshes: PHONE_ACCOUNT_ID -> slot index
    // essentially never changes for the lifetime of the process, unlike contact data.
    private val simSlotCache = mutableMapOf<String, Int>()

    // Cache of the built "number -> contact" index (see buildContactIndex), kept across refreshes
    // and only rebuilt when the contacts table actually changes. With a large contact list (1000s
    // of contacts, each with 2-3 numbers) rebuilding this from scratch on *every* getCallLogs()
    // call - which can happen several times back-to-back right after a single call ends, on top
    // of the call-log ContentObserver and the screen's own onResume refresh - was a real source of
    // slowness that had nothing to do with the actual call log data. @Volatile since getCallLogs()
    // can be invoked from different coroutine dispatchers across refreshes.
    @Volatile private var cachedContactIndex: Pair<Map<String, ContactMatch>, Map<String, MutableList<ContactMatch>>>? = null
    @Volatile private var contactIndexDirty = true

    private val contactsObserver = object : android.database.ContentObserver(
        android.os.Handler(android.os.Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean) {
            // A contact was added/edited/deleted/renamed - the cached index above is now stale,
            // so mark it dirty and let the next getCallLogs() pass rebuild it. We don't rebuild it
            // right here because onChange can fire in rapid bursts (e.g. a contact sync) and we
            // only want to pay the rebuild cost once, right before it's actually needed.
            contactIndexDirty = true
        }
    }

    init {
        try {
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, contactsObserver
            )
        } catch (_: Exception) {}
    }

    override fun getCallLogs(): List<CallLogEntry> = try {
        getCallLogsInternal()
    } catch (_: SecurityException) {
        // READ_CALL_LOG / READ_CONTACTS not granted (e.g. right after a fresh install before
        // the user answers the permission prompt) - fail safe instead of crashing.
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun getCallLogsInternal(): List<CallLogEntry> {
        // Reuse the cached "number -> contact" index unless a contact was actually added, edited,
        // or removed since it was built. Every lookup in this pass (both for pruning and for the
        // real call log) is then a plain in-memory map lookup - no IPC at all on the common case
        // of "contacts haven't changed since last refresh".
        val existing = cachedContactIndex
        val (exactIndex, suffixIndex) = if (!contactIndexDirty && existing != null) {
            existing
        } else {
            val built = buildContactIndex()
            cachedContactIndex = built
            contactIndexDirty = false
            built
        }

        pruneAutoDeletedUnknownCalls(exactIndex, suffixIndex)

        val rawCalls = readRawCallLogRows()
        val dedupedCalls = dedupeDuplicateProviderRows(rawCalls)

        val callLogs = mutableListOf<CallLogEntry>()
        for (raw in dedupedCalls) {
            val match = resolveContact(raw.digits, exactIndex, suffixIndex)
            val displayName = match?.name ?: raw.cachedName ?: raw.number
            val isCallerIdName = match == null && raw.cachedName != null

            val lastEntry = callLogs.lastOrNull()
            // Only merge consecutive rows into one grouped entry when they're the same number
            // AND fall on the same calendar day. Without the day check, e.g. 2 calls to a
            // contact yesterday immediately followed (in the query, since there's nothing else
            // in between for that number) by 1 call to the same contact today would all collapse
            // into a single "3 calls" entry stamped with today's date.
            //
            // Bug fix: a withheld/private/blocked caller has a blank NUMBER in the provider, which
            // readRawCallLogRows() displays as the literal string "Unknown" - but two calls from
            // two DIFFERENT unidentifiable callers on the same day both show that same "Unknown"
            // string, and without this guard they'd wrongly merge into one inflated entry (e.g.
            // "3 missed calls" from "Unknown" when they were 3 unrelated private callers). Since
            // there's no real number to tell them apart, never merge blank-number rows - always
            // show each as its own entry.
            val canMerge = raw.digits.isNotEmpty() &&
                lastEntry != null && lastEntry.number == raw.number && isSameCalendarDay(lastEntry.date, raw.date)
            if (canMerge) {
                callLogs[callLogs.size - 1] = lastEntry!!.copy(types = lastEntry.types + raw.type)
            } else {
                callLogs.add(
                    CallLogEntry(
                        number = raw.number,
                        name = displayName,
                        type = raw.type,
                        date = raw.date,
                        duration = raw.duration,
                        photoUri = match?.photoUri,
                        contactId = match?.contactId?.toString(),
                        types = listOf(raw.type),
                        isCallerIdName = isCallerIdName,
                        simSlot = raw.simSlot
                    )
                )
            }
        }

        return callLogs
    }

    // ── Raw provider reads ──────────────────────────────────────────────────────

    private fun readRawCallLogRows(): List<RawCall> {
        val rows = mutableListOf<RawCall>()
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
                val phoneAccountId = if (phoneAccountIdIdx >= 0) cursor.getString(phoneAccountIdIdx) else null
                val simSlot = if (phoneAccountId.isNullOrBlank()) -1 else
                    simSlotCache.getOrPut(phoneAccountId) { getSimSlotForPhoneAccountId(phoneAccountId) }
                val type = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                var duration = cursor.getLong(durationIdx)
                if (type == CallLog.Calls.MISSED_TYPE && duration <= 0L) {
                    duration = com.coolappstore.everdialer.by.svhp.controller.util.MissedCallDurationStore.getDuration(context, number, date)
                }

                rows.add(
                    RawCall(
                        number = number,
                        digits = number.filter { it.isDigit() },
                        cachedName = cursor.getString(cachedNameIdx),
                        type = type,
                        date = date,
                        duration = duration,
                        simSlot = simSlot
                    )
                )
            }
        }
        return rows
    }

    /**
     * Collapses rows that the provider itself has logged more than once for what is really a
     * single call event. Some devices' telecom/telephony stacks (and some carriers' spam-check
     * integrations) write a second, essentially identical row for the same call - same number,
     * same type, same exact timestamp down to the millisecond. Two genuinely separate calls will
     * always differ in DATE (dialing/ringing takes real time between them), so requiring an exact
     * timestamp match to collapse rows never risks merging two real, distinct calls - it only
     * ever removes true duplicates. This is what previously caused a single real missed call to
     * be displayed as "4 missed calls".
     */
    private fun dedupeDuplicateProviderRows(rows: List<RawCall>): List<RawCall> {
        if (rows.isEmpty()) return rows
        val result = mutableListOf<RawCall>()
        for (row in rows) {
            val prev = result.lastOrNull()
            val isDuplicate = prev != null &&
                prev.digits == row.digits &&
                prev.type == row.type &&
                prev.date == row.date &&
                prev.duration == row.duration
            if (!isDuplicate) result.add(row)
        }
        return result
    }

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

    // ── Contact index (built once per refresh, from a single query) ────────────

    /**
     * Reads every saved phone number for every contact exactly once and indexes it two ways:
     *  - by exact digit string, for an instant O(1) match on the common case
     *  - by its last [suffixBucketLen] digits, bucketed, so a contact saved with a country code
     *    (or a call log number recorded without one, or vice versa) still resolves in O(1)
     *    average time instead of a full linear scan.
     * A contact with multiple saved numbers (home/mobile/work) simply produces multiple index
     * entries all pointing at the same contactId/name/photo - so a call from any of them
     * resolves correctly.
     */
    private fun buildContactIndex(): Pair<Map<String, ContactMatch>, Map<String, MutableList<ContactMatch>>> {
        val exact = HashMap<String, ContactMatch>()
        val suffix = HashMap<String, MutableList<ContactMatch>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val savedNumber = cursor.getString(numberIdx) ?: continue
                    val digits = normalizeNumberDigits(savedNumber).filter { it.isDigit() }
                    if (digits.isEmpty()) continue

                    val match = ContactMatch(
                        contactId = cursor.getLong(idIdx),
                        name = cursor.getString(nameIdx),
                        photoUri = cursor.getString(photoIdx),
                        savedDigits = digits
                    )

                    // Exact key: first contact to claim a given exact number wins - fine, since a
                    // genuine duplicate saved number across two different contacts is rare and
                    // ambiguous either way.
                    exact.putIfAbsent(digits, match)

                    if (digits.length >= suffixBucketLen) {
                        val key = digits.takeLast(suffixBucketLen)
                        suffix.getOrPut(key) { mutableListOf() }.add(match)
                    }
                }
            }
        } catch (_: Exception) {
            // READ_CONTACTS not granted, or provider unavailable - resolve nothing, fail safe.
        }

        return exact to suffix
    }

    private fun resolveContact(
        digits: String,
        exactIndex: Map<String, ContactMatch>,
        suffixIndex: Map<String, MutableList<ContactMatch>>
    ): ContactMatch? {
        if (digits.isEmpty()) return null

        exactIndex[digits]?.let { return it }

        if (digits.length < suffixBucketLen) return null
        val bucket = suffixIndex[digits.takeLast(suffixBucketLen)] ?: return null

        // Bucketing only guarantees the last 7 digits match. Re-verify each candidate with the
        // real numbersLikelyMatch rule (using the candidate's own saved digit string) before
        // accepting it, so e.g. an 8-digit number can't wrongly match a 10-digit saved number
        // that merely happens to share the same last-7-digit bucket.
        for (candidate in bucket) {
            if (numbersLikelyMatch(digits, candidate.savedDigits)) return candidate
        }
        return null
    }

    /**
     * Deletes call log entries from numbers that aren't saved contacts, once they're older than
     * the configured "Auto Delete Unknown No in call log" threshold - but only entries whose call
     * date is at/after [PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED_AT], i.e. from
     * *after* the feature was turned on. This is what guarantees existing call history is never
     * touched by turning the feature on: anything older than that timestamp is never considered
     * for deletion, no matter how old or how "unknown" it is.
     */
    private fun pruneAutoDeletedUnknownCalls(
        exactIndex: Map<String, ContactMatch>,
        suffixIndex: Map<String, MutableList<ContactMatch>>
    ) {
        if (!prefs.getBoolean(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED, false)) return
        val enabledAt = prefs.getLong(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_ENABLED_AT, 0L)
        if (enabledAt <= 0L) return // safety: never prune anything unless we know exactly when this was turned on

        val amount = prefs.getInt(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_VALUE, 1).coerceAtLeast(1)
        val unit = prefs.getString(PreferenceManager.KEY_AUTO_DELETE_UNKNOWN_CALLS_UNIT, "days") ?: "days"
        val thresholdMillis = amount.toLong() * if (unit == "hours") 3_600_000L else 86_400_000L
        val cutoff = System.currentTimeMillis() - thresholdMillis
        if (cutoff <= enabledAt) return // the threshold window hasn't elapsed yet since it was turned on

        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} < ?",
                arrayOf(enabledAt.toString(), cutoff.toString()),
                null
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIdx) ?: continue
                    val date = cursor.getLong(dateIdx)
                    val digits = number.filter { it.isDigit() }
                    val match = resolveContact(digits, exactIndex, suffixIndex)
                    if (match == null) {
                        try {
                            contentResolver.delete(
                                CallLog.Calls.CONTENT_URI,
                                "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
                                arrayOf(number, date.toString())
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
