package com.quartz.powerline.domain.models

import com.quartz.powerline.data.HistoryItem

data class AppBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val hiddenApps: Set<String>,
    val aliases: Map<String, String>,
    val maxRecentItems: Int,
    val history: List<HistoryItem>,
    val overlayCloseOnBackgroundTap: Boolean? = true,
    val overlayDimAmount: Float? = 0.5f,
    val overlayBlurRadius: Int? = 0,

    // New Features
    val favoriteApps: Set<String> = emptySet(),
    val appWeights: Map<String, Int> = emptyMap(),
    val enableWeightSearch: Boolean? = true,
    val enablePackageSearch: Boolean? = true,
    val homeScreenText: String? = null,
    val customSearchEngines: List<CustomSearchEngine>? = emptyList(),
    val materialYouEnabled: Boolean? = true,

    // Plugin Settings
    val calculatorEnabled: Boolean? = true,
    val historyEnabled: Boolean? = true,

    // Theme Settings
    val appTheme: String? = null,
    val customAccentColor: Int? = null
)
