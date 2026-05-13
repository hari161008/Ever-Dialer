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

fun formatDate(timestamp: Long): String {
    val relative = getRelativeDay(timestamp)
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    return if (relative != null) "$relative, $time" else "${formatDateHeader(timestamp)}, $time"
}

fun formatDuration(durationSeconds: Long): String {
    return DateUtils.formatElapsedTime(durationSeconds)
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
