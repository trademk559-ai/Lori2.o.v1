package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.commands.CommandExecutionResult
import com.example.commands.CommandExecutor
import com.example.commands.CommandParser
import com.example.commands.CommandValidator
import com.example.commands.LoriCommand
import com.example.data.local.ChatMessageEntity
import com.example.data.local.LoriDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.prefs.LoriPreferences
import com.example.data.prefs.LoriSettingsState
import com.example.data.remote.GroundingSource
import com.example.modules.background.LoriForegroundService
import com.example.modules.chat.ChatResult
import com.example.modules.chat.LoriChatEngine
import com.example.modules.notifications.LoriNotificationManager
import com.example.modules.security.AuthState
import com.example.modules.security.SecureAuthManager
import com.example.modules.settings.PermissionHelper
import com.example.modules.settings.PermissionStatus
import com.example.modules.telephony.CallAction
import com.example.modules.telephony.IncomingCallInfo
import com.example.modules.telephony.LoriCallModule
import com.example.modules.voice.LoriVoiceEngine
import com.example.modules.voice.VoiceState
import com.example.modules.whatsapp.LoriWhatsAppModule
import com.example.modules.whatsapp.WhatsAppDraft
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoriMainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val db = LoriDatabase.getDatabase(context)
    private val dao = db.loriDao()
    private val prefs = LoriPreferences.getInstance(context)
    private val voiceEngine = LoriVoiceEngine.getInstance(context)
    private val chatEngine = LoriChatEngine(context)
    val authManager = SecureAuthManager.getInstance(context)

    // Command System
    private val commandParser = CommandParser()
    private val commandValidator = CommandValidator(context)
    private val commandExecutor = CommandExecutor(context)

    // Authentication State
    val authState: StateFlow<AuthState> = authManager.authState

    // Settings
    val settings: StateFlow<LoriSettingsState> = prefs.settings

    // Database Flows
    val allMessages: StateFlow<List<ChatMessageEntity>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<NotificationLogEntity>> = dao.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice State
    val voiceState: StateFlow<VoiceState> = voiceEngine.voiceState
    val rmsDb: StateFlow<Float> = voiceEngine.rmsDb
    val liveSpokenText: StateFlow<String> = voiceEngine.liveSpokenText
    val voiceStatusMessage: StateFlow<String> = voiceEngine.statusMessage

    // Incoming Call State
    val incomingCallState: StateFlow<IncomingCallInfo?> = LoriCallModule.currentCallState

    val deviceController = commandExecutor.deviceController

    private val _batteryTelemetry = MutableStateFlow(deviceController.getBatteryTelemetry())
    val batteryTelemetry: StateFlow<com.example.modules.device.DeviceController.BatteryInfo> = _batteryTelemetry.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(deviceController.isTorchActive())
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _isSosActive = MutableStateFlow(deviceController.isSosActive())
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    // Background Service Running State
    val isBackgroundServiceRunning: StateFlow<Boolean> = LoriForegroundService.isRunning
    val isForegroundServiceRunning: StateFlow<Boolean> = isBackgroundServiceRunning

    // UI Interactive States
    private val _pendingWhatsAppDraft = MutableStateFlow<WhatsAppDraft?>(null)
    val pendingWhatsAppDraft: StateFlow<WhatsAppDraft?> = _pendingWhatsAppDraft.asStateFlow()

    private val _pendingCommandConfirmation = MutableStateFlow<LoriCommand?>(null)
    val pendingCommandConfirmation: StateFlow<LoriCommand?> = _pendingCommandConfirmation.asStateFlow()

    private val _isVoiceOverlayActive = MutableStateFlow(false)
    val isVoiceOverlayActive: StateFlow<Boolean> = _isVoiceOverlayActive.asStateFlow()

    private val _permissionsState = MutableStateFlow(PermissionHelper.checkAllPermissions(context))
    val permissionStatus: StateFlow<PermissionStatus> = _permissionsState.asStateFlow()
    val permissionsState: StateFlow<PermissionStatus> = permissionStatus

    private val _selectedTab = MutableStateFlow(0) // 0: Home, 1: Chat, 2: Search, 3: Alerts, 4: Settings
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val sourcesAdapter = moshi.adapter<List<GroundingSource>>(
        Types.newParameterizedType(List::class.java, GroundingSource::class.java)
    )

    init {
        // Automatically sync wake word state and acoustic sensitivity with AndroidSpeechManager
        viewModelScope.launch {
            settings.collect { s ->
                voiceEngine.updateWakeWordConfig(s.isWakeWordEnabled, s.wakeWordSensitivity)
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setVoiceOverlayActive(active: Boolean) {
        _isVoiceOverlayActive.value = active
        if (!active) {
            voiceEngine.stopListening()
            voiceEngine.stopSpeaking()
        }
    }

    fun setContinuousVoiceMode(enabled: Boolean) {
        updateSettings { it.copy(isContinuousVoiceMode = enabled) }
        voiceEngine.setContinuousMode(enabled)
    }

    fun pauseContinuousVoice() {
        voiceEngine.pauseVoiceMode()
    }

    fun resumeContinuousVoice() {
        voiceEngine.resumeVoiceMode()
    }

    // --- Voice Assistant Trigger ---
    fun startVoiceInteraction() {
        _isVoiceOverlayActive.value = true
        voiceEngine.stopSpeaking()
        voiceEngine.setContinuousMode(settings.value.isContinuousVoiceMode)

        val langCode = settings.value.preferredVoiceLang.ifBlank { "hi-IN" }
        voiceEngine.startListening(
            languageCode = langCode,
            onResult = { recognizedText ->
                processVoiceCommand(recognizedText)
            },
            onError = { _ ->
                // Voice engine will update statusMessage
            }
        )
    }

    fun stopVoiceInteraction() {
        voiceEngine.setContinuousMode(false)
        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
        _isVoiceOverlayActive.value = false
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
    }

    fun processVoiceCommand(spokenText: String) {
        viewModelScope.launch {
            val lower = spokenText.lowercase().trim()

            // Check if there is an active WhatsApp confirmation pending
            val draft = _pendingWhatsAppDraft.value
            if (draft != null) {
                if (lower.contains("haan") || lower.contains("bhej do") || lower.contains("kar do") || lower.contains("yes") || lower.contains("send")) {
                    confirmSendWhatsApp()
                    return@launch
                } else if (lower.contains("nahi") || lower.contains("mat karo") || lower.contains("rehne do") || lower.contains("cancel")) {
                    cancelWhatsAppDraft()
                    return@launch
                }
            }

            // Check if call action is active
            val currentCall = incomingCallState.value
            if (currentCall != null && currentCall.state == "RINGING") {
                when (LoriCallModule.parseCallVoiceCommand(spokenText)) {
                    CallAction.ANSWER -> {
                        LoriCallModule.answerCall(context)
                        voiceEngine.speak("Call receive kar rahi hoon", settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                        return@launch
                    }
                    CallAction.REJECT -> {
                        LoriCallModule.endCall(context)
                        voiceEngine.speak("Call cut kar diya", settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                        return@launch
                    }
                    CallAction.IGNORE -> {
                        voiceEngine.stopSpeaking()
                        return@launch
                    }
                    CallAction.NONE -> {}
                }
            }

            // Check if command confirmation is pending
            val pendingCmd = _pendingCommandConfirmation.value
            if (pendingCmd != null) {
                if (lower.contains("haan") || lower.contains("yes") || lower.contains("kar do") || lower.contains("confirm")) {
                    _pendingCommandConfirmation.value = null
                    val exec = commandExecutor.execute(pendingCmd, userConfirmed = true)
                    if (exec is CommandExecutionResult.Success) {
                        voiceEngine.speak(exec.message, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                    }
                    return@launch
                } else if (lower.contains("nahi") || lower.contains("no") || lower.contains("cancel") || lower.contains("mat karo")) {
                    _pendingCommandConfirmation.value = null
                    val cancelMsg = "Theek hai Boss, cancel kar diya gaya."
                    voiceEngine.speak(cancelMsg, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                    return@launch
                }
            }

            // 1. WAKE WORD FILTER: Answer ONLY when user addresses Lori
            val isWakeDetected = voiceEngine.isWakeWordDetected(
                spokenText = spokenText,
                wakePhrase = settings.value.wakePhrase,
                sensitivity = settings.value.wakeWordSensitivity
            )
            if (settings.value.isWakeWordEnabled && !isWakeDetected) {
                if (settings.value.isContinuousVoiceMode) {
                    voiceEngine.setVoiceState(VoiceState.LISTENING, "Listening for 'Lori'...")
                } else {
                    val promptUser = "Haan Boss! Aadesh dene ke liye 'Lori' bol kar baat kijiye."
                    voiceEngine.speak(promptUser, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                }
                return@launch
            }

            // 2. FRIENDLY WAKE CALL: When user specifically calls Lori ("Lori", "Hey Lori", "Lori suno")
            if (voiceEngine.isPureWakeCall(spokenText, settings.value.wakePhrase)) {
                val friendlyReply = voiceEngine.getFriendlyWakeCallReply()
                dao.insertMessage(ChatMessageEntity(role = "user", text = spokenText, messageType = "voice", timestamp = System.currentTimeMillis()))
                dao.insertMessage(ChatMessageEntity(role = "assistant", text = friendlyReply, messageType = "system", timestamp = System.currentTimeMillis()))
                voiceEngine.speak(friendlyReply, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                return@launch
            }

            // Check for Hardware & Device Commands (Flashlight, Battery, Volume, Alarms, Routines)
            val parsedCmd = commandParser.parse(spokenText)
            if (parsedCmd.intent != com.example.commands.CommandIntent.UNKNOWN &&
                parsedCmd.intent != com.example.commands.CommandIntent.SEARCH_WEB) {
                val validation = commandValidator.validate(parsedCmd)
                if (validation is com.example.commands.CommandValidator.ValidationResult.Valid) {
                    val execution = commandExecutor.execute(parsedCmd, userConfirmed = false)
                    when (execution) {
                        is CommandExecutionResult.RequiresConfirmation -> {
                            _pendingCommandConfirmation.value = execution.command
                            voiceEngine.speak(execution.prompt, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                            dao.insertMessage(ChatMessageEntity(role = "user", text = spokenText, messageType = "voice", timestamp = System.currentTimeMillis()))
                            dao.insertMessage(ChatMessageEntity(role = "assistant", text = execution.prompt, messageType = "system", timestamp = System.currentTimeMillis()))
                            return@launch
                        }
                        is CommandExecutionResult.Success -> {
                            _isFlashlightOn.value = deviceController.isTorchActive()
                            _batteryTelemetry.value = deviceController.getBatteryTelemetry()
                            voiceEngine.speak(execution.message, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                            dao.insertMessage(ChatMessageEntity(role = "user", text = spokenText, messageType = "voice", timestamp = System.currentTimeMillis()))
                            dao.insertMessage(ChatMessageEntity(role = "assistant", text = execution.message, messageType = "system", timestamp = System.currentTimeMillis()))
                            return@launch
                        }
                        is CommandExecutionResult.PermissionRequired -> {
                            voiceEngine.speak(execution.message, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
                            dao.insertMessage(ChatMessageEntity(role = "user", text = spokenText, messageType = "voice", timestamp = System.currentTimeMillis()))
                            dao.insertMessage(ChatMessageEntity(role = "assistant", text = execution.message, messageType = "system", timestamp = System.currentTimeMillis()))
                            return@launch
                        }
                        is CommandExecutionResult.Error -> {
                            // Fall through to conversational AI
                        }
                    }
                }
            }

            // Indicate Thinking / Searching State
            if (lower.contains("weather") || lower.contains("mausam") || lower.contains("news") || lower.contains("khabar") || lower.contains("search") || lower.contains("score")) {
                voiceEngine.setVoiceState(VoiceState.SEARCHING, "Searching live information...")
            } else {
                voiceEngine.setVoiceState(VoiceState.THINKING, "Lori soch rahi hai...")
            }

            // Normal AI Processing with full message understanding
            val result = chatEngine.processUserMessage(spokenText, isVoiceInput = true)
            when (result) {
                is ChatResult.Success -> {
                    if (result.whatsAppDraft != null) {
                        _pendingWhatsAppDraft.value = result.whatsAppDraft
                    }
                    // Speak response aloud (preventing self-listening inside speak)
                    voiceEngine.speak(
                        result.responseText,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
                is ChatResult.Error -> {
                    voiceEngine.speak(
                        result.errorMessage,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
            }
        }
    }

    fun sendTextMessage(text: String) {
        viewModelScope.launch {
            val result = chatEngine.processUserMessage(text, isVoiceInput = false)
            if (result is ChatResult.Success) {
                if (result.whatsAppDraft != null) {
                    _pendingWhatsAppDraft.value = result.whatsAppDraft
                }
                // Optional voice readout if voice assistant is enabled
                if (settings.value.isVoiceAssistantEnabled && !settings.value.isQuietMode) {
                    voiceEngine.speak(
                        result.responseText,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
            }
        }
    }

    fun confirmSendWhatsApp() {
        val draft = _pendingWhatsAppDraft.value ?: return
        LoriWhatsAppModule.sendWhatsAppMessage(context, draft.proposedReply, draft.phoneNumber)
        _pendingWhatsAppDraft.value = null
        voiceEngine.speak(
            "WhatsApp message bhej diya bhai!",
            settings.value.ttsSpeechRate,
            settings.value.ttsSpeechPitch
        )
    }

    fun cancelWhatsAppDraft() {
        _pendingWhatsAppDraft.value = null
        voiceEngine.speak(
            "Theek hai, cancel kar diya.",
            settings.value.ttsSpeechRate,
            settings.value.ttsSpeechPitch
        )
    }

    fun speakMessageAloud(text: String) {
        voiceEngine.speak(text, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
    }

    fun clearChat() {
        viewModelScope.launch {
            chatEngine.clearHistory()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            dao.clearAllNotifications()
        }
    }

    fun toggleBackgroundService(enable: Boolean) {
        prefs.updateSettings { it.copy(isBackgroundModeEnabled = enable) }
        if (enable) {
            LoriForegroundService.startService(context)
        } else {
            LoriForegroundService.stopService(context)
        }
    }

    fun refreshPermissions() {
        _permissionsState.value = PermissionHelper.checkAllPermissions(context)
    }

    /**
     * Emergency Killswitch: Stops microphone, TTS, background foreground service,
     * cancels active drafts/confirmations, and disables continuous voice activation.
     */
    fun stopAllActivity() {
        voiceEngine.stopListening()
        voiceEngine.stopSpeaking()
        _isVoiceOverlayActive.value = false
        _pendingWhatsAppDraft.value = null
        _pendingCommandConfirmation.value = null
        
        // Stop background foreground service
        LoriForegroundService.stopService(context)
        prefs.updateSettings {
            it.copy(
                isBackgroundModeEnabled = false,
                isVoiceAssistantEnabled = false,
                isWakeWordEnabled = false
            )
        }
    }

    fun executeDeviceCommand(commandText: String) {
        viewModelScope.launch {
            val command = commandParser.parse(commandText)
            val validation = commandValidator.validate(command)
            if (validation is CommandValidator.ValidationResult.Invalid) {
                // If not a structured command, fallback to natural conversational AI
                sendTextMessage(commandText)
                return@launch
            }

            val execution = commandExecutor.execute(command, userConfirmed = false)
            when (execution) {
                is CommandExecutionResult.RequiresConfirmation -> {
                    _pendingCommandConfirmation.value = execution.command
                    voiceEngine.speak(
                        execution.prompt,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
                is CommandExecutionResult.Success -> {
                    voiceEngine.speak(
                        execution.message,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
                is CommandExecutionResult.Error -> {
                    voiceEngine.speak(
                        execution.errorMessage,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
                is CommandExecutionResult.PermissionRequired -> {
                    voiceEngine.speak(
                        execution.message,
                        settings.value.ttsSpeechRate,
                        settings.value.ttsSpeechPitch
                    )
                }
            }
        }
    }

    fun confirmPendingCommand() {
        val cmd = _pendingCommandConfirmation.value ?: return
        _pendingCommandConfirmation.value = null
        val result = commandExecutor.execute(cmd, userConfirmed = true)
        if (result is CommandExecutionResult.Success) {
            voiceEngine.speak(result.message, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
        }
    }

    fun cancelPendingCommand() {
        _pendingCommandConfirmation.value = null
        voiceEngine.speak("Command cancel kar diya.", settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
    }

    fun toggleFlashlight() {
        val (success, msg) = deviceController.toggleFlashlight()
        _isFlashlightOn.value = deviceController.isTorchActive()
        voiceEngine.speak(msg, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
    }

    fun toggleSosStrobe() {
        val (success, msg) = deviceController.toggleSosStrobe()
        _isSosActive.value = deviceController.isSosActive()
        _isFlashlightOn.value = deviceController.isTorchActive()
        voiceEngine.speak(msg, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
    }

    fun refreshBatteryTelemetry() {
        _batteryTelemetry.value = deviceController.getBatteryTelemetry()
    }

    fun triggerRoutine(routineType: String) {
        viewModelScope.launch {
            val reply = when (routineType) {
                "morning" -> deviceController.runMorningRoutine()
                "night" -> {
                    val res = deviceController.runNightRoutine()
                    _isFlashlightOn.value = deviceController.isTorchActive()
                    res
                }
                "diagnostics" -> deviceController.runSystemDiagnostics()
                else -> deviceController.runSystemDiagnostics()
            }
            _batteryTelemetry.value = deviceController.getBatteryTelemetry()
            dao.insertMessage(ChatMessageEntity(role = "user", text = "Trigger $routineType routine", messageType = "routine", timestamp = System.currentTimeMillis()))
            dao.insertMessage(ChatMessageEntity(role = "assistant", text = reply, messageType = "routine", timestamp = System.currentTimeMillis()))
            voiceEngine.speak(reply, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
        }
    }

    fun adjustVolume(direction: Int) {
        val msg = deviceController.adjustVolume(direction)
        voiceEngine.speak(msg, settings.value.ttsSpeechRate, settings.value.ttsSpeechPitch)
    }

    fun updateSettings(update: (LoriSettingsState) -> LoriSettingsState) {
        prefs.updateSettings(update)
    }

    fun parseSourcesJson(jsonStr: String?): List<GroundingSource> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            sourcesAdapter.fromJson(jsonStr) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
