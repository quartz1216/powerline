package com.quartz.powerline.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSettingsScreen(viewModel: MainViewModel, plugin: Plugin) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plugin.name) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeSettings() }) {
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
