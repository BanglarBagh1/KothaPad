package com.example.markdown

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.SaffronPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownSettingsScreen(
    settings: MarkdownSettings,
    onUpdateSettings: ((MarkdownSettings) -> MarkdownSettings) -> Unit,
    onResetToDefaults: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("markdown_settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Markdown Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("markdown_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AmoledSurfaceVariant.takeIf { it != AmoledSurface }
                        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (settings.markdownEnabled) SaffronPrimary.copy(alpha = 0.8f) else AmoledBorder.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("master_markdown_toggle_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Markdown in Reading Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.markdownEnabled) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.markdownEnabled) {
                                    "Render notes with rich Obsidian-style Markdown in Reading Mode"
                                } else {
                                    "Markdown rendering is paused; Reading Mode displays plain text"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.markdownEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(markdownEnabled = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SaffronPrimary,
                                checkedTrackColor = SaffronPrimary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("markdown_master_switch")
                        )
                    }
                }
            }

            // Reading Font Scale Slider Card
            item {
                MarkdownGroupCard(title = "Reading View") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reading Font Size Scale",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Scales all text, headings, and tables in Reading Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${"%.1f".format(settings.readingFontScale)}x",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SaffronPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = settings.readingFontScale,
                            onValueChange = { newVal ->
                                val rounded = (newVal * 10f).roundToInt() / 10f
                                onUpdateSettings { it.copy(readingFontScale = rounded) }
                            },
                            valueRange = 0.8f..1.8f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = SaffronPrimary,
                                activeTrackColor = SaffronPrimary
                            ),
                            modifier = Modifier.testTag("reading_font_scale_slider")
                        )
                    }
                }
            }

            // Group: Editor (Phase 2)
            item {
                MarkdownGroupCard(title = "Editor") {
                    MarkdownToggleRow(
                        title = "Live Editor Styling",
                        subtitle = "Format Markdown visually while typing in edit mode",
                        checked = settings.liveStylingEnabled,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(liveStylingEnabled = chk) } },
                        testTag = "toggle_live_styling"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Dim Syntax Symbols",
                        subtitle = "Display symbols like #, **, >, - in muted color",
                        checked = settings.dimSyntaxSymbols,
                        enabled = settings.markdownEnabled && settings.liveStylingEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(dimSyntaxSymbols = chk) } },
                        testTag = "toggle_dim_syntax"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Monospace Font for Code",
                        subtitle = "Use monospace font for inline code and code blocks in editor",
                        checked = settings.codeFontMonospace,
                        enabled = settings.markdownEnabled && settings.liveStylingEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(codeFontMonospace = chk) } },
                        testTag = "toggle_code_monospace"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Auto-continue Lists",
                        subtitle = "Pressing Enter automatically creates the next list or task item",
                        checked = settings.autoContinueLists,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(autoContinueLists = chk) } },
                        testTag = "toggle_auto_continue_lists"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Heading Size Scale in Editor",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (settings.markdownEnabled && settings.liveStylingEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    }
                                )
                                Text(
                                    text = "Scale heading font size in the editor (# to ######)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (settings.markdownEnabled && settings.liveStylingEnabled) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                            }
                            Text(
                                text = "${"%.1f".format(settings.headingSizeScale)}x",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.markdownEnabled && settings.liveStylingEnabled) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = settings.headingSizeScale,
                            onValueChange = { newVal ->
                                val rounded = (newVal * 10f).roundToInt() / 10f
                                onUpdateSettings { it.copy(headingSizeScale = rounded) }
                            },
                            valueRange = 0.8f..1.5f,
                            steps = 6,
                            enabled = settings.markdownEnabled && settings.liveStylingEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = SaffronPrimary,
                                activeTrackColor = SaffronPrimary
                            ),
                            modifier = Modifier.testTag("heading_size_scale_slider")
                        )
                    }
                }
            }

            // Group: Text Styling
            item {
                MarkdownGroupCard(title = "Text Styling") {
                    MarkdownToggleRow(
                        title = "Bold & Italic",
                        subtitle = "**bold**, *italic*, ***both***",
                        checked = settings.boldItalic,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(boldItalic = chk) } },
                        testTag = "toggle_bold_italic"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Strikethrough",
                        subtitle = "~~strikethrough~~",
                        checked = settings.strikethrough,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(strikethrough = chk) } },
                        testTag = "toggle_strikethrough"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Highlight",
                        subtitle = "==highlighted text==",
                        checked = settings.highlight,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(highlight = chk) } },
                        testTag = "toggle_highlight"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Inline Code",
                        subtitle = "`inline code`",
                        checked = settings.inlineCode,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(inlineCode = chk) } },
                        testTag = "toggle_inline_code"
                    )
                }
            }

            // Group: Blocks
            item {
                MarkdownGroupCard(title = "Blocks") {
                    MarkdownToggleRow(
                        title = "Headings",
                        subtitle = "# Heading 1 through ###### Heading 6",
                        checked = settings.headings,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(headings = chk) } },
                        testTag = "toggle_headings"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Fenced Code Blocks",
                        subtitle = "```language ... ``` formatted code box",
                        checked = settings.codeBlocks,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(codeBlocks = chk) } },
                        testTag = "toggle_code_blocks"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Blockquotes",
                        subtitle = "> quoted lines with vertical accent bar",
                        checked = settings.blockquotes,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(blockquotes = chk) } },
                        testTag = "toggle_blockquotes"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Horizontal Rule",
                        subtitle = "---, ***, or ___ divider lines",
                        checked = settings.horizontalRule,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(horizontalRule = chk) } },
                        testTag = "toggle_horizontal_rule"
                    )
                }
            }

            // Group: Lists & Tasks
            item {
                MarkdownGroupCard(title = "Lists & Tasks") {
                    MarkdownToggleRow(
                        title = "Bullet & Numbered Lists",
                        subtitle = "- bullet, 1. numbered, with nested indentations",
                        checked = settings.lists,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(lists = chk) } },
                        testTag = "toggle_lists"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Task Lists",
                        subtitle = "- [ ] unchecked and - [x] completed tasks",
                        checked = settings.taskLists,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(taskLists = chk) } },
                        testTag = "toggle_task_lists"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Tappable Checkboxes in Reading Mode",
                        subtitle = "Tap checkboxes directly in Reading Mode to toggle complete state and save note",
                        checked = settings.tappableCheckboxes,
                        enabled = settings.markdownEnabled && settings.taskLists,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(tappableCheckboxes = chk) } },
                        testTag = "toggle_tappable_checkboxes"
                    )
                }
            }

            // Group: Links & Tables
            item {
                MarkdownGroupCard(title = "Links & Tables") {
                    MarkdownToggleRow(
                        title = "Links",
                        subtitle = "[anchor text](url) and auto-linked bare URLs",
                        checked = settings.links,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(links = chk) } },
                        testTag = "toggle_links"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    MarkdownToggleRow(
                        title = "Pipe Tables",
                        subtitle = "| col | col | with horizontal scrolling",
                        checked = settings.tables,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(tables = chk) } },
                        testTag = "toggle_tables"
                    )
                }
            }

            // Group: Format Bar (Phase 3)
            item {
                MarkdownGroupCard(title = "Format Bar") {
                    MarkdownToggleRow(
                        title = "Quick Format Bar",
                        subtitle = "Toolbar of Markdown syntax buttons above keyboard while editing",
                        checked = settings.formatBarEnabled,
                        enabled = settings.markdownEnabled,
                        onCheckedChange = { chk -> onUpdateSettings { it.copy(formatBarEnabled = chk) } },
                        testTag = "toggle_format_bar"
                    )
                    HorizontalDivider(color = AmoledBorder.copy(alpha = 0.3f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Format Buttons",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (settings.markdownEnabled && settings.formatBarEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        onUpdateSettings { it.copy(formatBarButtons = MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS) }
                                    },
                                    enabled = settings.markdownEnabled && settings.formatBarEnabled
                                ) {
                                    Text("All", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = {
                                        onUpdateSettings { it.copy(formatBarButtons = emptyList()) }
                                    },
                                    enabled = settings.markdownEnabled && settings.formatBarEnabled
                                ) {
                                    Text("None", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose which syntax buttons appear in the quick format bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.markdownEnabled && settings.formatBarEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val allButtons = remember { getFormatBarButtonDefinitions() }
                        allButtons.forEach { btn ->
                            val isChecked = settings.formatBarButtons.contains(btn.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(
                                        enabled = settings.markdownEnabled && settings.formatBarEnabled,
                                        onClick = {
                                            val newButtons = if (isChecked) {
                                                settings.formatBarButtons - btn.id
                                            } else {
                                                MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS.filter { id ->
                                                    id == btn.id || settings.formatBarButtons.contains(id)
                                                }
                                            }
                                            onUpdateSettings { it.copy(formatBarButtons = newButtons) }
                                        }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val newButtons = if (checked) {
                                            MarkdownSettings.DEFAULT_FORMAT_BAR_BUTTONS.filter { id ->
                                                id == btn.id || settings.formatBarButtons.contains(id)
                                            }
                                        } else {
                                            settings.formatBarButtons - btn.id
                                        }
                                        onUpdateSettings { it.copy(formatBarButtons = newButtons) }
                                    },
                                    enabled = settings.markdownEnabled && settings.formatBarEnabled,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SaffronPrimary
                                    ),
                                    modifier = Modifier.testTag("format_bar_check_${btn.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = btn.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (settings.markdownEnabled && settings.formatBarEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = btn.contentDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (settings.markdownEnabled && settings.formatBarEnabled) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Reset Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_markdown_settings_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Reset Markdown settings to default", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Markdown Settings") },
            text = { Text("Are you sure you want to reset all Markdown toggles and font scale to their default values?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MarkdownGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = SaffronPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AmoledSurfaceVariant.takeIf { it != AmoledSurface }
                ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, AmoledBorder.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun MarkdownToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SaffronPrimary,
                checkedTrackColor = SaffronPrimary.copy(alpha = 0.4f)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
