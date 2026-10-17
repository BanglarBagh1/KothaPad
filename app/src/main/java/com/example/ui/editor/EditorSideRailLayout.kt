package com.example.ui.editor

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DictationMode
import com.example.data.model.DictationState
import com.example.data.model.FloatingLayoutSettings
import com.example.markdown.MarkdownFormatBar
import com.example.markdown.MarkdownSettings
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.MicErrorRed
import com.example.ui.theme.MicPausedAmber
import com.example.ui.theme.MicRestartingBlue
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorSideRailLayout(
    settings: FloatingLayoutSettings,
    dictationState: DictationState,
    dictationMode: DictationMode,
    audioLevelRms: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMicClick: () -> Unit,
    onMicLongClick: () -> Unit,
    onModeSelect: (DictationMode) -> Unit,
    onPushToTalkPress: (Boolean) -> Unit,
    onReadingModeClick: () -> Unit,
    markdownSettings: MarkdownSettings,
    onFormatBarAction: (String) -> Unit,
    isTextFieldFocused: Boolean,
    isKeyboardOpen: Boolean,
    micButtonColor: String = "DEFAULT",
    dictationAnimationStyle: String = "PULSE_RING",
    hapticsLevel: String = "MEDIUM",
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showModeMenu by remember { mutableStateOf(false) }

    fun triggerHaptic(strong: Boolean = false) {
        when (hapticsLevel.uppercase()) {
            "OFF" -> {}
            "LIGHT" -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            "HEAVY" -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            else -> view.performHapticFeedback(
                if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
    }

    val isStarting = dictationState is DictationState.Starting
    val isListening = dictationState is DictationState.Listening
    val isRestarting = dictationState is DictationState.Restarting
    val isPaused = dictationState is DictationState.Paused
    val isError = dictationState is DictationState.Error
    val isRecording = isListening || isStarting

    val defaultStateColor = when {
        isListening || isStarting -> SaffronPrimary
        isRestarting -> MicRestartingBlue
        isPaused -> MicPausedAmber
        isError -> MicErrorRed
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val targetColor = when (micButtonColor.uppercase()) {
        "SAFFRON" -> if (isListening || isStarting) SaffronPrimary else Color(0xFFFF7043)
        "CORAL" -> Color(0xFFFF5252)
        "EMERALD" -> Color(0xFF2E7D32)
        "BLUE" -> Color(0xFF1976D2)
        "PURPLE" -> Color(0xFF7B1FA2)
        else -> defaultStateColor
    }

    val micColor by animateColorAsState(
        targetValue = targetColor,
        label = "rail_mic_color"
    )

    // Sizing
    val railScale = settings.railScale.coerceIn(0.5f, 1.5f)
    val micBtnSize = (48 * railScale).dp
    val railBtnSize = (40 * railScale).dp
    val railWidth = maxOf(micBtnSize, railBtnSize)
    val railSpacing = settings.railSpacingDp.coerceIn(0, 16).dp

    val toggleScale = settings.toggleScale.coerceIn(0.5f, 1.5f)
    val toggleBtnSize = (40 * toggleScale).dp
    val toggleGap = settings.toggleGapDp.coerceIn(0, 24).dp

    val formatBarScale = settings.formatBarScale.coerceIn(0.5f, 1.5f)
    val waveformScale = settings.waveformScale.coerceIn(0.5f, 1.5f)
    val waveformHeight = (36 * waveformScale).dp

    val showFormatBar = markdownSettings.markdownEnabled &&
            markdownSettings.formatBarEnabled &&
            isTextFieldFocused &&
            isKeyboardOpen &&
            !isRecording

    val screenMargin = 8.dp
    val isRailOnLeft = settings.railSide.uppercase() == "LEFT"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_side_rail_layout")
    ) {
        val totalWidth = maxWidth

        // Calculate available width for format bar and waveform
        // Total available width = screen width - rail width - toggle width - (toggle gaps) - screen margins
        val spaceForBar = (totalWidth - railWidth - toggleBtnSize - (toggleGap * 2) - (screenMargin * 2))
            .coerceAtLeast(60.dp)
        val barWidthFraction = (settings.formatBarWidthPercent.coerceIn(50, 100) / 100f)
        val targetBarWidth = spaceForBar * barWidthFraction

        // Horizontal alignment of format bar / waveform within spaceForBar
        val barAlignmentFactor = when (settings.formatBarAlignment.uppercase()) {
            "CENTER" -> 0.5f
            "RIGHT" -> 1.0f
            else -> 0f // LEFT
        }
        val barExtraShift = (spaceForBar - targetBarWidth) * barAlignmentFactor

        // Side Rail Buttons composables
        val undoButton = @Composable {
            FilledIconButton(
                onClick = {
                    triggerHaptic()
                    onUndoClick()
                },
                enabled = canUndo,
                modifier = Modifier
                    .size(railBtnSize)
                    .testTag("undo_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size((18 * railScale).dp)
                )
            }
        }

        val redoButton = @Composable {
            FilledIconButton(
                onClick = {
                    triggerHaptic()
                    onRedoClick()
                },
                enabled = canRedo,
                modifier = Modifier
                    .size(railBtnSize)
                    .testTag("redo_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    modifier = Modifier.size((18 * railScale).dp)
                )
            }
        }

        val copyButton = @Composable {
            FilledIconButton(
                onClick = {
                    triggerHaptic()
                    onCopyClick()
                },
                modifier = Modifier
                    .size(railBtnSize)
                    .testTag("copy_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy all text",
                    modifier = Modifier.size((18 * railScale).dp)
                )
            }
        }

        val micButton = @Composable {
            Box(
                modifier = Modifier
                    .size(micBtnSize)
                    .testTag("mic_fab_container"),
                contentAlignment = Alignment.Center
            ) {
                // Animated pulsing halo ring confined within mic bounds + small margin so it never overlaps neighboring rail buttons
                val infiniteTransition = rememberInfiniteTransition(label = "rail_pulse")
                val basePulse by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rail_pulse_scale"
                )

                val dynamicScale = if (isListening) {
                    basePulse + (audioLevelRms.coerceIn(0f, 1f) * 0.08f)
                } else if (isStarting || isRestarting) {
                    basePulse
                } else {
                    1f
                }

                val showHalo = (isListening || isStarting || isRestarting) &&
                        (dictationAnimationStyle.uppercase() == "PULSE_RING" || dictationAnimationStyle.uppercase() == "GLOW")

                if (showHalo) {
                    val alpha = if (dictationAnimationStyle.uppercase() == "GLOW") 0.4f else 0.25f
                    Box(
                        modifier = Modifier
                            .size(micBtnSize + (4 * railScale).dp)
                            .scale(dynamicScale)
                            .clip(CircleShape)
                            .background(micColor.copy(alpha = alpha))
                    )
                }

                // Main Mic Button
                val pttModifier = if (dictationMode == DictationMode.PUSH_TO_TALK) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                triggerHaptic(strong = true)
                                onPushToTalkPress(true)
                                tryAwaitRelease()
                                onPushToTalkPress(false)
                            }
                        )
                    }
                } else {
                    Modifier.combinedClickable(
                        onClick = {
                            triggerHaptic()
                            onMicClick()
                        },
                        onLongClick = {
                            triggerHaptic(strong = true)
                            showModeMenu = true
                            onMicLongClick()
                        }
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = micColor,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, AmoledBorder.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .size(micBtnSize)
                        .clip(CircleShape)
                        .then(pttModifier)
                        .testTag("mic_fab")
                ) {
                    Box(
                        modifier = Modifier.size(micBtnSize),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            isListening || isStarting -> Icons.Default.Mic
                            isRestarting -> Icons.Default.Refresh
                            isPaused -> Icons.Default.Pause
                            isError -> Icons.Default.Warning
                            else -> Icons.Default.MicOff
                        }

                        val iconTint = if (isListening || isStarting || isRestarting || isPaused || isError) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = "Dictation Mic ($dictationMode)",
                            tint = iconTint,
                            modifier = Modifier.size((22 * railScale).dp)
                        )
                    }
                }

                // Long-press Mode Menu
                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false }
                ) {
                    Text(
                        text = "Dictation Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    for (mode in DictationMode.values()) {
                        val isSelected = mode == dictationMode
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Text("✓", color = SaffronPrimary, fontSize = 14.sp)
                                    }
                                }
                            },
                            onClick = {
                                showModeMenu = false
                                onModeSelect(mode)
                            }
                        )
                    }
                }
            }
        }

        // Reading/Edit Toggle Button (Relocated on the bottom row between format bar and rail)
        val readingEditToggleButton = @Composable {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, AmoledBorder.copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(toggleBtnSize)
                    .testTag("reading_mode_button_container")
            ) {
                FilledIconButton(
                    onClick = {
                        triggerHaptic()
                        onReadingModeClick()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reading_mode_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Reading Mode",
                        modifier = Modifier.size((19 * toggleScale).dp)
                    )
                }
            }
        }

        // 1. SIDE RAIL COLUMN (Slim vertical column of 4 buttons at screen edge, bottom-aligned with bottom row)
        // Ordered according to settings.railButtonOrder (top to bottom)
        Box(
            modifier = Modifier
                .align(if (isRailOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(
                    start = if (isRailOnLeft) screenMargin else 0.dp,
                    end = if (!isRailOnLeft) screenMargin else 0.dp,
                    bottom = settings.railBottomOffsetDp.coerceIn(0, 200).dp
                )
                .width(railWidth)
        ) {
            Surface(
                shape = RoundedCornerShape((22 * railScale).dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, AmoledBorder.copy(alpha = 0.45f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .width(railWidth)
                    .testTag("floating_side_rail")
            ) {
                Column(
                    modifier = Modifier
                        .padding((4 * railScale).dp),
                    verticalArrangement = Arrangement.spacedBy(railSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    settings.railButtonOrder.forEach { buttonId ->
                        Box(
                            modifier = Modifier.size(railWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            when (buttonId) {
                                FloatingLayoutSettings.BUTTON_MIC -> micButton()
                                FloatingLayoutSettings.BUTTON_UNDO -> undoButton()
                                FloatingLayoutSettings.BUTTON_REDO -> redoButton()
                                FloatingLayoutSettings.BUTTON_COPY -> copyButton()
                            }
                        }
                    }
                }
            }
        }

        // 2. TOGGLE BUTTON (Placed on the bottom row, between format bar and rail)
        val toggleLeftPadding = if (isRailOnLeft) {
            screenMargin + railWidth + toggleGap
        } else {
            0.dp
        }
        val toggleRightPadding = if (!isRailOnLeft) {
            screenMargin + railWidth + toggleGap
        } else {
            0.dp
        }

        Box(
            modifier = Modifier
                .align(if (isRailOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(
                    start = toggleLeftPadding,
                    end = toggleRightPadding,
                    bottom = settings.toggleBottomOffsetDp.coerceIn(0, 200).dp
                )
        ) {
            readingEditToggleButton()
        }

        // 3. FORMAT BAR OR WAVEFORM AREA
        // Sits on the bottom row opposite the rail, or directly above format bar area while recording
        val barAreaLeftPadding = if (isRailOnLeft) {
            screenMargin + railWidth + (toggleGap * 2) + toggleBtnSize + barExtraShift
        } else {
            screenMargin + barExtraShift
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = barAreaLeftPadding,
                    bottom = settings.formatBarBottomOffsetDp.coerceIn(0, 200).dp
                )
                .width(targetBarWidth)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // While recording: show live waveform in a pill directly above the format bar
                // (or if format bar is hidden, in its slot above the bottom row)
                if (isRecording && settings.showWaveform) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, AmoledBorder.copy(alpha = 0.45f)),
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(waveformHeight)
                            .testTag("floating_live_waveform_pill")
                    ) {
                        HorizontalRecordingWaveform(
                            audioLevelRms = audioLevelRms,
                            accentColor = micColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        )
                    }
                }

                // Markdown Format Bar (shown only when focused, keyboard open, not recording)
                if (showFormatBar) {
                    MarkdownFormatBar(
                        enabledButtonIds = markdownSettings.formatBarButtons,
                        onAction = onFormatBarAction,
                        scale = formatBarScale,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
