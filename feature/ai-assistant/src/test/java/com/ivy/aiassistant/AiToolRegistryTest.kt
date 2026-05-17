package com.ivy.aiassistant

import com.ivy.aiassistant.tools.AiToolRegistry
import com.ivy.aiassistant.tools.builtin.AppInfoTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiToolRegistryTest {

    private val registry = AiToolRegistry(setOf(AppInfoTool()))

    @Test
    fun `app_info tool is registered`() {
        val tools = registry.availableTools()
        assertEquals(1, tools.size)
        assertEquals("app_info", tools.first().name)
    }

    @Test
    fun `app_info tool executes successfully`() = runTest {
        val result = registry.execute(name = "app_info", argsJson = "{}")
        assertFalse(result.isError)
        assertTrue(result.outputJson.contains("Ivy Wallet"))
    }

    @Test
    fun `unknown tool returns error result`() = runTest {
        val result = registry.execute(name = "does_not_exist", argsJson = "{}")
        assertTrue(result.isError)
        assertTrue(result.outputJson.contains("unknown tool"))
    }
}
