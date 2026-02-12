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
import com.quartz.powerline.ui.LauncherScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.utils.ActionExecutor

class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        // Onboarding Check
        if (!viewModel.settingsRepository.getOnboardingCompleted()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        applyWindowAttributes(viewModel)

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dimAmount = viewModel.settingsRepository.getOverlayDimAmount()
            val useDarkTheme = if (dimAmount > 0.4f) {
                true // High dim -> Dark background -> Light text -> Dark Theme
            } else {
                systemDark // Low dim -> Background depends on system (Light/Dark) -> Match system theme (Dark BG->Light Text / Light BG->Dark Text)
            }

            CommandLauncherTheme(
                darkTheme = useDarkTheme,
                dynamicColor = true
            ) {
                // Transparent container that detects clicks outside to close
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Background tap clears the search query but keeps the overlay active (Home behavior)
                            viewModel.onQueryChange("")
                        }
                ) {
                    // Prevent clicks on the actual UI from closing the activity
                    Box(modifier = Modifier) {
                        LauncherScreen(
                            viewModel = viewModel,
                            onResultClick = { result ->
                                if (result.payload == "settings_action") {
                                    startActivity(Intent(this@OverlayActivity, SettingsActivity::class.java))
                                    viewModel.onQueryChange("")
                                } else {
                                    ActionExecutor.execute(this@OverlayActivity, result, viewModel.settingsRepository, viewModel.pluginManager)
                                    viewModel.onQueryChange("")
                                }
                            },
                            onSettingsClick = {
                                startActivity(Intent(this@OverlayActivity, SettingsActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
        applyWindowAttributes(viewModel)
    }

    override fun onResume() {
        super.onResume()
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
        viewModel.refreshSettings()
    }

    private fun applyWindowAttributes(viewModel: MainViewModel) {
        val dimAmount = viewModel.settingsRepository.getOverlayDimAmount()
        val blurRadius = viewModel.settingsRepository.getOverlayBlurRadius()

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (Build.VERSION.SDK_INT >= 31) {
             window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        val params = window.attributes
        params.dimAmount = dimAmount
        if (Build.VERSION.SDK_INT >= 31) {
            params.blurBehindRadius = blurRadius
        }
        window.attributes = params
    }
}
