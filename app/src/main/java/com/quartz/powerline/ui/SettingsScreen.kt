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
import androidx.compose.material.icons.filled.*
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
import com.quartz.powerline.utils.GoogleMobileAdsConsentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

                val isAdvanced by viewModel.isAdvanced.collectAsState()
                ListItem(
                    headlineContent = { Text(if (isAdvanced) "Powerline Advanced (Active)" else "Powerline Advanced") },
                    leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, PurchaseActivity::class.java))
                    }
                )

                ListItem(
                    headlineContent = { Text("Overlay Settings") },
                    leadingContent = { Icon(Icons.Default.Layers, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.openOverlaySettings()
                    }
                )

                ListItem(
                    headlineContent = { Text("Appearance Settings") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.openAppearanceSettings()
                    }
                )

                ListItem(
                    headlineContent = { Text("Additional Settings") },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.openAdditionalSettings()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Powerline Functions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(viewModel.pluginManager.plugins.filter { it.name != "System" }) { plugin ->
                ListItem(
                    headlineContent = { Text(plugin.name) },
                    leadingContent = { Icon(plugin.icon, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.openPluginSettings(plugin.id)
                    }
                )
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
                    leadingContent = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Trigger", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        headlineContent = { Text("Center Gap: $centerGap%") },
                        supportingContent = {
                            Slider(
                                value = centerGap.toFloat(),
                                onValueChange = { centerGap = it.toInt() },
                                onValueChangeFinished = { viewModel.settingsRepository.setBottomTriggerCenterGapPercent(centerGap) },
                                valueRange = 0f..90f,
                                steps = 17
                            )
                        },
                        leadingContent = { Icon(Icons.Default.SpaceBar, contentDescription = null) }
                    )

                    ListItem(
                        headlineContent = { Text("Trigger Height: ${if (triggerHeight == 0) "Auto" else "$triggerHeight dp"}") },
                        supportingContent = {
                            Slider(
                                value = triggerHeight.toFloat(),
                                onValueChange = { triggerHeight = it.toInt() },
                                onValueChangeFinished = { viewModel.settingsRepository.setBottomTriggerHeightDp(triggerHeight) },
                                valueRange = 0f..100f,
                                steps = 19
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Height, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Features", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                     ListItem(
                        headlineContent = { Text("Close Overlay on Background Tap") },
                        leadingContent = { Icon(Icons.Default.TouchApp, contentDescription = null) },
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
                        leadingContent = { Icon(Icons.Default.KeyboardHide, contentDescription = null) },
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
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val consentManager = GoogleMobileAdsConsentManager.getInstance(context)
    val scope = rememberCoroutineScope()
    val isAdvanced by viewModel.isAdvanced.collectAsState()

    // Helper to reload settings after import/reset
    fun reloadState() {
        viewModel.reloadApps()
        // isAdvanced observed from flow
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val json = viewModel.settingsRepository.exportSettings()
                        context.contentResolver.openOutputStream(it)?.use { os ->
                            os.write(json.toByteArray())
                        }
                    }
                    Toast.makeText(context, "Settings exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { `is` ->
                            `is`.bufferedReader().use { reader -> reader.readText() }
                        }
                    }
                    if (json != null) {
                        viewModel.settingsRepository.importSettings(json)
                        reloadState()
                        Toast.makeText(context, "Settings imported successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
                Text("Backup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Export Settings") },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    modifier = Modifier.clickable { exportLauncher.launch("powerline_backup.json") }
                )
                ListItem(
                    headlineContent = { Text("Import Settings") },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json")) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (consentManager.isPrivacyOptionsRequired) {
                    Text("Privacy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        headlineContent = { Text("Privacy Settings") },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                        modifier = Modifier.clickable {
                            if (activity != null) {
                                consentManager.showPrivacyOptionsForm(activity) { error ->
                                    if (error != null) {
                                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text("System", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Welcome Tutorial") },
                    leadingContent = { Icon(Icons.Default.Help, contentDescription = null) },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, WelcomeActivity::class.java))
                    }
                )

                ListItem(
                    headlineContent = { Text("App Info") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )

                ListItem(
                    headlineContent = { Text("Restart App") },
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
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
                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.settingsRepository.clearAllSettings()
                        reloadState()
                        Toast.makeText(context, "Settings reset.", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Debug", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Toggle Advanced Mode") },
                    supportingContent = { Text("For testing purposes only") },
                    trailingContent = {
                        Switch(
                            checked = isAdvanced,
                            onCheckedChange = {
                                viewModel.setIsAdvanced(it)
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("If Overlay doesn't start...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(On OSs with strict task management such as Xiaomi HyperOS, even accessibility services may be automatically killed. You may need to uncheck 'Pause app activity if unused' in App Info or set battery restrictions to 'No restrictions'.)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
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

