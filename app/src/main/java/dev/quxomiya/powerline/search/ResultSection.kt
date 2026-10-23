package dev.quxomiya.powerline.search

// Declaration order is the display order of the result list. Each section's provider runs only
// while its built-in is switched on.
enum class ResultSection(val builtIn: BuiltIn) {
    APPS(BuiltIn.APPS),
    CALCULATOR(BuiltIn.CALCULATOR),
    WEB(BuiltIn.WEB),
    SHARE(BuiltIn.SHARE)
}
