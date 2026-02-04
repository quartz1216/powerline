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

    // --- Debug Mode ---
    fun getDebugMode(): Boolean {
        return prefs.getBoolean("debug_mode", false)
    }

    fun setDebugMode(enabled: Boolean) {
        prefs.edit().putBoolean("debug_mode", enabled).apply()
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
            statusBarTapEnabled = getStatusBarTapEnabled()
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
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }
}
