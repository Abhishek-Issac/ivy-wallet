package com.ivy.aiassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun findById(id: String): AiConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: AiConversationEntity)

    @Query("UPDATE ai_conversations SET updatedAt = :updatedAt, totalTokens = :totalTokens WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long, totalTokens: Int)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ai_conversations")
    suspend fun clear()
}
