package com.ivy.aiassistant.domain

import com.ivy.aiassistant.data.remote.AiClientFactory
import com.ivy.aiassistant.data.repository.AiChatRepository
import com.ivy.aiassistant.data.repository.AiSettingsRepository
import com.ivy.aiassistant.tools.AiToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates a single user → assistant turn:
 *   1. Persists the user's message.
 *   2. Streams the model's response, forwarding chunks to the caller.
 *   3. Persists the final assistant message and updates conversation totals.
 *
 * The flow returned by [sendMessage] does NOT swallow [StreamingChunk.Error] —
 * callers should render an error bubble when they see one.
 */
@Singleton
class ChatOrchestrator @Inject constructor(
    private val settings: AiSettingsRepository,
    private val chat: AiChatRepository,
    private val clientFactory: AiClientFactory,
    private val toolRegistry: AiToolRegistry,
) {
    fun sendMessage(
        conversationId: String,
        userMessage: ChatMessage,
    ): Flow<StreamingChunk> = flow {
        val config = currentConfig()
        if (!config.enabled) {
            emit(StreamingChunk.Error("AI is disabled in settings."))
            return@flow
        }
        if (config.offlineModeOnly && !config.provider.isLocal) {
            emit(
                StreamingChunk.Error(
                    "Offline-only mode is on. " +
                        "Switch to a local provider like Ollama or LM Studio.",
                ),
            )
            return@flow
        }
        if (config.provider.requiresApiKey && settings.getApiKey(config.provider).isBlank()) {
            emit(StreamingChunk.Error("API key required for ${config.provider.displayName}."))
            return@flow
        }

        chat.saveMessage(conversationId, userMessage)
        val history = chat.listMessages(conversationId)

        val client = clientFactory.clientFor(config.provider)
        val apiKey = settings.getApiKey(config.provider)
        val tools = if (config.toolCallingEnabled && config.appActionsEnabled) {
            toolRegistry.availableTools()
        } else {
            emptyList()
        }

        val assistantBuffer = StringBuilder()
        var promptTokens = 0
        var completionTokens = 0
        var sawError = false

        client.streamCompletion(config, apiKey, history, tools).collect { chunk ->
            when (chunk) {
                is StreamingChunk.Delta -> assistantBuffer.append(chunk.text)
                is StreamingChunk.Usage -> {
                    promptTokens = chunk.promptTokens
                    completionTokens = chunk.completionTokens
                }
                is StreamingChunk.ToolCallRequest -> Unit // Surfaced upstream; execution stubbed.
                is StreamingChunk.Done -> Unit
                is StreamingChunk.Error -> sawError = true
            }
            emit(chunk)
        }

        if (assistantBuffer.isNotEmpty()) {
            val assistantMessage = ChatMessage(
                role = ChatRole.ASSISTANT,
                content = assistantBuffer.toString(),
                tokensIn = promptTokens.takeIf { it > 0 },
                tokensOut = completionTokens.takeIf { it > 0 },
                isError = sawError,
            )
            chat.saveMessage(conversationId, assistantMessage)
            chat.touchConversation(
                id = conversationId,
                tokensDelta = promptTokens + completionTokens,
            )
        }
    }

    suspend fun fetchModels(): Result<List<ModelInfo>> = runCatching {
        val config = currentConfig()
        if (!config.provider.supportsModelListing) return@runCatching emptyList()
        val apiKey = settings.getApiKey(config.provider)
        clientFactory.clientFor(config.provider).listModels(config, apiKey)
    }

    private suspend fun currentConfig(): AiConfig = settings.getConfig()
}
