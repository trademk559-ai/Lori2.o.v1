package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoriDao {

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    // --- Notifications Log ---
    @Query("SELECT * FROM notifications_log ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notifications_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotifications(limit: Int): List<NotificationLogEntity>

    @Query("SELECT * FROM notifications_log WHERE isRead = 0 ORDER BY timestamp DESC")
    suspend fun getUnreadNotifications(): List<NotificationLogEntity>

    @Query("SELECT * FROM notifications_log WHERE packageName LIKE '%' || :query || '%' OR appName LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestNotificationForApp(query: String): NotificationLogEntity?

    @Query("SELECT * FROM notifications_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestNotification(): NotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationLogEntity): Long

    @Update
    suspend fun updateNotification(notification: NotificationLogEntity)

    @Query("UPDATE notifications_log SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM notifications_log")
    suspend fun clearAllNotifications()
}
