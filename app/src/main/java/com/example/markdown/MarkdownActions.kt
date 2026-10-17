package com.example.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Pure helper functions for Obsidian-style Markdown formatting actions.
 * Wraps the current selection or inserts syntax at the cursor.
 *
 * Each action operates on a [TextFieldValue] and returns a new [TextFieldValue],
 * allowing single-step undo/redo when applied via ViewModel.
 */
object MarkdownActions {

    fun applyActionById(id: String, value: TextFieldValue): TextFieldValue {
        return when (id) {
            "heading" -> cycleHeading(value)
            "bold" -> applyBold(value)
            "italic" -> applyItalic(value)
            "strikethrough" -> applyStrikethrough(value)
            "highlight" -> applyHighlight(value)
            "bullet_list" -> applyBulletList(value)
            "numbered_list" -> applyNumberedList(value)
            "checkbox" -> applyCheckbox(value)
            "quote" -> applyQuote(value)
            "inline_code" -> applyInlineCode(value)
            "code_block" -> applyCodeBlock(value)
            "link" -> applyLink(value)
            "horizontal_rule" -> applyHorizontalRule(value)
            else -> value
        }
    }

    /**
     * Bolds the selected text (**text**) or inserts **** with the cursor in the middle.
     */
    fun applyBold(value: TextFieldValue): TextFieldValue {
        return wrapInline(value, "**")
    }

    /**
     * Italicizes the selected text (*text*) or inserts ** with the cursor in the middle.
     */
    fun applyItalic(value: TextFieldValue): TextFieldValue {
        return wrapInline(value, "*")
    }

    /**
     * Strikethrough the selected text (~~text~~) or inserts ~~~~ with the cursor in the middle.
     */
    fun applyStrikethrough(value: TextFieldValue): TextFieldValue {
        return wrapInline(value, "~~")
    }

    /**
     * Highlights the selected text (==text==) or inserts ==== with the cursor in the middle.
     */
    fun applyHighlight(value: TextFieldValue): TextFieldValue {
        return wrapInline(value, "==")
    }

    /**
     * Inline code (`code`) or inserts `` with the cursor in the middle.
     */
    fun applyInlineCode(value: TextFieldValue): TextFieldValue {
        return wrapInline(value, "`")
    }

