package dev.quxomiya.powerline.search

// Search features that need no setup, each with an on/off switch in settings (on by default)
// Apps off keeps the standby app grid. Web search and Share off also drop their aliases, chips
// and settings section.
enum class BuiltIn(val key: String, val title: String) {
    APPS("apps_enabled", "Apps"),
    CALCULATOR("calculator_enabled", "Calculator"),
    WEB("web_enabled", "Web search"),
    SHARE("share_enabled", "Share")
}
