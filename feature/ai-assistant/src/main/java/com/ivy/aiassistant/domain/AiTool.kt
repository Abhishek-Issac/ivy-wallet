@file:Suppress("DataClassDefaultValues")

package com.ivy.aiassistant.domain

/**
 * Description of a callable tool exposed to the AI model. Mirrors the
 * "function" object in OpenAI's function-calling protocol.
 */
data class AiTool(
    val name: String,
    val description: String,
    val parametersJsonSchema: String,
)

/**
 * Result of executing a tool. Returned to the model as a tool-message.
 */
data class ToolResult(
    val toolName: String,
    val outputJson: String,
    val isError: Boolean = false,
)
