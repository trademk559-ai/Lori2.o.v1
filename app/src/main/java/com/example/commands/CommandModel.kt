package com.example.commands

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class CommandIntent {
    OPEN_APP,
    SET_ALARM,
    SET_REMINDER,
    START_NAVIGATION,
    SEARCH_WEB,
    PLAY_MEDIA,
    PAUSE_MEDIA,
    STOP_MEDIA,
    CALENDAR_ACTION,
    SUPPORTED_PHONE_ACTION,
    NOTIFICATION_ACTION,
    UNKNOWN
}

@JsonClass(generateAdapter = true)
data class LoriCommand(
    @Json(name = "intent") val intent: CommandIntent,
    @Json(name = "target") val target: String? = null,
    @Json(name = "query") val query: String? = null,
    @Json(name = "hour") val hour: Int? = null,
    @Json(name = "minute") val minute: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "destination") val destination: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "requiresConfirmation") val requiresConfirmation: Boolean = false,
    @Json(name = "confirmationPrompt") val confirmationPrompt: String? = null
)

sealed class CommandExecutionResult {
    data class Success(val message: String, val launchedIntent: Boolean = true) : CommandExecutionResult()
    data class RequiresConfirmation(val command: LoriCommand, val prompt: String) : CommandExecutionResult()
    data class PermissionRequired(val permissionName: String, val message: String) : CommandExecutionResult()
    data class Error(val errorMessage: String) : CommandExecutionResult()
}
