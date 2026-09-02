package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" or "assistant" or "system"
    val text: String,
    val messageType: String = "text", // "text", "voice", "search", "whatsapp", "call", "youtube", "notification"
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesJson: String? = null, // JSON string of sources/citations if web search was used
    val actionDataJson: String? = null, // Additional structured action metadata
    val isSpoken: Boolean = false
)
