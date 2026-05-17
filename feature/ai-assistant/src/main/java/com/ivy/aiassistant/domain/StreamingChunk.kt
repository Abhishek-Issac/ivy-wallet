package com.ivy.aiassistant.domain

/**
 * Incremental piece of a streamed chat completion produced by an [AiClient].
 */
sealed interface StreamingChunk {
    /** Text delta appended to the current assistant message. */
    data class Delta(val text: String) : StreamingChunk

    /** A tool/function-call request emitted by the model. */
    data class ToolCallRequest(
        val toolName: String,
        val argumentsJson: String,
    ) : StreamingChunk

    /** Final usage info, emitted exactly once before completion. */
    data class Usage(val promptTokens: Int, val completionTokens: Int) : StreamingChunk

    /** Terminal event signalling the stream completed normally. */
    data object Done : StreamingChunk

    /** Terminal event signalling the stream failed. */
    data class Error(val message: String) : StreamingChunk
}
