package com.example.modules.youtube

import android.content.Context
import android.content.Intent
import android.net.Uri

data class YouTubeRequest(
    val query: String,
    val mood: String? = null,
    val artist: String? = null,
    val spokenConfirmation: String
)

object LoriYouTubeModule {

    /**
     * Checks if user prompt is a YouTube music or video request
     */
    fun isYouTubeAction(prompt: String): Boolean {
        val lower = prompt.lowercase()
        val keywords = listOf(
            "youtube", "gana", "gaana", "song", "music",
            "chalao", "sunao", "bajao", "play", "video",
            "workout song", "romantic gana", "sad song", "party song",
            "arijit", "shreya", "sidhu", "apne hisaab se koi gana"
        )
        return keywords.any { lower.contains(it) }
    }

    /**
     * Extracts YouTube search keywords from natural Hindi/Hinglish instructions
     */
    fun extractYouTubeQuery(prompt: String): YouTubeRequest {
        val lower = prompt.lowercase()

        val moodOrGenre = when {
            lower.contains("workout") || lower.contains("gym") || lower.contains("tagda") -> "tagda energetic workout songs"
            lower.contains("romantic") || lower.contains("love") || lower.contains("pyaar") -> "romantic bollywood hindi songs"
            lower.contains("party") || lower.contains("dance") || lower.contains("dhamaka") -> "best party dance hindi songs"
            lower.contains("sad") || lower.contains("dard") -> "sad emotional hindi songs"
            lower.contains("relax") || lower.contains("peaceful") || lower.contains("soothing") -> "calm relaxing lo-fi hindi acoustic songs"
            lower.contains("bhajan") || lower.contains("devotional") || lower.contains("aarti") -> "popular devotional bhajan"
            lower.contains("apne hisaab se") || lower.contains("koi achha") || lower.contains("kuchh achha") -> "superhit hindi songs latest trending"
            else -> null
        }

        // Clean out conversational prefixes
        var cleaned = prompt
            .replace(Regex("""(?i)\b(lori|suno|ek kaam karo|bhai|yaar|chalo|kholo|aur|par|pe|chalao|chala|bajao|baja do|sunao|suna do|play|karo|do)\b"""), " ")
            .replace(Regex("""(?i)\byoutube\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val finalQuery = if (cleaned.isBlank() && moodOrGenre != null) {
            moodOrGenre
        } else if (cleaned.isNotBlank()) {
            if (!cleaned.contains("song", ignoreCase = true) && !cleaned.contains("gana", ignoreCase = true)) {
                "$cleaned song"
            } else {
                cleaned
            }
        } else {
            "top trending hindi songs"
        }

        val spokenMsg = if (moodOrGenre != null) {
            "Frequency calibrated. Streaming audio sequence on YouTube: $finalQuery"
        } else {
            "Initializing media protocol. Playing '$finalQuery' on YouTube."
        }

        return YouTubeRequest(
            query = finalQuery,
            mood = moodOrGenre,
            spokenConfirmation = spokenMsg
        )
    }

    /**
     * Opens official YouTube app or Web YouTube search
     */
    fun openYouTube(context: Context, query: String): Boolean {
        try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appIntent)
            return true
        } catch (e: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                return true
            } catch (ex: Exception) {
                return false
            }
        }
    }
}
