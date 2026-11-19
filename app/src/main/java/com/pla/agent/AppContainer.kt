package com.pla.agent

import android.app.Application
import androidx.room.Room
import android.util.Log

class AppContainer : Application() {

    lateinit var aiEngine: PlAiEngine
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Inisialisasi Preferences
            preferences = AppPreferences(this)

            // Inisialisasi Database Room
            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "pla-agent-db"
            ).fallbackToDestructiveMigration().build()

            // Inisialisasi Mesin AI
            aiEngine = PlAiEngine(this, database.appDao())
            
        } catch (e: Exception) {
            Log.e("AppContainer", "Gagal inisialisasi: ${e.message}")
        }
    }
}