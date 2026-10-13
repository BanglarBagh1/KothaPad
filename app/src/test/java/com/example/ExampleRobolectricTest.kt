package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SpokenCommand
import com.example.engine.pipeline.PipelineContext
import com.example.engine.pipeline.StampProcessor
import com.example.engine.pipeline.TextPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KothaPad", appName)
    }

    @Test
    fun `test stamp token replacement`() {
        val template = "Entry: {date} at {time}\n{cursor}End"
        val result = StampProcessor.processTemplate(template, locale = Locale.US)
        assertTrue(result.outputText.contains("Entry: "))
        assertTrue(result.outputText.contains("End"))
        assertTrue(result.cursorOffsetFromStart > 0)
    }

    @Test
    fun `test bengali punctuation pipeline`() = runBlocking {
        val pipeline = TextPipeline.createDefault()
        val commands = listOf(
            SpokenCommand(language = "bn", phrase = "দাঁড়ি", actionType = "PUNCTUATION", replacementText = "। ")
        )
        val context = PipelineContext(
            languageCode = "bn-BD",
            precedingText = "",
            activeCommands = commands
        )
        val processed = pipeline.execute("আমি ভাত খাব দাঁড়ি", context)
        assertTrue(processed.contains("।"))
    }

    @Test
    fun `test keyboard serialization round trip`() {
        val original = com.example.ui.keyboard.KeyboardLayouts.getDefault3x10Layout()
        val json = com.example.ui.keyboard.KeyboardLayouts.serializeToJson(original)
        val deserialized = com.example.ui.keyboard.KeyboardLayouts.deserializeFromJson(json)

        assertEquals(original.rows.size, deserialized.rows.size)
        for (r in original.rows.indices) {
            val originalKeys = original.rows[r].keys
            val deserializedKeys = deserialized.rows[r].keys
            assertEquals(originalKeys.size, deserializedKeys.size)
            for (k in originalKeys.indices) {
                assertEquals(originalKeys[k].id, deserializedKeys[k].id)
                assertEquals(originalKeys[k].label, deserializedKeys[k].label)
                assertEquals(originalKeys[k].actionType, deserializedKeys[k].actionType)
                assertEquals(originalKeys[k].outputText, deserializedKeys[k].outputText)
            }
        }
    }

    @Test
    fun `test note metrics order, visibility, and formatting`() {
        val note = com.example.data.model.Note(
            id = 1L,
            title = "Test Note",
            content = "Hello world from test suite",
            sessionDurationSec = 135L, // 2m 15s
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L
        )

        // Default order: WORDS, SESSION_TIME, CHARS, READ_TIME, MODIFIED_TIME, CREATED_TIME
        val compactMetrics = com.example.ui.notes.NoteMetricsConfig.formatMetrics(
            note = note,
            style = "COMPACT",
            orderStr = com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING,
            hiddenItems = emptySet()
        )
        assertTrue(compactMetrics.contains("5w"))
        assertTrue(compactMetrics.contains("2m 15s spent"))
        assertTrue(compactMetrics.contains("27c"))
        assertTrue(compactMetrics.contains("~1m read"))

        // Test hiding specific items
        val hiddenMetrics = com.example.ui.notes.NoteMetricsConfig.formatMetrics(
            note = note,
            style = "COMPACT",
            orderStr = com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING,
            hiddenItems = setOf(
                com.example.ui.notes.NoteMetricsConfig.METRIC_CHARS,
                com.example.ui.notes.NoteMetricsConfig.METRIC_CREATED_TIME
            )
        )
        org.junit.Assert.assertFalse(hiddenMetrics.contains("27c"))
        assertTrue(hiddenMetrics.contains("5w"))
        assertTrue(hiddenMetrics.contains("2m 15s spent"))

        // Test custom serial order (SESSION_TIME first, then WORDS)
        val customOrder = "${com.example.ui.notes.NoteMetricsConfig.METRIC_SESSION_TIME},${com.example.ui.notes.NoteMetricsConfig.METRIC_WORDS}"
        val reorderedMetrics = com.example.ui.notes.NoteMetricsConfig.formatMetrics(
            note = note,
            style = "COMPACT",
            orderStr = customOrder,
            hiddenItems = setOf(
                com.example.ui.notes.NoteMetricsConfig.METRIC_CHARS,
                com.example.ui.notes.NoteMetricsConfig.METRIC_READ_TIME,
                com.example.ui.notes.NoteMetricsConfig.METRIC_MODIFIED_TIME,
                com.example.ui.notes.NoteMetricsConfig.METRIC_CREATED_TIME
            )
        )
        assertEquals("2m 15s spent • 5w", reorderedMetrics)
    }
}
