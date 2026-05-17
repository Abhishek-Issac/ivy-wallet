package com.ivy.aiassistant

import com.ivy.aiassistant.domain.AiConfig
import com.ivy.aiassistant.domain.AiProtocol
import com.ivy.aiassistant.domain.AiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderDefaultsTest {

    @Test
    fun `every provider has a non-blank default base url and a protocol`() {
        AiProvider.entries.forEach { provider ->
            assertTrue(
                "provider ${provider.name} should have a non-blank base url",
                provider.defaultBaseUrl.isNotBlank(),
            )
            assertTrue(
                "provider ${provider.name} should have a protocol",
                provider.protocol in AiProtocol.entries,
            )
        }
    }

    @Test
    fun `local providers do not require api keys`() {
        assertFalse(AiProvider.OLLAMA.requiresApiKey)
        assertFalse(AiProvider.LM_STUDIO.requiresApiKey)
        assertFalse(AiProvider.CUSTOM.requiresApiKey)
    }

    @Test
    fun `default config is sane`() {
        val cfg = AiConfig.defaults(AiProvider.OPENAI)
        assertFalse(cfg.enabled)
        assertEquals(AiProvider.OPENAI, cfg.provider)
        assertEquals(AiProvider.OPENAI.defaultBaseUrl, cfg.baseUrl)
        assertEquals(AiProvider.OPENAI.defaultModel, cfg.model)
        assertTrue(cfg.streaming)
        assertEquals(AiConfig.DEFAULT_TEMPERATURE, cfg.temperature)
        assertEquals(AiConfig.DEFAULT_MAX_TOKENS, cfg.maxTokens)
        assertFalse(cfg.appActionsEnabled)
    }
}
