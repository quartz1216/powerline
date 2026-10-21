package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.AppRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.AppTheme
import com.quartz.powerline.plugins.PluginManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SettingsNav {
    MAIN,
    ADDITIONAL,
    APPEARANCE, // Keeping for now if needed, but might be merged into Overlay
    PERMISSIONS, // Keeping for now
    PLUGIN
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    val pluginManager = PluginManager(settingsRepository, _installedApps, _installedApps)

    private val _currentNav = MutableStateFlow(SettingsNav.MAIN)
    val currentNav: StateFlow<SettingsNav> = _currentNav.asStateFlow()

    private val _selectedPluginId = MutableStateFlow<String?>(null)
    val selectedPluginId: StateFlow<String?> = _selectedPluginId.asStateFlow()

    private val _homeScreenText = MutableStateFlow(settingsRepository.getHomeScreenText())
    val homeScreenText: StateFlow<String> = _homeScreenText.asStateFlow()

    private val _materialYouEnabled = MutableStateFlow(settingsRepository.getMaterialYouEnabled())
    val materialYouEnabled: StateFlow<Boolean> = _materialYouEnabled.asStateFlow()

    private val _isAdvanced = MutableStateFlow(settingsRepository.getIsAdvanced())
    val isAdvanced: StateFlow<Boolean> = _isAdvanced.asStateFlow()

    private val _appTheme = MutableStateFlow(settingsRepository.getAppTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _customAccentColor = MutableStateFlow(settingsRepository.getCustomAccentColor())
    val customAccentColor: StateFlow<Int> = _customAccentColor.asStateFlow()

    init {
        reloadApps()
    }

    fun setIsAdvanced(isAdvanced: Boolean) {
        settingsRepository.setIsAdvanced(isAdvanced)
        _isAdvanced.value = isAdvanced
    }

    fun setMaterialYouEnabled(enabled: Boolean) {
        settingsRepository.setMaterialYouEnabled(enabled)
        _materialYouEnabled.value = enabled
    }

    fun setAppTheme(theme: AppTheme) {
        settingsRepository.setAppTheme(theme)
        _appTheme.value = theme
        // Sync legacy toggle
        _materialYouEnabled.value = (theme == AppTheme.MATERIAL_YOU)
    }

    fun setCustomAccentColor(color: Int) {
        settingsRepository.setCustomAccentColor(color)
        _customAccentColor.value = color
    }

    fun reloadApps() {
        viewModelScope.launch {
            _installedApps.value = appRepository.getInstalledApps()
            // Reload settings that might have changed externally (e.g. import)
            _homeScreenText.value = settingsRepository.getHomeScreenText()
        }
    }

    fun updateHomeScreenText(text: String) {
        settingsRepository.setHomeScreenText(text)
        _homeScreenText.value = text
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

    fun openAdditionalSettings() {
        _currentNav.value = SettingsNav.ADDITIONAL
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
