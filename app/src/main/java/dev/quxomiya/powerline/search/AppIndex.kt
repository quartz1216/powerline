package dev.quxomiya.powerline.search

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.domain.models.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// A launchable app with its search keys normalized once at load time
data class IndexedApp(
    val app: AppInfo,
    // Label, package name minus its first segment, built-in aliases (SearchRanker.normalize'd)
    val searchKeys: List<String>
)

// Launchable apps (standby grid, AppsProvider) and the search engines usable on this device
// (chips, WebProvider, alias parsing). Loaded once, then reloaded whenever a package is
// installed, removed or changed.
class AppIndex(
    private val context: Context,
    private val scope: CoroutineScope,
    private val engines: List<SearchEngine> = PowerlineDefaults.SEARCH_ENGINES
) {

    private val _apps = MutableStateFlow<List<IndexedApp>>(emptyList())
    val apps: StateFlow<List<IndexedApp>> = _apps.asStateFlow()

    // Icons arrive with the first load
    private val _engines = MutableStateFlow(engines.map { AvailableEngine(it) })
    val availableEngines: StateFlow<List<AvailableEngine>> = _engines.asStateFlow()

    // Absent only in test environments; the index then never auto-reloads
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private var loadJob: Job? = null

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reload()
    }

    init {
        launcherApps?.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _engines.value = loadEngines()
            _apps.value = loadApps()
        }
    }

    fun close() {
        launcherApps?.unregisterCallback(packageCallback)
    }

    private suspend fun loadApps(): List<IndexedApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(mainIntent, 0)
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm),
                    className = resolveInfo.activityInfo.name
                )
            }
            .sortedBy { it.label.lowercase() }
            .map { app ->
                val keys = listOf(app.label, app.packageName.substringAfter('.')) +
                    PowerlineDefaults.APP_ALIASES[app.packageName].orEmpty()
                IndexedApp(app, keys.map(SearchRanker::normalize).filter { it.isNotEmpty() })
            }
    }

    private suspend fun loadEngines(): List<AvailableEngine> = withContext(Dispatchers.IO) {
        val browserIcon = handlerIcon("https://www.example.com/")
        engines.map { engine ->
            AvailableEngine(engine, handlerIcon(engine.url.replace("%s", "test")) ?: browserIcon)
        }
    }

    // Icon of the app Android opens this URL with by default: an app that claims the link
    // (YouTube for youtube.com) or the default browser. null when the user would be asked
    // to choose (no default set).
    private fun handlerIcon(url: String): Drawable? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        return try {
            val handler = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
            // When there is no default, resolveActivity returns the chooser, which is not
            // among the actual handlers
            val handlers = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (handlers.none { it.activityInfo.packageName == handler.activityInfo.packageName }) return null
            handler.loadIcon(pm)
        } catch (e: Exception) {
            null
        }
    }
}
