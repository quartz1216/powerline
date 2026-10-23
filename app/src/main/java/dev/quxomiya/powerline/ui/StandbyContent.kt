package dev.quxomiya.powerline.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.ui.components.AppTile
import dev.quxomiya.powerline.search.AliasChip
import dev.quxomiya.powerline.ui.components.AppIcon
import dev.quxomiya.powerline.ui.theme.PanelItemInset

// Empty-query state of the overlay: feature chips, recent apps row, divider, all apps by name.
// Chips and apps can each be empty (switched off in Built-in); then their part is left out.
@Composable
fun StandbyContent(
    chips: List<AliasChip>,
    recentApps: List<AppInfo>,
    allApps: List<AppInfo>,
    grid: StandbyGrid,
    // -1 while a chip is selected
    selectedIndex: Int,
    selectedChip: Int?,
    gridState: LazyGridState,
    onChipClick: (AliasChip) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (chips.isNotEmpty()) {
            FeatureChips(chips = chips, selectedChip = selectedChip, onChipClick = onChipClick)
        }
        if (grid.size == 0) return@Column

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(grid.columns),
            state = gridState,
            contentPadding = PaddingValues(PanelItemInset),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (recentApps.isNotEmpty()) {
                itemsIndexed(recentApps, key = { _, app -> "recent:${app.packageName}:${app.className}" }) { index, app ->
                    AppTile(
                        app = app,
                        isSelected = index == selectedIndex,
                        onClick = { onAppClick(app) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            itemsIndexed(allApps, key = { _, app -> "all:${app.packageName}:${app.className}" }) { index, app ->
                AppTile(
                    app = app,
                    isSelected = grid.recentCount + index == selectedIndex,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

// Shortcuts for the search engine and share aliases; tapping one types "<alias> " into the field
@Composable
private fun FeatureChips(chips: List<AliasChip>, selectedChip: Int?, onChipClick: (AliasChip) -> Unit) {
    val rowState = rememberLazyListState()

    LaunchedEffect(selectedChip) {
        if (selectedChip != null) rowState.animateScrollToItem(selectedChip)
    }

    LazyRow(
        state = rowState,
        // No top padding: the search field above already leaves room
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rowItemsIndexed(chips, key = { _, chip -> chip.id }) { index, chip ->
            val isSelected = index == selectedChip
            AssistChip(
                onClick = { onChipClick(chip) },
                colors = if (isSelected) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    AssistChipDefaults.assistChipBorder(enabled = true)
                },
                label = { Text(chip.name) },
                leadingIcon = {
                    // Icon of the app that opens the engine's URLs, or of the share target
                    val appIcon = chip.icon
                    if (appIcon != null) {
                        AppIcon(drawable = appIcon, size = AssistChipDefaults.IconSize)
                    } else {
                        Icon(
                            imageVector = if (chip.isShare) Icons.Default.Share else Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                },
                trailingIcon = {
                    Text(
                        text = chip.alias,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}
