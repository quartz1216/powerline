package com.quartz.powerline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.quartz.powerline.domain.models.AppTheme
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val isAdvanced by viewModel.isAdvanced.collectAsState()
    val materialYouEnabled by viewModel.materialYouEnabled.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()
    val homeScreenText by viewModel.homeScreenText.collectAsState()

    var showHomeTextDialog by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }

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

                // Theme Dropdown
                ExposedDropdownMenuBox(
                    expanded = showThemeDropdown,
                    onExpandedChange = { showThemeDropdown = !showThemeDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Theme Selection") },
                        supportingContent = { Text(themeLabel(appTheme)) },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                        modifier = Modifier
                            .menuAnchor()
                            .clickable { showThemeDropdown = true }
                    )
                    ExposedDropdownMenu(
                        expanded = showThemeDropdown,
                        onDismissRequest = { showThemeDropdown = false }
                    ) {
                        AppTheme.values().forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(themeLabel(theme)) },
                                onClick = {
                                    viewModel.setAppTheme(theme)
                                    showThemeDropdown = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = themeDescription(appTheme),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Use Material You Colors") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = if (isAdvanced) materialYouEnabled else false,
                            onCheckedChange = { viewModel.setMaterialYouEnabled(it) },
                            enabled = isAdvanced
                        )
                    }
                )

                // Color Picker (Only for LIGHT, DARK, SYSTEM)
                if (appTheme == AppTheme.LIGHT || appTheme == AppTheme.DARK || appTheme == AppTheme.SYSTEM) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Accent Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    var hexInput by remember(customAccentColor) {
                        mutableStateOf(String.format("#%06X", (0xFFFFFF and customAccentColor)))
                    }
                    var isHexError by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            if (input.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                                isHexError = false
                                try {
                                    val color = android.graphics.Color.parseColor(input)
                                    viewModel.setCustomAccentColor(color)
                                } catch (e: Exception) {
                                    isHexError = true
                                }
                            } else {
                                isHexError = true
                            }
                        },
                        label = { Text("Hex Color (#RRGGBB)") },
                        singleLine = true,
                        isError = isHexError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(customAccentColor))
                            )
                        }
                    )

                    val colors = listOf(
                        0xFF4CDC8F, // Brand Green
                        0xFFE91E63, // Pink
                        0xFF9C27B0, // Purple
                        0xFF673AB7, // Deep Purple
                        0xFF3F51B5, // Indigo
                        0xFF2196F3, // Blue
                        0xFF03A9F4, // Light Blue
                        0xFF00BCD4, // Cyan
                        0xFF009688, // Teal
                        0xFF4CAF50, // Green
                        0xFF8BC34A, // Light Green
                        0xFFCDDC39, // Lime
                        0xFFFFEB3B, // Yellow
                        0xFFFFC107, // Amber
                        0xFFFF9800, // Orange
                        0xFFFF5722, // Deep Orange
                        0xFF795548, // Brown
                        0xFF607D8B  // Blue Grey
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(colors) { colorLong ->
                            val colorInt = colorLong.toInt()
                            val isSelected = customAccentColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .clickable { viewModel.setCustomAccentColor(colorInt) }
                                    .padding(4.dp), // Padding for border effect
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                    )
                                }
                            }
                        }
                    }
                }

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
                    modifier = Modifier.clickable(enabled = isAdvanced) {
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

private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.BRAND -> "Brand"
    AppTheme.MATERIAL_YOU -> "Material You"
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
    AppTheme.SYSTEM -> "System"
}

private fun themeDescription(theme: AppTheme): String = when (theme) {
    AppTheme.BRAND -> "Uses the brand color as the accent."
    AppTheme.MATERIAL_YOU -> "Dynamically follows the device light/dark theme and wallpaper colors."
    AppTheme.LIGHT -> "Always uses the light theme with a custom accent color."
    AppTheme.DARK -> "Always uses the dark theme with a custom accent color."
    AppTheme.SYSTEM -> "Follows the system dark/light setting with a custom accent color."
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
