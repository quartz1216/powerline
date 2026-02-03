package com.quartz.powerline.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quartz.powerline.ui.viewmodel.MainViewModel
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.CustomPlugin
import com.quartz.powerline.domain.models.PluginAction
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var maxRecent by remember { mutableStateOf(viewModel.settingsRepository.getMaxRecentItems()) }
    var hiddenApps by remember { mutableStateOf(viewModel.settingsRepository.getHiddenApps()) }
    var aliases by remember { mutableStateOf(viewModel.settingsRepository.getAliases()) }
    var plugins by remember { mutableStateOf(viewModel.settingsRepository.getPlugins()) }
    val allApps by viewModel.installedApps.collectAsState()

    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showAddAliasDialog by remember { mutableStateOf(false) }
    var showAddPluginDialog by remember { mutableStateOf(false) }
    var editingPlugin by remember { mutableStateOf<CustomPlugin?>(null) }

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val json = viewModel.settingsRepository.exportSettings()
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(json.toByteArray())
                }
                Toast.makeText(context, "Settings exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { `is` ->
                    val json = `is`.bufferedReader().use { reader -> reader.readText() }
                    viewModel.settingsRepository.importSettings(json)
                    // Reload state from repository
                    maxRecent = viewModel.settingsRepository.getMaxRecentItems()
                    hiddenApps = viewModel.settingsRepository.getHiddenApps()
                    aliases = viewModel.settingsRepository.getAliases()
                    // Reload viewmodel data
                    plugins = viewModel.settingsRepository.getPlugins()
                    viewModel.reloadApps()

                    Toast.makeText(context, "Settings imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeSettings() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // General / Tools
            item {
                Text("General", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.openPermissions() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Permissions & Defaults")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("powerline_backup.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Settings")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Settings")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Max Recent Items
            item {
                Text("Max Recent Items: $maxRecent", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = maxRecent.toFloat(),
                    onValueChange = { maxRecent = it.toInt() },
                    onValueChangeFinished = { viewModel.settingsRepository.setMaxRecentItems(maxRecent) },
                    valueRange = 1f..20f,
                    steps = 19
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Hidden Apps
            item {
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
            }

            // Custom Plugins
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom Plugins", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        editingPlugin = null
                        showAddPluginDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Plugin")
                    }
                }
            }

            items(plugins) { plugin ->
                ListItem(
                    headlineContent = { Text(plugin.name) },
                    supportingContent = { Text("${plugin.keyword} -> ${plugin.action}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                editingPlugin = plugin
                                showAddPluginDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                val newPlugins = plugins.toMutableList()
                                newPlugins.remove(plugin)
                                plugins = newPlugins
                                viewModel.settingsRepository.setPlugins(newPlugins)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
                Divider()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Aliases
            item {
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
            }

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
                            viewModel.settingsRepository.setAliases(newAliases)
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
                viewModel.settingsRepository.setHiddenApps(newHidden)
                showHiddenAppsDialog = false
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
                viewModel.settingsRepository.setAliases(newAliases)
                showAddAliasDialog = false
            }
        )
    }

    if (showAddPluginDialog) {
        ManagePluginDialog(
            plugin = editingPlugin,
            onDismiss = { showAddPluginDialog = false },
            onSave = { plugin ->
                val newPlugins = plugins.toMutableList()
                val index = newPlugins.indexOfFirst { it.id == plugin.id }
                if (index != -1) {
                    newPlugins[index] = plugin
                } else {
                    newPlugins.add(plugin)
                }
                plugins = newPlugins
                viewModel.settingsRepository.setPlugins(newPlugins)
                showAddPluginDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePluginDialog(
    plugin: CustomPlugin?,
    onDismiss: () -> Unit,
    onSave: (CustomPlugin) -> Unit
) {
    var name by remember { mutableStateOf(plugin?.name ?: "") }
    var keyword by remember { mutableStateOf(plugin?.keyword ?: "") }
    var action by remember { mutableStateOf(plugin?.action ?: PluginAction.OPEN_URL) }
    var actionData by remember { mutableStateOf(plugin?.actionData ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (plugin == null) "Add Plugin" else "Edit Plugin", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Keyword (e.g. gh)") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))
                Text("Action:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = action == PluginAction.OPEN_URL, onClick = { action = PluginAction.OPEN_URL })
                    Text("Open URL")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = action == PluginAction.COPY_TO_CLIPBOARD, onClick = { action = PluginAction.COPY_TO_CLIPBOARD })
                    Text("Copy")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = actionData,
                    onValueChange = { actionData = it },
                    label = { Text("Action Data") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Use {q} for user input template") }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            onSave(CustomPlugin(
                                id = plugin?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                keyword = keyword,
                                action = action,
                                actionData = actionData
                            ))
                        },
                        enabled = name.isNotBlank() && keyword.isNotBlank() && actionData.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun HiddenAppsDialog(
    allApps: List<AppInfo>,
    hiddenApps: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val currentHidden = remember { mutableStateOf(hiddenApps) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Manage Hidden Apps", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allApps) { app ->
                        val isHidden = currentHidden.value.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = currentHidden.value.toMutableSet()
                                    if (isHidden) newSet.remove(app.packageName) else newSet.add(app.packageName)
                                    currentHidden.value = newSet
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isHidden, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(app.label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(currentHidden.value) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddAliasDialog(
    allApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Alias", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select App:", style = MaterialTheme.typography.labelLarge)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    items(allApps) { app ->
                        val isSelected = selectedApp?.packageName == app.packageName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedApp = app }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.label, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (alias.isNotBlank() && selectedApp != null) {
                                onSave(alias, selectedApp!!.packageName)
                            }
                        },
                        enabled = alias.isNotBlank() && selectedApp != null
                    ) { Text("Save") }
                }
            }
        }
    }
}
