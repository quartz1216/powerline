package dev.quxomiya.powerline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Material You: colors follow the wallpaper on Android 12+; older versions fall back to the brand scheme.
@Composable
fun PowerlineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    setSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Keyed on configuration so a wallpaper change (delivered as a config change) refreshes the scheme
    val configuration = LocalConfiguration.current
    val colorScheme = remember(configuration, darkTheme) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> BrandDarkColorScheme
            else -> BrandLightColorScheme
        }
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
