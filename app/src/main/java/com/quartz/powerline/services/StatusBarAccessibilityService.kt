package com.quartz.powerline.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.Toast
import com.quartz.powerline.OverlayActivity
import com.quartz.powerline.data.SettingsRepository
import kotlin.math.abs

class StatusBarAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var settingsRepository: SettingsRepository
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayAdded = false

    // Touch handling
    private var startY = 0f
    private var startX = 0f
    private var isGestureTriggered = false
    private val swipeThreshold = 20f // Pixels
    private var touchSlop = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository = SettingsRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        // Register for settings changes
        getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this)

        updateOverlayState()

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Powerline Service Connected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "status_bar_tap_enabled" || key == "debug_mode") {
            updateOverlayState()
        }
    }

    private fun updateOverlayState() {
        val enabled = settingsRepository.getStatusBarTapEnabled()
        val debug = settingsRepository.getDebugMode()

        if (enabled || debug) {
            // Ensure overlay exists
            if (!isOverlayAdded) {
                createOverlay()
            }
            // Update visual (debug color)
            overlayView?.setBackgroundColor(if (debug) 0x55FF0000 else Color.TRANSPARENT)
        } else {
            // Remove overlay
            removeOverlay()
        }
    }

    private fun createOverlay() {
        if (isOverlayAdded) return

        val statusBarHeight = getStatusBarHeight()
        val height = if (statusBarHeight > 0) statusBarHeight else 60 // Fallback ~24dp

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        val view = FrameLayout(this)
        view.setBackgroundColor(if (settingsRepository.getDebugMode()) 0x55FF0000 else Color.TRANSPARENT)

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startX = event.rawX
                    isGestureTriggered = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isGestureTriggered) {
                        val deltaY = event.rawY - startY
                        val deltaX = event.rawX - startX

                        // Check for vertical swipe down
                        // Allow some horizontal movement, but it should be primarily vertical?
                        // Actually, simplified check: just moving down enough triggers it.
                        if (deltaY > swipeThreshold) {
                            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                            isGestureTriggered = true
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isGestureTriggered) {
                        val deltaY = abs(event.rawY - startY)
                        val deltaX = abs(event.rawX - startX)

                        // Treat as tap if movement is minimal
                        if (deltaY < touchSlop && deltaX < touchSlop) {
                            if (settingsRepository.getStatusBarTapEnabled()) {
                                val intent = Intent(this@StatusBarAccessibilityService, OverlayActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(view, params)
            overlayView = view
            isOverlayAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        if (isOverlayAdded && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
                isOverlayAdded = false
                overlayView = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No longer relying on events for trigger, but keeping service alive
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
    }
}
