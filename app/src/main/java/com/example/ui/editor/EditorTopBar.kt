package com.example.ui.editor

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LanguageItem
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorTopBar(
    noteTitle: String,
    currentLanguageCode: String,
    isKeyboardEditMode: Boolean = false,
    onMenuClick: () -> Unit,
    onTitleClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onLanguageLongClick: () -> Unit,
    onShareClick: () -> Unit,
    onNewNoteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleKeyboardEditMode: () -> Unit = {},
    onClearNoteClick: () -> Unit,
    onDeleteNoteClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    val langItem = remember(currentLanguageCode) {
        LanguageItem.findByCode(currentLanguageCode)
    }

    TopAppBar(
        modifier = modifier.testTag("editor_top_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onMenuClick()
                },
                modifier = Modifier.testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open notes drawer"
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .combinedClickable(
                        onClick = onTitleClick
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = noteTitle.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            // Language Flag Button (Tap = picker, Long-press = cycle favorites)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onLanguageClick()
                        },
                        onLongClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onLanguageLongClick()
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("language_flag_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = langItem.flagEmoji, fontSize = 14.sp)
                    Text(
                        text = langItem.code.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    )
                }
            }

            // Send/Share Button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onShareClick()
                },
                modifier = Modifier.testTag("share_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Share note",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // New Note Button
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onNewNoteClick()
                },
                modifier = Modifier.testTag("new_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new note"
                )
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.testTag("overflow_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isKeyboardEditMode) "Done editing keyboard" else "Edit keyboard keys",
                                color = if (isKeyboardEditMode) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleKeyboardEditMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            showOverflowMenu = false
                            onSettingsClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Quick Tips & Help") },
                        onClick = {
                            showOverflowMenu = false
                            onHelpClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Note Text") },
                        onClick = {
                            showOverflowMenu = false
                            onClearNoteClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showOverflowMenu = false
                            onDeleteNoteClick()
                        }
                    )
                }
            }
        }
    )
}
