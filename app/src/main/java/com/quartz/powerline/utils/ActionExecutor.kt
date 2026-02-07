package com.quartz.powerline.utils

import android.content.Context
import com.quartz.powerline.data.HistoryItem
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.PluginManager

object ActionExecutor {

    fun execute(
        context: Context,
        result: SearchResult,
        settingsRepository: SettingsRepository,
        pluginManager: PluginManager
    ) {
        val plugin = pluginManager.getPlugin(result.pluginId)
        val success = plugin?.execute(context, result) ?: false

        if (success) {
            // Determine HistoryItem content based on ResultType
            val (mainText, subText) = when (result.type) {
                com.quartz.powerline.domain.models.ResultType.APP -> {
                    // Main: App Name, Sub: "Application"
                    result.title to "Application"
                }
                com.quartz.powerline.domain.models.ResultType.WEB -> {
                    // Main: Query (subtitle), Sub: "Search with [Title]"
                    // WebSearchPlugin: Title="Google", Subtitle="cat"
                    (result.subtitle ?: "") to "Search with ${result.title}"
                }
                com.quartz.powerline.domain.models.ResultType.CALC -> {
                    // Main: Expression=Result (Title), Sub: "Calculator"
                    result.title to "Calculator"
                }
                com.quartz.powerline.domain.models.ResultType.INTEGRATION -> {
                    // Main: Shared Text (Payload), Sub: "Share with..."
                    // IntegrationPlugin: Title="Share Text", Subtitle=query, Payload=query
                    (result.payload ?: "") to "Share with..."
                }
                com.quartz.powerline.domain.models.ResultType.MEMO -> {
                    // Do not save to history
                    return
                }
                else -> {
                    // Fallback
                    result.title to (result.subtitle ?: "")
                }
            }

            val historyItem = HistoryItem(
                id = result.id,
                type = result.type,
                title = mainText,
                subtitle = subText,
                timestamp = System.currentTimeMillis()
            )
            settingsRepository.addToHistory(historyItem)
        }
    }
}
