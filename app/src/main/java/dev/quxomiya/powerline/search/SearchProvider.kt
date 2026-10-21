package dev.quxomiya.powerline.search

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector

interface SearchProvider {
    val id: String
    val displayName: String
    val priority: Int
    val icon: ImageVector
    val section: ResultSection

    suspend fun search(request: SearchRequest, context: Context): List<SearchItem>
}
