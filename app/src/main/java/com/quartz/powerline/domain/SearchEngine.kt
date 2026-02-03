package com.quartz.powerline.domain

import android.content.Context
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.PluginManager

class SearchEngine(
    private val context: Context,
    private val pluginManager: PluginManager
) {
    fun search(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        for (plugin in pluginManager.plugins) {
            results.addAll(plugin.search(query, context))
        }
        return results
    }
}
