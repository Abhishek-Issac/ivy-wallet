package com.ivy.aiassistant.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Lightweight Markdown renderer that supports the subset most chat models
 * produce: headings, bold/italic, inline code, fenced code blocks, and
 * paragraphs. Avoids pulling in a heavy Markdown library for the first cut.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = renderInline(block.text),
                    style = when (block.level) {
                        HeadingLevel1 -> MaterialTheme.typography.headlineSmall
                        HeadingLevel2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                is MdBlock.Paragraph -> Text(
                    text = renderInline(block.text),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp),
                )

                is MdBlock.BulletList -> Column {
                    block.items.forEach { item ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = renderInline(item),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                is MdBlock.CodeBlock -> CodeBlock(language = block.language, code = block.code)
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String?, code: String) {
    val clipboard = LocalClipboardManager.current
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val onColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language?.takeIf { it.isNotBlank() } ?: "code",
                style = MaterialTheme.typography.labelSmall,
                color = onColor,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint = onColor,
                )
            }
        }
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = onColor,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    val codeColor: Color = MaterialTheme.colorScheme.tertiary
    val defaultColor: Color = LocalContentColor.current
    val tokens = tokenizeInline(text)
    tokens.forEach { token ->
        when (token) {
            is MdInline.Text -> withStyle(SpanStyle(color = defaultColor)) { append(token.value) }
            is MdInline.Bold -> withStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor),
            ) { append(token.value) }
            is MdInline.Italic -> withStyle(
                SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor),
            ) { append(token.value) }
            is MdInline.InlineCode -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = codeColor,
                ),
            ) { append(token.value) }
        }
    }
}

private const val HeadingLevel1 = 1
private const val HeadingLevel2 = 2
private const val HeadingLevel3 = 3

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class BulletList(val items: List<String>) : MdBlock
    data class CodeBlock(val language: String?, val code: String) : MdBlock
}

private sealed interface MdInline {
    data class Text(val value: String) : MdInline
    data class Bold(val value: String) : MdInline
    data class Italic(val value: String) : MdInline
    data class InlineCode(val value: String) : MdInline
}

private fun parseMarkdown(input: String): List<MdBlock> {
    val parser = MarkdownParser(input.split("\n"))
    return parser.parse()
}

@Suppress("CyclomaticComplexMethod")
private class MarkdownParser(private val lines: List<String>) {
    private val blocks = mutableListOf<MdBlock>()
    private val paragraph = StringBuilder()
    private val bullets = mutableListOf<String>()
    private var i = 0

    fun parse(): List<MdBlock> {
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("```") -> handleFence(line)
                line.startsWith("# ") -> handleHeading(HeadingLevel1, "# ", line)
                line.startsWith("## ") -> handleHeading(HeadingLevel2, "## ", line)
                line.startsWith("### ") -> handleHeading(HeadingLevel3, "### ", line)
                line.startsWith("- ") || line.startsWith("* ") -> handleBullet(line)
                line.isBlank() -> handleBlank()
                else -> handleParagraph(line)
            }
        }
        flushParagraph()
        flushBullets()
        return blocks
    }

    private fun handleFence(line: String) {
        flushParagraph()
        flushBullets()
        val lang = line.removePrefix("```").trim().ifEmpty { null }
        val code = StringBuilder()
        i++
        while (i < lines.size && !lines[i].startsWith("```")) {
            if (code.isNotEmpty()) code.append('\n')
            code.append(lines[i])
            i++
        }
        blocks += MdBlock.CodeBlock(lang, code.toString())
        if (i < lines.size) i++
    }

    private fun handleHeading(level: Int, prefix: String, line: String) {
        flushParagraph()
        flushBullets()
        blocks += MdBlock.Heading(level, line.removePrefix(prefix).trim())
        i++
    }

    private fun handleBullet(line: String) {
        flushParagraph()
        bullets += line.drop(2).trim()
        i++
    }

    private fun handleBlank() {
        flushParagraph()
        flushBullets()
        i++
    }

    private fun handleParagraph(line: String) {
        flushBullets()
        if (paragraph.isNotEmpty()) paragraph.append('\n')
        paragraph.append(line)
        i++
    }

    private fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MdBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }
    }

    private fun flushBullets() {
        if (bullets.isNotEmpty()) {
            blocks += MdBlock.BulletList(bullets.toList())
            bullets.clear()
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun tokenizeInline(text: String): List<MdInline> {
    val tokens = mutableListOf<MdInline>()
    val buffer = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        i = when {
            c == '`' -> handleInlineCode(text, i, buffer, tokens)
            c == '*' && i + 1 < text.length && text[i + 1] == '*' ->
                handleBold(text, i, buffer, tokens)
            c == '*' -> handleItalic(text, i, buffer, tokens)
            else -> {
                buffer.append(c)
                i + 1
            }
        }
    }
    if (buffer.isNotEmpty()) tokens += MdInline.Text(buffer.toString())
    return tokens
}

private fun flushBuffer(buffer: StringBuilder, tokens: MutableList<MdInline>) {
    if (buffer.isNotEmpty()) {
        tokens += MdInline.Text(buffer.toString())
        buffer.clear()
    }
}

private fun handleInlineCode(
    text: String,
    i: Int,
    buffer: StringBuilder,
    tokens: MutableList<MdInline>,
): Int {
    flushBuffer(buffer, tokens)
    val end = text.indexOf('`', i + 1)
    return if (end > i) {
        tokens += MdInline.InlineCode(text.substring(i + 1, end))
        end + 1
    } else {
        buffer.append(text[i])
        i + 1
    }
}

private fun handleBold(
    text: String,
    i: Int,
    buffer: StringBuilder,
    tokens: MutableList<MdInline>,
): Int {
    flushBuffer(buffer, tokens)
    val end = text.indexOf("**", i + 2)
    return if (end > i) {
        tokens += MdInline.Bold(text.substring(i + 2, end))
        end + 2
    } else {
        buffer.append(text[i])
        i + 1
    }
}

private fun handleItalic(
    text: String,
    i: Int,
    buffer: StringBuilder,
    tokens: MutableList<MdInline>,
): Int {
    flushBuffer(buffer, tokens)
    val end = text.indexOf('*', i + 1)
    return if (end > i) {
        tokens += MdInline.Italic(text.substring(i + 1, end))
        end + 1
    } else {
        buffer.append(text[i])
        i + 1
    }
}
