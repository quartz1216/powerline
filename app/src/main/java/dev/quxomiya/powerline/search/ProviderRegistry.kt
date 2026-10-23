package dev.quxomiya.powerline.search

// The one place the Built-in switches apply to search: a switched-off built-in's provider does not
// run. Recents and System are not providers.
class ProviderRegistry(
    val providers: List<SearchProvider>,
    private val isEnabled: (BuiltIn) -> Boolean
) {

    // An engine alias ("yt laneige") is a web search only; a share alias ("cl hello") a share only
    fun providersFor(request: SearchRequest): List<SearchProvider> {
        val enabled = providers.filter { isEnabled(it.section.builtIn) }
        return when {
            request.shareTarget != null -> enabled.filter { it.section == ResultSection.SHARE }
            request.engine != null -> enabled.filter { it.section == ResultSection.WEB }
            else -> enabled
        }
    }
}
