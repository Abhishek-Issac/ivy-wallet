@file:Suppress("DataClassDefaultValues")

package com.ivy.aiassistant.ui.settings

import androidx.compose.runtime.Immutable
import com.ivy.aiassistant.domain.AiProvider
import com.ivy.aiassistant.domain.ModelInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AiSettingsState(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.OPENAI,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val streaming: Boolean = true,
    val systemPrompt: String = "",
    val toolCallingEnabled: Boolean = true,
    val appActionsEnabled: Boolean = false,
    val offlineModeOnly: Boolean = false,
    val availableProviders: ImmutableList<AiProvider> = persistentListOf(),
    val availableModels: ImmutableList<ModelInfo> = persistentListOf(),
    val fetchingModels: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)
