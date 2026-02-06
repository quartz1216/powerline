package com.quartz.powerline.plugins

import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.plugins.impl.*
import kotlinx.coroutines.flow.StateFlow

class PluginManager(
    settingsRepository: SettingsRepository,
    installedAppsFlow: StateFlow<List<AppInfo>>
) {
    val plugins: List<Plugin> = listOf(
        RecentsPlugin(settingsRepository, installedAppsFlow),
        SystemPlugin(),
        AppLauncherPlugin(settingsRepository, installedAppsFlow),
        CalculatorPlugin(),
        WebSearchPlugin(),
        MemoPlugin(),
        IntegrationPlugin()
    )

    fun getPlugin(id: String): Plugin? {
        return plugins.find { it.id == id }
    }
}
