package com.example.ui.editor

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import com.example.data.model.DictationMode
import com.example.data.model.DictationState
import com.example.ui.theme.MicErrorRed
import com.example.ui.theme.MicPausedAmber
import com.example.ui.theme.MicRestartingBlue
import com.example.ui.theme.SaffronPrimary

@Composable
fun HorizontalRecordingWaveform(
    audioLevelRms: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Smoothed RMS response to prevent jitter while reacting fluidly
    val smoothedRms by animateFloatAsState(
        targetValue = audioLevelRms.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "waveform_rms"
    )

    // Gentle wave phase for continuous organic breathing
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        if (totalWidth <= 0f || totalHeight <= 0f) return@Canvas

        val centerY = totalHeight / 2f
        val dotDiameter = 4.dp.toPx()
        val baseStep = 10.dp.toPx()

        val count = (totalWidth / baseStep).toInt().coerceAtLeast(7)
        val spacing = (totalWidth - (count * dotDiameter)) / (count - 1).coerceAtLeast(1).toFloat()
        val maxBarHeight = (totalHeight * 0.72f).coerceAtLeast(dotDiameter * 2f)

        for (i in 0 until count) {
            val x = i * (dotDiameter + spacing) + (dotDiameter / 2f)

            // Normalized distance from center (0 at center, 1 at edges)
            val centerIndex = (count - 1) / 2f
            val dist = abs(i - centerIndex) / centerIndex.coerceAtLeast(1f)

            // Bell curve: center is tallest, fades toward edges
            val bellCurve = cos(dist * (Math.PI / 2.0).toFloat()).coerceIn(0f, 1f)

            // Dynamic wave modulation
            val wave = sin(phase + i * 0.45f) * 0.22f
            val dynamicLevel = (smoothedRms + wave * (0.25f + smoothedRms * 0.75f)).coerceIn(0f, 1f)

            val barHeight = (dotDiameter + (maxBarHeight - dotDiameter) * bellCurve * dynamicLevel)
                .coerceIn(dotDiameter, maxBarHeight)

            val alpha = (0.35f + 0.65f * bellCurve).coerceIn(0.2f, 1f)
            val color = accentColor.copy(alpha = alpha)

            if (barHeight <= dotDiameter * 1.2f) {
                drawCircle(
                    color = color,
                    radius = dotDiameter / 2f,
                    center = Offset(x, centerY)
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(x, centerY - barHeight / 2f),
                    end = Offset(x, centerY + barHeight / 2f),
                    strokeWidth = dotDiameter,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorFloatingCluster(
    canUndo: Boolean,
    canRedo: Boolean,
    dictationState: DictationState,
    dictationMode: DictationMode,
    audioLevelRms: Float,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMicClick: () -> Unit,
    onMicLongClick: () -> Unit,
    onModeSelect: (DictationMode) -> Unit,
    onPushToTalkPress: (Boolean) -> Unit,
    onReadingModeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    micButtonSize: String = "MEDIUM",
    micButtonShape: String = "CIRCLE",
    micButtonColor: String = "DEFAULT",
    micButtonPosition: String = "RIGHT",
    micButtonOffsetY: Int = 8,
    dictationAnimationStyle: String = "PULSE_RING",
    hapticsLevel: String = "MEDIUM"
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

    val (btnSize, iconSize, haloSize) = when (micButtonSize.uppercase()) {
        "SMALL" -> Triple(48.dp, 22.dp, 58.dp)
        "LARGE" -> Triple(70.dp, 32.dp, 82.dp)
        "EXTRA_LARGE", "XL" -> Triple(82.dp, 38.dp, 96.dp)
        else -> Triple(60.dp, 28.dp, 72.dp)
    }

    val buttonShape: Shape = when (micButtonShape.uppercase()) {
        "SQUIRCLE" -> RoundedCornerShape(18.dp)
        "SQUARE" -> RoundedCornerShape(8.dp)
        else -> CircleShape
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
        label = "mic_color"
    )

    val editClusterContent = @Composable {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shadowElevation = 6.dp,
            modifier = Modifier.testTag("floating_edit_cluster")
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo
                FilledIconButton(
                    onClick = {
                        triggerHaptic()
                        onUndoClick()
                    },
                    enabled = canUndo,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("undo_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Redo
                FilledIconButton(
                    onClick = {
                        triggerHaptic()
                        onRedoClick()
                    },
                    enabled = canRedo,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("redo_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Copy (smoothly hides during active recording to give waveform maximum space)
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
                    exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150))
                ) {
                    FilledIconButton(
                        onClick = {
                            triggerHaptic()
                            onCopyClick()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("copy_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all text",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    val micFabContent = @Composable {
        Box(
            modifier = Modifier.testTag("mic_fab_container"),
            contentAlignment = Alignment.Center
        ) {
            // Animated pulsing halo ring
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val baseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            val dynamicScale = if (isListening) {
                baseScale + (audioLevelRms * 0.3f)
            } else if (isStarting || isRestarting) {
                baseScale
            } else {
                1f
            }

            // Waveform / Halo Animation around voice input button
            val showHalo = (isListening || isStarting || isRestarting) &&
                    (dictationAnimationStyle.uppercase() == "PULSE_RING" || dictationAnimationStyle.uppercase() == "GLOW")

            if (showHalo) {
                val alpha = if (dictationAnimationStyle.uppercase() == "GLOW") 0.45f else 0.25f
                Box(
                    modifier = Modifier
                        .size(haloSize)
                        .scale(dynamicScale)
                        .clip(buttonShape)
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
                shape = buttonShape,
                color = micColor,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(btnSize)
                    .clip(buttonShape)
                    .then(pttModifier)
                    .testTag("mic_fab")
            ) {
                Box(
                    modifier = Modifier.size(btnSize),
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
                        modifier = Modifier.size(iconSize)
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

    val readingModeButtonContent = @Composable {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shadowElevation = 6.dp,
            modifier = Modifier.testTag("floating_reading_mode_container")
        ) {
            Box(
                modifier = Modifier.padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                FilledIconButton(
                    onClick = {
                        triggerHaptic()
                        onReadingModeClick()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("reading_mode_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Reading Mode",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    val position = micButtonPosition.uppercase()

    // Single unified horizontal row: [undo] [redo] [ horizontal waveform / Reading Mode button ] [Record button]
    // All buttons and waveform vertically centered on the exact same horizontal axis
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = micButtonOffsetY.coerceAtLeast(0).dp,
                top = 4.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (position == "LEFT") {
            micFabContent()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        HorizontalRecordingWaveform(
                            audioLevelRms = audioLevelRms,
                            accentColor = micColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    readingModeButtonContent()
                }
            }
            editClusterContent()
        } else {
            // Default & Center: Left controls, Waveform or Reading Mode centered in gap, Mic on right
            editClusterContent()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        HorizontalRecordingWaveform(
                            audioLevelRms = audioLevelRms,
                            accentColor = micColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    readingModeButtonContent()
                }
            }
            micFabContent()
        }
    }
}
