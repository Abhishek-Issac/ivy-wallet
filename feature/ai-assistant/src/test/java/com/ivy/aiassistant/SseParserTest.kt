package com.ivy.aiassistant

import com.ivy.aiassistant.data.remote.parseSse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SseParserTest {

    @Test
    fun `emits data payloads and stops on done sentinel`() = runTest {
        val raw = buildString {
            append("data: {\"id\":1}\n\n")
            append(": comment that should be ignored\n")
            append("data: {\"id\":2}\n\n")
            append("data: [DONE]\n\n")
            append("data: {\"id\":3}\n\n") // should NOT be delivered
        }
        val channel = ByteReadChannel(raw.toByteArray(Charsets.UTF_8))

        val payloads = mutableListOf<String>()
        var done = false
        parseSse(
            channel = channel,
            onData = { payloads += it },
            onDone = { done = true },
        )

        assertEquals(listOf("{\"id\":1}", "{\"id\":2}"), payloads)
        assertEquals(true, done)
    }

    @Test
    fun `joins multi-line data payloads`() = runTest {
        val raw = "data: line1\ndata: line2\n\n"
        val channel = ByteReadChannel(raw.toByteArray(Charsets.UTF_8))

        val payloads = mutableListOf<String>()
        parseSse(
            channel = channel,
            onData = { payloads += it },
            onDone = {},
        )

        assertEquals(listOf("line1\nline2"), payloads)
    }
}
