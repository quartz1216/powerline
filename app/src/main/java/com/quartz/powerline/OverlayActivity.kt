package com.quartz.powerline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.data.PowerlineDefaults
import com.quartz.powerline.ui.LauncherScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.utils.ActionExecutor

class OverlayActivity : ComponentActivity() {

    companion object {
        const val ACTION_TOGGLE_SEARCH = "com.quartz.powerline.action.TOGGLE_SEARCH"
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        applyWindowAttributes()

        setContent {
            CommandLauncherTheme {
                // Transparent container that detects clicks outside to close
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                        if (PowerlineDefaults.OVERLAY_CLOSE_ON_BACKGROUND_TAP) {
                            finish()
                        }
                        }
                ) {
                    // Prevent clicks on the actual UI from closing the activity
                    Box(modifier = Modifier) {
                        LauncherScreen(
                            viewModel = viewModel,
                            onResultClick = { result ->
                                if (result.payload == "settings_action") {
                                    startActivity(Intent(this@OverlayActivity, SettingsActivity::class.java))
                                    finish()
                                } else {
                                    ActionExecutor.execute(this@OverlayActivity, result, viewModel.userDataRepository, viewModel.pluginManager)
                                    finish()
                                }
                            },
                            onSettingsClick = {
                                startActivity(Intent(this@OverlayActivity, SettingsActivity::class.java))
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
        applyWindowAttributes()
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_TOGGLE_SEARCH) {
            if (isVisible) {
                finish()
            } else {
                ViewModelProvider(
                    this,
                    ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )[MainViewModel::class.java].resetForOpen()
            }
        }
    }

    private fun applyWindowAttributes() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (Build.VERSION.SDK_INT >= 31) {
             window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        val params = window.attributes
        params.dimAmount = PowerlineDefaults.OVERLAY_DIM_AMOUNT
        if (Build.VERSION.SDK_INT >= 31) {
            params.blurBehindRadius = PowerlineDefaults.OVERLAY_BLUR_RADIUS
        }
        window.attributes = params
    }
}
