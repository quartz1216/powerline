package com.quartz.powerline.ui

import android.content.ComponentName
import android.content.Context
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quartz.powerline.PurchaseActivity
import com.quartz.powerline.WelcomeActivity
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.services.StatusBarAccessibilityService
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                Text("General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Powerline Advanced") },
                    supportingContent = { Text("View purchase options.") },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, PurchaseActivity::class.java))
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("Overlay Settings") },
                    supportingContent = { Text("Configure triggers, appearance, and permissions.") },
                    modifier = Modifier.clickable {
                        viewModel.openOverlaySettings()
                    }
                )

                Divider()

                ListItem(
                    headlineContent = { Text("Additional Settings") },
                    supportingContent = { Text("Backup, system tools, and launcher options.") },
                    modifier = Modifier.clickable {
                        viewModel.openAdditionalSettings()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Powerline Functions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySettingsSubpage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var bottomTriggerEnabled by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerEnabled()) }
    var dimAmount by remember { mutableStateOf(viewModel.settingsRepository.getOverlayDimAmount()) }
    var blurRadius by remember { mutableStateOf(viewModel.settingsRepository.getOverlayBlurRadius()) }
    var centerGap by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerCenterGapPercent()) }
    var triggerHeight by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerHeightDp()) }
    var closeOnBgTap by remember { mutableStateOf(viewModel.settingsRepository.getOverlayCloseOnBackgroundTap()) }
    var hideOnKeyboard by remember { mutableStateOf(viewModel.settingsRepository.getHideTriggersOnKeyboard()) }

    // Auto-enable debug mode (highlight) when entering this screen
    DisposableEffect(Unit) {
        viewModel.setDebugMode(true)
        onDispose {
            viewModel.setDebugMode(false)
        }
    }

    // Lifecycle observer to refresh permissions state when returning from system settings
    var accessibilityPermissionGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        accessibilityPermissionGranted = checkAccessibilityPermission(context)
    }

    // Check initially
    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // Check on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overlay") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                ListItem(
                    headlineContent = { Text("Enable Overlay & Trigger") },
                    trailingContent = {
                        Switch(
                            checked = bottomTriggerEnabled,
                            onCheckedChange = {
                                bottomTriggerEnabled = it
                                viewModel.settingsRepository.setBottomTriggerEnabled(it)
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Permissions", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                             val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                             context.startActivity(intent)
                        },
                        label = { Text("Accessibility") },
                        leadingIcon = {
                             Icon(
                                if (accessibilityPermissionGranted) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (accessibilityPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }

                if (bottomTriggerEnabled) {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Background Dim Amount: ${(dimAmount * 100).toInt()}%")
                    Slider(
                        value = dimAmount,
                        onValueChange = { dimAmount = it },
                        onValueChangeFinished = { viewModel.settingsRepository.setOverlayDimAmount(dimAmount) },
                        valueRange = 0f..1f
                    )

                    Text("Background Blur Amount: $blurRadius px")
                    Slider(
                        value = blurRadius.toFloat(),
                        onValueChange = { blurRadius = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setOverlayBlurRadius(blurRadius) },
                        valueRange = 0f..50f,
                        steps = 49
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Trigger", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Center Gap: $centerGap%")
                    Slider(
                        value = centerGap.toFloat(),
                        onValueChange = { centerGap = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setBottomTriggerCenterGapPercent(centerGap) },
                        valueRange = 0f..90f,
                        steps = 17
                    )

                    Text("Trigger Height: ${if (triggerHeight == 0) "Auto" else "$triggerHeight dp"}")
                    Slider(
                        value = triggerHeight.toFloat(),
                        onValueChange = { triggerHeight = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setBottomTriggerHeightDp(triggerHeight) },
                        valueRange = 0f..100f,
                        steps = 19
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Features", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                     ListItem(
                        headlineContent = { Text("Close Overlay on Background Tap") },
                        trailingContent = {
                            Switch(
                                checked = closeOnBgTap,
                                onCheckedChange = {
                                    closeOnBgTap = it
                                    viewModel.settingsRepository.setOverlayCloseOnBackgroundTap(it)
                                }
                            )
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Disable Trigger on Keyboard") },
                        trailingContent = {
                            Switch(
                                checked = hideOnKeyboard,
                                onCheckedChange = {
                                    hideOnKeyboard = it
                                    viewModel.settingsRepository.setHideTriggersOnKeyboard(it)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalSettingsSubpage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val homeScreenText by viewModel.homeScreenText.collectAsState()
    var showHomeTextDialog by remember { mutableStateOf(false) }

    // Helper to reload settings after import/reset
    fun reloadState() {
        viewModel.reloadApps()
    }

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
                    reloadState()
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
                title = { Text("Additional Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                Text("Launcher", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Custom Header Text") },
                    supportingContent = { Text(if (homeScreenText.isNotEmpty()) homeScreenText else "Tap to set text") },
                    modifier = Modifier.clickable {
                        showHomeTextDialog = true
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Backup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text("System", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Welcome Tutorial") },
                    supportingContent = { Text("Replay the onboarding introduction.") },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, WelcomeActivity::class.java))
                    }
                )

                ListItem(
                    headlineContent = { Text("App Info") },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )

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

                ListItem(
                    headlineContent = { Text("Reset App Settings") },
                    supportingContent = { Text("Clears all internal preferences and history.") },
                    modifier = Modifier.clickable {
                        viewModel.settingsRepository.clearAllSettings()
                        reloadState()
                        Toast.makeText(context, "Settings reset.", Toast.LENGTH_SHORT).show()
                    }
                )
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

// Reuse existing dialogs
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

