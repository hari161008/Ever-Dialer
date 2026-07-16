package com.coolappstore.everdialer.by.svhp.modal.data

import kotlinx.serialization.Serializable

/**
 * A scheduled "Fake Call" — simulates an incoming call from [displayName] / [phoneNumber]
 * at [hour]:[minute]. If [days] is empty the call rings once; otherwise it repeats on the
 * given days of week (using [java.util.Calendar] values: 1 = Sunday … 7 = Saturday).
 */
@Serializable
data class FakeCallEntry(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val hour: Int,
    val minute: Int,
    val days: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** Epoch millis of the next time this fake call is scheduled to ring. */
    val triggerAt: Long = 0L,
    /** True if this entry was created via "Set Timer" (e.g. "ring in 30 seconds") rather
     *  than "Set Clock" (a fixed time of day). Timer-based entries always reschedule
     *  relative to *now* — including every time they're re-enabled after having already
     *  rung — instead of being treated as a fixed daily hour:minute alarm. Without this
     *  flag, re-enabling a "ring in 30s" entry could wait until the same clock time
     *  *tomorrow*, since [hour]/[minute] alone can't be told apart from a real clock-based
     *  entry that just happens to share the current time. */
    val isTimerBased: Boolean = false,
    /** For [isTimerBased] entries: how many milliseconds after being armed this should ring. */
    val timerDelayMillis: Long = 0L
)
