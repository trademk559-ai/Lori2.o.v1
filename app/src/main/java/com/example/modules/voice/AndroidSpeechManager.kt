package com.example.modules.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * AndroidSpeechManager:
 * Manages the continuous audio input lifecycle, end-of-speech & silence detection,
 * handling of pauses during long user utterances, and full bidirectional synchronization
 * with the Text-to-Speech engine to ensure a seamless hands-free loop:
 *
 *   [LISTENING] -> [USER_SPEAKING] -> [PROCESSING] -> [THINKING] -> [SPEAKING] -> (Stabilize) -> [LISTENING]
 */
class AndroidSpeechManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AndroidSpeechManager"
        private const val DEFAULT_SILENCE_WINDOW_MS = 2500L
        private const val POST_TTS_STABILIZATION_MS = 450L
        private const val PAUSE_DEBOUNCE_MS = 1200L

        @Volatile
        private var instance: AndroidSpeechManager? = null

        fun getInstance(context: Context): AndroidSpeechManager {
            return instance ?: synchronized(this) {
                instance ?: AndroidSpeechManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    // State Observables
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    // Configuration & Callbacks
    private var activeLanguageCode = "hi-IN"
    private var isPaused = false
    private var onFinalSpeechResult: ((String) -> Unit)? = null
    private var onSpeechError: ((String) -> Unit)? = null

    // Wake Word Integration & Dynamic Acoustic Sensitivity
    private var isWakeWordActive = true
    private var wakeWordSensitivity = 0.75f
    val currentSensitivity: Float get() = wakeWordSensitivity
    val isWakeWordFilterActive: Boolean get() = isWakeWordActive

    // Long Utterance & Pause Accumulation Buffer
    private val accumulatedSpeech = StringBuilder()
    private var speechPauseDebounceJob: Job? = null

    // Audio Focus Request (Android O+)
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        initializeTextToSpeech()
    }

    // =========================================================================
    // 1. Text-To-Speech Initialization & Lifecycle
    // =========================================================================

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val hiLocale = Locale("hi", "IN")
                val langResult = textToSpeech?.setLanguage(hiLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.language = Locale("en", "IN")
                }
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.05f)

                setupTtsProgressListener()
                Log.d(TAG, "TextToSpeech successfully initialized.")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            }
        }
    }

    private fun setupTtsProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    _voiceState.value = VoiceState.SPEAKING
                    _statusMessage.value = "Lori bol rahi hai..."
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    abandonAudioFocus()
                    _voiceState.value = VoiceState.IDLE
                    _statusMessage.value = "Ready"

                    // PREVENT SELF-LISTENING:
                    // After TTS completes, allow a brief stabilization buffer before re-enabling mic
                    if (_isContinuousMode.value && !isPaused && onFinalSpeechResult != null) {
                        mainHandler.postDelayed({
                            if (_isContinuousMode.value && !isPaused && _voiceState.value != VoiceState.SPEAKING) {
                                startListeningInternal(activeLanguageCode)
                            }
                        }, POST_TTS_STABILIZATION_MS)
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    abandonAudioFocus()
                    _voiceState.value = VoiceState.IDLE
                    _statusMessage.value = "Speech output error"

                    if (_isContinuousMode.value && !isPaused) {
                        mainHandler.postDelayed({
                            startListeningInternal(activeLanguageCode)
                        }, POST_TTS_STABILIZATION_MS)
                    }
                }
            }
        })
    }

    // =========================================================================
    // 2. Speech Recognition & Continuous Lifecycle
    // =========================================================================

    /**
     * Start speech input with explicit callbacks.
     */
    fun startListening(
        languageCode: String = "hi-IN",
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        isPaused = false
        activeLanguageCode = languageCode
        onFinalSpeechResult = onResult
        onSpeechError = onError
        startListeningInternal(languageCode)
    }

    private fun startListeningInternal(languageCode: String) {
        // Prevent Self-Listening: Never capture audio if TTS is speaking
        if (textToSpeech?.isSpeaking == true) {
            Log.w(TAG, "Speech recognition ignored because TTS is currently speaking.")
            return
        }

        // Check Permissions
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            val errMsg = "Microphone permission required! Please grant permission in Settings."
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = errMsg
            onSpeechError?.invoke(errMsg)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val errMsg = "Speech recognition is not available on this device."
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = errMsg
            onSpeechError?.invoke(errMsg)
            return
        }

        mainHandler.post {
            try {
                // Safely reset any previous recognizer instance
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                    // Dynamic Silence Thresholds based on sensitivity:
                    // High sensitivity (0.9) -> snappier 1800ms silence detection
                    // Conservative sensitivity (0.2) -> patient 3600ms silence window
                    val dynamicSilenceMs = (1600L + ((1.0f - wakeWordSensitivity) * 2200L)).toLong()
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, dynamicSilenceMs)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, dynamicSilenceMs)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, dynamicSilenceMs)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "hi-Latn", "en-US"))
                }

                requestAudioFocus()
                speechRecognizer?.startListening(intent)
                _voiceState.value = VoiceState.LISTENING
                _statusMessage.value = when {
                    !isWakeWordActive -> "Lori sun rahi hai... (Direct Speech / No Wake Word)"
                    _isContinuousMode.value -> "Lori sun rahi hai... (Wake Word: 'Lori' Active)"
                    else -> "Lori sun rahi hai... ('Lori' boliye)"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _voiceState.value = VoiceState.ERROR
                _statusMessage.value = "Mic start error: ${e.localizedMessage}"
                onSpeechError?.invoke(e.localizedMessage ?: "Unknown speech recognizer error")
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
                _liveTranscript.value = ""
                _rmsDb.value = 0f
            }

            override fun onBeginningOfSpeech() {
                speechPauseDebounceJob?.cancel()
                _voiceState.value = VoiceState.USER_SPEAKING
                _statusMessage.value = "Listening to your message..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                val scaledRms = (rmsdB * (0.6f + (wakeWordSensitivity * 0.7f))).coerceIn(0f, 10f)
                _rmsDb.value = scaledRms
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.PROCESSING
                _statusMessage.value = "Processing complete message..."
            }

            override fun onError(error: Int) {
                _rmsDb.value = 0f
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission missing."
                    SpeechRecognizer.ERROR_NETWORK -> "Internet connection required for speech recognition."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timeout."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout."
                    else -> "Speech recognition error ($error)"
                }

                // In continuous mode, gracefully recover from benign timeouts or silence
                if (_isContinuousMode.value && !isPaused &&
                    (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH)
                ) {
                    _voiceState.value = VoiceState.LISTENING
                    _statusMessage.value = "Lori sun rahi hai... (Hands-Free Active)"
                    mainHandler.postDelayed({
                        if (_isContinuousMode.value && !isPaused && _voiceState.value != VoiceState.SPEAKING) {
                            startListeningInternal(activeLanguageCode)
                        }
                    }, 300L)
                } else {
                    _voiceState.value = if (_isContinuousMode.value) VoiceState.LISTENING else VoiceState.IDLE
                    _statusMessage.value = message
                    onSpeechError?.invoke(message)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val fullResult = matches?.firstOrNull()?.trim() ?: ""

                val finalSpoken = if (fullResult.isNotBlank()) {
                    fullResult
                } else {
                    accumulatedSpeech.toString().trim()
                }

                accumulatedSpeech.clear()
                _liveTranscript.value = finalSpoken

                if (finalSpoken.isNotBlank()) {
                    _voiceState.value = VoiceState.PROCESSING
                    _statusMessage.value = "Processing complete utterance..."
                    onFinalSpeechResult?.invoke(finalSpoken)
                } else {
                    if (_isContinuousMode.value && !isPaused) {
                        startListeningInternal(activeLanguageCode)
                    } else {
                        _voiceState.value = VoiceState.IDLE
                        _statusMessage.value = "Ready"
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = partials?.firstOrNull()?.trim() ?: ""
                if (text.isNotEmpty()) {
                    _liveTranscript.value = text
                    _voiceState.value = VoiceState.USER_SPEAKING

                    // Debounce pause handler for long utterances
                    speechPauseDebounceJob?.cancel()
                    speechPauseDebounceJob = scope.launch {
                        delay(PAUSE_DEBOUNCE_MS)
                        // User paused slightly; preserve transcript in buffer
                        accumulatedSpeech.clear()
                        accumulatedSpeech.append(text)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    // =========================================================================
    // 3. Continuous Mode Controls (ON / OFF / Pause / Resume / Stop)
    // =========================================================================

    /**
     * Integrates with AndroidSpeechManager lifecycle:
     * Dynamically updates wake word detection toggle and sensitivity threshold.
     * Recalculates acoustic parameters and seamlessly re-configures active listening session if running.
     */
    fun updateWakeWordConfig(enabled: Boolean, sensitivity: Float) {
        val clampedSensitivity = sensitivity.coerceIn(0.1f, 1.0f)
        val enabledChanged = isWakeWordActive != enabled
        val sensitivityChanged = kotlin.math.abs(wakeWordSensitivity - clampedSensitivity) > 0.02f

        isWakeWordActive = enabled
        wakeWordSensitivity = clampedSensitivity

        Log.d(TAG, "WakeWord Config: enabled=$enabled, sensitivity=$clampedSensitivity")

        if ((enabledChanged || sensitivityChanged) &&
            _voiceState.value == VoiceState.LISTENING &&
            !isPaused &&
            textToSpeech?.isSpeaking != true
        ) {
            mainHandler.post {
                if (_voiceState.value == VoiceState.LISTENING && !isPaused && textToSpeech?.isSpeaking != true) {
                    stopListening()
                    startListeningInternal(activeLanguageCode)
                }
            }
        }
    }

    fun getComputedSilenceWindowMs(): Long {
        return (1600L + ((1.0f - wakeWordSensitivity) * 2200L)).toLong()
    }

    fun setContinuousMode(enabled: Boolean) {
        _isContinuousMode.value = enabled
        if (!enabled && _voiceState.value == VoiceState.LISTENING) {
            stopListening()
        }
    }

    fun pauseContinuousMode() {
        isPaused = true
        stopListening()
        _voiceState.value = VoiceState.PAUSED
        _statusMessage.value = "Hands-Free Voice Mode Paused."
    }

    fun resumeContinuousMode() {
        isPaused = false
        if (onFinalSpeechResult != null) {
            startListeningInternal(activeLanguageCode)
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechPauseDebounceJob?.cancel()
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                _rmsDb.value = 0f
                if (_voiceState.value != VoiceState.SPEAKING && _voiceState.value != VoiceState.PAUSED) {
                    _voiceState.value = if (_isContinuousMode.value) VoiceState.STOPPED else VoiceState.IDLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognizer", e)
            }
        }
    }

    // =========================================================================
    // 4. Text-To-Speech Synthesis & Loop Handlers
    // =========================================================================

    /**
     * Speaks the assistant's answer while strictly preventing mic loopback.
     */
    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.05f
    ) {
        if (!isTtsReady || text.isBlank()) return

        // 1. Immediately halt listening before audio output
        stopListening()

        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(pitch)

        // Clean formatting artifacts for pristine audio
        val cleanSpeechText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("`"), "")
            .trim()

        requestAudioFocus()
        val utteranceId = "lori_utterance_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        _voiceState.value = VoiceState.SPEAKING
        _statusMessage.value = "Lori bol rahi hai..."
        textToSpeech?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            abandonAudioFocus()
            _voiceState.value = VoiceState.IDLE
            _statusMessage.value = "Ready"
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun setVoiceState(state: VoiceState, message: String = "") {
        _voiceState.value = state
        if (message.isNotBlank()) {
            _statusMessage.value = message
        }
    }

    // =========================================================================
    // 5. Audio Focus Management
    // =========================================================================

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* handled inline */ }
                    .build()
            }
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    // =========================================================================
    // 6. Cleanup & Destruction
    // =========================================================================

    fun destroy() {
        try {
            _isContinuousMode.value = false
            speechPauseDebounceJob?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsReady = false
            abandonAudioFocus()
            instance = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during destruction", e)
        }
    }
}
