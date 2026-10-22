package dev.quxomiya.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.quxomiya.powerline.data.SearchSettingsRepository
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.search.AppIndex
import dev.quxomiya.powerline.search.AliasChip
import dev.quxomiya.powerline.search.BuiltIn
import dev.quxomiya.powerline.search.ProviderRegistry
import dev.quxomiya.powerline.search.QueryParser
import dev.quxomiya.powerline.search.RankingSignals
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchPipeline
import dev.quxomiya.powerline.search.SearchUiState
import dev.quxomiya.powerline.search.providers.toSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    val userDataRepository = UserDataRepository(application)
    private val searchSettings = SearchSettingsRepository.get(application)
    private val appIndex = AppIndex(application, viewModelScope, searchSettings.engines, searchSettings.shareTargets)

    // Launchable apps by name (the standby "all apps" grid)
    private val _visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val visibleApps: StateFlow<List<AppInfo>> = _visibleApps.asStateFlow()

    // Apps launched from Powerline, most recent first (shown in the standby state)
    private val _recentApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val recentApps: StateFlow<List<AppInfo>> = _recentApps.asStateFlow()

    // Aliased search engines, then share targets, each only while its built-in is on
    private val aliasChips: StateFlow<List<AliasChip>> =
        combine(
            appIndex.availableEngines,
            appIndex.availableShareTargets,
            searchSettings.disabledBuiltIns
        ) { allEngines, allTargets, disabled ->
            val engines = if (BuiltIn.WEB in disabled) emptyList() else allEngines
            val targets = if (BuiltIn.SHARE in disabled) emptyList() else allTargets
            engines.mapNotNull { available ->
                val engine = available.engine
                val alias = engine.alias?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AliasChip("web:${engine.id}", engine.name, alias, available.icon, isShare = false)
            } + targets.map { available ->
                val target = available.target
                AliasChip("share:${target.id}", target.name, target.alias, available.icon, isShare = true)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Standby chips: the alias chips, none at all with Chips off
    val chips: StateFlow<List<AliasChip>> =
        combine(aliasChips, searchSettings.disabledBuiltIns) { chips, disabled ->
            if (BuiltIn.CHIPS in disabled) emptyList() else chips
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // The standby app grid (recent row and all apps), off with the Home switch
    val homeEnabled: StateFlow<Boolean> = searchSettings.disabledBuiltIns
        .map { BuiltIn.HOME !in it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, searchSettings.isEnabled(BuiltIn.HOME))

    // A switched-off built-in's aliases read as plain text
    private val parser = QueryParser(
        { if (searchSettings.isEnabled(BuiltIn.WEB)) searchSettings.engines.value else emptyList() },
        { if (searchSettings.isEnabled(BuiltIn.SHARE)) searchSettings.shareTargets.value else emptyList() }
    )

    private val pipeline = SearchPipeline(
        context = application,
        registry = ProviderRegistry(appIndex, searchSettings::isEnabled) { searchSettings.showPackageName.value },
        parser = parser,
        signals = {
            RankingSignals(
                favoriteApps = userDataRepository.getFavoriteApps(),
                appFrecency = userDataRepository.getAppFrecency()
            )
        }
    )

    // The whole query the pipeline sees, alias included ("yt lofi")
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Once an alias is typed with its space, it shows as this chip in the search field and the
    // field holds only the rest ("lofi")
    private val _aliasChip = MutableStateFlow<AliasChip?>(null)
    val aliasChip: StateFlow<AliasChip?> = _aliasChip.asStateFlow()

    private val _fieldText = MutableStateFlow("")
    val fieldText: StateFlow<String> = _fieldText.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Initial load and every reload after a package change
        viewModelScope.launch {
            appIndex.apps.collect { indexed ->
                _visibleApps.value = indexed.map { it.app }
                refreshRecentApps()
                performSearch(_query.value, debounce = false)
            }
        }
        // Redraw open results when a built-in or the package name switch changes. Re-split too:
        // the field's alias chip goes when its built-in is switched off.
        viewModelScope.launch {
            combine(searchSettings.disabledBuiltIns, searchSettings.showPackageName) { _, _ -> }
                .drop(1)
                .collect { setQuery(_query.value, debounce = false) }
        }
    }

    // A whole query: cleared, or "<alias> " from a standby chip
    fun onQueryChange(newQuery: String) = setQuery(newQuery, debounce = true)

    // An edit in the search field, which holds the text after the alias chip
    fun onFieldChange(text: String) {
        val chip = _aliasChip.value
        setQuery(if (chip != null) "${chip.alias} $text" else text, debounce = true)
    }

    // Backspace in the empty field: the chip turns back into its alias text, minus the space
    fun removeAliasChip() {
        val chip = _aliasChip.value ?: return
        setQuery(chip.alias, debounce = true)
    }

    private fun setQuery(newQuery: String, debounce: Boolean) {
        val split = parser.splitAlias(newQuery)
        val chip = split?.first?.let { request ->
            val id = request.shareTarget?.let { "share:${it.id}" } ?: "web:${request.engine?.id}"
            // Without its icon until AppIndex has reloaded an alias just added in settings
            aliasChips.value.find { it.id == id }
                ?: request.shareTarget?.let { AliasChip(id, it.name, it.alias, null, isShare = true) }
                ?: request.engine?.let { AliasChip(id, it.name, it.alias.orEmpty(), null, isShare = false) }
        }
        val fieldText = if (chip != null) split.second else newQuery
        // Stored as typed, the alias part normalized to the chip's alias
        val query = if (chip != null) "${chip.alias} $fieldText" else newQuery
        _aliasChip.value = chip
        _fieldText.value = fieldText
        _query.value = query
        performSearch(query, debounce)
    }

    private val _openCount = MutableStateFlow(0)
    val openCount: StateFlow<Int> = _openCount.asStateFlow()

    // Called when the overlay is re-shown: start from an empty query and re-request focus
    fun resetForOpen() {
        onQueryChange("")
        refreshRecentApps()
        _openCount.value += 1
    }

    private fun refreshRecentApps() {
        val apps = _visibleApps.value
        val appsByPackage = apps.groupBy { it.packageName }
        _recentApps.value = userDataRepository.getHistory()
            .asSequence()
            // The launched activity, or the package's first one for V1 entries without a class
            .mapNotNull { item ->
                val candidates = appsByPackage[item.packageName] ?: return@mapNotNull null
                candidates.find { it.className == item.className } ?: candidates.first()
            }
            .distinct()
            .take(PowerlineDefaults.MAX_STANDBY_RECENT_APPS)
            .toList()
    }

    fun appItem(app: AppInfo): SearchItem = app.toSearchItem(showPackageName = searchSettings.showPackageName.value)

    fun selectNext() {
        _uiState.update { state ->
            if (state.selectedIndex < state.items.lastIndex) state.copy(selectedIndex = state.selectedIndex + 1) else state
        }
    }

    fun selectPrevious() {
        _uiState.update { state ->
            if (state.selectedIndex > 0) state.copy(selectedIndex = state.selectedIndex - 1) else state
        }
    }

    fun selectNextSection() {
        _uiState.update { it.copy(selectedIndex = it.nextSectionStart()) }
    }

    fun selectPreviousSection() {
        _uiState.update { it.copy(selectedIndex = it.previousSectionStart()) }
    }

    // Cancels the previous search right away, then waits out the debounce before searching
    private fun performSearch(query: String, debounce: Boolean = true) {
        searchJob?.cancel()
        if (query.isBlank()) {
            // Back to standby: drop the old results now so they never flash on the next keystroke
            _uiState.value = SearchUiState()
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        // Runs on Main like the cancel above, so a cancelled search can never publish its results
        searchJob = viewModelScope.launch {
            if (debounce) delay(PowerlineDefaults.SEARCH_DEBOUNCE_MS)
            val sections = withContext(Dispatchers.Default) { pipeline.search(query) }
            _uiState.value = SearchUiState(sections = sections)
        }
    }

    fun toggleFavorite(packageName: String) {
        val current = userDataRepository.getFavoriteApps().toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        userDataRepository.setFavoriteApps(current)
        // Refresh search to update ordering
        performSearch(_query.value, debounce = false)
    }

    fun isFavorite(packageName: String): Boolean {
        return userDataRepository.getFavoriteApps().contains(packageName)
    }

    override fun onCleared() {
        appIndex.close()
        pipeline.close()
    }
}
