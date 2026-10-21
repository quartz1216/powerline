package dev.quxomiya.powerline.search

// Drops later items that would do the same thing as an earlier one (input results only;
// the standby state is allowed to show an app twice).
object SearchDeduplicator {

    fun deduplicate(items: List<SearchItem>): List<SearchItem> = items.distinctBy { key(it.action) }

    private fun key(action: SearchAction): Any = when (action) {
        // One entry per app, whichever launcher activity it came with
        is SearchAction.LaunchApp -> "app:${action.packageName}"
        is SearchAction.OpenUrl -> "url:${action.url.trimEnd('/')}"
        else -> action
    }
}
