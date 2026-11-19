package com.pla.agent

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pla_prefs", Context.MODE_PRIVATE)

    // Fungsi Internet
    fun setInternetEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("internet_enabled", enabled).apply()
    }

    fun isInternetEnabled(): Boolean {
        return prefs.getBoolean("internet_enabled", false)
    }

    // Fungsi Nama dan Avatar AI
    fun setAiName(name: String) {
        prefs.edit().putString("ai_name", name).apply()
    }

    fun getAiName(): String {
        return prefs.getString("ai_name", "PLA Agent") ?: "PLA Agent"
    }

    fun setAiAvatar(avatarId: Int) {
        prefs.edit().putInt("ai_avatar", avatarId).apply()
    }

    fun getAiAvatar(): Int {
        return prefs.getInt("ai_avatar", R.drawable.avatar_umum)
    }

    // Fungsi Suara (TTS)
    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("tts_enabled", enabled).apply()
    }

    fun isTtsEnabled(): Boolean {
        return prefs.getBoolean("tts_enabled", true) // Default: Nyala
    }
}