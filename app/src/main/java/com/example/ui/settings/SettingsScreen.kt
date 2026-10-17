package com.example.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.data.backup.BackupSummary
import com.example.data.backup.ParsedBackupData
import com.example.ui.dialogs.FinishSetupDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DictationMode
import com.example.data.model.InsertionMode
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.SaffronPrimary

// ==========================================
// Reusable, Extensible Setting Data Models
// ==========================================

data class ChipOption(
    val key: String,
    val label: String
)

data class SettingButtonOption(
    val label: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

sealed interface SettingItem {
    val id: String

    data class Switch(
        override val id: String,
        val title: String,
        val subtitle: String? = null,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingItem

    data class Slider(
        override val id: String,
        val title: String,
        val subtitle: String? = null,
        val value: Float,
        val valueRange: ClosedFloatingPointRange<Float>,
        val steps: Int = 0,
        val displayValue: String,
        val onValueChange: (Float) -> Unit
    ) : SettingItem

    data class ChipGroup(
        override val id: String,
        val title: String,
        val subtitle: String? = null,
        val options: List<ChipOption>,
        val selectedKey: String,
        val onOptionSelected: (String) -> Unit
    ) : SettingItem

    data class ButtonRow(
        override val id: String,
        val title: String = "",
        val subtitle: String? = null,
        val buttons: List<SettingButtonOption>
    ) : SettingItem

    data class InfoText(
        override val id: String,
        val title: String = "",
        val description: String,
        val badge: String? = null,
        val actionButtonText: String? = null,
        val onActionClick: (() -> Unit)? = null
    ) : SettingItem

    data class CustomContent(
        override val id: String,
        val content: @Composable () -> Unit
    ) : SettingItem
}

data class SettingHeadlineSection(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val items: List<SettingItem>
)

// ==========================================
// Main Settings Screen
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dictationMode: String,
    timedMinutes: Int,
    pauseToleranceSeconds: Int,
    idleAutoStopMinutes: Int,
    muteBeepMethod: String,
    insertionMode: String,
    offlinePreferred: Boolean,
    keepScreenOn: Boolean,
    defaultLanguage: String,
    appTheme: String,
    fontSize: Float,
    lineSpacing: Float,
    fontFamily: String,
    pipelineSpacing: Boolean,
    pipelineAutoCap: Boolean,
    pipelineCommands: Boolean,
    showWordCount: Boolean,
    showNoteMetrics: Boolean = true,
    noteMetricsStyle: String = "COMPACT",
    noteMetricsOrder: String = com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING,
    noteMetricsHiddenItems: Set<String> = emptySet(),
    onShowNoteMetricsChange: (Boolean) -> Unit = {},
    onNoteMetricsStyleChange: (String) -> Unit = {},
    onMoveNoteMetricItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleNoteMetricVisibility: (String) -> Unit = {},
    onResetNoteMetricsOrder: () -> Unit = {},
    // Custom Theme Colors
    customPrimaryColor: Int = 0xFFFF7043.toInt(),
    customBackgroundColor: Int = 0xFF000000.toInt(),
    customSurfaceColor: Int = 0xFF121212.toInt(),
    customTextColor: Int = 0xFFFFFFFF.toInt(),
    customSecondaryColor: Int = 0xFFFF8A65.toInt(),
    onCustomThemeColorsChange: (primary: Int, bg: Int, surface: Int, text: Int, secondary: Int) -> Unit = { _, _, _, _, _ -> },
    // Typography Ext
    customFontName: String = "",
    customFontPath: String? = null,
    letterSpacing: Float = 0f,
    paragraphSpacing: Int = 8,
    textAlignment: String = "LEFT",
    marginDp: Int = 16,
    maxWidthDp: Int = 0,
    onCustomFontChange: (name: String, path: String?) -> Unit = { _, _ -> },
    onLetterSpacingChange: (Float) -> Unit = {},
    onParagraphSpacingChange: (Int) -> Unit = {},
    onTextAlignmentChange: (String) -> Unit = {},
    onMarginDpChange: (Int) -> Unit = {},
    onMaxWidthDpChange: (Int) -> Unit = {},
    // Toolbar Items & Order
    toolbarItemOrder: String = "DICTATION_STATUS,METRICS,KEYBOARD_MODE,TEXT_SIZE,TEXT_STYLE,AUDIO_SOURCE",
    toolbarHiddenItems: Set<String> = emptySet(),
    onMoveToolbarItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleToolbarItemVisibility: (String) -> Unit = {},
    onResetToolbarOrder: () -> Unit = {},
    // Mic Button & Cluster
    micButtonSize: String = "MEDIUM",
    micButtonShape: String = "CIRCLE",
    micButtonColor: String = "DEFAULT",
    micButtonPosition: String = "RIGHT",
    micButtonOffsetY: Int = 8,
    onMicButtonSizeChange: (String) -> Unit = {},
    onMicButtonShapeChange: (String) -> Unit = {},
    onMicButtonColorChange: (String) -> Unit = {},
    onMicButtonPositionChange: (String) -> Unit = {},
    onMicButtonOffsetYChange: (Int) -> Unit = {},
    // Visual Polish, Animation & Haptics
    statusChipStyle: String = "PILL",
    uiDensity: String = "NORMAL",
    cornerRadiusDp: Int = 12,
    dictationAnimationStyle: String = "PULSE_RING",
    hapticsLevel: String = "MEDIUM",
    onStatusChipStyleChange: (String) -> Unit = {},
    onUiDensityChange: (String) -> Unit = {},
    onCornerRadiusDpChange: (Int) -> Unit = {},
    onDictationAnimationStyleChange: (String) -> Unit = {},
    onHapticsLevelChange: (String) -> Unit = {},
    // Keyboard Settings
    keyboardKeyHeightDp: Int = 40,
    keyboardFontSizeSp: Int = 15,
    keyboardHaptic: Boolean = true,
    keyboardHoldBehavior: String = "CUSTOMIZE",
    onKeyboardKeyHeightChange: (Int) -> Unit = {},
    onKeyboardFontSizeChange: (Int) -> Unit = {},
    onKeyboardHapticChange: (Boolean) -> Unit = {},
    onKeyboardHoldBehaviorChange: (String) -> Unit = {},
    onResetKeyboardLayout: () -> Unit = {},
    onExportKeyboardLayout: () -> String = { "" },
    onImportKeyboardLayout: (String) -> Boolean = { false },
    // Callbacks
    onDictationModeChange: (String) -> Unit,
    onTimedMinutesChange: (Int) -> Unit,
    onPauseToleranceChange: (Int) -> Unit,
    onIdleAutoStopChange: (Int) -> Unit,
    onMuteBeepMethodChange: (String) -> Unit,
    onInsertionModeChange: (String) -> Unit,
    onOfflinePreferredChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onAppThemeChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onPipelineSpacingChange: (Boolean) -> Unit,
    onPipelineAutoCapChange: (Boolean) -> Unit,
    onPipelineCommandsChange: (Boolean) -> Unit,
    onShowWordCountChange: (Boolean) -> Unit,
    onOpenNotesManagerSettings: () -> Unit = {},
    onOpenMarkdownSettings: () -> Unit = {},
    // Floating Layout Settings
    floatingLayoutSettings: com.example.data.model.FloatingLayoutSettings = com.example.data.model.FloatingLayoutSettings(),
    onFloatingUseSideRailChange: (Boolean) -> Unit = {},
    onRailSideChange: (String) -> Unit = {},
    onRailScaleChange: (Float) -> Unit = {},
    onRailBottomOffsetChange: (Int) -> Unit = {},
    onRailSpacingChange: (Int) -> Unit = {},
    onRailKeepTextClearChange: (Boolean) -> Unit = {},
    onMoveRailButtonItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onResetRailButtonOrder: () -> Unit = {},
    onToggleScaleChange: (Float) -> Unit = {},
    onToggleBottomOffsetChange: (Int) -> Unit = {},
    onToggleGapChange: (Int) -> Unit = {},
    onFormatBarScaleChange: (Float) -> Unit = {},
    onFormatBarBottomOffsetChange: (Int) -> Unit = {},
    onFormatBarWidthPercentChange: (Int) -> Unit = {},
    onFormatBarAlignmentChange: (String) -> Unit = {},
    onWaveformScaleChange: (Float) -> Unit = {},
    onWaveformShowChange: (Boolean) -> Unit = {},
    onResetFloatingLayout: () -> Unit = {},
    // Backup & Restore Callbacks
    onExportBackup: (Uri, (Result<BackupSummary>) -> Unit) -> Unit = { _, _ -> },
    onValidateBackup: (Uri, (Result<ParsedBackupData>) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackup: (ParsedBackupData, (Result<BackupSummary>) -> Unit) -> Unit = { _, _ -> },
    // Vault Section Callbacks & State
    vaultState: com.example.data.vault.VaultState = com.example.data.vault.VaultState(),
    onSwitchVault: (com.example.data.vault.VaultInfo) -> Unit = {},
    onOpenVaultUri: (Uri) -> Unit = {},
    onCreateNewVault: (Uri, String) -> Unit = { _, _ -> },
    onRemoveRecentVault: (String) -> Unit = {},
    onCloseVault: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {


    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }

    // Backup & Restore states
    var isExporting by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var pendingRestoreData by remember { mutableStateOf<ParsedBackupData?>(null) }
    var showFinishSetupDialog by remember { mutableStateOf(false) }

    // Vault Dialog states and launchers
    var showSwitchVaultDialog by remember { mutableStateOf(false) }
    var showOpenOrNewVaultDialog by remember { mutableStateOf(false) }
    var showRecentVaultsDialog by remember { mutableStateOf(false) }
    var showCreateVaultNameDialog by remember { mutableStateOf(false) }
    var selectedParentUri by remember { mutableStateOf<Uri?>(null) }
    var newVaultNameInput by remember { mutableStateOf("KothaVault") }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onOpenVaultUri(uri)
        }
    }

    val createParentFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedParentUri = uri
            newVaultNameInput = "KothaVault"
            showCreateVaultNameDialog = true
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            onExportBackup(uri) { result ->
                isExporting = false
                result.onSuccess { summary ->
                    Toast.makeText(
                        context,
                        "Backup exported: ${summary.notesCount} notes, ${summary.templatesCount} templates",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        "Export failed: ${err.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isValidating = true
            onValidateBackup(uri) { result ->
                isValidating = false
                result.onSuccess { parsed ->
                    pendingRestoreData = parsed
                }.onFailure { err ->
                    validationError = err.message ?: "Invalid or unsupported backup file"
                }
            }
        }
    }

    // Exactly one section open at a time; starts with ALL collapsed
    var expandedSectionId by remember { mutableStateOf<String?>(null) }

    // Check notification policy access for vibrate once
    val notifManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }
    val needsDndPermission = muteBeepMethod == "VIBRATE_ONCE" &&
            notifManager != null && !notifManager.isNotificationPolicyAccessGranted

    // Construct the fully data-driven sections list
    val sections = remember(
        keyboardKeyHeightDp, keyboardFontSizeSp, keyboardHaptic, keyboardHoldBehavior,
        dictationMode, muteBeepMethod, pauseToleranceSeconds, insertionMode,
        offlinePreferred, keepScreenOn, idleAutoStopMinutes,
        appTheme, fontSize, lineSpacing, fontFamily, showWordCount, showNoteMetrics, noteMetricsStyle, noteMetricsOrder, noteMetricsHiddenItems,
        customPrimaryColor, customBackgroundColor, customSurfaceColor, customTextColor, customSecondaryColor,
        customFontName, customFontPath, letterSpacing, paragraphSpacing, textAlignment, marginDp, maxWidthDp,
        toolbarItemOrder, toolbarHiddenItems,
        micButtonSize, micButtonShape, micButtonColor, micButtonPosition, micButtonOffsetY,
        statusChipStyle, uiDensity, cornerRadiusDp, dictationAnimationStyle, hapticsLevel,
        pipelineSpacing, pipelineAutoCap, pipelineCommands, needsDndPermission,
        floatingLayoutSettings, vaultState
    ) {
        listOf(
            // Vault Section
            SettingHeadlineSection(
                id = "vault_section",
                title = "Vault",
                subtitle = "Active folder where all settings and workspace configurations are stored",
                icon = Icons.Outlined.Folder,
                items = buildList {
                    val curr = vaultState.currentVault
                    val currentPath = if (curr != null) {
                        try {
                            Uri.decode(curr.uriString).substringAfterLast("/tree/").replace("%2F", "/").replace(":", " > ")
                        } catch (_: Exception) {
                            curr.uriString
                        }
                    } else "None"

                    add(
                        SettingItem.InfoText(
                            id = "current_vault_info",
                            title = curr?.name ?: "No vault selected",
                            description = "Location: $currentPath\nStatus: ${if (vaultState.isAccessible) "Active & Synced (.kothapad/settings.json)" else "Inaccessible"}",
                            badge = if (vaultState.isAccessible) "Active" else "Inaccessible"
                        )
                    )

                    add(
                        SettingItem.ButtonRow(
                            id = "vault_actions_row",
                            title = "Vault Controls",
                            subtitle = "Switch, open, or close the workspace vault",
                            buttons = buildList {
                                add(
                                    SettingButtonOption(
                                        label = "Switch Vault",
                                        onClick = { showSwitchVaultDialog = true }
                                    )
                                )
                                add(
                                    SettingButtonOption(
                                        label = "Open / New Vault",
                                        onClick = { showOpenOrNewVaultDialog = true }
                                    )
                                )
                                if (vaultState.recentVaults.isNotEmpty()) {
                                    add(
                                        SettingButtonOption(
                                            label = "Recent Vaults (${vaultState.recentVaults.size})",
                                            onClick = { showRecentVaultsDialog = true }
                                        )
                                    )
                                }
                                add(
                                    SettingButtonOption(
                                        label = "Close Vault",
                                        isDestructive = true,
                                        onClick = onCloseVault
                                    )
                                )
                            }
                        )
                    )
                }
            ),

            // 0. Notes Manager Settings Entry Section
            SettingHeadlineSection(
                id = "notes_manager_settings_entry",
                title = "Notes Manager",
                subtitle = "Where new notes are created, popup dialog & naming rules",
                icon = Icons.Outlined.Folder,
                items = listOf(
                    SettingItem.ButtonRow(
                        id = "open_notes_manager_settings",
                        title = "Notes Manager Settings",
                        subtitle = "Dedicated page for note creation location, templates & naming",
                        buttons = listOf(
                            SettingButtonOption(
                                label = "Open Notes Manager Settings",
                                onClick = onOpenNotesManagerSettings
                            )
                        )
                    )
                )
            ),

            // Markdown Settings Entry Section
            SettingHeadlineSection(
                id = "markdown_settings_entry",
                title = "Markdown",
                subtitle = "Obsidian-style formatting, reading mode rendering & styling",
                icon = Icons.Outlined.Tune,
                items = listOf(
                    SettingItem.ButtonRow(
                        id = "open_markdown_settings",
                        title = "Markdown Settings",
                        subtitle = "Master toggle, per-element switches & reading font scale",
                        buttons = listOf(
                            SettingButtonOption(
                                label = "Open Markdown Settings",
                                onClick = onOpenMarkdownSettings
                            )
                        )
                    )
                )
            ),

            // 1. In-App Keyboard Section
            SettingHeadlineSection(
                id = "keyboard",
                title = "In-App Keyboard",
                subtitle = "3x10 grid size, font, haptics & custom layout",
                icon = Icons.Outlined.Keyboard,
                items = listOf(
                    SettingItem.Slider(
                        id = "key_height",
                        title = "Key Height",
                        value = keyboardKeyHeightDp.toFloat(),
                        valueRange = 30f..56f,
                        steps = 25,
                        displayValue = "$keyboardKeyHeightDp dp",
                        onValueChange = { onKeyboardKeyHeightChange(it.toInt()) }
                    ),
                    SettingItem.Slider(
                        id = "key_font_size",
                        title = "Key Font Size",
                        value = keyboardFontSizeSp.toFloat(),
                        valueRange = 11f..22f,
                        steps = 10,
                        displayValue = "$keyboardFontSizeSp sp",
                        onValueChange = { onKeyboardFontSizeChange(it.toInt()) }
                    ),
                    SettingItem.Switch(
                        id = "keyboard_haptic",
                        title = "Haptic Feedback on Tap",
                        subtitle = "Vibrates lightly on key press",
                        checked = keyboardHaptic,
                        onCheckedChange = onKeyboardHapticChange
                    ),
                    SettingItem.ChipGroup(
                        id = "keyboard_hold_behavior",
                        title = "Hold Behavior",
                        subtitle = "Action when long-pressing a key",
                        options = listOf(
                            ChipOption("CUSTOMIZE", "Customize Key (Default)"),
                            ChipOption("REPEAT", "Repeat Key")
                        ),
                        selectedKey = keyboardHoldBehavior,
                        onOptionSelected = onKeyboardHoldBehaviorChange
                    ),
                    SettingItem.ButtonRow(
                        id = "keyboard_layout_actions",
                        title = "Layout Actions",
                        buttons = listOf(
                            SettingButtonOption(
                                label = "Reset Layout",
                                isDestructive = true,
                                onClick = onResetKeyboardLayout
                            ),
                            SettingButtonOption(
                                label = "Export JSON",
                                onClick = { showExportDialog = true }
                            ),
                            SettingButtonOption(
                                label = "Import JSON",
                                onClick = {
                                    importJsonText = ""
                                    importError = false
                                    showImportDialog = true
                                }
                            )
                        )
                    )
                )
            ),

            // Floating layout Section (placed next to keyboard size setting)
            SettingHeadlineSection(
                id = "floating_layout",
                title = "Floating layout",
                subtitle = "Side rail, button order, format bar & waveform positioning",
                icon = Icons.Outlined.Tune,
                items = listOf(
                    SettingItem.Switch(
                        id = "use_side_rail",
                        title = "Use side rail layout",
                        subtitle = "Slim vertical column of action buttons anchored above keyboard",
                        checked = floatingLayoutSettings.useSideRail,
                        onCheckedChange = onFloatingUseSideRailChange
                    ),
                    SettingItem.ChipGroup(
                        id = "rail_side",
                        title = "Rail: Side",
                        subtitle = "Screen edge position of the vertical rail",
                        options = listOf(
                            ChipOption("RIGHT", "Right (Default)"),
                            ChipOption("LEFT", "Left")
                        ),
                        selectedKey = floatingLayoutSettings.railSide,
                        onOptionSelected = onRailSideChange
                    ),
                    SettingItem.Slider(
                        id = "rail_scale",
                        title = "Rail: Size Scale",
                        value = floatingLayoutSettings.railScale,
                        valueRange = 0.5f..1.5f,
                        steps = 20,
                        displayValue = "${"%.2f".format(floatingLayoutSettings.railScale)}x",
                        onValueChange = onRailScaleChange
                    ),
                    SettingItem.Slider(
                        id = "rail_bottom_offset",
                        title = "Rail: Bottom Offset Above Keyboard",
                        value = floatingLayoutSettings.railBottomOffsetDp.toFloat(),
                        valueRange = 0f..200f,
                        steps = 40,
                        displayValue = "${floatingLayoutSettings.railBottomOffsetDp} dp",
                        onValueChange = { onRailBottomOffsetChange(it.toInt()) }
                    ),
                    SettingItem.Slider(
                        id = "rail_spacing",
                        title = "Rail: Spacing Between Buttons",
                        value = floatingLayoutSettings.railSpacingDp.toFloat(),
                        valueRange = 0f..16f,
                        steps = 16,
                        displayValue = "${floatingLayoutSettings.railSpacingDp} dp",
                        onValueChange = { onRailSpacingChange(it.toInt()) }
                    ),
                    SettingItem.Switch(
                        id = "rail_keep_text_clear",
                        title = "Keep text clear of rail",
                        subtitle = "Reserves editor side margin so text never slides behind the rail",
                        checked = floatingLayoutSettings.keepTextClearOfRail,
                        onCheckedChange = onRailKeepTextClearChange
                    ),
                    SettingItem.CustomContent(
                        id = "rail_button_order_control",
                        content = {
                            RailButtonOrderReorderControl(
                                railButtonOrder = floatingLayoutSettings.railButtonOrder,
                                onMoveItem = onMoveRailButtonItem,
                                onResetOrder = onResetRailButtonOrder
                            )
                        }
                    ),
                    SettingItem.Slider(
                        id = "toggle_scale",
                        title = "Reading/Edit Toggle: Size Scale",
                        value = floatingLayoutSettings.toggleScale,
                        valueRange = 0.5f..1.5f,
                        steps = 20,
                        displayValue = "${"%.2f".format(floatingLayoutSettings.toggleScale)}x",
                        onValueChange = onToggleScaleChange
                    ),
                    SettingItem.Slider(
                        id = "toggle_bottom_offset",
                        title = "Reading/Edit Toggle: Bottom Offset",
                        subtitle = "Offset above active keyboard",
                        value = floatingLayoutSettings.toggleBottomOffsetDp.toFloat(),
                        valueRange = 0f..200f,
                        steps = 40,
                        displayValue = "${floatingLayoutSettings.toggleBottomOffsetDp} dp",
                        onValueChange = { onToggleBottomOffsetChange(it.toInt()) }
                    ),
                    SettingItem.Slider(
                        id = "toggle_gap",
                        title = "Reading/Edit Toggle: Gap to Neighbors",
                        value = floatingLayoutSettings.toggleGapDp.toFloat(),
                        valueRange = 0f..24f,
                        steps = 24,
                        displayValue = "${floatingLayoutSettings.toggleGapDp} dp",
                        onValueChange = { onToggleGapChange(it.toInt()) }
                    ),
                    SettingItem.Slider(
                        id = "format_bar_scale",
                        title = "Format Bar: Size Scale",
                        subtitle = "Scales height, icons and padding together",
                        value = floatingLayoutSettings.formatBarScale,
                        valueRange = 0.5f..1.5f,
                        steps = 20,
                        displayValue = "${"%.2f".format(floatingLayoutSettings.formatBarScale)}x",
                        onValueChange = onFormatBarScaleChange
                    ),
                    SettingItem.Slider(
                        id = "format_bar_bottom_offset",
                        title = "Format Bar: Bottom Offset",
                        value = floatingLayoutSettings.formatBarBottomOffsetDp.toFloat(),
                        valueRange = 0f..200f,
                        steps = 40,
                        displayValue = "${floatingLayoutSettings.formatBarBottomOffsetDp} dp",
                        onValueChange = { onFormatBarBottomOffsetChange(it.toInt()) }
                    ),
                    SettingItem.Slider(
                        id = "format_bar_width",
                        title = "Format Bar: Width",
                        subtitle = "Percentage of available space",
                        value = floatingLayoutSettings.formatBarWidthPercent.toFloat(),
                        valueRange = 50f..100f,
                        steps = 50,
                        displayValue = "${floatingLayoutSettings.formatBarWidthPercent}%",
                        onValueChange = { onFormatBarWidthPercentChange(it.toInt()) }
                    ),
                    SettingItem.ChipGroup(
                        id = "format_bar_alignment",
                        title = "Format Bar: Alignment",
                        subtitle = "Alignment when width is below 100%",
                        options = listOf(
                            ChipOption("LEFT", "Left (Default)"),
                            ChipOption("CENTER", "Center"),
                            ChipOption("RIGHT", "Right")
                        ),
                        selectedKey = floatingLayoutSettings.formatBarAlignment,
                        onOptionSelected = onFormatBarAlignmentChange
                    ),
                    SettingItem.Slider(
                        id = "waveform_scale",
                        title = "Waveform: Size Scale",
                        subtitle = "Scales height of live dictation waveform",
                        value = floatingLayoutSettings.waveformScale,
                        valueRange = 0.5f..1.5f,
                        steps = 20,
                        displayValue = "${"%.2f".format(floatingLayoutSettings.waveformScale)}x",
                        onValueChange = onWaveformScaleChange
                    ),
                    SettingItem.Switch(
                        id = "waveform_show",
                        title = "Show waveform",
                        subtitle = "Live audio wave animation while recording",
                        checked = floatingLayoutSettings.showWaveform,
                        onCheckedChange = onWaveformShowChange
                    ),
                    SettingItem.ButtonRow(
                        id = "reset_floating_layout_row",
                        title = "Reset Floating Layout",
                        subtitle = "Restore all layout sizes, positions, scales and order to defaults",
                        buttons = listOf(
                            SettingButtonOption(
                                label = "Reset layout to defaults",
                                isDestructive = true,
                                onClick = onResetFloatingLayout
                            )
                        )
                    )
                )
            ),

            // 2. Dictation Engine Section
            SettingHeadlineSection(
                id = "dictation",
                title = "Dictation Engine",
                subtitle = "Continuous speech, timeouts, beep muting & insertion",
                icon = Icons.Outlined.Mic,
                items = buildList {
                    add(
                        SettingItem.ChipGroup(
                            id = "dictation_mode",
                            title = "Default Dictation Mode",
                            subtitle = "Dictation behavior on mic tap",
                            options = DictationMode.values().map {
                                ChipOption(it.name, it.displayName)
                            },
                            selectedKey = dictationMode,
                            onOptionSelected = onDictationModeChange
                        )
                    )
                    add(
                        SettingItem.ChipGroup(
                            id = "mute_beep_method",
                            title = "Mute Recognizer Beep",
                            subtitle = "Method to eliminate repetitive recognition beeps across sessions",
                            options = listOf(
                                ChipOption("MEDIA_SYSTEM", "Media + System streams (Default)"),
                                ChipOption("OFF", "Off"),
                                ChipOption("VIBRATE_ONCE", "Vibrate mode once per session")
                            ),
                            selectedKey = muteBeepMethod,
                            onOptionSelected = onMuteBeepMethodChange
                        )
                    )
                    if (muteBeepMethod == "VIBRATE_ONCE") {
                        add(
                            SettingItem.InfoText(
                                id = "vibrate_once_info",
                                title = "Vibrate Mode",
                                description = "Phone vibrates once when dictation starts." +
                                        if (needsDndPermission) " Requires Do Not Disturb / Notification Policy access." else "",
                                actionButtonText = if (needsDndPermission) "Grant Policy Access" else null,
                                onActionClick = if (needsDndPermission) {
                                    {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                } else null
                            )
                        )
                    }
                    add(
                        SettingItem.Slider(
                            id = "pause_tolerance",
                            title = "Pause Tolerance",
                            subtitle = "Max silence duration before session auto-restarts",
                            value = pauseToleranceSeconds.toFloat(),
                            valueRange = 5f..120f,
                            steps = 22,
                            displayValue = "$pauseToleranceSeconds s",
                            onValueChange = { onPauseToleranceChange(it.toInt()) }
                        )
                    )
                    add(
                        SettingItem.ChipGroup(
                            id = "insertion_mode",
                            title = "Text Insertion Position",
                            options = InsertionMode.values().map {
                                ChipOption(it.name, it.displayName)
                            },
                            selectedKey = insertionMode,
                            onOptionSelected = onInsertionModeChange
                        )
                    )
                    add(
                        SettingItem.Switch(
                            id = "offline_preferred",
                            title = "Offline Preferred",
                            subtitle = "Prefer on-device speech engine if available",
                            checked = offlinePreferred,
                            onCheckedChange = onOfflinePreferredChange
                        )
                    )
                    add(
                        SettingItem.Switch(
                            id = "keep_screen_on",
                            title = "Keep Screen On While Dictating",
                            subtitle = "Prevents device from sleeping during long dictation sessions",
                            checked = keepScreenOn,
                            onCheckedChange = onKeepScreenOnChange
                        )
                    )
                }
            ),

            // 3. Editor & Appearance Section
            SettingHeadlineSection(
                id = "appearance",
                title = "Editor & Appearance",
                subtitle = "Themes, color pickers, typography & metrics",
                icon = Icons.Outlined.Palette,
                items = buildList {
                    add(
                        SettingItem.ChipGroup(
                            id = "app_theme",
                            title = "Theme Palette",
                            subtitle = "Select AMOLED, Dark, Light, System, or build a Full Custom theme",
                            options = listOf("AMOLED", "DARK", "LIGHT", "SYSTEM", "CUSTOM").map {
                                ChipOption(it, it)
                            },
                            selectedKey = appTheme,
                            onOptionSelected = onAppThemeChange
                        )
                    )
                    // Full Custom Theme with Color Pickers
                    add(
                        SettingItem.CustomContent(
                            id = "custom_theme_colors",
                            content = {
                                FullCustomThemeColorPicker(
                                    primaryColor = customPrimaryColor,
                                    backgroundColor = customBackgroundColor,
                                    surfaceColor = customSurfaceColor,
                                    textColor = customTextColor,
                                    secondaryColor = customSecondaryColor,
                                    onColorsChanged = onCustomThemeColorsChange
                                )
                            }
                        )
                    )
                    // Typography: Font Family
                    add(
                        SettingItem.ChipGroup(
                            id = "editor_font_family",
                            title = "Font Family",
                            subtitle = "Choose from bundled fonts or import your own TTF/OTF",
                            options = listOf(
                                ChipOption("BENGALI", "Bengali (Noto)"),
                                ChipOption("INTER", "Latin (Inter)"),
                                ChipOption("MONOSPACE", "Monospace"),
                                ChipOption("SYSTEM", "System Default"),
                                ChipOption("CUSTOM", if (customFontName.isNotBlank()) customFontName else "Custom TTF/OTF")
                            ),
                            selectedKey = fontFamily,
                            onOptionSelected = onFontFamilyChange
                        )
                    )
                    // Custom TTF/OTF file picker control
                    add(
                        SettingItem.CustomContent(
                            id = "custom_font_import",
                            content = {
                                CustomFontImportControl(
                                    customFontName = customFontName,
                                    customFontPath = customFontPath,
                                    onCustomFontSelected = onCustomFontChange,
                                    onFontOptionChanged = onFontFamilyChange
                                )
                            }
                        )
                    )
                    // Font Size Slider
                    add(
                        SettingItem.Slider(
                            id = "editor_font_size",
                            title = "Font Size",
                            value = fontSize,
                            valueRange = 12f..36f,
                            steps = 12,
                            displayValue = "${fontSize.toInt()} sp",
                            onValueChange = onFontSizeChange
                        )
                    )
                    // Line Spacing
                    add(
                        SettingItem.ChipGroup(
                            id = "line_spacing",
                            title = "Line Spacing Multiplier",
                            options = listOf(1.0f, 1.2f, 1.5f, 1.8f, 2.0f, 2.4f).map {
                                ChipOption(it.toString(), "${it}x")
                            },
                            selectedKey = lineSpacing.toString(),
                            onOptionSelected = { it.toFloatOrNull()?.let(onLineSpacingChange) }
                        )
                    )
                    // Letter Spacing
                    add(
                        SettingItem.Slider(
                            id = "letter_spacing",
                            title = "Letter Spacing",
                            subtitle = "Adjust tracking between characters",
                            value = letterSpacing,
                            valueRange = -1.0f..4.0f,
                            steps = 10,
                            displayValue = "${"%.1f".format(letterSpacing)} sp",
                            onValueChange = onLetterSpacingChange
                        )
                    )
                    // Paragraph Spacing
                    add(
                        SettingItem.Slider(
                            id = "paragraph_spacing",
                            title = "Paragraph Spacing",
                            subtitle = "Extra breathing space below paragraphs",
                            value = paragraphSpacing.toFloat(),
                            valueRange = 0f..24f,
                            steps = 12,
                            displayValue = "$paragraphSpacing dp",
                            onValueChange = { onParagraphSpacingChange(it.toInt()) }
                        )
                    )
                    // Text Alignment
                    add(
                        SettingItem.ChipGroup(
                            id = "text_alignment",
                            title = "Text Alignment",
                            options = listOf(
                                ChipOption("LEFT", "Left"),
                                ChipOption("CENTER", "Center"),
                                ChipOption("RIGHT", "Right"),
                                ChipOption("JUSTIFY", "Justify")
                            ),
                            selectedKey = textAlignment,
                            onOptionSelected = onTextAlignmentChange
                        )
                    )
                    // Editor Margins / Horizontal Padding
                    add(
                        SettingItem.Slider(
                            id = "editor_margin",
                            title = "Horizontal Margins",
                            subtitle = "Editor side padding",
                            value = marginDp.toFloat(),
                            valueRange = 8f..40f,
                            steps = 16,
                            displayValue = "$marginDp dp",
                            onValueChange = { onMarginDpChange(it.toInt()) }
                        )
                    )
                    // Max Line Width
                    add(
                        SettingItem.ChipGroup(
                            id = "max_line_width",
                            title = "Max Line Width",
                            subtitle = "Constrain text column width on wide screens / foldables",
                            options = listOf(
                                ChipOption("0", "Full Width"),
                                ChipOption("480", "480 dp"),
                                ChipOption("600", "600 dp"),
                                ChipOption("720", "720 dp")
                            ),
                            selectedKey = maxWidthDp.toString(),
                            onOptionSelected = { it.toIntOrNull()?.let(onMaxWidthDpChange) }
                        )
                    )
                    // UI Density
                    add(
                        SettingItem.ChipGroup(
                            id = "ui_density",
                            title = "UI Density",
                            options = listOf(
                                ChipOption("COMPACT", "Compact"),
                                ChipOption("NORMAL", "Normal"),
                                ChipOption("COMFORTABLE", "Comfortable")
                            ),
                            selectedKey = uiDensity,
                            onOptionSelected = onUiDensityChange
                        )
                    )
                    // Corner Radius
                    add(
                        SettingItem.ChipGroup(
                            id = "corner_radius",
                            title = "UI Corner Radius",
                            options = listOf(
                                ChipOption("4", "4 dp (Sharp)"),
                                ChipOption("12", "12 dp (Smooth)"),
                                ChipOption("24", "24 dp (Round)")
                            ),
                            selectedKey = cornerRadiusDp.toString(),
                            onOptionSelected = { it.toIntOrNull()?.let(onCornerRadiusDpChange) }
                        )
                    )
                    // Word count switch
                    add(
                        SettingItem.Switch(
                            id = "word_count",
                            title = "Show Word & Character Count",
                            subtitle = "Displays live metrics in the secondary toolbar",
                            checked = showWordCount,
                            onCheckedChange = onShowWordCountChange
                        )
                    )
                    // Note metrics in list switch
                    add(
                        SettingItem.Switch(
                            id = "note_metrics_in_list",
                            title = "Show note metrics in list",
                            subtitle = "Displays word count, reading time, and date under note titles in Notes Manager",
                            checked = showNoteMetrics,
                            onCheckedChange = onShowNoteMetricsChange
                        )
                    )
                    // Note metrics format chip group
                    add(
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
                        )
                    )
                    add(
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
                }
            ),

            // 4. Toolbar & Mic Controls Section
            SettingHeadlineSection(
                id = "toolbar_mic",
                title = "Toolbar, Mic & Animations",
                subtitle = "Reorder toolbar, visibility, mic shape/size & animation styles",
                icon = Icons.Outlined.Tune,
                items = listOf(
                    SettingItem.CustomContent(
                        id = "toolbar_reorder_content",
                        content = {
                            ToolbarReorderAndVisibilityControl(
                                toolbarOrderStr = toolbarItemOrder,
                                hiddenItems = toolbarHiddenItems,
                                onMoveItem = onMoveToolbarItem,
                                onToggleVisibility = onToggleToolbarItemVisibility,
                                onResetOrder = onResetToolbarOrder
                            )
                        }
                    ),
                    SettingItem.ChipGroup(
                        id = "mic_size",
                        title = "Mic Button Size",
                        subtitle = "Floating dictation action button size",
                        options = listOf(
                            ChipOption("SMALL", "Small (48dp)"),
                            ChipOption("MEDIUM", "Medium (60dp)"),
                            ChipOption("LARGE", "Large (72dp)"),
                            ChipOption("EXTRA_LARGE", "XL (84dp)")
                        ),
                        selectedKey = micButtonSize,
                        onOptionSelected = onMicButtonSizeChange
                    ),
                    SettingItem.ChipGroup(
                        id = "mic_shape",
                        title = "Mic Button Shape",
                        options = listOf(
                            ChipOption("CIRCLE", "Circle"),
                            ChipOption("SQUIRCLE", "Squircle"),
                            ChipOption("SQUARE", "Square")
                        ),
                        selectedKey = micButtonShape,
                        onOptionSelected = onMicButtonShapeChange
                    ),
                    SettingItem.ChipGroup(
                        id = "mic_color",
                        title = "Mic Button Color Accent",
                        options = listOf(
                            ChipOption("DEFAULT", "Dynamic State"),
                            ChipOption("SAFFRON", "Saffron"),
                            ChipOption("CORAL", "Coral"),
                            ChipOption("EMERALD", "Emerald"),
                            ChipOption("BLUE", "Blue"),
                            ChipOption("PURPLE", "Purple")
                        ),
                        selectedKey = micButtonColor,
                        onOptionSelected = onMicButtonColorChange
                    ),
                    SettingItem.ChipGroup(
                        id = "mic_position",
                        title = "Mic Button Position",
                        options = listOf(
                            ChipOption("RIGHT", "Bottom-Right (Default)"),
                            ChipOption("CENTER", "Bottom-Center"),
                            ChipOption("LEFT", "Bottom-Left")
                        ),
                        selectedKey = micButtonPosition,
                        onOptionSelected = onMicButtonPositionChange
                    ),
                    SettingItem.Slider(
                        id = "mic_offset_y",
                        title = "Mic Bottom Offset",
                        subtitle = "Vertical distance from bottom edge",
                        value = micButtonOffsetY.toFloat(),
                        valueRange = 0f..32f,
                        steps = 8,
                        displayValue = "$micButtonOffsetY dp",
                        onValueChange = { onMicButtonOffsetYChange(it.toInt()) }
                    ),
                    SettingItem.ChipGroup(
                        id = "status_chip_style",
                        title = "Status Chip Style",
                        subtitle = "Shape style for dictation status & metrics chips",
                        options = listOf(
                            ChipOption("PILL", "Pill (Rounded)"),
                            ChipOption("ROUNDED", "Rounded Rect"),
                            ChipOption("OUTLINE", "Outline Border")
                        ),
                        selectedKey = statusChipStyle,
                        onOptionSelected = onStatusChipStyleChange
                    ),
                    SettingItem.ChipGroup(
                        id = "dictation_animation",
                        title = "Voice Dictation Animation Style",
                        subtitle = "Animation effects while speech engine is listening",
                        options = listOf(
                            ChipOption("PULSE_RING", "Pulsing Ring (around button)"),
                            ChipOption("HORIZONTAL_WAVE", "Horizontal Waveform (Equalizer)"),
                            ChipOption("GLOW", "Glow Aura"),
                            ChipOption("NONE", "None (Static)")
                        ),
                        selectedKey = dictationAnimationStyle,
                        onOptionSelected = onDictationAnimationStyleChange
                    ),
                    SettingItem.ChipGroup(
                        id = "haptics_level",
                        title = "Haptics Feedback Level",
                        subtitle = "Vibration strength on mic tap, toolbar clicks and typing",
                        options = listOf(
                            ChipOption("OFF", "Off"),
                            ChipOption("LIGHT", "Light"),
                            ChipOption("MEDIUM", "Medium (Default)"),
                            ChipOption("HEAVY", "Heavy")
                        ),
                        selectedKey = hapticsLevel,
                        onOptionSelected = onHapticsLevelChange
                    )
                )
            ),

            // 4. Text Pipeline & Spoken Commands Section
            SettingHeadlineSection(
                id = "pipeline",
                title = "Text Pipeline & Commands",
                subtitle = "Automatic punctuation, spacing cleanups & spoken commands",
                icon = Icons.Outlined.Tune,
                items = listOf(
                    SettingItem.Switch(
                        id = "pipeline_spacing",
                        title = "Spacing Cleanups",
                        subtitle = "Removes duplicate spaces and cleans spacing around punctuation",
                        checked = pipelineSpacing,
                        onCheckedChange = onPipelineSpacingChange
                    ),
                    SettingItem.Switch(
                        id = "pipeline_autocap",
                        title = "Auto-Capitalization",
                        subtitle = "Capitalizes sentences after periods, exclamation, and question marks",
                        checked = pipelineAutoCap,
                        onCheckedChange = onPipelineAutoCapChange
                    ),
                    SettingItem.Switch(
                        id = "pipeline_commands",
                        title = "Spoken Commands & Punctuation",
                        subtitle = "Converts spoken words (e.g. 'দাঁড়ি' → '।', 'full stop' → '.') and actions ('delete last word')",
                        checked = pipelineCommands,
                        onCheckedChange = onPipelineCommandsChange
                    )
                )
            ),

            // Backup & Restore Section
            SettingHeadlineSection(
                id = "backup_restore",
                title = "Backup & Restore",
                subtitle = "Export or replace all notes, folders, templates & app settings",
                icon = Icons.Outlined.Backup,
                items = listOf(
                    SettingItem.ButtonRow(
                        id = "export_backup_action",
                        title = "Export Backup",
                        subtitle = "Saves all notes, templates, folder tree & settings into a ZIP archive",
                        buttons = listOf(
                            SettingButtonOption(
                                label = "Export Backup",
                                onClick = {
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                    exportLauncher.launch("KothaPad_backup_$dateStr.zip")
                                }
                            )
                        )
                    ),
                    SettingItem.ButtonRow(
                        id = "import_backup_action",
                        title = "Import Backup (Replace mode)",
                        subtitle = "Validates and restores notes, templates & settings from a ZIP backup",
                        buttons = listOf(
                            SettingButtonOption(
                                label = "Import Backup",
                                isDestructive = true,
                                onClick = {
                                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                }
                            )
                        )
                    )
                )
            ),

            // About Section
            SettingHeadlineSection(
                id = "about",
                title = "About KothaPad",
                subtitle = "Version, architecture & privacy policy",
                icon = Icons.Outlined.Info,
                items = listOf(
                    SettingItem.InfoText(
                        id = "about_info",
                        title = "KothaPad v1.0",
                        description = "100% Kotlin & Jetpack Compose native speech notepad for power users. Local-first architecture with Room, DataStore, and Android SpeechRecognizer.",
                        badge = "🔒 Privacy: 0 accounts • 0 ads • 0 analytics • Audio processed only on device/system recognizer."
                    )
                )
            )
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
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
                if (section.id == "notes_manager_settings_entry") {
                    NavigationSettingCard(
                        section = section,
                        onClick = onOpenNotesManagerSettings
                    )
                } else {
                    val isExpanded = expandedSectionId == section.id
                    HeadlineCard(
                        section = section,
                        isExpanded = isExpanded,
                        onToggle = {
                            // Tapping expands in place; tapping again collapses it; only one open at a time
                            expandedSectionId = if (isExpanded) null else section.id
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Export Layout Dialog
    if (showExportDialog) {
        val exportedJson = remember { onExportKeyboardLayout() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Keyboard Layout (JSON)") },
            text = {
                OutlinedTextField(
                    value = exportedJson,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedJson))
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Layout Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Keyboard Layout (JSON)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = false
                        },
                        placeholder = { Text("Paste layout JSON here...") },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                        isError = importError
                    )
                    if (importError) {
                        Text(
                            text = "Invalid keyboard JSON format",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onImportKeyboardLayout(importJsonText)
                        if (success) {
                            showImportDialog = false
                        } else {
                            importError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Confirmation Dialog (Replace All)
    pendingRestoreData?.let { restoreData ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) pendingRestoreData = null
            },
            modifier = Modifier.testTag("confirm_restore_dialog"),
            title = {
                Text(
                    text = "Replace All Data?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will delete all current notes and settings and replace them with the backup. Continue?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Backup details:\n• Notes: ${restoreData.notes.size}\n• Templates: ${restoreData.stamps.size}\n• Backup Version: v${restoreData.backupVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRestore = restoreData
                        pendingRestoreData = null
                        isRestoring = true
                        onRestoreBackup(toRestore) { result ->
                            isRestoring = false
                            result.onSuccess { summary ->
                                Toast.makeText(
                                    context,
                                    "Backup restored: ${summary.notesCount} notes, ${summary.templatesCount} templates",
                                    Toast.LENGTH_LONG
                                ).show()
                                showFinishSetupDialog = true
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.message ?: "Restoration failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Replace", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingRestoreData = null },
                    modifier = Modifier.testTag("cancel_restore_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Validation Error Dialog
    validationError?.let { errText ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = {
                Text(
                    text = "Backup Verification Failed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = errText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { validationError = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Progress Indicator Dialog
    if (isExporting || isValidating || isRestoring) {
        val progressMessage = when {
            isExporting -> "Exporting backup to ZIP..."
            isValidating -> "Verifying backup file..."
            isRestoring -> "Restoring notes and settings..."
            else -> "Processing..."
        }
        AlertDialog(
            onDismissRequest = { /* Non-dismissible while active */ },
            title = {
                Text(
                    text = progressMessage,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = SaffronPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Please wait, do not close the app...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Finish Setup Dialog (Permission & System Setting Checklist)
    if (showFinishSetupDialog) {
        FinishSetupDialog(
            onDismiss = { showFinishSetupDialog = false }
        )
    }

    // Switch Vault Dialog
    if (showSwitchVaultDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchVaultDialog = false },
            title = { Text("Switch Vault") },
            text = {
                val otherVaults = vaultState.recentVaults.filter { it.uriString != vaultState.currentVault?.uriString }
                if (otherVaults.isEmpty()) {
                    Text("No other vaults recorded. Open another folder or create a new vault to switch.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(otherVaults) { vault ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AmoledSurface,
                                border = BorderStroke(1.dp, AmoledBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        showSwitchVaultDialog = false
                                        onSwitchVault(vault)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(vault.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            Uri.decode(vault.uriString).substringAfterLast("/tree/").replace("%2F", "/"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchVaultDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Open or New Vault Choice Dialog
    if (showOpenOrNewVaultDialog) {
        AlertDialog(
            onDismissRequest = { showOpenOrNewVaultDialog = false },
            title = { Text("Open or Create Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select how you want to configure your vault:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = {
                            showOpenOrNewVaultDialog = false
                            openFolderLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Existing Folder")
                    }
                    Button(
                        onClick = {
                            showOpenOrNewVaultDialog = false
                            createParentFolderLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Vault Folder")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpenOrNewVaultDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recent Vaults Management Dialog
    if (showRecentVaultsDialog) {
        AlertDialog(
            onDismissRequest = { showRecentVaultsDialog = false },
            title = { Text("Recent Vaults") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(vaultState.recentVaults) { vault ->
                        val isCurrent = vault.uriString == vaultState.currentVault?.uriString
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmoledSurface,
                            border = BorderStroke(1.dp, if (isCurrent) SaffronPrimary else AmoledBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!isCurrent) {
                                        showRecentVaultsDialog = false
                                        onSwitchVault(vault)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            vault.name + if (isCurrent) " (Active)" else "",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            Uri.decode(vault.uriString).substringAfterLast("/tree/").replace("%2F", "/"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { onRemoveRecentVault(vault.uriString) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecentVaultsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Create New Vault Name Dialog
    if (showCreateVaultNameDialog && selectedParentUri != null) {
        AlertDialog(
            onDismissRequest = {
                showCreateVaultNameDialog = false
                selectedParentUri = null
            },
            title = { Text("New Vault Name") },
            text = {
                Column {
                    Text("Enter a folder name for the new vault:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newVaultNameInput,
                        onValueChange = { newVaultNameInput = it },
                        singleLine = true,
                        label = { Text("Vault Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newVaultNameInput.trim().ifEmpty { "KothaVault" }
                        showCreateVaultNameDialog = false
                        val parent = selectedParentUri
                        selectedParentUri = null
                        if (parent != null) {
                            onCreateNewVault(parent, name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateVaultNameDialog = false
                        selectedParentUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// Direct Navigation Setting Card
// ==========================================

@Composable
fun NavigationSettingCard(
    section: SettingHeadlineSection,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("headline_${section.id}"),
        shape = RoundedCornerShape(14.dp),
        color = AmoledSurfaceVariant.takeIf { it != AmoledSurface }
            ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = AmoledBorder.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (section.icon != null) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = SaffronPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (!section.subtitle.isNullOrBlank()) {
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = SaffronPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ==========================================
// Collapsible Headline Card with Rotating Chevron
// ==========================================

@Composable
fun HeadlineCard(
    section: SettingHeadlineSection,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevron_rotation_${section.id}"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("headline_${section.id}"),
        shape = RoundedCornerShape(14.dp),
        color = AmoledSurfaceVariant.takeIf { it != AmoledSurface }
            ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) SaffronPrimary.copy(alpha = 0.6f) else AmoledBorder.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tappable Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (section.icon != null) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = if (isExpanded) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpanded) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (!section.subtitle.isNullOrBlank()) {
                        Text(
                            text = section.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = if (isExpanded) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            // In-place animated expansion of controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    section.items.forEachIndexed { index, item ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        SettingItemControl(item = item)
                    }
                }
            }
        }
    }
}

// ==========================================
// Reusable Control Renderers
// ==========================================

@Composable
fun SettingItemControl(item: SettingItem) {
    when (item) {
        is SettingItem.Switch -> SwitchControlRow(item)
        is SettingItem.Slider -> SliderControlRow(item)
        is SettingItem.ChipGroup -> ChipGroupControlRow(item)
        is SettingItem.ButtonRow -> ButtonRowControl(item)
        is SettingItem.InfoText -> InfoTextControl(item)
        is SettingItem.CustomContent -> item.content()
    }
}

@Composable
private fun SwitchControlRow(item: SettingItem.Switch) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = item.checked,
            onCheckedChange = item.onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SaffronPrimary,
                checkedTrackColor = SaffronPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SliderControlRow(item: SettingItem.Slider) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.displayValue,
                style = MaterialTheme.typography.bodySmall,
                color = SaffronPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (!item.subtitle.isNullOrBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = item.value,
            onValueChange = item.onValueChange,
            valueRange = item.valueRange,
            steps = item.steps,
            colors = SliderDefaults.colors(
                thumbColor = SaffronPrimary,
                activeTrackColor = SaffronPrimary
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroupControlRow(item: SettingItem.ChipGroup) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (!item.subtitle.isNullOrBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item.options.forEach { opt ->
                FilterChip(
                    selected = item.selectedKey.equals(opt.key, ignoreCase = true),
                    onClick = { item.onOptionSelected(opt.key) },
                    label = { Text(opt.label, fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
private fun ButtonRowControl(item: SettingItem.ButtonRow) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (item.title.isNotBlank()) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.buttons.forEach { btn ->
                OutlinedButton(
                    onClick = btn.onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = btn.label,
                        fontSize = 12.sp,
                        color = if (btn.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTextControl(item: SettingItem.InfoText) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (item.title.isNotBlank()) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SaffronPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!item.badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.badge,
                style = MaterialTheme.typography.labelSmall,
                color = SaffronPrimary
            )
        }
        if (!item.actionButtonText.isNullOrBlank() && item.onActionClick != null) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = item.onActionClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(item.actionButtonText, fontSize = 12.sp)
            }
        }
    }
}
