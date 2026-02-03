package com.quartz.powerline.plugins.impl

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.AddAliasDialog
import com.quartz.powerline.ui.HiddenAppsDialog
import com.quartz.powerline.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.StateFlow

class AppLauncherPlugin(
    private val settingsRepository: SettingsRepository,
    private val installedAppsFlow: StateFlow<List<AppInfo>>
) : Plugin {
    override val id = "app_launcher"
    override val name = "Apps"
    override val description = "Search and launch installed applications"

    override fun search(query: String, context: Context): List<SearchResult> {
        val apps = installedAppsFlow.value
        val hiddenApps = settingsRepository.getHiddenApps()
        val visibleApps = apps.filter { !hiddenApps.contains(it.packageName) }

        if (query.isBlank()) return emptyList() // Recents handled elsewhere or not at all by this plugin

        val trimmedQuery = query.trim()

        val aliases = settingsRepository.getAliases()
        val history = settingsRepository.getHistory()
        val scores = history.groupingBy { it.id }.eachCount()

        val aliasPackage = aliases[trimmedQuery]

        val appMatches = visibleApps.mapNotNull { app ->
            val matchAlias = aliasPackage == app.packageName
            val fuzzyScore = fuzzyMatch(trimmedQuery, app.label)

            if (matchAlias) {
                app to Int.MAX_VALUE
            } else if (fuzzyScore > 0) {
                app to fuzzyScore
            } else {
                null
            }
        }.sortedWith(
            compareByDescending<Pair<AppInfo, Int>> { it.second }
                .thenByDescending { scores[it.first.packageName] ?: 0 }
                .thenBy { it.first.label }
        )

        return appMatches.map { (app, _) ->
            SearchResult(
                id = app.packageName,
                pluginId = id,
                title = app.label,
                subtitle = if (aliasPackage == app.packageName) "Alias" else "Application",
                icon = app.icon,
                type = ResultType.APP,
                packageName = app.packageName,
                className = app.className
            )
        }
    }

    private fun fuzzyMatch(query: String, target: String): Int {
        if (query.isEmpty()) return 0
        val q = query.lowercase()
        val t = target.lowercase()
        if (t == q) return 1000
        if (t.startsWith(q)) return 100
        if (t.contains(q)) return 50
        var qIdx = 0
        var tIdx = 0
        var matches = 0
        while (qIdx < q.length && tIdx < t.length) {
            if (q[qIdx] == t[tIdx]) {
                matches++
                qIdx++
            }
            tIdx++
        }
        return if (matches == q.length) 10 else 0
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        if (result.packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
            if (intent != null) {
                context.startActivity(intent)
                return true
            } else {
                Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    @Composable
    override fun SettingsScreen(viewModel: MainViewModel) {
        var hiddenApps by remember { mutableStateOf(settingsRepository.getHiddenApps()) }
        var aliases by remember { mutableStateOf(settingsRepository.getAliases()) }
        val allApps by installedAppsFlow.collectAsState()

        var showHiddenAppsDialog by remember { mutableStateOf(false) }
        var showAddAliasDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("App Launcher Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Hidden Apps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hidden Apps", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showHiddenAppsDialog = true }) {
                    Text("Manage (${hiddenApps.size})")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Aliases
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aliases", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showAddAliasDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alias")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(aliases.toList()) { (alias, packageName) ->
                    val appName = allApps.find { it.packageName == packageName }?.label ?: packageName
                    ListItem(
                        headlineContent = { Text(alias) },
                        supportingContent = { Text(appName) },
                        trailingContent = {
                            IconButton(onClick = {
                                val newAliases = aliases.toMutableMap()
                                newAliases.remove(alias)
                                aliases = newAliases
                                settingsRepository.setAliases(newAliases)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    )
                    Divider()
                }
            }
        }

        if (showHiddenAppsDialog) {
            HiddenAppsDialog(
                allApps = allApps,
                hiddenApps = hiddenApps,
                onDismiss = { showHiddenAppsDialog = false },
                onSave = { newHidden ->
                    hiddenApps = newHidden
                    settingsRepository.setHiddenApps(newHidden)
                    showHiddenAppsDialog = false
                    viewModel.reloadApps()
                }
            )
        }

        if (showAddAliasDialog) {
            AddAliasDialog(
                allApps = allApps,
                onDismiss = { showAddAliasDialog = false },
                onSave = { alias, pkg ->
                    val newAliases = aliases.toMutableMap()
                    newAliases[alias] = pkg
                    aliases = newAliases
                    settingsRepository.setAliases(newAliases)
                    showAddAliasDialog = false
                }
            )
        }
    }
}
