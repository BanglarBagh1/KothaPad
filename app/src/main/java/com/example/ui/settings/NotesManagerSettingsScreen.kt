package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesManagerSettingsScreen(
    newNoteLocationMode: String,
    fixedNewNoteFolder: String,
    askBeforeCreating: Boolean,
    askChooseTemplateToggle: Boolean,
    askStartRecordingToggle: Boolean,
    newNoteNamingRule: String,
    afterCreatingNote: String,
    availableFolders: List<String>,
    onNewNoteLocationModeChange: (String) -> Unit,
    onFixedNewNoteFolderChange: (String) -> Unit,
    onAskBeforeCreatingChange: (Boolean) -> Unit,
    onAskChooseTemplateToggleChange: (Boolean) -> Unit,
    onAskStartRecordingToggleChange: (Boolean) -> Unit,
    onNewNoteNamingRuleChange: (String) -> Unit,
    onAfterCreatingNoteChange: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    onBackClick: () -> Unit,
    showNoteMetrics: Boolean = true,
    noteMetricsStyle: String = "COMPACT",
    noteMetricsOrder: String = com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING,
    noteMetricsHiddenItems: Set<String> = emptySet(),
    trashAutoEmptyDays: Int = 7,
    onShowNoteMetricsChange: (Boolean) -> Unit = {},
    onNoteMetricsStyleChange: (String) -> Unit = {},
    onMoveNoteMetricItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleNoteMetricVisibility: (String) -> Unit = {},
    onResetNoteMetricsOrder: () -> Unit = {},
    onUpdateTrashDays: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFolderPickerDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var expandedSectionIds by remember { mutableStateOf(setOf("new_note_section", "list_metrics_section", "trash_retention_section")) }

    val sections = remember(
        newNoteLocationMode, fixedNewNoteFolder, askBeforeCreating,
        askChooseTemplateToggle, askStartRecordingToggle, newNoteNamingRule,
        afterCreatingNote, showNoteMetrics, noteMetricsStyle, noteMetricsOrder,
        noteMetricsHiddenItems, trashAutoEmptyDays
    ) {
        listOf(
            // Section 1: New Note Configuration
            SettingHeadlineSection(
                id = "new_note_section",
                title = "New Note",
                subtitle = "Configure creation folder, prompts, naming rules & post-action",
                icon = Icons.Outlined.Folder,
                items = buildList {
                    add(
                        SettingItem.ChipGroup(
                            id = "location_mode",
                            title = "Where new notes are created",
                            subtitle = "Default folder location when tapping Plus (+) button",
                            options = listOf(
                                ChipOption("LAST_WORKED_ON", "Last worked-on folder (Default)"),
                                ChipOption("CURRENT_OPEN", "Currently open folder"),
                                ChipOption("FIXED", "A fixed folder I choose ($fixedNewNoteFolder)"),
                                ChipOption("ROOT", "Root / main notes folder"),
                                ChipOption("ASK_POPUP", "Choose destination via popup")
                            ),
                            selectedKey = newNoteLocationMode,
                            onOptionSelected = { key ->
                                onNewNoteLocationModeChange(key)
                                if (key == "FIXED") {
                                    showFolderPickerDialog = true
                                }
                            }
                        )
                    )

                    if (newNoteLocationMode == "FIXED") {
                        add(
                            SettingItem.ButtonRow(
                                id = "change_fixed_folder",
                                title = "Selected Fixed Folder: $fixedNewNoteFolder",
                                subtitle = "Tap to choose a different folder for new notes",
                                buttons = listOf(
                                    SettingButtonOption(
                                        label = "Change Folder",
                                        onClick = { showFolderPickerDialog = true }
                                    )
                                )
                            )
                        )
                    }

                    add(
                        SettingItem.Switch(
                            id = "ask_before_creating",
                            title = "Ask before creating popup",
                            subtitle = "When ON, tapping Plus (+) shows a creation dialog instead of creating instantly",
                            checked = askBeforeCreating,
                            onCheckedChange = onAskBeforeCreatingChange
                        )
                    )

                    if (askBeforeCreating) {
                        add(
                            SettingItem.Switch(
                                id = "ask_template_toggle",
                                title = "Optional extra: Choose a template",
                                subtitle = "Allow picking a template from the Templates folder inside the creation popup",
                                checked = askChooseTemplateToggle,
                                onCheckedChange = onAskChooseTemplateToggleChange
                            )
                        )
                        add(
                            SettingItem.Switch(
                                id = "ask_recording_toggle",
                                title = "Optional extra: Start recording immediately",
                                subtitle = "Allow starting dictation automatically upon note creation",
                                checked = askStartRecordingToggle,
                                onCheckedChange = onAskStartRecordingToggleChange
                            )
                        )
                    }

                    add(
                        SettingItem.ChipGroup(
                            id = "naming_rule",
                            title = "New note naming rule",
                            subtitle = "How the title of a newly created note is generated",
                            options = listOf(
                                ChipOption("UNTITLED", "Untitled note"),
                                ChipOption("DATE_TIME", "Date and time (e.g. 2026-10-13 21:40)"),
                                ChipOption("DATE_ONLY", "Date only (e.g. 2026-10-13)"),
                                ChipOption("FIRST_LINE", "First line of note (auto-title)"),
                                ChipOption("ASK_ME", "Ask me (use popup)")
                            ),
                            selectedKey = newNoteNamingRule,
                            onOptionSelected = onNewNoteNamingRuleChange
                        )
                    )

                    add(
                        SettingItem.ChipGroup(
                            id = "after_creating",
                            title = "After creating a note",
                            subtitle = "Action taken immediately after a new note is created",
                            options = listOf(
                                ChipOption("OPEN_EDITOR", "Open the note in the editor (Default)"),
                                ChipOption("STAY_IN_LIST", "Stay in the Notes Manager list")
                            ),
                            selectedKey = afterCreatingNote,
                            onOptionSelected = onAfterCreatingNoteChange
                        )
                    )
                }
            ),

            // Section 2: Note List & Metrics Display
            SettingHeadlineSection(
                id = "list_metrics_section",
                title = "Note List & Metrics",
                subtitle = "Choose metrics components, serial order, visibility & format under note items",
                icon = Icons.Outlined.Tune,
                items = listOf(
                    SettingItem.Switch(
                        id = "note_metrics_in_list",
                        title = "Show note metrics in list",
                        subtitle = "Displays metrics like word count, time spent, reading time under note titles in Notes Manager",
                        checked = showNoteMetrics,
                        onCheckedChange = onShowNoteMetricsChange
                    ),
                    SettingItem.ChipGroup(
                        id = "note_metrics_style",
                        title = "Note metrics format",
                        subtitle = "Choose between compact abbreviations and detailed descriptions",
                        options = listOf(
                            ChipOption("COMPACT", "Compact (e.g. 120w • ~1m)"),
                            ChipOption("DETAILED", "Detailed (e.g. 120 words • 650 chars • ~1m read)")
                        ),
                        selectedKey = noteMetricsStyle,
                        onOptionSelected = onNoteMetricsStyleChange
                    ),
                    SettingItem.CustomContent(
                        id = "note_metrics_reorder_content",
                        content = {
                            NoteMetricsReorderAndVisibilityControl(
                                metricsOrderStr = noteMetricsOrder,
                                hiddenItems = noteMetricsHiddenItems,
                                metricsStyle = noteMetricsStyle,
                                onMoveItem = onMoveNoteMetricItem,
                                onToggleVisibility = onToggleNoteMetricVisibility,
                                onResetOrder = onResetNoteMetricsOrder
                            )
                        }
                    )
                )
            ),

            // Section 3: Trash & Retention
            SettingHeadlineSection(
                id = "trash_retention_section",
                title = "Trash & Retention",
                subtitle = "Automatic cleanup interval for deleted notes",
                icon = Icons.Outlined.Tune,
                items = listOf(
                    SettingItem.ChipGroup(
                        id = "trash_auto_empty_days",
                        title = "Auto-empty trash",
                        subtitle = "Automatically permanently delete trashed notes older than selected days",
                        options = listOf(
                            ChipOption("0", "Never (Manual empty)"),
                            ChipOption("3", "3 days"),
                            ChipOption("7", "7 days (Default)"),
                            ChipOption("14", "14 days"),
                            ChipOption("30", "30 days")
                        ),
                        selectedKey = trashAutoEmptyDays.toString(),
                        onOptionSelected = { it.toIntOrNull()?.let(onUpdateTrashDays) }
                    )
                )
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notes Manager Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = sections, key = { it.id }) { section ->
                val isExpanded = expandedSectionIds.contains(section.id)
                HeadlineCard(
                    section = section,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedSectionIds = if (isExpanded) {
                            expandedSectionIds - section.id
                        } else {
                            expandedSectionIds + section.id
                        }
                    }
                )
            }

            // Reset Button at Bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Reset Notes Manager settings to default", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Folder Picker Dialog for Fixed Folder
    if (showFolderPickerDialog) {
        AlertDialog(
            onDismissRequest = { showFolderPickerDialog = false },
            title = { Text("Select Fixed Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val folders = (listOf("General", "Templates") + availableFolders).distinct()
                    folders.forEach { folder ->
                        TextButton(
                            onClick = {
                                onFixedNewNoteFolderChange(folder)
                                showFolderPickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = folder,
                                fontWeight = if (folder == fixedNewNoteFolder) FontWeight.Bold else FontWeight.Normal,
                                color = if (folder == fixedNewNoteFolder) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all Notes Manager settings to their default values?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToDefaults()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
