package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.AppRepository
import com.quartz.powerline.domain.SearchEngine
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    LAUNCHER,
    SETTINGS,
    PERMISSIONS,
    PLUGIN_SETTINGS,
    APPEARANCE_SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _visibleApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val visibleApps: StateFlow<List<AppInfo>> = _visibleApps.asStateFlow()

    val pluginManager = com.quartz.powerline.plugins.PluginManager(settingsRepository, _installedApps, _visibleApps)
    private val searchEngine = SearchEngine(application, pluginManager, settingsRepository)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.LAUNCHER)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedPluginId = MutableStateFlow<String?>(null)
    val selectedPluginId: StateFlow<String?> = _selectedPluginId.asStateFlow()

    private val _homeScreenText = MutableStateFlow(settingsRepository.getHomeScreenText())
    val homeScreenText: StateFlow<String> = _homeScreenText.asStateFlow()

    private val _materialYouEnabled = MutableStateFlow(settingsRepository.getMaterialYouEnabled())
    val materialYouEnabled: StateFlow<Boolean> = _materialYouEnabled.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadApps()
    }

    fun refreshSettings() {
        _materialYouEnabled.value = settingsRepository.getMaterialYouEnabled()
    }

    fun reloadApps() {
        loadApps()
        // Reload settings that might have changed externally (e.g. import)
        _homeScreenText.value = settingsRepository.getHomeScreenText()
        refreshSettings()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.getInstalledApps()
            _installedApps.value = apps
            val hiddenApps = settingsRepository.getHiddenApps()
            _visibleApps.value = apps.filter { !hiddenApps.contains(it.packageName) }
            // Initial search (empty)
            performSearch(_query.value)
        }
    }

    fun updateHomeScreenText(text: String) {
        settingsRepository.setHomeScreenText(text)
        _homeScreenText.value = text
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
        if (_currentScreen.value == Screen.PLUGIN_SETTINGS || _currentScreen.value == Screen.APPEARANCE_SETTINGS) {
            _currentScreen.value = Screen.SETTINGS
            _selectedPluginId.value = null
        } else {
            _currentScreen.value = Screen.LAUNCHER
            // Reload apps in case settings changed (hidden apps)
            // And refresh search
            reloadApps()
        }
    }

    fun openAppearanceSettings() {
        _currentScreen.value = Screen.APPEARANCE_SETTINGS
    }

    fun openPluginSettings(pluginId: String) {
        _selectedPluginId.value = pluginId
        _currentScreen.value = Screen.PLUGIN_SETTINGS
    }

    fun openPermissions() {
        _currentScreen.value = Screen.PERMISSIONS
    }

    fun closePermissions() {
        _currentScreen.value = Screen.SETTINGS
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
        val current = settingsRepository.getFavoriteApps().toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        settingsRepository.setFavoriteApps(current)
        // Refresh search to update ordering
        performSearch(_query.value)
    }

    fun isFavorite(packageName: String): Boolean {
        return settingsRepository.getFavoriteApps().contains(packageName)
    }
}
