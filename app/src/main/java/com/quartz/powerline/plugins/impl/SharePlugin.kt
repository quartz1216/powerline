package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class SharePlugin(private val settingsRepository: SettingsRepository) : Plugin {
    override val id = "share"
    override val name = "Share"
    override val description = "Share text via system share sheet"
    override val icon = Icons.Default.Share

    override suspend fun search(query: String, context: Context): List<SearchResult> {
        if (!settingsRepository.getShareEnabled()) return emptyList()

        if (query.isBlank()) return emptyList()

        return listOf(
            SearchResult(
                id = "share_text",
                pluginId = id,
                title = query,
                subtitle = "Share Text",
                type = ResultType.SHARE
            )
        )
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, result.title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
        return true
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        var enabled by remember { mutableStateOf(settingsRepository.getShareEnabled()) }

        Column(modifier = Modifier.padding(16.dp)) {
            ListItem(
                headlineContent = { Text("Enable Share Action") },
                trailingContent = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            settingsRepository.setShareEnabled(it)
                        }
                    )
                }
            )
        }
    }
}
