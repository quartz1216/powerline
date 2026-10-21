package dev.quxomiya.powerline.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.pow

// Stores user data only (launch history, favorites). Settings are fixed in PowerlineDefaults.
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

    // --- History ---

    // Reads both the current entries and V1 entries ({id, type, title, ...}), where an app
    // launch was type "APP" with the package name as id. V1 web / calc / share entries are dropped.
    private data class StoredHistoryItem(
        val packageName: String? = null,
        val className: String? = null,
        val id: String? = null,
        val type: String? = null,
        val timestamp: Long = 0
    )

    // App launches, most recent first
    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history", "[]")
        return try {
            val typeToken = object : TypeToken<List<StoredHistoryItem>>() {}.type
            val stored: List<StoredHistoryItem> = gson.fromJson(json, typeToken) ?: emptyList()
            stored.mapNotNull { item ->
                val packageName = item.packageName
                    ?: item.id.takeIf { item.type == "APP" && it != "settings_action" }
                    ?: return@mapNotNull null
                HistoryItem(packageName, item.className, item.timestamp)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordAppLaunch(packageName: String, className: String?, timestamp: Long = System.currentTimeMillis()) {
        val history = listOf(HistoryItem(packageName, className, timestamp)) + getHistory()
        prefs.edit().putString("history", gson.toJson(history.take(PowerlineDefaults.MAX_HISTORY_ITEMS))).apply()
    }

    // Package name -> sum over its launches of 0.5^(age / half-life); a launch just now counts 1
    fun getAppFrecency(now: Long = System.currentTimeMillis()): Map<String, Double> {
        val frecency = mutableMapOf<String, Double>()
        for (item in getHistory()) {
            val age = (now - item.timestamp).coerceAtLeast(0)
            val weight = 0.5.pow(age.toDouble() / PowerlineDefaults.FRECENCY_HALF_LIFE_MS)
            frecency[item.packageName] = (frecency[item.packageName] ?: 0.0) + weight
        }
        return frecency
    }
}
