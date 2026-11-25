/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy

import android.media.MediaFormat
import android.media.MediaMuxer
import com.coolappstore.evercallrecorder.by.svhp.R
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService

/**
 * The audio codecs supported by scrcpy-server, each carrying all container metadata needed
 * by [ScrcpyAudioMuxer] and [RecordingForegroundService].
 *
 * FourCC values and MIME types mirror AudioCodec.java in scrcpy-server:
 *   https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/audio/AudioCodec.java
 *
 * @property cliKey             Value passed to scrcpy-server's `audio_codec=` argument.
 * @property codecFourCC        4-byte identifier sent in the stream header (big-endian uint32).
 * @property defaultBitRate     Recommended bit rate in bps when no preference is stored.
 * @property outputFormat       [MediaMuxer.OutputFormat] constant for the output container.
 * @property mimeType           MIME type for SAF file creation.
 * @property containerExtension Output file extension including the dot (e.g. ".ogg").
 * @property titleResId         String resource for the Settings dropdown label.
 */
enum class ScrcpyAudioCodec(
    val cliKey: String,
    val codecFourCC: Int,
    val defaultBitRate: Int,
    val outputFormat: Int,
    val mimeType: String,
    val containerExtension: String,
    val titleResId: Int
) {

    /**
     * Opus codec — preferred for call recording because it delivers intelligible voice quality
     * at very low bit rates (16 kbps).
     * Recordings are stored in an OGG container ([MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG],
     * available since Android 10 / API 29).
     *
     * FourCC: ASCII "opus" = 0x6F707573.
     */
    OPUS(
        cliKey             = "opus",
        codecFourCC        = 0x6F707573,
        defaultBitRate     = 16000,
        outputFormat       = MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG,
        mimeType           = MediaFormat.MIMETYPE_AUDIO_OPUS,
        containerExtension = ".ogg",
        titleResId         = R.string.audio_codec_opus
    ),

    /**
     * AAC codec — widely compatible, but requires higher bit rates than Opus for equivalent
     * voice quality (typically 32 kbps). Recordings are saved in an MPEG-4 (M4A) container.
     *
     * FourCC: ASCII "\0aac" = 0x00616163.
     */
    AAC(
        cliKey             = "aac",
        codecFourCC        = 0x00616163,
        defaultBitRate     = 32000,
        outputFormat       = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        mimeType           = MediaFormat.MIMETYPE_AUDIO_AAC,
        containerExtension = ".m4a",
        titleResId         = R.string.audio_codec_aac
    );

    companion object {
        /**
         * Returns the [ScrcpyAudioCodec] whose [cliKey] matches [key].
         *
         * @throws IllegalArgumentException if no matching entry is found.
         * @param key The raw codec key string stored in preferences (e.g. "opus" or "aac").
         * @return The matching [ScrcpyAudioCodec], or throws an error if no match is found.
         */
        fun fromKey(key: String): ScrcpyAudioCodec =
            entries.firstOrNull { it.cliKey == key } ?: throw IllegalArgumentException("Unknown ScrcpyAudioCodec key: $key")

        /**
         * Returns the [ScrcpyAudioCodec] whose [codecFourCC] matches [fourCC].
         *
         * @throws IllegalArgumentException if no matching entry is found.
         * @param fourCC The 4-byte codec identifier read from the scrcpy stream header.
         * @return The matching [ScrcpyAudioCodec], or throws an error if no match is found.
         */
        fun fromFourCC(fourCC: Int): ScrcpyAudioCodec =
            entries.firstOrNull { it.codecFourCC == fourCC } ?: throw IllegalArgumentException("Unknown ScrcpyAudioCodec fourCC: $fourCC")
    }
}
