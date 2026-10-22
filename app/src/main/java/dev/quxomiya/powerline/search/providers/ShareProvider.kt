package dev.quxomiya.powerline.search.providers

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import dev.quxomiya.powerline.search.AvailableShareTarget
import dev.quxomiya.powerline.search.ResultSection
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchMode
import dev.quxomiya.powerline.search.SearchProvider
import dev.quxomiya.powerline.search.SearchRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Sends the query text to the system share sheet, or straight to an app after its share alias.
// Off with the Built-in switch, aliases included.
class ShareProvider(
    private val shareTargets: StateFlow<List<AvailableShareTarget>> = MutableStateFlow(emptyList()),
    private val enabled: () -> Boolean = { true }
) : SearchProvider {
    override val id = "share"
    override val displayName = "Share"
    override val priority = 10
    override val icon = Icons.Default.Share
    override val section = ResultSection.SHARE

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
        if (!enabled()) return emptyList()
        if (request.mode == SearchMode.ALIAS) {
            val target = request.shareTarget ?: return emptyList()
            if (request.aliasQuery.isEmpty()) return emptyList()
            return listOf(
                SearchItem(
                    id = "share:${target.id}:${request.aliasQuery}",
                    providerId = id,
                    title = request.aliasQuery,
                    subtitle = "Share to ${target.name}",
                    // The target app's icon once AppIndex has loaded it
                    icon = shareTargets.value.find { it.target == target }?.icon,
                    section = section,
                    score = 0,
                    action = SearchAction.ShareText(request.aliasQuery, target),
                    vectorIcon = icon
                )
            )
        }
        return listOf(
            SearchItem(
                id = "share:${request.text}",
                providerId = id,
                title = request.text,
                subtitle = "Share text",
                icon = null,
                section = section,
                score = 0,
                action = SearchAction.ShareText(request.text),
                vectorIcon = icon
            )
        )
    }
}
