package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        NotificationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LoriDatabase : RoomDatabase() {
    abstract fun loriDao(): LoriDao

    companion object {
        @Volatile
        private var INSTANCE: LoriDatabase? = null

        fun getDatabase(context: Context): LoriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LoriDatabase::class.java,
                    "lori_assistant_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
