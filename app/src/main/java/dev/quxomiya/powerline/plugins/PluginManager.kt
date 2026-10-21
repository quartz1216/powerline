package dev.quxomiya.powerline.plugins

import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.plugins.impl.*
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
