package dev.quxomiya.powerline.data

import dev.quxomiya.powerline.search.SearchEngine

// Fixed values in place of the old preferences. The only settings are the search settings
// (SearchSettingsRepository: Calculator, and engines starting from SEARCH_ENGINES, share targets).
object PowerlineDefaults {
    const val OVERLAY_CLOSE_ON_BACKGROUND_TAP = true

    // Opened in the browser from the status screen's About section
    const val PRIVACY_POLICY_URL = "https://github.com/quartz1216/powerline/blob/main/PRIVACY.md"

    const val MAX_STANDBY_RECENT_APPS = 8

    // App launches kept for the recent row and frecency
    const val MAX_HISTORY_ITEMS = 100
    // A launch counts half as much after this long
    const val FRECENCY_HALF_LIFE_MS = 7L * 24 * 60 * 60 * 1000

    // Search pipeline (ROADMAP §4.4)
    const val SEARCH_DEBOUNCE_MS = 40L
    const val PROVIDER_TIMEOUT_MS = 150L
    const val SEARCH_TIMEOUT_MS = 300L
    const val MAX_RESULTS_PER_SECTION = 5
    const val MAX_TOTAL_RESULTS = 20

    // The first engine without an alias is the default. Order is the chip order.
    val SEARCH_ENGINES: List<SearchEngine> = listOf(
        SearchEngine("google", "Google", null, "https://www.google.com/search?q=%s"),
        SearchEngine("youtube_music", "YouTube Music", "ytm", "https://music.youtube.com/search?q=%s"),
        SearchEngine("maps", "Maps", "maps", "https://www.google.com/maps/search/%s"),
        SearchEngine("x", "X", "x", "https://x.com/search?q=%s"),
        SearchEngine("youtube", "YouTube", "yt", "https://www.youtube.com/results?search_query=%s"),
        SearchEngine("wikipedia", "Wikipedia", "wiki", "https://en.wikipedia.org/wiki/Special:Search?search=%s"),
        SearchEngine("play_store", "Play Store", "play", "https://play.google.com/store/search?q=%s")
    )

    // English names for common system apps whose label (often localized) and package name
    // do not contain them, so "phone" finds 電話 / Telefon
    val APP_ALIASES: Map<String, List<String>> = mapOf(
        "com.android.vending" to listOf("play store", "store"),
        "com.android.dialer" to listOf("phone", "call"),
        "com.google.android.dialer" to listOf("phone", "call"),
        "com.android.mms" to listOf("messages", "sms"),
        "com.google.android.apps.messaging" to listOf("messages", "sms"),
        "com.android.settings" to listOf("settings"),
        "com.google.android.GoogleCamera" to listOf("camera"),
        "com.google.android.deskclock" to listOf("clock", "alarm", "timer"),
        "com.google.android.documentsui" to listOf("files"),
        "com.google.android.apps.nbu.files" to listOf("files"),
        "com.google.android.contacts" to listOf("contacts", "people"),
        "com.google.android.calendar" to listOf("calendar")
    )
}
