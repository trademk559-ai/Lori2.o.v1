package com.example.modules.notifications

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.LoriDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.prefs.LoriPreferences
import com.example.modules.voice.LoriVoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoriNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        // Skip Lori's own persistent notifications or system status bar noises
        if (packageName == applicationContext.packageName) return
        if (sbn.isOngoing) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        if (title.isBlank() && text.isBlank()) return

        val pm = applicationContext.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val prefs = LoriPreferences.getInstance(applicationContext).settings.value

        // Check if app is in excluded list
        if (prefs.excludedApps.contains(packageName) || prefs.excludedApps.contains(appName)) {
            return
        }

        val entity = NotificationLogEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            timestamp = System.currentTimeMillis(),
            isSpokenAloud = false,
            isRead = false
        )

        scope.launch {
            try {
                val db = LoriDatabase.getDatabase(applicationContext)
                db.loriDao().insertNotification(entity)

                // Voice Alert if enabled and not in quiet mode
                if (prefs.isVoiceNotificationAlertsEnabled && !prefs.isQuietMode) {
                    val alertSpeech = LoriNotificationManager.formatVoiceNotification(
                        appName = appName,
                        senderName = title,
                        messageText = text,
                        readFullContent = prefs.isReadFullNotificationContent,
                        readAppName = prefs.isReadAppName,
                        readSenderName = prefs.isReadSenderName
                    )

                    LoriVoiceEngine.getInstance(applicationContext).speak(
                        alertSpeech,
                        prefs.ttsSpeechRate,
                        prefs.ttsSpeechPitch
                    )
                }
            } catch (e: Exception) {
                Log.e("LoriNotificationService", "Error saving/announcing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification dismissed by user
    }
}
