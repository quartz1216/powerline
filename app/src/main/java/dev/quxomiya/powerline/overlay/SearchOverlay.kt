package dev.quxomiya.powerline.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.quxomiya.powerline.MainActivity
import dev.quxomiya.powerline.R
import dev.quxomiya.powerline.ui.SearchScreen
import dev.quxomiya.powerline.ui.theme.PowerlineTheme
import dev.quxomiya.powerline.ui.viewmodel.SearchViewModel
import dev.quxomiya.powerline.utils.ActionExecutor

// Search surface drawn as an accessibility overlay window instead of an Activity,
// so desktop / freeform windowing cannot turn it into an app window.
class SearchOverlay(private val service: AccessibilityService) {

    private val owner = OverlayOwner()
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val context: Context = NewTaskContext(
        android.view.ContextThemeWrapper(service, R.style.Theme_Powerline)
    )
    private val viewModel: SearchViewModel by lazy {
        ViewModelProvider(
            owner,
            ViewModelProvider.AndroidViewModelFactory.getInstance(service.application)
        )[SearchViewModel::class.java]
    }

    private var rootView: FrameLayout? = null

    val isShowing: Boolean
        get() = rootView != null

    // triggeredAt: uptime of the key press that opened the overlay, for the open-time log
    fun toggle(triggeredAt: Long = SystemClock.uptimeMillis()) {
        if (isShowing) hide() else show(triggeredAt)
    }

    fun show(triggeredAt: Long = SystemClock.uptimeMillis()) {
        if (isShowing) return
        viewModel.resetForOpen()

        val composeView = ComposeView(context).apply {
            setContent {
                PowerlineTheme(setSystemBars = false) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                hide()
                            }
                    ) {
                        SearchScreen(
                            viewModel = viewModel,
                            onResultClick = { item ->
                                ActionExecutor.execute(context, item, viewModel.userDataRepository)
                                hide()
                            },
                            onInfoClick = { openStatusScreen() }
                        )
                    }
                }
            }
        }

        val root = DismissKeyLayout(context, onDismiss = ::hide).apply {
            addView(composeView)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
        }

        try {
            windowManager.addView(root, layoutParams())
            rootView = root
            logOpenTime(root, triggeredAt)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot show the overlay", e)
        }
    }

    // The fade-out is the window exit animation (windowAnimations), run by the system after removal
    fun hide() {
        val root = rootView ?: return
        rootView = null
        try {
            windowManager.removeView(root)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot remove the overlay", e)
        }
    }

    // Time from the key press to the first frame (ROADMAP Phase 7 target: 200ms); read by OverlayE2ETest
    private fun logOpenTime(root: View, triggeredAt: Long) {
        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                root.viewTreeObserver.removeOnPreDrawListener(this)
                Log.i(TAG, "Shown ${SystemClock.uptimeMillis() - triggeredAt} ms after the key press")
                return true
            }
        })
    }

    fun destroy() {
        hide()
        owner.destroy()
    }

    private fun openStatusScreen() {
        context.startActivity(Intent(context, MainActivity::class.java))
        hide()
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Hardware acceleration is off by default for service windows
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            windowAnimations = R.style.OverlayWindowAnimation
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // Closes the overlay on Back / Esc
    private class DismissKeyLayout(
        context: Context,
        private val onDismiss: () -> Unit
    ) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (event.action == KeyEvent.ACTION_UP) onDismiss()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }

    // Activities started from the service context need a new task
    private class NewTaskContext(base: Context) : ContextWrapper(base) {
        override fun startActivity(intent: Intent) {
            super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        override fun startActivity(intent: Intent, options: Bundle?) {
            super.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
        }
    }

    // Lifecycle / ViewModel / SavedState owner for the Compose content of the overlay window
    private class OverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore = ViewModelStore()
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        init {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }

    private companion object {
        const val TAG = "SearchOverlay"
    }
}
