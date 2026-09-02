package com.example.commands

import java.util.regex.Pattern

/**
 * Command Parser that detects official device intents from Hindi, Hinglish, and English phrases.
 */
class CommandParser {

    fun parse(input: String): LoriCommand {
        val text = input.trim().lowercase()

        // 1. OPEN_APP detection (e.g. "YouTube kholo", "open WhatsApp", "camera open karo")
        val openAppKeywords = listOf("kholo", "open", "chalao", "start", "launch")
        for (kw in openAppKeywords) {
            if (text.contains("whatsapp") && (text.contains(kw) || text.startsWith("open"))) {
                return LoriCommand(intent = CommandIntent.OPEN_APP, target = "com.whatsapp", title = "WhatsApp")
            }
            if (text.contains("youtube") && (text.contains(kw) || text.startsWith("open"))) {
                return LoriCommand(intent = CommandIntent.OPEN_APP, target = "com.google.android.youtube", title = "YouTube")
            }
            if ((text.contains("camera") || text.contains("photo")) && text.contains(kw)) {
                return LoriCommand(intent = CommandIntent.OPEN_APP, target = "camera", title = "Camera")
            }
            if ((text.contains("calculator") || text.contains("calc")) && text.contains(kw)) {
                return LoriCommand(intent = CommandIntent.OPEN_APP, target = "calculator", title = "Calculator")
            }
            if ((text.contains("settings") || text.contains("setting")) && text.contains(kw)) {
                return LoriCommand(intent = CommandIntent.OPEN_APP, target = "settings", title = "Settings")
            }
            if ((text.contains("clock") || text.contains("ghadi")) && text.contains(kw)) {
                return LoriCommand(intent = CommandIntent.OPEN_APP, target = "clock", title = "Clock")
            }
        }

        // 2. SET_ALARM detection (e.g. "Subah 7 baje ka alarm lagao", "Set alarm for 6:30 AM")
        if (text.contains("alarm") || text.contains("jaga dena") || text.contains("wake me up")) {
            val timeRegex = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm|baje)?")
            val matcher = timeRegex.matcher(text)
            if (matcher.find()) {
                var hour = matcher.group(1)?.toIntOrNull() ?: 7
                val minute = matcher.group(3)?.toIntOrNull() ?: 0
                val amPm = matcher.group(4)

                if (amPm != null && amPm.contains("pm") && hour < 12) {
                    hour += 12
                } else if ((text.contains("shaam") || text.contains("raat")) && hour < 12) {
                    hour += 12
                }

                return LoriCommand(
                    intent = CommandIntent.SET_ALARM,
                    hour = hour,
                    minute = minute,
                    title = "Lori Alarm"
                )
            }
        }

        // 3. START_NAVIGATION detection (e.g. "Delhi ka rasta dikhao", "Navigate to Connaught Place", "Rasta batao")
        if (text.contains("rasta") || text.contains("navigate") || text.contains("direction") || text.contains("map")) {
            val cleanDest = text
                .replace("rasta dikhao", "")
                .replace("rasta batao", "")
                .replace("navigate to", "")
                .replace("directions to", "")
                .replace("ka rasta", "")
                .replace("map par", "")
                .trim()
            if (cleanDest.isNotBlank()) {
                return LoriCommand(
                    intent = CommandIntent.START_NAVIGATION,
                    destination = cleanDest
                )
            }
        }

        // 4. SOS STROBE / EMERGENCY BLINK (e.g. "SOS chalao", "Emergency light", "Strobe on karo")
        if (text.contains("sos") || text.contains("emergency light") || text.contains("strobe")) {
            return LoriCommand(
                intent = CommandIntent.TOGGLE_SOS_STROBE,
                title = "SOS Emergency Strobe"
            )
        }

        // 5. PLAY_MEDIA / YOUTUBE SEARCH (e.g. "Arijit Singh ke gaane chalao", "Play songs on YouTube")
        if (text.contains("gaana") || text.contains("gaane") || text.contains("play song") || text.contains("play music") || text.contains("song chalao") || text.contains("music chalao")) {
            val songQuery = text
                .replace("gaana chalao", "")
                .replace("gaane chalao", "")
                .replace("song chalao", "")
                .replace("music chalao", "")
                .replace("play song", "")
                .replace("play", "")
                .replace("sunao", "")
                .trim()
            return LoriCommand(
                intent = CommandIntent.PLAY_MEDIA,
                query = songQuery.ifBlank { "Top Hindi Songs" }
            )
        }

