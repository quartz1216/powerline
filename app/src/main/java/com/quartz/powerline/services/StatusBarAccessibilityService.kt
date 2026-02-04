package com.quartz.powerline.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.quartz.powerline.OverlayActivity
import com.quartz.powerline.data.SettingsRepository

class StatusBarAccessibilityService : AccessibilityService() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository = SettingsRepository(this)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Powerline Service Connected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!::settingsRepository.isInitialized) return

        val isDebug = settingsRepository.getDebugMode()

        if (!settingsRepository.getStatusBarTapEnabled() && !isDebug) return

        if (isDebug) {
            val typeName = AccessibilityEvent.eventTypeToString(event.eventType)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Event: $typeName, Pkg: ${event.packageName}", Toast.LENGTH_SHORT).show()
            }
        }

        // Filter for Click events (or Touch Interaction if supported)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {

            val source = event.source ?: return
            try {
                val rect = Rect()
                source.getBoundsInScreen(rect)

                if (isDebug) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Click: ${event.packageName}, top: ${rect.top}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Check if the click is in the top status bar area.
                // Status bar height varies but is generally small. 150px covers most high density screens safe area.
                if (settingsRepository.getStatusBarTapEnabled() && rect.top < 150) {
                    // Launch Overlay
                    val intent = Intent(this, OverlayActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                if (isDebug) {
                     Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                source.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}
