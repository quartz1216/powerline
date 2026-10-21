package dev.quxomiya.powerline.search

import android.graphics.drawable.Drawable

data class SearchItem(
    val id: String,
    val providerId: String,
    val title: String,
    val subtitle: String?,
    val icon: Drawable?,
    val section: ResultSection,
    // Provider's own score; SearchRanker adds match and usage bonuses on top
    val score: Int,
    val action: SearchAction,
    // Extra strings matched besides the title (e.g. the package name of an app)
    val keywords: List<String> = emptyList(),
    // Text-match the query against title and keywords. Off for items built from the query
    // itself (calculator, web, share), whose order the provider decides.
    val rankByMatch: Boolean = false,
    val iconUrl: String? = null
)
