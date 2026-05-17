package com.ivy.aiassistant.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.aiassistant.data.repository.AiSettingsRepository
import com.ivy.aiassistant.domain.AiProvider
import com.ivy.aiassistant.domain.ChatOrchestrator
import com.ivy.aiassistant.domain.ModelInfo
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val repository: AiSettingsRepository,
    private val orchestrator: ChatOrchestrator,
) : ComposeViewModel<AiSettingsState, AiSettingsEvent>() {

    private var enabled by mutableStateOf(false)
    private var provider by mutableStateOf(AiProvider.OPENAI)
    private var baseUrl by mutableStateOf("")
    private var apiKey by mutableStateOf("")
    private var model by mutableStateOf("")
    private var temperature by mutableFloatStateOf(0.7f)
    private var maxTokens by mutableIntStateOf(1024)
    private var streaming by mutableStateOf(true)
    private var systemPrompt by mutableStateOf("")
    private var toolCallingEnabled by mutableStateOf(true)
    private var appActionsEnabled by mutableStateOf(false)
    private var offlineModeOnly by mutableStateOf(false)
    private var availableModels by mutableStateOf(persistentListOf<ModelInfo>())
    private var fetchingModels by mutableStateOf(false)
    private var infoMessage by mutableStateOf<String?>(null)
    private var errorMessage by mutableStateOf<String?>(null)

    @Composable
    override fun uiState(): AiSettingsState {
        LaunchedEffect(Unit) { observeSettings() }
        return AiSettingsState(
            enabled = enabled,
            provider = provider,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            streaming = streaming,
            systemPrompt = systemPrompt,
            toolCallingEnabled = toolCallingEnabled,
            appActionsEnabled = appActionsEnabled,
            offlineModeOnly = offlineModeOnly,
            availableProviders = AiProvider.entries.toList().toPersistentList(),
            availableModels = availableModels,
            fetchingModels = fetchingModels,
            infoMessage = infoMessage,
            errorMessage = errorMessage,
        )
    }

    override fun onEvent(event: AiSettingsEvent) {
        when (event) {
            is AiSettingsEvent.ToggleEnabled -> persist { setEnabled(event.enabled) }
            is AiSettingsEvent.ProviderChanged -> applyProviderChange(event.provider)
            is AiSettingsEvent.BaseUrlChanged -> persist { setBaseUrl(event.baseUrl) }
            is AiSettingsEvent.ApiKeyChanged -> {
                apiKey = event.apiKey
                viewModelScope.launch { repository.setApiKey(provider, event.apiKey) }
            }
            is AiSettingsEvent.ModelChanged -> persist { setModel(event.model) }
            is AiSettingsEvent.TemperatureChanged -> persist { setTemperature(event.value) }
            is AiSettingsEvent.MaxTokensChanged -> persist { setMaxTokens(event.value) }
            is AiSettingsEvent.StreamingToggled -> persist { setStreaming(event.enabled) }
            is AiSettingsEvent.SystemPromptChanged -> persist { setSystemPrompt(event.value) }
            is AiSettingsEvent.ToolCallingToggled -> persist { setToolCallingEnabled(event.enabled) }
            is AiSettingsEvent.AppActionsToggled -> persist { setAppActionsEnabled(event.enabled) }
            is AiSettingsEvent.OfflineModeToggled -> persist { setOfflineModeOnly(event.enabled) }
            AiSettingsEvent.FetchModels -> fetchModels()
            AiSettingsEvent.ResetAll -> resetAll()
            AiSettingsEvent.DismissMessages -> {
                infoMessage = null
                errorMessage = null
            }
        }
    }

    private fun applyProviderChange(newProvider: AiProvider) {
        viewModelScope.launch {
            repository.setProvider(newProvider)
            apiKey = repository.getApiKey(newProvider)
            availableModels = persistentListOf()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.configFlow.collectLatest { config ->
                enabled = config.enabled
                provider = config.provider
                baseUrl = config.baseUrl
                model = config.model
                temperature = config.temperature
                maxTokens = config.maxTokens
                streaming = config.streaming
                systemPrompt = config.systemPrompt
                toolCallingEnabled = config.toolCallingEnabled
                appActionsEnabled = config.appActionsEnabled
                offlineModeOnly = config.offlineModeOnly
                apiKey = repository.getApiKey(config.provider)
            }
        }
    }

    private fun fetchModels() {
        if (fetchingModels) return
        fetchingModels = true
        errorMessage = null
        viewModelScope.launch {
            val result = orchestrator.fetchModels()
            fetchingModels = false
            result.onSuccess { list ->
                availableModels = list.toPersistentList()
                if (list.isEmpty()) {
                    infoMessage = "This provider does not expose a list-models API."
                } else {
                    infoMessage = "Fetched ${list.size} models."
                }
            }.onFailure { t ->
                errorMessage = "Failed to fetch models: ${t.message ?: "unknown"}"
            }
        }
    }

    private fun resetAll() {
        viewModelScope.launch {
            repository.resetAll()
            infoMessage = "AI settings reset."
        }
    }

    private fun persist(block: suspend AiSettingsRepository.() -> Unit) {
        viewModelScope.launch { repository.block() }
    }
}
