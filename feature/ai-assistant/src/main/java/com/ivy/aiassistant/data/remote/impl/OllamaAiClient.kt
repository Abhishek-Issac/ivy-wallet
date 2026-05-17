@file:Suppress("DataClassDefaultValues")

package com.ivy.aiassistant.data.remote.impl

import androidx.annotation.Keep
import com.ivy.aiassistant.data.remote.AiClient
import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiTool
import com.ivy.aiassistant.domain.ChatMessage
import com.ivy.aiassistant.domain.ChatRole
import com.ivy.aiassistant.domain.ModelInfo
import com.ivy.aiassistant.domain.StreamingChunk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements the Ollama /api/chat endpoint (JSON Lines streaming).
 * https://github.com/ollama/ollama/blob/main/docs/api.md
 */
@Singleton
class OllamaAiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : AiClient {

    override fun streamCompletion(
        config: AiConfig,
        apiKey: String,
        history: List<ChatMessage>,
        tools: List<AiTool>,
    ): Flow<StreamingChunk> = channelFlow {
        val request = OllamaChatRequest(
            model = config.model,
            messages = history.toWire(config.systemPrompt),
            stream = config.streaming,
            options = OllamaOptions(
                temperature = config.temperature,
                numPredict = config.maxTokens,
            ),
        )
        try {
            if (config.streaming) {
                httpClient.preparePost(buildUrl(config.baseUrl, "/api/chat")) {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(OllamaChatRequest.serializer(), request))
                }.execute { response ->
                    val channel = response.bodyAsChannel()
                    var finished = false
                    while (!finished) {
                        val line = channel.readUTF8Line()
                        if (line == null) {
                            finished = true
                        } else if (line.isNotBlank()) {
                            finished = handleOllamaLine(line) { send(it) }
                        }
                    }
                    send(StreamingChunk.Done)
                }
            } else {
                val resp: OllamaChatResponse = httpClient.post(buildUrl(config.baseUrl, "/api/chat")) {
                    contentType(ContentType.Application.Json)
                    setBody(request.copy(stream = false))
                }.body()
                resp.message?.content?.takeIf { it.isNotEmpty() }
                    ?.let { send(StreamingChunk.Delta(it)) }
                val prompt = resp.promptEvalCount ?: 0
                val completion = resp.evalCount ?: 0
                send(StreamingChunk.Usage(prompt, completion))
                send(StreamingChunk.Done)
            }
        } catch (t: Throwable) {
            send(StreamingChunk.Error(t.message ?: "Unknown error"))
        }
    }

    @Suppress("ReturnCount")
    private suspend fun handleOllamaLine(
        line: String,
        emit: suspend (StreamingChunk) -> Unit,
    ): Boolean {
        val event = runCatching {
            json.decodeFromString(OllamaChatStreamEvent.serializer(), line)
        }.getOrNull() ?: return false
        event.message?.content?.takeIf { it.isNotEmpty() }
            ?.let { emit(StreamingChunk.Delta(it)) }
        if (event.done) {
            val prompt = event.promptEvalCount ?: 0
            val completion = event.evalCount ?: 0
            emit(StreamingChunk.Usage(prompt, completion))
            return true
        }
        return false
    }

    override suspend fun listModels(config: AiConfig, apiKey: String): List<ModelInfo> {
        val resp: OllamaTagsResponse = httpClient.get(
            buildUrl(config.baseUrl, "/api/tags"),
        ).body()
        return resp.models.map { ModelInfo(id = it.name) }
    }

    private fun buildUrl(baseUrl: String, path: String): String =
        baseUrl.trimEnd('/') + path

    private fun List<ChatMessage>.toWire(systemPrompt: String): List<OllamaMessage> {
        val out = mutableListOf<OllamaMessage>()
        if (systemPrompt.isNotBlank() && none { it.role == ChatRole.SYSTEM }) {
            out += OllamaMessage(role = "system", content = systemPrompt)
        }
        forEach { m ->
            out += OllamaMessage(
                role = when (m.role) {
                    ChatRole.USER -> "user"
                    ChatRole.ASSISTANT -> "assistant"
                    ChatRole.SYSTEM -> "system"
                    ChatRole.TOOL -> "tool"
                },
                content = m.content,
            )
        }
        return out
    }

    @Keep
    @Serializable
    internal data class OllamaChatRequest(
        val model: String,
        val messages: List<OllamaMessage>,
        val stream: Boolean = true,
        val options: OllamaOptions? = null,
    )

    @Keep
    @Serializable
    internal data class OllamaMessage(
        val role: String,
        val content: String,
    )

    @Keep
    @Serializable
    internal data class OllamaOptions(
        val temperature: Float? = null,
        @SerialName("num_predict") val numPredict: Int? = null,
    )

    @Keep
    @Serializable
    internal data class OllamaChatStreamEvent(
        val model: String? = null,
        val message: OllamaMessage? = null,
        val done: Boolean = false,
        @SerialName("prompt_eval_count") val promptEvalCount: Int? = null,
        @SerialName("eval_count") val evalCount: Int? = null,
    )

    @Keep
    @Serializable
    internal data class OllamaChatResponse(
        val model: String? = null,
        val message: OllamaMessage? = null,
        val done: Boolean = true,
        @SerialName("prompt_eval_count") val promptEvalCount: Int? = null,
        @SerialName("eval_count") val evalCount: Int? = null,
    )

    @Keep
    @Serializable
    internal data class OllamaTagsResponse(
        val models: List<OllamaTagModel> = emptyList(),
    )

    @Keep
    @Serializable
    internal data class OllamaTagModel(val name: String)
}
