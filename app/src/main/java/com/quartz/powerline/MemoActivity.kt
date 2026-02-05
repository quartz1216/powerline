package com.quartz.powerline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.quartz.powerline.ui.MemoScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme
import com.quartz.powerline.ui.viewmodel.MemoViewModel

class MemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        setContent {
            CommandLauncherTheme(dynamicColor = true) {
                MemoScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}
