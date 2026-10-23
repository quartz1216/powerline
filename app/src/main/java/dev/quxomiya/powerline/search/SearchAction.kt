package dev.quxomiya.powerline.search

sealed interface SearchAction {
    data class LaunchApp(val packageName: String, val className: String) : SearchAction
    data class OpenUrl(val url: String) : SearchAction
    data class CopyText(val text: String) : SearchAction
    // Straight to `target` when set, else through the system share sheet
    data class ShareText(val text: String, val target: ShareTarget? = null) : SearchAction
}
