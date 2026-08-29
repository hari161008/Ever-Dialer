package com.coolappstore.everdialer.by.svhp.controller.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isYesterday(timestamp: Long): Boolean {
    return DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS)
}

private fun isSameYear(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

private fun getRelativeDay(timestamp: Long): String? {
    return when {
        DateUtils.isToday(timestamp) -> "Today"
        isYesterday(timestamp) -> "Yesterday"
        else -> null
    }
}

fun formatDateHeader(timestamp: Long): String {
    val relative = getRelativeDay(timestamp)
    if (relative != null) return relative

    val pattern = if (isSameYear(timestamp, System.currentTimeMillis())) "MMMM d" else "MMMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

fun formatDate(timestamp: Long, use24Hour: Boolean = false): String {
    val relative = getRelativeDay(timestamp)
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val time = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))
    return if (relative != null) "$relative, $time" else "${formatDateHeader(timestamp)}, $time"
}

/**
 * Formats just the time portion of a call log entry, respecting the
 * Settings → Appearance → "Call Time Format in call logs" preference
 * (12-hour "h:mm a" by default, or 24-hour "HH:mm" when [use24Hour] is true).
 */
fun formatTimeOnly(timestamp: Long, use24Hour: Boolean = false): String {
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))
}

fun formatDuration(durationSeconds: Long): String {
    return DateUtils.formatElapsedTime(durationSeconds)
}

/** Returns true if the device currently has 2 or more call-capable SIMs (dual/multi-SIM). */
fun hasDualSim(context: Context): Boolean {
    return try {
        val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return false
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        telecomManager.callCapablePhoneAccounts.size >= 2
    } catch (_: Exception) {
        false
    }
}

/** Resolves a telecom PhoneAccountHandle (as reported on the live Call object) to a
 *  0-based SIM slot index via SubscriptionManager, or -1 if it can't be determined. */
fun getSimSlotForAccountHandle(context: Context, accountHandle: PhoneAccountHandle?): Int {
    if (accountHandle == null) return -1
    return try {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as? android.telephony.SubscriptionManager ?: return -1
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val phoneAccount = tm?.getPhoneAccount(accountHandle)
        val subId = phoneAccount?.extras?.getInt("android.telecom.extra.SUBSCRIPTION_ID", -1)?.takeIf { it != -1 }
            ?: accountHandle.id.toIntOrNull()
        if (subId != null && subId != -1) {
            val slot = sm.getActiveSubscriptionInfo(subId)?.simSlotIndex
            if (slot != null && slot in 0..1) return slot
        }
        val activeList = sm.activeSubscriptionInfoList
        if (!activeList.isNullOrEmpty()) {
            val match = activeList.firstOrNull { sub ->
                accountHandle.id.contains(sub.subscriptionId.toString()) ||
                        (sub.iccId != null && accountHandle.id.contains(sub.iccId))
            }
            if (match != null && match.simSlotIndex in 0..1) {
                return match.simSlotIndex
            }
            if (activeList.size == 1) {
                return activeList[0].simSlotIndex
            }
        }
        -1
    } catch (_: Exception) { -1 }
}

