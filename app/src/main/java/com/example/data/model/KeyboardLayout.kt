package com.example.data.model

enum class KeyActionType {
    INSERT,
    CURSOR_LEFT,
    CURSOR_RIGHT,
    CURSOR_UP,
    CURSOR_DOWN,
    WORD_LEFT,
    WORD_RIGHT,
    LINE_START,
    LINE_END,
    BACKSPACE,
    DELETE_WORD,
    ENTER,
    NEW_PARAGRAPH,
    SPACE,
    OPEN_TEMPLATES,
    CHANGE_LANGUAGE,
    UNDO,
    REDO,
    SELECT_ALL,
    PASTE,
    TOGGLE_SYSTEM_KEYBOARD,
    SWITCH_LAYOUT,
    DISMISS_KEYBOARD
}

data class KeyboardKey(
    val id: String = "",
    val label: String,
    val actionType: KeyActionType = KeyActionType.INSERT,
    val outputText: String? = null,
    val smartSpacing: Boolean = false,
    val dateFormat: String? = null,
    val timeFormat: String? = null,
    val targetLayout: String? = null,
    val weight: Float = 1f,
    val alternates: List<String> = emptyList()
)

data class KeyboardRow(
    val keys: List<KeyboardKey>
)

data class KeyboardLayout(
    val id: String,
    val name: String,
    val languageCode: String,
    val rows: List<KeyboardRow>
)
