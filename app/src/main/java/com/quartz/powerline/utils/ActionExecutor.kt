package com.quartz.powerline.utils

import android.content.Context
import com.quartz.powerline.data.HistoryItem
import com.quartz.powerline.data.UserDataRepository
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.PluginManager

object ActionExecutor {

    fun execute(
        context: Context,
        result: SearchResult,
        userDataRepository: UserDataRepository,
        pluginManager: PluginManager
    ) {
        val plugin = pluginManager.getPlugin(result.pluginId)
        val success = plugin?.execute(context, result) ?: false

        if (success) {
            val historyItem = HistoryItem(
                id = result.id,
                type = result.type,
                title = result.title,
                subtitle = result.subtitle,
                payload = result.payload,
                timestamp = System.currentTimeMillis(),
                faviconUrl = result.faviconUrl
            )
            userDataRepository.addToHistory(historyItem)
        }
    }
}
