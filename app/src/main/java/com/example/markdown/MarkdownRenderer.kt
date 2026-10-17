package com.example.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighlightGold
import com.example.ui.theme.SaffronPrimary

@Composable
fun MarkdownRenderer(
    text: String,
    settings: MarkdownSettings,
    baseTextStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    if (text.isEmpty()) {
        SelectionContainer {
            Text(
                text = "Empty note",
                modifier = modifier,
                style = baseTextStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
        return
    }

    if (!settings.markdownEnabled) {
        SelectionContainer {
            Text(
                text = text,
                modifier = modifier,
                style = baseTextStyle
            )
        }
        return
    }

    val blocks = remember(text, settings) {
        parseMarkdownBlocks(text, settings)
    }

    val fontScale = settings.readingFontScale.coerceIn(0.5f, 3.0f)
    val scaledFontSize = (baseTextStyle.fontSize.value * fontScale).sp
    val scaledLineHeight = (baseTextStyle.lineHeight.value * fontScale).sp
    val effectiveBaseStyle = baseTextStyle.copy(
        fontSize = scaledFontSize,
        lineHeight = scaledLineHeight
    )

    val linkColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val highlightBgColor = HighlightGold.copy(alpha = 0.35f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val headingStyle = getHeadingStyle(
                            level = block.level,
                            baseStyle = effectiveBaseStyle,
                            fontScale = fontScale,
                            themeTextColor = MaterialTheme.colorScheme.onBackground
                        )
                        val annotated = remember(block.content, settings) {
                            renderInlineMarkdown(
                                input = block.content,
                                settings = settings,
                                linkColor = linkColor,
                                codeBgColor = codeBgColor,
                                highlightBgColor = highlightBgColor,
                                baseFontSize = headingStyle.fontSize.value
                            )
                        }
                        Text(
                            text = annotated,
                            style = headingStyle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is MarkdownBlock.HorizontalRule -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = borderColor,
                            thickness = 1.dp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    is MarkdownBlock.CodeBlock -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (block.language.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = block.language,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = (11 * fontScale).sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                    HorizontalDivider(color = borderColor)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = block.code,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = (13.5f * fontScale).sp,
                                            lineHeight = (19 * fontScale).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.Blockquote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp) // minimal bar height, adapts with row content
                                    .background(SaffronPrimary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                block.lines.forEach { line ->
                                    val annotated = remember(line, settings) {
                                        renderInlineMarkdown(
                                            input = line,
                                            settings = settings,
                                            linkColor = linkColor,
                                            codeBgColor = codeBgColor,
                                            highlightBgColor = highlightBgColor,
                                            baseFontSize = effectiveBaseStyle.fontSize.value
                                        )
                                    }
                                    Text(
                                        text = annotated,
                                        style = effectiveBaseStyle.copy(
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.TaskListBlock -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            block.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = (item.indent * 18).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.isChecked) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                        contentDescription = if (item.isChecked) "Completed" else "Incomplete",
                                        tint = if (item.isChecked) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size((18 * fontScale).dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val annotated = remember(item.content, settings) {
                                        renderInlineMarkdown(
                                            input = item.content,
                                            settings = settings,
                                            linkColor = linkColor,
                                            codeBgColor = codeBgColor,
                                            highlightBgColor = highlightBgColor,
                                            baseFontSize = effectiveBaseStyle.fontSize.value
                                        )
                                    }
                                    Text(
                                        text = annotated,
                                        style = effectiveBaseStyle.copy(
                                            color = if (item.isChecked) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            } else {
                                                MaterialTheme.colorScheme.onBackground
                                            },
                                            textDecoration = if (item.isChecked && settings.strikethrough) {
                                                TextDecoration.LineThrough
                                            } else null
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.ListBlock -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            block.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = (item.indent * 18).dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = if (item.isOrdered) "${item.bullet} " else "• ",
                                        style = effectiveBaseStyle.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isOrdered) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.widthIn(min = (16 * fontScale).dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val annotated = remember(item.content, settings) {
                                        renderInlineMarkdown(
                                            input = item.content,
                                            settings = settings,
                                            linkColor = linkColor,
                                            codeBgColor = codeBgColor,
                                            highlightBgColor = highlightBgColor,
                                            baseFontSize = effectiveBaseStyle.fontSize.value
                                        )
                                    }
                                    Text(
                                        text = annotated,
                                        style = effectiveBaseStyle,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.TableBlock -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                            ) {
                                // Header row
                                Row(
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                    )
                                ) {
                                    block.headers.forEachIndexed { colIdx, headerCell ->
                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 90.dp)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            val annotated = remember(headerCell, settings) {
                                                renderInlineMarkdown(
                                                    input = headerCell,
                                                    settings = settings,
                                                    linkColor = linkColor,
                                                    codeBgColor = codeBgColor,
                                                    highlightBgColor = highlightBgColor,
                                                    baseFontSize = effectiveBaseStyle.fontSize.value
                                                )
                                            }
                                            Text(
                                                text = annotated,
                                                style = effectiveBaseStyle.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = borderColor)
                                // Data rows
                                block.rows.forEachIndexed { rowIdx, rowCells ->
                                    val rowBg = if (rowIdx % 2 == 1) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    } else {
                                        Color.Transparent
                                    }
                                    Row(modifier = Modifier.background(rowBg)) {
                                        rowCells.forEachIndexed { colIdx, cellText ->
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(min = 90.dp)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                val annotated = remember(cellText, settings) {
                                                    renderInlineMarkdown(
                                                        input = cellText,
                                                        settings = settings,
                                                        linkColor = linkColor,
                                                        codeBgColor = codeBgColor,
                                                        highlightBgColor = highlightBgColor,
                                                        baseFontSize = effectiveBaseStyle.fontSize.value
                                                    )
                                                }
                                                Text(
                                                    text = annotated,
                                                    style = effectiveBaseStyle
                                                )
                                            }
                                        }
                                    }
                                    if (rowIdx < block.rows.size - 1) {
                                        HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    is MarkdownBlock.Paragraph -> {
                        val annotated = remember(block.content, settings) {
                            renderInlineMarkdown(
                                input = block.content,
                                settings = settings,
                                linkColor = linkColor,
                                codeBgColor = codeBgColor,
                                highlightBgColor = highlightBgColor,
                                baseFontSize = effectiveBaseStyle.fontSize.value
                            )
                        }
                        Text(
                            text = annotated,
                            style = effectiveBaseStyle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun getHeadingStyle(
    level: Int,
    baseStyle: TextStyle,
    fontScale: Float,
    themeTextColor: Color
): TextStyle {
    val multiplier = when (level) {
        1 -> 1.70f
        2 -> 1.45f
        3 -> 1.25f
        4 -> 1.10f
        5 -> 1.00f
        else -> 0.90f
    }
    val weight = when (level) {
        1, 2 -> FontWeight.Bold
        3, 4 -> FontWeight.SemiBold
        5 -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    val targetSize = (baseStyle.fontSize.value * fontScale * multiplier).sp
    val targetLineHeight = (targetSize.value * 1.35f).sp
    return baseStyle.copy(
        fontSize = targetSize,
        lineHeight = targetLineHeight,
        fontWeight = weight,
        color = themeTextColor
    )
}

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val content: String, val rawText: String) : MarkdownBlock
    data class HorizontalRule(val rawText: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String, val rawText: String) : MarkdownBlock
    data class Blockquote(val lines: List<String>, val rawText: String) : MarkdownBlock
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>, val rawText: String) : MarkdownBlock
    data class TaskListBlock(val items: List<TaskItem>) : MarkdownBlock
    data class ListBlock(val items: List<ListItem>) : MarkdownBlock
    data class Paragraph(val content: String, val rawText: String) : MarkdownBlock
}

data class TaskItem(val indent: Int, val isChecked: Boolean, val content: String)
data class ListItem(val indent: Int, val bullet: String, val isOrdered: Boolean, val content: String)

fun parseMarkdownBlocks(text: String, settings: MarkdownSettings): List<MarkdownBlock> {
    val lines = text.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val currentLine = lines[i]
        val trimmed = currentLine.trim()

        // 1. Fenced Code Block
        if (settings.codeBlocks && trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            val rawBuilder = StringBuilder().append(currentLine).append("\n")
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                rawBuilder.append(lines[i]).append("\n")
                i++
            }
            if (i < lines.size) {
                rawBuilder.append(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n"), rawBuilder.toString()))
            continue
        }

        // 2. Pipe Table
        if (settings.tables && trimmed.contains("|") && i + 1 < lines.size) {
            val nextTrimmed = lines[i + 1].trim()
            if (isTableSeparatorLine(nextTrimmed)) {
                val headers = parseTableRow(trimmed)
                val rows = mutableListOf<List<String>>()
                val rawBuilder = StringBuilder().append(currentLine).append("\n").append(lines[i + 1]).append("\n")
                i += 2
                while (i < lines.size && lines[i].trim().contains("|")) {
                    rows.add(parseTableRow(lines[i].trim()))
                    rawBuilder.append(lines[i]).append("\n")
                    i++
                }
                blocks.add(MarkdownBlock.TableBlock(headers, rows, rawBuilder.toString()))
                continue
            }
        }

        // 3. Blockquote
        if (settings.blockquotes && trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            val rawBuilder = StringBuilder()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                val stripped = lines[i].trim().removePrefix(">").removePrefix(" ")
                quoteLines.add(stripped)
                rawBuilder.append(lines[i]).append("\n")
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines, rawBuilder.toString()))
            continue
        }

        // 4. Horizontal Rule
        if (settings.horizontalRule && isHorizontalRule(trimmed)) {
            blocks.add(MarkdownBlock.HorizontalRule(currentLine))
            i++
            continue
        }

        // 5. Headings: # to ######
        if (settings.headings) {
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmed)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val content = headingMatch.groupValues[2]
                blocks.add(MarkdownBlock.Heading(level, content, currentLine))
                i++
                continue
            }
        }

        // 6. Task List items (- [ ] / - [x])
        if (settings.taskLists && isTaskListItem(currentLine)) {
            val taskItems = mutableListOf<TaskItem>()
            while (i < lines.size && isTaskListItem(lines[i])) {
                val match = Regex("^(\\s*)([-*+])\\s+\\[([ xX])\\]\\s*(.*)$").find(lines[i])
                if (match != null) {
                    val indent = match.groupValues[1].length / 2
                    val isChecked = match.groupValues[3].lowercase() == "x"
                    val content = match.groupValues[4]
                    taskItems.add(TaskItem(indent, isChecked, content))
                }
                i++
            }
            blocks.add(MarkdownBlock.TaskListBlock(taskItems))
            continue
        }

        // 7. Bullet or Ordered List items
        if (settings.lists && isRegularListItem(currentLine)) {
            val listItems = mutableListOf<ListItem>()
            while (i < lines.size && isRegularListItem(lines[i])) {
                val match = Regex("^(\\s*)([-*+]|\\d+\\.)\\s+(.*)$").find(lines[i])
                if (match != null) {
                    val indent = match.groupValues[1].length / 2
                    val bullet = match.groupValues[2]
                    val isOrdered = bullet.endsWith(".")
                    val content = match.groupValues[3]
                    listItems.add(ListItem(indent, bullet, isOrdered, content))
                }
                i++
            }
            blocks.add(MarkdownBlock.ListBlock(listItems))
            continue
        }

        // 8. Regular paragraph / text line
        blocks.add(MarkdownBlock.Paragraph(currentLine, currentLine))
        i++
    }

    return blocks
}

private fun isTableSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains("|")) return false
    val parts = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return false
    return parts.all { it.matches(Regex("^:?-+:?$")) }
}

private fun parseTableRow(line: String): List<String> {
    var s = line.trim()
    if (s.startsWith("|")) s = s.substring(1)
    if (s.endsWith("|")) s = s.substring(0, s.length - 1)
    return s.split("|").map { it.trim() }
}

private fun isHorizontalRule(line: String): Boolean {
    val clean = line.replace(" ", "")
    return clean == "---" || clean == "***" || clean == "___" ||
            clean.matches(Regex("^-{3,}$")) || clean.matches(Regex("^\\*{3,}$")) || clean.matches(Regex("^_{3,}$"))
}

private fun isTaskListItem(line: String): Boolean {
    return line.matches(Regex("^\\s*[-*+]\\s+\\[[ xX]\\]\\s*.*$"))
}

private fun isRegularListItem(line: String): Boolean {
    return line.matches(Regex("^\\s*([*+-]|\\d+\\.)\\s+.*$"))
}

/**
 * Parses and formats inline Markdown tokens into an AnnotatedString:
 * - Backslash escapes
 * - Inline code `code`
 * - Links [text](url) and bare URLs
 * - Highlight ==text==
 * - Strikethrough ~~text~~
 * - Bold & Italic: ***both***, **bold**, *italic*
 */
fun renderInlineMarkdown(
    input: String,
    settings: MarkdownSettings,
    linkColor: Color,
    codeBgColor: Color,
    highlightBgColor: Color,
    baseFontSize: Float
): AnnotatedString {
    if (input.isEmpty()) return AnnotatedString("")

    // 1. Tokenize Backslash Escapes (e.g. \* -> \uE001)
    val escapeMap = mapOf(
        "\\\\" to "\uE000",
        "\\*" to "\uE001",
        "\\_" to "\uE002",
        "\\~" to "\uE003",
        "\\=" to "\uE004",
        "\\`" to "\uE005",
        "\\#" to "\uE006",
        "\\[" to "\uE007",
        "\\]" to "\uE008",
        "\\>" to "\uE009",
        "\\-" to "\uE00A"
    )
    val reverseEscapeMap = mapOf(
        "\uE000" to "\\",
        "\uE001" to "*",
        "\uE002" to "_",
        "\uE003" to "~",
        "\uE004" to "=",
        "\uE005" to "`",
        "\uE006" to "#",
        "\uE007" to "[",
        "\uE008" to "]",
        "\uE009" to ">",
        "\uE00A" to "-"
    )

    var processed = input
    escapeMap.forEach { (esc, placeholder) ->
        processed = processed.replace(esc, placeholder)
    }

    // Helper to unescape placeholders
    fun unescape(str: String): String {
        var res = str
        reverseEscapeMap.forEach { (placeholder, original) ->
            res = res.replace(placeholder, original)
        }
        return res
    }

    return buildAnnotatedString {
        // Parse segments recursively or sequentially
        parseInlineRecursive(
            text = processed,
            settings = settings,
            linkColor = linkColor,
            codeBgColor = codeBgColor,
            highlightBgColor = highlightBgColor,
            baseFontSize = baseFontSize,
            unescape = ::unescape
        )
    }
}

private fun AnnotatedString.Builder.parseInlineRecursive(
    text: String,
    settings: MarkdownSettings,
    linkColor: Color,
    codeBgColor: Color,
    highlightBgColor: Color,
    baseFontSize: Float,
    unescape: (String) -> String
) {
    if (text.isEmpty()) return

    // Find the earliest matching token
    data class TokenMatch(
        val type: String,
        val start: Int,
        val end: Int,
        val inner1: String,
        val inner2: String = ""
    )

    var earliest: TokenMatch? = null

    fun checkMatch(type: String, regex: Regex, extractor: (MatchResult) -> Pair<String, String>) {
        val m = regex.find(text) ?: return
        val start = m.range.first
        val end = m.range.last + 1
        if (earliest == null || start < earliest!!.start) {
            val (in1, in2) = extractor(m)
            earliest = TokenMatch(type, start, end, in1, in2)
        }
    }

    // Inline Code: `code`
    if (settings.inlineCode) {
        checkMatch("CODE", Regex("`([^`]+)`")) { m -> Pair(m.groupValues[1], "") }
    }

    // Links: [text](url)
    if (settings.links) {
        checkMatch("MD_LINK", Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")) { m ->
            Pair(m.groupValues[1], m.groupValues[2])
        }
        // Bare URL: https://... or http://...
        checkMatch("BARE_URL", Regex("(?<!\\()(https?://[a-zA-Z0-9_./?=&%#+-]+)")) { m ->
            Pair(m.groupValues[1], m.groupValues[1])
        }
    }

    // Highlight: ==text==
    if (settings.highlight) {
        checkMatch("HIGHLIGHT", Regex("==([^=]+)==")) { m -> Pair(m.groupValues[1], "") }
    }

    // Strikethrough: ~~text~~
    if (settings.strikethrough) {
        checkMatch("STRIKE", Regex("~~([^~]+)~~")) { m -> Pair(m.groupValues[1], "") }
    }

    // Bold & Italic: ***both*** or ___both___
    if (settings.boldItalic) {
        checkMatch("BOLD_ITALIC_STAR", Regex("\\*\\*\\*([^*]+)\\*\\*\\*")) { m -> Pair(m.groupValues[1], "") }
        checkMatch("BOLD_ITALIC_UNDER", Regex("___([^_]+)___")) { m -> Pair(m.groupValues[1], "") }
        // Bold: **bold** or __bold__
        checkMatch("BOLD_STAR", Regex("\\*\\*([^*]+)\\*\\*")) { m -> Pair(m.groupValues[1], "") }
        checkMatch("BOLD_UNDER", Regex("__([^_]+)__")) { m -> Pair(m.groupValues[1], "") }
        // Italic: *italic* or _italic_
        checkMatch("ITALIC_STAR", Regex("\\*([^*]+)\\*")) { m -> Pair(m.groupValues[1], "") }
        checkMatch("ITALIC_UNDER", Regex("_([^_]+)_")) { m -> Pair(m.groupValues[1], "") }
    }

    if (earliest == null) {
        // No tokens found: append plain text (unescaped)
        append(unescape(text))
        return
    }

    val match = earliest!!
    // Append leading plain text before token
    if (match.start > 0) {
        append(unescape(text.substring(0, match.start)))
    }

    when (match.type) {
        "CODE" -> {
            val startIdx = length
            append(unescape(match.inner1))
            val endIdx = length
            addStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (baseFontSize * 0.90f).sp,
                    background = codeBgColor
                ),
                startIdx,
                endIdx
            )
        }

        "MD_LINK", "BARE_URL" -> {
            val linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            )
            val startIdx = length
            addLink(
                LinkAnnotation.Url(url = match.inner2, styles = linkStyles),
                startIdx,
                startIdx + unescape(match.inner1).length
            )
            append(unescape(match.inner1))
        }

        "HIGHLIGHT" -> {
            val startIdx = length
            parseInlineRecursive(
                match.inner1,
                settings,
                linkColor,
                codeBgColor,
                highlightBgColor,
                baseFontSize,
                unescape
            )
            val endIdx = length
            addStyle(
                SpanStyle(
                    background = highlightBgColor,
                    fontWeight = FontWeight.Medium
                ),
                startIdx,
                endIdx
            )
        }

        "STRIKE" -> {
            val startIdx = length
            parseInlineRecursive(
                match.inner1,
                settings,
                linkColor,
                codeBgColor,
                highlightBgColor,
                baseFontSize,
                unescape
            )
            val endIdx = length
            addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
                startIdx,
                endIdx
            )
        }

        "BOLD_ITALIC_STAR", "BOLD_ITALIC_UNDER" -> {
            val startIdx = length
            parseInlineRecursive(
                match.inner1,
                settings,
                linkColor,
                codeBgColor,
                highlightBgColor,
                baseFontSize,
                unescape
            )
            val endIdx = length
            addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                ),
                startIdx,
                endIdx
            )
        }

        "BOLD_STAR", "BOLD_UNDER" -> {
            val startIdx = length
            parseInlineRecursive(
                match.inner1,
                settings,
                linkColor,
                codeBgColor,
                highlightBgColor,
                baseFontSize,
                unescape
            )
            val endIdx = length
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                startIdx,
                endIdx
            )
        }

        "ITALIC_STAR", "ITALIC_UNDER" -> {
            val startIdx = length
            parseInlineRecursive(
                match.inner1,
                settings,
                linkColor,
                codeBgColor,
                highlightBgColor,
                baseFontSize,
                unescape
            )
            val endIdx = length
            addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                startIdx,
                endIdx
            )
        }
    }

    // Process remaining trailing text
    if (match.end < text.length) {
        parseInlineRecursive(
            text.substring(match.end),
            settings,
            linkColor,
            codeBgColor,
            highlightBgColor,
            baseFontSize,
            unescape
        )
    }
}
