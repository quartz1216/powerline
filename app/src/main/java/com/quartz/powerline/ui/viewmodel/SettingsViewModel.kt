package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.AppRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.plugins.PluginManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SettingsNav {
    MAIN,
    APPEARANCE,
    PERMISSIONS,
    PLUGIN
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val pluginManager = PluginManager(settingsRepository, _installedApps)

    private val _currentNav = MutableStateFlow(SettingsNav.MAIN)
    val currentNav: StateFlow<SettingsNav> = _currentNav.asStateFlow()

    private val _selectedPluginId = MutableStateFlow<String?>(null)
    val selectedPluginId: StateFlow<String?> = _selectedPluginId.asStateFlow()

    init {
        reloadApps()
    }

    fun reloadApps() {
        viewModelScope.launch {
            _installedApps.value = appRepository.getInstalledApps()
        }
    }

    fun openAppearanceSettings() {
        _currentNav.value = SettingsNav.APPEARANCE
    }

    fun openPermissions() {
        _currentNav.value = SettingsNav.PERMISSIONS
    }

    fun openPluginSettings(pluginId: String) {
        _selectedPluginId.value = pluginId
        _currentNav.value = SettingsNav.PLUGIN
    }

    fun navigateBack(): Boolean {
        return if (_currentNav.value != SettingsNav.MAIN) {
            _currentNav.value = SettingsNav.MAIN
            _selectedPluginId.value = null
            true
        } else {
            false
        }
    }
}
