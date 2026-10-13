package com.example.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeyActionType
import com.example.data.model.KeyboardKey
import com.example.data.model.KeyboardLayout
import com.example.ui.theme.SaffronPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun InAppKeyboard(
    layout: KeyboardLayout,
    keyHeightDp: Int = 40,
    fontSizeSp: Int = 15,
    hapticEnabled: Boolean = true,
    holdBehavior: String = "CUSTOMIZE", // "CUSTOMIZE" or "REPEAT"
    isEditMode: Boolean = false,
    onKeyAction: (KeyboardKey) -> Unit,
    onKeyEditRequest: (KeyboardKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("in_app_keyboard_3x10"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Exactly 3 rows x 10 columns
            for (rowIndex in 0 until minOf(3, layout.rows.size)) {
                val row = layout.rows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (colIndex in 0 until minOf(10, row.keys.size)) {
                        val key = row.keys[colIndex]
                        InAppGridKey(
                            key = key,
                            keyHeightDp = keyHeightDp,
                            fontSizeSp = fontSizeSp,
                            hapticEnabled = hapticEnabled,
                            holdBehavior = holdBehavior,
                            isEditMode = isEditMode,
                            modifier = Modifier.weight(1f),
                            onClick = { onKeyAction(key) },
                            onEdit = { onKeyEditRequest(key) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InAppGridKey(
    key: KeyboardKey,
    keyHeightDp: Int,
    fontSizeSp: Int,
    hapticEnabled: Boolean,
    holdBehavior: String,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val isControlKey = key.actionType != KeyActionType.INSERT && key.actionType != KeyActionType.SPACE
    val keyBgColor = when {
        isEditMode -> SaffronPrimary.copy(alpha = 0.15f)
        isControlKey -> MaterialTheme.colorScheme.surfaceVariant
        key.actionType == KeyActionType.SPACE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.background
    }

    val textColor = when (key.actionType) {
        KeyActionType.CHANGE_LANGUAGE -> SaffronPrimary
        KeyActionType.OPEN_TEMPLATES -> SaffronPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(keyHeightDp.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(keyBgColor)
            .combinedClickable(
                onClick = {
                    if (hapticEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    if (isEditMode) {
                        onEdit()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (hapticEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    if (holdBehavior == "REPEAT" && (key.actionType == KeyActionType.BACKSPACE ||
                                key.actionType == KeyActionType.CURSOR_LEFT ||
                                key.actionType == KeyActionType.CURSOR_RIGHT ||
                                key.actionType == KeyActionType.CURSOR_UP ||
                                key.actionType == KeyActionType.CURSOR_DOWN)) {
                        // Repeat action in hold behavior
                        onClick()
                    } else {
                        // Default hold behavior: Customize key
                        onEdit()
                    }
                }
            )
            .testTag("key_${key.id.ifEmpty { key.label }}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = if (key.label.length > 2) (fontSizeSp - 3).coerceAtLeast(10).sp else fontSizeSp.sp,
                fontWeight = if (isControlKey || isEditMode) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
