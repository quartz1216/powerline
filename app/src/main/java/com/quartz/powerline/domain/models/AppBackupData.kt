package com.quartz.powerline.domain.models

import com.quartz.powerline.data.HistoryItem

data class AppBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val hiddenApps: Set<String>,
    val aliases: Map<String, String>,
    val maxRecentItems: Int,
    val history: List<HistoryItem>,
    val statusBarTapEnabled: Boolean = false, // Deprecated / Reused as general toggle? No, clearer to keep for legacy import.
    val statusBarTriggerWidthPercent: Int = 30, // Deprecated
    val bottomTriggerEnabled: Boolean = false,
    val bottomTriggerCenterGapPercent: Int = 40,
    val bottomTriggerHeightDp: Int = 0, // 0 = Auto
    val overlayCloseOnBackgroundTap: Boolean? = true,
    val overlayDimAmount: Float? = 0.5f,
    val overlayBlurRadius: Int? = 0,

    // New Features
    val favoriteApps: Set<String> = emptySet(),
    val appWeights: Map<String, Int> = emptyMap(),
    val enableWeightSearch: Boolean? = true,
    val enablePackageSearch: Boolean? = true,
    val homeScreenText: String? = null,
    val hideTriggersOnKeyboard: Boolean? = false,
    val customSearchEngines: List<CustomSearchEngine>? = emptyList(),
    val materialYouEnabled: Boolean? = true,

    // Plugin Settings
    val calculatorEnabled: Boolean? = true,
    val calculatorAlias: String? = null,
    val historyEnabled: Boolean? = true,
    val recentsShowOnEmpty: Boolean? = true,
    val recentsAlias: String? = null
)
