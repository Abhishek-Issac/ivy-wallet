package com.ivy.aiassistant.tools

import com.ivy.aiassistant.domain.AiTool
import com.ivy.aiassistant.domain.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Function/tool-calling registry. Each [ToolExecutor] declares an [AiTool]
 * description and an [execute] implementation. The registry is wired via
 * Hilt multibindings so new tools can be added by simply contributing a
 * `@IntoSet` binding.
 *
 * Tools are gated by the `appActionsEnabled` user preference and by
 * per-execution permission callbacks.
 */
@Singleton
class AiToolRegistry @Inject constructor(
    private val executors: Set<@JvmSuppressWildcards ToolExecutor>,
) {
    fun availableTools(): List<AiTool> = executors.map { it.tool }

    suspend fun execute(name: String, argsJson: String): ToolResult {
        val executor = executors.firstOrNull { it.tool.name == name }
            ?: return ToolResult(
                toolName = name,
                outputJson = "{\"error\":\"unknown tool '$name'\"}",
                isError = true,
            )
        return runCatching { executor.execute(argsJson) }
            .getOrElse {
                ToolResult(
                    toolName = name,
                    outputJson = "{\"error\":\"${it.message?.replace("\"", "'") ?: "execution failed"}\"}",
                    isError = true,
                )
            }
    }
}

/**
 * Single AI tool capable of executing one specific in-app action.
 * Implementations should be thread-safe and idempotent where reasonable.
 */
interface ToolExecutor {
    val tool: AiTool
    suspend fun execute(argsJson: String): ToolResult
}
