package com.example.commandlauncher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.commandlauncher.ui.LauncherScreen
import com.example.commandlauncher.ui.theme.CommandLauncherTheme
import com.example.commandlauncher.ui.viewmodel.MainViewModel
import com.example.commandlauncher.utils.ActionExecutor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkOverlayPermission()

        // Show persistent notification
        com.example.commandlauncher.utils.NotificationHelper.showNotification(this)

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        setContent {
            CommandLauncherTheme(dynamicColor = false) { // Use default dark/light for consistency
                LauncherScreen(
                    viewModel = viewModel,
                    onResultClick = { result ->
                        ActionExecutor.execute(this, result)
                        // If it's a calculator copy, we might want to stay?
                        // But usually we want to launch and minimize.
                        // For Home app, we can't really "minimize". We just launch something else.
                        // If we copy, we might want to clear query.
                        if (result.type == com.example.commandlauncher.domain.models.ResultType.CALC) {
                            viewModel.onQueryChange("")
                        }
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
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure query is cleared if we returned from an app?
        // User didn't specify. Standard behavior is to keep state or reset.
        // Given "stateNotNeeded=true", it might reset.
    }
}
