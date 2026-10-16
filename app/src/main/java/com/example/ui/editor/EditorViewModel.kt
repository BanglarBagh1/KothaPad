package com.example.ui.editor

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KothaPadApp
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupSummary
import com.example.data.backup.ParsedBackupData
import com.example.data.model.AudioSourceType
import com.example.data.model.DictationMode
import com.example.data.model.DictationState
import com.example.data.model.InsertionMode
import com.example.data.model.KeyActionType
import com.example.data.model.KeyboardKey
import com.example.data.model.KeyboardLayout
import com.example.data.model.LanguageItem
import com.example.data.model.Note
import com.example.data.model.SpokenCommand
import com.example.data.model.Stamp
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.NoteRepository
import com.example.data.repository.SpokenCommandRepository
import com.example.data.repository.StampRepository
import com.example.engine.dictation.SpeechSupervisor
import com.example.engine.pipeline.PipelineContext
import com.example.engine.pipeline.StampProcessor
import com.example.engine.pipeline.TextPipeline
import com.example.service.DictationService
import com.example.ui.keyboard.KeyboardLayouts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(
    application: Application,
    private val noteRepository: NoteRepository,
    private val stampRepository: StampRepository,
    private val spokenCommandRepository: SpokenCommandRepository,
    private val preferencesManager: PreferencesManager,
    private val speechSupervisor: SpeechSupervisor
) : AndroidViewModel(application) {

    // Notes Data
    val allNotes: StateFlow<List<Note>> = noteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<Note>> = noteRepository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = noteRepository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = noteRepository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStamps: StateFlow<List<Stamp>> = stampRepository.allStamps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashAutoEmptyDays: StateFlow<Int> = preferencesManager.trashAutoEmptyDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    val defaultNewNoteFolder: StateFlow<String> = preferencesManager.defaultNewNoteFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "General")

    val isNoteManagerOpen = MutableStateFlow(false)

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    // Editor Text State
    private val _textFieldValue = MutableStateFlow(TextFieldValue())
    val textFieldValue: StateFlow<TextFieldValue> = _textFieldValue.asStateFlow()

    // Undo / Redo in-memory stack
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Reading Mode State
    private val _isReadingMode = MutableStateFlow(false)
    val isReadingMode: StateFlow<Boolean> = _isReadingMode.asStateFlow()

    fun setReadingMode(enabled: Boolean) {
        if (enabled && dictationState.value !is DictationState.Idle) {
            return
        }
        _isReadingMode.value = enabled
    }

    fun toggleReadingMode() {
        setReadingMode(!_isReadingMode.value)
    }

    // Supervisor states
    val dictationState: StateFlow<DictationState> = speechSupervisor.state
    val partialText: StateFlow<String> = speechSupervisor.partialText
    val audioLevelRms: StateFlow<Float> = speechSupervisor.audioLevelRms
    val audioSourceType: StateFlow<AudioSourceType> = speechSupervisor.audioSourceDetector.currentSource
    val isOnDevice: StateFlow<Boolean> = speechSupervisor.isOnDevice

    // Preferences Flows
    val defaultLanguage: StateFlow<String> = preferencesManager.defaultLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "bn-BD")
    val favoriteLanguages: StateFlow<Set<String>> = preferencesManager.favoriteLanguages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("bn-BD", "bn-IN", "en-US"))
    val appTheme: StateFlow<String> = preferencesManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AMOLED")
    val editorFontSize: StateFlow<Float> = preferencesManager.editorFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18f)
    val editorLineSpacing: StateFlow<Float> = preferencesManager.editorLineSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.5f)
    val editorFontFamily: StateFlow<String> = preferencesManager.editorFontFamily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BENGALI")
    val dictationModePref: StateFlow<String> = preferencesManager.dictationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CONTINUOUS")
    val timedMinutesPref: StateFlow<Int> = preferencesManager.timedMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val pauseToleranceSecPref: StateFlow<Int> = preferencesManager.pauseToleranceSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)
    val idleAutoStopMinPref: StateFlow<Int> = preferencesManager.idleAutoStopMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val muteBeepMethodPref: StateFlow<String> = preferencesManager.muteBeepMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MEDIA_SYSTEM")
    val insertionModePref: StateFlow<String> = preferencesManager.insertionMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AT_CURSOR")
    val offlinePreferredPref: StateFlow<Boolean> = preferencesManager.offlinePreferred
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val keepScreenOnPref: StateFlow<Boolean> = preferencesManager.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val pipelineSpacingPref: StateFlow<Boolean> = preferencesManager.pipelineSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val pipelineAutoCapPref: StateFlow<Boolean> = preferencesManager.pipelineAutoCap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val pipelineCommandsPref: StateFlow<Boolean> = preferencesManager.pipelineCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val onboardingCompletedPref: StateFlow<Boolean> = preferencesManager.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferencesManager.isOnboardingCompletedSync())
    val showWordCountPref: StateFlow<Boolean> = preferencesManager.showWordCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val showNoteMetricsInList: StateFlow<Boolean> = preferencesManager.showNoteMetricsInList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val noteMetricsStyle: StateFlow<String> = preferencesManager.noteMetricsStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "COMPACT")
    val noteMetricsOrder: StateFlow<String> = preferencesManager.noteMetricsOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING)
    val noteMetricsHiddenItems: StateFlow<Set<String>> = preferencesManager.noteMetricsHiddenItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val expandedFolders: StateFlow<Set<String>> = preferencesManager.expandedFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferencesManager.getExpandedFoldersSync())
    val customFolders: StateFlow<Set<String>> = preferencesManager.customFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Custom Theme Colors
    val customPrimaryColor: StateFlow<Int> = preferencesManager.customPrimaryColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFFFF7043.toInt())
    val customBackgroundColor: StateFlow<Int> = preferencesManager.customBackgroundColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF000000.toInt())
    val customSurfaceColor: StateFlow<Int> = preferencesManager.customSurfaceColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF121212.toInt())
    val customTextColor: StateFlow<Int> = preferencesManager.customTextColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFFFFFFFF.toInt())
    val customSecondaryColor: StateFlow<Int> = preferencesManager.customSecondaryColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFFFF8A65.toInt())

    // Typography Ext Flows
    val customFontPath: StateFlow<String?> = preferencesManager.customFontPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val customFontName: StateFlow<String> = preferencesManager.customFontName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val editorLetterSpacing: StateFlow<Float> = preferencesManager.editorLetterSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val editorParagraphSpacing: StateFlow<Int> = preferencesManager.editorParagraphSpacing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)
    val editorTextAlignment: StateFlow<String> = preferencesManager.editorTextAlignment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LEFT")
    val editorMarginDp: StateFlow<Int> = preferencesManager.editorMarginDp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)
    val editorMaxWidthDp: StateFlow<Int> = preferencesManager.editorMaxWidthDp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Toolbar Settings Flows
    val toolbarItemOrder: StateFlow<String> = preferencesManager.toolbarItemOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DICTATION_STATUS,METRICS,KEYBOARD_MODE,TEXT_SIZE,TEXT_STYLE,AUDIO_SOURCE")
    val toolbarHiddenItems: StateFlow<Set<String>> = preferencesManager.toolbarHiddenItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Mic Button & Cluster Flows
    val micButtonSize: StateFlow<String> = preferencesManager.micButtonSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MEDIUM")
    val micButtonShape: StateFlow<String> = preferencesManager.micButtonShape
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CIRCLE")
    val micButtonColor: StateFlow<String> = preferencesManager.micButtonColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")
    val micButtonPosition: StateFlow<String> = preferencesManager.micButtonPosition
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "RIGHT")
    val micButtonOffsetY: StateFlow<Int> = preferencesManager.micButtonOffsetY
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    // Visual Polish, Animation & Haptics Flows
    val statusChipStyle: StateFlow<String> = preferencesManager.statusChipStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PILL")
    val uiDensity: StateFlow<String> = preferencesManager.uiDensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NORMAL")
    val cornerRadiusDp: StateFlow<Int> = preferencesManager.cornerRadiusDp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)
    val dictationAnimationStyle: StateFlow<String> = preferencesManager.dictationAnimationStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PULSE_RING")
    val hapticsLevel: StateFlow<String> = preferencesManager.hapticsLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MEDIUM")

    // Keyboard Settings Flows
    val keyboardKeyHeightDp: StateFlow<Int> = preferencesManager.keyboardKeyHeightDp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 40)
    val keyboardFontSizeSp: StateFlow<Int> = preferencesManager.keyboardFontSizeSp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)
    val keyboardHaptic: StateFlow<Boolean> = preferencesManager.keyboardHaptic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val keyboardHoldBehavior: StateFlow<String> = preferencesManager.keyboardHoldBehavior
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CUSTOMIZE")
    val keyboardEditMode: StateFlow<Boolean> = preferencesManager.keyboardEditMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // UI state
    // userWantsBuiltIn reflects the toggle in the toolbar / settings
    private val _userWantsBuiltInKeyboard = MutableStateFlow(true)
    val userWantsBuiltInKeyboard: StateFlow<Boolean> = _userWantsBuiltInKeyboard.asStateFlow()

    // 3x10 Layout State
    private val _keyboardLayout = MutableStateFlow(KeyboardLayouts.getDefault3x10Layout())
    val keyboardLayout: StateFlow<KeyboardLayout> = _keyboardLayout.asStateFlow()

    private val _currentScreen = MutableStateFlow("EDITOR") // "EDITOR" or "SETTINGS"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Dialog & Sheet states
    val showStyleSheet = MutableStateFlow(false)
    val showLanguagePicker = MutableStateFlow(false)
    val showRenameDialog = MutableStateFlow(false)
    val showTemplatesSheet = MutableStateFlow(false)
    val editingKey = MutableStateFlow<KeyboardKey?>(null)

    // Auto-save debounce job
    private var autoSaveJob: Job? = null
    private var lastDeletedNote: Note? = null

    init {
        // Collect supervisor finalized text
        viewModelScope.launch {
            speechSupervisor.finalCommittedText.collect { rawText ->
                processAndCommitDictatedText(rawText)
            }
        }

        // Load persisted keyboard layout from DataStore
        viewModelScope.launch {
            val v = preferencesManager.keyboardLayoutVersion.first()
            if (v < 2) {
                preferencesManager.setKeyboardLayoutJson(null)
                preferencesManager.setKeyboardLayoutVersion(2)
                _keyboardLayout.value = KeyboardLayouts.getDefault3x10Layout()
            }
            preferencesManager.keyboardLayoutJson.collect { jsonStr ->
                if (!jsonStr.isNullOrBlank()) {
                    try {
                        _keyboardLayout.value = KeyboardLayouts.deserializeFromJson(jsonStr)
                    } catch (_: Exception) {
                        _keyboardLayout.value = KeyboardLayouts.getDefault3x10Layout()
                    }
                } else {
                    _keyboardLayout.value = KeyboardLayouts.getDefault3x10Layout()
                }
            }
        }

        // Auto-clean trash on start if configured
        viewModelScope.launch(Dispatchers.IO) {
            val days = preferencesManager.trashAutoEmptyDays.first()
            if (days > 0) {
                val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
                noteRepository.autoCleanTrash(cutoff)
            }
        }

        // Ensure returning users with existing notes are never prompted for onboarding
        viewModelScope.launch(Dispatchers.IO) {
            val existing = noteRepository.getLatestNote()
            if (existing != null && !preferencesManager.isOnboardingCompletedSync()) {
                preferencesManager.setOnboardingCompleted(true)
            }
        }

        // Initialize active note on start: restore last-opened note
        viewModelScope.launch {
            val lastId = preferencesManager.getLastOpenedNoteIdSync()
            var noteToLoad: Note? = null
            if (lastId > 0) {
                val found = noteRepository.getNoteOnce(lastId)
                if (found != null && !found.isDeleted) {
                    noteToLoad = found
                }
            }
            if (noteToLoad == null) {
                noteToLoad = noteRepository.getLatestActiveNote() ?: noteRepository.getLatestNote()
            }
            if (noteToLoad != null) {
                loadNote(noteToLoad)
            } else {
                createNewNote()
            }
        }

        // Keep session timing synchronized with note manager & screens
        viewModelScope.launch {
            isNoteManagerOpen.collect {
                checkAndResumeSessionTimer()
            }
        }

        viewModelScope.launch {
            _currentScreen.collect {
                checkAndResumeSessionTimer()
            }
        }
    }

    private var sessionDurationJob: Job? = null
    private var isAppInForeground: Boolean = true
    private var lastPeriodicDbFlushTime: Long = 0L

    fun onAppForegroundStateChanged(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (!inForeground) {
            pauseSessionTimerAndPersist()
        } else {
            checkAndResumeSessionTimer()
        }
    }

    private fun checkAndResumeSessionTimer() {
        val note = _currentNote.value
        val isEditorActive = isAppInForeground && _currentScreen.value == "EDITOR" && !isNoteManagerOpen.value
        if (isEditorActive && note != null) {
            startSessionTimer()
        } else {
            pauseSessionTimerAndPersist()
        }
    }

    fun loadNote(note: Note, preserveReadingMode: Boolean = true) {
        if (!preserveReadingMode) {
            _isReadingMode.value = false
        }
        pauseSessionTimerAndPersist()
        flushAutoSave()
        _currentNote.value = note
        _textFieldValue.value = TextFieldValue(
            text = note.content,
            selection = TextRange(note.content.length)
        )
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoStates()

        if (note.folder.isNotBlank()) {
            preferencesManager.setLastActiveFolderSync(note.folder)
            viewModelScope.launch {
                preferencesManager.setLastActiveFolder(note.folder)
            }
        }

        // Persist last opened note ID
        preferencesManager.setLastOpenedNoteIdSync(note.id)
        viewModelScope.launch {
            preferencesManager.setLastOpenedNoteId(note.id)
        }

        checkAndResumeSessionTimer()
    }

    private fun startSessionTimer() {
        sessionDurationJob?.cancel()
        sessionDurationJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000L)
                val current = _currentNote.value ?: break
                val updated = current.copy(sessionDurationSec = current.sessionDurationSec + 1L)
                _currentNote.value = updated

                val now = System.currentTimeMillis()
                if (now - lastPeriodicDbFlushTime >= 10000L) {
                    lastPeriodicDbFlushTime = now
                    viewModelScope.launch(Dispatchers.IO) {
                        noteRepository.update(updated)
                    }
                }
            }
        }
    }

    fun pauseSessionTimerAndPersist() {
        sessionDurationJob?.cancel()
        sessionDurationJob = null
        val note = _currentNote.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.update(note)
        }
    }

    fun updateTextFieldValue(newValue: TextFieldValue, isUserTyping: Boolean = true) {
        if (isUserTyping && newValue.text != _textFieldValue.value.text) {
            pushUndoState(_textFieldValue.value)
        }
        _textFieldValue.value = newValue
        triggerAutoSave(newValue.text)
    }

    fun triggerAutoSave(newContent: String) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            val note = _currentNote.value ?: return@launch
            val updated = note.copy(
                content = newContent,
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.update(updated)
            _currentNote.value = updated
        }
    }

    fun flushAutoSave() {
        val note = _currentNote.value ?: return
        val currentText = _textFieldValue.value.text
        viewModelScope.launch(Dispatchers.IO) {
            val toSave = if (note.content != currentText) {
                note.copy(content = currentText, updatedAt = System.currentTimeMillis())
            } else {
                note
            }
            noteRepository.update(toSave)
        }
    }

    // In-memory Undo / Redo
    private fun pushUndoState(oldValue: TextFieldValue) {
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        undoStack.add(oldValue)
        redoStack.clear()
        updateUndoRedoStates()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _textFieldValue.value
            redoStack.add(current)
            val previous = undoStack.removeAt(undoStack.lastIndex)
            _textFieldValue.value = previous
            triggerAutoSave(previous.text)
            updateUndoRedoStates()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _textFieldValue.value
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.lastIndex)
            _textFieldValue.value = next
            triggerAutoSave(next.text)
            updateUndoRedoStates()
        }
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    // Keyboard Key Action Handler
    fun onKeyboardKey(key: KeyboardKey) {
        val currentVal = _textFieldValue.value
        val text = currentVal.text
        val selection = currentVal.selection

        when (key.actionType) {
            KeyActionType.INSERT -> {
                val output = key.outputText ?: key.label
                // Check if output contains date/time/weekday/cursor tokens
                if (output.contains("{date}") || output.contains("{time}") ||
                    output.contains("{datetime}") || output.contains("{weekday}") ||
                    output.contains("{cursor}")) {
                    val processed = StampProcessor.processTemplate(
                        template = output,
                        customDateFormat = key.dateFormat,
                        customTimeFormat = key.timeFormat
                    )
                    insertTextAtSelection(processed.outputText, processed.cursorOffsetFromStart)
                } else if (key.smartSpacing) {
                    insertSmartPunctuation(output)
                } else {
                    insertTextAtSelection(output)
                }
            }
            KeyActionType.SPACE -> {
                insertTextAtSelection(" ")
            }
            KeyActionType.ENTER -> {
                insertTextAtSelection("\n")
            }
            KeyActionType.NEW_PARAGRAPH -> {
                insertTextAtSelection("\n\n")
            }
            KeyActionType.BACKSPACE -> {
                if (selection.start != selection.end) {
                    val newText = text.removeRange(selection.min, selection.max)
                    updateTextFieldValue(
                        TextFieldValue(newText, TextRange(selection.min))
                    )
                } else if (selection.start > 0) {
                    val deletePos = selection.start - 1
                    val newText = text.removeRange(deletePos, deletePos + 1)
                    updateTextFieldValue(
                        TextFieldValue(newText, TextRange(deletePos))
                    )
                }
            }
            KeyActionType.DELETE_WORD -> {
                deleteLastWord()
            }
            KeyActionType.CURSOR_LEFT -> {
                if (selection.min > 0) {
                    val newPos = (selection.min - 1).coerceAtLeast(0)
                    _textFieldValue.value = currentVal.copy(selection = TextRange(newPos))
                }
            }
            KeyActionType.CURSOR_RIGHT -> {
                if (selection.max < text.length) {
                    val newPos = (selection.max + 1).coerceAtMost(text.length)
                    _textFieldValue.value = currentVal.copy(selection = TextRange(newPos))
                }
            }
            KeyActionType.CURSOR_UP -> {
                moveCursorVertically(up = true)
            }
            KeyActionType.CURSOR_DOWN -> {
                moveCursorVertically(up = false)
            }
            KeyActionType.WORD_LEFT -> {
                val cursor = selection.min
                val before = text.take(cursor).trimEnd()
                val lastSpace = before.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '।', '.', ',', '?', '!'))
                val newPos = if (lastSpace >= 0) lastSpace + 1 else 0
                _textFieldValue.value = currentVal.copy(selection = TextRange(newPos))
            }
            KeyActionType.WORD_RIGHT -> {
                val cursor = selection.max
                val after = text.substring(cursor)
                val firstSpace = after.indexOfAny(charArrayOf(' ', '\n', '\t', '।', '.', ',', '?', '!'))
                val newPos = if (firstSpace >= 0) {
                    (cursor + firstSpace + 1).coerceAtMost(text.length)
                } else text.length
                _textFieldValue.value = currentVal.copy(selection = TextRange(newPos))
            }
            KeyActionType.LINE_START -> {
                val cursor = selection.min
                val lineStart = text.lastIndexOf('\n', cursor - 1)
                val newPos = if (lineStart >= 0) lineStart + 1 else 0
                _textFieldValue.value = currentVal.copy(selection = TextRange(newPos))
            }
            KeyActionType.LINE_END -> {
                val cursor = selection.max
                val lineEnd = text.indexOf('\n', cursor)
                val newPos = if (lineEnd >= 0) lineEnd else text.length
                _textFieldValue.value = currentVal.copy(selection = TextRange(newPos))
            }
            KeyActionType.OPEN_TEMPLATES -> {
                showTemplatesSheet.value = true
            }
            KeyActionType.CHANGE_LANGUAGE -> {
                showLanguagePicker.value = true
            }
            KeyActionType.UNDO -> {
                undo()
            }
            KeyActionType.REDO -> {
                redo()
            }
            KeyActionType.SELECT_ALL -> {
                _textFieldValue.value = currentVal.copy(selection = TextRange(0, text.length))
            }
            KeyActionType.PASTE -> {
                val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val item = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                if (!item.isNullOrEmpty()) {
                    insertTextAtSelection(item)
                }
            }
            KeyActionType.TOGGLE_SYSTEM_KEYBOARD -> {
                _userWantsBuiltInKeyboard.value = !_userWantsBuiltInKeyboard.value
            }
            KeyActionType.SWITCH_LAYOUT -> {
                // Not used in 3x10 fixed grid
            }
            KeyActionType.DISMISS_KEYBOARD -> {
                _userWantsBuiltInKeyboard.value = false
            }
        }
    }

    private fun insertSmartPunctuation(mark: String) {
        val currentVal = _textFieldValue.value
        val text = currentVal.text
        val selection = currentVal.selection
        val start = selection.min
        val end = selection.max

        // Check if there is one trailing space right before the cursor
        val deleteTrailingSpace = (start > 0 && text[start - 1] == ' ')
        val actualStart = if (deleteTrailingSpace) start - 1 else start

        val insertion = "$mark "
        val newText = text.substring(0, actualStart) + insertion + text.substring(end)
        val newCursor = (actualStart + insertion.length).coerceIn(0, newText.length)

        updateTextFieldValue(
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        )
    }

    private fun moveCursorVertically(up: Boolean) {
        val currentVal = _textFieldValue.value
        val text = currentVal.text
        val cursor = currentVal.selection.min
        val lines = text.split("\n")

        var currentLineIndex = 0
        var charCount = 0
        var colInLine = 0

        for ((index, line) in lines.withIndex()) {
            val lineLengthWithNewline = line.length + 1
            if (cursor <= charCount + line.length) {
                currentLineIndex = index
                colInLine = cursor - charCount
                break
            }
            charCount += lineLengthWithNewline
        }

        val targetLineIndex = if (up) currentLineIndex - 1 else currentLineIndex + 1
        if (targetLineIndex in lines.indices) {
            var targetCharOffset = 0
            for (i in 0 until targetLineIndex) {
                targetCharOffset += lines[i].length + 1
            }
            val targetLine = lines[targetLineIndex]
            val newCursor = (targetCharOffset + minOf(colInLine, targetLine.length)).coerceIn(0, text.length)
            _textFieldValue.value = currentVal.copy(selection = TextRange(newCursor))
        } else {
            // Reached start or end
            val newCursor = if (up) 0 else text.length
            _textFieldValue.value = currentVal.copy(selection = TextRange(newCursor))
        }
    }

    fun insertTextAtSelection(insertText: String, cursorRelativeOffset: Int = insertText.length) {
        val currentVal = _textFieldValue.value
        val text = currentVal.text
        val selection = currentVal.selection

        val start = selection.min
        val end = selection.max

        val newText = text.substring(0, start) + insertText + text.substring(end)
        val newCursor = (start + cursorRelativeOffset).coerceIn(0, newText.length)

        updateTextFieldValue(
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        )
    }

    // Templates & Stamps
    fun insertTemplate(template: Stamp) {
        val processed = StampProcessor.processTemplate(template.templateText)
        insertTextAtSelection(processed.outputText, processed.cursorOffsetFromStart)
    }

    fun saveTemplate(template: Stamp) {
        viewModelScope.launch(Dispatchers.IO) {
            if (template.id == 0L) {
                stampRepository.insert(template)
            } else {
                stampRepository.update(template)
            }
        }
    }

    fun deleteTemplate(template: Stamp) {
        viewModelScope.launch(Dispatchers.IO) {
            stampRepository.delete(template)
        }
    }

    // Keyboard Customization & Persistence
    fun updateKeyInLayout(updatedKey: KeyboardKey) {
        val current = _keyboardLayout.value
        val newRows = current.rows.map { row ->
            val newKeys = row.keys.map { k ->
                if (k.id == updatedKey.id) updatedKey else k
            }
            row.copy(keys = newKeys)
        }
        val newLayout = current.copy(rows = newRows)
        _keyboardLayout.value = newLayout

        viewModelScope.launch(Dispatchers.IO) {
            val json = KeyboardLayouts.serializeToJson(newLayout)
            preferencesManager.setKeyboardLayoutJson(json)
        }
    }

    fun resetKeyToDefault(keyToReset: KeyboardKey) {
        val defaultLayout = KeyboardLayouts.getDefault3x10Layout()
        val defaultKey = defaultLayout.rows.flatMap { it.keys }.find { it.id == keyToReset.id } ?: return
        updateKeyInLayout(defaultKey)
    }

    fun resetKeyboardLayout() {
        val defaultLayout = KeyboardLayouts.getDefault3x10Layout()
        _keyboardLayout.value = defaultLayout
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.setKeyboardLayoutJson(null)
        }
    }

    fun importKeyboardLayoutJson(jsonStr: String): Boolean {
        return try {
            val layout = KeyboardLayouts.deserializeFromJson(jsonStr)
            _keyboardLayout.value = layout
            viewModelScope.launch(Dispatchers.IO) {
                preferencesManager.setKeyboardLayoutJson(jsonStr)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exportKeyboardLayoutJson(): String {
        return KeyboardLayouts.serializeToJson(_keyboardLayout.value)
    }

    fun setUserWantsBuiltInKeyboard(wants: Boolean) {
        _userWantsBuiltInKeyboard.value = wants
    }

    // Dictated Text Processing via Pipeline
    private suspend fun processAndCommitDictatedText(rawSpeech: String) {
        val activeLang = _currentNote.value?.languageCode ?: defaultLanguage.value
        val activeCommands = withContext(Dispatchers.IO) {
            spokenCommandRepository.getCommandsForLanguage(if (activeLang.startsWith("bn")) "bn" else "en")
        }

        val pipeline = TextPipeline.createDefault(
            spacingEnabled = pipelineSpacingPref.value,
            autoCapEnabled = pipelineAutoCapPref.value,
            commandsEnabled = pipelineCommandsPref.value
        )

        val currentVal = _textFieldValue.value
        val preceding = currentVal.text.take(currentVal.selection.min)

        val context = PipelineContext(
            languageCode = activeLang,
            precedingText = preceding,
            activeCommands = activeCommands,
            onTriggerAction = { actionCode ->
                handleSpokenAction(actionCode)
            }
        )

        val processedText = pipeline.execute(rawSpeech, context)

        if (processedText.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                when (insertionModePref.value) {
                    "APPEND_END" -> {
                        val currentText = _textFieldValue.value.text
                        val separator = if (currentText.isNotEmpty() && !currentText.endsWith(" ") && !currentText.endsWith("\n")) " " else ""
                        val finalText = currentText + separator + processedText
                        updateTextFieldValue(
                            TextFieldValue(finalText, TextRange(finalText.length))
                        )
                    }
                    "REPLACE_SELECTION", "AT_CURSOR" -> {
                        insertTextAtSelection(processedText)
                    }
                    else -> insertTextAtSelection(processedText)
                }
            }
        }
    }

    private fun handleSpokenAction(actionCode: String) {
        viewModelScope.launch(Dispatchers.Main) {
            when (actionCode) {
                "DELETE_LAST_WORD" -> {
                    deleteLastWord()
                }
                "UNDO" -> {
                    undo()
                }
                "STOP_DICTATION" -> {
                    stopDictation()
                }
            }
        }
    }

    private fun deleteLastWord() {
        val currentVal = _textFieldValue.value
        val text = currentVal.text
        val cursor = currentVal.selection.min
        if (cursor <= 0) return

        val beforeCursor = text.substring(0, cursor)
        val trimmed = beforeCursor.trimEnd()
        val lastSpaceIndex = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '।', '.', ',', '?', '!'))

        val newCursor = if (lastSpaceIndex >= 0) lastSpaceIndex + 1 else 0
        val newText = text.substring(0, newCursor) + text.substring(cursor)

        updateTextFieldValue(
            TextFieldValue(newText, TextRange(newCursor))
        )
    }

    // Dictation Supervisor Controls
    fun toggleDictation() {
        when (dictationState.value) {
            is DictationState.Idle -> startDictation()
            is DictationState.Listening, is DictationState.Starting, is DictationState.Restarting -> stopDictation()
            is DictationState.Paused -> resumeDictation()
            is DictationState.Error -> startDictation()
        }
    }

    fun startDictation() {
        _isReadingMode.value = false
        val lang = _currentNote.value?.languageCode ?: defaultLanguage.value
        val mode = DictationMode.valueOf(dictationModePref.value)

        speechSupervisor.startDictation(
            language = lang,
            mode = mode,
            offline = offlinePreferredPref.value,
            muteBeepMethod = muteBeepMethodPref.value,
            pauseToleranceSec = pauseToleranceSecPref.value,
            idleStopMin = idleAutoStopMinPref.value,
            timedDurationMin = timedMinutesPref.value
        )

        // Start background foreground service for microphone
        DictationService.startService(getApplication())
    }

    fun stopDictation() {
        speechSupervisor.stopDictation()
        DictationService.stopService(getApplication())
    }

    fun pauseDictation() {
        speechSupervisor.pauseDictation()
    }

    fun resumeDictation() {
        speechSupervisor.resumeDictation()
    }

    fun setDictationMode(mode: DictationMode) {
        viewModelScope.launch {
            preferencesManager.setDictationMode(mode.name)
        }
    }

    fun onPushToTalkPress(isPressed: Boolean) {
        if (isPressed) {
            startDictation()
        } else {
            speechSupervisor.pauseDictation()
        }
    }

    // Language
    fun setNoteLanguage(langCode: String) {
        val note = _currentNote.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(languageCode = langCode)
            noteRepository.update(updated)
            _currentNote.value = updated
        }
    }

    fun cycleFavoriteLanguage() {
        val favorites = favoriteLanguages.value.toList()
        if (favorites.isEmpty()) return
        val currentLang = _currentNote.value?.languageCode ?: defaultLanguage.value
        val currentIndex = favorites.indexOfFirst { it.equals(currentLang, ignoreCase = true) }
        val nextIndex = if (currentIndex != -1) (currentIndex + 1) % favorites.size else 0
        setNoteLanguage(favorites[nextIndex])
    }

    fun toggleFavoriteLanguage(langCode: String) {
        val current = favoriteLanguages.value.toMutableSet()
        if (current.contains(langCode)) {
            if (current.size > 1) current.remove(langCode)
        } else {
            current.add(langCode)
        }
        viewModelScope.launch {
            preferencesManager.setFavoriteLanguages(current)
        }
    }

    // Notes Management
    fun createNewNote(
        folder: String? = null,
        template: Stamp? = null,
        customTitle: String? = null,
        startRecording: Boolean = false
    ) {
        _isReadingMode.value = false
        flushAutoSave()
        viewModelScope.launch(Dispatchers.IO) {
            val defaultLang = defaultLanguage.value
            val locationMode = preferencesManager.newNoteLocationMode.first()
            val fixedFolder = preferencesManager.fixedNewNoteFolder.first()
            val lastActive = preferencesManager.getLastActiveFolderSync()

            val targetFolder = when {
                !folder.isNullOrBlank() -> folder
                locationMode == "LAST_WORKED_ON" -> if (lastActive.isNotBlank()) lastActive else "General"
                locationMode == "CURRENT_OPEN" -> _currentNote.value?.folder ?: "General"
                locationMode == "FIXED" -> if (fixedFolder.isNotBlank()) fixedFolder else "General"
                locationMode == "ROOT" -> "General"
                locationMode == "ASK_POPUP" -> if (lastActive.isNotBlank()) lastActive else "General"
                else -> if (lastActive.isNotBlank()) lastActive else "General"
            }

            preferencesManager.setLastActiveFolder(targetFolder)

            val namingRule = preferencesManager.newNoteNamingRule.first()
            val initialTitle = if (!customTitle.isNullOrBlank()) {
                customTitle
            } else if (template != null) {
                template.label
            } else {
                when (namingRule) {
                    "UNTITLED" -> "Untitled note"
                    "DATE_TIME" -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    "DATE_ONLY" -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    "FIRST_LINE" -> "Untitled note"
                    "ASK_ME" -> "Untitled note"
                    else -> "Untitled note"
                }
            }

            val initialContent = if (template != null) {
                StampProcessor.processTemplate(template.templateText).outputText
            } else ""

            val newNote = Note(
                title = initialTitle,
                content = initialContent,
                languageCode = defaultLang,
                folder = targetFolder,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = noteRepository.insert(newNote)
            val inserted = noteRepository.getNoteOnce(id)

            val afterAction = preferencesManager.afterCreatingNote.first()

            withContext(Dispatchers.Main) {
                if (inserted != null) {
                    if (afterAction == "OPEN_EDITOR") {
                        loadNote(inserted)
                        isNoteManagerOpen.value = false
                        if (startRecording) {
                            startDictation()
                        }
                    } else {
                        isNoteManagerOpen.value = true
                    }
                }
            }
        }
    }

    fun renameCurrentNote(newTitle: String) {
        val note = _currentNote.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(title = newTitle, updatedAt = System.currentTimeMillis())
            noteRepository.update(updated)
            _currentNote.value = updated
        }
    }

    fun togglePinCurrentNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(isPinned = !note.isPinned)
            noteRepository.update(updated)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = updated
            }
        }
    }

    // Soft Delete (Trash)
    fun trashNote(note: Note) {
        lastDeletedNote = note
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(isDeleted = true, deletedAt = System.currentTimeMillis(), isPinned = false)
            noteRepository.update(updated)
            if (_currentNote.value?.id == note.id) {
                val remaining = noteRepository.getLatestActiveNote()
                withContext(Dispatchers.Main) {
                    if (remaining != null) {
                        loadNote(remaining)
                    } else {
                        createNewNote()
                    }
                }
            }
        }
    }

    fun trashNotes(notes: List<Note>) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val updated = notes.map { it.copy(isDeleted = true, deletedAt = now, isPinned = false) }
            noteRepository.updateAll(updated)
            if (notes.any { it.id == _currentNote.value?.id }) {
                val remaining = noteRepository.getLatestActiveNote()
                withContext(Dispatchers.Main) {
                    if (remaining != null) {
                        loadNote(remaining)
                    } else {
                        createNewNote()
                    }
                }
            }
        }
    }

    // Restore from Trash
    fun restoreNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(isDeleted = false, deletedAt = null)
            noteRepository.update(updated)
        }
    }

    fun restoreNotes(notes: List<Note>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = notes.map { it.copy(isDeleted = false, deletedAt = null) }
            noteRepository.updateAll(updated)
        }
    }

    // Permanent Delete
    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.delete(note)
        }
    }

    fun permanentlyDeleteNotes(notes: List<Note>) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.deleteAll(notes)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.emptyTrash()
        }
    }

    // Archive / Unarchive
    fun toggleArchiveNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(isArchived = !note.isArchived)
            noteRepository.update(updated)
            if (updated.isArchived && _currentNote.value?.id == note.id) {
                val remaining = noteRepository.getLatestActiveNote()
                withContext(Dispatchers.Main) {
                    if (remaining != null) loadNote(remaining) else createNewNote()
                }
            }
        }
    }

    fun archiveNotes(notes: List<Note>, archive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = notes.map { it.copy(isArchived = archive) }
            noteRepository.updateAll(updated)
            if (archive && notes.any { it.id == _currentNote.value?.id }) {
                val remaining = noteRepository.getLatestActiveNote()
                withContext(Dispatchers.Main) {
                    if (remaining != null) loadNote(remaining) else createNewNote()
                }
            }
        }
    }

    // Multi-Select: Merge Notes
    fun mergeNotes(notesToMerge: List<Note>, mergedTitle: String) {
        if (notesToMerge.isEmpty()) return
        flushAutoSave()
        viewModelScope.launch(Dispatchers.IO) {
            val sorted = notesToMerge.sortedBy { it.createdAt }
            val combinedBody = buildString {
                sorted.forEachIndexed { idx, n ->
                    if (idx > 0) append("\n\n─────────────\n\n")
                    append("### ${n.title.ifBlank { "Untitled" }}\n")
                    append(n.content)
                }
            }
            val primaryFolder = sorted.firstOrNull()?.folder ?: "General"
            val combinedTags = sorted.flatMap { it.tags.split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(",")

            val mergedNote = Note(
                title = mergedTitle.ifBlank { "Merged Note (${sorted.size} notes)" },
                content = combinedBody,
                folder = primaryFolder,
                tags = combinedTags,
                languageCode = defaultLanguage.value,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = noteRepository.insert(mergedNote)

            // Send original merged notes to trash
            val now = System.currentTimeMillis()
            val trashed = sorted.map { it.copy(isDeleted = true, deletedAt = now) }
            noteRepository.updateAll(trashed)

            val inserted = noteRepository.getNoteOnce(newId)
            withContext(Dispatchers.Main) {
                if (inserted != null) {
                    loadNote(inserted)
                    isNoteManagerOpen.value = false
                }
            }
        }
    }

    // Multi-Select: Move to Folder
    fun moveNotesToFolder(notes: List<Note>, targetFolder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = notes.map { it.copy(folder = targetFolder, updatedAt = System.currentTimeMillis()) }
            noteRepository.updateAll(updated)
            if (notes.any { it.id == _currentNote.value?.id }) {
                _currentNote.value = _currentNote.value?.copy(folder = targetFolder)
            }
        }
    }

    // Multi-Select: Add Tags
    fun addTagsToNotes(notes: List<Note>, newTags: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = notes.map { n ->
                val existing = n.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val merged = (existing + newTags).distinct().joinToString(",")
                n.copy(tags = merged, updatedAt = System.currentTimeMillis())
            }
            noteRepository.updateAll(updated)
            if (notes.any { it.id == _currentNote.value?.id }) {
                _currentNote.value = _currentNote.value?.let { curr ->
                    val existing = curr.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    curr.copy(tags = (existing + newTags).distinct().joinToString(","))
                }
            }
        }
    }

    fun deleteNote(note: Note, onDeleted: () -> Unit) {
        trashNote(note)
        onDeleted()
    }

    fun restoreLastDeletedNote() {
        val note = lastDeletedNote ?: return
        restoreNote(note)
        loadNote(note)
    }

    // Notes Manager & Tree Explorer Operations
    fun setShowNoteMetricsInList(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setShowNoteMetricsInList(show)
        }
    }

    fun setNoteMetricsStyle(style: String) {
        viewModelScope.launch {
            preferencesManager.setNoteMetricsStyle(style)
        }
    }

    fun toggleFolderExpanded(folder: String, expanded: Boolean) {
        viewModelScope.launch {
            val current = expandedFolders.value.toMutableSet()
            if (expanded) {
                current.add(folder)
            } else {
                current.remove(folder)
            }
            preferencesManager.setExpandedFolders(current)
        }
    }

    fun setAllFoldersExpanded(expanded: Boolean, allFolders: Set<String>) {
        viewModelScope.launch {
            val newSet = if (expanded) allFolders else emptySet()
            preferencesManager.setExpandedFolders(newSet)
        }
    }

    fun addCustomFolder(folder: String) {
        val trimmed = folder.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val current = customFolders.value.toMutableSet()
            current.add(trimmed)
            preferencesManager.setCustomFolders(current)
            toggleFolderExpanded(trimmed, true)
        }
    }

    fun renameFolder(oldFolder: String, newFolder: String) {
        val trimmed = newFolder.trim()
        if (trimmed.isBlank() || trimmed.equals(oldFolder, ignoreCase = true)) return
        viewModelScope.launch(Dispatchers.IO) {
            val all = noteRepository.allNotes.first()
            val notesToUpdate = all.filter {
                it.folder.equals(oldFolder, ignoreCase = true) || it.folder.startsWith("$oldFolder/", ignoreCase = true)
            }
            val updated = notesToUpdate.map {
                val newPath = if (it.folder.equals(oldFolder, ignoreCase = true)) {
                    trimmed
                } else {
                    trimmed + it.folder.substring(oldFolder.length)
                }
                it.copy(folder = newPath, updatedAt = System.currentTimeMillis())
            }
            noteRepository.updateAll(updated)

            val currentCustom = customFolders.value.toMutableSet()
            val toAdd = mutableSetOf<String>()
            val toRemove = mutableSetOf<String>()
            for (f in currentCustom) {
                if (f.equals(oldFolder, ignoreCase = true)) {
                    toRemove.add(f)
                    toAdd.add(trimmed)
                } else if (f.startsWith("$oldFolder/", ignoreCase = true)) {
                    toRemove.add(f)
                    toAdd.add(trimmed + f.substring(oldFolder.length))
                }
            }
            currentCustom.removeAll(toRemove)
            currentCustom.addAll(toAdd)
            preferencesManager.setCustomFolders(currentCustom)

            val currentExp = expandedFolders.value.toMutableSet()
            val expToAdd = mutableSetOf<String>()
            val expToRemove = mutableSetOf<String>()
            for (f in currentExp) {
                if (f.equals(oldFolder, ignoreCase = true)) {
                    expToRemove.add(f)
                    expToAdd.add(trimmed)
                } else if (f.startsWith("$oldFolder/", ignoreCase = true)) {
                    expToRemove.add(f)
                    expToAdd.add(trimmed + f.substring(oldFolder.length))
                }
            }
            currentExp.removeAll(expToRemove)
            currentExp.addAll(expToAdd)
            preferencesManager.setExpandedFolders(currentExp)

            if (_currentNote.value?.folder.equals(oldFolder, ignoreCase = true)) {
                _currentNote.value = _currentNote.value?.copy(folder = trimmed)
            } else if (_currentNote.value?.folder?.startsWith("$oldFolder/", ignoreCase = true) == true) {
                val curFolder = _currentNote.value!!.folder
                _currentNote.value = _currentNote.value?.copy(folder = trimmed + curFolder.substring(oldFolder.length))
            }
        }
    }

    fun deleteFolder(folder: String, deleteContainedNotes: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = noteRepository.allNotes.first()
            val notesInFolder = all.filter {
                it.folder.equals(folder, ignoreCase = true) || it.folder.startsWith("$folder/", ignoreCase = true)
            }
            if (deleteContainedNotes) {
                val now = System.currentTimeMillis()
                val trashed = notesInFolder.map { it.copy(isDeleted = true, deletedAt = now, isPinned = false) }
                noteRepository.updateAll(trashed)
            } else {
                val moved = notesInFolder.map { it.copy(folder = "General", updatedAt = System.currentTimeMillis()) }
                noteRepository.updateAll(moved)
            }
            val currentCustom = customFolders.value.toMutableSet()
            val toRemove = currentCustom.filter {
                it.equals(folder, ignoreCase = true) || it.startsWith("$folder/", ignoreCase = true)
            }.toSet()
            currentCustom.removeAll(toRemove)
            preferencesManager.setCustomFolders(currentCustom)

            val currentExp = expandedFolders.value.toMutableSet()
            val expToRemove = currentExp.filter {
                it.equals(folder, ignoreCase = true) || it.startsWith("$folder/", ignoreCase = true)
            }.toSet()
            currentExp.removeAll(expToRemove)
            preferencesManager.setExpandedFolders(currentExp)
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val copy = note.copy(
                id = 0,
                title = if (note.title.isNotBlank()) "${note.title} (Copy)" else "Untitled Note (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isPinned = false
            )
            val newId = noteRepository.insert(copy)
            val inserted = noteRepository.getNoteOnce(newId)
            if (inserted != null) {
                withContext(Dispatchers.Main) {
                    loadNote(inserted)
                }
            }
        }
    }

    fun renameNote(note: Note, newTitle: String) {
        val trimmed = newTitle.trim()
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(title = trimmed, updatedAt = System.currentTimeMillis())
            noteRepository.update(updated)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = updated
            }
        }
    }

    // Template Operations
    fun togglePinTemplate(stamp: Stamp) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = stamp.copy(isPinned = !stamp.isPinned)
            stampRepository.update(updated)
        }
    }

    fun reorderTemplates(stamps: List<Stamp>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = stamps.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
            stampRepository.updateAll(updated)
        }
    }

    fun setTrashAutoEmptyDaysPref(days: Int) {
        viewModelScope.launch {
            preferencesManager.setTrashAutoEmptyDays(days)
        }
    }

    fun setDefaultNewNoteFolderPref(folder: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultNewNoteFolder(folder)
        }
    }

    fun clearCurrentNoteText() {
        pushUndoState(_textFieldValue.value)
        updateTextFieldValue(TextFieldValue(""))
    }

    // Styling & Appearance Settings
    fun setFontSize(size: Float) {
        viewModelScope.launch { preferencesManager.setEditorFontSize(size) }
    }

    fun zoomIn() {
        val newSize = (editorFontSize.value + 2f).coerceAtMost(32f)
        setFontSize(newSize)
    }

    fun zoomOut() {
        val newSize = (editorFontSize.value - 2f).coerceAtLeast(12f)
        setFontSize(newSize)
    }

    fun setLineSpacing(spacing: Float) {
        viewModelScope.launch { preferencesManager.setEditorLineSpacing(spacing) }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch { preferencesManager.setEditorFontFamily(family) }
    }

    fun setNoteHighlight(tintKey: String?) {
        val note = _currentNote.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = note.copy(highlightColor = tintKey)
            noteRepository.update(updated)
            _currentNote.value = updated
        }
    }

    fun toggleKeyboard() {
        _userWantsBuiltInKeyboard.value = !_userWantsBuiltInKeyboard.value
    }

    fun setKeyboardKeyHeightDp(dp: Int) = viewModelScope.launch { preferencesManager.setKeyboardKeyHeightDp(dp) }
    fun setKeyboardFontSizeSp(sp: Int) = viewModelScope.launch { preferencesManager.setKeyboardFontSizeSp(sp) }
    fun setKeyboardHaptic(enabled: Boolean) = viewModelScope.launch { preferencesManager.setKeyboardHaptic(enabled) }
    fun setKeyboardHoldBehavior(behavior: String) = viewModelScope.launch { preferencesManager.setKeyboardHoldBehavior(behavior) }
    fun setKeyboardEditMode(editMode: Boolean) = viewModelScope.launch { preferencesManager.setKeyboardEditMode(editMode) }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    // Settings proxies
    fun setDictationModePref(mode: String) = viewModelScope.launch { preferencesManager.setDictationMode(mode) }
    fun setTimedMinutesPref(min: Int) = viewModelScope.launch { preferencesManager.setTimedMinutes(min) }
    fun setPauseToleranceSecPref(sec: Int) = viewModelScope.launch { preferencesManager.setPauseToleranceSeconds(sec) }
    fun setIdleAutoStopMinPref(min: Int) = viewModelScope.launch { preferencesManager.setIdleAutoStopMinutes(min) }
    fun setMuteBeepMethodPref(method: String) = viewModelScope.launch { preferencesManager.setMuteBeepMethod(method) }
    fun setInsertionModePref(mode: String) = viewModelScope.launch { preferencesManager.setInsertionMode(mode) }
    fun setOfflinePreferredPref(offline: Boolean) = viewModelScope.launch { preferencesManager.setOfflinePreferred(offline) }
    fun setKeepScreenOnPref(keep: Boolean) = viewModelScope.launch { preferencesManager.setKeepScreenOn(keep) }
    fun setAppThemePref(theme: String) = viewModelScope.launch { preferencesManager.setAppTheme(theme) }
    fun setPipelineSpacingPref(enabled: Boolean) = viewModelScope.launch { preferencesManager.setPipelineSpacing(enabled) }
    fun setPipelineAutoCapPref(enabled: Boolean) = viewModelScope.launch { preferencesManager.setPipelineAutoCap(enabled) }
    fun setPipelineCommandsPref(enabled: Boolean) = viewModelScope.launch { preferencesManager.setPipelineCommands(enabled) }
    fun setShowWordCountPref(show: Boolean) = viewModelScope.launch { preferencesManager.setShowWordCount(show) }
    fun setDefaultLanguagePref(lang: String) = viewModelScope.launch { preferencesManager.setDefaultLanguage(lang) }

    // Appearance & Custom Theme
    fun setCustomThemeColors(primary: Int, bg: Int, surface: Int, text: Int, secondary: Int) {
        viewModelScope.launch {
            preferencesManager.setCustomThemeColors(primary, bg, surface, text, secondary)
        }
    }

    // Typography
    fun setCustomFont(name: String, path: String?) {
        viewModelScope.launch { preferencesManager.setCustomFont(name, path) }
    }
    fun setLetterSpacing(spacing: Float) = viewModelScope.launch { preferencesManager.setEditorLetterSpacing(spacing) }
    fun setParagraphSpacing(spacing: Int) = viewModelScope.launch { preferencesManager.setEditorParagraphSpacing(spacing) }
    fun setTextAlignment(align: String) = viewModelScope.launch { preferencesManager.setEditorTextAlignment(align) }
    fun setMarginDp(margin: Int) = viewModelScope.launch { preferencesManager.setEditorMarginDp(margin) }
    fun setMaxWidthDp(width: Int) = viewModelScope.launch { preferencesManager.setEditorMaxWidthDp(width) }

    // Toolbar Order & Visibility
    fun setToolbarItemOrder(order: String) = viewModelScope.launch { preferencesManager.setToolbarItemOrder(order) }
    fun setToolbarHiddenItems(hidden: Set<String>) = viewModelScope.launch { preferencesManager.setToolbarHiddenItems(hidden) }

    fun moveToolbarItem(fromIndex: Int, toIndex: Int) {
        val currentOrder = toolbarItemOrder.value.split(",").map { it.trim() }.toMutableList()
        if (fromIndex in currentOrder.indices && toIndex in currentOrder.indices) {
            val item = currentOrder.removeAt(fromIndex)
            currentOrder.add(toIndex, item)
            setToolbarItemOrder(currentOrder.joinToString(","))
        }
    }

    fun toggleToolbarItemVisibility(item: String) {
        val currentHidden = toolbarHiddenItems.value.toMutableSet()
        if (currentHidden.contains(item)) {
            currentHidden.remove(item)
        } else {
            currentHidden.add(item)
        }
        setToolbarHiddenItems(currentHidden)
    }

    fun resetToolbarOrder() {
        setToolbarItemOrder("DICTATION_STATUS,METRICS,KEYBOARD_MODE,TEXT_SIZE,TEXT_STYLE,AUDIO_SOURCE")
        setToolbarHiddenItems(emptySet())
    }

    // Mic Button & Cluster Settings
    fun setMicButtonSize(size: String) = viewModelScope.launch { preferencesManager.setMicButtonSize(size) }
    fun setMicButtonShape(shape: String) = viewModelScope.launch { preferencesManager.setMicButtonShape(shape) }
    fun setMicButtonColor(color: String) = viewModelScope.launch { preferencesManager.setMicButtonColor(color) }
    fun setMicButtonPosition(pos: String) = viewModelScope.launch { preferencesManager.setMicButtonPosition(pos) }
    fun setMicButtonOffsetY(offset: Int) = viewModelScope.launch { preferencesManager.setMicButtonOffsetY(offset) }

    // Visual Polish, Animation & Haptics
    fun setStatusChipStyle(style: String) = viewModelScope.launch { preferencesManager.setStatusChipStyle(style) }
    fun setUiDensity(density: String) = viewModelScope.launch { preferencesManager.setUiDensity(density) }
    fun setCornerRadiusDp(radius: Int) = viewModelScope.launch { preferencesManager.setCornerRadiusDp(radius) }
    fun setDictationAnimationStyle(style: String) = viewModelScope.launch { preferencesManager.setDictationAnimationStyle(style) }
    fun setHapticsLevel(level: String) = viewModelScope.launch { preferencesManager.setHapticsLevel(level) }

    // Notes Manager Settings Flows & Methods
    val newNoteLocationMode: StateFlow<String> = preferencesManager.newNoteLocationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LAST_WORKED_ON")
    val fixedNewNoteFolder: StateFlow<String> = preferencesManager.fixedNewNoteFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "General")
    val askBeforeCreating: StateFlow<Boolean> = preferencesManager.askBeforeCreating
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val askChooseTemplateToggle: StateFlow<Boolean> = preferencesManager.askChooseTemplateToggle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val askStartRecordingToggle: StateFlow<Boolean> = preferencesManager.askStartRecordingToggle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val newNoteNamingRule: StateFlow<String> = preferencesManager.newNoteNamingRule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "UNTITLED")
    val afterCreatingNote: StateFlow<String> = preferencesManager.afterCreatingNote
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "OPEN_EDITOR")
    val lastActiveFolder: StateFlow<String> = preferencesManager.lastActiveFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "General")

    fun setNewNoteLocationMode(mode: String) = viewModelScope.launch { preferencesManager.setNewNoteLocationMode(mode) }
    fun setFixedNewNoteFolder(folder: String) = viewModelScope.launch { preferencesManager.setFixedNewNoteFolder(folder) }
    fun setAskBeforeCreating(ask: Boolean) = viewModelScope.launch { preferencesManager.setAskBeforeCreating(ask) }
    fun setAskChooseTemplateToggle(toggle: Boolean) = viewModelScope.launch { preferencesManager.setAskChooseTemplateToggle(toggle) }
    fun setAskStartRecordingToggle(toggle: Boolean) = viewModelScope.launch { preferencesManager.setAskStartRecordingToggle(toggle) }
    fun setNewNoteNamingRule(rule: String) = viewModelScope.launch { preferencesManager.setNewNoteNamingRule(rule) }
    fun setAfterCreatingNote(action: String) = viewModelScope.launch { preferencesManager.setAfterCreatingNote(action) }
    private var notesManagerSettingsReturnTarget: String = "SETTINGS"

    fun openNotesManagerSettings(fromNoteManager: Boolean = false) {
        notesManagerSettingsReturnTarget = if (fromNoteManager) "NOTES_MANAGER" else "SETTINGS"
        isNoteManagerOpen.value = false
        _currentScreen.value = "NOTES_MANAGER_SETTINGS"
    }

    fun onBackFromNotesManagerSettings() {
        if (notesManagerSettingsReturnTarget == "NOTES_MANAGER") {
            _currentScreen.value = "EDITOR"
            isNoteManagerOpen.value = true
        } else {
            _currentScreen.value = "SETTINGS"
        }
    }

    fun setNoteMetricsOrder(order: String) = viewModelScope.launch { preferencesManager.setNoteMetricsOrder(order) }
    fun setNoteMetricsHiddenItems(hidden: Set<String>) = viewModelScope.launch { preferencesManager.setNoteMetricsHiddenItems(hidden) }

    fun moveNoteMetricItem(fromIndex: Int, toIndex: Int) {
        val currentOrder = com.example.ui.notes.NoteMetricsConfig.parseOrder(noteMetricsOrder.value).toMutableList()
        if (fromIndex in currentOrder.indices && toIndex in currentOrder.indices) {
            val item = currentOrder.removeAt(fromIndex)
            currentOrder.add(toIndex, item)
            setNoteMetricsOrder(currentOrder.joinToString(","))
        }
    }

    fun toggleNoteMetricVisibility(itemKey: String) {
        val currentHidden = noteMetricsHiddenItems.value.toMutableSet()
        if (currentHidden.contains(itemKey)) {
            currentHidden.remove(itemKey)
        } else {
            currentHidden.add(itemKey)
        }
        setNoteMetricsHiddenItems(currentHidden)
    }

    fun resetNoteMetricsOrder() {
        setNoteMetricsOrder(com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING)
        setNoteMetricsHiddenItems(emptySet())
    }

    fun resetNotesManagerSettingsToDefault() {
        viewModelScope.launch {
            preferencesManager.setNewNoteLocationMode("LAST_WORKED_ON")
            preferencesManager.setFixedNewNoteFolder("General")
            preferencesManager.setAskBeforeCreating(false)
            preferencesManager.setAskChooseTemplateToggle(true)
            preferencesManager.setAskStartRecordingToggle(false)
            preferencesManager.setNewNoteNamingRule("UNTITLED")
            preferencesManager.setAfterCreatingNote("OPEN_EDITOR")
            preferencesManager.setShowNoteMetricsInList(true)
            preferencesManager.setNoteMetricsStyle("COMPACT")
            preferencesManager.setNoteMetricsOrder(com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING)
            preferencesManager.setNoteMetricsHiddenItems(emptySet())
            preferencesManager.setTrashAutoEmptyDays(7)
        }
    }

    // Backup & Restore Operations
    fun exportBackup(uri: Uri, onResult: (Result<BackupSummary>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            try {
                val outputStream = app.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Unable to open output destination.")
                val summary = BackupManager.exportBackup(
                    context = app,
                    noteRepository = noteRepository,
                    stampRepository = stampRepository,
                    spokenCommandRepository = spokenCommandRepository,
                    preferencesManager = preferencesManager,
                    outputStream = outputStream
                )
                withContext(Dispatchers.Main) {
                    onResult(Result.success(summary))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun validateBackup(uri: Uri, onResult: (Result<ParsedBackupData>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            try {
                val inputStream = app.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Unable to open backup file.")
                val parsed = inputStream.use {
                    BackupManager.validateAndParseBackup(it)
                }
                withContext(Dispatchers.Main) {
                    onResult(Result.success(parsed))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun restoreBackup(backupData: ParsedBackupData, onResult: (Result<BackupSummary>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val summary = BackupManager.restoreBackup(
                    noteRepository = noteRepository,
                    stampRepository = stampRepository,
                    spokenCommandRepository = spokenCommandRepository,
                    preferencesManager = preferencesManager,
                    backupData = backupData
                )
                reloadAfterImport()
                withContext(Dispatchers.Main) {
                    onResult(Result.success(summary))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    suspend fun reloadAfterImport() {
        val lastId = preferencesManager.getLastOpenedNoteIdSync()
        var noteToLoad: Note? = null
        if (lastId > 0) {
            val found = noteRepository.getNoteOnce(lastId)
            if (found != null && !found.isDeleted) {
                noteToLoad = found
            }
        }
        if (noteToLoad == null) {
            noteToLoad = noteRepository.getLatestActiveNote() ?: noteRepository.getLatestNote()
        }
        withContext(Dispatchers.Main) {
            if (noteToLoad != null) {
                loadNote(noteToLoad)
            } else {
                createNewNote()
            }
        }

        val layoutJson = preferencesManager.keyboardLayoutJson.first()
        withContext(Dispatchers.Main) {
            if (!layoutJson.isNullOrBlank()) {
                try {
                    _keyboardLayout.value = KeyboardLayouts.deserializeFromJson(layoutJson)
                } catch (_: Exception) {
                    _keyboardLayout.value = KeyboardLayouts.getDefault3x10Layout()
                }
            } else {
                _keyboardLayout.value = KeyboardLayouts.getDefault3x10Layout()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        flushAutoSave()
    }
}
