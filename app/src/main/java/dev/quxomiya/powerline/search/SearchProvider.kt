package dev.quxomiya.powerline.search

import android.content.Context

interface SearchProvider {
    val id: String
    val priority: Int
    val section: ResultSection

    suspend fun search(request: SearchRequest, context: Context): List<SearchItem>
}
