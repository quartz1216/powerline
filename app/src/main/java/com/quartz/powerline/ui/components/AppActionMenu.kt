package com.quartz.powerline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionMenu(
    result: SearchResult,
    shortcuts: List<SearchResult>,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onShortcutClick: (SearchResult) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Basic Actions
            item {
                ActionItem(
                    icon = Icons.Default.Info,
                    label = "App Info",
                    onClick = {
                        onAppInfo()
                        onDismiss()
                    }
                )
            }
            item {
                ActionItem(
                    icon = Icons.Default.Delete,
                    label = "Uninstall",
                    onClick = {
                        onUninstall()
                        onDismiss()
                    }
                )
            }

            if (shortcuts.isNotEmpty()) {
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Shortcuts",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(shortcuts) { shortcut ->
                    ResultItem(
                        result = shortcut,
                        isSelected = false,
                        onClick = {
                            onShortcutClick(shortcut)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
