package dev.quxomiya.powerline.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dev.quxomiya.powerline.domain.models.AppInfo
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
}
