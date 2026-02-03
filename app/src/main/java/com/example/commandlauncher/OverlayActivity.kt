package com.example.commandlauncher

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
import androidx.lifecycle.ViewModelProvider
import com.example.commandlauncher.ui.LauncherScreen
import com.example.commandlauncher.ui.theme.CommandLauncherTheme
import com.example.commandlauncher.ui.viewmodel.MainViewModel
import com.example.commandlauncher.utils.ActionExecutor

class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

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
                            finish()
                        }
                ) {
                    // Prevent clicks on the actual UI from closing the activity
                    Box(modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}) {
                        LauncherScreen(
                            viewModel = viewModel,
                            onResultClick = { result ->
                                ActionExecutor.execute(this@OverlayActivity, result)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
