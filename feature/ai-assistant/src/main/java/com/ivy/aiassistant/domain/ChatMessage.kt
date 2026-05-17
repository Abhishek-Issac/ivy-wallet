@file:Suppress("DataClassDefaultValues", "DataClassTypedIDs")

package com.ivy.aiassistant.domain

import java.util.UUID

/**
 * Role of a message in a chat conversation.
 */
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL,
}

/**
 * Domain representation of a single chat message. Mirrors the structure used
 * by most LLM chat-completion APIs.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val tokensIn: Int? = null,
    val tokensOut: Int? = null,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val toolName: String? = null,
)
