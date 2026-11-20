package com.pla.agent

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoTyperAccessibility : AccessibilityService() {

    companion object {
        var instance: AutoTyperAccessibility? = null
        
        fun typeText(text: String): Boolean {
            val service = instance ?: return false
            return service.performType(text)
        }
        
        fun isReady(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu proses event, hanya standby untuk perintah ketik
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun performType(text: String): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            
            // Cari node yang sedang fokus (kolom teks aktif)
            val focusedNode = findFocusedEditableNode(rootNode)
            
            if (focusedNode != null) {
                val args = Bundle()
                args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                    text
                )
                val success = focusedNode.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT, 
                    args
                )
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

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Cek apakah node ini editable dan focused
        if (node.isEditable && node.isFocused) {
            return node
        }
        
        // Cari child yang focused & editable
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