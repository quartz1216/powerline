package dev.quxomiya.powerline.search

import dev.quxomiya.powerline.search.providers.AppsProvider
import dev.quxomiya.powerline.search.providers.CalculatorProvider
import dev.quxomiya.powerline.search.providers.ShareProvider
import dev.quxomiya.powerline.search.providers.WebProvider

// All providers are always on (V2 has no per-provider settings).
// Recents and System are not providers; see ROADMAP §4.4.
class ProviderRegistry(val providers: List<SearchProvider>) {

    constructor(appIndex: AppIndex) : this(
        listOf(
            AppsProvider(appIndex.apps),
            CalculatorProvider(),
            WebProvider(appIndex.availableEngines),
            ShareProvider()
        )
    )

    // An alias query ("yt laneige") is a web search only
    fun providersFor(request: SearchRequest): List<SearchProvider> = when (request.mode) {
        SearchMode.TEXT -> providers
        SearchMode.ALIAS -> providers.filter { it.section == ResultSection.WEB }
    }
}
