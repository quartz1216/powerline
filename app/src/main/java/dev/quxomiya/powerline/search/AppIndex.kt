package dev.quxomiya.powerline.search

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserHandle
import android.util.Log
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.domain.models.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// A launchable app with its search keys prepared once at load time
data class IndexedApp(
    val app: AppInfo,
    // Label, package name minus its first segment, built-in aliases
    val searchKeys: List<MatchKey>
) {
    companion object {
        fun of(app: AppInfo, aliases: List<String> = emptyList()) = IndexedApp(
            app,
            (listOf(app.label, app.packageName.substringAfter('.')) + aliases).mapNotNull(MatchKey::of)
        )
    }
}

// Launchable apps (standby grid, AppsProvider), and the search engines and share targets with
// their icons (chips, WebProvider, ShareProvider). Loaded once, then reloaded once package events
// settle (an app store updating many apps reloads once); engines and share targets also whenever
// settings edit them.
class AppIndex(
    private val context: Context,
    private val scope: CoroutineScope,
    private val engines: StateFlow<List<SearchEngine>>,
    private val shareTargets: StateFlow<List<ShareTarget>>
) {

    private val _apps = MutableStateFlow<List<IndexedApp>>(emptyList())
    val apps: StateFlow<List<IndexedApp>> = _apps.asStateFlow()

    // Icons arrive with the first load
    private val _engines = MutableStateFlow(engines.value.map { AvailableEngine(it) })
    val availableEngines: StateFlow<List<AvailableEngine>> = _engines.asStateFlow()

    // Targets whose app is installed
    private val _shareTargets = MutableStateFlow<List<AvailableShareTarget>>(emptyList())
    val availableShareTargets: StateFlow<List<AvailableShareTarget>> = _shareTargets.asStateFlow()

    // Absent only in test environments; the index then never auto-reloads
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private var loadJob: Job? = null

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reloadAfterPackageEvents()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reloadAfterPackageEvents()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reloadAfterPackageEvents()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
            reloadAfterPackageEvents()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
            reloadAfterPackageEvents()
    }

    init {
        launcherApps?.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        // The first values are loaded by reload() below
        scope.launch {
            combine(engines, shareTargets) { e, t -> e to t }.drop(1).collectLatest { (e, t) -> loadChips(e, t) }
        }
        reload()
    }

    // Each event restarts the wait, so a burst of events ends in one reload
    private fun reloadAfterPackageEvents() = reload(delayMs = PACKAGE_EVENT_SETTLE_MS)

    private fun reload(delayMs: Long = 0) {
        loadJob?.cancel()
        loadJob = scope.launch {
            delay(delayMs)
            val start = SystemClock.uptimeMillis()
            loadChips(engines.value, shareTargets.value)
            _apps.value = loadApps()
            // Load time and reload count (ROADMAP §6); read by scripts/measure.sh
            Log.i(TAG, "Indexed ${_apps.value.size} apps in ${SystemClock.uptimeMillis() - start} ms")
        }
    }

    private suspend fun loadChips(engines: List<SearchEngine>, shareTargets: List<ShareTarget>) {
        _engines.value = loadEngines(engines)
        _shareTargets.value = loadShareTargets(shareTargets)
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
            .map { app -> IndexedApp.of(app, PowerlineDefaults.APP_ALIASES[app.packageName].orEmpty()) }
    }

    private suspend fun loadEngines(engines: List<SearchEngine>): List<AvailableEngine> = withContext(Dispatchers.IO) {
        val browserIcon = context.urlHandlerIcon(BROWSER_PROBE_URL)
        engines.map { engine -> AvailableEngine(engine, context.engineIcon(engine, browserIcon)) }
    }

    private suspend fun loadShareTargets(targets: List<ShareTarget>): List<AvailableShareTarget> =
        withContext(Dispatchers.IO) {
            targets.mapNotNull { target ->
                // Uninstalled since it was set up: kept in settings in case it comes back
                context.shareTargetIcon(target)?.let { AvailableShareTarget(target, it) }
            }
        }
}

private const val TAG = "AppIndex"
private const val PACKAGE_EVENT_SETTLE_MS = 500L
private const val BROWSER_PROBE_URL = "https://www.example.com/"

// Icon of the app that opens this engine's URLs, else of the default browser (`browserIcon`,
// passed in when looking up many engines at once). Also the icon in the Web search settings.
fun Context.engineIcon(engine: SearchEngine, browserIcon: Drawable? = urlHandlerIcon(BROWSER_PROBE_URL)): Drawable? =
    urlHandlerIcon(engine.url.replace("%s", "test")) ?: browserIcon

// Icon of the target's share activity; null once its app is uninstalled
fun Context.shareTargetIcon(target: ShareTarget): Drawable? = try {
    packageManager.getActivityIcon(ComponentName(target.packageName, target.className))
} catch (e: PackageManager.NameNotFoundException) {
    null
}

// Icon of the app Android opens this URL with by default: an app that claims the link
// (YouTube for youtube.com) or the default browser. null when the user would be asked
// to choose (no default set).
private fun Context.urlHandlerIcon(url: String): Drawable? {
    val pm = packageManager
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
