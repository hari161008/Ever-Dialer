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
    val triggerAt: Long = 0L
)
