package dev.quxomiya.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.AppRepository
import dev.quxomiya.powerline.domain.SearchEngine
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.domain.models.ResultType
import dev.quxomiya.powerline.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    val userDataRepository = UserDataRepository(application)
    private val appRepository = AppRepository(application)

    private val _visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val visibleApps: StateFlow<List<AppInfo>> = _visibleApps.asStateFlow()

    // Apps launched from Powerline, most recent first (shown in the standby state)
    private val _recentApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val recentApps: StateFlow<List<AppInfo>> = _recentApps.asStateFlow()

    val pluginManager = dev.quxomiya.powerline.plugins.PluginManager(userDataRepository, _visibleApps)
    private val searchEngine = SearchEngine(application, pluginManager)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.getInstalledApps()
            _visibleApps.value = apps
            refreshRecentApps()
            // Initial search (empty)
            performSearch(_query.value)
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
        val appsByPackage = _visibleApps.value.associateBy { it.packageName }
        _recentApps.value = userDataRepository.getHistory()
            .asSequence()
            .filter { it.type == ResultType.APP }
            .map { it.id }
            .distinct()
            .mapNotNull { appsByPackage[it] }
            .take(PowerlineDefaults.MAX_STANDBY_RECENT_APPS)
            .toList()
    }

    fun appResult(app: AppInfo): SearchResult = SearchResult(
        id = app.packageName,
        pluginId = "app_launcher",
        title = app.label,
        subtitle = app.packageName,
        icon = app.icon,
        type = ResultType.APP,
        packageName = app.packageName,
        className = app.className
    )

    fun selectNext() {
        val current = _selectedIndex.value
        val max = _results.value.lastIndex
        if (current < max) {
            _selectedIndex.value = current + 1
        }
    }

    fun selectPrevious() {
        val current = _selectedIndex.value
        if (current > 0) {
            _selectedIndex.value = current - 1
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            val results = searchEngine.search(query)
            if (isActive) {
                _results.value = results
                _selectedIndex.value = 0
            }
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
        performSearch(_query.value)
    }

    fun isFavorite(packageName: String): Boolean {
        return userDataRepository.getFavoriteApps().contains(packageName)
    }
}
