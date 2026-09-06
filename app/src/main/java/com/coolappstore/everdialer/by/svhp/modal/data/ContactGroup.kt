package com.coolappstore.everdialer.by.svhp.modal.data

import kotlinx.serialization.Serializable

@Serializable
data class ContactGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val contactIds: List<String> = emptyList()
)
