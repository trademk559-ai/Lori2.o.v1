package com.example.modules.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.ChatMessageEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    JSON("json", "application/json", "JSON (.json)"),
    TEXT("txt", "text/plain", "Text File (.txt)")
}

/**
 * ChatExportManager:
 * Exports conversation history to JSON or Plain Text files and facilitates
 * sharing, saving, or copying to clipboard.
 */
object ChatExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateJsonExport(messages: List<ChatMessageEntity>): String {
        val root = JSONObject()
        val now = Date()

        root.put("assistant", "Lori AI Voice Assistant")
        root.put("version", "1.0")
        root.put("exportedAt", dateFormat.format(now))
        root.put("exportedTimestamp", now.time)
        root.put("totalMessages", messages.size)

        val messageArray = JSONArray()
        for (msg in messages) {
            val msgObj = JSONObject()
            msgObj.put("id", msg.id)
            msgObj.put("role", msg.role) // "user", "assistant", "system"
            msgObj.put("sender", if (msg.role == "user") "User" else "Lori")
            msgObj.put("text", msg.text)
            msgObj.put("messageType", msg.messageType)
            msgObj.put("timestamp", msg.timestamp)
            msgObj.put("formattedTime", dateFormat.format(Date(msg.timestamp)))
            
            if (!msg.sourcesJson.isNullOrBlank()) {
                try {
                    msgObj.put("sources", JSONArray(msg.sourcesJson))
                } catch (e: Exception) {
                    msgObj.put("sourcesRaw", msg.sourcesJson)
                }
            }

            if (!msg.actionDataJson.isNullOrBlank()) {
                try {
                    msgObj.put("actionData", JSONObject(msg.actionDataJson))
                } catch (e: Exception) {
                    msgObj.put("actionDataRaw", msg.actionDataJson)
                }
            }

            messageArray.put(msgObj)
        }

        root.put("messages", messageArray)
        return root.toString(2) // 2-space pretty printing
    }

    fun generateTextExport(messages: List<ChatMessageEntity>): String {
        val sb = StringBuilder()
        val now = Date()

        sb.append("====================================================\n")
        sb.append(" Lori Voice Assistant - Chat Conversation Export\n")
        sb.append("====================================================\n")
        sb.append("Exported Date : ${dateFormat.format(now)}\n")
        sb.append("Total Messages: ${messages.size}\n")
        sb.append("====================================================\n\n")

        if (messages.isEmpty()) {
            sb.append("(No messages in conversation history)\n")
            return sb.toString()
        }

        for (msg in messages) {
            val senderLabel = when (msg.role) {
                "user" -> "👤 YOU (User)"
                "assistant" -> "🤖 LORI (Assistant)"
                else -> "⚙️ SYSTEM"
            }
            val timeStr = dateFormat.format(Date(msg.timestamp))

            sb.append("----------------------------------------------------\n")
            sb.append("[$timeStr] $senderLabel [Type: ${msg.messageType}]\n")
            sb.append("----------------------------------------------------\n")
            sb.append(msg.text.trim())
            sb.append("\n\n")
        }

        sb.append("=================== END OF EXPORT ==================\n")
        return sb.toString()
    }

    /**
     * Writes the exported data to a local cache file and launches the Android Share Sheet.
     */
    fun exportAndShare(
        context: Context,
        messages: List<ChatMessageEntity>,
        format: ExportFormat
    ): File? {
        if (messages.isEmpty()) {
            Toast.makeText(context, "No chat history to export.", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val content = when (format) {
                ExportFormat.JSON -> generateJsonExport(messages)
                ExportFormat.TEXT -> generateTextExport(messages)
            }

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val fileName = "lori_chat_${fileTimestampFormat.format(Date())}.${format.extension}"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Lori Chat History Export - ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Exported ${messages.size} chat messages from Lori Assistant.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export & Save Lori Chat (${format.label})").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            return file
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    /**
     * Copies the formatted export string directly to the clipboard.
     */
    fun copyExportToClipboard(
        context: Context,
        messages: List<ChatMessageEntity>,
        format: ExportFormat
    ) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "No chat history to copy.", Toast.LENGTH_SHORT).show()
            return
        }

        val content = when (format) {
            ExportFormat.JSON -> generateJsonExport(messages)
            ExportFormat.TEXT -> generateTextExport(messages)
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Lori Chat Export (${format.extension})", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Chat history copied as ${format.label}!", Toast.LENGTH_SHORT).show()
    }
}
