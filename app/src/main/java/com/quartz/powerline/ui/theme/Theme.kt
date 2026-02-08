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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.quartz.powerline.domain.models.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

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

@Composable
fun CommandLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    appTheme: AppTheme = AppTheme.MATERIAL_YOU, // Default to Material You
    customAccentColor: Int = 0xFF4CDC8F.toInt(), // Default to Brand Color
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (appTheme) {
        AppTheme.BRAND -> {
            if (darkTheme) BrandDarkColorScheme else BrandLightColorScheme
        }
        AppTheme.MATERIAL_YOU -> {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
             } else {
                 // Fallback to Brand if Material You not supported
                 if (darkTheme) BrandDarkColorScheme else BrandLightColorScheme
             }
        }
        AppTheme.LIGHT -> {
            // Light background, custom accent
            val accent = Color(customAccentColor)
            lightColorScheme(
                primary = accent,
                secondary = PurpleGrey40,
                tertiary = Pink40
            )
        }
        AppTheme.DARK -> {
            // Dark background, custom accent
             val accent = Color(customAccentColor)
             darkColorScheme(
                primary = accent,
                secondary = PurpleGrey80,
                tertiary = Pink80
            )
        }
        AppTheme.SYSTEM -> {
            // Follow system dark/light, custom accent
             val accent = Color(customAccentColor)
             if (darkTheme) {
                 darkColorScheme(
                    primary = accent,
                    secondary = PurpleGrey80,
                    tertiary = Pink80
                )
             } else {
                 lightColorScheme(
                    primary = accent,
                    secondary = PurpleGrey40,
                    tertiary = Pink40
                )
             }
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
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
