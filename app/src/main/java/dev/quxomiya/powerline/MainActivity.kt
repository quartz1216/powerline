package dev.quxomiya.powerline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.quxomiya.powerline.ui.StatusScreen
import dev.quxomiya.powerline.ui.theme.CommandLauncherTheme

// Launcher entry: status / permissions / About. Search is the Alt+Space overlay (SearchOverlay).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CommandLauncherTheme {
                StatusScreen()
            }
        }
    }
}
