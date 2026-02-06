package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchEngine
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import java.util.UUID

class WebSearchPlugin : Plugin {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "Search the web or open URLs"
    override val icon = Icons.Default.Search

    private val urlPattern = java.util.regex.Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(/.*)?$"
    )

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val trimmedQuery = query.trim()
        val results = mutableListOf<SearchResult>()

        // Direct URL
        if (urlPattern.matcher(trimmedQuery).matches()) {
            results.add(
                SearchResult(
                    id = "web_direct",
                    pluginId = id,
                    title = "Go to $trimmedQuery",
                    subtitle = "Web Address",
                    type = ResultType.WEB,
                    payload = if (trimmedQuery.startsWith("http")) trimmedQuery else "https://$trimmedQuery"
                )
            )
        }

        // Custom Search Engines
        val repo = SettingsRepository(context)
        val engines = repo.getCustomSearchEngines()

        engines.filter { it.isEnabled }.forEach { engine ->
            results.add(
                SearchResult(
                    id = "web_engine:${engine.id}:$trimmedQuery",
                    pluginId = id,
                    title = engine.title,
                    subtitle = trimmedQuery,
                    type = ResultType.WEB,
                    payload = "engine:${engine.id}:$trimmedQuery"
                )
            )
        }

        return results
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val payload = result.payload ?: return false

        val url = if (payload.startsWith("engine:")) {
            val parts = payload.split(":", limit = 3)
            if (parts.size < 3) return false
            val engineId = parts[1]
            val query = parts[2]

            val repo = SettingsRepository(context)
            val engine = repo.getCustomSearchEngines().find { it.id == engineId } ?: return false

            try {
                engine.urlTemplate.replace("%s", Uri.encode(query))
            } catch (e: Exception) {
                return false
            }
        } else {
             // Direct URL or legacy fallback
             if (payload.startsWith("http")) payload else "https://$payload"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No browser/app found", Toast.LENGTH_SHORT).show()
            false
        }
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        var engines by remember { mutableStateOf(viewModel.settingsRepository.getCustomSearchEngines()) }
        var showDialog by remember { mutableStateOf(false) }
        var engineToEdit by remember { mutableStateOf<SearchEngine?>(null) }

        if (showDialog || engineToEdit != null) {
            com.quartz.powerline.ui.components.SearchEngineDialog(
                engine = engineToEdit,
                onDismiss = {
                    showDialog = false
                    engineToEdit = null
                },
                onConfirm = { title, url ->
                    val newEngines = engines.toMutableList()
                    if (engineToEdit != null) {
                        // Edit
                        val index = newEngines.indexOfFirst { it.id == engineToEdit!!.id }
                        if (index != -1) {
                            newEngines[index] = engineToEdit!!.copy(title = title, urlTemplate = url)
                        }
                    } else {
                        // Add
                        newEngines.add(SearchEngine(title = title, urlTemplate = url))
                    }
                    engines = newEngines
                    viewModel.settingsRepository.setCustomSearchEngines(newEngines)
                    showDialog = false
                    engineToEdit = null
                },
                onDelete = {
                     if (engineToEdit != null) {
                        val newEngines = engines.toMutableList()
                        newEngines.removeIf { it.id == engineToEdit!!.id }
                        engines = newEngines
                        viewModel.settingsRepository.setCustomSearchEngines(newEngines)
                        showDialog = false
                        engineToEdit = null
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Web Search Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Search Engine")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(engines) { engine ->
                    ListItem(
                        headlineContent = { Text(engine.title) },
                        supportingContent = {
                            Text(
                                engine.urlTemplate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Switch(
                                checked = engine.isEnabled,
                                onCheckedChange = { isChecked ->
                                    val newEngines = engines.toMutableList()
                                    val index = newEngines.indexOfFirst { it.id == engine.id }
                                    if (index != -1) {
                                        newEngines[index] = engine.copy(isEnabled = isChecked)
                                        engines = newEngines
                                        viewModel.settingsRepository.setCustomSearchEngines(newEngines)
                                    }
                                }
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { engineToEdit = engine }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    )
                }
            }
        }
    }
}
