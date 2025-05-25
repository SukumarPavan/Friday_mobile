package com.example.friday

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FridayAccessibilityService : AccessibilityService() {
    companion object {
        var instance: FridayAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional: React to window changes, etc.
    }

    override fun onInterrupt() {
        // Required override
    }

    fun performGlobalActionByName(action: String) {
        when (action) {
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "recent" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "close_app" -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    fun performSwipe(direction: String) {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val path = Path()
        when (direction) {
            "right" -> { path.moveTo(width * 0.1f, height / 2f); path.lineTo(width * 0.9f, height / 2f) }
            "left" -> { path.moveTo(width * 0.9f, height / 2f); path.lineTo(width * 0.1f, height / 2f) }
            "up" -> { path.moveTo(width / 2f, height * 0.9f); path.lineTo(width / 2f, height * 0.1f) }
            "down" -> { path.moveTo(width / 2f, height * 0.1f); path.lineTo(width / 2f, height * 0.9f) }
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun clickSendButton() {
        val rootNode = rootInActiveWindow ?: return
        val sendNodes = rootNode.findAccessibilityNodeInfosByText("Send")
        for (node in sendNodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
        }
    }
} 