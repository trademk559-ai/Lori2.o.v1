package com.example.modules.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceState {
    VOICE_MODE_OFF,
    IDLE,
    LISTENING,
    USER_SPEAKING,
    PROCESSING,
    SEARCHING,
    THINKING,
    SPEAKING,
    PAUSED,
    ERROR,
    STOPPED
}

class LoriVoiceEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Continuous Hands-free Conversation State
    private var isContinuousModeEnabled = false
    private var isPaused = false
    private var activeLanguageCode = "hi-IN"

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var onSpeechErrorCallback: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val hiLocale = Locale("hi", "IN")
                val langResult = textToSpeech?.setLanguage(hiLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.language = Locale("en", "IN")
                }
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.05f)

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                        _statusMessage.value = "Lori bol rahi hai..."
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch {
                            _voiceState.value = VoiceState.IDLE
                            _statusMessage.value = "Ready"

                            // PREVENT SELF-LISTENING & AUTO-RESUME CONTINUOUS LISTENING
                            // Wait for a short transition (450ms) to ensure audio hardware is silent
                            if (isContinuousModeEnabled && !isPaused && onSpeechResultCallback != null) {
                                mainHandler.postDelayed({
                                    if (isContinuousModeEnabled && !isPaused && _voiceState.value != VoiceState.SPEAKING) {
                                        startListeningInternal(activeLanguageCode)
                                    }
                                }, 450L)
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch {
                            _voiceState.value = VoiceState.IDLE
                            _statusMessage.value = "Voice output error"
                            if (isContinuousModeEnabled && !isPaused) {
                                mainHandler.postDelayed({
                                    startListeningInternal(activeLanguageCode)
                                }, 500L)
                            }
                        }
                    }
                })
            }
        }
    }

    /**
     * Set Continuous Voice Conversation Mode (ON / OFF)
     */
    fun setContinuousMode(enabled: Boolean) {
        isContinuousModeEnabled = enabled
        if (!enabled && _voiceState.value == VoiceState.LISTENING) {
            stopListening()
        }
    }

    fun isContinuousMode(): Boolean = isContinuousModeEnabled

    /**
     * Pause continuous voice listening
     */
    fun pauseVoiceMode() {
        isPaused = true
        stopListening()
        _voiceState.value = VoiceState.PAUSED
        _statusMessage.value = "Voice Mode Paused. Tap Resume."
    }

    /**
     * Resume continuous voice listening
     */
    fun resumeVoiceMode() {
        isPaused = false
        if (onSpeechResultCallback != null) {
            startListeningInternal(activeLanguageCode)
        }
    }

    /**
     * Starts listening directly from the device microphone for Hindi and Hinglish commands.
     * Completes full message capture without cutting off user during natural pauses.
     */
    fun startListening(
        languageCode: String = "hi-IN",
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        isPaused = false
        activeLanguageCode = languageCode
        onSpeechResultCallback = onResult
        onSpeechErrorCallback = onError
        startListeningInternal(languageCode)
    }

    private fun startListeningInternal(languageCode: String) {
        // Strict check: if Lori is speaking, NEVER start listening (prevents self-listening)
        if (textToSpeech?.isSpeaking == true) {
            return
        }

        // Verify microphone permission
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            val err = "Microphone permission required! Please allow audio permission in Settings."
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = err
            onSpeechErrorCallback?.invoke(err)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = "Speech recognition is not available on this device."
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = err
            onSpeechErrorCallback?.invoke(err)
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                            _statusMessage.value = if (isContinuousModeEnabled) {
                                "Lori sun rahi hai... (Continuous Hands-Free)"
                            } else {
                                "Lori sun rahi hai... (Hindi / Hinglish)"
                            }
                            _liveSpokenText.value = ""
                        }

                        override fun onBeginningOfSpeech() {
                            _voiceState.value = VoiceState.USER_SPEAKING
                            _statusMessage.value = "Listening to your message..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _rmsDb.value = rmsdB.coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = VoiceState.PROCESSING
                            _statusMessage.value = "Lori samajh rahi hai... (Processing full request)"
                        }

                        override fun onError(error: Int) {
                            _rmsDb.value = 0f
                            
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic."
                                SpeechRecognizer.ERROR_CLIENT -> "Client error in SpeechRecognizer."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                SpeechRecognizer.ERROR_NETWORK -> "Internet connection chahiye speech recognition ke liye."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout ho gaya."
                                SpeechRecognizer.ERROR_NO_MATCH -> "Kuchh sunai nahi diya. Kripya dobara boliye."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy hai."
                                SpeechRecognizer.ERROR_SERVER -> "Server error occurred."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timeout."
                                else -> "Speech recognition error ($error)"
                            }

                            // If in continuous mode and it was a timeout / no match, seamlessly restart listening
                            if (isContinuousModeEnabled && !isPaused && (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH)) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusMessage.value = "Lori sun rahi hai... (Hands-free active)"
                                mainHandler.postDelayed({
                                    if (isContinuousModeEnabled && !isPaused && _voiceState.value != VoiceState.SPEAKING) {
                                        startListeningInternal(activeLanguageCode)
                                    }
                                }, 300L)
                            } else {
                                _voiceState.value = if (isContinuousModeEnabled) VoiceState.LISTENING else VoiceState.IDLE
                                _statusMessage.value = message
                                onSpeechErrorCallback?.invoke(message)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _rmsDb.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            _liveSpokenText.value = text

                            if (text.isNotEmpty()) {
                                _voiceState.value = VoiceState.PROCESSING
                                _statusMessage.value = "Processing complete message..."
                                onSpeechResultCallback?.invoke(text)
                            } else {
                                if (isContinuousModeEnabled && !isPaused) {
                                    startListeningInternal(activeLanguageCode)
                                } else {
                                    _voiceState.value = VoiceState.IDLE
                                    _statusMessage.value = "Ready"
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = partial?.firstOrNull()?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                _liveSpokenText.value = text
                                _voiceState.value = VoiceState.USER_SPEAKING
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                // Full Listening Parameters: 2500ms silence length for natural pauses without premature interruption
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "hi-Latn", "en-US"))
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("LoriVoiceEngine", "Error starting speech recognition", e)
                _voiceState.value = VoiceState.ERROR
                _statusMessage.value = "Mic start karne me dikkat aayi: ${e.localizedMessage}"
                onSpeechErrorCallback?.invoke(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                if (_voiceState.value != VoiceState.SPEAKING && _voiceState.value != VoiceState.PAUSED) {
                    _voiceState.value = if (isContinuousModeEnabled) VoiceState.STOPPED else VoiceState.IDLE
                }
                _rmsDb.value = 0f
            } catch (e: Exception) {
                Log.e("LoriVoiceEngine", "Error stopping listening", e)
            }
        }
    }

    fun setVoiceState(state: VoiceState, message: String = "") {
        _voiceState.value = state
        if (message.isNotBlank()) {
            _statusMessage.value = message
        }
    }

    /**
     * Speaks text using Text-to-Speech.
     * Prevents self-listening by stopping microphone recognition during speech.
     */
    fun speak(text: String, speechRate: Float = 1.0f, pitch: Float = 1.05f) {
        if (!isTtsInitialized || text.isBlank()) return
        
        // Strict: stop mic before speaking so Lori doesn't hear herself
        stopListening()
        
        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(pitch)

        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "$1")
            .replace(Regex("[#_`~]"), "")
            .trim()

        val utteranceId = "lori_speech_${System.currentTimeMillis()}"
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        _voiceState.value = VoiceState.IDLE
        _statusMessage.value = "Ready"
    }

    fun isWakeWordDetected(spokenText: String, wakePhrase: String = "Lori"): Boolean {
        val lower = spokenText.lowercase()
        val wake = wakePhrase.lowercase()
        return lower.contains("hey $wake") ||
                lower.contains("hi $wake") ||
                lower.contains("namaste $wake") ||
                lower.contains("ok $wake") ||
                lower.startsWith(wake) ||
                lower.contains(wake)
    }

    fun extractCommandAfterWakeWord(spokenText: String, wakePhrase: String = "Lori"): String {
        val regex = Regex("(?i)(?:hey\\s+|hi\\s+|namaste\\s+|ok\\s+)?${Regex.escape(wakePhrase)}[,\\s]*(.*)", RegexOption.IGNORE_CASE)
        val match = regex.find(spokenText.trim())
        val command = match?.groups?.get(1)?.value?.trim()
        return if (!command.isNullOrBlank()) command else spokenText.trim()
    }

    fun destroy() {
        try {
            isContinuousModeEnabled = false
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e("LoriVoiceEngine", "Error destroying voice engine", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LoriVoiceEngine? = null

        fun getInstance(context: Context): LoriVoiceEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoriVoiceEngine(context).also { INSTANCE = it }
            }
        }
    }
}


