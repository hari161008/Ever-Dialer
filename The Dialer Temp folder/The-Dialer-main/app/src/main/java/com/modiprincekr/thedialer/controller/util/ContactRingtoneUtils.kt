package com.modiprincekr.thedialer.controller.util

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract

/**
 * Reads/writes a contact's individual ringtone via ContactsContract.Contacts.CUSTOM_RINGTONE —
 * the same column Android's own Contacts app and Telecom's incoming-call ringer use, so setting
 * it here is enough for that contact to actually ring with the chosen tone; no extra playback
 * code is needed on our side. A null/blank value means "use the system default ringtone".
 */
object ContactRingtoneUtils {

    fun getCustomRingtoneUri(context: Context, contactId: String): Uri? = try {
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.CUSTOM_RINGTONE),
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val raw = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE))
                when {
                    raw == null -> null // column unset → use the system default ringtone
                    raw.isBlank() -> Uri.EMPTY // explicitly stored as "" → silent
                    else -> Uri.parse(raw)
                }
            } else null
        }
    } catch (_: Exception) {
        null
    }

    fun setCustomRingtoneUri(context: Context, contactId: String, uri: Uri?) {
        try {
            val values = ContentValues().apply {
                put(
                    ContactsContract.Contacts.CUSTOM_RINGTONE,
                    when {
                        uri == null -> null // default
                        uri == Uri.EMPTY -> "" // silent
                        else -> uri.toString()
                    }
                )
            }
            val target = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            context.contentResolver.update(target, values, null, null)
        } catch (_: Exception) {
            // Ignore — worst case the contact keeps ringing with the system default.
        }
    }

    /** Human-readable label for the ringtone picker row: the ringtone's own title, "Silent" for
     *  an explicitly-silent contact, or "Default (System Ringtone)" when nothing custom is set. */
    fun ringtoneLabel(context: Context, uri: Uri?): String {
        if (uri == null) return "Default (System Ringtone)"
        if (uri == Uri.EMPTY || uri.toString().isBlank()) return "Silent"
        return try {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Custom"
        } catch (_: Exception) {
            "Custom"
        }
    }
}
