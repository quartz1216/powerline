package com.quartz.powerline.domain.models

import com.quartz.powerline.data.HistoryItem

data class AppBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val hiddenApps: Set<String>,
    val aliases: Map<String, String>,
    val maxRecentItems: Int,
    val history: List<HistoryItem>,
    val memo: String = ""
)
