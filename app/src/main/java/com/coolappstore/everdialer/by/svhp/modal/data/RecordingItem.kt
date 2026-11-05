package com.coolappstore.everdialer.by.svhp.modal.data

import android.net.Uri
import java.util.Date

data class RecordingItem(
    val uri: Uri,
    val displayName: String,
    val phoneNumber: String,
    val contactName: String?,
    val direction: String,
    val date: Date?,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val extension: String,
    val isFavourite: Boolean = false,
    val noteText: String = "",
    val contactId: String? = null,
    val photoUri: String? = null,
    val secondPhoneNumber: String? = null,
    val secondContactName: String? = null,
    val secondPhotoUri: String? = null,
    val secondContactId: String? = null,
    val isMultiCall: Boolean = false,
    val isMergedCall: Boolean = false
)
