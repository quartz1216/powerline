package com.quartz.powerline.domain

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.PluginManager

class SearchEngine(
    private val context: Context,
    private val pluginManager: PluginManager,
    private val settingsRepository: SettingsRepository
) {
    fun search(query: String): List<SearchResult> {
        val engines = settingsRepository.getCustomSearchEngines().filter { it.isEnabled && !it.alias.isNullOrBlank() }
        val aliasMatch = engines.find { query.startsWith("${it.alias} ") }

        if (aliasMatch != null) {
            // Exclusive search for alias
            return pluginManager.getPlugin("web_search")?.search(query, context) ?: emptyList()
        }

        val results = mutableListOf<SearchResult>()
        for (plugin in pluginManager.plugins) {
            results.addAll(plugin.search(query, context))
        }
        return results
    }
}
