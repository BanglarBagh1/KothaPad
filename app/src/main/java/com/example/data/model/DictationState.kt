package com.example.data.model

sealed interface DictationState {
    object Idle : DictationState
    object Starting : DictationState
    object Listening : DictationState
    object Restarting : DictationState
    object Paused : DictationState
    data class Error(val message: String, val isRecoverable: Boolean = true) : DictationState
}

enum class DictationMode(val displayName: String, val description: String) {
    CONTINUOUS("Continuous", "Keeps listening through pauses without stopping"),
    TOGGLE("Toggle", "Tap mic to start, tap again to stop"),
    PUSH_TO_TALK("Push-to-talk", "Hold down mic to speak, release to pause"),
    SINGLE_SHOT("Single-shot", "Transcribes one sentence or phrase, then stops"),
    TIMED("Timed", "Stops automatically after selected duration")
}

enum class InsertionMode(val displayName: String) {
    AT_CURSOR("At Cursor"),
    APPEND_END("Append at End"),
    REPLACE_SELECTION("Replace Selection")
}

enum class AudioSourceType(val displayName: String, val icon: String) {
    PHONE_MIC("Phone Mic", "🎙️"),
    BLUETOOTH_SCO("Bluetooth Headset", "🎧"),
    WIRED_HEADSET("Wired Headset", "🔌"),
    UNKNOWN("Microphone", "🎙️")
}
