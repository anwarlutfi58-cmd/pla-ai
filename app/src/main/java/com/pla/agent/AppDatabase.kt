package com.pla.agent

import androidx.room.*

@Entity(tableName = "ai_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_topics")
data class TopicEntity(
    @PrimaryKey val name: String,
    val isActive: Boolean = false
)

// TABEL BARU: Untuk menyimpan riwayat chat
@Entity(tableName = "chat_history")
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "User" atau "AI"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AppDao {
    // Fungsi Memori AI
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("SELECT * FROM ai_memory WHERE topic = :topic ORDER BY timestamp DESC LIMIT 10")
    suspend fun getMemoriesByTopic(topic: String): List<MemoryEntity>

    // Fungsi Topik
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Query("UPDATE user_topics SET isActive = 0")
    suspend fun clearActiveTopics()

    @Query("UPDATE user_topics SET isActive = 1 WHERE name = :topicName")
    suspend fun setActiveTopic(topicName: String)
    
    @Query("SELECT * FROM user_topics WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTopic(): TopicEntity?

    // Fungsi Riwayat Chat
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistoryEntity)

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    suspend fun getAllChats(): List<ChatHistoryEntity>

    @Query("SELECT * FROM chat_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentChats(): List<ChatHistoryEntity>

    @Query("DELETE FROM chat_history")
    suspend fun clearAllChats()
}

@Database(entities = [MemoryEntity::class, TopicEntity::class, ChatHistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}