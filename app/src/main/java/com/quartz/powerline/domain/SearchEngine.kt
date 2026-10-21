package com.quartz.powerline.domain

import android.content.Context
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SearchEngine(
    private val context: Context,
    private val pluginManager: PluginManager,
    private val settingsRepository: SettingsRepository
) {
    suspend fun search(query: String): List<SearchResult> = coroutineScope {
        val engines = settingsRepository.getCustomSearchEngines().filter { it.isEnabled && !it.alias.isNullOrBlank() }
        val aliasMatch = engines.find { query.startsWith("${it.alias} ") }

        if (aliasMatch != null) {
            // Exclusive search for alias
            return@coroutineScope pluginManager.getPlugin("web_search")?.search(query, context) ?: emptyList()
        }

        val deferredResults = pluginManager.plugins.map { plugin ->
            async(Dispatchers.Default) {
                plugin.search(query, context)
            }
        }
        deferredResults.awaitAll().flatten()
    }
}
