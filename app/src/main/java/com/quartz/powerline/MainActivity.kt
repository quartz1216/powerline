package com.quartz.powerline

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.LauncherScreen
import com.quartz.powerline.ui.PermissionsScreen
import com.quartz.powerline.ui.SettingsScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.ui.viewmodel.Screen
import com.quartz.powerline.utils.ActionExecutor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkOverlayPermission()

        // Show persistent notification
        com.quartz.powerline.utils.NotificationHelper.showNotification(this)

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        setContent {
            CommandLauncherTheme(dynamicColor = false) {
                val currentScreen by viewModel.currentScreen.collectAsState()

                when (currentScreen) {
                    Screen.LAUNCHER -> {
                        // Transparent container that detects clicks outside to "dismiss" (go back)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    moveTaskToBack(true)
                                }
                        ) {
                            // Prevent clicks on the actual UI from triggering dismissal
                            Box(modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}) {
                                LauncherScreen(
                                    viewModel = viewModel,
                                    onResultClick = { result ->
                                        if (result.payload == "settings_action") {
                                            viewModel.openSettings()
                                            viewModel.onQueryChange("")
                                        } else {
                                            ActionExecutor.execute(this@MainActivity, result, viewModel.settingsRepository)
                                            viewModel.onQueryChange("")
                                            // Return to previous app after action
                                            moveTaskToBack(true)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Screen.SETTINGS -> {
                        BackHandler {
                            viewModel.closeSettings()
                        }
                        SettingsScreen(viewModel = viewModel)
                    }
                    Screen.PERMISSIONS -> {
                        BackHandler {
                            viewModel.closePermissions()
                        }
                        PermissionsScreen(onBack = { viewModel.closePermissions() })
                    }
                }
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
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
