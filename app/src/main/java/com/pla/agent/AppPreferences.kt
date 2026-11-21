package com.pla.agent

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pla_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AI_NAME = "ai_name"
        private const val DEFAULT_AI_NAME = "PLA Agent"
        private const val KEY_HARDWARE_TRIGGER = "hardware_trigger_enabled"
        private const val KEY_AI_AVATAR = "ai_avatar_id"
        private const val DEFAULT_AVATAR_ID = R.drawable.avatar_umum
        private const val KEY_TTS_ENABLED = "tts_enabled"
        
        // BARU: Toggle Privasi Passive Learning
        private const val KEY_VOICE_LEARNING = "voice_learning_enabled"
    }

    fun getAiName(): String = prefs.getString(KEY_AI_NAME, DEFAULT_AI_NAME) ?: DEFAULT_AI_NAME
    fun setAiName(name: String) = prefs.edit().putString(KEY_AI_NAME, name).apply()

    fun isHardwareTriggerEnabled(): Boolean = prefs.getBoolean(KEY_HARDWARE_TRIGGER, false)
    fun setHardwareTriggerEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HARDWARE_TRIGGER, enabled).apply()

    fun getAiAvatar(): Int = prefs.getInt(KEY_AI_AVATAR, DEFAULT_AVATAR_ID)
    fun setAiAvatar(resId: Int) = prefs.edit().putInt(KEY_AI_AVATAR, resId).apply()

    fun isTtsEnabled(): Boolean = prefs.getBoolean(KEY_TTS_ENABLED, true)
    fun setTtsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()

    // === METODE BARU UNTUK PASSIVE LEARNING ===
    fun isVoiceLearningEnabled(): Boolean = prefs.getBoolean(KEY_VOICE_LEARNING, false)
    fun setVoiceLearningEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VOICE_LEARNING, enabled).apply()
}