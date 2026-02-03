package com.quartz.powerline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quartz.powerline.domain.AppRepository
import com.quartz.powerline.domain.SearchEngine
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.SearchResult
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

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

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
        _results.value = searchEngine.search(query, allApps)
        _selectedIndex.value = 0
    }
}
