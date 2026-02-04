package com.quartz.powerline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (results.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

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
                    if (results.isNotEmpty() && selectedIndex in results.indices) {
                        onResultClick(results[selectedIndex])
                    }
                },
                onArrowDown = viewModel::selectNext,
                onArrowUp = viewModel::selectPrevious
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (results.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    itemsIndexed(results) { index, result ->
                        ResultItem(
                            result = result,
                            isSelected = index == selectedIndex,
                            onClick = { onResultClick(result) }
                        )
                    }
                }
            }
        }

        // Settings FAB
        FloatingActionButton(
            onClick = { viewModel.openSettings() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}
