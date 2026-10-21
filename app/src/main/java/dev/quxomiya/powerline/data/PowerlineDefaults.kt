package dev.quxomiya.powerline.data

import dev.quxomiya.powerline.domain.models.CustomSearchEngine

// V2 keeps no user settings; these fixed values replace the old preferences.
object PowerlineDefaults {
    const val OVERLAY_CLOSE_ON_BACKGROUND_TAP = true

    const val MAX_RECENT_ITEMS = 5
    const val MAX_STANDBY_RECENT_APPS = 8

    const val ENABLE_WEIGHT_SEARCH = true
    const val ENABLE_PACKAGE_SEARCH = true

    val SEARCH_ENGINES: List<CustomSearchEngine> = listOf(
        CustomSearchEngine("google", "Google", "https://www.google.com/search?q=%s", isEnabled = true),
        CustomSearchEngine("youtube_music", "YouTube Music", "https://music.youtube.com/search?q=%s", "ytm", true),
        CustomSearchEngine("maps", "Maps", "https://www.google.com/maps/search/%s", "maps", true),
        CustomSearchEngine("x", "X", "https://twitter.com/search?q=%s", "x", true),
        CustomSearchEngine("youtube", "YouTube", "https://www.youtube.com/results?search_query=%s", "yt", true),
        CustomSearchEngine("wikipedia", "Wikipedia", "https://en.wikipedia.org/wiki/Special:Search?search=%s", "wiki", true),
        CustomSearchEngine("play_store", "Play Store", "https://play.google.com/store/search?q=%s", "play", true)
    )
}
