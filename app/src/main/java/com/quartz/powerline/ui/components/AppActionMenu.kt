package com.quartz.powerline.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.quartz.powerline.domain.models.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionMenu(
    searchResult: SearchResult,
    isFavorite: Boolean,
    onDismissRequest: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    var shortcuts by remember { mutableStateOf<List<ShortcutInfo>>(emptyList()) }

    LaunchedEffect(searchResult.packageName) {
        if (searchResult.packageName != null) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = LauncherApps.ShortcutQuery()
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            query.setPackage(searchResult.packageName)
            try {
                shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResultIcon(result = searchResult)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = searchResult.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Divider()

            // Actions
            ListItem(
                headlineContent = { Text(if (isFavorite) "Unfavorite" else "Favorite") },
                leadingContent = {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    onToggleFavorite()
                    onDismissRequest()
                }
            )

            ListItem(
                headlineContent = { Text("App Info") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", searchResult.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    onDismissRequest()
                }
            )

            if (shortcuts.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Shortcuts",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    shortcuts.forEach { shortcut ->
                        ShortcutItem(shortcut = shortcut) {
                            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                            try {
                                launcherApps.startShortcut(shortcut.`package`, shortcut.id, null, null, Process.myUserHandle())
                                onDismissRequest()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutItem(
    shortcut: ShortcutInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val icon = try {
         launcherApps.getShortcutIconDrawable(shortcut, 0)
    } catch (e: Exception) {
        null
    }

    ListItem(
        headlineContent = { Text(shortcut.shortLabel?.toString() ?: "Shortcut") },
        leadingContent = {
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Default.Shortcut, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
