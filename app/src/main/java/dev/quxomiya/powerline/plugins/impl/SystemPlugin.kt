package dev.quxomiya.powerline.plugins.impl

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.core.content.ContextCompat
import dev.quxomiya.powerline.domain.models.ResultType
import dev.quxomiya.powerline.domain.models.SearchResult
import dev.quxomiya.powerline.plugins.Plugin

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
                    title = "Powerline Status",
                    subtitle = "Alt+Space status and permissions",
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
}