fun makeCall(context: Context, number: String, accountHandle: PhoneAccountHandle? = null) {
    val sanitized = number.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
    if (sanitized.isEmpty()) {
        android.util.Log.w("EverDialerCall", "makeCall: empty number after sanitizing '$number', aborting")
        return
    }
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = Uri.fromParts("tel", sanitized, null)
    val extras = Bundle()
    if (accountHandle != null) {
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
        rememberLastUsedSim(context, telecomManager, accountHandle)
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
        try {
            android.util.Log.d("EverDialerCall", "placeCall uri=$uri account=$accountHandle")
            telecomManager.placeCall(uri, extras)
        } catch (e: SecurityException) {
            // placeCall() can still throw even after the permission check above — e.g. Telecom
            // enforces it can't always be reasoned about purely from PackageManager's granted
            // state (appops, per-user restrictions, or a stale/invalid accountHandle passed for
            // a SIM that's since been removed/disabled). Don't fail silently: fall back to the
            // system dialer with the number pre-filled so the user still gets *something*
            // actionable instead of a dead tap.
            android.util.Log.e("EverDialerCall", "placeCall threw SecurityException, falling back to ACTION_DIAL", e)
            val intent = Intent(Intent.ACTION_DIAL, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    } else {
        android.util.Log.w("EverDialerCall", "makeCall: CALL_PHONE not granted, falling back to ACTION_DIAL")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

/** Persists which SIM (1-based index) [accountHandle] corresponds to, so a later call can honor
 *  the "Choose the last used SIM in previous call" per-contact preference. Best-effort/no-op if
 *  the handle can't be matched to a call-capable account. */
private fun rememberLastUsedSim(context: Context, telecomManager: TelecomManager, accountHandle: PhoneAccountHandle) {
    try {
        val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return
        val idx = telecomManager.callCapablePhoneAccounts.indexOf(accountHandle)
        if (idx >= 0) {
            context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE).edit()
                .putInt(PreferenceManager.KEY_LAST_USED_SIM_GLOBAL, idx + 1)
                .apply()
        }
    } catch (_: Exception) { /* best-effort only */ }
}

/**
 * Places a call respecting the user's default SIM preference.
 * simPref: 0 = ask, 1 = SIM1 (index 0), 2 = SIM2 (index 1)
 * Returns true if a direct call was placed, false if sim picker should be shown.
 */
fun placeCallWithSimPreference(
    context: Context,
    number: String,
    simPref: Int,
    onShowSimPicker: () -> Unit
) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    if (hasPhoneState) {
        val accounts = telecomManager.callCapablePhoneAccounts
        if (accounts.size > 1) {
            when {
                simPref == 1 && accounts.isNotEmpty() -> makeCall(context, number, accounts[0])
                simPref == 2 && accounts.size >= 2 -> makeCall(context, number, accounts[1])
                else -> onShowSimPicker()
            }
        } else {
            makeCall(context, number)
        }
    } else {
        makeCall(context, number)
    }
}

/**
 * Places a call for a specific contact, honoring that contact's "Choose Sim" preference
 * (Contact Info → Choose Sim) before falling back to the app-wide default SIM setting.
 *
 * [contactSimChoice] is one of the PreferenceManager.SIM_CHOICE_* constants.
 * [recentSimSlotForContact] is the 0-based SIM slot of this contact's most recent call log
 * entry (or null/unknown), used for the "last used SIM for this contact" option.
 * [globalSimPref] is the app-wide default-SIM setting (0 = ask, 1 = SIM1, 2 = SIM2), used when
 * the contact's own choice is "According to settings".
 */
fun placeCallWithContactSimPreference(
    context: Context,
    number: String,
    contactSimChoice: String,
    globalSimPref: Int,
    recentSimSlotForContact: Int?,
    onShowSimPicker: () -> Unit
) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    if (!hasPhoneState) { makeCall(context, number); return }

    val accounts = telecomManager.callCapablePhoneAccounts
    if (accounts.size <= 1) { makeCall(context, number, accounts.firstOrNull()); return }

    when (contactSimChoice) {
        PreferenceManager.SIM_CHOICE_ASK -> onShowSimPicker()
        PreferenceManager.SIM_CHOICE_SIM1 -> makeCall(context, number, accounts[0])
        PreferenceManager.SIM_CHOICE_SIM2 -> {
            if (accounts.size >= 2) makeCall(context, number, accounts[1]) else onShowSimPicker()
        }
        PreferenceManager.SIM_CHOICE_LAST_FOR_CONTACT -> {
            val slot = recentSimSlotForContact
            if (slot != null && slot in accounts.indices) makeCall(context, number, accounts[slot])
            else onShowSimPicker()
        }
        PreferenceManager.SIM_CHOICE_LAST_IN_CALL -> {
            val lastIdx = context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
                .getInt(PreferenceManager.KEY_LAST_USED_SIM_GLOBAL, 0)
            if (lastIdx in 1..accounts.size) makeCall(context, number, accounts[lastIdx - 1])
            else onShowSimPicker()
        }
        else -> placeCallWithSimPreference(context, number, globalSimPref, onShowSimPicker) // SIM_CHOICE_SETTINGS
    }
}

/**
 * Resolves and places a call honoring a specific contact's "Choose Sim" preference (Contact
 * Info → Choose Sim), falling back to the app-wide default SIM setting when the contact has no
 * override — the same resolution Contact Info itself uses, but reusable from any screen that
 * places calls (Favorites, Recents/Call Log, Dialpad, etc.) so the per-contact choice actually
 * applies everywhere a call to that contact can be started, not just from Contact Info.
 *
 * [contactKey] must match the keying used elsewhere (contact id for a saved contact, or the raw
 * phone number for an unsaved/unknown one) so the correct stored preference is looked up.
 */
fun placeCallHonoringContactSim(
    context: Context,
    prefs: PreferenceManager,
    contactKey: String,
    number: String,
    recentSimSlotForContact: Int? = null,
    onShowSimPicker: () -> Unit
) {
    val globalSimPref = prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, prefs.getDefaultSimIndexDefault())
    val contactSimChoice = prefs.getContactSimChoice(contactKey)
    placeCallWithContactSimPreference(context, number, contactSimChoice, globalSimPref, recentSimSlotForContact, onShowSimPicker)
}

