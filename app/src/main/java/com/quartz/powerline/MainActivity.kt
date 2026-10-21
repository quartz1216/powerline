package com.quartz.powerline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quartz.powerline.data.UserDataRepository
import com.quartz.powerline.ui.StatusScreen
import com.quartz.powerline.ui.theme.CommandLauncherTheme

// Launcher entry: status / permissions / About. Search is the Alt+Space overlay (SearchOverlay).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Onboarding Check
        if (!UserDataRepository(this).getOnboardingCompleted()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        setContent {
            CommandLauncherTheme {
                StatusScreen()
            }
        }
    }
}
