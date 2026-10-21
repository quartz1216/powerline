package dev.quxomiya.powerline.utils

import android.content.Context
import dev.quxomiya.powerline.data.HistoryItem
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.models.SearchResult
import dev.quxomiya.powerline.plugins.PluginManager

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
