package com.pla.agent

import androidx.room.*

@Entity(tableName = "ai_versions")
data class VersionEntity(
    @PrimaryKey val versionName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActiveForChat: Boolean = false
)

@Entity(tableName = "ai_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val versionName: String,
    val sourceUrl: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_history")
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AppDao {
    // Fungsi Versi
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVersion(version: VersionEntity)

    @Query("SELECT * FROM ai_versions ORDER BY createdAt ASC")
    suspend fun getAllVersions(): List<VersionEntity>

    @Query("UPDATE ai_versions SET isActiveForChat = 0")
    suspend fun clearActiveVersion()

    @Query("UPDATE ai_versions SET isActiveForChat = 1 WHERE versionName = :versionName")
    suspend fun setActiveVersion(versionName: String)

    @Query("SELECT * FROM ai_versions WHERE isActiveForChat = 1 LIMIT 1")
    suspend fun getActiveVersion(): VersionEntity?

    // Fungsi Memori
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("SELECT * FROM ai_memory WHERE versionName = :versionName ORDER BY timestamp DESC")
    suspend fun getMemoriesByVersion(versionName: String): List<MemoryEntity>

    @Query("SELECT DISTINCT sourceUrl FROM ai_memory WHERE versionName = :versionName")
    suspend fun getSourcesByVersion(versionName: String): List<String>

    @Query("DELETE FROM ai_memory WHERE versionName = :versionName AND sourceUrl = :sourceUrl")
    suspend fun deleteMemoriesBySource(versionName: String, sourceUrl: String)

    // Fungsi Chat
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistoryEntity)

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    suspend fun getAllChats(): List<ChatHistoryEntity>

    @Query("SELECT * FROM chat_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentChats(): List<ChatHistoryEntity>

    @Query("DELETE FROM chat_history")
    suspend fun clearAllChats()
}

@Database(entities = [MemoryEntity::class, VersionEntity::class, ChatHistoryEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}