        // 5. SEARCH_WEB detection (e.g. "Google par search karo", "Search on web")
        if (text.startsWith("search") || text.contains("search karo") || text.contains("google karo")) {
            val query = text
                .replace("search karo", "")
                .replace("google karo", "")
                .replace("search", "")
                .replace("google", "")
                .replace("par", "")
                .trim()
            return LoriCommand(
                intent = CommandIntent.SEARCH_WEB,
                query = query
            )
        }

        // 6. SUPPORTED_PHONE_ACTION / CALL with Confirmation (e.g. "Rahul ko call lagao", "Call Papa")
        if (text.contains("call") || text.contains("phone lagao")) {
            val targetName = text
                .replace("call karo", "")
                .replace("call lagao", "")
                .replace("phone lagao", "")
                .replace("ko call", "")
                .replace("call", "")
                .trim()
            return LoriCommand(
                intent = CommandIntent.SUPPORTED_PHONE_ACTION,
                target = targetName.ifBlank { "contact" },
                requiresConfirmation = true,
                confirmationPrompt = "Kya aap $targetName ko call lagane ki confirmation dete hain?"
            )
        }

        // 7. SET_REMINDER / CALENDAR (e.g. "Reminder set karo", "Add reminder for meeting")
        if (text.contains("reminder") || text.contains("yaad dilana")) {
            val note = text
                .replace("reminder set karo", "")
                .replace("reminder lagao", "")
                .replace("yaad dilana", "")
                .replace("reminder", "")
                .trim()
            return LoriCommand(
                intent = CommandIntent.SET_REMINDER,
                title = note.ifBlank { "Important Reminder" }
            )
        }

        // 8. FLASHLIGHT / TORCH (e.g. "Torch on karo", "Flashlight band karo", "Batti jalao")
        if (text.contains("torch") || text.contains("flashlight") || text.contains("batti")) {
            val turnOff = text.contains("off") || text.contains("band") || text.contains("bujhao")
            return LoriCommand(
                intent = CommandIntent.TOGGLE_FLASHLIGHT,
                enable = !turnOff,
                title = if (turnOff) "Flashlight Off" else "Flashlight On"
            )
        }

        // 9. CHECK_BATTERY (e.g. "Battery kitni hai", "Battery status", "Charge check karo")
        if (text.contains("battery") || text.contains("charge kitna") || text.contains("battery level") || text.contains("charging status")) {
            return LoriCommand(
                intent = CommandIntent.CHECK_BATTERY,
                title = "Battery Telemetry"
            )
        }

        // 10. VOLUME_CONTROL (e.g. "Volume badhao", "Aawaz kam karo", "Mute karo")
        if (text.contains("volume") || text.contains("aawaz")) {
            val direction = when {
                text.contains("badhao") || text.contains("tez") || text.contains("up") || text.contains("high") -> 1
                text.contains("kam") || text.contains("dheemi") || text.contains("down") || text.contains("low") -> -1
                text.contains("mute") || text.contains("band") -> 0
                else -> 1
            }
            return LoriCommand(
                intent = CommandIntent.VOLUME_CONTROL,
                volumeLevel = direction,
                title = "Volume Adjustment"
            )
        }

        // 11. SMART_ROUTINE (e.g. "Good morning", "Subah ka update", "Good night", "Sone ja raha hoon")
        if (text.contains("good morning") || text.contains("subah ho gayi") || text.contains("morning routine")) {
            return LoriCommand(
                intent = CommandIntent.SMART_ROUTINE,
                routineType = "morning",
                title = "Morning Protocol"
            )
        }
        if (text.contains("good night") || text.contains("shubh ratri") || text.contains("sone ja raha")) {
            return LoriCommand(
                intent = CommandIntent.SMART_ROUTINE,
                routineType = "night",
                title = "Night Protocol"
            )
        }

        // 12. SYSTEM DIAGNOSTICS (e.g. "Diagnostics", "System status", "Jarvis status report")
        if (text.contains("diagnostic") || text.contains("status report") || text.contains("system status") || text.contains("health check")) {
            return LoriCommand(
                intent = CommandIntent.DIAGNOSTICS,
                title = "System Diagnostics"
            )
        }

        return LoriCommand(intent = CommandIntent.UNKNOWN, query = text)
    }
}
