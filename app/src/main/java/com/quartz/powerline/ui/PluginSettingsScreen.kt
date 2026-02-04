package com.quartz.powerline.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSettingsScreen(
    viewModel: SettingsViewModel,
    plugin: Plugin,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plugin.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            plugin.SettingsScreen(viewModel = viewModel)
        }
    }
}
