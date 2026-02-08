package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quartz.powerline.domain.models.AppBackupData
import com.quartz.powerline.domain.models.CustomSearchEngine
import com.quartz.powerline.domain.models.ResultType

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var cachedCustomSearchEngines: List<CustomSearchEngine>? = null

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

    // --- Appearance ---
    fun getMaterialYouEnabled(): Boolean {
        return prefs.getBoolean("material_you_enabled", true)
    }

    fun setMaterialYouEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("material_you_enabled", enabled).apply()
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

    // --- Custom Search Engines ---
    fun getCustomSearchEngines(): List<CustomSearchEngine> {
        if (cachedCustomSearchEngines != null) return cachedCustomSearchEngines!!

        if (!prefs.contains("custom_search_engines")) {
            // Initialize defaults
            val defaults = listOf(
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "Google", "https://www.google.com/search?q=%s", isEnabled = true),
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "YouTube Music", "https://music.youtube.com/search?q=%s", "ytm", true),
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "Maps", "https://www.google.com/maps/search/%s", "maps", true),
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "X", "https://twitter.com/search?q=%s", "x", true),
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "YouTube", "https://www.youtube.com/results?search_query=%s", "yt", true),
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "Wikipedia", "https://en.wikipedia.org/wiki/Special:Search?search=%s", "wiki", true),
                CustomSearchEngine(java.util.UUID.randomUUID().toString(), "Play Store", "https://play.google.com/store/search?q=%s", "play", true)
            )
            setCustomSearchEngines(defaults)
            return defaults
        }
        val json = prefs.getString("custom_search_engines", "[]")
        val type = object : TypeToken<List<CustomSearchEngine>>() {}.type
        val engines: List<CustomSearchEngine> = gson.fromJson(json, type)
        cachedCustomSearchEngines = engines
        return engines
    }

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) {
        cachedCustomSearchEngines = engines
        val json = gson.toJson(engines)
        prefs.edit().putString("custom_search_engines", json).apply()
    }

    // --- Share Plugin ---
    fun getShareEnabled(): Boolean {
        return prefs.getBoolean("share_enabled", true)
    }

    fun setShareEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("share_enabled", enabled).apply()
    }

    fun getShareAlias(): String {
        return prefs.getString("share_alias", "share") ?: "share"
    }

    fun setShareAlias(alias: String) {
        prefs.edit().putString("share_alias", alias).apply()
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

    private data class LegacyHistoryItem(
        val id: String,
        val type: String,
        val title: String,
        val subtitle: String? = null,
        val payload: String? = null,
        val timestamp: Long,
        val faviconUrl: String? = null
    )

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history", "[]")
        return try {
            val typeToken = object : TypeToken<List<LegacyHistoryItem>>() {}.type
            val legacyItems: List<LegacyHistoryItem> = gson.fromJson(json, typeToken) ?: emptyList()

            legacyItems.mapNotNull { legacy ->
                try {
                    if (legacy.type == "MEMO") return@mapNotNull null
                    HistoryItem(
                        id = legacy.id,
                        type = ResultType.valueOf(legacy.type),
                        title = legacy.title,
                        subtitle = legacy.subtitle,
                        payload = legacy.payload,
                        timestamp = legacy.timestamp,
                        faviconUrl = legacy.faviconUrl
                    )
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
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
            homeScreenText = getHomeScreenText(),
            customSearchEngines = getCustomSearchEngines(),
            materialYouEnabled = getMaterialYouEnabled()
        )
        return gson.toJson(data)
    }

    fun importSettings(json: String) {
        try {
            val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
            if (jsonObject.has("history")) {
                val historyArray = jsonObject.getAsJsonArray("history")
                val newHistoryArray = com.google.gson.JsonArray()
                historyArray.forEach { element ->
                    val item = element.asJsonObject
                    if (item.has("type") && item.get("type").asString != "MEMO") {
                        newHistoryArray.add(item)
                    }
                }
                jsonObject.add("history", newHistoryArray)
            }

            val data = gson.fromJson(jsonObject, AppBackupData::class.java)
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
            setCustomSearchEngines(data.customSearchEngines ?: emptyList())
            setMaterialYouEnabled(data.materialYouEnabled ?: true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }
}
