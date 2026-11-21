package com.pla.agent

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pla_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AI_NAME = "ai_name"
        private const val DEFAULT_AI_NAME = "PLA Agent"
        
        // Kunci untuk Fitur Hardware Trigger
        private const val KEY_HARDWARE_TRIGGER = "hardware_trigger_enabled"
        
        // Kunci untuk Avatar AI (disimpan sebagai resource ID integer)
        private const val KEY_AI_AVATAR = "ai_avatar_id"
        private const val DEFAULT_AVATAR_ID = R.drawable.avatar_umum
        
        // Kunci untuk TTS
        private const val KEY_TTS_ENABLED = "tts_enabled"
    }

    // === 1. NAMA AI ===
    fun getAiName(): String {
        return prefs.getString(KEY_AI_NAME, DEFAULT_AI_NAME) ?: DEFAULT_AI_NAME
    }

    fun setAiName(name: String) {
        prefs.edit().putString(KEY_AI_NAME, name).apply()
    }

    // === 2. HARDWARE TRIGGER (Fitur Baru) ===
    fun isHardwareTriggerEnabled(): Boolean {
        return prefs.getBoolean(KEY_HARDWARE_TRIGGER, false)
    }

    fun setHardwareTriggerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HARDWARE_TRIGGER, enabled).apply()
    }

    // === 3. AVATAR AI (Dilengkapi Kembali) ===
    fun getAiAvatar(): Int {
        return prefs.getInt(KEY_AI_AVATAR, DEFAULT_AVATAR_ID)
    }

    fun setAiAvatar(resId: Int) {
        prefs.edit().putInt(KEY_AI_AVATAR, resId).apply()
    }

    // === 4. TTS ENABLED (Dilengkapi Kembali) ===
    fun isTtsEnabled(): Boolean {
        return prefs.getBoolean(KEY_TTS_ENABLED, true) // Default aktif agar TTS jalan
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
    }
}