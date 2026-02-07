package com.quartz.powerline.ui.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Returns true if the system wallpaper is "light" (and thus requires dark text/light theme).
 * Returns false if the system wallpaper is "dark" (and thus requires light text/dark theme).
 * Returns null if undetermined or API level is too low.
 */
@Composable
fun rememberIsWallpaperLight(): Boolean? {
    // API 27+ (O_MR1) required for WallpaperColors
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
        return null
    }

    val context = LocalContext.current
    val wallpaperManager = remember { WallpaperManager.getInstance(context) }
    val isLight = remember { mutableStateOf<Boolean?>(null) }

    DisposableEffect(wallpaperManager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, _ ->
             isLight.value = checkWallpaperLight(colors)
        }

        // Initial check
        // getWallpaperColors requires API 27
        val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        isLight.value = checkWallpaperLight(colors)

        // Register listener
        // Use main looper for callback
        val handler = Handler(Looper.getMainLooper())
        wallpaperManager.addOnColorsChangedListener(listener, handler)

        onDispose {
            wallpaperManager.removeOnColorsChangedListener(listener)
        }
    }

    return isLight.value
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
private fun checkWallpaperLight(colors: WallpaperColors?): Boolean? {
    if (colors == null) return null
    // If supports dark text, it means the wallpaper is light.
    return (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
}
