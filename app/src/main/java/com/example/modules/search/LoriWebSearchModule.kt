package com.example.modules.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.remote.GroundingSource

object LoriWebSearchModule {

    /**
     * Detects if the user prompt implies real-time internet search or live data verification
     */
    fun shouldTriggerWebSearch(prompt: String): Boolean {
        val lower = prompt.lowercase()
        val searchKeywords = listOf(
            "internet", "search", "google", "web", "online",
            "latest", "taaza", "aaj ki", "news", "khabar",
            "price", "daam", "keemat", "rate",
            "current", "abhi", "update", "released", "release date",
            "kahan available", "where to watch", "ott", "theatre",
            "restaurant", "hotel", "score", "match", "weather", "mausam",
            "dekh ke bata", "dhoondo", "search karo", "pata karo"
        )
        return searchKeywords.any { lower.contains(it) }
    }

    /**
     * Opens a verified source URL in the system browser
     */
    fun openSourceInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
