package com.ivy.aiassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AiConversationEntity::class,
        AiMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AiAssistantDatabase : RoomDatabase() {
    abstract fun conversationDao(): AiConversationDao
    abstract fun messageDao(): AiMessageDao

    companion object {
        const val DB_NAME = "ivy_ai_assistant.db"

        fun create(context: Context): AiAssistantDatabase =
            Room.databaseBuilder(context, AiAssistantDatabase::class.java, DB_NAME).build()
    }
}
