package com.example.modules.voice

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * LoriVoiceEngine:
 * Unified facade over AndroidSpeechManager for voice processing, wake word detection,
 * and command parsing.
 */
class LoriVoiceEngine private constructor(private val context: Context) {

    private val speechManager = AndroidSpeechManager.getInstance(context)

    val voiceState: StateFlow<VoiceState> = speechManager.voiceState
    val rmsDb: StateFlow<Float> = speechManager.rmsDb
    val liveSpokenText: StateFlow<String> = speechManager.liveTranscript
    val statusMessage: StateFlow<String> = speechManager.statusMessage
    val isContinuousMode: StateFlow<Boolean> = speechManager.isContinuousMode

    fun setContinuousMode(enabled: Boolean) {
        speechManager.setContinuousMode(enabled)
    }

    fun pauseVoiceMode() {
        speechManager.pauseContinuousMode()
    }

    fun resumeVoiceMode() {
        speechManager.resumeContinuousMode()
    }

    fun startListening(
        languageCode: String = "hi-IN",
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        speechManager.startListening(languageCode, onResult, onError)
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun setVoiceState(state: VoiceState, message: String = "") {
        speechManager.setVoiceState(state, message)
    }

    fun speak(text: String, speechRate: Float = 1.0f, pitch: Float = 1.05f) {
        speechManager.speak(text, speechRate, pitch)
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
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
        speechManager.destroy()
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



