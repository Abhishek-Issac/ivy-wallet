package com.ivy.aiassistant.domain

/**
 * Supported AI providers. Most are OpenAI-compatible; Anthropic and Ollama
 * use their own protocols.
 */
enum class AiProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val protocol: AiProtocol,
    val requiresApiKey: Boolean,
    val supportsModelListing: Boolean,
) {
    OPENAI(
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = true,
        supportsModelListing = true,
    ),
    NVIDIA_NIM(
        displayName = "NVIDIA NIM",
        defaultBaseUrl = "https://integrate.api.nvidia.com/v1",
        defaultModel = "meta/llama-3.1-8b-instruct",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = true,
        supportsModelListing = true,
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "openrouter/auto",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = true,
        supportsModelListing = true,
    ),
    GROQ(
        displayName = "Groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.1-8b-instant",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = true,
        supportsModelListing = true,
    ),
    TOGETHER_AI(
        displayName = "Together AI",
        defaultBaseUrl = "https://api.together.xyz/v1",
        defaultModel = "meta-llama/Llama-3.1-8B-Instruct-Turbo",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = true,
        supportsModelListing = true,
    ),
    ANTHROPIC(
        displayName = "Anthropic",
        defaultBaseUrl = "https://api.anthropic.com/v1",
        defaultModel = "claude-3-5-haiku-latest",
        protocol = AiProtocol.ANTHROPIC,
        requiresApiKey = true,
        supportsModelListing = false,
    ),
    OLLAMA(
        displayName = "Ollama",
        defaultBaseUrl = "http://localhost:11434",
        defaultModel = "llama3.2",
        protocol = AiProtocol.OLLAMA,
        requiresApiKey = false,
        supportsModelListing = true,
    ),
    LM_STUDIO(
        displayName = "LM Studio",
        defaultBaseUrl = "http://localhost:1234/v1",
        defaultModel = "local-model",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = false,
        supportsModelListing = true,
    ),
    CUSTOM(
        displayName = "Custom / Self-hosted",
        defaultBaseUrl = "http://localhost:8080/v1",
        defaultModel = "",
        protocol = AiProtocol.OPENAI_COMPATIBLE,
        requiresApiKey = false,
        supportsModelListing = true,
    ),
    ;

    val isLocal: Boolean
        get() = this == OLLAMA || this == LM_STUDIO || this == CUSTOM
}

/**
 * Wire protocol used to communicate with the provider. New protocols can be
 * added without changing the rest of the chat plumbing.
 */
enum class AiProtocol {
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    OLLAMA,
}
