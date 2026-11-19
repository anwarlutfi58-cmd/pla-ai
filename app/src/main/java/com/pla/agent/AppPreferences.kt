package com.pla.agent

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pla_prefs", Context.MODE_PRIVATE)

    fun setInternetEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("internet_enabled", enabled).apply()
    }

    fun isInternetEnabled(): Boolean {
        return prefs.getBoolean("internet_enabled", false)
    }
}