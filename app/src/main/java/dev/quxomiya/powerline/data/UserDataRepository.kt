package dev.quxomiya.powerline.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.pow

// Stores user data (launch history, favorites). The search settings live in SearchSettingsRepository;
// everything else is fixed in PowerlineDefaults.
// Kept in its own file so Auto Backup can leave it out (res/xml/backup_rules.xml): only the
// search settings are backed up.
class UserDataRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences(SearchSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        moveUserDataOutOfSettings()
        removeLegacyKeys()
    }

    // Earlier builds kept history and favorites in the settings file
    private fun moveUserDataOutOfSettings() {
        val stored = settingsPrefs.all.filterKeys { it in USER_DATA_KEYS }
        if (stored.isEmpty()) return
        prefs.edit().apply {
            stored[KEY_HISTORY]?.let { if (!prefs.contains(KEY_HISTORY)) putString(KEY_HISTORY, it as String) }
            @Suppress("UNCHECKED_CAST")
            stored[KEY_FAVORITE_APPS]?.let {
                if (!prefs.contains(KEY_FAVORITE_APPS)) putStringSet(KEY_FAVORITE_APPS, (it as Set<String>).toSet())
            }
        }.apply()
        settingsPrefs.edit().apply { stored.keys.forEach(::remove) }.apply()
    }

    // Anything but the search settings in the settings file is a leftover setting from an earlier
    // build (onboarding_completed, app_weights, ...). V1 itself was com.quartz.powerline, so its
    // data never lives here.
    private fun removeLegacyKeys() {
        val stale = settingsPrefs.all.keys - SearchSettingsRepository.KEYS
        if (stale.isEmpty()) return
        settingsPrefs.edit().apply { stale.forEach(::remove) }.apply()
    }

    // --- Favorite Apps ---
    fun getFavoriteApps(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_APPS, emptySet()) ?: emptySet()
    }

    fun setFavoriteApps(favorites: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITE_APPS, favorites).apply()
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
        val json = prefs.getString(KEY_HISTORY, "[]")
        return try {
            // Built explicitly rather than from an anonymous TypeToken subclass, whose generic
            // signature R8 strips in release builds
            val typeToken = TypeToken.getParameterized(List::class.java, StoredHistoryItem::class.java).type
            val stored: List<StoredHistoryItem> = gson.fromJson(json, typeToken) ?: emptyList()
            stored.mapNotNull { item ->
                val packageName = item.packageName
                    ?: item.id.takeIf { item.type == "APP" && it != "settings_action" }
                    ?: return@mapNotNull null
                HistoryItem(packageName, item.className, item.timestamp)
            }
        } catch (e: Exception) {
            Log.w("UserDataRepository", "Unreadable history", e)
            emptyList()
        }
    }

    fun recordAppLaunch(packageName: String, className: String?, timestamp: Long = System.currentTimeMillis()) {
        val history = listOf(HistoryItem(packageName, className, timestamp)) + getHistory()
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history.take(PowerlineDefaults.MAX_HISTORY_ITEMS))).apply()
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

    private companion object {
        const val PREFS_NAME = "powerline_user_data"
        const val KEY_HISTORY = "history"
        const val KEY_FAVORITE_APPS = "favorite_apps"
        val USER_DATA_KEYS = setOf(KEY_HISTORY, KEY_FAVORITE_APPS)
    }
}
