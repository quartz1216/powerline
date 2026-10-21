package dev.quxomiya.powerline.search.providers

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import dev.quxomiya.powerline.search.ResultSection
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchMode
import dev.quxomiya.powerline.search.SearchProvider
import dev.quxomiya.powerline.search.SearchRequest

// Sends the query text to the system share sheet
class ShareProvider : SearchProvider {
    override val id = "share"
    override val displayName = "Share"
    override val priority = 10
    override val icon = Icons.Default.Share
    override val section = ResultSection.SHARE

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
        if (request.mode != SearchMode.TEXT) return emptyList()
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
