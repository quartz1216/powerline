package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SettingsNav {
    MAIN,
    PERMISSIONS
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentNav = MutableStateFlow(SettingsNav.MAIN)
    val currentNav: StateFlow<SettingsNav> = _currentNav.asStateFlow()

    fun openPermissions() {
        _currentNav.value = SettingsNav.PERMISSIONS
    }

    fun navigateBack(): Boolean {
        return if (_currentNav.value != SettingsNav.MAIN) {
            _currentNav.value = SettingsNav.MAIN
            true
        } else {
            false
        }
    }
}
