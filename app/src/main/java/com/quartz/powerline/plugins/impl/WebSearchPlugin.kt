package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchEngine
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import java.util.UUID

class WebSearchPlugin(
    private val settingsRepository: SettingsRepository
) : Plugin {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "Search the web or open URLs"

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
        val engines = settingsRepository.getCustomSearchEngines()
        for (engine in engines) {
            if (!engine.isEnabled) continue

            val alias = engine.alias
            // Check for alias match
            if (!alias.isNullOrBlank() && trimmedQuery.startsWith("$alias ", ignoreCase = true)) {
                val queryPart = trimmedQuery.substring(alias.length + 1)
                if (queryPart.isNotBlank()) {
                    val encodedQuery = java.net.URLEncoder.encode(queryPart, "UTF-8")
                    val url = try {
                        engine.url.format(encodedQuery)
                    } catch (e: Exception) {
                        engine.url.replace("%s", encodedQuery)
                    }
                    results.add(
                        SearchResult(
                            id = "web_search:${engine.id}:$queryPart",
                            pluginId = id,
                            title = trimmedQuery, // Topline: Textfield input
                            subtitle = engine.name, // Underline: EngineName
                            type = ResultType.WEB,
                            payload = url
                        )
                    )
                }
            }
            // Check for default (no alias)
            else if (alias.isNullOrBlank()) {
                val encodedQuery = java.net.URLEncoder.encode(trimmedQuery, "UTF-8")
                val url = try {
                    engine.url.format(encodedQuery)
                } catch (e: Exception) {
                    engine.url.replace("%s", encodedQuery)
                }
                results.add(
                    SearchResult(
                        id = "web_search:${engine.id}:$trimmedQuery",
                        pluginId = id,
                        title = trimmedQuery,
                        subtitle = engine.name,
                        type = ResultType.WEB,
                        payload = url
                    )
                )
            }
        }

        return results
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        // If payload is a full URL (http/https), use it.
        // Otherwise fallback to Google (legacy support for old history items)
        val url = if (result.payload?.startsWith("http") == true) {
            result.payload
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(result.payload ?: "", "UTF-8")}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            false
        }
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        var engines by remember { mutableStateOf(settingsRepository.getCustomSearchEngines()) }
        var showDialog by remember { mutableStateOf(false) }
        var editingEngine by remember { mutableStateOf<SearchEngine?>(null) }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editingEngine = null
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Search Engine")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Text(
                        text = "Custom Search Engines",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(engines) { engine ->
                    ListItem(
                        headlineContent = { Text(engine.name) },
                        supportingContent = { Text(engine.url) },
                        leadingContent = {
                            if (!engine.alias.isNullOrBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = engine.alias,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = engine.isEnabled,
                                    onCheckedChange = { isEnabled ->
                                        val newEngines = engines.map {
                                            if (it.id == engine.id) it.copy(isEnabled = isEnabled) else it
                                        }
                                        engines = newEngines
                                        settingsRepository.setCustomSearchEngines(newEngines)
                                    }
                                )
                                IconButton(onClick = {
                                    val newEngines = engines.filter { it.id != engine.id }
                                    engines = newEngines
                                    settingsRepository.setCustomSearchEngines(newEngines)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            editingEngine = engine
                            showDialog = true
                        }
                    )
                    Divider()
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                }
            }
        }

        if (showDialog) {
            EngineDialog(
                engine = editingEngine,
                onDismiss = { showDialog = false },
                onSave = { engine ->
                    val newEngines = if (editingEngine == null) {
                        engines + engine
                    } else {
                        engines.map { if (it.id == engine.id) engine else it }
                    }
                    engines = newEngines
                    settingsRepository.setCustomSearchEngines(newEngines)
                    showDialog = false
                }
            )
        }
    }

    @Composable
    fun EngineDialog(
        engine: SearchEngine?,
        onDismiss: () -> Unit,
        onSave: (SearchEngine) -> Unit
    ) {
        var name by remember { mutableStateOf(engine?.name ?: "") }
        var url by remember { mutableStateOf(engine?.url ?: "") }
        var alias by remember { mutableStateOf(engine?.alias ?: "") }
        var isEnabled by remember { mutableStateOf(engine?.isEnabled ?: true) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (engine == null) "Add Search Engine" else "Edit Search Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL (use %s for query)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text("Alias (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { isEnabled = !isEnabled }
                    ) {
                        Text("Enabled", modifier = Modifier.weight(1f))
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            onSave(
                                SearchEngine(
                                    id = engine?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    url = url,
                                    alias = alias.takeIf { it.isNotBlank() },
                                    isEnabled = isEnabled
                                )
                            )
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
