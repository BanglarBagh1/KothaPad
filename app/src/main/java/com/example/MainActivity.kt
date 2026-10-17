package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.model.DictationState
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.settings.NotesManagerSettingsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.KothaPadTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels {
        val app = application as KothaPadApp
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(
                    application = app,
                    noteRepository = app.noteRepository,
                    stampRepository = app.stampRepository,
                    spokenCommandRepository = app.spokenCommandRepository,
                    preferencesManager = app.preferencesManager,
                    speechSupervisor = app.speechSupervisor
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val customPrimary by viewModel.customPrimaryColor.collectAsState()
            val customBg by viewModel.customBackgroundColor.collectAsState()
            val customSurface by viewModel.customSurfaceColor.collectAsState()
            val customText by viewModel.customTextColor.collectAsState()
            val customSecondary by viewModel.customSecondaryColor.collectAsState()
            val cornerRadiusDp by viewModel.cornerRadiusDp.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val isNoteManagerOpen by viewModel.isNoteManagerOpen.collectAsState()
            val showRenameDialog by viewModel.showRenameDialog.collectAsState()
            val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
            val showStyleSheet by viewModel.showStyleSheet.collectAsState()
            val showTemplatesSheet by viewModel.showTemplatesSheet.collectAsState()
            val editingKey by viewModel.editingKey.collectAsState()
            val userWantsBuiltIn by viewModel.userWantsBuiltInKeyboard.collectAsState()
            val dictationState by viewModel.dictationState.collectAsState()
            val onboardingCompleted by viewModel.onboardingCompletedPref.collectAsState()
            val isReadingMode by viewModel.isReadingMode.collectAsState()

            val density = LocalDensity.current
            val imeBottom = WindowInsets.ime.getBottom(density)
            val isImeVisible = imeBottom > 0

            var lastBackPressTime by remember { mutableStateOf(0L) }

            // Centralized Back Navigation Hierarchy
            // Priority 1: System keyboard open -> Do NOT intercept, let default behavior dismiss keyboard
            BackHandler(enabled = !isImeVisible || isReadingMode || isNoteManagerOpen) {
                when {
                    // Priority 2: Dialog, bottom sheet, popup, or key customization open -> close it
                    editingKey != null -> {
                        viewModel.editingKey.value = null
                    }
                    showRenameDialog -> {
                        viewModel.showRenameDialog.value = false
                    }
                    showLanguagePicker -> {
                        viewModel.showLanguagePicker.value = false
                    }
                    showStyleSheet -> {
                        viewModel.showStyleSheet.value = false
                    }
                    showTemplatesSheet -> {
                        viewModel.showTemplatesSheet.value = false
                    }
                    !onboardingCompleted -> {
                        viewModel.completeOnboarding()
                    }

                    // Priority 3: Notes Manager Settings open -> back to source (Note Manager or Settings)
                    currentScreen == "NOTES_MANAGER_SETTINGS" -> {
                        viewModel.onBackFromNotesManagerSettings()
                    }

                    currentScreen == "MARKDOWN_SETTINGS" -> {
                        viewModel.navigateTo("SETTINGS")
                    }

                    // Priority 4: Notes list drawer / manager open -> close drawer
                    isNoteManagerOpen -> {
                        viewModel.isNoteManagerOpen.value = false
                    }

                    // Priority 5: Reading mode active -> Exit reading mode
                    isReadingMode -> {
                        viewModel.setReadingMode(false)
                    }

                    // Priority 6: Settings screen or any sub-screen open -> return to editor
                    currentScreen != "EDITOR" -> {
                        viewModel.navigateTo("EDITOR")
                    }

                    // Priority 5: Built-in custom keyboard hidden because system keyboard was showing -> restore custom keyboard
                    !userWantsBuiltIn -> {
                        viewModel.setUserWantsBuiltInKeyboard(true)
                    }

                    // Priority 6: Recording/dictation active -> do NOT stop or exit, show toast
                    dictationState !is DictationState.Idle -> {
                        Toast.makeText(this@MainActivity, "Recording in progress. Tap mic to pause or stop.", Toast.LENGTH_SHORT).show()
                    }

                    // Priority 7: Main editor screen with nothing open -> exit confirmation (toast within 2s)
                    else -> {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressTime < 2000L) {
                            viewModel.flushAutoSave()
                            finishAffinity()
                        } else {
                            lastBackPressTime = now
                            Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            KothaPadTheme(
                themeSetting = appTheme,
                customPrimary = androidx.compose.ui.graphics.Color(customPrimary),
                customBackground = androidx.compose.ui.graphics.Color(customBg),
                customSurface = androidx.compose.ui.graphics.Color(customSurface),
                customText = androidx.compose.ui.graphics.Color(customText),
                customSecondary = androidx.compose.ui.graphics.Color(customSecondary),
                cornerRadiusDp = cornerRadiusDp
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "SETTINGS" -> {
                            val dictationMode by viewModel.dictationModePref.collectAsState()
                            val timedMinutes by viewModel.timedMinutesPref.collectAsState()
                            val pauseToleranceSec by viewModel.pauseToleranceSecPref.collectAsState()
                            val idleAutoStopMin by viewModel.idleAutoStopMinPref.collectAsState()
                            val muteBeepMethod by viewModel.muteBeepMethodPref.collectAsState()
                            val insertionMode by viewModel.insertionModePref.collectAsState()
                            val offlinePreferred by viewModel.offlinePreferredPref.collectAsState()
                            val keepScreenOn by viewModel.keepScreenOnPref.collectAsState()
                            val defaultLang by viewModel.defaultLanguage.collectAsState()
                            val fontSize by viewModel.editorFontSize.collectAsState()
                            val lineSpacing by viewModel.editorLineSpacing.collectAsState()
                            val fontFamily by viewModel.editorFontFamily.collectAsState()
                            val pipelineSpacing by viewModel.pipelineSpacingPref.collectAsState()
                            val pipelineAutoCap by viewModel.pipelineAutoCapPref.collectAsState()
                            val pipelineCommands by viewModel.pipelineCommandsPref.collectAsState()
                            val showWordCount by viewModel.showWordCountPref.collectAsState()
                            val showNoteMetrics by viewModel.showNoteMetricsInList.collectAsState()
                            val noteMetricsStyle by viewModel.noteMetricsStyle.collectAsState()
                            val noteMetricsOrder by viewModel.noteMetricsOrder.collectAsState()
                            val noteMetricsHiddenItems by viewModel.noteMetricsHiddenItems.collectAsState()
                            val keyHeightDp by viewModel.keyboardKeyHeightDp.collectAsState()
                            val keyFontSizeSp by viewModel.keyboardFontSizeSp.collectAsState()
                            val keyHaptic by viewModel.keyboardHaptic.collectAsState()
                            val keyHoldBehavior by viewModel.keyboardHoldBehavior.collectAsState()

                            val customFontName by viewModel.customFontName.collectAsState()
                            val customFontPath by viewModel.customFontPath.collectAsState()
                            val letterSpacing by viewModel.editorLetterSpacing.collectAsState()
                            val paragraphSpacing by viewModel.editorParagraphSpacing.collectAsState()
                            val textAlignment by viewModel.editorTextAlignment.collectAsState()
                            val marginDp by viewModel.editorMarginDp.collectAsState()
                            val maxWidthDp by viewModel.editorMaxWidthDp.collectAsState()
                            val toolbarItemOrder by viewModel.toolbarItemOrder.collectAsState()
                            val toolbarHiddenItems by viewModel.toolbarHiddenItems.collectAsState()
                            val micButtonSize by viewModel.micButtonSize.collectAsState()
                            val micButtonShape by viewModel.micButtonShape.collectAsState()
                            val micButtonColor by viewModel.micButtonColor.collectAsState()
                            val micButtonPosition by viewModel.micButtonPosition.collectAsState()
                            val micButtonOffsetY by viewModel.micButtonOffsetY.collectAsState()
                            val statusChipStyle by viewModel.statusChipStyle.collectAsState()
                            val uiDensity by viewModel.uiDensity.collectAsState()
                            val dictationAnimationStyle by viewModel.dictationAnimationStyle.collectAsState()
                            val hapticsLevel by viewModel.hapticsLevel.collectAsState()
                            val floatingLayoutSettings by viewModel.floatingLayoutSettings.collectAsState()

                            SettingsScreen(
                                dictationMode = dictationMode,
                                timedMinutes = timedMinutes,
                                pauseToleranceSeconds = pauseToleranceSec,
                                idleAutoStopMinutes = idleAutoStopMin,
                                muteBeepMethod = muteBeepMethod,
                                insertionMode = insertionMode,
                                offlinePreferred = offlinePreferred,
                                keepScreenOn = keepScreenOn,
                                defaultLanguage = defaultLang,
                                appTheme = appTheme,
                                fontSize = fontSize,
                                lineSpacing = lineSpacing,
                                fontFamily = fontFamily,
                                pipelineSpacing = pipelineSpacing,
                                pipelineAutoCap = pipelineAutoCap,
                                pipelineCommands = pipelineCommands,
                                showWordCount = showWordCount,
                                customPrimaryColor = customPrimary,
                                customBackgroundColor = customBg,
                                customSurfaceColor = customSurface,
                                customTextColor = customText,
                                customSecondaryColor = customSecondary,
                                onCustomThemeColorsChange = { p, b, s, t, sec -> viewModel.setCustomThemeColors(p, b, s, t, sec) },
                                customFontName = customFontName,
                                customFontPath = customFontPath,
                                letterSpacing = letterSpacing,
                                paragraphSpacing = paragraphSpacing,
                                textAlignment = textAlignment,
                                marginDp = marginDp,
                                maxWidthDp = maxWidthDp,
                                onCustomFontChange = { name, path -> viewModel.setCustomFont(name, path) },
                                onLetterSpacingChange = { viewModel.setLetterSpacing(it) },
                                onParagraphSpacingChange = { viewModel.setParagraphSpacing(it) },
                                onTextAlignmentChange = { viewModel.setTextAlignment(it) },
                                onMarginDpChange = { viewModel.setMarginDp(it) },
                                onMaxWidthDpChange = { viewModel.setMaxWidthDp(it) },
                                toolbarItemOrder = toolbarItemOrder,
                                toolbarHiddenItems = toolbarHiddenItems,
                                onMoveToolbarItem = { from, to -> viewModel.moveToolbarItem(from, to) },
                                onToggleToolbarItemVisibility = { viewModel.toggleToolbarItemVisibility(it) },
                                onResetToolbarOrder = { viewModel.resetToolbarOrder() },
                                micButtonSize = micButtonSize,
                                micButtonShape = micButtonShape,
                                micButtonColor = micButtonColor,
                                micButtonPosition = micButtonPosition,
                                micButtonOffsetY = micButtonOffsetY,
                                onMicButtonSizeChange = { viewModel.setMicButtonSize(it) },
                                onMicButtonShapeChange = { viewModel.setMicButtonShape(it) },
                                onMicButtonColorChange = { viewModel.setMicButtonColor(it) },
                                onMicButtonPositionChange = { viewModel.setMicButtonPosition(it) },
                                onMicButtonOffsetYChange = { viewModel.setMicButtonOffsetY(it) },
                                statusChipStyle = statusChipStyle,
                                uiDensity = uiDensity,
                                cornerRadiusDp = cornerRadiusDp,
                                dictationAnimationStyle = dictationAnimationStyle,
                                hapticsLevel = hapticsLevel,
                                onStatusChipStyleChange = { viewModel.setStatusChipStyle(it) },
                                onUiDensityChange = { viewModel.setUiDensity(it) },
                                onCornerRadiusDpChange = { viewModel.setCornerRadiusDp(it) },
                                onDictationAnimationStyleChange = { viewModel.setDictationAnimationStyle(it) },
                                onHapticsLevelChange = { viewModel.setHapticsLevel(it) },
                                keyboardKeyHeightDp = keyHeightDp,
                                keyboardFontSizeSp = keyFontSizeSp,
                                keyboardHaptic = keyHaptic,
                                keyboardHoldBehavior = keyHoldBehavior,
                                onKeyboardKeyHeightChange = { viewModel.setKeyboardKeyHeightDp(it) },
                                onKeyboardFontSizeChange = { viewModel.setKeyboardFontSizeSp(it) },
                                onKeyboardHapticChange = { viewModel.setKeyboardHaptic(it) },
                                onKeyboardHoldBehaviorChange = { viewModel.setKeyboardHoldBehavior(it) },
                                onResetKeyboardLayout = { viewModel.resetKeyboardLayout() },
                                onExportKeyboardLayout = { viewModel.exportKeyboardLayoutJson() },
                                onImportKeyboardLayout = { viewModel.importKeyboardLayoutJson(it) },
                                onDictationModeChange = { viewModel.setDictationModePref(it) },
                                onTimedMinutesChange = { viewModel.setTimedMinutesPref(it) },
                                onPauseToleranceChange = { viewModel.setPauseToleranceSecPref(it) },
                                onIdleAutoStopChange = { viewModel.setIdleAutoStopMinPref(it) },
                                onMuteBeepMethodChange = { viewModel.setMuteBeepMethodPref(it) },
                                onInsertionModeChange = { viewModel.setInsertionModePref(it) },
                                onOfflinePreferredChange = { viewModel.setOfflinePreferredPref(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOnPref(it) },
                                onAppThemeChange = { viewModel.setAppThemePref(it) },
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onPipelineSpacingChange = { viewModel.setPipelineSpacingPref(it) },
                                onPipelineAutoCapChange = { viewModel.setPipelineAutoCapPref(it) },
                                onPipelineCommandsChange = { viewModel.setPipelineCommandsPref(it) },
                                onShowWordCountChange = { viewModel.setShowWordCountPref(it) },
                                showNoteMetrics = showNoteMetrics,
                                noteMetricsStyle = noteMetricsStyle,
                                noteMetricsOrder = noteMetricsOrder,
                                noteMetricsHiddenItems = noteMetricsHiddenItems,
                                onShowNoteMetricsChange = { viewModel.setShowNoteMetricsInList(it) },
                                onNoteMetricsStyleChange = { viewModel.setNoteMetricsStyle(it) },
                                onMoveNoteMetricItem = { from, to -> viewModel.moveNoteMetricItem(from, to) },
                                onToggleNoteMetricVisibility = { viewModel.toggleNoteMetricVisibility(it) },
                                onResetNoteMetricsOrder = { viewModel.resetNoteMetricsOrder() },
                                onOpenNotesManagerSettings = { viewModel.openNotesManagerSettings(fromNoteManager = false) },
                                onOpenMarkdownSettings = { viewModel.navigateTo("MARKDOWN_SETTINGS") },
                                floatingLayoutSettings = floatingLayoutSettings,
                                onFloatingUseSideRailChange = { viewModel.setFloatingUseSideRail(it) },
                                onRailSideChange = { viewModel.setRailSide(it) },
                                onRailScaleChange = { viewModel.setRailScale(it) },
                                onRailBottomOffsetChange = { viewModel.setRailBottomOffsetDp(it) },
                                onRailSpacingChange = { viewModel.setRailSpacingDp(it) },
                                onRailKeepTextClearChange = { viewModel.setRailKeepTextClear(it) },
                                onMoveRailButtonItem = { from, to -> viewModel.moveRailButtonItem(from, to) },
                                onResetRailButtonOrder = { viewModel.resetRailButtonOrder() },
                                onToggleScaleChange = { viewModel.setToggleScale(it) },
                                onToggleBottomOffsetChange = { viewModel.setToggleBottomOffsetDp(it) },
                                onToggleGapChange = { viewModel.setToggleGapDp(it) },
                                onFormatBarScaleChange = { viewModel.setFormatBarScale(it) },
                                onFormatBarBottomOffsetChange = { viewModel.setFormatBarBottomOffsetDp(it) },
                                onFormatBarWidthPercentChange = { viewModel.setFormatBarWidthPercent(it) },
                                onFormatBarAlignmentChange = { viewModel.setFormatBarAlignment(it) },
                                onWaveformScaleChange = { viewModel.setWaveformScale(it) },
                                onWaveformShowChange = { viewModel.setWaveformShow(it) },
                                onResetFloatingLayout = { viewModel.resetFloatingLayoutToDefaults() },
                                onExportBackup = { uri, cb -> viewModel.exportBackup(uri, cb) },
                                onValidateBackup = { uri, cb -> viewModel.validateBackup(uri, cb) },
                                onRestoreBackup = { backup, cb -> viewModel.restoreBackup(backup, cb) },
                                onBackClick = { viewModel.navigateTo("EDITOR") }
                            )
                        }
                        "NOTES_MANAGER_SETTINGS" -> {
                            val newNoteLocationMode by viewModel.newNoteLocationMode.collectAsState()
                            val fixedNewNoteFolder by viewModel.fixedNewNoteFolder.collectAsState()
                            val askBeforeCreating by viewModel.askBeforeCreating.collectAsState()
                            val askChooseTemplateToggle by viewModel.askChooseTemplateToggle.collectAsState()
                            val askStartRecordingToggle by viewModel.askStartRecordingToggle.collectAsState()
                            val newNoteNamingRule by viewModel.newNoteNamingRule.collectAsState()
                            val afterCreatingNote by viewModel.afterCreatingNote.collectAsState()
                            val customFolders by viewModel.customFolders.collectAsState()
                            val allNotes by viewModel.allNotes.collectAsState()
                            val showNoteMetrics by viewModel.showNoteMetricsInList.collectAsState()
                            val noteMetricsStyle by viewModel.noteMetricsStyle.collectAsState()
                            val noteMetricsOrder by viewModel.noteMetricsOrder.collectAsState()
                            val noteMetricsHiddenItems by viewModel.noteMetricsHiddenItems.collectAsState()
                            val trashAutoEmptyDays by viewModel.trashAutoEmptyDays.collectAsState()

                            val availableFolders = remember(allNotes, customFolders) {
                                (allNotes.map { it.folder }.filter { it.isNotBlank() } + customFolders).distinct()
                            }

                            NotesManagerSettingsScreen(
                                newNoteLocationMode = newNoteLocationMode,
                                fixedNewNoteFolder = fixedNewNoteFolder,
                                askBeforeCreating = askBeforeCreating,
                                askChooseTemplateToggle = askChooseTemplateToggle,
                                askStartRecordingToggle = askStartRecordingToggle,
                                newNoteNamingRule = newNoteNamingRule,
                                afterCreatingNote = afterCreatingNote,
                                availableFolders = availableFolders,
                                onNewNoteLocationModeChange = { viewModel.setNewNoteLocationMode(it) },
                                onFixedNewNoteFolderChange = { viewModel.setFixedNewNoteFolder(it) },
                                onAskBeforeCreatingChange = { viewModel.setAskBeforeCreating(it) },
                                onAskChooseTemplateToggleChange = { viewModel.setAskChooseTemplateToggle(it) },
                                onAskStartRecordingToggleChange = { viewModel.setAskStartRecordingToggle(it) },
                                onNewNoteNamingRuleChange = { viewModel.setNewNoteNamingRule(it) },
                                onAfterCreatingNoteChange = { viewModel.setAfterCreatingNote(it) },
                                onResetToDefaults = { viewModel.resetNotesManagerSettingsToDefault() },
                                onBackClick = { viewModel.onBackFromNotesManagerSettings() },
                                showNoteMetrics = showNoteMetrics,
                                noteMetricsStyle = noteMetricsStyle,
                                noteMetricsOrder = noteMetricsOrder,
                                noteMetricsHiddenItems = noteMetricsHiddenItems,
                                onMoveNoteMetricItem = { from, to -> viewModel.moveNoteMetricItem(from, to) },
                                onToggleNoteMetricVisibility = { viewModel.toggleNoteMetricVisibility(it) },
                                onResetNoteMetricsOrder = { viewModel.resetNoteMetricsOrder() },
                                trashAutoEmptyDays = trashAutoEmptyDays,
                                onShowNoteMetricsChange = { viewModel.setShowNoteMetricsInList(it) },
                                onNoteMetricsStyleChange = { viewModel.setNoteMetricsStyle(it) },
                                onUpdateTrashDays = { viewModel.setTrashAutoEmptyDaysPref(it) }
                            )
                        }
                        "MARKDOWN_SETTINGS" -> {
                            val markdownSettings by viewModel.markdownSettings.collectAsState()
                            com.example.markdown.MarkdownSettingsScreen(
                                settings = markdownSettings,
                                onUpdateSettings = { viewModel.updateMarkdownSettings(it) },
                                onResetToDefaults = { viewModel.resetMarkdownSettingsToDefault() },
                                onBackClick = { viewModel.navigateTo("SETTINGS") }
                            )
                        }
                        else -> {
                            EditorScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForegroundStateChanged(true)
    }

    override fun onPause() {
        super.onPause()
        window?.let {
            WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.navigationBars())
        }
        viewModel.onAppForegroundStateChanged(false)
        viewModel.flushAutoSave()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppForegroundStateChanged(false)
        viewModel.flushAutoSave()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as? KothaPadApp)?.speechSupervisor?.restoreCrashSafetyMute()
    }
}
