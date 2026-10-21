package com.quartz.powerline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.PermissionsScreen
import com.quartz.powerline.ui.SettingsScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.SettingsNav
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            CommandLauncherTheme {
                val currentNav by viewModel.currentNav.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Handle system back button to navigate within settings or finish activity
                    BackHandler {
                        if (!viewModel.navigateBack()) {
                            finish()
                        }
                    }

                    when (currentNav) {
                        SettingsNav.MAIN -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { finish() }
                            )
                        }
                        SettingsNav.PERMISSIONS -> {
                            PermissionsScreen(
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
