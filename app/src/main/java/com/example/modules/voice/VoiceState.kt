package com.example.modules.voice

/**
 * VoiceState represents all discrete lifecycle states for speech input & synthesis.
 */
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
