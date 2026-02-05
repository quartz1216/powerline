package com.quartz.powerline.plugins.impl

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
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
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

class AppLauncherPlugin(
    private val settingsRepository: SettingsRepository,
    private val installedAppsFlow: StateFlow<List<AppInfo>>
) : Plugin {
    override val id = "app_launcher"
    override val name = "Apps"
    override val description = "Search and launch installed applications"
    override val icon = Icons.Default.Apps

    override fun search(query: String, context: Context): List<SearchResult> {
        val apps = installedAppsFlow.value
        val hiddenApps = settingsRepository.getHiddenApps()
        val visibleApps = apps.filter { !hiddenApps.contains(it.packageName) }

        if (query.isBlank()) return emptyList()

        val trimmedQuery = query.trim()

        val aliases = settingsRepository.getAliases()
        val favorites = settingsRepository.getFavoriteApps()
        val weights = settingsRepository.getAppWeights()
        val enableWeightSearch = settingsRepository.getEnableWeightSearch()
        val enablePackageSearch = settingsRepository.getEnablePackageSearch()

        val aliasPackage = aliases[trimmedQuery]

        val appMatches = visibleApps.mapNotNull { app ->
            val isFavorite = favorites.contains(app.packageName)
            val isAlias = aliasPackage == app.packageName

            var finalScore = 0
            var matched = false

            if (isAlias) {
                finalScore = 500_000
                matched = true
            } else {
                val nameScore = fuzzyMatch(trimmedQuery, app.label)

                var pkgScore = 0
                if (enablePackageSearch) {
                    val searchPkg = if (app.packageName.contains('.')) {
                        app.packageName.substringAfter('.')
                    } else {
                        app.packageName
                    }
                    pkgScore = fuzzyMatch(trimmedQuery, searchPkg)
                }

                val matchScore = max(nameScore, pkgScore)

                if (matchScore > 0) {
                    var baseScore = matchScore

                    if (enableWeightSearch) {
                        baseScore += (weights[app.packageName] ?: 0)
                    }

                    if (isFavorite) {
                        baseScore += 100_000
                    }

                    finalScore = baseScore
                    matched = true
                }
            }

            if (matched) {
                app to finalScore
            } else {
                null
            }
        }.sortedWith(
            compareByDescending<Pair<AppInfo, Int>> { it.second }
                .thenBy { it.first.label }
        )

        return appMatches.map { (app, _) ->
            val subtitle = if (aliasPackage == app.packageName) {
                "Alias: ${app.packageName}"
            } else {
                app.packageName
            }

            SearchResult(
                id = app.packageName,
                pluginId = id,
                title = app.label,
                subtitle = subtitle,
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

        // Exact, Prefix, Substring
        if (t.contains(q)) return 100

        // Discontinuous (Subsequence)
        var qIdx = 0
        var tIdx = 0
        while (qIdx < q.length && tIdx < t.length) {
            if (q[qIdx] == t[tIdx]) {
                qIdx++
            }
            tIdx++
        }
        if (qIdx == q.length) return 50

        return 0
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        if (result.packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
            if (intent != null) {
                context.startActivity(intent)

                // Update weights
                if (settingsRepository.getEnableWeightSearch()) {
                    val weights = settingsRepository.getAppWeights().toMutableMap()

                    // Decay all existing weights and add newly executed app if missing
                    val allApps = weights.keys.toMutableSet()
                    allApps.add(result.packageName)

                    for (pkg in allApps) {
                        val current = weights[pkg] ?: 0
                        var next = (current * 0.95).toInt()

                        if (pkg == result.packageName) {
                            next += 5
                        }

                        // Optional: Clean up 0 weights to keep map small?
                        // Requirement doesn't say, but keeping them allows them to decay properly if they were high.
                        // However, 0 * 0.95 is 0.
                        if (next > 0) {
                            weights[pkg] = next
                        } else {
                            weights.remove(pkg)
                        }
                    }

                    settingsRepository.setAppWeights(weights)
                }

                return true
            } else {
                Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        var hiddenApps by remember { mutableStateOf(settingsRepository.getHiddenApps()) }
        var aliases by remember { mutableStateOf(settingsRepository.getAliases()) }
        var enableWeightSearch by remember { mutableStateOf(settingsRepository.getEnableWeightSearch()) }
        var enablePackageSearch by remember { mutableStateOf(settingsRepository.getEnablePackageSearch()) }
        val allApps by installedAppsFlow.collectAsState()

        var showHiddenAppsDialog by remember { mutableStateOf(false) }
        var showAddAliasDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("App Launcher Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Search Options
            Text("Search Options", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Weight Search")
                Switch(
                    checked = enableWeightSearch,
                    onCheckedChange = {
                        enableWeightSearch = it
                        settingsRepository.setEnableWeightSearch(it)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Package Search")
                Switch(
                    checked = enablePackageSearch,
                    onCheckedChange = {
                        enablePackageSearch = it
                        settingsRepository.setEnablePackageSearch(it)
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

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
