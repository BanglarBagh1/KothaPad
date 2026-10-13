package com.example

import com.example.data.model.KeyActionType
import com.example.ui.keyboard.KeyboardLayouts
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun defaultKeyboardLayout_matchesUserSpecification() {
        val layout = KeyboardLayouts.getDefault3x10Layout()
        assertEquals(3, layout.rows.size)

        // Row 1: ো, া, ে, ি, ।, ,, ?, ., ⌫, ⌫
        val row1 = layout.rows[0].keys
        assertEquals(10, row1.size)
        assertEquals("ো", row1[0].label)
        assertEquals(KeyActionType.INSERT, row1[0].actionType)
        assertEquals("া", row1[1].label)
        assertEquals("ে", row1[2].label)
        assertEquals("ি", row1[3].label)
        assertEquals("।", row1[4].label)
        assertEquals(",", row1[5].label)
        assertEquals("?", row1[6].label)
        assertEquals(".", row1[7].label)
        assertEquals("⌫", row1[8].label)
        assertEquals(KeyActionType.BACKSPACE, row1[8].actionType)
        assertEquals("⌫", row1[9].label)
        assertEquals(KeyActionType.BACKSPACE, row1[9].actionType)

        // Row 2: ◀, ▲, ▼, ▶, ↵, 📅, 🕒, 📋, ¶, 🌐
        val row2 = layout.rows[1].keys
        assertEquals(10, row2.size)
        assertEquals("◀", row2[0].label)
        assertEquals(KeyActionType.CURSOR_LEFT, row2[0].actionType)
        assertEquals("▲", row2[1].label)
        assertEquals(KeyActionType.CURSOR_UP, row2[1].actionType)
        assertEquals("▼", row2[2].label)
        assertEquals(KeyActionType.CURSOR_DOWN, row2[2].actionType)
        assertEquals("▶", row2[3].label)
        assertEquals(KeyActionType.CURSOR_RIGHT, row2[3].actionType)
        assertEquals("↵", row2[4].label)
        assertEquals(KeyActionType.ENTER, row2[4].actionType)
        assertEquals("📅", row2[5].label)
        assertEquals("{date}", row2[5].outputText)
        assertEquals("🕒", row2[6].label)
        assertEquals("{time}", row2[6].outputText)
        assertEquals("📋", row2[7].label)
        assertEquals(KeyActionType.OPEN_TEMPLATES, row2[7].actionType)
        assertEquals("¶", row2[8].label)
        assertEquals(KeyActionType.NEW_PARAGRAPH, row2[8].actionType)
        assertEquals("🌐", row2[9].label)
        assertEquals(KeyActionType.CHANGE_LANGUAGE, row2[9].actionType)

        // Row 3: -, (, ),  ,  , ই, ও, ঁ, হ, ␣
        val row3 = layout.rows[2].keys
        assertEquals(10, row3.size)
        assertEquals("-", row3[0].label)
        assertEquals("(", row3[1].label)
        assertEquals(")", row3[2].label)
        assertEquals(" ", row3[3].label)
        assertEquals(" ", row3[4].label)
        assertEquals("ই", row3[5].label)
        assertEquals("ও", row3[6].label)
        assertEquals("ঁ", row3[7].label)
        assertEquals("হ", row3[8].label)
        assertEquals("␣", row3[9].label)
        assertEquals(KeyActionType.SPACE, row3[9].actionType)
    }
}
