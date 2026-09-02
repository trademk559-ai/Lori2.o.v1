package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoriSettingsState(
    val isVoiceAssistantEnabled: Boolean = true,
    val isWakeWordEnabled: Boolean = true,
    val wakePhrase: String = "Lori",
    val isContinuousVoiceMode: Boolean = false,
    val isBackgroundModeEnabled: Boolean = true,
    val isInternetSearchEnabled: Boolean = true,
    val isWhatsAppAssistantEnabled: Boolean = true,
    val isCallAssistantEnabled: Boolean = true,
    val isYouTubeAssistantEnabled: Boolean = true,
    val isVoiceNotificationAlertsEnabled: Boolean = true,
    val isReadFullNotificationContent: Boolean = true,
    val isReadAppName: Boolean = true,
    val isReadSenderName: Boolean = true,
    val isReadOnlyWhenUnlocked: Boolean = false,
    val isQuietMode: Boolean = false,
    val isDarkMode: Boolean = true,
    val excludedApps: Set<String> = emptySet(),
    val ttsSpeechRate: Float = 1.0f,
    val ttsSpeechPitch: Float = 1.05f,
    val preferredVoiceLang: String = "hi-IN" // "hi-IN" or "en-IN"
)

class LoriPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("lori_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LoriSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): LoriSettingsState {
        return LoriSettingsState(
            isVoiceAssistantEnabled = prefs.getBoolean("voice_assistant_enabled", true),
            isWakeWordEnabled = prefs.getBoolean("wake_word_enabled", true),
            wakePhrase = prefs.getString("wake_phrase", "Lori") ?: "Lori",
            isContinuousVoiceMode = prefs.getBoolean("continuous_voice_mode", false),
            isBackgroundModeEnabled = prefs.getBoolean("background_mode_enabled", true),
            isInternetSearchEnabled = prefs.getBoolean("internet_search_enabled", true),
            isWhatsAppAssistantEnabled = prefs.getBoolean("whatsapp_assistant_enabled", true),
            isCallAssistantEnabled = prefs.getBoolean("call_assistant_enabled", true),
            isYouTubeAssistantEnabled = prefs.getBoolean("youtube_assistant_enabled", true),
            isVoiceNotificationAlertsEnabled = prefs.getBoolean("voice_notification_alerts_enabled", true),
            isReadFullNotificationContent = prefs.getBoolean("read_full_notification_content", true),
            isReadAppName = prefs.getBoolean("read_app_name", true),
            isReadSenderName = prefs.getBoolean("read_sender_name", true),
            isReadOnlyWhenUnlocked = prefs.getBoolean("read_only_when_unlocked", false),
            isQuietMode = prefs.getBoolean("quiet_mode", false),
            isDarkMode = prefs.getBoolean("dark_mode", true),
            excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet(),
            ttsSpeechRate = prefs.getFloat("tts_speech_rate", 1.0f),
            ttsSpeechPitch = prefs.getFloat("tts_speech_pitch", 1.05f),
            preferredVoiceLang = prefs.getString("preferred_voice_lang", "hi-IN") ?: "hi-IN"
        )
    }

    fun updateSettings(update: (LoriSettingsState) -> LoriSettingsState) {
        val newState = update(_settings.value)
        _settings.value = newState
        prefs.edit().apply {
            putBoolean("voice_assistant_enabled", newState.isVoiceAssistantEnabled)
            putBoolean("wake_word_enabled", newState.isWakeWordEnabled)
            putString("wake_phrase", newState.wakePhrase)
            putBoolean("continuous_voice_mode", newState.isContinuousVoiceMode)
            putBoolean("background_mode_enabled", newState.isBackgroundModeEnabled)
            putBoolean("internet_search_enabled", newState.isInternetSearchEnabled)
            putBoolean("whatsapp_assistant_enabled", newState.isWhatsAppAssistantEnabled)
            putBoolean("call_assistant_enabled", newState.isCallAssistantEnabled)
            putBoolean("youtube_assistant_enabled", newState.isYouTubeAssistantEnabled)
            putBoolean("voice_notification_alerts_enabled", newState.isVoiceNotificationAlertsEnabled)
            putBoolean("read_full_notification_content", newState.isReadFullNotificationContent)
            putBoolean("read_app_name", newState.isReadAppName)
            putBoolean("read_sender_name", newState.isReadSenderName)
            putBoolean("read_only_when_unlocked", newState.isReadOnlyWhenUnlocked)
            putBoolean("quiet_mode", newState.isQuietMode)
            putBoolean("dark_mode", newState.isDarkMode)
            putStringSet("excluded_apps", newState.excludedApps)
            putFloat("tts_speech_rate", newState.ttsSpeechRate)
            putFloat("tts_speech_pitch", newState.ttsSpeechPitch)
            putString("preferred_voice_lang", newState.preferredVoiceLang)
            apply()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LoriPreferences? = null

        fun getInstance(context: Context): LoriPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoriPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
