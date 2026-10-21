package com.quartz.powerline.ui

import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quartz.powerline.data.PowerlineDefaults
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.CustomSearchEngine
import com.quartz.powerline.ui.components.AppTile

// Empty-query state of the overlay: feature chips, recent apps row, divider, all apps by name
@Composable
fun StandbyContent(
    recentApps: List<AppInfo>,
    allApps: List<AppInfo>,
    grid: StandbyGrid,
    selectedIndex: Int,
    gridState: LazyGridState,
    onChipClick: (CustomSearchEngine) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        FeatureChips(onChipClick = onChipClick)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(grid.columns),
            state = gridState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
private fun FeatureChips(onChipClick: (CustomSearchEngine) -> Unit) {
    val engines = PowerlineDefaults.SEARCH_ENGINES.filter { it.isEnabled && !it.alias.isNullOrBlank() }
    val fallbackIcon = rememberVectorPainter(Icons.Default.Search)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(engines, key = { it.id }) { engine ->
            AssistChip(
                onClick = { onChipClick(engine) },
                label = { Text(engine.name) },
                leadingIcon = {
                    AsyncImage(
                        model = faviconUrl(engine),
                        contentDescription = null,
                        placeholder = fallbackIcon,
                        error = fallbackIcon,
                        modifier = Modifier
                            .size(AssistChipDefaults.IconSize)
                            .clip(CircleShape)
                    )
                },
                trailingIcon = {
                    Text(
                        text = engine.alias.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

private fun faviconUrl(engine: CustomSearchEngine): String? {
    val engineUrl = if (engine.url.startsWith("http")) engine.url else "https://${engine.url}"
    val domain = Uri.parse(engineUrl).host ?: return null
    return "https://www.google.com/s2/favicons?sz=64&domain_url=$domain"
}
