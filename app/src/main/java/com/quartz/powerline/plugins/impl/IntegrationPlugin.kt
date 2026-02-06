package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class IntegrationPlugin : Plugin {
    override val id = "integration"
    override val name = "Integration"
    override val description = "Deep integration with other apps"
    override val icon = Icons.Default.Share

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        return listOf(
            SearchResult(
                id = "integration:share:$query",
                pluginId = id,
                title = "Share Text",
                subtitle = query,
                type = ResultType.INTEGRATION,
                payload = query
            )
        )
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        // Only share action supported now
        val query = result.payload ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return true
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Integration Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Currently supports: Share Text")
        }
    }
}
