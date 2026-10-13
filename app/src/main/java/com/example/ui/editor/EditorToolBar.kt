package com.example.ui.editor

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSourceType
import com.example.data.model.DictationState
import com.example.ui.theme.MicErrorRed
import com.example.ui.theme.MicPausedAmber
import com.example.ui.theme.MicRestartingBlue
import com.example.ui.theme.SaffronPrimary
import com.example.util.NoteTimeFormatter

// Known Toolbar Item Identifiers in their default requested order:
// 1. Dictation Status
// 2. Metrics (Word & Char count)
// 3. Note Timer (Total time spent on specific note)
// 4. Keyboard Mode
// 5. Text Size (Zoom Out/In)
// 6. Text Style (Style sheet, highlight)
// 7. Audio Source (Phone mic & cloud processing system)
const val TOOLBAR_ITEM_DICTATION_STATUS = "DICTATION_STATUS"
const val TOOLBAR_ITEM_METRICS = "METRICS"
const val TOOLBAR_ITEM_NOTE_TIMER = "NOTE_TIMER"
const val TOOLBAR_ITEM_KEYBOARD_MODE = "KEYBOARD_MODE"
const val TOOLBAR_ITEM_TEXT_SIZE = "TEXT_SIZE"
const val TOOLBAR_ITEM_TEXT_STYLE = "TEXT_STYLE"
const val TOOLBAR_ITEM_AUDIO_SOURCE = "AUDIO_SOURCE"

val DEFAULT_TOOLBAR_ORDER = listOf(
    TOOLBAR_ITEM_DICTATION_STATUS,
    TOOLBAR_ITEM_METRICS,
    TOOLBAR_ITEM_NOTE_TIMER,
    TOOLBAR_ITEM_KEYBOARD_MODE,
    TOOLBAR_ITEM_TEXT_SIZE,
    TOOLBAR_ITEM_TEXT_STYLE,
    TOOLBAR_ITEM_AUDIO_SOURCE
)

