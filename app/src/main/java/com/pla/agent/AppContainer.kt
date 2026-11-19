package com.pla.agent

import android.app.Application

class AppContainer : Application() {
    override fun onCreate() {
        super.onCreate()
        // Di sinilah nanti kita akan menginisialisasi Database AI, 
        // Mesin Llama.cpp, dan pengaturan awal saat aplikasi dibuka.
    }
}