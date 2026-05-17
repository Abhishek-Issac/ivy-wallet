package com.ivy.aiassistant.ui.settings

import com.ivy.aiassistant.domain.AiProvider

sealed interface AiSettingsEvent {
    data class ToggleEnabled(val enabled: Boolean) : AiSettingsEvent
    data class ProviderChanged(val provider: AiProvider) : AiSettingsEvent
    data class BaseUrlChanged(val baseUrl: String) : AiSettingsEvent
    data class ApiKeyChanged(val apiKey: String) : AiSettingsEvent
    data class ModelChanged(val model: String) : AiSettingsEvent
    data class TemperatureChanged(val value: Float) : AiSettingsEvent
    data class MaxTokensChanged(val value: Int) : AiSettingsEvent
    data class StreamingToggled(val enabled: Boolean) : AiSettingsEvent
    data class SystemPromptChanged(val value: String) : AiSettingsEvent
    data class ToolCallingToggled(val enabled: Boolean) : AiSettingsEvent
    data class AppActionsToggled(val enabled: Boolean) : AiSettingsEvent
    data class OfflineModeToggled(val enabled: Boolean) : AiSettingsEvent
    data object FetchModels : AiSettingsEvent
    data object ResetAll : AiSettingsEvent
    data object DismissMessages : AiSettingsEvent
}
