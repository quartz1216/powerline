package com.quartz.powerline.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.widget.Toast
import com.quartz.powerline.data.HistoryItem
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult

object ActionExecutor {

    fun execute(context: Context, result: SearchResult, settingsRepository: SettingsRepository) {
        // Log History
        // We log before execution or after?
        // If we log after, we ensure success.

        var success = false

        when (result.type) {
            ResultType.APP -> {
                if (result.packageName != null) {
                    if (result.payload != null && result.payload.startsWith("shortcut:")) {
                        val shortcutId = result.payload.removePrefix("shortcut:")
                        executeShortcut(context, result.packageName, shortcutId)
                        success = true
                    } else {
                        val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
                        if (intent != null) {
                            context.startActivity(intent)
                            success = true
                        } else {
                            Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            ResultType.WEB -> {
                val url = if (result.payload!!.startsWith("http")) result.payload else "https://www.google.com/search?q=${Uri.encode(result.payload)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    success = true
                } catch (e: Exception) {
                    Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                }
            }
            ResultType.CALC -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Calculation Result", result.payload)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied: ${result.payload}", Toast.LENGTH_SHORT).show()
                success = true
            }
        }

        if (success) {
            val historyItem = HistoryItem(
                id = result.id,
                type = result.type,
                title = result.title,
                timestamp = System.currentTimeMillis()
            )
            settingsRepository.addToHistory(historyItem)
        }
    }

    fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open app info", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot uninstall app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeShortcut(context: Context, packageName: String, shortcutId: String) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(packageName, shortcutId, null, null, android.os.Process.myUserHandle())
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch shortcut", Toast.LENGTH_SHORT).show()
        }
    }
}
