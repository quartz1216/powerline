package dev.quxomiya.powerline.search

// Features that need no setup, each with an on/off switch in settings (on by default)
// Apps off keeps the standby app grid. Web search and Share off also drop their aliases, chips
// and settings section. Home and Chips are the empty-query app grid and alias chip row.
enum class BuiltIn(val key: String, val title: String) {
    APPS("apps_enabled", "Apps"),
    CALCULATOR("calculator_enabled", "Calculator"),
    WEB("web_enabled", "Web search"),
    SHARE("share_enabled", "Share"),
    HOME("home_enabled", "Home"),
    CHIPS("chips_enabled", "Chips")
}
