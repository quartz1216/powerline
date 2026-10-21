package dev.quxomiya.powerline.ui.components

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.ui.theme.PanelItemShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultItem(
    item: SearchItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    // Same tile look as the standby grid (AppTile)
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelItemShape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        ResultIcon(item = item, isSelected = isSelected)

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ResultIcon(item: SearchItem, isSelected: Boolean = false) {
    val tintColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
    val drawable = item.icon
    if (drawable != null) {
        AppIcon(drawable = drawable)
        return
    }
    val vector = item.vectorIcon ?: when (item.action) {
        is SearchAction.LaunchApp -> Icons.Default.Apps
        is SearchAction.CopyText -> Icons.Default.Calculate
        is SearchAction.OpenUrl -> Icons.Default.Search
        is SearchAction.ShareText -> Icons.Default.Share
    }
    Icon(
        imageVector = vector,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = tintColor
    )
}

@Composable
fun AppIcon(drawable: Drawable, size: Dp = 40.dp) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageDrawable(drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { view ->
            view.setImageDrawable(drawable)
        },
        modifier = Modifier.size(size)
    )
}
