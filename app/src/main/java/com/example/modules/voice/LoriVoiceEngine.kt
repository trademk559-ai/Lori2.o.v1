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

    fun updateWakeWordConfig(enabled: Boolean, sensitivity: Float) {
        speechManager.updateWakeWordConfig(enabled, sensitivity)
    }

    fun getComputedSilenceWindowMs(): Long {
        return speechManager.getComputedSilenceWindowMs()
    }

    /**
     * Checks if Lori's wake word is spoken. Supports English and Devanagari variations,
     * adapting keyword tolerance according to the sensitivity level.
     */
    fun isWakeWordDetected(
        spokenText: String,
        wakePhrase: String = "Lori",
        sensitivity: Float = 0.75f
    ): Boolean {
        val lower = spokenText.lowercase().trim()
        val wake = wakePhrase.lowercase().trim()
        val baseKeywords = listOf(wake, "lori", "लोरी")
        val standardKeywords = listOf(
            wake, "lori", "lauri", "lowri", "lory", "loori", "lore",
            "लोरी", "लौरी", "लॉरी"
        )
        val highSensitivityKeywords = listOf(
            wake, "lori", "lauri", "lowri", "lory", "loori", "lore",
            "lorri", "rolli", "lo ree", "l ori",
            "लोरी", "लौरी", "लॉरी", "लोरि", "लॉरि"
        )

        val targetKeywords = when {
            sensitivity < 0.4f -> baseKeywords
            sensitivity <= 0.8f -> standardKeywords
            else -> highSensitivityKeywords
        }

        return targetKeywords.any { kw -> lower.contains(kw) }
    }

    /**
     * Detects when the user is simply calling out Lori's name to summon her
     * (e.g. "Lori", "Hey Lori", "Lori suno", "Arey Lori", "लोरी", "हे लोरी")
     */
    fun isPureWakeCall(spokenText: String, wakePhrase: String = "Lori"): Boolean {
        val lower = spokenText.lowercase().trim()
        val clean = lower.replace(Regex("[?,.!\n\r]"), "").trim()

        val directCalls = setOf(
            "lori", "hey lori", "hi lori", "hello lori", "ok lori", "okay lori",
            "suno lori", "lori suno", "arey lori", "oye lori", "bol lori", "lori ji",
            "namaste lori", "lauri", "hey lauri", "hi lauri", "lori hazir ho", "lori kaha ho",
            "lori online ho", "kaisi ho lori", "kya haal hai lori",
            "लोरी", "हे लोरी", "लोरी सुनो", "सुनो लोरी", "अरे लोरी", "नमस्ते लोरी", "लोरी जी"
        )
        if (directCalls.contains(clean)) return true

        val afterWake = extractCommandAfterWakeWord(clean, wakePhrase).trim()
        val conversationalPings = setOf(
            "", "suno", "kaho", "batao", "ji", "hazir ho", "kaha ho", "sun rahi ho",
            "kaisi ho", "kya haal hai", "boliye", "सुनो", "कहो", "बताओ", "हाजिर हो", "कहाँ हो"
        )
        return conversationalPings.contains(afterWake)
    }

    /**
     * Returns a warm, loyal, and friendly response greeting the user as Boss.
     */
    fun getFriendlyWakeCallReply(): String {
        val replies = listOf(
            "Haan Boss! Boliye, main sun rahi hoon. Kya aadesh hai?",
            "Haan Boss! Lori hazir hai. Boliye, kya madad karoon?",
            "Ji Boss! All systems ready hain. Hukum kijiye!",
            "Haan Boss! Boliye, main bilkul taiyaar hoon. Bataiye kya karna hai?",
            "Yes Boss! Sun rahi hoon, bataiye aaj kya command hai aapka?",
            "Haan Boss! Lori online hai. Boliye, kya seva karoon?"
        )
        return replies.random()
    }

    fun extractCommandAfterWakeWord(spokenText: String, wakePhrase: String = "Lori"): String {
        val regex = Regex("(?i)(?:hey\\s+|hi\\s+|namaste\\s+|ok\\s+|arey\\s+|oye\\s+|suno\\s+)?(?:${Regex.escape(wakePhrase)}|लोरी|लौरी|लॉरी)[,\\s]*(.*)", RegexOption.IGNORE_CASE)
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



