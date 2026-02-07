package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

    override fun search(query: String, context: Context): List<SearchResult> {
        if (!settingsRepository.getShareEnabled()) return emptyList()

        val alias = settingsRepository.getShareAlias()
        if (alias.isEmpty()) return emptyList()

        if (query.startsWith("$alias ")) {
            val text = query.substring(alias.length + 1)
            if (text.isEmpty()) return emptyList()

            return listOf(
                SearchResult(
                    id = "share_text",
                    pluginId = id,
                    title = text,
                    subtitle = "Share Text",
                    type = ResultType.SHARE
                )
            )
        }

        return emptyList()
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
        var alias by remember { mutableStateOf(settingsRepository.getShareAlias()) }

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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = alias,
                onValueChange = {
                    alias = it
                    settingsRepository.setShareAlias(it)
                },
                label = { Text("Alias") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Type this alias followed by a space to trigger the share action.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
