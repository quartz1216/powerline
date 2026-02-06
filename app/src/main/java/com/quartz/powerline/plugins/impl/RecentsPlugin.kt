package com.quartz.powerline.plugins.impl

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.StateFlow

class RecentsPlugin(
    private val settingsRepository: SettingsRepository,
    private val installedAppsFlow: StateFlow<List<AppInfo>>
) : Plugin {
    override val id = "recents"
    override val name = "Recent Activity"
    override val description = "Shows recently used apps and searches"
    override val icon = Icons.Default.History

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isNotBlank()) return emptyList()

        val history = settingsRepository.getHistory()
        val maxRecent = settingsRepository.getMaxRecentItems()
        val visibleApps = installedAppsFlow.value
        val hiddenApps = settingsRepository.getHiddenApps()
        val filteredApps = visibleApps.filter { !hiddenApps.contains(it.packageName) }

        // Deduplicate by ID to show unique recent items
        val distinctHistory = history.distinctBy { it.id }.take(maxRecent)

        return distinctHistory.mapNotNull { item ->
            when (item.type) {
                ResultType.APP -> {
                    if (item.id == "settings_action") {
                        SearchResult(
                            id = "settings_action",
                            pluginId = "system",
                            title = "Settings",
                            subtitle = "Recent",
                            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_preferences),
                            type = ResultType.APP,
                            payload = "settings_action"
                        )
                    } else {
                        val app = filteredApps.find { it.packageName == item.id }
                        if (app != null) {
                            SearchResult(
                                id = app.packageName,
                                pluginId = "app_launcher",
                                title = app.label,
                                subtitle = "Recent",
                                icon = app.icon,
                                type = ResultType.APP,
                                packageName = app.packageName,
                                className = app.className
                            )
                        } else null
                    }
                }
                ResultType.WEB -> {
                    SearchResult(
                        id = item.id,
                        pluginId = "web_search",
                        title = item.title,
                        subtitle = "Recent Web",
                        type = ResultType.WEB,
                        payload = item.title
                    )
                }
                ResultType.CALC -> {
                    SearchResult(
                        id = item.id,
                        pluginId = "calculator",
                        title = item.title,
                        subtitle = "Recent Calc",
                        type = ResultType.CALC,
                        payload = item.title
                    )
                }
                ResultType.MEMO -> {
                    SearchResult(
                        id = item.id,
                        pluginId = "memo",
                        title = item.title,
                        subtitle = "Recent Memo",
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit),
                        type = ResultType.MEMO,
                        payload = item.title
                    )
                }
                ResultType.INTEGRATION -> {
                    val query = item.id.removePrefix("integration_gemini:")
                    SearchResult(
                        id = item.id,
                        pluginId = "integration",
                        title = item.title,
                        subtitle = query,
                        type = ResultType.INTEGRATION,
                        payload = query
                    )
                }
            }
        }
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        // Recents results are delegating to other plugins based on their pluginId
        // But ActionExecutor will handle that.
        return false
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Recent Activity Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("This plugin displays your recent history.")
        }
    }
}
