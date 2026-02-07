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
            if (result.type == com.quartz.powerline.domain.models.ResultType.MEMO) {
                // Do not save to history
                return
            }

            val historyItem = HistoryItem(
                id = result.id,
                type = result.type,
                title = result.title,
                subtitle = result.subtitle,
                payload = result.payload,
                timestamp = System.currentTimeMillis()
            )
            settingsRepository.addToHistory(historyItem)
        }
    }
}
