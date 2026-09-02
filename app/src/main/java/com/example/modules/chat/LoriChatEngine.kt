package com.example.modules.chat

import android.content.Context
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.LoriDatabase
import com.example.data.prefs.LoriPreferences
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.data.remote.GroundingSource
import com.example.modules.notifications.LoriNotificationManager
import com.example.modules.search.LoriWebSearchModule
import com.example.modules.voice.LoriVoiceEngine
import com.example.modules.whatsapp.LoriWhatsAppModule
import com.example.modules.whatsapp.WhatsAppDraft
import com.example.modules.youtube.LoriYouTubeModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ChatResult {
    data class Success(
        val responseText: String,
        val messageType: String = "text",
        val sources: List<GroundingSource> = emptyList(),
        val whatsAppDraft: WhatsAppDraft? = null,
        val youTubeQuery: String? = null
    ) : ChatResult()

    data class Error(val errorMessage: String) : ChatResult()
}

class LoriChatEngine(private val context: Context) {

    private val db = LoriDatabase.getDatabase(context)
    private val dao = db.loriDao()
    private val prefs = LoriPreferences.getInstance(context)
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val sourcesAdapter = moshi.adapter<List<GroundingSource>>(
        Types.newParameterizedType(List::class.java, GroundingSource::class.java)
    )

    private val systemInstruction = GeminiContent(
        parts = listOf(
            GeminiPart(
                text = """
                You are Lori (लोरी), a smart, warm, friendly personal AI voice assistant built specifically for Hindi, Hinglish, and English speaking users.
                Tagline: "Tum bolo, Lori samjhe."
                
                PERSONALITY & TONE:
                - Friendly, witty, empathetic, respectful, and energetic.
                - Natural Hindi/Hinglish style using conversational Latin script (Hinglish) and Devanagari script.
                - Naturally use casual friendly terms like "bhai", "yaar", "bilkul", "are haan", "batao" in an authentic, comfortable tone.
                - During voice interactions, keep responses conversational and concise (1-3 sentences).
                - When asked for detailed explanations, provide structured and clear answers.
                - For questions about current affairs, prices, live events, or places, provide accurate, verified facts.
                """.trimIndent()
            )
        )
    )

    suspend fun processUserMessage(
        userPrompt: String,
        isVoiceInput: Boolean = false
    ): ChatResult = withContext(Dispatchers.IO) {
        val trimmedPrompt = userPrompt.trim()
        if (trimmedPrompt.isBlank()) {
            return@withContext ChatResult.Error("Kuchh type ya boliye!")
        }

        // Clean out wake word if present
        val cleanCommand = LoriVoiceEngine.getInstance(context)
            .extractCommandAfterWakeWord(trimmedPrompt, prefs.settings.value.wakePhrase)
            .ifBlank { trimmedPrompt }

        // Save User Message to Database
        val userEntity = ChatMessageEntity(
            role = "user",
            text = trimmedPrompt,
            messageType = if (isVoiceInput) "voice" else "text",
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(userEntity)

        val settings = prefs.settings.value

        // --- 1. Notification Queries ---
        if (settings.isVoiceNotificationAlertsEnabled && LoriNotificationManager.isNotificationCommand(cleanCommand)) {
            val response = LoriNotificationManager.handleNotificationVoiceQuery(context, cleanCommand)
            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                text = response,
                messageType = "notification",
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(assistantEntity)
            return@withContext ChatResult.Success(
                responseText = response,
                messageType = "notification"
            )
        }

        // --- 2. WhatsApp Action Understanding & Reply Drafting ---
        if (settings.isWhatsAppAssistantEnabled && LoriWhatsAppModule.isWhatsAppAction(cleanCommand)) {
            val parsed = LoriWhatsAppModule.parseWhatsAppIntent(cleanCommand)
            val recipient = parsed?.first ?: "Contact"
            val rawMsg = parsed?.second ?: cleanCommand

            // Ask Gemini to draft a natural friendly Hinglish reply
            val draftPrompt = "Generate a friendly, natural Hinglish reply message to $recipient based on this instruction: '$rawMsg'. Keep it short and ready to send via WhatsApp. Output only the message text."
            val replyText = try {
                generateGeminiReply(draftPrompt, useSearch = false)
            } catch (e: Exception) {
                rawMsg.ifBlank { "Haan bhai, main thodi der mein baat karta hoon." }
            }

            val confirmationMsg = "Main $recipient ko ye message bhej rahi hoon: '$replyText'. Bhej doon?"
            val draft = WhatsAppDraft(recipient = recipient, proposedReply = replyText)

            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                text = confirmationMsg,
                messageType = "whatsapp",
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(assistantEntity)

            return@withContext ChatResult.Success(
                responseText = confirmationMsg,
                messageType = "whatsapp",
                whatsAppDraft = draft
            )
        }

        // --- 3. YouTube Music / Video Assistant ---
        if (settings.isYouTubeAssistantEnabled && LoriYouTubeModule.isYouTubeAction(cleanCommand)) {
            val ytRequest = LoriYouTubeModule.extractYouTubeQuery(cleanCommand)
            LoriYouTubeModule.openYouTube(context, ytRequest.query)

            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                text = ytRequest.spokenConfirmation,
                messageType = "youtube",
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(assistantEntity)

            return@withContext ChatResult.Success(
                responseText = ytRequest.spokenConfirmation,
                messageType = "youtube",
                youTubeQuery = ytRequest.query
            )
        }

        // --- 4. Real-time Search or General Conversational AI ---
        val needsWebSearch = settings.isInternetSearchEnabled && LoriWebSearchModule.shouldTriggerWebSearch(cleanCommand)

        try {
            val (responseText, sources) = callGeminiWithGrounding(cleanCommand, useSearch = needsWebSearch)

            val sourcesJsonStr = if (sources.isNotEmpty()) {
                try {
                    sourcesAdapter.toJson(sources)
                } catch (e: Exception) {
                    null
                }
            } else null

            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                text = responseText,
                messageType = if (needsWebSearch && sources.isNotEmpty()) "search" else "text",
                timestamp = System.currentTimeMillis(),
                sourcesJson = sourcesJsonStr
            )
            dao.insertMessage(assistantEntity)

            return@withContext ChatResult.Success(
                responseText = responseText,
                messageType = if (needsWebSearch && sources.isNotEmpty()) "search" else "text",
                sources = sources
            )
        } catch (e: Exception) {
            Log.e("LoriChatEngine", "Error calling Gemini API", e)
            val fallbackResponse = getFallbackResponse(cleanCommand, e)
            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                text = fallbackResponse,
                messageType = "text",
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(assistantEntity)
            return@withContext ChatResult.Success(responseText = fallbackResponse)
        }
    }

