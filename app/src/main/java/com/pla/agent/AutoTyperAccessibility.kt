package com.pla.agent

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoTyperAccessibility : AccessibilityService() {

    companion object {
        var instance: AutoTyperAccessibility? = null
        
        fun typeText(text: String): Boolean {
            val service = instance ?: return false
            return service.performType(text)
        }
        
        fun isReady(): Boolean = instance != null
    }

    private var db: AppDatabase? = null
    private var prefs: AppPreferences? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Inisialisasi DB & Preferences saat service aktif
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "pla_database")
            .fallbackToDestructiveMigration() 
            .build()
        prefs = AppPreferences(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hanya standby, tidak memproses event secara massal demi performa A06
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        db?.close()
    }

    private fun performType(text: String): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            val focusedNode = findFocusedEditableNode(rootNode)
            
            if (focusedNode != null) {
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                        text
                    )
                }
                val success = focusedNode.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT, 
                    args
                )
                
                // === FITUR BARU: PASSIVE LEARNING DARI SUARA ===
                if (success && prefs?.isVoiceLearningEnabled() == true) {
                    saveVoiceInteraction(text)
                }
                
                focusedNode.recycle()
                return success
            }
            
            rootNode.recycle()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun saveVoiceInteraction(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = db?.appDao() ?: return@launch
                // Simpan dengan format standar dataset AI agar langsung bisa diproses engine
                val memoryContent = """{"pertanyaan": "$text", "jawaban": "", "topik": "voice_interaction", "kategori": "user_input"}"""
                
                // Cek duplikat sederhana sebelum insert
                val existing = dao.getMemoriesByVersion("V1. pla")
                    .any { it.content.contains(text.take(30)) }
                    
                if (!existing) {
                    dao.insertMemory(
                        MemoryEntity(
                            versionName = "V1. pla",
                            sourceUrl = "voice_overlay",
                            content = memoryContent,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    // Trigger incremental learning di Engine (opsional, bisa via broadcast)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedEditableNode(child)
            if (result != null) {
                if (result !== child) child.recycle()
                return result
            }
            child.recycle()
        }
        return null
    }
}