    /**
     * Cycles the current line's heading level:
     * Normal text -> # H1 -> ## H2 -> ### H3 -> Normal text
     */
    fun cycleHeading(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val (newLine, delta) = when {
            currentLine.startsWith("### ") -> {
                Pair(currentLine.removePrefix("### "), -4)
            }
            currentLine.startsWith("## ") -> {
                Pair("### " + currentLine.removePrefix("## "), 1)
            }
            currentLine.startsWith("# ") -> {
                Pair("## " + currentLine.removePrefix("# "), 1)
            }
            currentLine.startsWith("#### ") || currentLine.startsWith("##### ") || currentLine.startsWith("###### ") -> {
                val stripped = currentLine.trimStart('#').trimStart()
                Pair("# $stripped", 2 - (currentLine.length - stripped.length))
            }
            else -> {
                Pair("# $currentLine", 2)
            }
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor + delta).coerceIn(lineStart, lineStart + newLine.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Bullet list item (- item):
     * Toggles bullet list on the current line or turns existing list item into bullet list.
     */
    fun applyBulletList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val (newLine, delta) = when {
            currentLine.startsWith("- [ ] ") -> Pair("- " + currentLine.removePrefix("- [ ] "), -4)
            currentLine.startsWith("- [x] ") || currentLine.startsWith("- [X] ") -> Pair("- " + currentLine.removePrefix(currentLine.substring(0, 6)), -4)
            currentLine.startsWith("- ") -> Pair(currentLine.removePrefix("- "), -2)
            currentLine.startsWith("* ") -> Pair("- " + currentLine.removePrefix("* "), 0)
            currentLine.startsWith("+ ") -> Pair("- " + currentLine.removePrefix("+ "), 0)
            Regex("^\\d+\\.\\s").containsMatchIn(currentLine) -> {
                val prefixMatch = Regex("^(\\d+\\.\\s)").find(currentLine)!!
                val prefixLen = prefixMatch.value.length
                Pair("- " + currentLine.substring(prefixLen), 2 - prefixLen)
            }
            else -> Pair("- $currentLine", 2)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor + delta).coerceIn(lineStart, lineStart + newLine.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Numbered list item (1. item):
     * Toggles numbered list on the current line.
     */
    fun applyNumberedList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val numMatch = Regex("^(\\d+\\.\\s)").find(currentLine)
        val (newLine, delta) = when {
            numMatch != null -> {
                val len = numMatch.value.length
                Pair(currentLine.substring(len), -len)
            }
            currentLine.startsWith("- [ ] ") -> Pair("1. " + currentLine.removePrefix("- [ ] "), -3)
            currentLine.startsWith("- [x] ") || currentLine.startsWith("- [X] ") -> Pair("1. " + currentLine.removePrefix(currentLine.substring(0, 6)), -3)
            currentLine.startsWith("- ") -> Pair("1. " + currentLine.removePrefix("- "), 1)
            currentLine.startsWith("* ") -> Pair("1. " + currentLine.removePrefix("* "), 1)
            else -> Pair("1. $currentLine", 3)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor + delta).coerceIn(lineStart, lineStart + newLine.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Checkbox task list (- [ ] item):
     * Cycles: Normal -> - [ ] -> - [x] -> Normal
     */
    fun applyCheckbox(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val (newLine, delta) = when {
            currentLine.startsWith("- [ ] ") -> {
                Pair("- [x] " + currentLine.removePrefix("- [ ] "), 0)
            }
            currentLine.startsWith("- [x] ") || currentLine.startsWith("- [X] ") -> {
                Pair(currentLine.substring(6), -6)
            }
            currentLine.startsWith("- ") -> {
                Pair("- [ ] " + currentLine.removePrefix("- "), 4)
            }
            currentLine.startsWith("* ") -> {
                Pair("- [ ] " + currentLine.removePrefix("* "), 4)
            }
            Regex("^\\d+\\.\\s").containsMatchIn(currentLine) -> {
                val match = Regex("^(\\d+\\.\\s)").find(currentLine)!!
                val len = match.value.length
                Pair("- [ ] " + currentLine.substring(len), 6 - len)
            }
            else -> {
                Pair("- [ ] $currentLine", 6)
            }
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor + delta).coerceIn(lineStart, lineStart + newLine.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Blockquote (> quote):
     * Toggles blockquote on current line.
     */
    fun applyQuote(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        val (newLine, delta) = when {
            currentLine.startsWith("> ") -> Pair(currentLine.removePrefix("> "), -2)
            currentLine.startsWith(">") -> Pair(currentLine.removePrefix(">"), -1)
            else -> Pair("> $currentLine", 2)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = (cursor + delta).coerceIn(lineStart, lineStart + newLine.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Fenced code block (```code```):
     * Wraps selection in fenced code block, or creates an empty code block at cursor.
     */
    fun applyCodeBlock(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)

        return if (start != end) {
            val selected = text.substring(start, end)
            val prefix = if (start > 0 && text[start - 1] != '\n') "\n```\n" else "```\n"
            val suffix = if (end < text.length && text[end] != '\n') "\n```\n" else "\n```"
            val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length, start + prefix.length + selected.length)
            )
        } else {
            val prefix = if (start > 0 && text[start - 1] != '\n') "\n```\n" else "```\n"
            val suffix = "\n```\n"
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length)
            )
        }
    }

    /**
     * Markdown Link ([text](url)):
     * If URL selected: puts URL into () and places cursor in []
     * If text selected: puts text into [] and places cursor in ()
     * If no selection: inserts []() with cursor in []
     */
    fun applyLink(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)

        return if (start != end) {
            val selected = text.substring(start, end)
            if (selected.startsWith("http://") || selected.startsWith("https://")) {
                val inserted = "[]($selected)"
                val newText = text.substring(0, start) + inserted + text.substring(end)
                TextFieldValue(newText, TextRange(start + 1))
            } else {
                val inserted = "[$selected]()"
                val newText = text.substring(0, start) + inserted + text.substring(end)
                TextFieldValue(newText, TextRange(start + selected.length + 3))
            }
        } else {
            val inserted = "[]()"
            val newText = text.substring(0, start) + inserted + text.substring(start)
            TextFieldValue(newText, TextRange(start + 1))
        }
    }

    /**
     * Horizontal Rule (---):
     * Inserts a horizontal divider rule on a fresh line.
     */
    fun applyHorizontalRule(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val cursor = value.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        return if (currentLine.trim().isEmpty()) {
            val newText = text.substring(0, lineStart) + "---\n" + text.substring(lineEnd)
            TextFieldValue(newText, TextRange(lineStart + 4))
        } else {
            val prefix = if (cursor > 0 && text[cursor - 1] != '\n') "\n\n---\n\n" else "\n---\n\n"
            val newText = text.substring(0, cursor) + prefix + text.substring(cursor)
            TextFieldValue(newText, TextRange(cursor + prefix.length))
        }
    }

    private fun wrapInline(value: TextFieldValue, delimiter: String): TextFieldValue {
        val text = value.text
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)
        val dLen = delimiter.length

        return if (start != end) {
            val selected = text.substring(start, end)
            // If already wrapped by delimiter on both sides, toggle off (unwrap)
            val isAlreadyWrapped = start >= dLen && end + dLen <= text.length &&
                    text.substring(start - dLen, start) == delimiter &&
                    text.substring(end, end + dLen) == delimiter

            if (isAlreadyWrapped) {
                val newText = text.substring(0, start - dLen) + selected + text.substring(end + dLen)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(start - dLen, end - dLen)
                )
            } else {
                val newText = text.substring(0, start) + delimiter + selected + delimiter + text.substring(end)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(start + dLen, end + dLen)
                )
            }
        } else {
            // No selection: insert delimiter pair and place cursor inside
            val newText = text.substring(0, start) + delimiter + delimiter + text.substring(start)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + dLen)
            )
        }
    }
}
