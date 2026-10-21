package com.quartz.powerline.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.quartz.powerline.OverlayActivity

class StatusBarAccessibilityService : AccessibilityService() {

    private var lastToggleAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: this service only filters key events
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SPACE && event.isAltPressed) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val now = SystemClock.uptimeMillis()
                if (now - lastToggleAt >= TOGGLE_DEBOUNCE_MS) {
                    lastToggleAt = now
                    val intent = Intent(this, OverlayActivity::class.java)
                        .setAction(OverlayActivity.ACTION_TOGGLE_SEARCH)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
            // Consume DOWN/UP/repeat so Space is not passed to other apps
            return true
        }
        return super.onKeyEvent(event)
    }

    companion object {
        private const val TOGGLE_DEBOUNCE_MS = 300L
    }
}
