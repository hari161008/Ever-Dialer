/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy

import com.coolappstore.evercallrecorder.by.svhp.R

/**
 * All audio sources supported by scrcpy-server, with metadata for the Settings UI.
 *
 * Each entry maps directly to an Android [android.media.MediaRecorder.AudioSource] constant
 * used by scrcpy-server.  Source reference:
 *  https://github.com/Genymobile/scrcpy/blob/master/server/src/main/java/com/genymobile/scrcpy/audio/AudioSource.java
 *
 * Each entry carries:
 *  - [cliKey]           — the exact string passed to scrcpy-server's `audio_source=` argument.
 *  - [titleResId]       — the string resource ID for the human-readable name shown in the dropdown.
 *  - [descriptionResId] — the string resource ID for the one-line description shown below the dropdown.
 *  - [minApi]           — the minimum Android API level required for this source.
 *  - [maxApi]           — the maximum Android API level that supports this source, or null if unbounded.
 *  - [isDebugOnly]      — when `true` the entry is hidden unless the debug panel is enabled.
 *
 * The Settings screen generates its dropdown by iterating [entries] and filtering on [isDebugOnly],
 * so adding a new audio source only requires adding a new enum entry here (plus string resources).
 *
 * @property cliKey          scrcpy `audio_source` CLI key (e.g. "mic-voice-communication").
 * @property titleResId      String resource ID for the display name.
 * @property descriptionResId String resource ID for the one-line description.
 * @property minApi          Minimum Android API level required (inclusive).
 * @property maxApi          Maximum Android API level supported (inclusive), or null for no upper bound.
 * @property isDebugOnly     `true` if this source should only appear when debug mode is active.
 */
