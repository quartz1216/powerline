package com.quartz.powerline

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

        applyWindowAttributes(viewModel)

        setContent {
            CommandLauncherTheme(dynamicColor = false) {
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
                                ActionExecutor.execute(this@OverlayActivity, result, viewModel.settingsRepository, viewModel.pluginManager)
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
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
        applyWindowAttributes(viewModel)
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
