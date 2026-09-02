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
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
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
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch {
                            _voiceState.value = VoiceState.IDLE
                            _statusMessage.value = "Voice output error"
                        }
                    }
                })
            }
        }
    }

    /**
     * Starts listening directly from the device microphone for Hindi and Hinglish commands.
     */
    fun startListening(
        languageCode: String = "hi-IN",
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stopSpeaking()
        onSpeechResultCallback = onResult
        onSpeechErrorCallback = onError

        // Verify microphone permission
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            val err = "Microphone permission required! Please allow audio permission in Settings."
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = err
            onError(err)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = "Speech recognition is not available on this device."
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = err
            onError(err)
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                            _statusMessage.value = "Lori sun rahi hai... (Hindi / Hinglish me boliye)"
                            _liveSpokenText.value = ""
                        }

                        override fun onBeginningOfSpeech() {
                            _statusMessage.value = "Listening..."
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _rmsDb.value = rmsdB.coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = VoiceState.PROCESSING
                            _statusMessage.value = "Lori samajh rahi hai... (Processing)"
                        }

                        override fun onError(error: Int) {
                            _rmsDb.value = 0f
                            _voiceState.value = VoiceState.IDLE
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic."
                                SpeechRecognizer.ERROR_CLIENT -> "Client error in SpeechRecognizer."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                SpeechRecognizer.ERROR_NETWORK -> "Internet connection chahiye speech recognition ke liye."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout ho gaya."
                                SpeechRecognizer.ERROR_NO_MATCH -> "Kuchh sunai nahi diya. Kripya dobara boliye."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy hai. Dubara tap karein."
                                SpeechRecognizer.ERROR_SERVER -> "Server error occurred."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout. Kuchh nahi suna."
                                else -> "Speech recognition error ($error)"
                            }
                            _statusMessage.value = message
                            onSpeechErrorCallback?.invoke(message)
                        }

                        override fun onResults(results: Bundle?) {
                            _rmsDb.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            _liveSpokenText.value = text
                            _voiceState.value = VoiceState.IDLE
                            _statusMessage.value = "Ready"

                            if (text.isNotEmpty()) {
                                onSpeechResultCallback?.invoke(text)
                            } else {
                                onSpeechErrorCallback?.invoke("No speech recognized")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = partial?.firstOrNull()?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                _liveSpokenText.value = text
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                // Multilingual Intent for Hindi & Hinglish recognition
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                    // Additional languages for seamless Hindi + English (Hinglish) understanding
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "hi-Latn", "en-US"))
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("LoriVoiceEngine", "Error starting speech recognition", e)
                _voiceState.value = VoiceState.ERROR
                _statusMessage.value = "Mic start karne me dikkat aayi: ${e.localizedMessage}"
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _voiceState.value = VoiceState.IDLE
                _rmsDb.value = 0f
            } catch (e: Exception) {
                Log.e("LoriVoiceEngine", "Error stopping listening", e)
            }
        }
    }

    fun speak(text: String, speechRate: Float = 1.0f, pitch: Float = 1.05f) {
        if (!isTtsInitialized || text.isBlank()) return
        stopSpeaking()
        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(pitch)

        // Clean out markdown asterisks or citations before speaking for clear audio
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

    /**
     * Checks if input contains the Lori wake phrase
     */
    fun matchesWakeWord(input: String, wakePhrase: String = "Lori"): Boolean {
        val lower = input.lowercase().trim()
        val phrase = wakePhrase.lowercase().trim()
        val wakeKeywords = listOf(
            phrase,
            "hey $phrase",
            "he $phrase",
            "hello $phrase",
            "$phrase suno",
            "suno $phrase",
            "$phrase ek kaam karo",
            "$phrase ji"
        )
        return wakeKeywords.any { lower.startsWith(it) || lower.contains(it) }
    }

    /**
     * Strips wake word from user speech to extract actual command
     */
    fun extractCommandAfterWakeWord(input: String, wakePhrase: String = "Lori"): String {
        var clean = input.trim()
        val phrase = wakePhrase.trim()
        val prefixes = listOf(
            "hey $phrase,", "hey $phrase",
            "he $phrase,", "he $phrase",
            "hello $phrase,", "hello $phrase",
            "$phrase suno,", "$phrase suno",
            "suno $phrase,", "suno $phrase",
            "$phrase ek kaam karo,", "$phrase ek kaam karo",
            "$phrase ji,", "$phrase ji",
            "$phrase,", "$phrase"
        )
        for (prefix in prefixes) {
            if (clean.startsWith(prefix, ignoreCase = true)) {
                clean = clean.substring(prefix.length).trim()
                break
            }
        }
        return clean.trimStart(',', ':', '-', ' ').trim()
    }

    fun destroy() {
        try {
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

