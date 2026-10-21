package com.quartz.powerline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
        if (!viewModel.userDataRepository.getOnboardingCompleted()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        setContent {
            val isWallpaperLight = rememberIsWallpaperLight()
            val systemDark = isSystemInDarkTheme()

            // Follow wallpaper contrast when known, otherwise the system theme
            val useDarkTheme = when (isWallpaperLight) {
                true -> false
                false -> true
                else -> systemDark
            }
            CommandLauncherTheme(darkTheme = useDarkTheme) {
                // Main Activity only shows Launcher. Settings and Memo are separate Activities.
                LauncherScreen(
                    viewModel = viewModel,
                    onResultClick = { result ->
                        if (result.payload == "settings_action") {
                            startActivity(Intent(this, SettingsActivity::class.java))
                            viewModel.onQueryChange("")
                        } else {
                            ActionExecutor.execute(this@MainActivity, result, viewModel.userDataRepository, viewModel.pluginManager)
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
}
