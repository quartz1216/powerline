package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.AppRepository
import com.quartz.powerline.domain.SearchEngine
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    LAUNCHER,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)
    private val searchEngine = SearchEngine(application, settingsRepository)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.LAUNCHER)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        loadApps()
    }

    fun reloadApps() {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.getInstalledApps()
            _installedApps.value = apps
            // Initial search (empty)
            performSearch(_query.value)
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        performSearch(newQuery)
    }

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

    fun openSettings() {
        _currentScreen.value = Screen.SETTINGS
    }

    fun closeSettings() {
        _currentScreen.value = Screen.LAUNCHER
        // Reload apps in case settings changed (hidden apps)
        // And refresh search
        reloadApps()
    }

    private fun performSearch(query: String) {
        _results.value = searchEngine.search(query, _installedApps.value)
        _selectedIndex.value = 0
    }
}
