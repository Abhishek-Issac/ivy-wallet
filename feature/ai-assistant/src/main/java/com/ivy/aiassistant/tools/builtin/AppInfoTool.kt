package com.ivy.aiassistant.tools.builtin

import com.ivy.aiassistant.domain.AiTool
import com.ivy.aiassistant.domain.ToolResult
import com.ivy.aiassistant.tools.ToolExecutor
import javax.inject.Inject

/**
 * Trivial built-in tool that always returns the app name and the AI feature
 * scope. Acts as a sanity check that tool-calling is wired up. Real
 * app-action tools (create transaction, list categories, etc.) plug into the
 * same registry; they're intentionally stubbed for the initial release.
 */
class AppInfoTool @Inject constructor() : ToolExecutor {
    override val tool: AiTool = AiTool(
        name = "app_info",
        description = "Returns metadata about the Ivy Wallet app and the AI assistant.",
        parametersJsonSchema = """{"type":"object","properties":{},"additionalProperties":false}""",
    )

    override suspend fun execute(argsJson: String): ToolResult = ToolResult(
        toolName = tool.name,
        outputJson = buildString {
            append("{")
            append("\"appName\":\"Ivy Wallet\",")
            append("\"assistant\":\"Ivy AI\",")
            append("\"capabilities\":[\"chat\",\"streaming\",\"markdown\",\"tool_calling_scaffold\"]")
            append("}")
        },
    )
}
