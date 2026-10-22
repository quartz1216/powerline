package dev.quxomiya.powerline.search

import dev.quxomiya.powerline.search.providers.AppsProvider
import dev.quxomiya.powerline.search.providers.CalculatorProvider
import dev.quxomiya.powerline.search.providers.ShareProvider
import dev.quxomiya.powerline.search.providers.WebProvider

// All providers are registered; a built-in switched off in settings returns nothing (Share
// still answers its aliases).
// Recents and System are not providers; see ROADMAP §4.4.
class ProviderRegistry(val providers: List<SearchProvider>) {

    constructor(appIndex: AppIndex, isEnabled: (BuiltIn) -> Boolean = { true }) : this(
        listOf(
            AppsProvider(appIndex.apps) { isEnabled(BuiltIn.APPS) },
            CalculatorProvider { isEnabled(BuiltIn.CALCULATOR) },
            WebProvider(appIndex.availableEngines),
            ShareProvider(appIndex.availableShareTargets) { isEnabled(BuiltIn.SHARE) }
        )
    )

    // An engine alias ("yt laneige") is a web search only; a share alias ("cl hello") a share only
    fun providersFor(request: SearchRequest): List<SearchProvider> = when (request.mode) {
        SearchMode.TEXT -> providers
        SearchMode.ALIAS -> {
            val section = if (request.shareTarget != null) ResultSection.SHARE else ResultSection.WEB
            providers.filter { it.section == section }
        }
    }
}
