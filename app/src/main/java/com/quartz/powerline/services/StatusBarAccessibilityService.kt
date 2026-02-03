package com.quartz.powerline.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import com.quartz.powerline.OverlayActivity
import com.quartz.powerline.data.SettingsRepository

class StatusBarAccessibilityService : AccessibilityService() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!settingsRepository.isStatusBarTapEnabled()) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val source = event.source ?: return
            if (source.packageName == "com.android.systemui") {
                val rect = Rect()
                source.getBoundsInScreen(rect)

                // Check if the click is at the top of the screen (status bar area)
                // Typically rect.top is 0 for status bar.
                // We also check if it's within a reasonable height.
                if (rect.top == 0 && rect.bottom > 0 && rect.bottom < 300) {
                    val intent = Intent(this, OverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(intent)
                }
            }
            source.recycle()
        }
    }

    override fun onInterrupt() {
        // Required method
    }
}
