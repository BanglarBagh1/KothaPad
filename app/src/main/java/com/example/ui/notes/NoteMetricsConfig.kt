package com.example.ui.notes

import android.text.format.DateFormat
import com.example.data.model.Note
import com.example.util.NoteTimeFormatter
import java.util.Date
import kotlin.math.ceil

object NoteMetricsConfig {
    const val METRIC_WORDS = "WORDS"
    const val METRIC_SESSION_TIME = "SESSION_TIME"
    const val METRIC_CHARS = "CHARS"
    const val METRIC_READ_TIME = "READ_TIME"
    const val METRIC_MODIFIED_TIME = "MODIFIED_TIME"
    const val METRIC_CREATED_TIME = "CREATED_TIME"

    val DEFAULT_ORDER = listOf(
        METRIC_WORDS,
        METRIC_SESSION_TIME,
        METRIC_CHARS,
        METRIC_READ_TIME,
        METRIC_MODIFIED_TIME,
        METRIC_CREATED_TIME
    )

    val DEFAULT_ORDER_STRING: String = DEFAULT_ORDER.joinToString(",")

    data class ItemMeta(
        val key: String,
        val label: String,
        val description: String,
        val compactSample: String,
        val detailedSample: String
    )

    val ALL_ITEMS: List<ItemMeta> = listOf(
        ItemMeta(
            key = METRIC_WORDS,
            label = "Words count",
            description = "Number of words in note content",
            compactSample = "120w",
            detailedSample = "120 words"
        ),
        ItemMeta(
            key = METRIC_SESSION_TIME,
            label = "Time passed in note",
            description = "Total time spent in note",
            compactSample = "2m 15s spent",
            detailedSample = "2m 15s spent"
        ),
        ItemMeta(
            key = METRIC_CHARS,
            label = "Character count",
            description = "Total characters in note content",
            compactSample = "650c",
            detailedSample = "650 characters"
        ),
        ItemMeta(
            key = METRIC_READ_TIME,
            label = "Time to read",
            description = "Estimated reading duration (200 wpm)",
            compactSample = "~1m read",
            detailedSample = "~1 min read"
        ),
        ItemMeta(
            key = METRIC_MODIFIED_TIME,
            label = "Time modified",
            description = "Date and time last modified",
            compactSample = "Mod: Oct 13, 08:30 PM",
            detailedSample = "Modified: Oct 13, 08:30 PM"
        ),
        ItemMeta(
            key = METRIC_CREATED_TIME,
            label = "Time created",
            description = "Date and time note was created",
            compactSample = "Created: Oct 12, 09:15 AM",
            detailedSample = "Created: Oct 12, 09:15 AM"
        )
    )

    fun parseOrder(orderStr: String): List<String> {
        val parsed = orderStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val result = parsed.toMutableList()
        for (item in DEFAULT_ORDER) {
            if (!result.contains(item)) {
                result.add(item)
            }
        }
        return result
    }

    fun formatMetrics(
        note: Note,
        style: String,
        orderStr: String = DEFAULT_ORDER_STRING,
        hiddenItems: Set<String> = emptySet()
    ): String {
        val isDetailed = style.equals("DETAILED", ignoreCase = true)
        val orderedKeys = parseOrder(orderStr)

        val words = if (note.content.isBlank()) 0 else note.content.trim().split("\\s+".toRegex()).size
        val chars = note.content.length
        val readMins = ceil(words / 200.0).toInt().coerceAtLeast(1)
        val sessionStr = NoteTimeFormatter.formatDuration(note.sessionDurationSec)
        val modDate = DateFormat.format("MMM dd, hh:mm a", Date(note.updatedAt)).toString()
        val createdDate = DateFormat.format("MMM dd, hh:mm a", Date(note.createdAt)).toString()

        val tokens = mutableListOf<String>()
        for (key in orderedKeys) {
            if (hiddenItems.contains(key)) continue
            val part = when (key) {
                METRIC_WORDS -> if (isDetailed) "$words words" else "${words}w"
                METRIC_SESSION_TIME -> "$sessionStr spent"
                METRIC_CHARS -> if (isDetailed) "$chars characters" else "${chars}c"
                METRIC_READ_TIME -> if (isDetailed) "~$readMins min read" else "~${readMins}m read"
                METRIC_MODIFIED_TIME -> if (isDetailed) "Modified: $modDate" else "Mod: $modDate"
                METRIC_CREATED_TIME -> if (isDetailed) "Created: $createdDate" else "Created: $createdDate"
                else -> null
            }
            if (!part.isNullOrBlank()) {
                tokens.add(part)
            }
        }
        return tokens.joinToString(" • ")
    }

    fun formatSampleMetrics(
        style: String,
        orderStr: String = DEFAULT_ORDER_STRING,
        hiddenItems: Set<String> = emptySet()
    ): String {
        val isDetailed = style.equals("DETAILED", ignoreCase = true)
        val orderedKeys = parseOrder(orderStr)

        val tokens = mutableListOf<String>()
        for (key in orderedKeys) {
            if (hiddenItems.contains(key)) continue
            val meta = ALL_ITEMS.find { it.key == key }
            if (meta != null) {
                tokens.add(if (isDetailed) meta.detailedSample else meta.compactSample)
            }
        }
        return tokens.joinToString(" • ")
    }
}
