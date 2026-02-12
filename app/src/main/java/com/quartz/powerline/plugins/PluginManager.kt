package com.quartz.powerline.plugins

import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.plugins.impl.*
import kotlinx.coroutines.flow.StateFlow

class PluginManager(
    settingsRepository: SettingsRepository,
    installedAppsFlow: StateFlow<List<AppInfo>>,
    visibleAppsFlow: StateFlow<List<AppInfo>>
) {
    val plugins: List<Plugin> = listOf(
        RecentsPlugin(settingsRepository, visibleAppsFlow),
        SystemPlugin(),
        AppLauncherPlugin(settingsRepository, installedAppsFlow, visibleAppsFlow),
        CalculatorPlugin(settingsRepository),
        WebSearchPlugin(settingsRepository),
        SharePlugin(settingsRepository)
    )

    fun getPlugin(id: String): Plugin? {
        return plugins.find { it.id == id }
    }
}
