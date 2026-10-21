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
import com.quartz.powerline.domain.models.AppTheme
import com.quartz.powerline.ui.LauncherScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.utils.ActionExecutor

class OverlayActivity : ComponentActivity() {

    companion object {
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        applyWindowAttributes(viewModel)

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dimAmount = viewModel.settingsRepository.getOverlayDimAmount()

            // App Theme logic
            val appTheme = viewModel.settingsRepository.getAppTheme()
            val customAccentColor = viewModel.settingsRepository.getCustomAccentColor()
            val materialYouEnabled = viewModel.settingsRepository.getMaterialYouEnabled()

            // Determine if we should use dark theme based on settings or fallback to dim amount heuristic
            val useDarkTheme = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> systemDark
                AppTheme.BRAND, AppTheme.MATERIAL_YOU -> {
                     // For Brand/MaterialYou, we stick to original logic: depends on Dim or System
                     if (dimAmount > 0.4f) true else systemDark
                }
            }

            CommandLauncherTheme(
                darkTheme = useDarkTheme,
                dynamicColor = materialYouEnabled,
                appTheme = appTheme,
                customAccentColor = customAccentColor
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
                        if (viewModel.settingsRepository.getOverlayCloseOnBackgroundTap()) {
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
                                    ActionExecutor.execute(this@OverlayActivity, result, viewModel.settingsRepository, viewModel.pluginManager)
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
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
        applyWindowAttributes(viewModel)
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("TOGGLE_OVERLAY", false)) {
            if (isVisible) {
                finish()
            }
        }
        setIntent(intent)
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
