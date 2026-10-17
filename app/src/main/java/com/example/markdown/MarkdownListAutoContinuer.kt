package com.example.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Helper for automatically continuing lists when Enter is pressed in edit mode.
 * - Enter on a bullet, numbered, or task list item creates the next item with matching indentation.
 * - Enter on an empty list item clears the marker and ends the list.
 */
object MarkdownListAutoContinuer {

    private val REGEX_TASK_LIST = Regex("^(\\s*)([-*+])\\s+\\[([ xX])\\]\\s*(.*)$")
    private val REGEX_NUMBERED_LIST = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
    private val REGEX_BULLET_LIST = Regex("^(\\s*)([-*+])\\s+(.*)$")

    /**
     * Handles programmatic Enter key press (e.g., from in-app custom keyboard).
     * Returns the updated TextFieldValue if a list continuation or termination occurred, or null otherwise.
     */
    fun handleEnter(oldValue: TextFieldValue): TextFieldValue? {
        val text = oldValue.text
        val cursor = oldValue.selection.min.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let {
            if (it == -1) text.length else it
        }

        val currentLine = text.substring(lineStart, lineEnd)
        val cursorInLine = cursor - lineStart
        val beforeCursor = currentLine.substring(0, cursorInLine)
        val afterCursor = currentLine.substring(cursorInLine)

        // 1. Task List: - [ ] or - [x]
        val taskMatch = REGEX_TASK_LIST.find(currentLine)
        if (taskMatch != null) {
            val indent = taskMatch.groupValues[1]
            val bullet = taskMatch.groupValues[2]
            val content = taskMatch.groupValues[4]

            // Check if current task item is empty
            if (content.isBlank() && afterCursor.isBlank()) {
                // Clear the empty task marker, ending the list
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(text = newText, selection = TextRange(lineStart))
            }

            // Create next unchecked task item
            val nextPrefix = "$indent$bullet [ ] "
            val newText = text.substring(0, cursor) + "\n" + nextPrefix + text.substring(cursor)
            val newCursor = cursor + 1 + nextPrefix.length
            return TextFieldValue(text = newText, selection = TextRange(newCursor))
        }

        // 2. Numbered List: 1. , 2. , etc.
        val numMatch = REGEX_NUMBERED_LIST.find(currentLine)
        if (numMatch != null) {
            val indent = numMatch.groupValues[1]
            val currentNum = numMatch.groupValues[2].toLongOrNull() ?: 1L
            val content = numMatch.groupValues[3]

            // Check if current numbered item is empty
            if (content.isBlank() && afterCursor.isBlank()) {
                // Clear the empty number marker, ending the list
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(text = newText, selection = TextRange(lineStart))
            }

            // Create next incremented numbered item
            val nextPrefix = "$indent${currentNum + 1}. "
            val newText = text.substring(0, cursor) + "\n" + nextPrefix + text.substring(cursor)
            val newCursor = cursor + 1 + nextPrefix.length
            return TextFieldValue(text = newText, selection = TextRange(newCursor))
        }

        // 3. Bullet List: - , * , +
        val bulletMatch = REGEX_BULLET_LIST.find(currentLine)
        if (bulletMatch != null) {
            val indent = bulletMatch.groupValues[1]
            val bullet = bulletMatch.groupValues[2]
            val content = bulletMatch.groupValues[3]

            // Check if current bullet item is empty
            if (content.isBlank() && afterCursor.isBlank()) {
                // Clear the empty bullet marker, ending the list
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(text = newText, selection = TextRange(lineStart))
            }

            // Create next bullet item
            val nextPrefix = "$indent$bullet "
            val newText = text.substring(0, cursor) + "\n" + nextPrefix + text.substring(cursor)
            val newCursor = cursor + 1 + nextPrefix.length
            return TextFieldValue(text = newText, selection = TextRange(newCursor))
        }

        return null
    }

    /**
     * Intercepts TextFieldValue changes from the system keyboard.
     * Detects if the change was an Enter key press and applies list continuation.
     */
    fun handleValueChange(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        // Detect single newline insertion at previous cursor position
        val oldCursor = oldValue.selection.min
        if (newValue.text.length == oldValue.text.length + 1 &&
            oldCursor in 0 until newValue.text.length &&
            newValue.text[oldCursor] == '\n'
        ) {
            val listContinuation = handleEnter(oldValue)
            if (listContinuation != null) {
                return listContinuation
            }
        }
        return newValue
    }
}
