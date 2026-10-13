package com.example.engine.pipeline

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProcessedStampResult(
    val outputText: String,
    val cursorOffsetFromStart: Int // Where cursor should be placed relative to inserted text
)

object StampProcessor {

    fun processTemplate(
        template: String,
        customDateFormat: String? = null,
        customTimeFormat: String? = null,
        locale: Locale = Locale.getDefault()
    ): ProcessedStampResult {
        val now = Date()

        val datePattern = if (!customDateFormat.isNullOrBlank()) customDateFormat else "yyyy-MM-dd"
        val timePattern = if (!customTimeFormat.isNullOrBlank()) customTimeFormat else "hh:mm a"

        val dateFormat = try {
            SimpleDateFormat(datePattern, locale)
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", locale)
        }

        val timeFormat = try {
            SimpleDateFormat(timePattern, locale)
        } catch (_: Exception) {
            SimpleDateFormat("hh:mm a", locale)
        }

        val dateTimeFormat = try {
            SimpleDateFormat("$datePattern $timePattern", locale)
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd hh:mm a", locale)
        }

        val weekdayFormat = SimpleDateFormat("EEEE", locale)

        val dateStr = dateFormat.format(now)
        val timeStr = timeFormat.format(now)
        val dateTimeStr = dateTimeFormat.format(now)
        val weekdayStr = weekdayFormat.format(now)

        var replaced = template
            .replace("{date}", dateStr)
            .replace("{time}", timeStr)
            .replace("{datetime}", dateTimeStr)
            .replace("{weekday}", weekdayStr)

        val cursorIndex = replaced.indexOf("{cursor}")
        val finalCursorOffset: Int

        if (cursorIndex != -1) {
            replaced = replaced.removeRange(cursorIndex, cursorIndex + "{cursor}".length)
            finalCursorOffset = cursorIndex
        } else {
            // Default cursor at end of inserted text
            finalCursorOffset = replaced.length
        }

        return ProcessedStampResult(
            outputText = replaced,
            cursorOffsetFromStart = finalCursorOffset
        )
    }
}
