package com.quartz.powerline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.ui.components.AppActionMenu
import com.quartz.powerline.ui.components.CommandBar
import com.quartz.powerline.ui.components.ResultItem
import com.quartz.powerline.ui.viewmodel.MainViewModel

@Composable
fun LauncherScreen(
    viewModel: MainViewModel,
    onResultClick: (SearchResult) -> Unit,
    onMemoClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val listState = rememberLazyListState()
    var longPressedResult by remember { mutableStateOf<SearchResult?>(null) }
    val homeScreenText by viewModel.homeScreenText.collectAsState()
    var showHomeTextDialog by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(64.dp))

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
                            onClick = { onResultClick(result) },
                            onLongClick = {
                                if (result.type == ResultType.APP && result.packageName != null) {
                                    longPressedResult = result
                                }
                            }
                        )
                    }
                }
            }
        }

        // Long Press Menu
        longPressedResult?.let { result ->
            AppActionMenu(
                searchResult = result,
                isFavorite = result.packageName?.let { viewModel.isFavorite(it) } ?: false,
                onDismissRequest = { longPressedResult = null },
                onToggleFavorite = {
                    result.packageName?.let { viewModel.toggleFavorite(it) }
                }
            )
        }

        // Home Screen Text (Top Left)
        if (homeScreenText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable { showHomeTextDialog = true }
            ) {
                val fontSize = when {
                    homeScreenText.length > 35 -> 12.sp
                    homeScreenText.length > 25 -> 14.sp
                    homeScreenText.length > 15 -> 16.sp
                    else -> 20.sp
                }

                Text(
                    text = homeScreenText,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Hamburger Menu (Top Right)
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = onMemoClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Memos")
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        if (showHomeTextDialog) {
            EditHomeScreenTextDialog(
                initialText = homeScreenText,
                onDismiss = { showHomeTextDialog = false },
                onSave = {
                    viewModel.updateHomeScreenText(it)
                    showHomeTextDialog = false
                }
            )
        }
    }
}
