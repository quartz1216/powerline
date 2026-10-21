package dev.quxomiya.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import dev.quxomiya.powerline.domain.models.ResultType
import dev.quxomiya.powerline.domain.models.SearchResult
import dev.quxomiya.powerline.plugins.Plugin

class SharePlugin : Plugin {
    override val id = "share"
    override val name = "Share"
    override val description = "Share text via system share sheet"
    override val icon = Icons.Default.Share

    override suspend fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        return listOf(
            SearchResult(
                id = "share_text",
                pluginId = id,
                title = query,
                subtitle = "Share Text",
                type = ResultType.SHARE
            )
        )
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, result.title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
        return true
    }
}
