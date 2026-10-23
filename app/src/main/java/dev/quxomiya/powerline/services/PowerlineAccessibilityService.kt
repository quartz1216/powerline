package dev.quxomiya.powerline.services

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.quxomiya.powerline.overlay.SearchOverlay

class PowerlineAccessibilityService : AccessibilityService() {

    private var overlay: SearchOverlay? = null
    private var lastToggleAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = SearchOverlay(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Close when another app comes to the front (Home, Recents, app switch)
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val current = overlay ?: return
        if (!current.isShowing) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == SYSTEM_UI_PACKAGE || pkg == currentImePackage()) return
        current.hide()
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
                    overlay?.toggle(triggeredAt = event.eventTime)
                }
            }
            // Consume DOWN/UP/repeat so Space is not passed to other apps
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        overlay?.destroy()
        overlay = null
        super.onDestroy()
    }

    private fun currentImePackage(): String? {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return null
        return id.substringBefore('/')
    }

    companion object {
        private const val TOGGLE_DEBOUNCE_MS = 300L
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
