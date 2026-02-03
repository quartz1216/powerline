package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quartz.powerline.domain.models.ResultType

data class HistoryItem(
    val id: String, // package name or special ID
    val type: ResultType,
    val title: String,
    val timestamp: Long
)

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
}
