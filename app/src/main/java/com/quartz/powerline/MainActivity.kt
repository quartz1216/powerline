package com.quartz.powerline

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.LauncherScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.utils.ActionExecutor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkOverlayPermission()

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
            CommandLauncherTheme(dynamicColor = false) {
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
                    onMemoClick = {
                        startActivity(Intent(this, MemoActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Overlay permission for full functionality", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) { // Android 13+
            // POST_NOTIFICATIONS removed
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
