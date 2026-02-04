package com.quartz.powerline.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var maxRecent by remember { mutableStateOf(viewModel.settingsRepository.getMaxRecentItems()) }
    var statusBarTapEnabled by remember { mutableStateOf(viewModel.settingsRepository.getStatusBarTapEnabled()) }
    var statusBarTriggerWidthPercent by remember { mutableStateOf(viewModel.settingsRepository.getStatusBarTriggerWidthPercent()) }
    var debugMode by remember { mutableStateOf(viewModel.settingsRepository.getDebugMode()) }

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
                    // Reload viewmodel data
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
            item {
                // General / Tools
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

                // Max Recent Items
                Text("Max Recent Items: $maxRecent", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = maxRecent.toFloat(),
                    onValueChange = { maxRecent = it.toInt() },
                    onValueChangeFinished = { viewModel.settingsRepository.setMaxRecentItems(maxRecent) },
                    valueRange = 1f..20f,
                    steps = 19
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Status Bar Tap Trigger
                Text("Features", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Status Bar Tap Trigger") },
                    supportingContent = { Text("Tap the top of the screen to open launcher.") },
                    trailingContent = {
                        Switch(
                            checked = statusBarTapEnabled,
                            onCheckedChange = {
                                statusBarTapEnabled = it
                                viewModel.settingsRepository.setStatusBarTapEnabled(it)
                                if (it && !checkAccessibilityPermission(context)) {
                                    Toast.makeText(context, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
                                    viewModel.openPermissions()
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                         val newState = !statusBarTapEnabled
                         statusBarTapEnabled = newState
                         viewModel.settingsRepository.setStatusBarTapEnabled(newState)
                         if (newState && !checkAccessibilityPermission(context)) {
                            Toast.makeText(context, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
                            viewModel.openPermissions()
                         }
                    }
                )

                if (statusBarTapEnabled) {
                    ListItem(
                        headlineContent = { Text("Trigger Width: ${statusBarTriggerWidthPercent}%") },
                        supportingContent = { Text("Aligns to the right side of the status bar.") },
                        trailingContent = {
                            // No content slot here, slider below
                        }
                    )
                    Slider(
                        value = statusBarTriggerWidthPercent.toFloat(),
                        onValueChange = { statusBarTriggerWidthPercent = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setStatusBarTriggerWidthPercent(statusBarTriggerWidthPercent) },
                        valueRange = 5f..100f,
                        steps = 19,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Debug / Tools
                Text("Debug & Maintenance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Debug Mode") },
                    supportingContent = { Text("Show toasts for accessibility events.") },
                    trailingContent = {
                        Switch(
                            checked = debugMode,
                            onCheckedChange = {
                                debugMode = it
                                viewModel.settingsRepository.setDebugMode(it)
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        val newState = !debugMode
                        debugMode = newState
                        viewModel.settingsRepository.setDebugMode(newState)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("App Info") },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("Restart App") },
                    modifier = Modifier.clickable {
                        val pm = context.packageManager
                        val intent = pm.getLaunchIntentForPackage(context.packageName)
                        val componentName = intent?.component
                        val mainIntent = Intent.makeRestartActivityTask(componentName)
                        context.startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("Reset App Settings") },
                    supportingContent = { Text("Clears all internal preferences and history.") },
                    modifier = Modifier.clickable {
                        viewModel.settingsRepository.clearAllSettings()
                        // Reload state
                        maxRecent = viewModel.settingsRepository.getMaxRecentItems()
                        statusBarTapEnabled = viewModel.settingsRepository.getStatusBarTapEnabled()
                        statusBarTriggerWidthPercent = viewModel.settingsRepository.getStatusBarTriggerWidthPercent()
                        debugMode = viewModel.settingsRepository.getDebugMode()
                        viewModel.reloadApps()
                        Toast.makeText(context, "Settings reset.", Toast.LENGTH_SHORT).show()
                    }
                )


                Spacer(modifier = Modifier.height(24.dp))

                Text("Plugins", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(viewModel.pluginManager.plugins) { plugin ->
                ListItem(
                    headlineContent = { Text(plugin.name) },
                    supportingContent = { Text(plugin.description) },
                    modifier = Modifier.clickable {
                        viewModel.openPluginSettings(plugin.id)
                    }
                )
                Divider()
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
