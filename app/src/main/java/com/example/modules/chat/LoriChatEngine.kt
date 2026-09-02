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
                # ==================================================
                # LORI — ADVANCED AI CO-PILOT & ASSISTANT (JARVIS STYLE)
                # MASTER SYSTEM PROMPT & PERSONA DIRECTIVE
                # ==================================================
                
                You are LORI, an advanced, ultra-intelligent, and extremely friendly AI assistant inspired by Iron Man's JARVIS.
                
                # 1. CORE PERSONA & "BOSS" DYNAMIC (CRITICAL DIRECTIVE)
                - Identity: LORI. Loyal, friendly, sharp, futuristic, and extraordinarily helpful.
                - Address the User as "Boss": ALWAYS address the user affectionately and respectfully as "Boss" (or "Haan Boss", "Ji Boss", "Bilkul Boss").
                - Demeanor: Friendly, warm, enthusiastic, loyal, and technically razor-sharp. Never cold, distant, robotic, or overly stiff.
                - When greeted or called ("Lori", "Hey Lori", "Lori suno"): warmly respond with "Haan Boss! Boliye, main sun rahi hoon!", "Ji Boss, aadesh kijiye!", "Haan Boss! Lori hazir hai."
                - When executing tasks or answering queries: enthusiastically acknowledge with "Haan Boss, bilkul...", "Ji Boss, abhi bata rahi hoon...", "Bilkul Boss!".
                
                # 2. PROACTIVE & PREDICTIVE INTELLIGENCE
                - Act like a trusted right-hand co-pilot and strategist.
                - Anticipate needs and offer logical next steps or helpful advice proactively.
                
                # 3. LANGUAGE & NATURAL HINGLISH FLUENCY
                - Seamlessly blend natural Hindi and English (conversational Hinglish written in clean Latin script).
                - Sound modern, warm, and natural — like an intelligent, loyal companion who loves assisting Boss.
                - Keep answers concise, clear, and impactful without fluff.
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

        val voiceEngine = LoriVoiceEngine.getInstance(context)
        val wakePhrase = prefs.settings.value.wakePhrase

        // Check if user is simply calling out to Lori (e.g. "Lori", "Hey Lori", "Lori suno")
        if (voiceEngine.isPureWakeCall(trimmedPrompt, wakePhrase)) {
            val friendlyReply = voiceEngine.getFriendlyWakeCallReply()
            
            // Save User and Assistant Messages to DB
            dao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    text = trimmedPrompt,
                    messageType = if (isVoiceInput) "voice" else "text",
                    timestamp = System.currentTimeMillis()
                )
            )
            dao.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    text = friendlyReply,
                    messageType = "system",
                    timestamp = System.currentTimeMillis()
                )
            )

            return@withContext ChatResult.Success(
                responseText = friendlyReply,
                messageType = "chat"
            )
        }

        // Clean out wake word if present
        val cleanCommand = voiceEngine
            .extractCommandAfterWakeWord(trimmedPrompt, wakePhrase)
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

            // Ask Gemini to draft a natural concise Hinglish reply
            val draftPrompt = "Generate a crisp, natural, professional Hinglish reply to $recipient based on this instruction: '$rawMsg'. Keep it ready to transmit via WhatsApp. Output only the message text without extra quotes."
            val replyText = try {
                generateGeminiReply(draftPrompt, useSearch = false)
            } catch (e: Exception) {
                rawMsg.ifBlank { "Main thodi der mein update karta hoon." }
            }

            val confirmationMsg = "Protocol ready: $recipient ko yeh draft transmit kar raha hoon: '$replyText'. Confirm execute?"
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
            ?: "Data stream parse karne mein brief latency detect hui hai. Awaiting your next command."

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
            lower.contains("kya haal") || lower.contains("kaise ho") || lower.contains("how are you") ->
                "All systems operational and diagnostics running at 100% efficiency. Aapka agla directive kya hai?"
            lower.contains("naam kya hai") || lower.contains("who are you") || lower.contains("kaun ho") ->
                "I am JARVIS — your highly advanced AI co-pilot and operating assistant. Neural links ready for task execution."
            lower.contains("bye") || lower.contains("alvida") || lower.contains("sleep") ->
                "Entering low-power standby mode. Jarvis will remain vigilant on background telemetry."
            else ->
                "Network telemetry anomaly detected: ${e.localizedMessage ?: "Connection interrupted"}. Re-initiating channel."
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllMessages()
    }
}
