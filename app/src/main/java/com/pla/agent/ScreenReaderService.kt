package com.pla.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ScreenReaderService : AccessibilityService() {

    private val TAG = "ScreenReaderService"
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Logika untuk membaca layar bisa ditaruh di sini.
        // Contoh: Jika ada notifikasi WA masuk, kita bisa baca.
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service terhenti")
    }

    // ==========================================
    // SIMULASI KLIK MANUSIA (ANTI-BANNED)
    // ==========================================
    fun humanLikeClick(node: AccessibilityNodeInfo) {
        scope.launch {
            // Jeda acak sebelum klik (antara 1.5 detik sampai 4 detik)
            val randomDelay = Random.nextLong(1500, 4000)
            delay(randomDelay)

            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()

            // Efek visual klik
            val path = Path()
            path.moveTo(x, y)

            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, Random.nextLong(100, 200)))
            
            dispatchGesture(gestureBuilder.build(), null, null)
            Log.d(TAG, "Melakukan klik manusia pada koordinat: $x, $y")
        }
    }

    // ==========================================
    // SIMULASI KETIK MANUSIA
    // ==========================================
    fun humanLikeTyping(node: AccessibilityNodeInfo, text: String) {
        scope.launch {
            // Jeda sebelum mulai mengetik
            delay(Random.nextLong(1000, 2500))
            
            // Ketik teks
            val arguments = android.os.Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "Mengetik teks: $text")
        }
    }
    
    // Fungsi bantu untuk mencari tombol berdasarkan teks
    fun findButtonByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull { it.isClickable }
    }
}