package com.quartz.powerline.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Theme color is fixed to the brand (icon background) color; no wallpaper-based dynamic color.
@Composable
fun CommandLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    setSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BrandDarkColorScheme else BrandLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode && setSystemBars) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
