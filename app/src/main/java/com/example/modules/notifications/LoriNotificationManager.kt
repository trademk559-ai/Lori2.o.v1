package com.example.modules.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.example.data.local.LoriDatabase
import com.example.data.local.NotificationLogEntity

object LoriNotificationManager {

    /**
     * Checks if Lori's Notification Listener is enabled in system settings
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Opens system settings screen to enable notification listener
     */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Formats natural Hindi notification announcement
     */
    fun formatVoiceNotification(
        appName: String,
        senderName: String,
        messageText: String,
        readFullContent: Boolean,
        readAppName: Boolean,
        readSenderName: Boolean
    ): String {
        val appPart = if (readAppName) "$appName par " else ""
        val senderPart = if (readSenderName && senderName.isNotBlank()) "$senderName ka notification aaya hai. " else "ek naya notification aaya hai. "

        return if (readFullContent && messageText.isNotBlank()) {
            val prefix = if (readSenderName && senderName.isNotBlank()) "$senderName ne likha: " else "Message hai: "
            "$appPart$senderPart$prefix'$messageText'"
        } else if (messageText.isBlank()) {
            "$appPart$senderPart lekin message ka content available nahi hai."
        } else {
            "$appPart$senderPart"
        }
    }

    /**
     * Identifies if a prompt is asking to read notifications
     */
    fun isNotificationCommand(prompt: String): Boolean {
        val lower = prompt.lowercase()
        val keywords = listOf(
            "notification", "notifications", "unread", "kisne message kiya",
            "kya message aaya", "padh ke sunao", "padh ke suna", "padho",
            "last notification", "whatsapp message padh", "unread notification"
        )
        return keywords.any { lower.contains(it) }
    }

    /**
     * Handles voice queries for notifications using local Room DB records
     */
    suspend fun handleNotificationVoiceQuery(context: Context, prompt: String): String {
        val db = LoriDatabase.getDatabase(context)
        val dao = db.loriDao()
        val lower = prompt.lowercase()

        return when {
            lower.contains("saare") || lower.contains("all") || lower.contains("unread") -> {
                val unread = dao.getUnreadNotifications()
                if (unread.isEmpty()) {
                    "Bhai, abhi koi naya ya unread notification nahi hai."
                } else {
                    val summary = unread.take(4).joinToString(". ") { n ->
                        "${n.appName} par ${if (n.title.isNotBlank()) n.title else "kisi"} ka message: '${n.text}'"
                    }
                    "Aapke ${unread.size} unread notifications hain: $summary"
                }
            }
            lower.contains("whatsapp") -> {
                val waNotif = dao.getLatestNotificationForApp("WhatsApp")
                if (waNotif != null) {
                    "Latest WhatsApp message: ${waNotif.title} ne bheja hai - '${waNotif.text}'"
                } else {
                    "WhatsApp ka koi recent notification nahi mila bhai."
                }
            }
            else -> {
                val latest = dao.getLatestNotification()
                if (latest != null) {
                    "Last notification ${latest.appName} se aaya hai. ${if (latest.title.isNotBlank()) "${latest.title}: " else ""}'${latest.text}'"
                } else {
                    "Abhi koi notification record nahi mila bhai. Notification Listener permission check kar lijiye."
                }
            }
        }
    }
}