enum class ScrcpyAudioSource(
    val cliKey: String,
    val titleResId: Int,
    val descriptionResId: Int,
    val minApi: Int,
    val maxApi: Int?,
    val isDebugOnly: Boolean
) {

    /**
     * Uses the Android `VOICE_COMMUNICATION` audio source.
     * Microphone audio source tuned for voice communications such as VoIP.
     * It will for instance take advantage of echo cancellation or automatic gain control if available.
     * Should also record the other side audio.
     * Requires API 11+ (Android 3.0.x)
     */
    VOICE_COMMUNICATION(
        cliKey            = "mic-voice-communication",
        titleResId        = R.string.audio_source_voice_comm,
        descriptionResId  = R.string.audio_source_voice_comm_description,
        minApi            = 11,
        maxApi            = null,
        isDebugOnly       = false
    ),

    /**
     * VOICE_CALL, captures both sides of the call, this is the raw processed audio that is only active during a call.
     * Voice call uplink + downlink audio source. It also includes phone tones audio.
     * This permission is reserved for use by system components and requires the CAPTURE_AUDIO_OUTPUT permission.
     * Requires API 4+ (Android 1.6)
     */
    VOICE_CALL(
        cliKey            = "voice-call",
        titleResId        = R.string.audio_source_voice_call,
        descriptionResId  = R.string.audio_source_voice_call_description,
        minApi            = 4,
        maxApi            = null,
        isDebugOnly       = false
    ),

    /**
     * VOICE_CALL_UPLINK, captures only the local (microphone) side of a phone call.
     * Requires API 4+ (Android 1.6)
     */
    VOICE_CALL_UPLINK(
        cliKey            = "voice-call-uplink",
        titleResId        = R.string.audio_source_voice_call_uplink,
        descriptionResId  = R.string.audio_source_voice_call_uplink_description,
        minApi            = 4,
        maxApi            = null,
        isDebugOnly       = false
    ),

    /**
     * VOICE_CALL_DOWNLINK, captures only the remote (earpiece) side of a phone call.
     * Requires API 4+ (Android 1.6)
     */
    VOICE_CALL_DOWNLINK(
        cliKey            = "voice-call-downlink",
        titleResId        = R.string.audio_source_voice_call_downlink,
        descriptionResId  = R.string.audio_source_voice_call_downlink_description,
        minApi            = 4,
        maxApi            = null,
        isDebugOnly       = false
    ),

    /**
     * OUTPUT — captures the final mixed audio rendered to the speaker via REMOTE_SUBMIX.
     * This is the audio that would be heard through the device's speaker output.
     * Audio source for a submix of audio streams to be presented remotely.
     * Requires API 19+ (Android 4.4)
     */
    OUTPUT(
        cliKey            = "output",
        titleResId        = R.string.audio_source_output,
        descriptionResId  = R.string.audio_source_output_description,
        minApi            = 19,
        maxApi            = null,
        isDebugOnly       = true
    ),


    /**
     * PLAYBACK — captures all audio output from other apps via AudioPlaybackCapture API.
     * Respects app-level opt-out flags (`allowAudioPlaybackCapture`), so apps that have
     * opted out will be silent in the recording.
     * Requires API 29+ (Android 10).
     */
    PLAYBACK(
        cliKey            = "playback",
        titleResId        = R.string.audio_source_playback,
        descriptionResId  = R.string.audio_source_playback_description,
        minApi            = 29,
        maxApi            = null,
        isDebugOnly       = true
    ),

    /**
     * VOICE_PERFORMANCE — low-latency mic optimised for live performance or musical applications like karaoke.
     * Requires API 29+ (Android 10).
     */
    VOICE_PERFORMANCE(
        cliKey            = "voice-performance",
        titleResId        = R.string.audio_source_voice_performance,
        descriptionResId  = R.string.audio_source_voice_performance_description,
        minApi            = 29,
        maxApi            = null,
        isDebugOnly       = true
    ),

    /**
     * MIC — captures the device microphone audio.
     * Requires API 1+ (Android 1.0)
     */
    MIC(
        cliKey            = "mic",
        titleResId        = R.string.audio_source_mic,
        descriptionResId  = R.string.audio_source_mic_description,
        minApi            = 1,
        maxApi            = null,
        isDebugOnly       = true
    ),

    /**
     * MIC_UNPROCESSED — raw microphone input with no AGC, echo cancellation, or noise reduction.
     * Requires API 24+ (Android 7.0).
     */
    MIC_UNPROCESSED(
        cliKey            = "mic-unprocessed",
        titleResId        = R.string.audio_source_mic_unprocessed,
        descriptionResId  = R.string.audio_source_mic_unprocessed_description,
        minApi            = 24,
        maxApi            = null,
        isDebugOnly       = true
    ),

    /**
     * MIC_CAMCORDER — microphone tuned for video recording (optimised for camera use cases).
     * Requires API 7+ (Android 2.1.x)
     */
    MIC_CAMCORDER(
        cliKey            = "mic-camcorder",
        titleResId        = R.string.audio_source_mic_camcorder,
        descriptionResId  = R.string.audio_source_mic_camcorder_description,
        minApi            = 7,
        maxApi            = null,
        isDebugOnly       = true
    ),

    /**
     * MIC_VOICE_RECOGNITION — microphone with processing tuned for speech recognition (reduced noise).
     * Requires API 7+ (Android 2.1.x)
     */
    MIC_VOICE_RECOGNITION(
        cliKey            = "mic-voice-recognition",
        titleResId        = R.string.audio_source_mic_voice_recognition,
        descriptionResId  = R.string.audio_source_mic_voice_recognition_description,
        minApi            = 7,
        maxApi            = null,
        isDebugOnly       = true
    );

    companion object {
        /**
         * Returns the [ScrcpyAudioSource] whose [cliKey] matches [key].
         *
         * @throws IllegalArgumentException if no matching entry is found.
         * @param key The raw audio-source key stored in preferences.
         * @return The matching [ScrcpyAudioSource].
         */
        fun fromKey(key: String): ScrcpyAudioSource =
            entries.firstOrNull { it.cliKey == key } ?: throw IllegalArgumentException("Unknown ScrcpyAudioSource key: $key")
    }
}
