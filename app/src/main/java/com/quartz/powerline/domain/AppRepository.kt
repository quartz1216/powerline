package com.quartz.powerline.domain

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)

        resolvedInfos.map { resolveInfo ->
            AppInfo(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(pm),
                className = resolveInfo.activityInfo.name
            )
        }.sortedBy { it.label.lowercase() }
    }

    suspend fun getAppShortcuts(packageName: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        }

        try {
            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
            shortcuts.map { shortcut ->
                SearchResult(
                    id = shortcut.id,
                    title = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut",
                    subtitle = shortcut.longLabel?.toString(),
                    icon = launcherApps.getShortcutIconDrawable(shortcut, 0),
                    type = ResultType.APP,
                    packageName = packageName,
                    payload = "shortcut:${shortcut.id}"
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
