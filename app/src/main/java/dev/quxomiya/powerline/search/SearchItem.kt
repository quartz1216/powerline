package dev.quxomiya.powerline.search

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.vector.ImageVector

data class SearchItem(
    val id: String,
    val providerId: String,
    val title: String,
    val subtitle: String?,
    // App icon; items without one use vectorIcon
    val icon: Drawable?,
    val section: ResultSection,
    // Provider's score: the match tier for apps, the intended order for items built from the
    // query itself (calculator, web, share). SearchRanker adds usage bonuses on top.
    val score: Int,
    val action: SearchAction,
    val vectorIcon: ImageVector? = null
)
