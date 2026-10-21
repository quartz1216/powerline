package dev.quxomiya.powerline.search

import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.plugins.impl.AppLauncherPlugin
import dev.quxomiya.powerline.plugins.impl.CalculatorPlugin
import dev.quxomiya.powerline.plugins.impl.SharePlugin
import dev.quxomiya.powerline.plugins.impl.WebSearchPlugin
import kotlinx.coroutines.flow.StateFlow

// All providers are always on (V2 has no per-provider settings).
// Recents and System are not providers; see ROADMAP §4.4.
class ProviderRegistry(
    userDataRepository: UserDataRepository,
    visibleAppsFlow: StateFlow<List<AppInfo>>
) {
    val providers: List<SearchProvider> = listOf(
        PluginSearchProviderAdapter(AppLauncherPlugin(userDataRepository, visibleAppsFlow), "apps", ResultSection.APPS, priority = 40),
        PluginSearchProviderAdapter(CalculatorPlugin(), "calculator", ResultSection.CALCULATOR, priority = 30),
        PluginSearchProviderAdapter(WebSearchPlugin(), "web", ResultSection.WEB, priority = 20),
        PluginSearchProviderAdapter(SharePlugin(), "share", ResultSection.SHARE, priority = 10)
    )

    // An alias query ("yt laneige") is a web search only
    fun providersFor(request: SearchRequest): List<SearchProvider> = when (request.mode) {
        SearchMode.TEXT -> providers
        SearchMode.ALIAS -> providers.filter { it.section == ResultSection.WEB }
    }
}
