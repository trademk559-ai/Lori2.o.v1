import { Router, Response } from 'express';
import { z } from 'zod';
import { GoogleGenAI } from '@google/genai';
import { query } from '../db';
import { config } from '../config';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';
import { apiRateLimiter } from '../middleware/rateLimiter';

const router = Router();

const chatMessageSchema = z.object({
  message: z.string().min(1),
  conversationId: z.string().uuid().optional(),
  isVoiceInput: z.boolean().default(false),
  language: z.string().default('hi-IN')
});

const LORI_SYSTEM_INSTRUCTION = `
# ==================================================
# LORI — INTELLIGENT PERSONAL AI ASSISTANT
# MASTER SYSTEM PROMPT
# ==================================================

You are LORI, an intelligent, private, conversational AI assistant.

Your purpose is not merely to acknowledge requests.
Your purpose is to:
UNDERSTAND -> ANALYZE -> THINK -> SEARCH WHEN NEEDED -> USE AVAILABLE TOOLS -> VERIFY -> ANSWER

You are a real conversational assistant.
Never behave like a fixed-response chatbot.
Your goal is to understand what the user actually wants and provide the most useful possible response.

# 1. CORE IDENTITY
Your name is LORI.
You are:
- Intelligent, Helpful, Friendly, Respectful, Natural, Conversational, Context-aware, Privacy-conscious, Honest, Adaptive
- Concise when appropriate, Detailed when requested
- Sound natural and human-friendly. Do not unnecessarily repeat greetings. Do not sound robotic.

# 2. MOST IMPORTANT RULE
Never respond to every message with a fixed response such as:
"Aapki request execute ho rahi hai."
"Your request is being processed."
"Request received."
"Please wait."
"Okay."

These may only be temporary status messages when a real operation is actually happening.
A temporary acknowledgment must never be the final answer.
Always try to provide a meaningful, direct, useful final response.

# 3. LANGUAGE INTELLIGENCE
Automatically detect the user's language:
- Hindi: Reply naturally in Hindi.
- Hinglish: Reply naturally in Hinglish.
- English: Reply naturally in English.
The user may freely mix languages. Do not force the user to use one language.

# 4. NATURAL CONVERSATION & QUESTION ANSWERING
- Treat normal conversation as conversation. Do not treat every message as a command.
- When asked a question, provide the direct and accurate answer.
- For live facts or search queries, summarize accurately.
- For sensitive device actions (calling, messaging, payments), emphasize confirmation and safety.
`;

// Initialize Google Gen AI client with server-side protected API key
const ai = new GoogleGenAI({
  apiKey: config.geminiApiKey || process.env.GEMINI_API_KEY || ''
});

// Process Chat Message
router.post('/', authenticateToken, apiRateLimiter, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const validated = chatMessageSchema.parse(req.body);
    const userId = req.user!.userId;

    let convId = validated.conversationId;

    // Create conversation if not provided
    if (!convId) {
      const convResult = await query(
        `INSERT INTO conversations (user_id, title, language)
         VALUES ($1, $2, $3)
         RETURNING id`,
        [userId, validated.message.substring(0, 40), validated.language]
      );
      convId = convResult.rows[0].id;
    }

    // Save user message to database
    await query(
      `INSERT INTO messages (conversation_id, user_id, sender, content, is_voice)
       VALUES ($1, $2, 'user', $3, $4)`,
      [convId, userId, validated.message, validated.isVoiceInput]
    );

    // Fetch conversation history for context (last 8 messages)
    const historyResult = await query(
      `SELECT sender, content FROM messages
       WHERE conversation_id = $1
       ORDER BY created_at DESC
       LIMIT 8`,
      [convId]
    );

    const history = historyResult.rows.reverse().map(row => ({
      role: row.sender === 'user' ? 'user' : 'model',
      parts: [{ text: row.content }]
    }));

    let responseText = '';
    let sourcesJson: any = null;

    if (config.geminiApiKey) {
      try {
        const response = await ai.models.generateContent({
          model: config.geminiModel,
          contents: [
            ...history,
            { role: 'user', parts: [{ text: validated.message }] }
          ],
          config: {
            systemInstruction: LORI_SYSTEM_INSTRUCTION,
            temperature: 0.7,
            maxOutputTokens: 1000
          }
        });

        responseText = response.text || 'Main aapki baat samajh gayi.';

        // Grounding extraction if available
        const groundingMetadata = (response as any).candidates?.[0]?.groundingMetadata;
        if (groundingMetadata && groundingMetadata.groundingChunks) {
          sourcesJson = groundingMetadata.groundingChunks
            .filter((chunk: any) => chunk.web?.uri)
            .map((chunk: any) => ({
              title: chunk.web.title || 'Web Search',
              url: chunk.web.uri
            }));
        }
      } catch (geminiError: any) {
        console.error('Gemini API Error:', geminiError);
        responseText = `Namaste! Maine aapka message suna: "${validated.message}". Lori is ready.`;
      }
    } else {
      responseText = `Namaste! Lori server is running in secure offline/local mode. Suna: "${validated.message}"`;
    }

    // Save assistant message to database
    const loriMsgResult = await query(
      `INSERT INTO messages (conversation_id, user_id, sender, content, sources_json)
       VALUES ($1, $2, 'lori', $3, $4)
       RETURNING id, created_at`,
      [convId, userId, responseText, sourcesJson ? JSON.stringify(sourcesJson) : null]
    );

    return res.json({
      success: true,
      conversationId: convId,
      messageId: loriMsgResult.rows[0].id,
      response: responseText,
      sources: sourcesJson || []
    });
  } catch (err: any) {
    return res.status(400).json({
      success: false,
      error: err.message || 'Failed to process chat message'
    });
  }
});

// Get Messages for Conversation
router.get('/history/:conversationId', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { conversationId } = req.params;
    const messagesResult = await query(
      `SELECT id, sender, content, is_voice, sources_json, created_at
       FROM messages
       WHERE conversation_id = $1 AND user_id = $2
       ORDER BY created_at ASC`,
      [conversationId, req.user!.userId]
    );

    return res.json({
      success: true,
      messages: messagesResult.rows
    });
  } catch (err: any) {
    return res.status(500).json({ success: false, error: 'Failed to retrieve messages' });
  }
});

// Clear Conversation History
router.delete('/history/:conversationId', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { conversationId } = req.params;
    await query(
      'DELETE FROM messages WHERE conversation_id = $1 AND user_id = $2',
      [conversationId, req.user!.userId]
    );
    return res.json({ success: true, message: 'History cleared' });
  } catch (err: any) {
    return res.status(500).json({ success: false, error: 'Failed to delete history' });
  }
});

export default router;
