package com.ivy.aiassistant.data.remote.impl

import com.ivy.aiassistant.data.remote.AiClient
import com.ivy.aiassistant.data.remote.dto.OpenAiChatRequest
import com.ivy.aiassistant.data.remote.dto.OpenAiChatResponse
import com.ivy.aiassistant.data.remote.dto.OpenAiMessage
import com.ivy.aiassistant.data.remote.dto.OpenAiModelsResponse
import com.ivy.aiassistant.data.remote.dto.OpenAiToolFunction
import com.ivy.aiassistant.data.remote.dto.OpenAiToolWire
import com.ivy.aiassistant.data.remote.parseSse
import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiTool
import com.ivy.aiassistant.domain.ChatMessage
import com.ivy.aiassistant.domain.ChatRole
import com.ivy.aiassistant.domain.ModelInfo
import com.ivy.aiassistant.domain.StreamingChunk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client implementing the OpenAI Chat Completions wire format. Compatible
 * with OpenAI itself plus all providers that mirror the protocol: NVIDIA NIM,
 * OpenRouter, Groq, Together AI, LM Studio, and any custom server.
 */
@Singleton
class OpenAiCompatibleClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : AiClient {

    override fun streamCompletion(
        config: AiConfig,
        apiKey: String,
        history: List<ChatMessage>,
        tools: List<AiTool>,
    ): Flow<StreamingChunk> = channelFlow {
        val request = OpenAiChatRequest(
            model = config.model,
            messages = history.toWire(config.systemPrompt),
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stream = config.streaming,
            tools = tools.toWire().takeIf { it.isNotEmpty() && config.toolCallingEnabled },
        )
        try {
            if (config.streaming) {
                streamSse(config, apiKey, request)
            } else {
                runOneShot(config, apiKey, request)
            }
        } catch (t: Throwable) {
            send(StreamingChunk.Error(t.message ?: "Unknown error"))
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamingChunk>.streamSse(
        config: AiConfig,
        apiKey: String,
        request: OpenAiChatRequest,
    ) {
        httpClient.preparePost(buildChatUrl(config.baseUrl)) {
            headers {
                appendAuthHeader(apiKey)
                append(HttpHeaders.Accept, "text/event-stream")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(OpenAiChatRequest.serializer(), request))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            parseSse(
                channel = channel,
                onData = { payload ->
                    val element = runCatching { json.parseToJsonElement(payload) }.getOrNull()
                        ?: return@parseSse
                    val obj = (element as? JsonObject) ?: return@parseSse
                    val choices = (obj["choices"] as? kotlinx.serialization.json.JsonArray)
                        ?: return@parseSse
                    val first = choices.firstOrNull() as? JsonObject ?: return@parseSse
                    val delta = first["delta"] as? JsonObject
                    val content = (delta?.get("content") as? kotlinx.serialization.json.JsonPrimitive)
                        ?.content
                    if (!content.isNullOrEmpty()) send(StreamingChunk.Delta(content))
                    val toolCalls = delta?.get("tool_calls") as? kotlinx.serialization.json.JsonArray
                    toolCalls?.forEach { call ->
                        val callObj = call as? JsonObject ?: return@forEach
                        val function = callObj["function"] as? JsonObject ?: return@forEach
                        val name = (function["name"] as? kotlinx.serialization.json.JsonPrimitive)
                            ?.content
                        val args = (function["arguments"] as? kotlinx.serialization.json.JsonPrimitive)
                            ?.content
                        if (!name.isNullOrEmpty()) {
                            send(StreamingChunk.ToolCallRequest(name, args.orEmpty()))
                        }
                    }
                    val usage = obj["usage"] as? JsonObject
                    if (usage != null) {
                        val prompt = (usage["prompt_tokens"] as? kotlinx.serialization.json.JsonPrimitive)
                            ?.content?.toIntOrNull() ?: 0
                        val completion =
                            (usage["completion_tokens"] as? kotlinx.serialization.json.JsonPrimitive)
                                ?.content?.toIntOrNull() ?: 0
                        send(StreamingChunk.Usage(prompt, completion))
                    }
                },
                onDone = { send(StreamingChunk.Done) },
            )
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamingChunk>.runOneShot(
        config: AiConfig,
        apiKey: String,
        request: OpenAiChatRequest,
    ) {
        val response: OpenAiChatResponse = httpClient.post(buildChatUrl(config.baseUrl)) {
            headers { appendAuthHeader(apiKey) }
            contentType(ContentType.Application.Json)
            setBody(request.copy(stream = false))
        }.body()
        val text = response.choices.firstOrNull()?.message?.content
        if (!text.isNullOrEmpty()) send(StreamingChunk.Delta(text))
        response.usage?.let {
            send(StreamingChunk.Usage(it.promptTokens, it.completionTokens))
        }
        send(StreamingChunk.Done)
    }

    override suspend fun listModels(config: AiConfig, apiKey: String): List<ModelInfo> {
        val url = buildModelsUrl(config.baseUrl)
        val resp: OpenAiModelsResponse = httpClient.get(url) {
            headers { appendAuthHeader(apiKey) }
        }.body()
        return resp.data.map { ModelInfo(id = it.id, ownedBy = it.ownedBy) }
    }

    private fun io.ktor.http.HeadersBuilder.appendAuthHeader(apiKey: String) {
        if (apiKey.isNotBlank()) append(HttpHeaders.Authorization, "Bearer $apiKey")
    }

    private fun buildChatUrl(baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/chat/completions"

    private fun buildModelsUrl(baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/models"

    private fun List<ChatMessage>.toWire(systemPrompt: String): List<OpenAiMessage> {
        val out = mutableListOf<OpenAiMessage>()
        if (systemPrompt.isNotBlank() && none { it.role == ChatRole.SYSTEM }) {
            out += OpenAiMessage(role = "system", content = systemPrompt)
        }
        forEach { m ->
            out += OpenAiMessage(
                role = when (m.role) {
                    ChatRole.USER -> "user"
                    ChatRole.ASSISTANT -> "assistant"
                    ChatRole.SYSTEM -> "system"
                    ChatRole.TOOL -> "tool"
                },
                content = m.content,
                name = m.toolName,
            )
        }
        return out
    }

    private fun List<AiTool>.toWire(): List<OpenAiToolWire> = map { tool ->
        val params = runCatching { json.parseToJsonElement(tool.parametersJsonSchema) }
            .getOrElse { buildJsonObject { put("type", "object") } }
        OpenAiToolWire(
            function = OpenAiToolFunction(
                name = tool.name,
                description = tool.description,
                parameters = params,
            ),
        )
    }
}
