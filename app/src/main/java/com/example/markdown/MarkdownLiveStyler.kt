package com.example.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighlightGold
import com.example.ui.theme.SaffronPrimary

/**
 * Creates and remembers a VisualTransformation that applies live Markdown styling
 * to the editor BasicTextField without altering the underlying raw text.
 */
@Composable
fun rememberMarkdownVisualTransformation(
    settings: MarkdownSettings,
    baseFontSize: Float
): VisualTransformation {
    if (!settings.markdownEnabled || !settings.liveStylingEnabled) {
        return VisualTransformation.None
    }

    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val highlightBg = HighlightGold.copy(alpha = 0.35f)
    val inlineCodeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val codeBlockBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    val linkColor = MaterialTheme.colorScheme.primary
    val quoteColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    val listMarkerColor = SaffronPrimary

    return remember(
        settings,
        baseFontSize,
        dimColor,
        highlightBg,
        inlineCodeBg,
        codeBlockBg,
        linkColor,
        quoteColor,
        listMarkerColor
    ) {
        MarkdownVisualTransformation(
            settings = settings,
            baseFontSize = baseFontSize,
            dimColor = dimColor,
            highlightBg = highlightBg,
            inlineCodeBg = inlineCodeBg,
            codeBlockBg = codeBlockBg,
            linkColor = linkColor,
            quoteColor = quoteColor,
            listMarkerColor = listMarkerColor
        )
    }
}

class MarkdownVisualTransformation(
    private val settings: MarkdownSettings,
    private val baseFontSize: Float,
    private val dimColor: Color,
    private val highlightBg: Color,
    private val inlineCodeBg: Color,
    private val codeBlockBg: Color,
    private val linkColor: Color,
    private val quoteColor: Color,
    private val listMarkerColor: Color
) : VisualTransformation {

    private var cachedRawText: String? = null
    private var cachedResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        if (!settings.markdownEnabled || !settings.liveStylingEnabled) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val raw = text.text
        if (raw == cachedRawText && cachedResult != null) {
            return cachedResult!!
        }

        val styled = styleMarkdownText(
            rawText = raw,
            settings = settings,
            baseFontSize = baseFontSize,
            dimColor = dimColor,
            highlightBg = highlightBg,
            inlineCodeBg = inlineCodeBg,
            codeBlockBg = codeBlockBg,
            linkColor = linkColor,
            quoteColor = quoteColor,
            listMarkerColor = listMarkerColor
        )

        val result = TransformedText(styled, OffsetMapping.Identity)
        cachedRawText = raw
        cachedResult = result
        return result
    }
}

private val REGEX_HEADING = Regex("^(#{1,6})(\\s+)(.*)$")
private val REGEX_BLOCKQUOTE = Regex("^(\\s*)(>+)(\\s*)(.*)$")
private val REGEX_TASK_LIST = Regex("^(\\s*)([-*+])(\\s+)(\\[[ xX]\\])(\\s*)(.*)$")
private val REGEX_LIST = Regex("^(\\s*)([-*+]|\\d+\\.)(\\s+)(.*)$")
private val REGEX_HORIZONTAL_RULE = Regex("^\\s*([-*_])\\s*\\1\\s*\\1[\\s\\-*_]*$")

private val REGEX_INLINE_CODE = Regex("(`)([^`\n]+)(`)")
private val REGEX_HIGHLIGHT = Regex("(==)([^=\n]+)(==)")
private val REGEX_STRIKE = Regex("(~~)([^~\n]+)(~~)")
private val REGEX_BOLD_ITALIC_STAR = Regex("(\\*\\*\\*)([^*\n]+)(\\*\\*\\*)")
private val REGEX_BOLD_ITALIC_UNDER = Regex("(___)([^_\n]+)(___)")
private val REGEX_BOLD_STAR = Regex("(\\*\\*)([^*\n]+)(\\*\\*)")
private val REGEX_BOLD_UNDER = Regex("(__)([^_\n]+)(__)")
private val REGEX_ITALIC_STAR = Regex("(?<!\\*)(\\*)[^*\n]+(\\*)(?!\\*)")
private val REGEX_ITALIC_UNDER = Regex("(?<!_)(_)[^_\n]+(_)(?!_)")
private val REGEX_MD_LINK = Regex("(\\[)([^\\]\n]+)(\\])(\\()([^)\n]+)(\\))")
private val REGEX_BARE_URL = Regex("(?<!\\()(https?://[a-zA-Z0-9_./?=&%#+-]+)")
private val REGEX_ESCAPED = Regex("\\\\([*`_~=\\-\\#\\[\\]>\\\\])")

