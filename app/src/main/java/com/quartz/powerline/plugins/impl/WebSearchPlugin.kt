package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.CustomSearchEngine
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class WebSearchPlugin(private val settingsRepository: SettingsRepository) : Plugin {
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

        // Search Engines
        val engines = settingsRepository.getCustomSearchEngines().filter { it.isEnabled }

        // Check for aliases (prefix + space)
        val aliasMatches = engines.filter { !it.alias.isNullOrBlank() && query.startsWith("${it.alias} ") }

        if (aliasMatches.isNotEmpty()) {
            // Use specific engine(s)
            for (engine in aliasMatches) {
                val realQuery = query.substring(engine.alias!!.length + 1).trim()
                if (realQuery.isBlank()) continue

                val finalUrl = engine.url.replace("%s", Uri.encode(realQuery))
                results.add(
                    SearchResult(
                        id = "web_search:${engine.id}:$realQuery",
                        pluginId = id,
                        title = realQuery,
                        subtitle = engine.name,
                        type = ResultType.WEB,
                        payload = finalUrl
                    )
                )
            }
        } else {
            // Use default engines (no alias required)
            val defaultEngines = engines.filter { it.alias.isNullOrBlank() }

            for (engine in defaultEngines) {
                val finalUrl = engine.url.replace("%s", Uri.encode(trimmedQuery))
                results.add(
                    SearchResult(
                        id = "web_search:${engine.id}:$trimmedQuery",
                        pluginId = id,
                        title = trimmedQuery,
                        subtitle = engine.name,
                        type = ResultType.WEB,
                        payload = finalUrl
                    )
                )
            }
        }

        return results
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val url = if (result.payload!!.startsWith("http")) result.payload else "https://www.google.com/search?q=${Uri.encode(result.payload)}"
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
        var engineToEdit by remember { mutableStateOf<CustomSearchEngine?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Web Search Engines", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(engines) { engine ->
                        ListItem(
                            modifier = Modifier.clickable {
                                engineToEdit = engine
                                showDialog = true
                            },
                            headlineContent = { Text(engine.name) },
                            supportingContent = {
                                Column {
                                    Text(engine.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (!engine.alias.isNullOrBlank()) {
                                        Text("Alias: ${engine.alias}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = engine.isEnabled,
                                    onCheckedChange = { isEnabled ->
                                        val updated = engines.map { if (it.id == engine.id) it.copy(isEnabled = isEnabled) else it }
                                        settingsRepository.setCustomSearchEngines(updated)
                                        engines = updated
                                    }
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    engineToEdit = null
                    showDialog = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Engine")
            }
        }

        if (showDialog) {
            AddEditEngineDialog(
                engine = engineToEdit,
                onDismiss = { showDialog = false },
                onSave = { newEngine ->
                    val updated = if (engineToEdit != null) {
                        engines.map { if (it.id == engineToEdit!!.id) newEngine else it }
                    } else {
                        engines + newEngine
                    }
                    settingsRepository.setCustomSearchEngines(updated)
                    engines = updated
                    showDialog = false
                },
                onDelete = if (engineToEdit != null) {
                    {
                        val updated = engines.filter { it.id != engineToEdit!!.id }
                        settingsRepository.setCustomSearchEngines(updated)
                        engines = updated
                        showDialog = false
                    }
                } else null
            )
        }
    }

    @Composable
    private fun AddEditEngineDialog(
        engine: CustomSearchEngine?,
        onDismiss: () -> Unit,
        onSave: (CustomSearchEngine) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        var name by remember { mutableStateOf(engine?.name ?: "") }
        var url by remember { mutableStateOf(engine?.url ?: "") }
        var alias by remember { mutableStateOf(engine?.alias ?: "") }
        var isEnabled by remember { mutableStateOf(engine?.isEnabled ?: true) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (engine == null) "Add Search Engine" else "Edit Search Engine") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL (use %s)") },
                        placeholder = { Text("https://example.com/search?q=%s") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text("Alias (optional)") },
                        placeholder = { Text("e.g. ytm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            onSave(
                                CustomSearchEngine(
                                    id = engine?.id ?: java.util.UUID.randomUUID().toString(),
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
                Row {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
