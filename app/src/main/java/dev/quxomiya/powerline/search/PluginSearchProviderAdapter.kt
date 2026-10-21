package dev.quxomiya.powerline.search

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import dev.quxomiya.powerline.domain.models.ResultType
import dev.quxomiya.powerline.domain.models.SearchResult
import dev.quxomiya.powerline.plugins.Plugin

// Wraps a V1 plugin as a SearchProvider until Phase 5 rewrites each provider
class PluginSearchProviderAdapter(
    private val plugin: Plugin,
    override val id: String,
    override val section: ResultSection,
    override val priority: Int
) : SearchProvider {
    override val displayName: String = plugin.name
    override val icon: ImageVector = plugin.icon

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
        // Plugins parse aliases themselves and expect them as written in PowerlineDefaults
        val query = when (request.mode) {
            SearchMode.TEXT -> request.text
            SearchMode.ALIAS -> "${request.engine!!.alias} ${request.aliasQuery}"
        }
        val results = plugin.search(query, context)
        return results.mapIndexedNotNull { index, result ->
            // Keeps the plugin's order where SearchRanker does not text-match
            toItem(result, score = results.size - index)
        }
    }

    private fun toItem(result: SearchResult, score: Int): SearchItem? {
        val action = when (result.type) {
            ResultType.APP -> SearchAction.LaunchApp(result.packageName ?: return null, result.className)
            ResultType.WEB -> SearchAction.OpenUrl(result.payload ?: return null)
            ResultType.CALC -> SearchAction.CopyText(result.payload ?: return null)
            ResultType.SHARE -> SearchAction.ShareText(result.title)
        }
        val isApp = action is SearchAction.LaunchApp
        return SearchItem(
            id = result.id,
            providerId = id,
            title = result.title,
            subtitle = result.subtitle,
            icon = result.icon,
            section = section,
            score = if (isApp) 0 else score,
            action = action,
            // AppLauncherPlugin also matches the package name minus its first segment
            keywords = if (isApp) listOfNotNull(result.packageName?.substringAfter('.')) else emptyList(),
            rankByMatch = isApp,
            iconUrl = result.faviconUrl
        )
    }
}
