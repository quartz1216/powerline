package dev.quxomiya.powerline.data

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.pow

// Stores user data (launch history, favorites). The search settings live in SearchSettingsRepository;
// everything else is fixed in PowerlineDefaults.
// Kept in its own file so Auto Backup can leave it out (res/xml/backup_rules.xml): only the
// search settings are backed up.
class UserDataRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Favorite Apps ---
    fun getFavoriteApps(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_APPS, emptySet()) ?: emptySet()
    }

    fun setFavoriteApps(favorites: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITE_APPS, favorites).apply()
    }

    // --- History ---

    // App launches, most recent first
    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return StoredJson.history(json) ?: emptyList()
    }

    fun recordAppLaunch(packageName: String, className: String, timestamp: Long = System.currentTimeMillis()) {
        val history = listOf(HistoryItem(packageName, className, timestamp)) + getHistory()
        prefs.edit().putString(KEY_HISTORY, StoredJson.ofHistory(history.take(PowerlineDefaults.MAX_HISTORY_ITEMS))).apply()
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

    companion object {
        private const val PREFS_NAME = "powerline_user_data"
        private const val KEY_HISTORY = "history"
        private const val KEY_FAVORITE_APPS = "favorite_apps"

        @Volatile
        private var instance: UserDataRepository? = null

        // One per process, like SearchSettingsRepository
        fun get(context: Context): UserDataRepository =
            instance ?: synchronized(this) {
                instance ?: UserDataRepository(context.applicationContext).also { instance = it }
            }
    }
}