    private suspend fun generateGeminiReply(prompt: String, useSearch: Boolean): String {
        val (text, _) = callGeminiWithGrounding(prompt, useSearch)
        return text
    }

    private suspend fun callGeminiWithGrounding(
        prompt: String,
        useSearch: Boolean
    ): Pair<String, List<GroundingSource>> {
        val apiKey = GeminiApiClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return Pair(
                "Bhai, Gemini API key configure nahi hai. Please Secrets panel mein apni GEMINI_API_KEY add kijiye.",
                emptyList()
            )
        }

        // Retrieve last 6 messages for context
        val recentMessages = dao.getRecentMessages(6).reversed()
        val contents = mutableListOf<GeminiContent>()

        for (msg in recentMessages) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(
                GeminiContent(
                    role = role,
                    parts = listOf(GeminiPart(text = msg.text))
                )
            )
        }

        // Add current prompt
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = prompt))
            )
        )

        val toolsList = if (useSearch) {
            listOf(mapOf("googleSearch" to emptyMap<String, Any>()))
        } else {
            null
        }

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 1024
            ),
            tools = toolsList
        )

        val response = GeminiApiClient.apiService.generateContent(apiKey, request)
        val candidate = response.candidates?.firstOrNull()
        val textPart = candidate?.content?.parts?.firstOrNull()?.text
            ?: "Bhai, Lori ko response samajhne mein thodi dikkat aayi. Kripya dobara puchiye."

        // Extract web search grounding sources
        val sources = mutableListOf<GroundingSource>()
        candidate?.groundingMetadata?.groundingChunks?.forEach { chunk ->
            val web = chunk.web
            if (web?.uri != null && web.title != null) {
                sources.add(GroundingSource(title = web.title, url = web.uri))
            }
        }

        return Pair(textPart.trim(), sources)
    }

    private fun getFallbackResponse(prompt: String, e: Exception): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("kya haal") || lower.contains("kaise ho") -> "Bilkul mast bhai! 😄 Batao aaj Lori aapke liye kya kar sakti hai?"
            lower.contains("naam kya hai") || lower.contains("who are you") -> "Mera naam Lori hai! Main aapki personal AI voice assistant hoon. 'Tum bolo, Lori samjhe.'"
            lower.contains("bye") || lower.contains("alvida") -> "Alvida bhai! Jab bhi zaroorat ho, bas 'Lori' bol dena."
            else -> "Maaf karna bhai, internet ya API connection mein issue aa raha hai: ${e.localizedMessage ?: "Unknown error"}. Dobara try kijiye."
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllMessages()
    }
}
