package com.quartz.powerline.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.quartz.powerline.data.HistoryItem
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.PluginAction
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
                    val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
                    if (intent != null) {
                        context.startActivity(intent)
                        success = true
                    } else {
                        Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
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
            ResultType.PLUGIN -> {
                val plugins = settingsRepository.getPlugins()
                val pluginId = result.id.removePrefix("plugin_")
                val plugin = plugins.find { it.id == pluginId }
                if (plugin != null) {
                    val arg = result.payload ?: ""
                    // For URL, we encode. For Clipboard, maybe we don't?
                    // Let's decide based on action.

                    when (plugin.action) {
                        PluginAction.OPEN_URL -> {
                            val actionData = plugin.actionData.replace("{q}", Uri.encode(arg))
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionData))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                                success = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to handle this action", Toast.LENGTH_SHORT).show()
                            }
                        }
                        PluginAction.COPY_TO_CLIPBOARD -> {
                            val actionData = plugin.actionData.replace("{q}", arg)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(plugin.name, actionData)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            success = true
                        }
                    }
                }
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
}
