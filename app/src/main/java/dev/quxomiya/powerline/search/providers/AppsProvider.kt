package dev.quxomiya.powerline.search.providers

import android.content.Context
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.search.IndexedApp
import dev.quxomiya.powerline.search.ResultSection
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchProvider
import dev.quxomiya.powerline.search.SearchRanker
import dev.quxomiya.powerline.search.SearchRequest
import kotlinx.coroutines.flow.StateFlow

// Installed apps matched by label, package name and built-in alias. Off with the Built-in switch.
// The package name subtitle follows the Advanced switch.
class AppsProvider(
    private val apps: StateFlow<List<IndexedApp>>,
    private val enabled: () -> Boolean = { true },
    private val showPackageName: () -> Boolean = { true }
) : SearchProvider {
    override val id = ID
    override val priority = 40
    override val section = ResultSection.APPS

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
        if (!enabled()) return emptyList()
        val showPackageName = showPackageName()
        return apps.value.mapNotNull { indexed ->
            val match = indexed.searchKeys.maxOfOrNull { SearchRanker.match(request, it) } ?: 0
            if (match > 0) indexed.app.toSearchItem(score = match, showPackageName = showPackageName) else null
        }
    }

    companion object {
        const val ID = "apps"
    }
}

fun AppInfo.toSearchItem(score: Int = 0, showPackageName: Boolean = true) = SearchItem(
    id = "app:$packageName:$className",
    providerId = AppsProvider.ID,
    title = label,
    subtitle = if (showPackageName) packageName else null,
    icon = icon,
    section = ResultSection.APPS,
    score = score,
    action = SearchAction.LaunchApp(packageName, className)
)
