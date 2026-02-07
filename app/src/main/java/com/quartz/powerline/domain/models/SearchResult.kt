package com.quartz.powerline.domain.models

import android.graphics.drawable.Drawable

enum class ResultType {
    APP, WEB, CALC, SHARE
}

data class SearchResult(
    val id: String,
    val pluginId: String,
    val title: String,
    val subtitle: String? = null,
    val icon: Drawable? = null,
    val type: ResultType,
    val packageName: String? = null,
    val className: String? = null,
    val payload: String? = null, // URL for web, Result for calc
    val faviconUrl: String? = null
)
