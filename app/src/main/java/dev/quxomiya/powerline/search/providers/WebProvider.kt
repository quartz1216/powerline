package dev.quxomiya.powerline.search.providers

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import dev.quxomiya.powerline.search.AvailableEngine
import dev.quxomiya.powerline.search.ResultSection
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchMode
import dev.quxomiya.powerline.search.SearchProvider
import dev.quxomiya.powerline.search.SearchRequest
import kotlinx.coroutines.flow.StateFlow

// Direct URLs and the built-in search engines. Nothing is sent anywhere until an item is run,
// which opens the URL in whatever app Android picks for it.
class WebProvider(private val engines: StateFlow<List<AvailableEngine>>) : SearchProvider {
    override val id = "web"
    override val displayName = "Web"
    override val priority = 20
    override val icon = Icons.Default.Search
    override val section = ResultSection.WEB

    // example.com, https://example.com/path, sub.example.co.jp:8080?q=1
    private val urlPattern = Regex(
        "^(https?://)?([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,24}(:\\d{1,5})?([/?#]\\S*)?$",
        RegexOption.IGNORE_CASE
    )

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
        if (request.mode == SearchMode.ALIAS) {
            val engine = engines.value.find { it.engine.id == request.engine?.id } ?: return emptyList()
            if (request.aliasQuery.isEmpty()) return emptyList()
            return listOf(engineItem(engine, request.aliasQuery))
        }

        val text = request.text
        val items = mutableListOf<SearchItem>()
        // A URL-looking query opens directly, ahead of searching for it
        if (urlPattern.matches(text)) {
            val hasScheme = text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)
            val url = if (hasScheme) text else "https://$text"
            items += SearchItem(
                id = "url:$url",
                providerId = id,
                title = text,
                subtitle = "Open in browser",
                icon = null,
                section = section,
                score = 1,
                action = SearchAction.OpenUrl(url),
                vectorIcon = Icons.Default.Public
            )
        }
        engines.value.firstOrNull { it.engine.alias == null }?.let { items += engineItem(it, text) }
        return items
    }

    private fun engineItem(available: AvailableEngine, query: String) = SearchItem(
        id = "web:${available.engine.id}:$query",
        providerId = id,
        title = query,
        subtitle = "Search ${available.engine.name}",
        icon = available.icon,
        section = section,
        score = 0,
        action = SearchAction.OpenUrl(available.engine.url.replace("%s", Uri.encode(query)))
    )
}
