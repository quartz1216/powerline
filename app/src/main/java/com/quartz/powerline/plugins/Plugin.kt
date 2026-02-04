package com.quartz.powerline.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

interface Plugin {
    val id: String
    val name: String
    val description: String

    fun search(query: String, context: Context): List<SearchResult>
    fun execute(context: Context, result: SearchResult): Boolean

    @Composable
    fun SettingsScreen(viewModel: SettingsViewModel)
}
