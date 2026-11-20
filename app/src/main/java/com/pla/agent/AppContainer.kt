package com.pla.agent

import android.app.Application
import androidx.room.Room
import android.util.Log

class AppContainer : Application() {

    lateinit var aiEngine: PlAiEngine
    lateinit var database: AppDatabase
    lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Inisialisasi Preferences
            preferences = AppPreferences(this)

            // Inisialisasi Database Room
            // destroy() memastikan tidak ada bentrok file database lama
            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "pla-agent-db"
            ).fallbackToDestructiveMigration().build()

            // Inisialisasi Mesin AI
            aiEngine = PlAiEngine(this, database.appDao())
            
        } catch (e: Exception) {
            Log.e("AppContainer", "Fatal Error saat inisialisasi: ${e.message}")
            e.printStackTrace()
        }
    }
}