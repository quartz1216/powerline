package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.MainViewModel

class WebSearchPlugin : Plugin {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "Search the web or open URLs"

    private val urlPattern = java.util.regex.Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(/.*)?$"
    )

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val trimmedQuery = query.trim()
        val results = mutableListOf<SearchResult>()

        // Direct URL
        if (urlPattern.matcher(trimmedQuery).matches()) {
            results.add(
                SearchResult(
                    id = "web_direct",
                    pluginId = id,
                    title = "Go to $trimmedQuery",
                    subtitle = "Web Address",
                    type = ResultType.WEB,
                    payload = if (trimmedQuery.startsWith("http")) trimmedQuery else "https://$trimmedQuery"
                )
            )
        }

        // Search Web (Fallthrough)
        results.add(
            SearchResult(
                id = "web_search:$trimmedQuery",
                pluginId = id,
                title = trimmedQuery,
                subtitle = "Search Web",
                type = ResultType.WEB,
                payload = trimmedQuery
            )
        )

        return results
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val url = if (result.payload!!.startsWith("http")) result.payload else "https://www.google.com/search?q=${Uri.encode(result.payload)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            false
        }
    }

    @Composable
    override fun SettingsScreen(viewModel: MainViewModel) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Web Search Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No settings available for this plugin.")
        }
    }
}
