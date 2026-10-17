package com.example.ui.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import com.example.data.model.DictationMode
import com.example.data.model.DictationState
import com.example.ui.dialogs.EditKeyDialog
import com.example.ui.dialogs.LanguagePickerDialog
import com.example.ui.dialogs.OnboardingDialog
import com.example.ui.dialogs.RenameNoteDialog
import com.example.ui.dialogs.StyleSheetBottomSheet
import com.example.ui.dialogs.TemplatesBottomSheet
import com.example.ui.keyboard.InAppKeyboard
import com.example.ui.notes.NotesManagerScreen
import com.example.ui.theme.HighlightCoral
import com.example.ui.theme.HighlightCyan
import com.example.ui.theme.HighlightGold
import com.example.ui.theme.HighlightMint
import com.example.ui.theme.HighlightSaffron
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.getFontFamilyByOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Note Manager state
    val isNoteManagerOpen by viewModel.isNoteManagerOpen.collectAsState()
    val activeNotes by viewModel.activeNotes.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val trashAutoEmptyDays by viewModel.trashAutoEmptyDays.collectAsState()
    val defaultFolder by viewModel.defaultNewNoteFolder.collectAsState()
    val showNoteMetrics by viewModel.showNoteMetricsInList.collectAsState()
    val noteMetricsStyle by viewModel.noteMetricsStyle.collectAsState()
    val noteMetricsOrder by viewModel.noteMetricsOrder.collectAsState()
    val noteMetricsHiddenItems by viewModel.noteMetricsHiddenItems.collectAsState()
    val expandedFolders by viewModel.expandedFolders.collectAsState()
    val customFolders by viewModel.customFolders.collectAsState()

    // State collections
    val currentNote by viewModel.currentNote.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val allStamps by viewModel.allStamps.collectAsState()
    val textFieldValue by viewModel.textFieldValue.collectAsState()
    val isReadingMode by viewModel.isReadingMode.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val dictationState by viewModel.dictationState.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val audioLevelRms by viewModel.audioLevelRms.collectAsState()
    val audioSourceType by viewModel.audioSourceType.collectAsState()
    val isOnDevice by viewModel.isOnDevice.collectAsState()

    // Settings collections
    val defaultLanguage by viewModel.defaultLanguage.collectAsState()
    val favoriteLanguages by viewModel.favoriteLanguages.collectAsState()
    val fontSize by viewModel.editorFontSize.collectAsState()
    val lineSpacing by viewModel.editorLineSpacing.collectAsState()
    val fontFamilyOption by viewModel.editorFontFamily.collectAsState()
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
    val dictationAnimationStyle by viewModel.dictationAnimationStyle.collectAsState()
    val hapticsLevel by viewModel.hapticsLevel.collectAsState()
    val dictationModeStr by viewModel.dictationModePref.collectAsState()
    val keepScreenOn by viewModel.keepScreenOnPref.collectAsState()
    val showWordCount by viewModel.showWordCountPref.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompletedPref.collectAsState()
    val askBeforeCreating by viewModel.askBeforeCreating.collectAsState()
    val askChooseTemplateToggle by viewModel.askChooseTemplateToggle.collectAsState()
    val askStartRecordingToggle by viewModel.askStartRecordingToggle.collectAsState()
    val newNoteNamingRule by viewModel.newNoteNamingRule.collectAsState()
    val newNoteLocationMode by viewModel.newNoteLocationMode.collectAsState()
    val markdownSettings by viewModel.markdownSettings.collectAsState()

    // Editor New Note Destination Dialog States
    var showNewNoteDialogFromEditor by remember { mutableStateOf(false) }
    var editorNewNoteFolder by remember { mutableStateOf("General") }
    var editorNewNoteTitle by remember { mutableStateOf("") }
    var editorShowFolderDropdown by remember { mutableStateOf(false) }
    var editorNewFolderNameInput by remember { mutableStateOf("") }
    var editorShowNewFolderField by remember { mutableStateOf(false) }

    // Keyboard Settings & State
    val userWantsBuiltIn by viewModel.userWantsBuiltInKeyboard.collectAsState()
    val keyboardLayout by viewModel.keyboardLayout.collectAsState()
    val keyHeightDp by viewModel.keyboardKeyHeightDp.collectAsState()
    val keyboardFontSizeSp by viewModel.keyboardFontSizeSp.collectAsState()
    val keyboardHaptic by viewModel.keyboardHaptic.collectAsState()
    val keyboardHoldBehavior by viewModel.keyboardHoldBehavior.collectAsState()
    val keyboardEditMode by viewModel.keyboardEditMode.collectAsState()

    // Dialog state collections
    val showStyleSheet by viewModel.showStyleSheet.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showTemplatesSheet by viewModel.showTemplatesSheet.collectAsState()
    val editingKey by viewModel.editingKey.collectAsState()

    // IME Detection via WindowInsets
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeBottom = imeInsets.getBottom(density)
    val isImeVisible = imeBottom > 0

    val builtInKeyboardVisible = userWantsBuiltIn

    // Handle switching between In-App Custom Keyboard and System Keyboard
    LaunchedEffect(userWantsBuiltIn) {
        if (!userWantsBuiltIn) {
            // Switching to System Keyboard: request focus and show soft input
            focusRequester.requestFocus()
            keyboardController?.show()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val activity = context as? Activity
            val view = activity?.currentFocus ?: activity?.window?.decorView
            if (view != null) {
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            // Switching to Custom In-App Keyboard: hide system keyboard and retain editor focus
            keyboardController?.hide()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val windowToken = (context as? Activity)?.window?.decorView?.windowToken
            if (windowToken != null) {
                imm?.hideSoftInputFromWindow(windowToken, 0)
            }
            focusRequester.requestFocus()
        }
    }

    // Hide system keyboard if it somehow becomes visible while user is in custom keyboard mode
    LaunchedEffect(userWantsBuiltIn, isImeVisible) {
        if (userWantsBuiltIn && isImeVisible) {
            keyboardController?.hide()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val windowToken = (context as? Activity)?.window?.decorView?.windowToken
            if (windowToken != null) {
                imm?.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }

    // Automatically hide system keyboard when Note Manager opens
    LaunchedEffect(isNoteManagerOpen) {
        if (isNoteManagerOpen) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        } else {
            // Restore editor focus when Note Manager closes only if not in reading mode
            if (!isReadingMode) {
                focusRequester.requestFocus()
            }
        }
    }

    // Request focus on editor when note is opened/loaded initially (never automatically on exiting reading mode)
    LaunchedEffect(currentNote?.id) {
        if (!isReadingMode) {
            focusRequester.requestFocus()
        }
    }

    // When entering or exiting reading mode, ensure focus is explicitly cleared and soft keyboard is hidden
    LaunchedEffect(isReadingMode) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    // Immersive mode: hide navigation bars when in Reading Mode (and Note Manager not open), restore on exit or pause/dispose
    DisposableEffect(isReadingMode, isNoteManagerOpen) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null && isReadingMode && !isNoteManagerOpen) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }



    // Keep screen on when dictating or setting is enabled
    DisposableEffect(keepScreenOn, dictationState) {
        val activity = context as? Activity
        val shouldKeepOn = keepScreenOn && (dictationState !is DictationState.Idle)
        if (shouldKeepOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val activeLanguage = currentNote?.languageCode ?: defaultLanguage
    val currentDictationMode = remember(dictationModeStr) {
        try { DictationMode.valueOf(dictationModeStr) } catch (_: Exception) { DictationMode.CONTINUOUS }
    }

    // Metrics
    val wordCount = remember(textFieldValue.text) {
        if (textFieldValue.text.isBlank()) 0
        else textFieldValue.text.trim().split("\\s+".toRegex()).size
    }
    val charCount = remember(textFieldValue.text) {
        textFieldValue.text.length
    }

    // Note highlight tint
    val highlightBgColor = remember(currentNote?.highlightColor) {
        when (currentNote?.highlightColor) {
            "SAFFRON" -> HighlightSaffron
            "GOLD" -> HighlightGold
            "CORAL" -> HighlightCoral
            "MINT" -> HighlightMint
            "CYAN" -> HighlightCyan
            else -> Color.Transparent
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Performing gesture by swiping from left to right on screen launches Note Manager!
            .pointerInput(isNoteManagerOpen) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 35f && !isNoteManagerOpen) {
                        change.consume()
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        viewModel.isNoteManagerOpen.value = true
                    } else if (dragAmount < -35f && !isNoteManagerOpen) {
                        change.consume()
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        viewModel.navigateTo("SETTINGS")
                    }
                }
            }
    ) {
        // Main Editor View
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .testTag("editor_screen"),
            topBar = {
                if (!isReadingMode) {
                    Column {
                        EditorTopBar(
                            noteTitle = currentNote?.title ?: "Untitled Note",
                            currentLanguageCode = activeLanguage,
                            isKeyboardEditMode = keyboardEditMode,
                            onMenuClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                viewModel.isNoteManagerOpen.value = true
                            },
                            onTitleClick = { viewModel.showRenameDialog.value = true },
                            onLanguageClick = { viewModel.showLanguagePicker.value = true },
                            onLanguageLongClick = { viewModel.cycleFavoriteLanguage() },
                            onShareClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textFieldValue.text)
                                    putExtra(Intent.EXTRA_TITLE, currentNote?.title ?: "Note")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Note via"))
                            },
                            onNewNoteClick = {
                                if (newNoteLocationMode == "ASK_POPUP" || askBeforeCreating) {
                                    editorNewNoteFolder = if (currentNote?.folder?.isNotBlank() == true) currentNote!!.folder else "General"
                                    editorNewNoteTitle = ""
                                    editorShowNewFolderField = false
                                    editorNewFolderNameInput = ""
                                    showNewNoteDialogFromEditor = true
                                } else {
                                    viewModel.createNewNote()
                                }
                            },
                            onSettingsClick = { viewModel.navigateTo("SETTINGS") },
                            onToggleKeyboardEditMode = { viewModel.setKeyboardEditMode(!keyboardEditMode) },
                            onClearNoteClick = { viewModel.clearCurrentNoteText() },
                            onDeleteNoteClick = {
                                currentNote?.let { note ->
                                    viewModel.trashNote(note)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Note moved to trash",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreLastDeletedNote()
                                        }
                                    }
                                }
                            },
                            onHelpClick = {
                                allNotes.find { it.title.contains("Quick Tips") }?.let {
                                    viewModel.loadNote(it)
                                }
                            }
                        )

                        EditorToolBar(
                            isInAppKeyboardActive = builtInKeyboardVisible,
                            dictationState = dictationState,
                            audioSourceType = audioSourceType,
                            isOnDevice = isOnDevice,
                            wordCount = wordCount,
                            charCount = charCount,
                            showWordCount = showWordCount,
                            onZoomOutClick = { viewModel.zoomOut() },
                            onZoomInClick = { viewModel.zoomIn() },
                            onStyleSheetClick = { viewModel.showStyleSheet.value = true },
                            onHighlightClick = { viewModel.showStyleSheet.value = true },
                            onKeyboardToggleClick = {
                                viewModel.setUserWantsBuiltInKeyboard(!builtInKeyboardVisible)
                            },
                            toolbarItemOrder = toolbarItemOrder,
                            hiddenItems = toolbarHiddenItems,
                            statusChipStyle = statusChipStyle,
                            hapticsLevel = hapticsLevel,
                            sessionDurationSec = currentNote?.sessionDurationSec ?: 0L
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isReadingMode && builtInKeyboardVisible) {
                    InAppKeyboard(
                        layout = keyboardLayout,
                        keyHeightDp = keyHeightDp,
                        fontSizeSp = keyboardFontSizeSp,
                        hapticEnabled = keyboardHaptic,
                        holdBehavior = keyboardHoldBehavior,
                        isEditMode = keyboardEditMode,
                        onKeyAction = { key ->
                            viewModel.onKeyboardKey(key)
                        },
                        onKeyEditRequest = { key ->
                            viewModel.editingKey.value = key
                        }
                    )
                }
            }
        ) { innerPadding ->
            var viewportCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(highlightBgColor)
                    .onGloballyPositioned { coords ->
                        viewportCoordinates = coords
                    }
                    .then(
                        if (!isReadingMode) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusRequester.requestFocus()
                                if (!userWantsBuiltIn) {
                                    keyboardController?.show()
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    val activity = context as? Activity
                                    val view = activity?.currentFocus ?: activity?.window?.decorView
                                    if (view != null) {
                                        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                                    }
                                } else {
                                    keyboardController?.hide()
                                }
                            }
                        } else Modifier
                    )
            ) {
                val viewportMaxHeight = maxHeight
                val bottomSpacerHeight = remember(viewportMaxHeight) {
                    (viewportMaxHeight * 0.65f).coerceAtLeast(200.dp)
                }
                val scrollState = rememberScrollState()
                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                var textFieldCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                var isTextFieldFocused by remember { mutableStateOf(false) }

                var savedReadingScrollPosition by remember { mutableStateOf<Int?>(null) }
                var shouldPreserveReadingScroll by remember { mutableStateOf(false) }

                // Continuously track the scroll position while the user is reading in Reading Mode
                LaunchedEffect(isReadingMode, scrollState.value) {
                    if (isReadingMode) {
                        savedReadingScrollPosition = scrollState.value
                        shouldPreserveReadingScroll = true
                    }
                }

                // When switching back from Reading Mode to Normal Editing Mode,
                // restore and preserve the exact scroll position from Reading Mode.
                LaunchedEffect(isReadingMode) {
                    if (!isReadingMode && shouldPreserveReadingScroll && savedReadingScrollPosition != null) {
                        val target = savedReadingScrollPosition!!
                        snapshotFlow { scrollState.maxValue }
                            .filter { it > 0 }
                            .first()
                        scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue))
                    }
                }

                // Clear scroll preservation only when user explicitly focuses the text field to edit
                LaunchedEffect(isTextFieldFocused) {
                    if (isTextFieldFocused) {
                        shouldPreserveReadingScroll = false
                    }
                }

                // Reset scroll preservation on note change
                LaunchedEffect(currentNote?.id) {
                    shouldPreserveReadingScroll = false
                    savedReadingScrollPosition = null
                }

                val fontFam = remember(fontFamilyOption, customFontPath) {
                    getFontFamilyByOption(fontFamilyOption, customFontPath)
                }

                val textAlignValue = when (textAlignment.uppercase()) {
                    "CENTER" -> TextAlign.Center
                    "RIGHT" -> TextAlign.Right
                    "JUSTIFY" -> TextAlign.Justify
                    else -> TextAlign.Start
                }

                // Smooth, continuous auto-scroll while typing:
                // Only scrolls when the text field is actively focused by the user!
                LaunchedEffect(
                    textFieldValue.text,
                    textFieldValue.selection,
                    textLayoutResult,
                    viewportMaxHeight,
                    isImeVisible,
                    builtInKeyboardVisible,
                    isTextFieldFocused
                ) {
                    if (!isTextFieldFocused) return@LaunchedEffect
                    if (isReadingMode) return@LaunchedEffect
                    if (shouldPreserveReadingScroll) return@LaunchedEffect
                    if (scrollState.isScrollInProgress) return@LaunchedEffect
                    val layout = textLayoutResult ?: return@LaunchedEffect
                    val tfCoords = textFieldCoordinates ?: return@LaunchedEffect
                    val vpCoords = viewportCoordinates ?: return@LaunchedEffect
                    if (!tfCoords.isAttached || !vpCoords.isAttached) return@LaunchedEffect

                    val textLen = layout.layoutInput.text.length
                    val cursorOffset = textFieldValue.selection.end.coerceIn(0, textLen)
                    val lineIndex = try {
                        layout.getLineForOffset(cursorOffset)
                    } catch (_: Exception) {
                        0
                    }
                    val lineTop = try { layout.getLineTop(lineIndex) } catch (_: Exception) { 0f }
                    val lineBottom = try { layout.getLineBottom(lineIndex) } catch (_: Exception) { 0f }
                    val cursorLineCenterInTf = (lineTop + lineBottom) / 2f

                    val cursorPositionInVp = try {
                        vpCoords.localPositionOf(tfCoords, Offset(0f, cursorLineCenterInTf))
                    } catch (_: Exception) {
                        return@LaunchedEffect
                    }
                    val currentCursorYOnScreen = cursorPositionInVp.y
                    val viewportHeightPx = vpCoords.size.height.toFloat()
                    if (viewportHeightPx <= 0f) return@LaunchedEffect

                    val targetCenterY = viewportHeightPx / 2f
                    val diff = currentCursorYOnScreen - targetCenterY

                    // Threshold (approx 12px) prevents micro-jitter during horizontal/in-line typing
                    if (abs(diff) > 12f) {
                        val targetScroll = (scrollState.value + diff).roundToInt()
                            .coerceIn(0, scrollState.maxValue)
                        if (targetScroll != scrollState.value) {
                            scrollState.animateScrollTo(
                                value = targetScroll,
                                animationSpec = tween(durationMillis = 150)
                            )
                        }
                    }
                }

                // Editor takes all space
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = marginDp.dp, vertical = 12.dp)
                        .then(
                            if (maxWidthDp > 0) Modifier.widthIn(max = maxWidthDp.dp).align(Alignment.TopCenter)
                            else Modifier
                        )
                ) {
                    if (isReadingMode) {
                        if (markdownSettings.markdownEnabled) {
                            com.example.markdown.MarkdownRenderer(
                                text = textFieldValue.text,
                                settings = markdownSettings,
                                baseTextStyle = TextStyle(
                                    fontFamily = fontFam,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * lineSpacing).sp,
                                    letterSpacing = letterSpacing.sp,
                                    textAlign = textAlignValue,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                onTextChange = { updatedText ->
                                    viewModel.updateTextFieldValue(
                                        TextFieldValue(
                                            text = updatedText,
                                            selection = TextRange(updatedText.length)
                                        ),
                                        isUserTyping = true
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reading_mode_text")
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = textFieldValue.text.ifEmpty { "Empty note" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reading_mode_text"),
                                    style = TextStyle(
                                        fontFamily = fontFam,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * lineSpacing).sp,
                                        letterSpacing = letterSpacing.sp,
                                        textAlign = textAlignValue,
                                        color = if (textFieldValue.text.isEmpty()) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        }
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(bottomSpacerHeight))
                    } else {
                        val defaultKeyboardController = LocalSoftwareKeyboardController.current
                        val customKeyboardController = remember(userWantsBuiltIn, defaultKeyboardController) {
                            object : SoftwareKeyboardController {
                                override fun show() {
                                    if (!userWantsBuiltIn) {
                                        defaultKeyboardController?.show()
                                    }
                                }
                                override fun hide() {
                                    defaultKeyboardController?.hide()
                                }
                            }
                        }

                        // Main Editable Text Field
                        val markdownVisualTransformation = com.example.markdown.rememberMarkdownVisualTransformation(
                            settings = markdownSettings,
                            baseFontSize = fontSize
                        )

                        key(userWantsBuiltIn) {
                            CompositionLocalProvider(
                                LocalSoftwareKeyboardController provides customKeyboardController
                            ) {
                                InterceptPlatformTextInput(
                                    interceptor = { request, nextHandler ->
                                        if (userWantsBuiltIn) {
                                            // Custom keyboard mode: prevent system IME from opening while keeping
                                            // text field editable with a visible, blinking cursor and no popup flickering
                                            awaitCancellation()
                                        } else {
                                            // System keyboard mode: pass through directly to Android platform IME
                                            nextHandler.startInputMethod(request)
                                        }
                                    }
                                ) {
                                    BasicTextField(
                                        value = textFieldValue,
                                        onValueChange = { newValue ->
                                            val processed = if (markdownSettings.markdownEnabled && markdownSettings.autoContinueLists) {
                                                com.example.markdown.MarkdownListAutoContinuer.handleValueChange(
                                                    oldValue = textFieldValue,
                                                    newValue = newValue
                                                )
                                            } else {
                                                newValue
                                            }
                                            viewModel.updateTextFieldValue(processed)
                                        },
                                        visualTransformation = markdownVisualTransformation,
                                        onTextLayout = { result ->
                                            textLayoutResult = result
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 240.dp)
                                            .onGloballyPositioned { coords ->
                                                textFieldCoordinates = coords
                                            }
                                            .onFocusChanged { focusState ->
                                                isTextFieldFocused = focusState.isFocused
                                            }
                                            .focusRequester(focusRequester)
                                            .testTag("editor_text_input"),
                                        textStyle = TextStyle(
                                            fontFamily = fontFam,
                                            fontSize = fontSize.sp,
                                            lineHeight = (fontSize * lineSpacing).sp,
                                            letterSpacing = letterSpacing.sp,
                                            textAlign = textAlignValue,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        cursorBrush = SolidColor(SaffronPrimary),
                                        decorationBox = { innerTextField ->
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (textFieldValue.text.isEmpty() && partialText.isEmpty()) {
                                                    Text(
                                                        text = "Start typing or tap the mic to speak...",
                                                        style = TextStyle(
                                                            fontFamily = fontFam,
                                                            fontSize = fontSize.sp,
                                                            lineHeight = (fontSize * lineSpacing).sp,
                                                            letterSpacing = letterSpacing.sp,
                                                            textAlign = textAlignValue,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Interim Speech Text Display
                        if (partialText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("interim_speech_text")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🎙️ ", fontSize = 13.sp)
                                    Text(
                                        text = partialText,
                                        style = TextStyle(
                                            fontFamily = fontFam,
                                            fontSize = (fontSize - 1).sp,
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Medium,
                                            color = SaffronPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Generous bottom space so that typing at the bottom can scroll up to the vertical middle of the viewport
                        val bottomSpacerHeight = remember(viewportMaxHeight) {
                            (viewportMaxHeight * 0.65f).coerceAtLeast(200.dp)
                        }
                        Spacer(modifier = Modifier.height(bottomSpacerHeight))
                    }
                }

                if (isReadingMode) {
                    // Exit Reading Mode button: horizontal midpoint, slightly above the bottom edge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.setReadingMode(false)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("exit_reading_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Exit Reading Mode",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    val isRecording = dictationState !is DictationState.Idle

                    val isKeyboardOpen = builtInKeyboardVisible || isImeVisible
                    val showFormatBar = markdownSettings.markdownEnabled &&
                            markdownSettings.formatBarEnabled &&
                            isTextFieldFocused &&
                            isKeyboardOpen &&
                            !isRecording

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (showFormatBar) {
                            com.example.markdown.MarkdownFormatBar(
                                enabledButtonIds = markdownSettings.formatBarButtons,
                                onAction = { buttonId ->
                                    val newTfv = com.example.markdown.MarkdownActions.applyActionById(buttonId, textFieldValue)
                                    viewModel.updateTextFieldValue(newTfv, isUserTyping = true)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }

                        // Bottom Floating Cluster
                        EditorFloatingCluster(
                            canUndo = canUndo,
                            canRedo = canRedo,
                            dictationState = dictationState,
                            dictationMode = currentDictationMode,
                            audioLevelRms = audioLevelRms,
                            onUndoClick = { viewModel.undo() },
                            onRedoClick = { viewModel.redo() },
                            onCopyClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("KothaPad Note", textFieldValue.text)
                                clipboard.setPrimaryClip(clip)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Note text copied to clipboard")
                                }
                            },
                            onMicClick = { viewModel.toggleDictation() },
                            onMicLongClick = { },
                            onModeSelect = { mode -> viewModel.setDictationMode(mode) },
                            onPushToTalkPress = { isPressed -> viewModel.onPushToTalkPress(isPressed) },
                            onReadingModeClick = { viewModel.setReadingMode(true) },
                            modifier = Modifier.fillMaxWidth(),
                            micButtonSize = micButtonSize,
                            micButtonShape = micButtonShape,
                            micButtonColor = micButtonColor,
                            micButtonPosition = micButtonPosition,
                            micButtonOffsetY = micButtonOffsetY,
                            dictationAnimationStyle = dictationAnimationStyle,
                            hapticsLevel = hapticsLevel
                        )
                    }
                }
            }
        }

        // FULLSCREEN NOTES MANAGER OVERLAY
        // Animated slide-in from left; swiping right-to-left closes/minimizes it and shows note viewer
        AnimatedVisibility(
            visible = isNoteManagerOpen,
            enter = slideInHorizontally(animationSpec = tween(280)) { -it } + fadeIn(animationSpec = tween(250)),
            exit = slideOutHorizontally(animationSpec = tween(250)) { -it } + fadeOut(animationSpec = tween(200))
        ) {
            NotesManagerScreen(
                activeNotes = activeNotes,
                archivedNotes = archivedNotes,
                trashedNotes = trashedNotes,
                templates = allStamps,
                currentNoteId = currentNote?.id ?: 0L,
                trashAutoEmptyDays = trashAutoEmptyDays,
                defaultFolder = defaultFolder,
                showNoteMetrics = showNoteMetrics,
                noteMetricsStyle = noteMetricsStyle,
                noteMetricsOrder = noteMetricsOrder,
                noteMetricsHiddenItems = noteMetricsHiddenItems,
                expandedFolders = expandedFolders,
                customFolders = customFolders,
                onOpenSettings = {
                    viewModel.openNotesManagerSettings(fromNoteManager = true)
                },
                onToggleFolderExpanded = { folder, exp -> viewModel.toggleFolderExpanded(folder, exp) },
                onSetAllFoldersExpanded = { exp, folders -> viewModel.setAllFoldersExpanded(exp, folders) },
                onAddCustomFolder = { viewModel.addCustomFolder(it) },
                onRenameFolder = { oldF, newF -> viewModel.renameFolder(oldF, newF) },
                onDeleteFolder = { folder, deleteNotes -> viewModel.deleteFolder(folder, deleteNotes) },
                onDuplicateNote = { viewModel.duplicateNote(it) },
                onRenameNote = { note, title -> viewModel.renameNote(note, title) },
                onNoteSelect = { note ->
                    viewModel.loadNote(note)
                    viewModel.isNoteManagerOpen.value = false
                },
                onNewNote = { folder, template, title, rec ->
                    viewModel.createNewNote(folder, template, title, rec)
                },
                askBeforeCreating = askBeforeCreating,
                askChooseTemplateToggle = askChooseTemplateToggle,
                askStartRecordingToggle = askStartRecordingToggle,
                newNoteNamingRule = newNoteNamingRule,
                newNoteLocationMode = newNoteLocationMode,
                availableFolders = allNotes.map { it.folder }.distinct() + customFolders.toList(),
                onPinToggle = { viewModel.togglePinCurrentNote(it) },
                onTrashNote = { viewModel.trashNote(it) },
                onTrashNotes = { viewModel.trashNotes(it) },
                onRestoreNote = { viewModel.restoreNote(it) },
                onPermanentlyDeleteNote = { viewModel.permanentlyDeleteNote(it) },
                onPermanentlyDeleteNotes = { viewModel.permanentlyDeleteNotes(it) },
                onEmptyTrash = { viewModel.emptyTrash() },
                onArchiveToggle = { viewModel.toggleArchiveNote(it) },
                onArchiveNotes = { notes, archive -> viewModel.archiveNotes(notes, archive) },
                onMergeNotes = { notes, title -> viewModel.mergeNotes(notes, title) },
                onMoveNotesToFolder = { notes, folder -> viewModel.moveNotesToFolder(notes, folder) },
                onAddTagsToNotes = { notes, tags -> viewModel.addTagsToNotes(notes, tags) },
                onSaveTemplate = { viewModel.saveTemplate(it) },
                onDeleteTemplate = { viewModel.deleteTemplate(it) },
                onPinTemplate = { viewModel.togglePinTemplate(it) },
                onReorderTemplates = { viewModel.reorderTemplates(it) },
                onUpdateTrashDays = { viewModel.setTrashAutoEmptyDaysPref(it) },
                onClose = { viewModel.isNoteManagerOpen.value = false }
            )
        }
    }

    // Dialogs & Sheets
    if (showNewNoteDialogFromEditor) {
        AlertDialog(
            onDismissRequest = { showNewNoteDialogFromEditor = false },
            title = { Text("Create New Note") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editorNewNoteTitle,
                        onValueChange = { editorNewNoteTitle = it },
                        label = { Text("Note Title (Optional)") },
                        placeholder = { Text("Enter title or leave blank") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Destination Folder", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { editorShowFolderDropdown = !editorShowFolderDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = editorNewNoteFolder)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = editorShowFolderDropdown,
                            onDismissRequest = { editorShowFolderDropdown = false }
                        ) {
                            val folders = (listOf("General") + customFolders.toList() + allNotes.map { it.folder }.filter { it.isNotBlank() }).distinct()
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder) },
                                    onClick = {
                                        editorNewNoteFolder = folder
                                        editorShowFolderDropdown = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                text = { Text("Create new folder...") },
                                onClick = {
                                    editorShowNewFolderField = true
                                    editorShowFolderDropdown = false
                                }
                            )
                        }

                        if (editorShowNewFolderField) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editorNewFolderNameInput,
                                    onValueChange = { editorNewFolderNameInput = it },
                                    placeholder = { Text("Folder name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val trimmed = editorNewFolderNameInput.trim()
                                        if (trimmed.isNotBlank()) {
                                            viewModel.addCustomFolder(trimmed)
                                            editorNewNoteFolder = trimmed
                                            editorNewFolderNameInput = ""
                                            editorShowNewFolderField = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (editorNewNoteTitle.isBlank()) null else editorNewNoteTitle
                        viewModel.createNewNote(
                            folder = editorNewNoteFolder,
                            customTitle = title
                        )
                        showNewNoteDialogFromEditor = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoteDialogFromEditor = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameDialog) {
        RenameNoteDialog(
            initialTitle = currentNote?.title ?: "",
            onConfirm = { newTitle -> viewModel.renameCurrentNote(newTitle) },
            onDismiss = { viewModel.showRenameDialog.value = false }
        )
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguageCode = activeLanguage,
            favoriteLanguages = favoriteLanguages,
            onSelectLanguage = { code -> viewModel.setNoteLanguage(code) },
            onToggleFavorite = { code -> viewModel.toggleFavoriteLanguage(code) },
            onDismiss = { viewModel.showLanguagePicker.value = false }
        )
    }

    if (showStyleSheet) {
        StyleSheetBottomSheet(
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            fontFamilyOption = fontFamilyOption,
            currentHighlightHex = currentNote?.highlightColor,
            onFontSizeChange = { viewModel.setFontSize(it) },
            onLineSpacingChange = { viewModel.setLineSpacing(it) },
            onFontFamilyChange = { viewModel.setFontFamily(it) },
            onHighlightChange = { viewModel.setNoteHighlight(it) },
            onDismiss = { viewModel.showStyleSheet.value = false }
        )
    }

    if (showTemplatesSheet) {
        TemplatesBottomSheet(
            templates = allStamps,
            onSelectTemplate = { template ->
                viewModel.insertTemplate(template)
            },
            onSaveTemplate = { updated ->
                viewModel.saveTemplate(updated)
            },
            onDeleteTemplate = { deleted ->
                viewModel.deleteTemplate(deleted)
            },
            onPinTemplate = { template ->
                viewModel.togglePinTemplate(template)
            },
            onReorderTemplates = { reordered ->
                viewModel.reorderTemplates(reordered)
            },
            onDismiss = { viewModel.showTemplatesSheet.value = false }
        )
    }

    editingKey?.let { keyToEdit ->
        EditKeyDialog(
            key = keyToEdit,
            onSaveKey = { updated ->
                viewModel.updateKeyInLayout(updated)
                viewModel.editingKey.value = null
            },
            onResetKey = {
                viewModel.resetKeyToDefault(keyToEdit)
                viewModel.editingKey.value = null
            },
            onDismiss = { viewModel.editingKey.value = null }
        )
    }

    if (!onboardingCompleted) {
        OnboardingDialog(
            selectedLanguage = defaultLanguage,
            selectedTheme = viewModel.appTheme.collectAsState().value,
            onLanguageChange = { viewModel.setDefaultLanguagePref(it) },
            onThemeChange = { viewModel.setAppThemePref(it) },
            onCompleteOnboarding = { viewModel.completeOnboarding() }
        )
    }
}
