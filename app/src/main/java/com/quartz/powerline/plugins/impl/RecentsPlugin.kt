package com.quartz.powerline.plugins.impl

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.math.roundToInt

class RecentsPlugin(
    private val settingsRepository: SettingsRepository,
    private val visibleAppsFlow: StateFlow<List<AppInfo>>
) : Plugin {
    override val id = "recents"
    override val name = "Recent"
    override val description = "Shows recently used apps and searches"
    override val icon = Icons.Default.History

    override fun search(query: String, context: Context): List<SearchResult> {
        // Only show if query is empty
        if (query.isNotBlank()) return emptyList()

        val history = settingsRepository.getHistory()
        val maxRecent = settingsRepository.getMaxRecentItems()
        val filteredApps = visibleAppsFlow.value

        // Deduplicate by ID to show unique recent items
        val distinctHistory = history.distinctBy { it.id }.take(maxRecent)

        return distinctHistory.mapNotNull { item ->
            when (item.type) {
                ResultType.APP -> {
                    if (item.id == "settings_action") {
                        SearchResult(
                            id = "settings_action",
                            pluginId = "system",
                            title = item.title,
                            subtitle = item.subtitle ?: "Recent",
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
                                title = item.title,
                                subtitle = item.subtitle ?: "Recent",
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
                        subtitle = item.subtitle ?: "Recent Web",
                        type = ResultType.WEB,
                        payload = item.payload ?: item.title,
                        faviconUrl = item.faviconUrl
                    )
                }
                ResultType.CALC -> {
                    SearchResult(
                        id = item.id,
                        pluginId = "calculator",
                        title = item.title,
                        subtitle = item.subtitle ?: "Recent Calc",
                        type = ResultType.CALC,
                        payload = item.payload ?: item.title
                    )
                }
                ResultType.SHARE -> {
                    SearchResult(
                        id = item.id,
                        pluginId = "share",
                        title = item.title,
                        subtitle = item.subtitle ?: "Recent Share",
                        type = ResultType.SHARE,
                        payload = item.payload ?: item.title
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
        var recordHistory by remember { mutableStateOf(settingsRepository.getHistoryEnabled()) }
        var maxRecentItems by remember { mutableFloatStateOf(settingsRepository.getMaxRecentItems().toFloat()) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ListItem(
                headlineContent = { Text("Record History") },
                supportingContent = { Text("Enable saving new items to history.") },
                trailingContent = {
                    Switch(
                        checked = recordHistory,
                        onCheckedChange = {
                            recordHistory = it
                            settingsRepository.setHistoryEnabled(it)
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ListItem(
                headlineContent = { Text("Number of Recent Items: ${maxRecentItems.roundToInt()}") },
                supportingContent = {
                    Slider(
                        value = maxRecentItems,
                        onValueChange = {
                            maxRecentItems = it
                            settingsRepository.setMaxRecentItems(it.roundToInt())
                        },
                        valueRange = 1f..20f,
                        steps = 19
                    )
                }
            )
        }
    }
}
