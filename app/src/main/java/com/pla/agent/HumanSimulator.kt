package com.pla.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

object HumanSimulator {

    // Klik dengan jeda manusia (1.5 - 4 detik)
    fun clickNode(service: AccessibilityService, node: AccessibilityNodeInfo, scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            delay(Random.nextLong(1500, 4000))
            
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val path = Path()
            path.moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            
            val stroke = GestureDescription.StrokeDescription(path, 0, Random.nextLong(100, 200))
            val builder = GestureDescription.Builder().addStroke(stroke)
            
            service.dispatchGesture(builder.build(), null, null)
        }
    }

    // Ketik huruf per huruf dengan jeda acak
    fun typeTextHuman(service: AccessibilityService, node: AccessibilityNodeInfo, text: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            delay(Random.nextLong(1000, 2500)) // Jeda sebelum mulai ngetik
            val currentText = StringBuilder()
            
            for (char in text) {
                currentText.append(char)
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentText.toString())
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                
                // Jeda antar huruf (50ms - 200ms)
                delay(Random.nextLong(50, 200))
            }
        }
    }
}