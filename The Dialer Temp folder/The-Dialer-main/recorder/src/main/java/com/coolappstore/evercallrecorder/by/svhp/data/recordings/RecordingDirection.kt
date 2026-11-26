/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.data.recordings

import com.coolappstore.evercallrecorder.by.svhp.R

/**
 * Enum that clearly indicates whether a call is incoming or outgoing.
 *
 * @param token The string token used when sending the direction as Intent extras.
 * @param labelResId The string resource ID for the human-readable label of this direction (e.g. "Incoming" or "Outgoing").
 */
enum class RecordingDirection(val token: String, val labelResId: Int) {
    /** The call was received by the device (someone called us). */
    INCOMING("incoming", R.string.general_incoming),

    /** The call was placed from the device (we called someone). */
    OUTGOING("outgoing" , R.string.general_outgoing);

    companion object {
        /**
         * Determines the direction of a call based on the initial TelephonyManager call state.
         *
         * @param callState The mapped `TelephonyManager.CALL_STATE_*` value.
         * @return The corresponding [RecordingDirection], or null if the state doesn't imply a direction.
         */
        fun fromCallStateOrNull(callState: Int): RecordingDirection? {
            // We use the first non-IDLE state transition to determine direction.
            // RINGING obviously means it's coming in.
            // A direct transition to OFFHOOK usually implies the user initiated the call.
            return when (callState) {
                android.telephony.TelephonyManager.CALL_STATE_RINGING -> INCOMING
                android.telephony.TelephonyManager.CALL_STATE_OFFHOOK -> OUTGOING
                else -> null
            }
        }

        /**
         * Looks up a [RecordingDirection] by its [token] string.
         *
         * @param token The token string (e.g. "incoming" or "outgoing"), or null.
         * @return The matching [RecordingDirection], or null if the token is unrecognized.
         */
        fun fromToken(token: String?): RecordingDirection? =
            entries.firstOrNull { it.token == token }
    }
}
