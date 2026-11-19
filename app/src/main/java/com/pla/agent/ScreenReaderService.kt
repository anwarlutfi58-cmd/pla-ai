package com.pla.agent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScreenReaderService : AccessibilityService() {

    private val TAG = "ScreenReaderService"
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        JarvisBridge.screenReaderService = this
        Log.d(TAG, "Layanan Jarvis Terhubung")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d(TAG, "Service terhenti")
    }

    // Fungsi untuk mengeksekusi perintah dari FloatingService
    fun executeCommand(command: String) {
        scope.launch {
            val lowerCommand = command.lowercase()
            
            if (lowerCommand.contains("buka")) {
                // Membuka aplikasi berdasarkan nama
                when {
                    lowerCommand.contains("facebook") -> openApp("com.facebook.katana")
                    lowerCommand.contains("tiktok") -> openApp("com.zhiliaoapp.musically")
                    lowerCommand.contains("whatsapp") -> openApp("com.whatsapp")
                    lowerCommand.contains("youtube") -> openApp("com.google.android.youtube")
                    else -> Log.d(TAG, "Aplikasi tidak dikenali")
                }
            } 
            else if (lowerCommand.contains("buat status")) {
                // Logika buat status (seperti sebelumnya)
                val statusButton = findNodeByText("What's on your mind") 
                    ?: findNodeByText("Buat status")
                    ?: findNodeByText("Apa yang Anda pikirkan")

                if (statusButton != null) {
                    statusButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    delay(3000)
                    
                    val inputField = findEditText()
                    if (inputField != null) {
                        val statusText = command.substringAfter("status").trim().replace("'", "").replace("\"", "")
                        HumanSimulator.typeTextHuman(this@ScreenReaderService, inputField, statusText, scope)
                        delay(3000)
                        
                        val postButton = findNodeByText("Post") ?: findNodeByText("Kirim")
                        if (postButton != null) {
                            HumanSimulator.clickNode(this@ScreenReaderService, postButton, scope)
                        }
                    }
                }
            }
        }
    }

    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
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