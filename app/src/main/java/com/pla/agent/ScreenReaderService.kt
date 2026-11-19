package com.pla.agent

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenReaderService : AccessibilityService() {

    private val TAG = "ScreenReaderService"
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        JarvisBridge.screenReaderService = this
        Log.d(TAG, "Layanan Jarvis Terhubung")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Logika mendengarkan notifikasi Facebook/WA bisa ditaruh di sini
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service terhenti")
    }

    // Fungsi untuk mengeksekusi perintah dari FloatingService
    fun executeCommand(command: String) {
        scope.launch {
            // Logika sederhana: Jika disuruh buat status
            if (command.contains("buat status", ignoreCase = true)) {
                // Cari tombol "What's on your mind" atau "Buat status"
                val statusButton = findNodeByText("What's on your mind") 
                    ?: findNodeByText("Buat status")
                    ?: findNodeByText("Apa yang Anda pikirkan")

                if (statusButton != null) {
                    // Klik tombol status
                    statusButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    
                    // Tunggu UI terbuka (3 detik)
                    kotlinx.coroutines.delay(3000)
                    
                    // Cari kolom ketik
                    val inputField = findEditText()
                    if (inputField != null) {
                        // Ketik teks (mengambil teks setelah kata 'status')
                        val statusText = command.substringAfter("status").trim().replace("'", "").replace("\"", "")
                        HumanSimulator.typeTextHuman(this@ScreenReaderService, inputField, statusText, scope)
                        
                        // Tunggu selesai ngetik (asumsi 3 detik)
                        kotlinx.coroutines.delay(3000)
                        
                        // Cari tombol Post
                        val postButton = findNodeByText("Post") ?: findNodeByText("Kirim")
                        if (postButton != null) {
                            HumanSimulator.clickNode(this@ScreenReaderService, postButton, scope)
                        }
                    }
                }
            }
        }
    }

    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull { it.isClickable || it.parent != null }
    }

    private fun findEditText(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findEditTextRecursive(root)
    }

    private fun findEditTextRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditTextRecursive(child)
            if (result != null) return result
        }
        return null
    }
}