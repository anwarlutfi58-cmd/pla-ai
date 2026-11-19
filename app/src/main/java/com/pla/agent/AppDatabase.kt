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

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("SELECT * FROM ai_memory WHERE topic = :topic ORDER BY timestamp DESC LIMIT 10")
    suspend fun getMemoriesByTopic(topic: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Query("UPDATE user_topics SET isActive = 0")
    suspend fun clearActiveTopics()

    @Query("UPDATE user_topics SET isActive = 1 WHERE name = :topicName")
    suspend fun setActiveTopic(topicName: String)
    
    @Query("SELECT * FROM user_topics WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTopic(): TopicEntity?
}

@Database(entities = [MemoryEntity::class, TopicEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}