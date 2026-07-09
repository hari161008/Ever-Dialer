/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.annotation.StringRes
import com.coolappstore.evercallrecorder.by.svhp.R
import com.coolappstore.evercallrecorder.by.svhp.data.recordings.RecordingDirection
import com.coolappstore.evercallrecorder.by.svhp.data.recordings.RecordingMetadata
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioCodec
import com.coolappstore.evercallrecorder.by.svhp.system.permissions.PermissionChecks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordingFileNameFormatter {
    const val TAG = "SCR:RecordingFileNameFormatter"
    /**
     * Represents the supported placeholders that can be used in the file name template.
     * Binds the literal tag used in formatting to a localized description for the UI.
     * @param tag The literal placeholder string that will be replaced in the template (e.g., "{date}").
     * @param descriptionResId The string resource ID for the description of this placeholder
     */
    enum class FileNamePlaceholder(val tag: String, @param:StringRes val descriptionResId: Int) {
        DATE("{date}", R.string.placeholder_date_desc),
        DIRECTION("{direction}", R.string.placeholder_direction_desc),
        PHONE_NUMBER("{phone_number}", R.string.placeholder_phone_number_desc),
        CONTACT_NAME("{contact_name}", R.string.placeholder_contact_name_desc),
        CROSS_COUNTRY("{cross_country}", R.string.placeholder_cross_country_desc),
        APP_SOURCE("{app_source}", R.string.placeholder_app_source_desc)
    }

    /**
     * Formats a filename based on the user defined string template and the recording metadata and audio codec.
     * Supported placeholders:
     * - {date}: The current date and time
     * - {direction}: The call direction (in/out)
     * - {phone_number}: The best available phone number
     * - {contact_name}: The contact name, if available
     * - {cross_country}: true/false indicating if the call is cross-country
     * - {app_source}: The originating app name (e.g. "WhatsApp"), empty for normal telephony calls
     *
     * @param context The context needed to resolve contacts and read preferences.
     * @param metadata Defines the main properties (direction, phone number, cross country).
     * @param codec The selected ScrcpyAudioCodec used to determine the file extension.
     * @param customFormat An optional custom format string to use instead of the one from preferences. Useful for testing or one-off formatting without changing user settings.
     * @return A filesystem-safe filename string.
     */
    fun formatFileName(
        context: Context,
        metadata: RecordingMetadata,
        codec: ScrcpyAudioCodec,
        customFormat: String? = null
    ): String {
        val template = customFormat ?: AppPreferences(context).getFileNameTemplate()

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss.SSSZ", Locale.CANADA).format(Date())

        val directionStr = when (metadata.direction) {
            RecordingDirection.INCOMING -> "in"
            RecordingDirection.OUTGOING -> "out"
        }

        val phoneStr = metadata.getBestNumber() ?: ""
        var contactStr = ""

        if (template.contains(FileNamePlaceholder.CONTACT_NAME.tag) && phoneStr.isNotEmpty()) {
            contactStr = getContactName(context, phoneStr) ?: ""
        }

        val crossCountryStr = metadata.isCrossCountry.toString()
        val appSourceStr = metadata.sourceApp.orEmpty()

        val baseName = template
            .replace(FileNamePlaceholder.DATE.tag, dateStr)
            .replace(FileNamePlaceholder.DIRECTION.tag, directionStr)
            .replace(FileNamePlaceholder.PHONE_NUMBER.tag, phoneStr)
            .replace(FileNamePlaceholder.CONTACT_NAME.tag, contactStr)
            .replace(FileNamePlaceholder.CROSS_COUNTRY.tag, crossCountryStr)
            .replace(FileNamePlaceholder.APP_SOURCE.tag, appSourceStr)

        // Recordings captured from a messaging app's VoIP call (see "Record calls from apps") are always
        // prefixed with the app name, regardless of the user's template, so they are easy to tell apart
        // from normal telephony recordings in file listings even if the template doesn't reference {app_source}.
        val finalName = if (metadata.sourceApp != null && !template.contains(FileNamePlaceholder.APP_SOURCE.tag)) {
            "${metadata.sourceApp}_$baseName"
        } else {
            baseName
        }

        AppLogger.v(TAG, "Formatted base filename: '$finalName' with template '$template'")
        return "$finalName${codec.containerExtension}"
    }

    /**
     * Looks up the display name for [phoneNumber], used to embed a contact name into the
     * recording's filename via {contact_name}.
     *
     * Bug fix: this previously queried PhoneLookup with the raw, unnormalized phone number and
     * trusted whatever contact it returned without checking that the match's own number
     * actually corresponds to the number being looked up. PhoneLookup's matching is loose, so
     * that could — and did — end up embedding a *different* contact's name into the recording,
     * i.e. the "misplaced contact name" bug. This mirrors the same normalize-then-verify
     * approach used for reading recordings back in HomeViewModel.resolveContactName().
     */
    private fun getContactName(context: Context, phoneNumber: String): String? {
        if (!PermissionChecks.hasContactsPermission(context)) return null

        val normalized = com.coolappstore.evercallrecorder.by.svhp.utils.PhoneNumberManager.normalisePhoneNumber(phoneNumber)
        if (normalized.isBlank()) return null

        val lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER)

        return context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null

            // Only trust the match if the returned contact's own number actually shares the
            // normalized digits with the number we looked up.
            val matchedNumber = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)) }.getOrNull()
            val matchedDigits = matchedNumber?.filter { it.isDigit() }.orEmpty()
            val queryDigits   = normalized.filter { it.isDigit() }
            val isPlausibleMatch = matchedDigits.isNotEmpty() && queryDigits.isNotEmpty() &&
                (matchedDigits.endsWith(queryDigits.takeLast(7)) || queryDigits.endsWith(matchedDigits.takeLast(7)))
            if (!isPlausibleMatch) return@use null

            val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            if (nameIndex != -1) cursor.getString(nameIndex) else null
        }
    }
}
