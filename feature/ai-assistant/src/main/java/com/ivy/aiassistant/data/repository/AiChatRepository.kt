package com.ivy.aiassistant.data.repository

import com.ivy.aiassistant.data.local.AiConversationDao
import com.ivy.aiassistant.data.local.AiConversationEntity
import com.ivy.aiassistant.data.local.AiMessageDao
import com.ivy.aiassistant.data.local.AiMessageEntity
import com.ivy.aiassistant.domain.ChatMessage
import com.ivy.aiassistant.domain.ChatRole
import com.ivy.aiassistant.domain.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists conversations + messages. Converts between Room entities and
 * the domain models the rest of the feature consumes.
 */
@Singleton
class AiChatRepository @Inject constructor(
    private val conversationDao: AiConversationDao,
    private val messageDao: AiMessageDao,
) {
    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun findConversation(id: String): Conversation? =
        conversationDao.findById(id)?.toDomain()

    suspend fun saveConversation(conversation: Conversation) {
        conversationDao.upsert(conversation.toEntity())
    }

    suspend fun touchConversation(id: String, totalTokens: Int) {
        conversationDao.touch(id, System.currentTimeMillis(), totalTokens)
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.delete(id)
    }

    suspend fun saveMessage(conversationId: String, message: ChatMessage) {
        messageDao.upsert(message.toEntity(conversationId))
    }

    suspend fun listMessages(conversationId: String): List<ChatMessage> =
        messageDao.listForConversation(conversationId).map { it.toDomain() }

    suspend fun clearMessages(conversationId: String) {
        messageDao.deleteForConversation(conversationId)
    }

    private fun AiConversationEntity.toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        createdAtEpochMs = createdAt,
        updatedAtEpochMs = updatedAt,
        providerName = providerName,
        model = model,
        totalTokens = totalTokens,
    )

    private fun Conversation.toEntity(): AiConversationEntity = AiConversationEntity(
        id = id,
        title = title,
        createdAt = createdAtEpochMs,
        updatedAt = updatedAtEpochMs,
        providerName = providerName,
        model = model,
        totalTokens = totalTokens,
    )

    private fun AiMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.ASSISTANT),
        content = content,
        createdAtEpochMs = createdAt,
        tokensIn = tokensIn,
        tokensOut = tokensOut,
        toolName = toolName,
        isError = isError,
        errorMessage = errorMessage,
    )

    private fun ChatMessage.toEntity(conversationId: String): AiMessageEntity = AiMessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        content = content,
        createdAt = createdAtEpochMs,
        tokensIn = tokensIn,
        tokensOut = tokensOut,
        toolName = toolName,
        isError = isError,
        errorMessage = errorMessage,
    )
}
