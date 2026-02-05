package com.quartz.powerline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.AppearanceSettingsScreen
import com.quartz.powerline.ui.PermissionsScreen
import com.quartz.powerline.ui.PluginSettingsScreen
import com.quartz.powerline.ui.SettingsScreen
import com.quartz.powerline.ui.OverlaySettingsSubpage
import com.quartz.powerline.ui.AdditionalSettingsSubpage
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.SettingsNav
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            CommandLauncherTheme(dynamicColor = true) {
                val currentNav by viewModel.currentNav.collectAsState()

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
                    SettingsNav.OVERLAY -> {
                        OverlaySettingsSubpage(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    SettingsNav.ADDITIONAL -> {
                        AdditionalSettingsSubpage(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    SettingsNav.APPEARANCE -> {
                        AppearanceSettingsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    SettingsNav.PERMISSIONS -> {
                        PermissionsScreen(
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    SettingsNav.PLUGIN -> {
                        val pluginId by viewModel.selectedPluginId.collectAsState()
                        val plugin = pluginId?.let { viewModel.pluginManager.getPlugin(it) }
                        if (plugin != null) {
                            PluginSettingsScreen(
                                viewModel = viewModel,
                                plugin = plugin,
                                onBack = { viewModel.navigateBack() }
                            )
                        } else {
                            viewModel.navigateBack()
                        }
                    }
                }
            }
        }
    }
}