fun styleMarkdownText(
    rawText: String,
    settings: MarkdownSettings,
    baseFontSize: Float,
    dimColor: Color,
    highlightBg: Color,
    inlineCodeBg: Color,
    codeBlockBg: Color,
    linkColor: Color,
    quoteColor: Color,
    listMarkerColor: Color
): AnnotatedString {
    if (rawText.isEmpty()) return AnnotatedString("")

    val dimStyle = SpanStyle(color = dimColor)
    val lines = rawText.split('\n')

    return buildAnnotatedString {
        append(rawText)

        var lineStartIndex = 0
        var inCodeBlock = false
        var codeBlockStartIndex = 0

        for (line in lines) {
            val lineEndIndex = lineStartIndex + line.length
            val trimmed = line.trim()

            // 1. Code Blocks
            if (settings.codeBlocks && trimmed.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeBlockStartIndex = lineStartIndex
                    if (settings.dimSyntaxSymbols) {
                        addStyle(dimStyle, lineStartIndex, lineEndIndex)
                    }
                } else {
                    inCodeBlock = false
                    val blockSpanEnd = lineEndIndex
                    addStyle(
                        SpanStyle(
                            background = codeBlockBg,
                            fontFamily = if (settings.codeFontMonospace) FontFamily.Monospace else null
                        ),
                        codeBlockStartIndex,
                        blockSpanEnd
                    )
                    if (settings.dimSyntaxSymbols) {
                        addStyle(dimStyle, lineStartIndex, lineEndIndex)
                    }
                }
                lineStartIndex = lineEndIndex + 1
                continue
            }

            if (inCodeBlock) {
                // Inside fenced code block
                lineStartIndex = lineEndIndex + 1
                continue
            }

            // 2. Headings
            if (settings.headings) {
                val match = REGEX_HEADING.find(line)
                if (match != null) {
                    val hashes = match.groupValues[1]
                    val spaces = match.groupValues[2]
                    val level = hashes.length
                    val scaleFactor = when (level) {
                        1 -> 1.45f
                        2 -> 1.30f
                        3 -> 1.20f
                        4 -> 1.10f
                        5 -> 1.05f
                        else -> 1.00f
                    } * settings.headingSizeScale
                    val weight = if (level <= 2) FontWeight.Bold else FontWeight.SemiBold
                    val headingStyle = SpanStyle(
                        fontSize = (baseFontSize * scaleFactor).sp,
                        fontWeight = weight
                    )
                    // Apply heading scale to entire line
                    addStyle(headingStyle, lineStartIndex, lineEndIndex)

                    // Dim the "# " syntax
                    if (settings.dimSyntaxSymbols) {
                        val prefixLen = hashes.length + spaces.length
                        addStyle(dimStyle, lineStartIndex, lineStartIndex + prefixLen)
                    }

                    // Style inlines inside heading content
                    val contentStart = lineStartIndex + hashes.length + spaces.length
                    styleInlinesInLine(
                        lineText = match.groupValues[3],
                        lineOffset = contentStart,
                        settings = settings,
                        dimColor = dimColor,
                        highlightBg = highlightBg,
                        inlineCodeBg = inlineCodeBg,
                        linkColor = linkColor
                    )

                    lineStartIndex = lineEndIndex + 1
                    continue
                }
            }

            // 3. Blockquotes
            if (settings.blockquotes) {
                val match = REGEX_BLOCKQUOTE.find(line)
                if (match != null) {
                    val indent = match.groupValues[1]
                    val markers = match.groupValues[2]
                    val spaces = match.groupValues[3]
                    val content = match.groupValues[4]

                    // Italic and quote color for the line content
                    addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor),
                        lineStartIndex,
                        lineEndIndex
                    )

                    if (settings.dimSyntaxSymbols) {
                        val markerStart = lineStartIndex + indent.length
                        val markerEnd = markerStart + markers.length + spaces.length
                        addStyle(dimStyle, markerStart, markerEnd)
                    }

                    val contentStart = lineStartIndex + indent.length + markers.length + spaces.length
                    styleInlinesInLine(
                        lineText = content,
                        lineOffset = contentStart,
                        settings = settings,
                        dimColor = dimColor,
                        highlightBg = highlightBg,
                        inlineCodeBg = inlineCodeBg,
                        linkColor = linkColor
                    )

                    lineStartIndex = lineEndIndex + 1
                    continue
                }
            }

            // 4. Task Lists (- [ ] / - [x])
            if (settings.taskLists) {
                val match = REGEX_TASK_LIST.find(line)
                if (match != null) {
                    val indent = match.groupValues[1]
                    val bullet = match.groupValues[2]
                    val space1 = match.groupValues[3]
                    val box = match.groupValues[4]
                    val space2 = match.groupValues[5]
                    val content = match.groupValues[6]

                    val isChecked = box.contains('x', ignoreCase = true)

                    // Highlight list bullet and box
                    val boxStart = lineStartIndex + indent.length + bullet.length + space1.length
                    val boxEnd = boxStart + box.length
                    addStyle(
                        SpanStyle(
                            color = if (isChecked) SaffronPrimary else listMarkerColor,
                            fontWeight = FontWeight.Bold
                        ),
                        boxStart,
                        boxEnd
                    )

                    if (settings.dimSyntaxSymbols) {
                        val bulletStart = lineStartIndex + indent.length
                        addStyle(dimStyle, bulletStart, bulletStart + bullet.length)
                        addStyle(dimStyle, boxStart, boxStart + 1) // '['
                        addStyle(dimStyle, boxEnd - 1, boxEnd)     // ']'
                    }

                    val contentStart = boxEnd + space2.length
                    if (isChecked && settings.strikethrough) {
                        addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.LineThrough,
                                color = dimColor
                            ),
                            contentStart,
                            lineEndIndex
                        )
                    }

                    styleInlinesInLine(
                        lineText = content,
                        lineOffset = contentStart,
                        settings = settings,
                        dimColor = dimColor,
                        highlightBg = highlightBg,
                        inlineCodeBg = inlineCodeBg,
                        linkColor = linkColor
                    )

                    lineStartIndex = lineEndIndex + 1
                    continue
                }
            }

            // 5. Regular Bullet / Numbered Lists
            if (settings.lists) {
                val match = REGEX_LIST.find(line)
                if (match != null) {
                    val indent = match.groupValues[1]
                    val bullet = match.groupValues[2]
                    val spaces = match.groupValues[3]
                    val content = match.groupValues[4]

                    val markerStart = lineStartIndex + indent.length
                    val markerEnd = markerStart + bullet.length
                    addStyle(
                        SpanStyle(color = listMarkerColor, fontWeight = FontWeight.Bold),
                        markerStart,
                        markerEnd
                    )

                    if (settings.dimSyntaxSymbols) {
                        addStyle(dimStyle, markerStart, markerEnd)
                    }

                    val contentStart = markerEnd + spaces.length
                    styleInlinesInLine(
                        lineText = content,
                        lineOffset = contentStart,
                        settings = settings,
                        dimColor = dimColor,
                        highlightBg = highlightBg,
                        inlineCodeBg = inlineCodeBg,
                        linkColor = linkColor
                    )

                    lineStartIndex = lineEndIndex + 1
                    continue
                }
            }

            // 6. Horizontal Rule
            if (settings.horizontalRule && REGEX_HORIZONTAL_RULE.matches(trimmed)) {
                addStyle(dimStyle, lineStartIndex, lineEndIndex)
                lineStartIndex = lineEndIndex + 1
                continue
            }

            // 7. Regular paragraph / text line: format inline elements
            styleInlinesInLine(
                lineText = line,
                lineOffset = lineStartIndex,
                settings = settings,
                dimColor = dimColor,
                highlightBg = highlightBg,
                inlineCodeBg = inlineCodeBg,
                linkColor = linkColor
            )

            lineStartIndex = lineEndIndex + 1
        }
    }
}

