package com.ivy.aiassistant.data.remote

import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiTool
import com.ivy.aiassistant.domain.ChatMessage
import com.ivy.aiassistant.domain.ModelInfo
import com.ivy.aiassistant.domain.StreamingChunk
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over a single AI provider. Implementations are picked by
 * [AiClientFactory] based on the configured [com.ivy.aiassistant.domain.AiProtocol].
 */
interface AiClient {

    /**
     * Streams a chat completion from the provider. Implementations should
     * emit [StreamingChunk.Delta] events as they arrive and a terminal
     * [StreamingChunk.Done] or [StreamingChunk.Error] before completing.
     */
    fun streamCompletion(
        config: AiConfig,
        apiKey: String,
        history: List<ChatMessage>,
        tools: List<AiTool>,
    ): Flow<StreamingChunk>

    /**
     * Lists models offered by the provider. Returns an empty list if the
     * provider doesn't support model discovery.
     */
    suspend fun listModels(config: AiConfig, apiKey: String): List<ModelInfo>
}
