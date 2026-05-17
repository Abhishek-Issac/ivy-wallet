@file:Suppress("DataClassDefaultValues", "DataClassTypedIDs")

package com.ivy.aiassistant.domain

import java.util.UUID

/**
 * A persisted chat conversation.
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val providerName: String,
    val model: String,
    val totalTokens: Int = 0,
)
