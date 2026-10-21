package dev.quxomiya.powerline.data

import dev.quxomiya.powerline.domain.models.ResultType

data class HistoryItem(
    val id: String, // package name or special ID
    val type: ResultType,
    val title: String,
    val subtitle: String? = null,
    val payload: String? = null,
    val timestamp: Long,
    val faviconUrl: String? = null
)
