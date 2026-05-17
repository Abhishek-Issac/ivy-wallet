package com.ivy.aiassistant.data.remote

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line

/**
 * Minimal Server-Sent Events parser sufficient for the OpenAI-style
 * `text/event-stream` chat completion responses. Emits the raw `data:` payload
 * (excluding the `[DONE]` sentinel) via [onData], and signals stream close via
 * [onDone].
 *
 * We intentionally avoid pulling in a full SSE library — both Ktor and OkHttp
 * only support SSE on JVM via additional plugins that aren't on this repo's
 * Ktor version path.
 */
internal suspend fun parseSse(
    channel: ByteReadChannel,
    onData: suspend (String) -> Unit,
    onDone: suspend () -> Unit,
) {
    val buffer = StringBuilder()
    val streamEnded = readSseLines(channel, buffer, onData, onDone)
    if (streamEnded) return
    if (buffer.isNotEmpty()) {
        val payload = buffer.toString().trimEnd()
        if (payload != DoneSentinel) onData(payload)
    }
    onDone()
}

/**
 * Reads SSE lines from [channel] into [buffer], dispatching complete events to
 * [onData]. Returns true if the stream encountered the `[DONE]` sentinel and
 * [onDone] was invoked.
 */
private suspend fun readSseLines(
    channel: ByteReadChannel,
    buffer: StringBuilder,
    onData: suspend (String) -> Unit,
    onDone: suspend () -> Unit,
): Boolean {
    while (true) {
        val line = channel.readUTF8Line() ?: return false
        if (line.isEmpty()) {
            if (flushEvent(buffer, onData, onDone)) return true
        } else if (!line.startsWith(":")) {
            appendData(line, buffer)
        }
    }
}

@Suppress("ReturnCount")
private suspend fun flushEvent(
    buffer: StringBuilder,
    onData: suspend (String) -> Unit,
    onDone: suspend () -> Unit,
): Boolean {
    if (buffer.isEmpty()) return false
    val payload = buffer.toString().trimEnd()
    buffer.clear()
    if (payload == DoneSentinel) {
        onDone()
        return true
    }
    onData(payload)
    return false
}

private fun appendData(line: String, buffer: StringBuilder) {
    if (!line.startsWith("data:")) return
    val data = line.removePrefix("data:").trimStart()
    if (buffer.isNotEmpty()) buffer.append('\n')
    buffer.append(data)
}

private const val DoneSentinel = "[DONE]"