private fun AnnotatedString.Builder.styleInlinesInLine(
    lineText: String,
    lineOffset: Int,
    settings: MarkdownSettings,
    dimColor: Color,
    highlightBg: Color,
    inlineCodeBg: Color,
    linkColor: Color
) {
    if (lineText.isEmpty()) return
    val dimStyle = SpanStyle(color = dimColor)

    // Backslash escapes: find \x and dim the slash
    if (settings.dimSyntaxSymbols) {
        REGEX_ESCAPED.findAll(lineText).forEach { m ->
            addStyle(dimStyle, lineOffset + m.range.first, lineOffset + m.range.first + 1)
        }
    }

    // 1. Inline code: `code`
    if (settings.inlineCode) {
        REGEX_INLINE_CODE.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 1
            val closeStart = lineOffset + m.range.last
            addStyle(
                SpanStyle(
                    background = inlineCodeBg,
                    fontFamily = if (settings.codeFontMonospace) FontFamily.Monospace else null
                ),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 1)
            }
        }
    }

    // 2. Highlight: ==text==
    if (settings.highlight) {
        REGEX_HIGHLIGHT.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 2
            val closeStart = lineOffset + m.range.last - 1
            addStyle(
                SpanStyle(background = highlightBg, fontWeight = FontWeight.Medium),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 2)
            }
        }
    }

    // 3. Strikethrough: ~~text~~
    if (settings.strikethrough) {
        REGEX_STRIKE.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 2
            val closeStart = lineOffset + m.range.last - 1
            addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 2)
            }
        }
    }

    // 4. Bold & Italic: ***both*** or ___both___
    if (settings.boldItalic) {
        REGEX_BOLD_ITALIC_STAR.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 3
            val closeStart = lineOffset + m.range.last - 2
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 3)
            }
        }
        REGEX_BOLD_ITALIC_UNDER.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 3
            val closeStart = lineOffset + m.range.last - 2
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 3)
            }
        }

        // Bold: **bold** or __bold__
        REGEX_BOLD_STAR.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 2
            val closeStart = lineOffset + m.range.last - 1
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 2)
            }
        }
        REGEX_BOLD_UNDER.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 2
            val closeStart = lineOffset + m.range.last - 1
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 2)
            }
        }

        // Italic: *italic* or _italic_
        REGEX_ITALIC_STAR.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 1
            val closeStart = lineOffset + m.range.last
            addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 1)
            }
        }
        REGEX_ITALIC_UNDER.findAll(lineText).forEach { m ->
            val openStart = lineOffset + m.range.first
            val contentStart = openStart + 1
            val closeStart = lineOffset + m.range.last
            addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                contentStart,
                closeStart
            )
            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openStart, contentStart)
                addStyle(dimStyle, closeStart, closeStart + 1)
            }
        }
    }

    // 5. Links: [text](url) and bare URLs
    if (settings.links) {
        REGEX_MD_LINK.findAll(lineText).forEach { m ->
            val linkTotalStart = lineOffset + m.range.first
            val openBracket = linkTotalStart
            val textStart = openBracket + 1
            val textEnd = textStart + m.groupValues[2].length
            val closeBracket = textEnd
            val openParen = closeBracket + 1
            val parenEnd = lineOffset + m.range.last + 1

            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                textStart,
                textEnd
            )

            if (settings.dimSyntaxSymbols) {
                addStyle(dimStyle, openBracket, textStart)
                addStyle(dimStyle, closeBracket, parenEnd)
            }
        }

        REGEX_BARE_URL.findAll(lineText).forEach { m ->
            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                lineOffset + m.range.first,
                lineOffset + m.range.last + 1
            )
        }
    }
}
