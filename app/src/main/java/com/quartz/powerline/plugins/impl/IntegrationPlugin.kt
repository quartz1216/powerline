package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
    override val icon = Icons.Default.Send

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        return listOf(
            SearchResult(
                id = "integration_gemini:$query",
                pluginId = id,
                title = "Ask Gemini",
                subtitle = query,
                type = ResultType.INTEGRATION,
                payload = query
            )
        )
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, result.payload)
            setPackage("com.google.android.apps.bard")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback: Try general share if specific app fails?
            // Or just inform user. User asked specifically for Gemini.
            Toast.makeText(context, "Gemini app not found", Toast.LENGTH_SHORT).show()
            false
        }
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Integration Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Currently supports: Gemini")
        }
    }
}
