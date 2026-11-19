package com.pla.agent

import android.app.Application

class AppContainer : Application() {

    // Kita buat satu mesin AI global yang dipakai bersama oleh semua layar
    lateinit var aiEngine: PlAiEngine
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Inisialisasi mesin AI saat aplikasi pertama kali dibuka
        aiEngine = PlAiEngine(this)
    }
}