/**
 * Strips everything except digits and a leading '+' so two differently-formatted
 * representations of the same number ("+1 (555) 123-4567" vs "5551234567") can be compared.
 */
fun normalizeNumberDigits(number: String): String =
    number.filter { it.isDigit() || it == '+' }

/**
 * The shortest digit length at which a suffix match is trusted. Below this, a short/partial
 * dialed number (e.g. a 3-digit short code like "787" or "875") must NOT be allowed to match
 * a saved contact just because the contact's full number happens to start with, end with, or
 * contain those same digits — that's what previously caused call-log entries for short codes
 * to incorrectly resolve to (and redirect to) an unrelated saved contact.
 */
private const val MIN_TRUSTED_SUFFIX_MATCH_LEN = 7

/**
 * Equality check for two phone numbers that correctly handles a contact being saved with a
 * country code (e.g. "+917875551234") while the call-log/dialed number is the plain national
 * number ("7875551234"), or vice versa — WITHOUT falling into the trap of a short number
 * (a 3-digit short code) matching against a much longer number that merely contains those
 * digits somewhere in it. Used to decide whether a call-log number belongs to a saved contact.
 */
fun numbersLikelyMatch(a: String, b: String): Boolean {
    if (a.isBlank() || b.isBlank()) return false
    if (a == b) return true
    try {
        if (android.telephony.PhoneNumberUtils.compare(a, b)) return true
    } catch (_: Exception) {}
    val da = normalizeNumberDigits(a).filter { it.isDigit() }
    val db = normalizeNumberDigits(b).filter { it.isDigit() }
    if (da.isEmpty() || db.isEmpty()) return false
    if (da == db) return true
    val shorterLen = minOf(da.length, db.length)
    if (shorterLen < MIN_TRUSTED_SUFFIX_MATCH_LEN) return false
    if (da.endsWith(db) || db.endsWith(da)) return true
    val matchLen = minOf(shorterLen, 10)
    for (len in matchLen downTo MIN_TRUSTED_SUFFIX_MATCH_LEN) {
        if (da.takeLast(len) == db.takeLast(len)) return true
    }
    return false
}

fun openInContacts(context: Context, contactId: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
    }
    context.startActivity(intent)
}

fun openLink(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_VIEW,
        link.toUri())
    context.startActivity(intent)
}

fun silenceRingingCall(context: Context) {
    try {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        tm?.silenceRinger()
    } catch (_: Exception) {}
}
