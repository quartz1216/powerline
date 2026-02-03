package com.quartz.powerline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.ui.components.CommandBar
import com.quartz.powerline.ui.components.ResultItem
import com.quartz.powerline.ui.viewmodel.MainViewModel

@Composable
fun LauncherScreen(
    viewModel: MainViewModel,
    onResultClick: (SearchResult) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Important: Handle IME padding so list isn't hidden by keyboard
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Spacer to push it down a bit from top status bar
            Spacer(modifier = Modifier.height(48.dp))

            CommandBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onEnter = {
                    if (results.isNotEmpty()) {
                        onResultClick(results.first())
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (results.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = androidx.compose.material3.MaterialTheme.shapes.medium
                        )
                ) {
                    items(results) { result ->
                        ResultItem(
                            result = result,
                            isSelected = false, // TODO: Implement keyboard navigation selection if time permits
                            onClick = { onResultClick(result) }
                        )
                    }
                }
            }
        }
    }
}
