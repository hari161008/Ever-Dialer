/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.data.recordings

import android.content.Context
import android.os.Parcelable
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger
import com.coolappstore.evercallrecorder.by.svhp.utils.PhoneNumberManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

/**
 * Carries the metadata associated with a single call that is being (or will be) recorded.
 *
 * **NOTE**: Enrichment fields are additional metadata that may not be available (null). Call [enrichMetadata] to attempt to fill them in after construction.
 *
 * @param rawPhoneNumber The raw phone number as received from the system, which may be in various formats or null.
 *                        For [sourceApp] sessions there usually is no real phone number; the caller/contact label
 *                        extracted from the app's call notification is stored here instead so it still shows up
 *                        in filenames and fallbacks.
 * @param direction The direction of the call (incoming or outgoing).
 * @param standardizedNumber **Enrichment**: An optional standardized E.164 phone number for better display and filename generation.
 * @param isCrossCountry **Enrichment**: An optional flag indicating if the call is cross-country.
 * @param isEnriched **Enrichment**: A flag indicating whether enrichment has been attempted on this metadata instance.
 * @param sourceApp Non-null when this session comes from [com.coolappstore.evercallrecorder.by.svhp.services.call.AppCallNotificationListenerService]
 *                  instead of a normal telephony call, set to the originating app's display name (e.g. "WhatsApp").
 *                  Drives the forced [com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioSource.OUTPUT]
 *                  capture in [com.coolappstore.evercallrecorder.by.svhp.services.recording.AudioRecordingEngine] and the
 *                  filename prefix in [com.coolappstore.evercallrecorder.by.svhp.utils.RecordingFileNameFormatter].
 */
@Parcelize
data class RecordingMetadata(
    val rawPhoneNumber: String?,
    val direction: RecordingDirection,
    // -- Enrichment fields --
    val standardizedNumber: String? = null,
    val isCrossCountry: Boolean = false,
    val isEnriched: Boolean = false,
    val sourceApp: String? = null
) : Parcelable {
    /**
     * Returns the best available phone number for display and filename purposes.
     * Try the standardized E.164 number first, then fall back to the raw phone number if necessary.
     */
    fun getBestNumber() = standardizedNumber ?: rawPhoneNumber

    companion object {
        const val TAG = "SCR:RecordingMetadata"

        /**
         * The key used to pass RecordingMetadata in an Intent when starting the recording service.
         */
        const val EXTRA_METADATA = "com.coolappstore.evercallrecorder.by.svhp.EXTRA_RECORDING_METADATA"

        /**
         * Attempts to enrich the provided RecordingMetadata with additional information such as standardized phone number and cross-country status.
         * @param context The context used to access phone number utilities.
         * @param base The base RecordingMetadata to enrich.
         * @return A new RecordingMetadata instance with enrichment fields filled in where possible.
         */
        suspend fun enrichMetadata(context: Context ,base: RecordingMetadata): RecordingMetadata = withContext(Dispatchers.Default) {
            if (base.isEnriched) {
                AppLogger.w(TAG, "Attempted to enrich metadata that is already marked as enriched. Returning original.")
                return@withContext base
            }

            // Null phone number and/or anonymous call
            val raw = base.rawPhoneNumber ?: return@withContext base.copy(
                isEnriched = true,
                isCrossCountry = true // We should assume it's cross-country to be safe, we cannot know from where it is coming from.
            )

            val phoneNumberManager = PhoneNumberManager.getInstance(context)
            val parsedNumber = phoneNumberManager.parsePhoneNumber(raw)

            if (parsedNumber == null) {
                return@withContext base.copy(
                    isEnriched = true,
                    isCrossCountry = true // Could not parse, we should assume it's cross-country to be safe.
                )
            }

            val standardized = phoneNumberManager.formatToE164(parsedNumber)
            val crossCountry = phoneNumberManager.isNumberFromDifferentCountry(parsedNumber)
            AppLogger.i(TAG, "Enriched metadata for number: raw='$raw', standardized='$standardized', crossCountry=$crossCountry")
            return@withContext base.copy(
                standardizedNumber = standardized,
                isCrossCountry = crossCountry,
                isEnriched = true
            )
        }
    }
}
