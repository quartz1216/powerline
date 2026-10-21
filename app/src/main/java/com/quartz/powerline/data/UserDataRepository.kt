package com.quartz.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quartz.powerline.domain.models.ResultType

// Stores user data only (history, favorites, usage weights). Settings are fixed in PowerlineDefaults.
class UserDataRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

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

    // --- Onboarding ---
    // Temporary until the Welcome flow is reworked in Phase 6
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
}
