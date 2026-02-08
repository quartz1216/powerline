package com.quartz.powerline

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.LauncherScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.theme.rememberIsWallpaperLight
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.utils.ActionExecutor

class MainActivity : ComponentActivity() {
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

        setContent {
            val isWallpaperLight = rememberIsWallpaperLight()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (isWallpaperLight) {
                true -> false // Wallpaper is light -> Use Light Theme (dark text)
                false -> true // Wallpaper is dark -> Use Dark Theme (light text)
                else -> systemDark // Fallback to system theme if undetermined
            }
            val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()

            CommandLauncherTheme(
                darkTheme = useDarkTheme,
                dynamicColor = materialYouEnabled
            ) {
                // Main Activity only shows Launcher. Settings and Memo are separate Activities.
                LauncherScreen(
                    viewModel = viewModel,
                    onResultClick = { result ->
                        if (result.payload == "settings_action") {
                            startActivity(Intent(this, SettingsActivity::class.java))
                            viewModel.onQueryChange("")
                        } else {
                            ActionExecutor.execute(this@MainActivity, result, viewModel.settingsRepository, viewModel.pluginManager)
                            viewModel.onQueryChange("")
                        }
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]
        viewModel.refreshSettings()
    }
}
