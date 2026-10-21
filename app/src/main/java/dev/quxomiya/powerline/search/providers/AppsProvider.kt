package dev.quxomiya.powerline.search.providers

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.search.IndexedApp
import dev.quxomiya.powerline.search.ResultSection
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchProvider
import dev.quxomiya.powerline.search.SearchRanker
import dev.quxomiya.powerline.search.SearchRequest
import kotlinx.coroutines.flow.StateFlow

// Installed apps matched by label, package name and built-in alias
class AppsProvider(private val apps: StateFlow<List<IndexedApp>>) : SearchProvider {
    override val id = ID
    override val displayName = "Apps"
    override val priority = 40
    override val icon = Icons.Default.Apps
    override val section = ResultSection.APPS

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> =
        apps.value.mapNotNull { indexed ->
            val match = indexed.searchKeys.maxOfOrNull { SearchRanker.match(request, it) } ?: 0
            if (match > 0) indexed.app.toSearchItem(score = match) else null
        }

    companion object {
        const val ID = "apps"
    }
}

fun AppInfo.toSearchItem(score: Int = 0) = SearchItem(
    id = "app:$packageName:$className",
    providerId = AppsProvider.ID,
    title = label,
    subtitle = packageName,
    icon = icon,
    section = ResultSection.APPS,
    score = score,
    action = SearchAction.LaunchApp(packageName, className)
)
