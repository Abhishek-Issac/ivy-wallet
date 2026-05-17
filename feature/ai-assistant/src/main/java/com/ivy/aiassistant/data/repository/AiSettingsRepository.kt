package com.ivy.aiassistant.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ivy.aiassistant.data.security.AiKeyStore
import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ivy_ai_settings",
)

/**
 * Persists non-sensitive AI configuration (provider, model, toggles, etc.)
 * to a Preferences DataStore. The API key lives in [AiKeyStore].
 */
@Singleton
class AiSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyStore: AiKeyStore,
) {
    private val dataStore: DataStore<Preferences> get() = context.aiSettingsDataStore

    val configFlow: Flow<AiConfig> = dataStore.data.map { it.toConfig() }

    suspend fun getConfig(): AiConfig {
        var snapshot: AiConfig = AiConfig.defaults()
        dataStore.data.collect {
            snapshot = it.toConfig()
            return@collect
        }
        return snapshot
    }

    suspend fun setEnabled(enabled: Boolean): Unit = update { it[ENABLED] = enabled }

    suspend fun setProvider(provider: AiProvider): Unit = update {
        it[PROVIDER] = provider.name
        if (it[BASE_URL].isNullOrBlank()) it[BASE_URL] = provider.defaultBaseUrl
        if (it[MODEL].isNullOrBlank()) it[MODEL] = provider.defaultModel
    }

    suspend fun setBaseUrl(baseUrl: String): Unit = update { it[BASE_URL] = baseUrl }

    suspend fun setModel(model: String): Unit = update { it[MODEL] = model }

    suspend fun setStreaming(enabled: Boolean): Unit = update { it[STREAMING] = enabled }

    suspend fun setTemperature(value: Float): Unit = update {
        it[TEMPERATURE] = value.coerceIn(0f, 2f)
    }

    suspend fun setMaxTokens(value: Int): Unit = update {
        it[MAX_TOKENS] = value.coerceIn(MIN_MAX_TOKENS, ABSOLUTE_MAX_TOKENS)
    }

    suspend fun setSystemPrompt(prompt: String): Unit = update { it[SYSTEM_PROMPT] = prompt }

    suspend fun setToolCallingEnabled(enabled: Boolean): Unit =
        update { it[TOOL_CALLING] = enabled }

    suspend fun setAppActionsEnabled(enabled: Boolean): Unit =
        update { it[APP_ACTIONS] = enabled }

    suspend fun setOfflineModeOnly(enabled: Boolean): Unit =
        update { it[OFFLINE_ONLY] = enabled }

    /**
     * Resets all settings & clears the encrypted key store.
     */
    suspend fun resetAll() {
        dataStore.edit { it.clear() }
        keyStore.clear()
    }

    fun getApiKey(provider: AiProvider): String = keyStore.getApiKey(provider)

    fun setApiKey(provider: AiProvider, key: String): Unit = keyStore.setApiKey(provider, key)

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun Preferences.toConfig(): AiConfig {
        val provider = runCatching { AiProvider.valueOf(this[PROVIDER] ?: "") }
            .getOrDefault(AiProvider.OPENAI)
        return AiConfig(
            enabled = this[ENABLED] ?: false,
            provider = provider,
            baseUrl = this[BASE_URL].orEmpty().ifBlank { provider.defaultBaseUrl },
            model = this[MODEL].orEmpty().ifBlank { provider.defaultModel },
            streaming = this[STREAMING] ?: true,
            temperature = this[TEMPERATURE] ?: AiConfig.DEFAULT_TEMPERATURE,
            maxTokens = this[MAX_TOKENS] ?: AiConfig.DEFAULT_MAX_TOKENS,
            systemPrompt = this[SYSTEM_PROMPT] ?: AiConfig.DEFAULT_SYSTEM_PROMPT,
            toolCallingEnabled = this[TOOL_CALLING] ?: true,
            appActionsEnabled = this[APP_ACTIONS] ?: false,
            offlineModeOnly = this[OFFLINE_ONLY] ?: false,
        )
    }

    companion object {
        private const val MIN_MAX_TOKENS = 32
        private const val ABSOLUTE_MAX_TOKENS = 32_000

        private val ENABLED = booleanPreferencesKey("ai_enabled")
        private val PROVIDER = stringPreferencesKey("ai_provider")
        private val BASE_URL = stringPreferencesKey("ai_base_url")
        private val MODEL = stringPreferencesKey("ai_model")
        private val STREAMING = booleanPreferencesKey("ai_streaming")
        private val TEMPERATURE = floatPreferencesKey("ai_temperature")
        private val MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        private val SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        private val TOOL_CALLING = booleanPreferencesKey("ai_tool_calling")
        private val APP_ACTIONS = booleanPreferencesKey("ai_app_actions")
        private val OFFLINE_ONLY = booleanPreferencesKey("ai_offline_only")
    }
}
