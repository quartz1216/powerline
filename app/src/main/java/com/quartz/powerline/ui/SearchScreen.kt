package com.quartz.powerline.ui

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.ui.components.AppActionMenu
import com.quartz.powerline.ui.components.CommandBar
import com.quartz.powerline.ui.components.ResultItem
import com.quartz.powerline.ui.viewmodel.SearchViewModel

private val PanelMaxWidth = 720.dp
// Search field + chips + recent row + three rows of all apps (the fourth just hidden)
private val PanelMaxHeight = 560.dp
private val AppTileMinWidth = 96.dp

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onResultClick: (SearchResult) -> Unit,
    onInfoClick: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val openCount by viewModel.openCount.collectAsState()
    val allApps by viewModel.visibleApps.collectAsState()
    val recentAppsAll by viewModel.recentApps.collectAsState()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var longPressedResult by remember { mutableStateOf<SearchResult?>(null) }

    val isStandby = query.isEmpty()
    var standbyIndex by remember(openCount, isStandby) { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndex) {
        if (results.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(openCount) {
        gridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Important: Handle IME padding so the panel isn't hidden by keyboard
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val panelWidth = minOf(maxWidth, PanelMaxWidth)
        // Top of the panel sits where a max-height panel would be vertically centered,
        // so it stays put when the content (standby / results) changes height
        val panelTop = ((maxHeight - PanelMaxHeight) / 2).coerceAtLeast(48.dp)
        // Grid content padding is 12dp on each side
        val columns = ((panelWidth - 24.dp) / AppTileMinWidth).toInt().coerceIn(3, 8)
        // Recent apps fill exactly one row
        val recentApps = recentAppsAll.take(columns)
        val grid = remember(recentApps.size, allApps.size, columns) {
            StandbyGrid(recentApps.size, allApps.size, columns)
        }
        val standbySelection = standbyIndex.coerceIn(0, maxOf(grid.size - 1, 0))

        fun standbyAppAt(index: Int) =
            if (index < recentApps.size) recentApps.getOrNull(index) else allApps.getOrNull(index - recentApps.size)

        // Keep the keyboard-selected tile visible
        LaunchedEffect(standbySelection, grid) {
            if (!isStandby || grid.size == 0) return@LaunchedEffect
            val target = grid.gridItemIndex(standbySelection)
            val layoutInfo = gridState.layoutInfo
            val item = layoutInfo.visibleItemsInfo.find { it.index == target }
            if (item == null) {
                gridState.animateScrollToItem(target)
            } else {
                val top = item.offset.y
                val bottom = top + item.size.height
                if (top < layoutInfo.viewportStartOffset) {
                    gridState.animateScrollBy((top - layoutInfo.viewportStartOffset).toFloat())
                } else if (bottom > layoutInfo.viewportEndOffset) {
                    gridState.animateScrollBy((bottom - layoutInfo.viewportEndOffset).toFloat())
                }
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = PanelMaxWidth)
                .fillMaxWidth()
                .padding(bottom = 64.dp)
        ) {
            Spacer(modifier = Modifier.height(panelTop))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = PanelMaxHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommandBar(
                            query = query,
                            onQueryChange = viewModel::onQueryChange,
                            onEnter = {
                                if (isStandby) {
                                    standbyAppAt(standbySelection)?.let { onResultClick(viewModel.appResult(it)) }
                                } else if (results.isNotEmpty() && selectedIndex in results.indices) {
                                    onResultClick(results[selectedIndex])
                                }
                            },
                            onArrowDown = {
                                if (isStandby) standbyIndex = grid.moveVertical(standbySelection, 1)
                                else viewModel.selectNext()
                            },
                            onArrowUp = {
                                if (isStandby) standbyIndex = grid.moveVertical(standbySelection, -1)
                                else viewModel.selectPrevious()
                            },
                            onArrowLeft = {
                                if (isStandby) standbyIndex = grid.moveHorizontal(standbySelection, -1)
                                isStandby
                            },
                            onArrowRight = {
                                if (isStandby) standbyIndex = grid.moveHorizontal(standbySelection, 1)
                                isStandby
                            },
                            modifier = Modifier.weight(1f),
                            focusKey = openCount
                        )

                        // Opens the status screen
                        IconButton(onClick = onInfoClick) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Powerline status",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isStandby) {
                        StandbyContent(
                            recentApps = recentApps,
                            allApps = allApps,
                            grid = grid,
                            selectedIndex = standbySelection,
                            gridState = gridState,
                            onChipClick = { engine -> viewModel.onQueryChange("${engine.alias} ") },
                            onAppClick = { onResultClick(viewModel.appResult(it)) },
                            onAppLongClick = { longPressedResult = viewModel.appResult(it) }
                        )
                    } else if (results.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 8.dp)
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
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
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
    }
}
