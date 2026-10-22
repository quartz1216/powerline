package dev.quxomiya.powerline.search

// Search features that need no setup, each with an on/off switch in settings (on by default)
// Apps off keeps the standby app grid; Share off keeps share aliases
enum class BuiltIn(val key: String, val title: String) {
    APPS("apps_enabled", "Apps"),
    CALCULATOR("calculator_enabled", "Calculator"),
    SHARE("share_enabled", "Share")
}
