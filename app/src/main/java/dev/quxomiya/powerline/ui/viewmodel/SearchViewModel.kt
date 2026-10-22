package dev.quxomiya.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.search.AppIndex
import dev.quxomiya.powerline.search.AvailableEngine
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    val userDataRepository = UserDataRepository(application)
    private val appIndex = AppIndex(application, viewModelScope)

    // Launchable apps by name (the standby "all apps" grid)
    private val _visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val visibleApps: StateFlow<List<AppInfo>> = _visibleApps.asStateFlow()

    // Apps launched from Powerline, most recent first (shown in the standby state)
    private val _recentApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val recentApps: StateFlow<List<AppInfo>> = _recentApps.asStateFlow()

    // Search engines with their icons (standby chips)
    val engines: StateFlow<List<AvailableEngine>> = appIndex.availableEngines

    private val pipeline = SearchPipeline(
        context = application,
        registry = ProviderRegistry(appIndex),
        parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES),
        signals = {
            RankingSignals(
                favoriteApps = userDataRepository.getFavoriteApps(),
                appFrecency = userDataRepository.getAppFrecency()
            )
        }
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

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
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        performSearch(newQuery)
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

    fun appItem(app: AppInfo): SearchItem = app.toSearchItem()

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
