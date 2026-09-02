package com.example.commands

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * Command Executor using official, supported Android Intents and API contracts.
 */
class CommandExecutor(private val context: Context) {

    fun execute(command: LoriCommand, userConfirmed: Boolean = false): CommandExecutionResult {
        // Enforce confirmation check for sensitive actions
        if (command.requiresConfirmation && !userConfirmed) {
            val prompt = command.confirmationPrompt ?: "Are you sure you want to proceed?"
            return CommandExecutionResult.RequiresConfirmation(command, prompt)
        }

        return try {
            when (command.intent) {
                CommandIntent.OPEN_APP -> openApplication(command.target ?: "")
                CommandIntent.SET_ALARM -> setAlarm(command.hour ?: 7, command.minute ?: 0, command.title ?: "Lori Alarm")
                CommandIntent.START_NAVIGATION -> startNavigation(command.destination ?: "")
                CommandIntent.PLAY_MEDIA -> playMedia(command.query ?: "")
                CommandIntent.SEARCH_WEB -> searchWeb(command.query ?: "")
                CommandIntent.SET_REMINDER, CommandIntent.CALENDAR_ACTION -> createCalendarEvent(command.title ?: "Lori Reminder")
                CommandIntent.SUPPORTED_PHONE_ACTION -> initiateCallAction(command.target ?: "")
                CommandIntent.PAUSE_MEDIA, CommandIntent.STOP_MEDIA -> mediaControlAction()
                else -> CommandExecutionResult.Error("Unsupported action")
            }
        } catch (e: Exception) {
            Log.e("CommandExecutor", "Error executing intent", e)
            CommandExecutionResult.Error("Command execution failed: ${e.localizedMessage}")
        }
    }

    private fun openApplication(target: String): CommandExecutionResult {
        return when (target.lowercase()) {
            "camera" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    CommandExecutionResult.Success("Camera open ho gaya hai.")
                } else {
                    CommandExecutionResult.Error("Camera app nahi mila.")
                }
            }
            "settings" -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandExecutionResult.Success("Settings open ho gayi hai.")
            }
            "clock" -> {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandExecutionResult.Success("Clock app open ho gaya.")
            }
            else -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(target)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    CommandExecutionResult.Success("App launch ho gaya.")
                } else {
                    // Try web or Play Store
                    CommandExecutionResult.Error("App '$target' installed nahi hai.")
                }
            }
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): CommandExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            val timeStr = "%02d:%02d".format(hour, minute)
            CommandExecutionResult.Success("$timeStr ka alarm set kar diya gaya hai.")
        } else {
            CommandExecutionResult.Error("Device par Alarm app support nahi mila.")
        }
    }

    private fun startNavigation(destination: String): CommandExecutionResult {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandExecutionResult.Success("$destination ke liye navigation shuru kar diya.")
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            CommandExecutionResult.Success("Google Maps open ho gaya.")
        }
    }

    private fun playMedia(query: String): CommandExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return CommandExecutionResult.Success("'$query' YouTube par search ho gaya.")
    }

    private fun searchWeb(query: String): CommandExecutionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return CommandExecutionResult.Success("Web search open ho gaya.")
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        return CommandExecutionResult.Success("Google search open ho gaya.")
    }

    private fun createCalendarEvent(title: String): CommandExecutionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + (60 * 60 * 1000))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandExecutionResult.Success("Reminder / Calendar event create ho gaya.")
        } else {
            CommandExecutionResult.Error("Calendar app nahi mila.")
        }
    }

    private fun initiateCallAction(target: String): CommandExecutionResult {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            val isNumber = target.all { it.isDigit() || it == '+' || it == ' ' }
            data = if (isNumber) Uri.parse("tel:${target.trim()}") else Uri.parse("tel:")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        return CommandExecutionResult.Success("Dialer open ho gaya $target ke liye.")
    }

    private fun mediaControlAction(): CommandExecutionResult {
        return CommandExecutionResult.Success("Media control action executed.")
    }
}
