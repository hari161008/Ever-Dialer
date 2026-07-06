package com.coolappstore.everdialer.by.svhp.modal.data

data class CallLogEntry(
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long,
    val photoUri: String?,
    val contactId: String?,
    val types: List<Int> = emptyList(),
    val isCallerIdName: Boolean = false,
    /** 0-based SIM slot this call was made/received on, or -1 if unknown/single-SIM device. */
    val simSlot: Int = -1
) {
    val count: Int get() = types.size.coerceAtLeast(1)
}