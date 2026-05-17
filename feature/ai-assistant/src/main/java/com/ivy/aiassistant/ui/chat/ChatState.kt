@file:Suppress("DataClassDefaultValues")

package com.ivy.aiassistant.ui.chat

import androidx.compose.runtime.Immutable
import com.ivy.aiassistant.domain.ChatMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChatState(
    val aiEnabled: Boolean = false,
    val providerName: String = "",
    val modelName: String = "",
    val streaming: Boolean = true,
    val messages: ImmutableList<ChatMessage> = persistentListOf(),
    val draft: String = "",
    val sending: Boolean = false,
    val conversationTitle: String = "New chat",
    val errorBanner: String? = null,
    val totalTokens: Int = 0,
)
