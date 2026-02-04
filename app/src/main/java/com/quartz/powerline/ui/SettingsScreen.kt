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
    var bottomTriggerEnabled by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerEnabled()) }
    var bottomTriggerCenterGapPercent by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerCenterGapPercent()) }
    var bottomTriggerHeightDp by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerHeightDp()) }
    var overlayCloseOnBackgroundTap by remember { mutableStateOf(viewModel.settingsRepository.getOverlayCloseOnBackgroundTap()) }
    var debugMode by remember { mutableStateOf(viewModel.settingsRepository.getDebugMode()) }
    val homeScreenText by viewModel.homeScreenText.collectAsState()
    var showHomeTextDialog by remember { mutableStateOf(false) }

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
                    bottomTriggerEnabled = viewModel.settingsRepository.getBottomTriggerEnabled()
                    bottomTriggerCenterGapPercent = viewModel.settingsRepository.getBottomTriggerCenterGapPercent()
                    bottomTriggerHeightDp = viewModel.settingsRepository.getBottomTriggerHeightDp()
                    overlayCloseOnBackgroundTap = viewModel.settingsRepository.getOverlayCloseOnBackgroundTap()
                    debugMode = viewModel.settingsRepository.getDebugMode()
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

                ListItem(
                    headlineContent = { Text("Appearance") },
                    supportingContent = { Text("Customize overlay background dimming and blur.") },
                    modifier = Modifier.clickable {
                        viewModel.openAppearanceSettings()
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("Home Screen Text") },
                    supportingContent = { Text(if (homeScreenText.isNotEmpty()) homeScreenText else "Tap to set text") },
                    modifier = Modifier.clickable {
                        showHomeTextDialog = true
                    }
                )

                Divider()

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

                // Bottom Trigger
                Text("Features", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Close Overlay on Background Tap") },
                    supportingContent = { Text("When the overlay is open, tapping the empty background area will close it.") },
                    trailingContent = {
                        Switch(
                            checked = overlayCloseOnBackgroundTap,
                            onCheckedChange = {
                                overlayCloseOnBackgroundTap = it
                                viewModel.settingsRepository.setOverlayCloseOnBackgroundTap(it)
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        val newState = !overlayCloseOnBackgroundTap
                        overlayCloseOnBackgroundTap = newState
                        viewModel.settingsRepository.setOverlayCloseOnBackgroundTap(newState)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("Bottom Corners Trigger") },
                    supportingContent = { Text("Tap the bottom corners of the screen to open launcher.") },
                    trailingContent = {
                        Switch(
                            checked = bottomTriggerEnabled,
                            onCheckedChange = {
                                bottomTriggerEnabled = it
                                viewModel.settingsRepository.setBottomTriggerEnabled(it)
                                if (it && !checkAccessibilityPermission(context)) {
                                    Toast.makeText(context, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
                                    viewModel.openPermissions()
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                         val newState = !bottomTriggerEnabled
                         bottomTriggerEnabled = newState
                         viewModel.settingsRepository.setBottomTriggerEnabled(newState)
                         if (newState && !checkAccessibilityPermission(context)) {
                            Toast.makeText(context, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
                            viewModel.openPermissions()
                         }
                    }
                )

                if (bottomTriggerEnabled) {
                    ListItem(
                        headlineContent = { Text("Center Gap: ${bottomTriggerCenterGapPercent}%") },
                        supportingContent = { Text("Percentage of center area to ignore.") },
                        trailingContent = {}
                    )
                    Slider(
                        value = bottomTriggerCenterGapPercent.toFloat(),
                        onValueChange = { bottomTriggerCenterGapPercent = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setBottomTriggerCenterGapPercent(bottomTriggerCenterGapPercent) },
                        valueRange = 0f..90f,
                        steps = 17, // 5% steps roughly
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                     ListItem(
                        headlineContent = { Text("Trigger Height: ${if (bottomTriggerHeightDp == 0) "Auto" else "$bottomTriggerHeightDp dp"}") },
                        supportingContent = { Text("0 = Auto (Navigation Bar Height)") },
                        trailingContent = {}
                    )
                    Slider(
                        value = bottomTriggerHeightDp.toFloat(),
                        onValueChange = { bottomTriggerHeightDp = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setBottomTriggerHeightDp(bottomTriggerHeightDp) },
                        valueRange = 0f..100f,
                        steps = 19, // 5dp steps
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
                        bottomTriggerEnabled = viewModel.settingsRepository.getBottomTriggerEnabled()
                        bottomTriggerCenterGapPercent = viewModel.settingsRepository.getBottomTriggerCenterGapPercent()
                        bottomTriggerHeightDp = viewModel.settingsRepository.getBottomTriggerHeightDp()
                        overlayCloseOnBackgroundTap = viewModel.settingsRepository.getOverlayCloseOnBackgroundTap()
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

        if (showHomeTextDialog) {
            EditHomeScreenTextDialog(
                initialText = homeScreenText,
                onDismiss = { showHomeTextDialog = false },
                onSave = {
                    viewModel.updateHomeScreenText(it)
                    showHomeTextDialog = false
                }
            )
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

@Composable
fun EditHomeScreenTextDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit Home Screen Text", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Leave empty to hide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onSave(text) }
                    ) { Text("Save") }
                }
            }
        }
    }
}
