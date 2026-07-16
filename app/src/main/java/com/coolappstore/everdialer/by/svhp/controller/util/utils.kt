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

fun makeCall(context: Context, number: String, accountHandle: PhoneAccountHandle? = null) {
    val sanitized = number.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
    if (sanitized.isEmpty()) return
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val uri = Uri.fromParts("tel", sanitized, null)
    val extras = Bundle()
    if (accountHandle != null) {
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
        telecomManager.placeCall(uri, extras)
    } else {
        val intent = Intent(Intent.ACTION_DIAL, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

/**
 * Sends a USSD/MMI code using the officially documented
 * [android.telephony.TelephonyManager.sendUssdRequest] API (added API 26), instead of
 * dialing the code as a regular call and trying to sniff the response out of
 * undocumented Telecom connection-event extras (which is unreliable across OEMs/carriers
 * and was the reason USSD responses often never appeared).
 *
 * @param accountHandle Which SIM to send from (null = default/only SIM).
 * @param onResult Called on the main thread with (request, response text) on success.
 * @param onFailure Called on the main thread with (request, failureCode) — failureCode is
 *  either [android.telephony.TelephonyManager.USSD_RETURN_FAILURE] or
 *  [android.telephony.TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL] — if the request could
 *  not be sent at all (e.g. unsupported on this device/OS version), this is invoked
 *  immediately with a -1 code so the caller can fall back to the legacy dial-based flow.
 */
fun sendUssdCode(
    context: Context,
    code: String,
    accountHandle: PhoneAccountHandle?,
    onResult: (String, String) -> Unit,
    onFailure: (String, Int) -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        onFailure(code, -1)
        return
    }
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
        // sendUssdRequest was added in API 26 — fall back on older devices.
        onFailure(code, -1)
        return
    }
    try {
        val baseTm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            ?: return onFailure(code, -1)
        val tm = if (accountHandle != null) {
            try {
                val subId = accountHandle.id?.toIntOrNull() ?: -1
                if (subId > 0) baseTm.createForSubscriptionId(subId) else baseTm
            } catch (_: Exception) { baseTm }
        } else baseTm

        val callback = object : android.telephony.TelephonyManager.UssdResponseCallback() {
            override fun onReceiveUssdResponse(telephonyManager: android.telephony.TelephonyManager, request: String, response: CharSequence) {
                onResult(request, response.toString())
            }
            override fun onReceiveUssdResponseFailed(telephonyManager: android.telephony.TelephonyManager, request: String, failureCode: Int) {
                onFailure(request, failureCode)
            }
        }
        tm.sendUssdRequest(code, callback, android.os.Handler(android.os.Looper.getMainLooper()))
    } catch (e: Exception) {
        onFailure(code, -1)
    }
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
