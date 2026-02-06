package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class IntegrationPlugin : Plugin {
    override val id = "integration"
    override val name = "Integration"
    override val description = "Deep integration with other apps"
    override val icon = Icons.Default.Send

    private data class IntegrationOption(
        val id: String,
        val label: String,
        val icon: ImageVector,
        val action: (Context, String) -> Unit
    )

    private val integrations = listOf(
        IntegrationOption(
            id = "share",
            label = "Share Text",
            icon = Icons.Default.Share,
            action = { context, query ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        ),
        IntegrationOption(
            id = "maps",
            label = "Search in Maps",
            icon = Icons.Default.Place,
            action = { context, query ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
                intent.setPackage("com.google.android.apps.maps")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to browser if app not installed
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/${Uri.encode(query)}"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
        ),
        IntegrationOption(
            id = "youtube",
            label = "Search in YouTube",
            icon = Icons.Default.PlayArrow,
            action = { context, query ->
                val intent = Intent(Intent.ACTION_SEARCH)
                intent.setPackage("com.google.android.youtube")
                intent.putExtra("query", query)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
        ),
        IntegrationOption(
            id = "play_store",
            label = "Search in Play Store",
            icon = Icons.Default.ShoppingBag,
            action = { context, query ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}"))
                intent.setPackage("com.android.vending")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
        ),
        IntegrationOption(
            id = "amazon",
            label = "Search in Amazon",
            icon = Icons.Default.ShoppingCart,
            action = { context, query ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/s?k=${Uri.encode(query)}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        ),
        IntegrationOption(
            id = "twitter",
            label = "Search in X (Twitter)",
            icon = Icons.Default.AccountCircle,
            action = { context, query ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/search?q=${Uri.encode(query)}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )
    )

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val repo = SettingsRepository(context)
        val enabledApps = repo.getIntegrationEnabledApps()

        return integrations.filter { enabledApps.contains(it.id) }.map { item ->
            SearchResult(
                id = "integration:${item.id}:$query", // Unique ID
                pluginId = id,
                title = item.label,
                subtitle = query, // Or "Search for '$query'"
                type = ResultType.INTEGRATION,
                payload = query
            )
        }
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        // ID format: integration:<appId>:<query>
        val parts = result.id.split(":", limit = 3)
        if (parts.size < 3) return false

        val appId = parts[1]
        val query = result.payload ?: return false

        val integration = integrations.find { it.id == appId } ?: return false

        return try {
            integration.action(context, query)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch ${integration.label}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        var enabledApps by remember { mutableStateOf(viewModel.settingsRepository.getIntegrationEnabledApps()) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Integration Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(integrations) { item ->
                    ListItem(
                        headlineContent = { Text(item.label) },
                        leadingContent = { Icon(item.icon, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = enabledApps.contains(item.id),
                                onCheckedChange = { isChecked ->
                                    val newSet = enabledApps.toMutableSet()
                                    if (isChecked) newSet.add(item.id) else newSet.remove(item.id)
                                    enabledApps = newSet
                                    viewModel.settingsRepository.setIntegrationEnabledApps(newSet)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
