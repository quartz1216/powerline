package com.example.commandlauncher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.commandlauncher.domain.AppRepository
import com.example.commandlauncher.domain.SearchEngine
import com.example.commandlauncher.domain.models.AppInfo
import com.example.commandlauncher.domain.models.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val searchEngine = SearchEngine()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private var allApps: List<AppInfo> = emptyList()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            allApps = appRepository.getInstalledApps()
            // Initial search (empty)
            performSearch("")
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        performSearch(newQuery)
    }

    private fun performSearch(query: String) {
        _results.value = searchEngine.search(query, allApps)
    }
}
