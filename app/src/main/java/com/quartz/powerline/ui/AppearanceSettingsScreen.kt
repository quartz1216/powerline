package com.quartz.powerline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.AppTheme
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val appTheme by viewModel.appTheme.collectAsState()
    val customAccentColor by viewModel.customAccentColor.collectAsState()

    var showThemeDropdown by remember { mutableStateOf(false) }

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
                        supportingContent = {
                            Text(
                                text = when (appTheme) {
                                    AppTheme.BRAND -> "ブランド"
                                    AppTheme.MATERIAL_YOU -> "Material You"
                                    AppTheme.LIGHT -> "ライトモード"
                                    AppTheme.DARK -> "ダークモード"
                                    AppTheme.SYSTEM -> "端末に合わせる"
                                }
                            )
                        },
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
                                text = {
                                    Text(
                                        when (theme) {
                                            AppTheme.BRAND -> "ブランド"
                                            AppTheme.MATERIAL_YOU -> "Material You"
                                            AppTheme.LIGHT -> "ライトモード"
                                            AppTheme.DARK -> "ダークモード"
                                            AppTheme.SYSTEM -> "端末に合わせる"
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.setAppTheme(theme)
                                    showThemeDropdown = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = when (appTheme) {
                        AppTheme.BRAND -> "ブランドカラーをアクセントに使用します。"
                        AppTheme.MATERIAL_YOU -> "Material Youは端末に合わせてライトモードとダークモードに動的に変化します。"
                        AppTheme.LIGHT -> "ライトモードで固定し、カスタムアクセントカラーを使用します。"
                        AppTheme.DARK -> "ダークモードで固定し、カスタムアクセントカラーを使用します。"
                        AppTheme.SYSTEM -> "システムのダークモード設定に従い、カスタムアクセントカラーを使用します。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
            }
        }
    }
}
