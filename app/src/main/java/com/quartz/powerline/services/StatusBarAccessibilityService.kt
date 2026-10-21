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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.FrameLayout
import android.widget.Toast
import com.quartz.powerline.OverlayActivity
import com.quartz.powerline.data.SettingsRepository
import kotlin.math.abs

class StatusBarAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var settingsRepository: SettingsRepository
    private var windowManager: WindowManager? = null

    // Bottom Triggers
    private var leftOverlayView: View? = null
    private var rightOverlayView: View? = null
    private var isOverlaysAdded = false
    private var isKeyboardVisible = false

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

        checkKeyboardVisibility()
        updateOverlayState()

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Powerline Service Connected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "bottom_trigger_enabled" ||
            key == "debug_mode" ||
            key == "bottom_trigger_center_gap_percent" ||
            key == "bottom_trigger_height_dp" ||
            key == "hide_triggers_on_keyboard") {
            updateOverlayState()
        }
    }

    private fun updateOverlayState() {
        val enabled = settingsRepository.getBottomTriggerEnabled()
        val debug = settingsRepository.getDebugMode()
        val hideOnKeyboard = settingsRepository.getHideTriggersOnKeyboard()

        val shouldShow = (enabled || debug) && !(enabled && hideOnKeyboard && isKeyboardVisible)

        if (shouldShow) {
            // Ensure overlay exists
            if (!isOverlaysAdded) {
                createOverlays()
            } else {
                updateOverlayLayout()
            }
            // Update visual (debug color)
            val color = if (debug) 0x55FF0000 else Color.TRANSPARENT
            leftOverlayView?.setBackgroundColor(color)
            rightOverlayView?.setBackgroundColor(color)
        } else {
            // Remove overlay
            removeOverlays()
        }
    }

    private fun checkKeyboardVisibility() {
        val windows = windows ?: return
        val visible = windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        if (visible != isKeyboardVisible) {
            isKeyboardVisible = visible
            updateOverlayState()
        }
    }

    private fun getTriggerHeight(): Int {
        val manualDp = settingsRepository.getBottomTriggerHeightDp()
        if (manualDp > 0) {
            val density = resources.displayMetrics.density
            return (manualDp * density).toInt()
        }
        return getNavigationBarHeight()
    }

    private fun updateOverlayLayout() {
        if (!isOverlaysAdded || windowManager == null) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val gapPercent = settingsRepository.getBottomTriggerCenterGapPercent()

        // 3% margin to avoid interference with corner gestures
        val edgeMarginPercent = 3f
        val edgeMargin = (screenWidth * (edgeMarginPercent / 100f)).toInt()

        // Width logic: (50 - gap/2 - 3)%
        val widthPercent = 50f - (gapPercent / 2f) - edgeMarginPercent
        val width = (screenWidth * (widthPercent / 100f)).toInt().coerceAtLeast(0)
        val height = getTriggerHeight()

        // Update Left
        if (leftOverlayView != null) {
            val params = leftOverlayView!!.layoutParams as WindowManager.LayoutParams
            params.width = width
            params.height = height
            params.x = edgeMargin
            params.gravity = Gravity.BOTTOM or Gravity.START
            try { windowManager?.updateViewLayout(leftOverlayView, params) } catch (e: Exception) { e.printStackTrace() }
        }

        // Update Right
        if (rightOverlayView != null) {
            val params = rightOverlayView!!.layoutParams as WindowManager.LayoutParams
            params.width = width
            params.height = height
            params.x = edgeMargin
            params.gravity = Gravity.BOTTOM or Gravity.END
            try { windowManager?.updateViewLayout(rightOverlayView, params) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun createOverlays() {
        if (isOverlaysAdded) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val gapPercent = settingsRepository.getBottomTriggerCenterGapPercent()

        // 3% margin to avoid interference with corner gestures
        val edgeMarginPercent = 3f
        val edgeMargin = (screenWidth * (edgeMarginPercent / 100f)).toInt()

        // Width logic: (50 - gap/2 - 3)%
        val widthPercent = 50f - (gapPercent / 2f) - edgeMarginPercent
        val width = (screenWidth * (widthPercent / 100f)).toInt().coerceAtLeast(0)
        val height = getTriggerHeight()

        val baseParams = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Left View
        val leftView = FrameLayout(this)
        leftView.setBackgroundColor(if (settingsRepository.getDebugMode()) 0x55FF0000 else Color.TRANSPARENT)
        setupTouchListener(leftView)
        val leftParams = WindowManager.LayoutParams().apply { copyFrom(baseParams) }
        leftParams.x = edgeMargin
        leftParams.gravity = Gravity.BOTTOM or Gravity.START

        // Right View
        val rightView = FrameLayout(this)
        rightView.setBackgroundColor(if (settingsRepository.getDebugMode()) 0x55FF0000 else Color.TRANSPARENT)
        setupTouchListener(rightView)
        val rightParams = WindowManager.LayoutParams().apply { copyFrom(baseParams) }
        rightParams.x = edgeMargin
        rightParams.gravity = Gravity.BOTTOM or Gravity.END

        try {
            windowManager?.addView(leftView, leftParams)
            leftOverlayView = leftView

            windowManager?.addView(rightView, rightParams)
            rightOverlayView = rightView

            isOverlaysAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTouchListener(view: View) {
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
                        val deltaY = startY - event.rawY // Swipe UP is negative Y, so Start - End > 0

                        // Check for vertical swipe UP
                        if (deltaY > swipeThreshold) {
                            launchOverlay()
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
                           launchOverlay()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun launchOverlay() {
         if (settingsRepository.getBottomTriggerEnabled()) {
            val intent = Intent(this@StatusBarAccessibilityService, OverlayActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun removeOverlays() {
        try {
            if (leftOverlayView != null) {
                windowManager?.removeView(leftOverlayView)
                leftOverlayView = null
            }
            if (rightOverlayView != null) {
                windowManager?.removeView(rightOverlayView)
                rightOverlayView = null
            }
            isOverlaysAdded = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        // Fallback if 0 (e.g. gesture nav might report 0 on some APIs)
        if (result == 0) {
            val density = resources.displayMetrics.density
            result = (48 * density).toInt() // Standard ~48dp
        }
        return result
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkKeyboardVisibility()
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SPACE && event.isAltPressed) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val intent = Intent(this, OverlayActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("TOGGLE_OVERLAY", true)
                startActivity(intent)
                return true
            }
            // Consume UP event as well to prevent passing Space to other apps
            if (event.action == KeyEvent.ACTION_UP) {
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlays()
        getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
    }
}
