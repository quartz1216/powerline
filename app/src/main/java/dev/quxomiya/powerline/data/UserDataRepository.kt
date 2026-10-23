package dev.quxomiya.powerline.data

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.pow

// Stores the launch history. The search settings live in SearchSettingsRepository;
// everything else is fixed in PowerlineDefaults.
// Kept in its own file so Auto Backup can leave it out (res/xml/backup_rules.xml): only the
// search settings are backed up.
class UserDataRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Read from prefs once, then kept in step with every write: each search reads it
    @Volatile
    private var history: List<HistoryItem>? = null

    // --- History ---

    // App launches, most recent first
    fun getHistory(): List<HistoryItem> = history ?: readHistory().also { history = it }

    private fun readHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return StoredJson.history(json) ?: emptyList()
    }

    fun recordAppLaunch(packageName: String, className: String, timestamp: Long = System.currentTimeMillis()) {
        val updated = (listOf(HistoryItem(packageName, className, timestamp)) + getHistory())
            .take(PowerlineDefaults.MAX_HISTORY_ITEMS)
        history = updated
        prefs.edit().putString(KEY_HISTORY, StoredJson.ofHistory(updated)).apply()
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

        @Volatile
        private var instance: UserDataRepository? = null

        // One per process, like SearchSettingsRepository
        fun get(context: Context): UserDataRepository =
            instance ?: synchronized(this) {
                instance ?: UserDataRepository(context.applicationContext).also { instance = it }
            }
    }
}
