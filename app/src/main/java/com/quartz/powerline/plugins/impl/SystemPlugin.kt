package com.quartz.powerline.plugins.impl

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class SystemPlugin : Plugin {
    override val id = "system"
    override val name = "System"
    override val description = "System commands and settings"
    override val icon = Icons.Default.Settings

    override suspend fun search(query: String, context: Context): List<SearchResult> {
        if (query.trim().lowercase() == "settings") {
            return listOf(
                SearchResult(
                    id = "settings_action",
                    pluginId = id,
                    title = "Settings",
                    subtitle = "Open Settings",
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_preferences),
                    type = ResultType.APP,
                    payload = "settings_action"
                )
            )
        }
        return emptyList()
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        // Handled specially in MainActivity or via viewModel for navigation
        // But we can return false here to indicate it's not a standard external action if we want.
        // Or we can let it return true and let the caller handle "settings_action" payload.
        return true
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
         Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("This plugin handles core system commands.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
