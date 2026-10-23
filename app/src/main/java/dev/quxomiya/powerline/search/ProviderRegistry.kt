package dev.quxomiya.powerline.search

import dev.quxomiya.powerline.search.providers.AppsProvider
import dev.quxomiya.powerline.search.providers.CalculatorProvider
import dev.quxomiya.powerline.search.providers.ShareProvider
import dev.quxomiya.powerline.search.providers.WebProvider

// All providers are registered; a built-in switched off in settings returns nothing.
// Recents and System are not providers; see ROADMAP §4.4.
class ProviderRegistry(val providers: List<SearchProvider>) {

    constructor(
        appIndex: AppIndex,
        isEnabled: (BuiltIn) -> Boolean = { true },
        showPackageName: () -> Boolean = { true }
    ) : this(
        listOf(
            AppsProvider(appIndex.apps, { isEnabled(BuiltIn.APPS) }, showPackageName),
            CalculatorProvider { isEnabled(BuiltIn.CALCULATOR) },
            WebProvider(appIndex.availableEngines) { isEnabled(BuiltIn.WEB) },
            ShareProvider(appIndex.availableShareTargets) { isEnabled(BuiltIn.SHARE) }
        )
    )

    // An engine alias ("yt laneige") is a web search only; a share alias ("cl hello") a share only
    fun providersFor(request: SearchRequest): List<SearchProvider> = when {
        request.shareTarget != null -> providers.filter { it.section == ResultSection.SHARE }
        request.engine != null -> providers.filter { it.section == ResultSection.WEB }
        else -> providers
    }
}
