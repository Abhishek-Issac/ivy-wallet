@file:Suppress("DataClassDefaultValues")

package com.ivy.aiassistant.data.remote.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Keep
@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
    val tools: List<OpenAiToolWire>? = null,
)

@Keep
@Serializable
internal data class OpenAiMessage(
    val role: String,
    val content: String? = null,
    val name: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Keep
@Serializable
internal data class OpenAiToolWire(
    val type: String = "function",
    val function: OpenAiToolFunction,
)

@Keep
@Serializable
internal data class OpenAiToolFunction(
    val name: String,
    val description: String,
    val parameters: JsonElement,
)

@Keep
@Serializable
internal data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val usage: OpenAiUsage? = null,
)

@Keep
@Serializable
internal data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiMessage? = null,
    val delta: OpenAiDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Keep
@Serializable
internal data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiDeltaToolCall>? = null,
)

@Keep
@Serializable
internal data class OpenAiDeltaToolCall(
    val index: Int = 0,
    val id: String? = null,
    val type: String? = null,
    val function: OpenAiDeltaToolCallFunction? = null,
)

@Keep
@Serializable
internal data class OpenAiDeltaToolCallFunction(
    val name: String? = null,
    val arguments: String? = null,
)

@Keep
@Serializable
internal data class OpenAiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Keep
@Serializable
internal data class OpenAiModelsResponse(
    val data: List<OpenAiModelDto> = emptyList(),
)

@Keep
@Serializable
internal data class OpenAiModelDto(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null,
)
