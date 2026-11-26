package com.modiprincekr.thedialer.modal.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
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
    val simSlot: Int = -1,
    val callIds: List<Long> = emptyList(),
    val dates: List<Long> = emptyList(),
    val isMultiCall: Boolean = false,
    val isMergedCall: Boolean = false,
    val secondNumber: String? = null,
    val secondName: String? = null,
    val secondContactId: String? = null,
    val secondPhotoUri: String? = null,
    val isWhatsApp: Boolean = false,
    val isVideo: Boolean = false,
    val sourcePackage: String? = null,
    val isAlternate: Boolean = false,
    val alternateName: String? = null,
    val alternateTag: String? = null
) {
    val count: Int get() = types.size.coerceAtLeast(1)
}