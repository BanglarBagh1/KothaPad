package com.example.engine.dictation

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.model.DictationMode
import com.example.data.model.DictationState
import com.example.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SpeechSupervisor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val scope: CoroutineScope
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // State flows
    private val _state = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> = _state.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalCommittedText = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val finalCommittedText: SharedFlow<String> = _finalCommittedText.asSharedFlow()

    private val _audioLevelRms = MutableStateFlow(0f)
    val audioLevelRms: StateFlow<Float> = _audioLevelRms.asStateFlow()

    private val _isOnDevice = MutableStateFlow(false)
    val isOnDevice: StateFlow<Boolean> = _isOnDevice.asStateFlow()

    // Config options
    private var activeLanguage: String = "bn-BD"
    private var activeMode: DictationMode = DictationMode.CONTINUOUS
    private var offlinePreferred: Boolean = false
    private var muteBeepMethod: String = "MEDIA_SYSTEM" // "MEDIA_SYSTEM", "VIBRATE_ONCE", "OFF"
    private var pauseToleranceSeconds: Int = 30
    private var idleAutoStopMinutes: Int = 0
    private var timedMinutes: Int = 3

    // Engine instance
    private var speechRecognizer: SpeechRecognizer? = null
    private var isSessionActive = false
    private var isManuallyPaused = false
    private var consecutiveErrors = 0
    private var lastSpeechTimestamp = System.currentTimeMillis()
    private var lastSessionEndTime = 0L
    private var lastPartialCommitted = false
    private var lastCommittedText = ""
    private var consecutiveBusyClientErrors = 0

    // Stream mute state for beep suppression (Muted ONCE per dictation session, never per restart)
    private var didMuteMusic = false
    private var didMuteSystem = false
    private var savedMusicVol = -1
    private var savedSystemVol = -1
    private var didChangeRingerMode = false
    private var savedRingerMode = -1

    // Timers & watchdogs
    private var idleCheckJob: Job? = null
    private var timedModeJob: Job? = null
    private var restartRunnable: Runnable? = null
    private var startTimeoutRunnable: Runnable? = null

    val audioSourceDetector = AudioSourceDetector(context)

    fun startDictation(
        language: String,
        mode: DictationMode,
        offline: Boolean,
        muteBeepMethod: String,
        pauseToleranceSec: Int,
        idleStopMin: Int,
        timedDurationMin: Int
    ) {
        activeLanguage = language
        activeMode = mode
        offlinePreferred = offline
        this.muteBeepMethod = muteBeepMethod
        pauseToleranceSeconds = pauseToleranceSec
        idleAutoStopMinutes = idleStopMin
        timedMinutes = timedDurationMin

        isSessionActive = true
        isManuallyPaused = false
        consecutiveErrors = 0
        consecutiveBusyClientErrors = 0
        lastSpeechTimestamp = System.currentTimeMillis()
        lastSessionEndTime = 0L
        lastPartialCommitted = false
        lastCommittedText = ""

        // 5. Logcat line at session start
        logAudioStatus("Session START")

        // 2. Mute ONCE per dictation session, never per restart
        muteSessionAudioOnce()

        mainHandler.post {
            initAndStartRecognizer()
        }

        startWatchdogTimers()
    }

    private fun muteSessionAudioOnce() {
        when (muteBeepMethod.uppercase()) {
            "MEDIA_SYSTEM" -> {
                try {
                    savedMusicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    savedSystemVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
                } catch (_: Exception) {}

                // 3. Persist record before muting
                scope.launch(Dispatchers.IO) {
                    preferencesManager.saveMutedRecord(savedMusicVol, savedSystemVol, -1)
                }

                try {
                    // flags = 0
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    didMuteMusic = true
                } catch (e: SecurityException) {
                    Log.w("KothaPadAudio", "SecurityException muting STREAM_MUSIC", e)
                }

                try {
                    // flags = 0
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                    didMuteSystem = true
                } catch (e: SecurityException) {
                    Log.w("KothaPadAudio", "SecurityException muting STREAM_SYSTEM", e)
                }
            }
            "VIBRATE_ONCE" -> {
                try {
                    savedRingerMode = audioManager.ringerMode
                } catch (_: Exception) {}

                scope.launch(Dispatchers.IO) {
                    preferencesManager.saveMutedRecord(-1, -1, savedRingerMode)
                }

                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    didChangeRingerMode = true
                } catch (e: SecurityException) {
                    Log.w("KothaPadAudio", "SecurityException setting RINGER_MODE_VIBRATE", e)
                }
            }
            "OFF" -> {
                // Do not mute anything
            }
        }
    }

    private fun restoreSessionAudioOnce() {
        // Restore ONLY the streams this app muted, once, after ~400 ms delay
        mainHandler.postDelayed({
            if (didMuteMusic) {
                try {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    if (savedMusicVol >= 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusicVol, 0)
                    }
                } catch (e: SecurityException) {
                    Log.w("KothaPadAudio", "Error unmuting STREAM_MUSIC", e)
                }
                didMuteMusic = false
                savedMusicVol = -1
            }

            if (didMuteSystem) {
                try {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                    if (savedSystemVol >= 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystemVol, 0)
                    }
                } catch (e: SecurityException) {
                    Log.w("KothaPadAudio", "Error unmuting STREAM_SYSTEM", e)
                }
                didMuteSystem = false
                savedSystemVol = -1
            }

            if (didChangeRingerMode) {
                try {
                    if (savedRingerMode >= 0) {
                        audioManager.ringerMode = savedRingerMode
                    }
                } catch (e: SecurityException) {
                    Log.w("KothaPadAudio", "Error restoring ringerMode", e)
                }
                didChangeRingerMode = false
                savedRingerMode = -1
            }

            scope.launch(Dispatchers.IO) {
                preferencesManager.clearMutedRecord()
            }

            // 5. Logcat line at session stop
            logAudioStatus("Session STOP")
        }, 400L)
    }

    private fun logAudioStatus(tag: String) {
        try {
            val ringerMode = audioManager.ringerMode
            val musicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val systemVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            val ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            val notifVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            Log.d(
                "KothaPadAudio",
                "$tag - ringerMode=$ringerMode, MUSIC=$musicVol, SYSTEM=$systemVol, RING=$ringVol, NOTIFICATION=$notifVol"
            )
        } catch (e: Exception) {
            Log.w("KothaPadAudio", "Error reading stream status for log", e)
        }
    }

    // 3. Crash safety restoration
    fun restoreCrashSafetyMute() {
        scope.launch(Dispatchers.IO) {
            try {
                val isMuted = preferencesManager.mutedByApp.first()
                if (isMuted) {
                    val savedMusic = preferencesManager.mutedMusicVol.first()
                    val savedSystem = preferencesManager.mutedSystemVol.first()
                    val savedRinger = preferencesManager.savedRingerMode.first()

                    if (savedMusic >= 0) {
                        try {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusic, 0)
                        } catch (_: Exception) {}
                    }
                    if (savedSystem >= 0) {
                        try {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystem, 0)
                        } catch (_: Exception) {}
                    }
                    if (savedRinger >= 0) {
                        try {
                            audioManager.ringerMode = savedRinger
                        } catch (_: Exception) {}
                    }

                    preferencesManager.clearMutedRecord()
                    Log.d("KothaPadAudio", "Crash safety mute restored successfully")
                }
            } catch (e: Exception) {
                Log.w("KothaPadAudio", "Error restoring crash safety mute", e)
            }
        }
    }

    fun pauseDictation() {
        isManuallyPaused = true
        _state.value = DictationState.Paused
        mainHandler.post {
            cancelStartTimeout()
            cancelPendingRestart()
            stopListeningInternal()
        }
    }

    fun resumeDictation() {
        if (!isSessionActive) {
            startDictation(
                language = activeLanguage,
                mode = activeMode,
                offline = offlinePreferred,
                muteBeepMethod = muteBeepMethod,
                pauseToleranceSec = pauseToleranceSeconds,
                idleStopMin = idleAutoStopMinutes,
                timedDurationMin = timedMinutes
            )
            return
        }
        isManuallyPaused = false
        lastSpeechTimestamp = System.currentTimeMillis()
        mainHandler.post {
            initAndStartRecognizer()
        }
    }

    fun stopDictation() {
        isSessionActive = false
        isManuallyPaused = false
        idleCheckJob?.cancel()
        timedModeJob?.cancel()
        cancelPendingRestart()
        cancelStartTimeout()

        mainHandler.post {
            commitPendingPartial()
            stopListeningInternal()
            destroyRecognizer()
            restoreSessionAudioOnce()
            _state.value = DictationState.Idle
            _partialText.value = ""
            _audioLevelRms.value = 0f
        }
    }

    private fun initAndStartRecognizer() {
        if (!isSessionActive || isManuallyPaused) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = DictationState.Error(
                "Speech recognition not available on device. Ensure Google Speech Services are enabled.",
                isRecoverable = false
            )
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = createRecognizerInstance()
        }

        _state.value = DictationState.Starting

        // Watchdog: If recognizer hangs in starting for > 3.5s, clean and restart
        // Cancelled as soon as onReadyForSpeech, onBeginningOfSpeech, onRmsChanged or onPartialResults is called!
        cancelStartTimeout()
        startTimeoutRunnable = Runnable {
            if (_state.value is DictationState.Starting && isSessionActive && !isManuallyPaused) {
                Log.d("KothaPadSpeech", "Watchdog triggered after 3.5s in Starting state. Recreating recognizer.")
                destroyRecognizer()
                scheduleRestart(immediate = true)
            }
        }
        mainHandler.postDelayed(startTimeoutRunnable!!, 3500L)

        // Calculate silence length in millis from pauseToleranceSeconds (cap 30000ms; use 30000 for <= 0 "Infinite")
        val silenceLengthMs = if (pauseToleranceSeconds <= 0 || pauseToleranceSeconds >= 30) 30000L else pauseToleranceSeconds * 1000L

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, activeLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceLengthMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceLengthMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, silenceLengthMs)

            if (offlinePreferred) {
                // Works on API < 33 as well as 33+
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            cancelStartTimeout()
            handleRecognizerFailure(e.message ?: "Failed to start listening")
        }
    }

    private fun createRecognizerInstance(): SpeechRecognizer {
        return if (offlinePreferred && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                _isOnDevice.value = true
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                _isOnDevice.value = false
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } else {
            _isOnDevice.value = false
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                cancelStartTimeout()
                if (isSessionActive && !isManuallyPaused) {
                    _state.value = DictationState.Listening
                    consecutiveErrors = 0
                    consecutiveBusyClientErrors = 0
                }
            }

            override fun onBeginningOfSpeech() {
                cancelStartTimeout()
                lastSpeechTimestamp = System.currentTimeMillis()
                if (isSessionActive && !isManuallyPaused) {
                    _state.value = DictationState.Listening
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (isSessionActive && !isManuallyPaused) {
                    cancelStartTimeout()
                    if (_state.value is DictationState.Starting) {
                        _state.value = DictationState.Listening
                    }
                    if (rmsdB > 1.0f) {
                        lastSpeechTimestamp = System.currentTimeMillis()
                    }
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioLevelRms.value = normalized
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _audioLevelRms.value = 0f
                // Do NOT restart here! The engine will deliver onResults or onError shortly.
                // Restarting here causes premature cancellations and lost words.
            }

            override fun onError(error: Int) {
                cancelStartTimeout()
                _audioLevelRms.value = 0f
                lastSessionEndTime = System.currentTimeMillis()

                if (!isSessionActive || isManuallyPaused) return

                Log.d("KothaPadSpeech", "onError code=$error")

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        commitPendingPartial()
                        consecutiveBusyClientErrors = 0
                        // Immediate restart via handler.post with no artificial delay
                        if (isSessionActive && !isManuallyPaused) {
                            mainHandler.post {
                                scheduleRestart(immediate = true)
                            }
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        commitPendingPartial()
                        consecutiveBusyClientErrors++
                        consecutiveErrors++
                        if (consecutiveBusyClientErrors > 3) {
                            val errorMsg = getErrorMessage(error)
                            _state.value = DictationState.Error(errorMsg, isRecoverable = true)
                            consecutiveBusyClientErrors = 0
                        } else {
                            try {
                                speechRecognizer?.cancel()
                            } catch (_: Exception) {}
                            if (consecutiveErrors >= 2) {
                                destroyRecognizer()
                                speechRecognizer = createRecognizerInstance()
                                consecutiveErrors = 0
                            }
                            scheduleRestart(immediate = false, delayMs = 150)
                        }
                    }
                    else -> {
                        commitPendingPartial()
                        consecutiveErrors++
                        if (consecutiveErrors >= 2) {
                            destroyRecognizer()
                            speechRecognizer = createRecognizerInstance()
                            consecutiveErrors = 0
                        }
                        val errorMsg = getErrorMessage(error)
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            _state.value = DictationState.Error(errorMsg, isRecoverable = false)
                            isSessionActive = false
                        } else {
                            _state.value = DictationState.Error(errorMsg, isRecoverable = true)
                            scheduleRestart(immediate = false, delayMs = 500)
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                cancelStartTimeout()
                _audioLevelRms.value = 0f
                lastSessionEndTime = System.currentTimeMillis()
                consecutiveErrors = 0
                consecutiveBusyClientErrors = 0

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalPhrase = matches?.firstOrNull()?.trim()

                if (!finalPhrase.isNullOrEmpty()) {
                    commitTextWithContinuationCheck(finalPhrase)
                } else {
                    commitPendingPartial()
                }

                if (activeMode == DictationMode.SINGLE_SHOT) {
                    stopDictation()
                } else if (isSessionActive && !isManuallyPaused) {
                    // Commit text first, then startListening via handler.post with no artificial delay
                    mainHandler.post {
                        scheduleRestart(immediate = true)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                cancelStartTimeout()
                lastSpeechTimestamp = System.currentTimeMillis()
                if (isSessionActive && !isManuallyPaused) {
                    _state.value = DictationState.Listening
                }
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialPhrase = matches?.firstOrNull()?.trim()
                if (!partialPhrase.isNullOrEmpty()) {
                    _partialText.value = partialPhrase
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun commitTextWithContinuationCheck(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isEmpty()) return

        // If the session ended while speaking mid-sentence (restart within 1500ms of last speech)
        // and we committed an earlier partial or the previous text had no sentence terminator,
        // format seamlessly with a single space and preserve capitalization.
        var textToCommit = trimmed

        // De-duplicate against the last partial if it was committed on error/interruption
        if (lastPartialCommitted && lastCommittedText.isNotEmpty()) {
            if (textToCommit.equals(lastCommittedText, ignoreCase = true)) {
                // Already fully committed from partial
                lastPartialCommitted = false
                return
            } else if (textToCommit.startsWith(lastCommittedText, ignoreCase = true)) {
                // It contains the prefix we already committed; strip it
                textToCommit = textToCommit.substring(lastCommittedText.length).trimStart()
            }
        }

        lastPartialCommitted = false
        lastSpeechTimestamp = System.currentTimeMillis()
        _partialText.value = ""
        lastCommittedText = textToCommit
        if (textToCommit.isNotEmpty()) {
            _finalCommittedText.tryEmit(textToCommit)
        }
    }

    private fun commitPendingPartial() {
        val pending = _partialText.value.trim()
        if (pending.isNotEmpty()) {
            _partialText.value = ""
            lastPartialCommitted = true
            lastCommittedText = pending
            lastSpeechTimestamp = System.currentTimeMillis()
            _finalCommittedText.tryEmit(pending)
        }
    }

    private fun scheduleRestart(immediate: Boolean, delayMs: Long = 0) {
        if (!isSessionActive || isManuallyPaused) return

        val pauseDurationMs = System.currentTimeMillis() - lastSpeechTimestamp
        if (pauseToleranceSeconds > 0 && pauseDurationMs > pauseToleranceSeconds * 1000L) {
            _state.value = DictationState.Paused
            isManuallyPaused = true
            return
        }

        cancelPendingRestart()
        _state.value = DictationState.Restarting

        restartRunnable = Runnable {
            if (isSessionActive && !isManuallyPaused) {
                initAndStartRecognizer()
            }
        }

        val actualDelay = if (immediate) 0L else delayMs
        if (actualDelay == 0L) {
            mainHandler.post(restartRunnable!!)
        } else {
            mainHandler.postDelayed(restartRunnable!!, actualDelay)
        }
    }

    private fun cancelPendingRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun cancelStartTimeout() {
        startTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        startTimeoutRunnable = null
    }

    private fun startWatchdogTimers() {
        idleCheckJob?.cancel()
        timedModeJob?.cancel()

        if (idleAutoStopMinutes > 0) {
            idleCheckJob = scope.launch(Dispatchers.Default) {
                while (isSessionActive) {
                    delay(5000)
                    val idleMs = System.currentTimeMillis() - lastSpeechTimestamp
                    if (idleMs >= idleAutoStopMinutes * 60 * 1000L) {
                        mainHandler.post { stopDictation() }
                        break
                    }
                }
            }
        }

        if (activeMode == DictationMode.TIMED && timedMinutes > 0) {
            timedModeJob = scope.launch(Dispatchers.Default) {
                delay(timedMinutes * 60 * 1000L)
                if (isSessionActive) {
                    mainHandler.post { stopDictation() }
                }
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun handleRecognizerFailure(msg: String) {
        _state.value = DictationState.Error(msg, isRecoverable = true)
        scheduleRestart(immediate = false, delayMs = 500)
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client connection issue"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network issue"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Speech recognition paused"
        }
    }

    fun release() {
        stopDictation()
        audioSourceDetector.release()
    }
}
