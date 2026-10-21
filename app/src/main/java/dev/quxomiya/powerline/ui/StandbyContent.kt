package dev.quxomiya.powerline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.ui.components.AppTile
import dev.quxomiya.powerline.search.AvailableEngine
import dev.quxomiya.powerline.ui.components.AppIcon
import dev.quxomiya.powerline.ui.theme.PanelItemInset

// Empty-query state of the overlay: feature chips, recent apps row, divider, all apps by name
@Composable
fun StandbyContent(
    engines: List<AvailableEngine>,
    recentApps: List<AppInfo>,
    allApps: List<AppInfo>,
    grid: StandbyGrid,
    selectedIndex: Int,
    gridState: LazyGridState,
    onChipClick: (AvailableEngine) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        FeatureChips(engines = engines, onChipClick = onChipClick)

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
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
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
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

// Shortcuts for the built-in search engine aliases; tapping one types "<alias> " into the field
@Composable
private fun FeatureChips(engines: List<AvailableEngine>, onChipClick: (AvailableEngine) -> Unit) {
    val aliased = engines.filter { !it.engine.alias.isNullOrBlank() }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(aliased, key = { it.engine.id }) { engine ->
            AssistChip(
                onClick = { onChipClick(engine) },
                label = { Text(engine.engine.name) },
                leadingIcon = {
                    // Icon of the app that opens the engine's URLs
                    val appIcon = engine.icon
                    if (appIcon != null) {
                        AppIcon(drawable = appIcon, size = AssistChipDefaults.IconSize)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                },
                trailingIcon = {
                    Text(
                        text = engine.engine.alias.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}
