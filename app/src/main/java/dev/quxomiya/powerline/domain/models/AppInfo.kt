package dev.quxomiya.powerline.domain.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val className: String
)
