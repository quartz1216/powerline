package dev.quxomiya.powerline.search

import android.graphics.drawable.Drawable

// Search engine defined by a URL template alone, so built-in and user-added engines
// work the same way. Android decides which app opens the URL.
data class SearchEngine(
    val id: String,
    val name: String,
    // null for the default engine, which needs no prefix
    val alias: String?,
    // %s is replaced with the encoded query
    val url: String
)

data class AvailableEngine(
    val engine: SearchEngine,
    // Icon of the app that opens this engine's URLs (the app claiming the link, else the
    // default browser); null when none resolves
    val icon: Drawable? = null
)
