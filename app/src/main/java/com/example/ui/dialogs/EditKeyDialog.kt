package com.example.ui.dialogs

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.KeyActionType
import com.example.data.model.KeyboardKey
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKeyDialog(
    key: KeyboardKey,
    onSaveKey: (KeyboardKey) -> Unit,
    onResetKey: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var label by remember { mutableStateOf(key.label) }
    var selectedAction by remember { mutableStateOf(key.actionType) }
    var outputText by remember { mutableStateOf(key.outputText ?: "") }
    var smartSpacing by remember { mutableStateOf(key.smartSpacing) }
    var dateFormat by remember { mutableStateOf(key.dateFormat ?: "dd MMM yyyy") }
    var timeFormat by remember { mutableStateOf(key.timeFormat ?: "hh:mm a") }

    var actionDropdownExpanded by remember { mutableStateOf(false) }

    val allActions = remember {
        listOf(
            KeyActionType.INSERT to "Insert text",
            KeyActionType.CURSOR_LEFT to "Cursor Left",
            KeyActionType.CURSOR_RIGHT to "Cursor Right",
            KeyActionType.CURSOR_UP to "Cursor Up",
            KeyActionType.CURSOR_DOWN to "Cursor Down",
            KeyActionType.WORD_LEFT to "Word Left",
            KeyActionType.WORD_RIGHT to "Word Right",
            KeyActionType.LINE_START to "Line Start",
            KeyActionType.LINE_END to "Line End",
            KeyActionType.BACKSPACE to "Backspace",
            KeyActionType.DELETE_WORD to "Delete word",
            KeyActionType.ENTER to "Enter",
            KeyActionType.NEW_PARAGRAPH to "New Paragraph (¶)",
            KeyActionType.SPACE to "Space",
            KeyActionType.OPEN_TEMPLATES to "Open Templates (📋)",
            KeyActionType.CHANGE_LANGUAGE to "Change Language (🌐)",
            KeyActionType.UNDO to "Undo",
            KeyActionType.REDO to "Redo",
            KeyActionType.SELECT_ALL to "Select All",
            KeyActionType.PASTE to "Paste",
            KeyActionType.TOGGLE_SYSTEM_KEYBOARD to "Toggle System Keyboard"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_key_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    text = "Edit key",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Label field
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_key_label_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Action dropdown
                ExposedDropdownMenuBox(
                    expanded = actionDropdownExpanded,
                    onExpandedChange = { actionDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = allActions.find { it.first == selectedAction }?.second ?: selectedAction.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("edit_key_action_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = actionDropdownExpanded,
                        onDismissRequest = { actionDropdownExpanded = false }
                    ) {
                        for ((action, name) in allActions) {
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedAction = action
                                    actionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Multi-line Text to Insert (relevant for INSERT action)
                if (selectedAction == KeyActionType.INSERT) {
                    OutlinedTextField(
                        value = outputText,
                        onValueChange = { outputText = it },
                        label = { Text("Text to insert") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("edit_key_text_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Current (Dynamic) Date & Time section
                    Text(
                        text = "Current (Dynamic) Date & Time",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SaffronPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tokenButtons = listOf(
                            "DATE" to "{date}",
                            "TIME" to "{time}",
                            "DATETIME" to "{datetime}",
                            "WEEKDAY" to "{weekday}"
                        )
                        for ((btnText, token) in tokenButtons) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        outputText += token
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = btnText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SaffronPrimary
                                )
                            }
                        }
                    }

                    // Optional date and time format fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dateFormat,
                            onValueChange = { dateFormat = it },
                            label = { Text("Date format") },
                            placeholder = { Text("dd MMM yyyy") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        OutlinedTextField(
                            value = timeFormat,
                            onValueChange = { timeFormat = it },
                            label = { Text("Time format") },
                            placeholder = { Text("hh:mm a") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SaffronPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Smart spacing toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart spacing",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "Removes space before, adds space after mark",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Switch(
                            checked = smartSpacing,
                            onCheckedChange = { smartSpacing = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                checkedTrackColor = SaffronPrimary
                            )
                        )
                    }
                }

                // Actions: Reset key, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onResetKey()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset key", color = MaterialTheme.colorScheme.error)
                    }

                    Row {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val updatedKey = key.copy(
                                    label = label.ifBlank { " " },
                                    actionType = selectedAction,
                                    outputText = if (selectedAction == KeyActionType.INSERT) outputText else null,
                                    smartSpacing = smartSpacing,
                                    dateFormat = dateFormat,
                                    timeFormat = timeFormat
                                )
                                onSaveKey(updatedKey)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE")
                        }
                    }
                }
            }
        }
    }
}
