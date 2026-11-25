/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.services.recording

import com.coolappstore.evercallrecorder.by.svhp.data.recordings.RecordingMetadata
/**
 * Represents the state of the recording service, including the current recording metadata and engine state.
 */
sealed class RecordingServiceState {
    abstract val metadata: RecordingMetadata?

    /**
     * Represents the state when the recording service is starting up.
     */
    data class Starting(override val metadata: RecordingMetadata) : RecordingServiceState()

    /**
     * Represents the state when the recording service is in standby mode, waiting for a call to start recording.
     * @param metadata The metadata associated with the current recording session, or null if the service is not initialized or has stopped (ended) recording.
     */
    data class Standby(override val metadata: RecordingMetadata? = null) : RecordingServiceState()

    /**
     * Represents the state when the recording service is actively recording a call. Contains the current recording engine and metadata.
     * @param engine The audio recording engine currently in use.
     * @param isPaused Indicates whether the recording is currently paused.
     * @param metadata The metadata associated with the current recording session
     */
    data class Active(
        val engine: AudioRecordingEngine,
        val isPaused: Boolean = false,
        override val metadata: RecordingMetadata
    ) : RecordingServiceState()
}
