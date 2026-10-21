package dev.quxomiya.powerline.plugins

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import dev.quxomiya.powerline.domain.models.SearchResult

interface Plugin {
    val id: String
    val name: String
    val description: String
    val icon: ImageVector

    suspend fun search(query: String, context: Context): List<SearchResult>
    fun execute(context: Context, result: SearchResult): Boolean
}
