package com.pla.agent

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pla_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AI_NAME = "ai_name"
        private const val DEFAULT_AI_NAME = "PLA Agent"
        
        // Kunci Baru untuk Fitur Hardware Trigger
        private const val KEY_HARDWARE_TRIGGER = "hardware_trigger_enabled"
    }

    fun getAiName(): String {
        return prefs.getString(KEY_AI_NAME, DEFAULT_AI_NAME) ?: DEFAULT_AI_NAME
    }

    fun setAiName(name: String) {
        prefs.edit().putString(KEY_AI_NAME, name).apply()
    }

    // === METODE BARU UNTUK FITUR HARDWARE TRIGGER ===
    
    fun isHardwareTriggerEnabled(): Boolean {
        return prefs.getBoolean(KEY_HARDWARE_TRIGGER, false)
    }

    fun setHardwareTriggerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HARDWARE_TRIGGER, enabled).apply()
    }
}