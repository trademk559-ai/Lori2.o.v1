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

        // 4. PLAY_MEDIA / YOUTUBE SEARCH (e.g. "Arijit Singh ke gaane chalao", "Play songs on YouTube")
        if (text.contains("gaana") || text.contains("gaane") || text.contains("play song") || text.contains("play music") || text.contains("chalao")) {
            val songQuery = text
                .replace("gaana chalao", "")
                .replace("gaane chalao", "")
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

        return LoriCommand(intent = CommandIntent.UNKNOWN, query = text)
    }
}
