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
            .replace(FileNamePlaceholder.PHONE_NUMBER.tag, sanitizeForFileName(phoneStr))
            .replace(FileNamePlaceholder.CONTACT_NAME.tag, sanitizeForFileName(contactStr))
            .replace(FileNamePlaceholder.CROSS_COUNTRY.tag, crossCountryStr)
            .replace(FileNamePlaceholder.APP_SOURCE.tag, sanitizeForFileName(appSourceStr))

        // Recordings captured from a messaging app's VoIP call (see "Record calls from apps") are always
        // prefixed with the app name, regardless of the user's template, so they are easy to tell apart
        // from normal telephony recordings in file listings even if the template doesn't reference {app_source}.
        var finalName = if (metadata.sourceApp != null && !template.contains(FileNamePlaceholder.APP_SOURCE.tag)) {
            "${sanitizeForFileName(metadata.sourceApp)}_$baseName"
        } else {
            baseName
        }

        // Bug fix: the real phone number used to only end up in the filename when the user's
        // template happened to contain {phone_number}. Templates that omit it (including the
        // app's own default, "{contact_name}_{date}_{direction}") had no way of getting the
        // number back afterwards, so the player and recordings list permanently showed
        // "Unknown" for the number — and, since the contact-photo/name lookups both key off
        // that number, the contact's photo (and sometimes even their name) silently disappeared
        // too. A short hidden suffix carrying the real number is now always appended (unless the
        // template already surfaces it), independent of what the visible template looks like, so
        // it can always be recovered when the recordings list is parsed back — without changing
        // how the filename looks to the user. See HomeViewModel.extractHiddenPhoneSuffix().
        if (phoneStr.isNotEmpty() && !template.contains(FileNamePlaceholder.PHONE_NUMBER.tag)) {
            finalName = "$finalName$HIDDEN_NUMBER_MARKER${sanitizeForFileName(phoneStr)}"
        }

        AppLogger.v(TAG, "Formatted base filename: '$finalName' with template '$template'")
        return "$finalName${codec.containerExtension}"
    }

    /**
     * Marker prefixing the hidden phone-number suffix appended to filenames whose visible
     * template doesn't already include {phone_number}. Kept short and unlikely to ever occur
     * naturally in a contact name or user-authored template. Must match
     * `HomeViewModel.HIDDEN_NUMBER_MARKER` exactly, since that's what parses it back out.
     */
    const val HIDDEN_NUMBER_MARKER = "~#~"

    /**
     * Strips characters that are illegal (or simply unsafe/confusing) in a filename on Android's
     * various storage backends (SAF documents providers, private app storage, FAT/exFAT SD cards)
     * from a single dynamic value — e.g. a contact name or app name — before it's substituted into
     * the file name template.
     *
     * Bug fix: values like contact names were previously substituted into the template completely
     * unsanitized. A contact name containing "/" (e.g. "Mom / Home"), or any of : * ? " < > |,
     * would either silently break the SAF file creation, get mangled by the storage provider into
     * something that no longer matches what the template parser expects when reading the list back,
     * or (on private storage, where a plain java.io.File is used) be interpreted as a path
     * separator and write the recording into the wrong place entirely — all of which surfaced to
     * the user as "the file name template isn't working".
     */
    private fun sanitizeForFileName(value: String): String =
        value.replace(Regex("""[/\\:*?"<>|\x00-\x1F]"""), "_").trim()



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
