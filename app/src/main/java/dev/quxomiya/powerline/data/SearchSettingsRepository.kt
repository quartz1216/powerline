package dev.quxomiya.powerline.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.quxomiya.powerline.search.BuiltIn
import dev.quxomiya.powerline.search.SearchEngine
import dev.quxomiya.powerline.search.ShareTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The search settings: the built-in feature switches, the search engines and share targets
// that become the overlay's alias chips, and the advanced display switch.
// Settings (MainActivity) and the overlay (accessibility service) share one process and one
// instance, so an edit reaches the open overlay right away.
class SearchSettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("powerline_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _engines = MutableStateFlow(readEngines())
    val engines: StateFlow<List<SearchEngine>> = _engines.asStateFlow()

    private val _shareTargets = MutableStateFlow(readShareTargets())
    val shareTargets: StateFlow<List<ShareTarget>> = _shareTargets.asStateFlow()

    private val _disabledBuiltIns = MutableStateFlow(readDisabledBuiltIns())
    val disabledBuiltIns: StateFlow<Set<BuiltIn>> = _disabledBuiltIns.asStateFlow()

    // Package name under each app result (on by default)
    private val _showPackageName = MutableStateFlow(readShowPackageName())
    val showPackageName: StateFlow<Boolean> = _showPackageName.asStateFlow()

    // Held here: SharedPreferences keeps its listeners only weakly
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_ENGINES -> _engines.value = readEngines()
            KEY_SHARE_TARGETS -> _shareTargets.value = readShareTargets()
            in BUILT_IN_KEYS -> _disabledBuiltIns.value = readDisabledBuiltIns()
            KEY_SHOW_PACKAGE_NAME -> _showPackageName.value = readShowPackageName()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setEngines(engines: List<SearchEngine>) {
        _engines.value = engines
        prefs.edit().putString(KEY_ENGINES, gson.toJson(engines)).apply()
    }

    // Back to PowerlineDefaults.SEARCH_ENGINES, including any built-in the user deleted
    fun resetEngines() {
        _engines.value = PowerlineDefaults.SEARCH_ENGINES
        prefs.edit().remove(KEY_ENGINES).apply()
    }

    fun isEnabled(builtIn: BuiltIn): Boolean = builtIn !in _disabledBuiltIns.value

    fun setEnabled(builtIn: BuiltIn, enabled: Boolean) {
        _disabledBuiltIns.value = if (enabled) _disabledBuiltIns.value - builtIn else _disabledBuiltIns.value + builtIn
        prefs.edit().putBoolean(builtIn.key, enabled).apply()
    }

    private fun readDisabledBuiltIns(): Set<BuiltIn> =
        BuiltIn.entries.filterTo(mutableSetOf()) { !prefs.getBoolean(it.key, true) }

    fun setShowPackageName(show: Boolean) {
        _showPackageName.value = show
        prefs.edit().putBoolean(KEY_SHOW_PACKAGE_NAME, show).apply()
    }

    private fun readShowPackageName(): Boolean = prefs.getBoolean(KEY_SHOW_PACKAGE_NAME, true)

    fun setShareTargets(targets: List<ShareTarget>) {
        _shareTargets.value = targets
        prefs.edit().putString(KEY_SHARE_TARGETS, gson.toJson(targets)).apply()
    }

    // Unset means the built-in engines; unreadable data falls back to them too
    private fun readEngines(): List<SearchEngine> {
        val json = prefs.getString(KEY_ENGINES, null) ?: return PowerlineDefaults.SEARCH_ENGINES
        return readList(json, SearchEngine::class.java)
            // Gson ignores Kotlin nullability, so drop entries missing a required field
            ?.filter { it.id != null && it.name != null && it.url != null }
            ?: PowerlineDefaults.SEARCH_ENGINES
    }

    private fun readShareTargets(): List<ShareTarget> {
        val json = prefs.getString(KEY_SHARE_TARGETS, null) ?: return emptyList()
        return readList(json, ShareTarget::class.java)
            ?.filter { it.id != null && it.name != null && it.alias != null && it.packageName != null && it.className != null }
            ?: emptyList()
    }

    private fun <T> readList(json: String, type: Class<T>): List<T>? = try {
        // Built explicitly rather than from an anonymous TypeToken subclass, whose generic
        // signature R8 strips in release builds
        gson.fromJson<List<T>>(json, TypeToken.getParameterized(List::class.java, type).type)
    } catch (e: Exception) {
        Log.w("SearchSettingsRepository", "Unreadable $type list", e)
        null
    }

    companion object {
        const val KEY_ENGINES = "search_engines"
        const val KEY_SHARE_TARGETS = "share_targets"
        const val KEY_SHOW_PACKAGE_NAME = "show_package_name"
        private val BUILT_IN_KEYS = BuiltIn.entries.map { it.key }.toSet()
        val KEYS = setOf(KEY_ENGINES, KEY_SHARE_TARGETS, KEY_SHOW_PACKAGE_NAME) + BUILT_IN_KEYS

        @Volatile
        private var instance: Pair<Context, SearchSettingsRepository>? = null

        fun get(context: Context): SearchSettingsRepository {
            val app = context.applicationContext
            instance?.let { (owner, repository) -> if (owner === app) return repository }
            return synchronized(this) {
                instance?.takeIf { it.first === app }?.second
                    ?: SearchSettingsRepository(app).also { instance = app to it }
            }
        }
    }
}

// Rules shared by the Web search and Share dialogs. Each check returns an error message, or null when valid.
object SearchSettingsValidator {

    fun name(name: String): String? = if (name.isBlank()) "Enter a name" else null

    // `others` are the aliases already used by every other engine and share target
    fun alias(alias: String, others: Collection<String?>, required: Boolean): String? {
        val trimmed = alias.trim()
        return when {
            trimmed.isEmpty() -> if (required) "Enter an alias" else null
            trimmed.any { it.isWhitespace() } -> "No spaces in an alias"
            others.any { it != null && it.equals(trimmed, ignoreCase = true) } -> "Alias \"$trimmed\" is already used"
            else -> null
        }
    }

    fun url(url: String): String? {
        val trimmed = url.trim()
        return when {
            !trimmed.startsWith("https://", ignoreCase = true) && !trimmed.startsWith("http://", ignoreCase = true) ->
                "Start with https://"
            !trimmed.contains("%s") -> "Put %s where the search terms go"
            else -> null
        }
    }
}
