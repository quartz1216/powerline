package com.quartz.powerline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BrandDarkColorScheme = darkColorScheme(
    primary = BrandColor,
    onPrimary = Color.Black,
    primaryContainer = BrandDarkPrimaryContainer,
    onPrimaryContainer = BrandDarkOnPrimaryContainer,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val BrandLightColorScheme = lightColorScheme(
    primary = BrandColor,
    onPrimary = Color.White,
    primaryContainer = BrandLightPrimaryContainer,
    onPrimaryContainer = BrandLightOnPrimaryContainer,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Material You is fixed: dynamic color on Android 12+, brand colors otherwise.
@Composable
fun CommandLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    setSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) BrandDarkColorScheme else BrandLightColorScheme
    }

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
