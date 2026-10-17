package com.example.data.model

/**
 * Configuration and state model for the Floating Controls Layout ("Side Rail").
 * All settings apply LIVE with no app restart.
 */
data class FloatingLayoutSettings(
    val useSideRail: Boolean = true,
    val railSide: String = "RIGHT", // "RIGHT" or "LEFT"
    val railScale: Float = 1.0f, // 0.5x - 1.5x
    val railBottomOffsetDp: Int = 8, // 0 - 200 dp
    val railSpacingDp: Int = 6, // 0 - 16 dp
    val keepTextClearOfRail: Boolean = true,
    // Order of buttons in the rail column from TOP to BOTTOM:
    // Default top-to-bottom: COPY, REDO, UNDO, MIC (which means bottom-to-top is MIC, UNDO, REDO, COPY)
    val railButtonOrder: List<String> = DEFAULT_RAIL_BUTTON_ORDER,
    val toggleScale: Float = 1.0f, // 0.5x - 1.5x
    val toggleBottomOffsetDp: Int = 8, // 0 - 200 dp
    val toggleGapDp: Int = 8, // 0 - 24 dp
    val formatBarScale: Float = 1.0f, // 0.5x - 1.5x
    val formatBarBottomOffsetDp: Int = 8, // 0 - 200 dp
    val formatBarWidthPercent: Int = 100, // 50% - 100%
    val formatBarAlignment: String = "LEFT", // "LEFT", "CENTER", "RIGHT"
    val waveformScale: Float = 1.0f, // 0.5x - 1.5x
    val showWaveform: Boolean = true
) {
    companion object {
        const val BUTTON_MIC = "MIC"
        const val BUTTON_UNDO = "UNDO"
        const val BUTTON_REDO = "REDO"
        const val BUTTON_COPY = "COPY"

        // Top to bottom default list
        val DEFAULT_RAIL_BUTTON_ORDER = listOf(BUTTON_COPY, BUTTON_REDO, BUTTON_UNDO, BUTTON_MIC)

        val ALL_BUTTON_IDS = listOf(BUTTON_MIC, BUTTON_UNDO, BUTTON_REDO, BUTTON_COPY)

        fun parseOrder(orderStr: String?): List<String> {
            if (orderStr.isNullOrBlank()) return DEFAULT_RAIL_BUTTON_ORDER
            val parsed = orderStr.split(",")
                .map { it.trim().uppercase() }
                .filter { it in ALL_BUTTON_IDS }
                .distinct()
            if (parsed.isEmpty()) return DEFAULT_RAIL_BUTTON_ORDER
            val result = parsed.toMutableList()
            for (btn in DEFAULT_RAIL_BUTTON_ORDER) {
                if (!result.contains(btn)) {
                    result.add(btn)
                }
            }
            return result
        }

        fun orderToString(order: List<String>): String {
            return order.joinToString(",")
        }
    }
}
