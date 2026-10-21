package com.quartz.powerline.plugins

import com.quartz.powerline.data.UserDataRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.plugins.impl.*
import kotlinx.coroutines.flow.StateFlow

class PluginManager(
    userDataRepository: UserDataRepository,
    visibleAppsFlow: StateFlow<List<AppInfo>>
) {
    val plugins: List<Plugin> = listOf(
        RecentsPlugin(userDataRepository, visibleAppsFlow),
        SystemPlugin(),
        AppLauncherPlugin(userDataRepository, visibleAppsFlow),
        CalculatorPlugin(),
        WebSearchPlugin(),
        SharePlugin()
    )

    fun getPlugin(id: String): Plugin? {
        return plugins.find { it.id == id }
    }
}
