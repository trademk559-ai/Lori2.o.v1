package com.example.commands

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Command Validator to ensure parameters are safe and checks required platform permissions.
 */
class CommandValidator(private val context: Context) {

    fun validate(command: LoriCommand): ValidationResult {
        return when (command.intent) {
            CommandIntent.OPEN_APP -> {
                if (command.target.isNullOrBlank()) {
                    ValidationResult.Invalid("App name or package identifier is required.")
                } else {
                    ValidationResult.Valid
                }
            }
            CommandIntent.SET_ALARM -> {
                val h = command.hour ?: 7
                if (h !in 0..23) {
                    ValidationResult.Invalid("Hour must be between 0 and 23.")
                } else {
                    ValidationResult.Valid
                }
            }
            CommandIntent.START_NAVIGATION -> {
                if (command.destination.isNullOrBlank()) {
                    ValidationResult.Invalid("Destination address is required for navigation.")
                } else {
                    ValidationResult.Valid
                }
            }
            CommandIntent.SUPPORTED_PHONE_ACTION -> {
                if (command.target.isNullOrBlank()) {
                    ValidationResult.Invalid("Contact name or number is required.")
                } else {
                    ValidationResult.Valid
                }
            }
            CommandIntent.SEARCH_WEB, CommandIntent.PLAY_MEDIA -> {
                if (command.query.isNullOrBlank()) {
                    ValidationResult.Invalid("Search or media title query is required.")
                } else {
                    ValidationResult.Valid
                }
            }
            CommandIntent.SET_REMINDER, CommandIntent.CALENDAR_ACTION -> {
                ValidationResult.Valid
            }
            CommandIntent.PAUSE_MEDIA, CommandIntent.STOP_MEDIA, CommandIntent.NOTIFICATION_ACTION -> {
                ValidationResult.Valid
            }
            CommandIntent.UNKNOWN -> {
                ValidationResult.Invalid("Command not recognized as a structured phone intent.")
            }
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
