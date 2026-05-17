@file:Suppress("DataClassDefaultValues")

package com.ivy.aiassistant.data.remote.impl

import androidx.annotation.Keep
import com.ivy.aiassistant.data.remote.AiClient
import com.ivy.aiassistant.data.remote.parseSse
import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiTool
import com.ivy.aiassistant.domain.ChatMessage
import com.ivy.aiassistant.domain.ChatRole
import com.ivy.aiassistant.domain.ModelInfo
import com.ivy.aiassistant.domain.StreamingChunk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements Anthropic's Messages API (https://docs.anthropic.com/en/api/messages).
 * Anthropic-compatible providers (proxies that emulate the same wire format)
 * also work here.
 */
@Singleton
class AnthropicAiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : AiClient {

    override fun streamCompletion(
        config: AiConfig,
        apiKey: String,
        history: List<ChatMessage>,
        tools: List<AiTool>,
    ): Flow<StreamingChunk> = channelFlow {
        val (system, messages) = history.toWire(config.systemPrompt)
        val request = AnthropicChatRequest(
            model = config.model,
            messages = messages,
            system = system,
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            stream = config.streaming,
        )
        try {
            if (config.streaming) {
                httpClient.preparePost(buildUrl(config.baseUrl)) {
                    headers {
                        appendAnthropicHeaders(apiKey)
                        append(HttpHeaders.Accept, "text/event-stream")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(AnthropicChatRequest.serializer(), request))
                }.execute { response ->
                    parseSse(
                        channel = response.bodyAsChannel(),
                        onData = { payload ->
                            val obj = runCatching { json.parseToJsonElement(payload) as? JsonObject }
                                .getOrNull() ?: return@parseSse
                            val type = (obj["type"] as? JsonPrimitive)?.content
                            when (type) {
                                "content_block_delta" -> {
                                    val delta = obj["delta"] as? JsonObject
                                    val text = (delta?.get("text") as? JsonPrimitive)?.content
                                    if (!text.isNullOrEmpty()) send(StreamingChunk.Delta(text))
                                }
                                "message_delta" -> {
                                    val usage = obj["usage"] as? JsonObject
                                    if (usage != null) {
                                        val input = (usage["input_tokens"] as? JsonPrimitive)
                                            ?.content?.toIntOrNull() ?: 0
                                        val output = (usage["output_tokens"] as? JsonPrimitive)
                                            ?.content?.toIntOrNull() ?: 0
                                        send(StreamingChunk.Usage(input, output))
                                    }
                                }
                                "message_stop" -> send(StreamingChunk.Done)
                                else -> Unit
                            }
                        },
                        onDone = { send(StreamingChunk.Done) },
                    )
                }
            } else {
                val resp: AnthropicChatResponse = httpClient.post(buildUrl(config.baseUrl)) {
                    headers { appendAnthropicHeaders(apiKey) }
                    contentType(ContentType.Application.Json)
                    setBody(request.copy(stream = false))
                }.body()
                val text = resp.content.firstOrNull { it.type == "text" }?.text
                if (!text.isNullOrEmpty()) send(StreamingChunk.Delta(text))
                resp.usage?.let {
                    send(StreamingChunk.Usage(it.inputTokens, it.outputTokens))
                }
                send(StreamingChunk.Done)
            }
        } catch (t: Throwable) {
            send(StreamingChunk.Error(t.message ?: "Unknown error"))
        }
    }

    override suspend fun listModels(config: AiConfig, apiKey: String): List<ModelInfo> {
        // Anthropic doesn't expose a public list-models endpoint at the moment.
        return emptyList()
    }

    private fun io.ktor.http.HeadersBuilder.appendAnthropicHeaders(apiKey: String) {
        if (apiKey.isNotBlank()) append("x-api-key", apiKey)
        append("anthropic-version", "2023-06-01")
    }

    private fun buildUrl(baseUrl: String): String = "${baseUrl.trimEnd('/')}/messages"

    private fun List<ChatMessage>.toWire(
        systemPrompt: String,
    ): Pair<String?, List<AnthropicMessage>> {
        val system = systemPrompt.takeIf { it.isNotBlank() }
        val messages = filter { it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT }
            .map {
                AnthropicMessage(
                    role = if (it.role == ChatRole.USER) "user" else "assistant",
                    content = it.content,
                )
            }
        return system to messages
    }

    @Keep
    @Serializable
    internal data class AnthropicChatRequest(
        val model: String,
        val messages: List<AnthropicMessage>,
        val system: String? = null,
        @SerialName("max_tokens") val maxTokens: Int,
        val temperature: Float? = null,
        val stream: Boolean = false,
    )

    @Keep
    @Serializable
    internal data class AnthropicMessage(
        val role: String,
        val content: String,
    )

    @Keep
    @Serializable
    internal data class AnthropicChatResponse(
        val content: List<AnthropicContentBlock> = emptyList(),
        val usage: AnthropicUsage? = null,
    )

    @Keep
    @Serializable
    internal data class AnthropicContentBlock(
        val type: String,
        val text: String? = null,
    )

    @Keep
    @Serializable
    internal data class AnthropicUsage(
        @SerialName("input_tokens") val inputTokens: Int = 0,
        @SerialName("output_tokens") val outputTokens: Int = 0,
    )
}
