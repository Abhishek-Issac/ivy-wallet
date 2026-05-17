package com.ivy.aiassistant.domain

/**
 * Snapshot of the user-configured AI settings. Immutable so it can safely
 * cross thread/coroutine boundaries.
 */
data class AiConfig(
    val enabled: Boolean,
    val provider: AiProvider,
    val baseUrl: String,
    val model: String,
    val streaming: Boolean,
    val temperature: Float,
    val maxTokens: Int,
    val systemPrompt: String,
    val toolCallingEnabled: Boolean,
    val appActionsEnabled: Boolean,
    val offlineModeOnly: Boolean,
) {
    companion object {
        const val DEFAULT_TEMPERATURE: Float = 0.7f
        const val DEFAULT_MAX_TOKENS: Int = 1024
        const val DEFAULT_SYSTEM_PROMPT: String =
            "You are Ivy, a helpful personal-finance assistant embedded in the " +
                "Ivy Wallet Android app. Be concise, accurate, and friendly. When the " +
                "user requests an in-app action that you cannot directly perform, " +
                "describe how to do it manually."

        fun defaults(provider: AiProvider = AiProvider.OPENAI): AiConfig = AiConfig(
            enabled = false,
            provider = provider,
            baseUrl = provider.defaultBaseUrl,
            model = provider.defaultModel,
            streaming = true,
            temperature = DEFAULT_TEMPERATURE,
            maxTokens = DEFAULT_MAX_TOKENS,
            systemPrompt = DEFAULT_SYSTEM_PROMPT,
            toolCallingEnabled = true,
            appActionsEnabled = false,
            offlineModeOnly = false,
        )
    }
}