@Composable
fun EditorToolBar(
    isInAppKeyboardActive: Boolean,
    dictationState: DictationState,
    audioSourceType: AudioSourceType,
    isOnDevice: Boolean,
    wordCount: Int,
    charCount: Int,
    showWordCount: Boolean,
    onZoomOutClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onStyleSheetClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onKeyboardToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarItemOrder: String = "DICTATION_STATUS,METRICS,NOTE_TIMER,KEYBOARD_MODE,TEXT_SIZE,TEXT_STYLE,AUDIO_SOURCE",
    hiddenItems: Set<String> = emptySet(),
    statusChipStyle: String = "PILL",
    hapticsLevel: String = "MEDIUM",
    sessionDurationSec: Long = 0L,
    onTimerClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current

    val hapticFeedback = {
        performHaptic(view, hapticsLevel)
    }

    val chipShape = when (statusChipStyle.uppercase()) {
        "ROUNDED" -> RoundedCornerShape(6.dp)
        "OUTLINE" -> RoundedCornerShape(8.dp)
        else -> RoundedCornerShape(16.dp)
    }

    val itemsList = rememberOrderedItems(toolbarItemOrder)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("secondary_toolbar"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in itemsList) {
                if (hiddenItems.contains(item)) continue

                when (item) {
                    TOOLBAR_ITEM_DICTATION_STATUS -> {
                        // 1. Voice dictation status (starting, listening, restarting, paused, error or idle)
                        val isListening = dictationState is DictationState.Listening
                        val isStarting = dictationState is DictationState.Starting
                        val isRestarting = dictationState is DictationState.Restarting
                        val isPaused = dictationState is DictationState.Paused
                        val isError = dictationState is DictationState.Error
                        val isIdle = dictationState is DictationState.Idle

                        val chipColor = when {
                            isListening || isStarting -> SaffronPrimary
                            isRestarting -> MicRestartingBlue
                            isPaused -> MicPausedAmber
                            isError -> MicErrorRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }

                        val statusText = when (dictationState) {
                            is DictationState.Starting -> "Starting..."
                            is DictationState.Listening -> "Listening"
                            is DictationState.Restarting -> "Restarting..."
                            is DictationState.Paused -> "Paused"
                            is DictationState.Error -> dictationState.message
                            else -> "Ready"
                        }

                        val isOutline = statusChipStyle.uppercase() == "OUTLINE"

                        Box(
                            modifier = Modifier
                                .clip(chipShape)
                                .then(
                                    if (isOutline) Modifier.border(1.dp, chipColor, chipShape)
                                    else Modifier.background(chipColor.copy(alpha = if (isIdle) 0.12f else 0.22f))
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("dictation_status_chip")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipColor
                                    )
                                )
                            }
                        }
                    }

                    TOOLBAR_ITEM_METRICS -> {
                        // 2. Note metrics: word count first and character count second
                        if (showWordCount) {
                            Box(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("metrics_chip")
                            ) {
                                Text(
                                    text = "$wordCount words • $charCount chars",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    TOOLBAR_ITEM_NOTE_TIMER -> {
                        // 3. Note-Specific Total Time Tracking button
                        Box(
                            modifier = Modifier
                                .clip(chipShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .clickable {
                                    hapticFeedback()
                                    onTimerClick?.invoke()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("note_timer_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Total Time Spent on Note",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = NoteTimeFormatter.formatDuration(sessionDurationSec),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    TOOLBAR_ITEM_KEYBOARD_MODE -> {
                        // 3. Switch keyboard modes button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isInAppKeyboardActive) SaffronPrimary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    hapticFeedback()
                                    onKeyboardToggleClick()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("keyboard_toggle_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isInAppKeyboardActive) Icons.Default.Keyboard else Icons.Default.KeyboardHide,
                                    contentDescription = "Toggle Keyboard",
                                    tint = if (isInAppKeyboardActive) SaffronPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isInAppKeyboardActive) "Custom" else "System",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (isInAppKeyboardActive) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    TOOLBAR_ITEM_TEXT_SIZE -> {
                        // 4. Buttons for increasing or decreasing text size
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    hapticFeedback()
                                    onZoomOutClick()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("zoom_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOut,
                                    contentDescription = "Zoom out font",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    hapticFeedback()
                                    onZoomInClick()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("zoom_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom in font",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    TOOLBAR_ITEM_TEXT_STYLE -> {
                        // Text Style Sheet button & Highlight Color button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    hapticFeedback()
                                    onStyleSheetClick()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("style_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = "Text style options",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    hapticFeedback()
                                    onHighlightClick()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("highlight_color_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatColorFill,
                                    contentDescription = "Highlight color",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    TOOLBAR_ITEM_AUDIO_SOURCE -> {
                        // 5. At the very end: audio input source (phone mic) and audio processing system (cloud)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${audioSourceType.icon} ${audioSourceType.displayName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isOnDevice) "⚡ Offline" else "☁️ Cloud",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun performHaptic(view: View, level: String) {
    when (level.uppercase()) {
        "OFF" -> {}
        "LIGHT" -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        "HEAVY" -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        else -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

private fun rememberOrderedItems(orderStr: String): List<String> {
    val parsed = orderStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val result = parsed.toMutableList()
    if (!result.contains(TOOLBAR_ITEM_NOTE_TIMER)) {
        val metricsIndex = result.indexOf(TOOLBAR_ITEM_METRICS)
        if (metricsIndex >= 0) {
            result.add(metricsIndex + 1, TOOLBAR_ITEM_NOTE_TIMER)
        } else {
            result.add(TOOLBAR_ITEM_NOTE_TIMER)
        }
    }
    for (defaultItem in DEFAULT_TOOLBAR_ORDER) {
        if (!result.contains(defaultItem)) {
            result.add(defaultItem)
        }
    }
    return result
}
