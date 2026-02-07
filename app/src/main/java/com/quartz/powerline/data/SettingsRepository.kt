package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quartz.powerline.domain.models.AppBackupData
import com.quartz.powerline.domain.models.ResultType

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Hidden Apps ---
    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
    }

    fun setHiddenApps(hiddenApps: Set<String>) {
        prefs.edit().putStringSet("hidden_apps", hiddenApps).apply()
    }

    // --- Aliases ---
    // Map<Alias, PackageName>
    fun getAliases(): Map<String, String> {
        val json = prefs.getString("aliases", "{}")
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun setAliases(aliases: Map<String, String>) {
        val json = gson.toJson(aliases)
        prefs.edit().putString("aliases", json).apply()
    }

    // --- Max Recent Items ---
    fun getMaxRecentItems(): Int {
        return prefs.getInt("max_recent_items", 5)
    }

    fun setMaxRecentItems(count: Int) {
        prefs.edit().putInt("max_recent_items", count).apply()
    }

    // --- Status Bar Tap ---
    fun getStatusBarTapEnabled(): Boolean {
        return prefs.getBoolean("status_bar_tap_enabled", false)
    }

    fun setStatusBarTapEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("status_bar_tap_enabled", enabled).apply()
    }

    fun getStatusBarTriggerWidthPercent(): Int {
        return prefs.getInt("status_bar_trigger_width_percent", 30)
    }

    fun setStatusBarTriggerWidthPercent(percent: Int) {
        prefs.edit().putInt("status_bar_trigger_width_percent", percent).apply()
    }

    // --- Bottom Trigger ---
    fun getBottomTriggerEnabled(): Boolean {
        return prefs.getBoolean("bottom_trigger_enabled", false)
    }

    fun setBottomTriggerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bottom_trigger_enabled", enabled).apply()
    }

    fun getBottomTriggerCenterGapPercent(): Int {
        return prefs.getInt("bottom_trigger_center_gap_percent", 40)
    }

    fun setBottomTriggerCenterGapPercent(percent: Int) {
        prefs.edit().putInt("bottom_trigger_center_gap_percent", percent).apply()
    }

    fun getBottomTriggerHeightDp(): Int {
        return prefs.getInt("bottom_trigger_height_dp", 0)
    }

    fun setBottomTriggerHeightDp(heightDp: Int) {
        prefs.edit().putInt("bottom_trigger_height_dp", heightDp).apply()
    }

    fun getHideTriggersOnKeyboard(): Boolean {
        return prefs.getBoolean("hide_triggers_on_keyboard", true)
    }

    fun setHideTriggersOnKeyboard(enabled: Boolean) {
        prefs.edit().putBoolean("hide_triggers_on_keyboard", enabled).apply()
    }

    // --- Overlay Behavior ---
    fun getOverlayCloseOnBackgroundTap(): Boolean {
        return prefs.getBoolean("overlay_close_on_background_tap", true)
    }

    fun setOverlayCloseOnBackgroundTap(enabled: Boolean) {
        prefs.edit().putBoolean("overlay_close_on_background_tap", enabled).apply()
    }

    fun getOverlayDimAmount(): Float {
        return prefs.getFloat("overlay_dim_amount", 0.5f)
    }

    fun setOverlayDimAmount(amount: Float) {
        prefs.edit().putFloat("overlay_dim_amount", amount).apply()
    }

    fun getOverlayBlurRadius(): Int {
        return prefs.getInt("overlay_blur_radius", 16)
    }

    fun setOverlayBlurRadius(radius: Int) {
        prefs.edit().putInt("overlay_blur_radius", radius).apply()
    }

    // --- Debug Mode ---
    fun getDebugMode(): Boolean {
        return prefs.getBoolean("debug_mode", false)
    }

    fun setDebugMode(enabled: Boolean) {
        prefs.edit().putBoolean("debug_mode", enabled).apply()
    }

    // --- Favorite Apps ---
    fun getFavoriteApps(): Set<String> {
        return prefs.getStringSet("favorite_apps", emptySet()) ?: emptySet()
    }

    fun setFavoriteApps(favorites: Set<String>) {
        prefs.edit().putStringSet("favorite_apps", favorites).apply()
    }

    // --- App Weights ---
    // Map<PackageName, Weight>
    fun getAppWeights(): Map<String, Int> {
        val json = prefs.getString("app_weights", "{}")
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(json, type)
    }

    fun setAppWeights(weights: Map<String, Int>) {
        val json = gson.toJson(weights)
        prefs.edit().putString("app_weights", json).apply()
    }

    // --- Search Features Toggles ---
    fun getEnableWeightSearch(): Boolean {
        return prefs.getBoolean("enable_weight_search", true)
    }

    fun setEnableWeightSearch(enabled: Boolean) {
        prefs.edit().putBoolean("enable_weight_search", enabled).apply()
    }

    fun getEnablePackageSearch(): Boolean {
        return prefs.getBoolean("enable_package_search", true)
    }

    fun setEnablePackageSearch(enabled: Boolean) {
        prefs.edit().putBoolean("enable_package_search", enabled).apply()
    }

    // --- Home Screen Text ---
    fun getHomeScreenText(): String {
        return prefs.getString("home_screen_text", "") ?: ""
    }

    fun setHomeScreenText(text: String) {
        prefs.edit().putString("home_screen_text", text).apply()
    }

    // --- Onboarding ---
    fun getOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    // --- History ---
    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history", "[]")
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addToHistory(item: HistoryItem) {
        val currentHistory = getHistory().toMutableList()
        currentHistory.add(0, item)
        // Keep last 100
        val trimmed = if (currentHistory.size > 100) currentHistory.take(100) else currentHistory
        saveHistory(trimmed)
    }

    private fun saveHistory(history: List<HistoryItem>) {
        val json = gson.toJson(history)
        prefs.edit().putString("history", json).apply()
    }

    // --- Import / Export ---
    fun exportSettings(): String {
        val data = AppBackupData(
            hiddenApps = getHiddenApps(),
            aliases = getAliases(),
            maxRecentItems = getMaxRecentItems(),
            history = getHistory(),
            statusBarTapEnabled = getStatusBarTapEnabled(),
            statusBarTriggerWidthPercent = getStatusBarTriggerWidthPercent(),
            bottomTriggerEnabled = getBottomTriggerEnabled(),
            bottomTriggerCenterGapPercent = getBottomTriggerCenterGapPercent(),
            bottomTriggerHeightDp = getBottomTriggerHeightDp(),
            hideTriggersOnKeyboard = getHideTriggersOnKeyboard(),
            overlayCloseOnBackgroundTap = getOverlayCloseOnBackgroundTap(),
            overlayDimAmount = getOverlayDimAmount(),
            overlayBlurRadius = getOverlayBlurRadius(),
            favoriteApps = getFavoriteApps(),
            appWeights = getAppWeights(),
            enableWeightSearch = getEnableWeightSearch(),
            enablePackageSearch = getEnablePackageSearch(),
            homeScreenText = getHomeScreenText()
        )
        return gson.toJson(data)
    }

    fun importSettings(json: String) {
        val data = gson.fromJson(json, AppBackupData::class.java)
        setHiddenApps(data.hiddenApps)
        setAliases(data.aliases)
        setMaxRecentItems(data.maxRecentItems)
        saveHistory(data.history)
        setStatusBarTapEnabled(data.statusBarTapEnabled)
        setStatusBarTriggerWidthPercent(data.statusBarTriggerWidthPercent)
        setBottomTriggerEnabled(data.bottomTriggerEnabled)
        setBottomTriggerCenterGapPercent(data.bottomTriggerCenterGapPercent)
        setBottomTriggerHeightDp(data.bottomTriggerHeightDp)
        setHideTriggersOnKeyboard(data.hideTriggersOnKeyboard ?: false)
        setOverlayCloseOnBackgroundTap(data.overlayCloseOnBackgroundTap ?: true)
        setOverlayDimAmount(data.overlayDimAmount ?: 0.5f)
        setOverlayBlurRadius(data.overlayBlurRadius ?: 0)
        setFavoriteApps(data.favoriteApps)
        setAppWeights(data.appWeights)
        setEnableWeightSearch(data.enableWeightSearch ?: true)
        setEnablePackageSearch(data.enablePackageSearch ?: true)
        setHomeScreenText(data.homeScreenText ?: "")
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }
}
