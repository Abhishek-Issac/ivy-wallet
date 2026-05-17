package com.ivy.aiassistant.data.remote

import com.ivy.aiassistant.data.remote.impl.AnthropicAiClient
import com.ivy.aiassistant.data.remote.impl.OllamaAiClient
import com.ivy.aiassistant.data.remote.impl.OpenAiCompatibleClient
import com.ivy.aiassistant.domain.AiProtocol
import com.ivy.aiassistant.domain.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns the [AiClient] implementation that speaks the protocol used by a
 * given [AiProvider].
 */
@Singleton
class AiClientFactory @Inject constructor(
    private val openAi: OpenAiCompatibleClient,
    private val anthropic: AnthropicAiClient,
    private val ollama: OllamaAiClient,
) {
    fun clientFor(provider: AiProvider): AiClient = when (provider.protocol) {
        AiProtocol.OPENAI_COMPATIBLE -> openAi
        AiProtocol.ANTHROPIC -> anthropic
        AiProtocol.OLLAMA -> ollama
    }
}
