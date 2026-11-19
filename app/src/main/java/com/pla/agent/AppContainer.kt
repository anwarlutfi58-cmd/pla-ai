package com.pla.agent

import android.app.Application
import androidx.room.Room

class AppContainer : Application() {

    lateinit var aiEngine: PlAiEngine
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Inisialisasi Preferences
        preferences = AppPreferences(this)

        // Inisialisasi Database Room
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pla-agent-db"
        ).fallbackToDestructiveMigration().build()

        // Inisialisasi Mesin AI dengan Database
        aiEngine = PlAiEngine(this, database.appDao())
    }
}