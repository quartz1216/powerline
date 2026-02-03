package com.quartz.powerline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

        setContent {
            CommandLauncherTheme(dynamicColor = false) {
                LauncherScreen(
                    viewModel = viewModel,
                    onResultClick = { result ->
                        if (result.payload == "settings_action") {
                            val intent = Intent(this@OverlayActivity, MainActivity::class.java)
                            intent.putExtra("open_settings", true)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            viewModel.onQueryChange("")
                            finish()
                        } else {
                            ActionExecutor.execute(this@OverlayActivity, result, viewModel.settingsRepository)
                            viewModel.onQueryChange("")
                            finish()
                        }
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}
