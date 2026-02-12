package com.quartz.powerline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()
    val homeScreenText by viewModel.homeScreenText.collectAsState()

    var showHomeTextDialog by remember { mutableStateOf(false) }

    // Read overlay settings
    val bottomTriggerEnabled by remember { mutableStateOf(viewModel.settingsRepository.getBottomTriggerEnabled()) }
    var dimAmount by remember { mutableStateOf(viewModel.settingsRepository.getOverlayDimAmount()) }
    var blurRadius by remember { mutableStateOf(viewModel.settingsRepository.getOverlayBlurRadius()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
                Text("Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Use Material You Colors") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = materialYouEnabled,
                            onCheckedChange = { viewModel.setMaterialYouEnabled(it) }
                        )
                    }
                )
                Text(
                    text = "Material Youは端末に合わせてライトモードとダークモードに動的に変化します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )

                if (bottomTriggerEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Overlay", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        headlineContent = { Text("Background Dim Amount: ${(dimAmount * 100).toInt()}%") },
                        supportingContent = {
                            Slider(
                                value = dimAmount,
                                onValueChange = { dimAmount = it },
                                onValueChangeFinished = { viewModel.settingsRepository.setOverlayDimAmount(dimAmount) },
                                valueRange = 0f..1f
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Opacity, contentDescription = null) }
                    )

                    ListItem(
                        headlineContent = { Text("Background Blur Amount: $blurRadius px") },
                        supportingContent = {
                            Slider(
                                value = blurRadius.toFloat(),
                                onValueChange = { blurRadius = it.toInt() },
                                onValueChangeFinished = { viewModel.settingsRepository.setOverlayBlurRadius(blurRadius) },
                                valueRange = 0f..50f,
                                steps = 49
                            )
                        },
                        leadingContent = { Icon(Icons.Default.BlurOn, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Launcher", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Custom Header: ${homeScreenText.ifEmpty { "(Not set)" }}") },
                    leadingContent = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showHomeTextDialog = true
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
