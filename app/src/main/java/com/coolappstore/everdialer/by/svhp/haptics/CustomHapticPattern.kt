package com.coolappstore.everdialer.by.svhp.haptics

/**
 * A recorded haptic beat: timestamp relative to recording start (ms) + amplitude 0..255.
 */
data class HapticBeat(val offsetMs: Long, val amplitude: Int)

/**
 * A user-recorded haptic sequence. Empty = not set.
 */
data class CustomHapticSequence(val beats: List<HapticBeat> = emptyList()) {
    val isEmpty: Boolean get() = beats.isEmpty()
    val durationMs: Long get() = beats.maxOfOrNull { it.offsetMs + 80L } ?: 0L
}
