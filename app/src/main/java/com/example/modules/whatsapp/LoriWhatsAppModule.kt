package com.example.modules.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WhatsAppDraft(
    @Json(name = "recipient") val recipient: String,
    @Json(name = "proposedReply") val proposedReply: String,
    @Json(name = "originalMessageSnippet") val originalMessageSnippet: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
)

object LoriWhatsAppModule {

    /**
     * Determines if user wants to compose or reply to a WhatsApp message
     */
    fun isWhatsAppAction(prompt: String): Boolean {
        val lower = prompt.lowercase()
        return lower.contains("whatsapp") ||
                lower.contains("ko reply") ||
                lower.contains("ko message") ||
                lower.contains("reply kar do") ||
                lower.contains("message bhej") ||
                lower.contains("jawab de do")
    }

    /**
     * Extracts recipient and intent from casual Hindi/Hinglish instructions
     */
    fun parseWhatsAppIntent(prompt: String): Pair<String, String>? {
        // e.g. "Rahul ko reply kar do ki main 5 baje aa raha hoon"
        // e.g. "Priya ko message bhejo Good morning"
        val regexWithKi = Regex("""([a-zA-Z\u0900-\u097F]+)\s+ko\s+(?:reply|message|jawab)\s+(?:kar do|bhejo|de do|kardo)?\s*(?:ki|that)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val match = regexWithKi.find(prompt)
        if (match != null) {
            val recipient = match.groupValues[1].trim()
            val text = match.groupValues[2].trim()
            return Pair(recipient, text)
        }
        return null
    }

    /**
     * Opens official WhatsApp chat or share sheet with prepared message
     */
    fun sendWhatsAppMessage(
        context: Context,
        message: String,
        phoneNumber: String? = null
    ): Boolean {
        try {
            if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
                val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            }

            // Standard WhatsApp Send Intent
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
            return true
        } catch (e: Exception) {
            // Fallback: general share intent if WhatsApp is not directly resolvable
            try {
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(chooserIntent, "Share message via WhatsApp / App")
                chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(chooser)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp open nahi ho paya", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }
}
