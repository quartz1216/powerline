package dev.quxomiya.powerline.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.quxomiya.powerline.data.HistoryItem
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.models.ResultType
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem

object ActionExecutor {

    fun execute(
        context: Context,
        item: SearchItem,
        userDataRepository: UserDataRepository
    ) {
        val success = when (val action = item.action) {
            is SearchAction.LaunchApp -> launchApp(context, action)
            is SearchAction.OpenUrl -> openUrl(context, action.url)
            is SearchAction.CopyText -> copyText(context, action.text)
            is SearchAction.ShareText -> shareText(context, action.text)
        }
        if (!success) return

        val action = item.action
        if (action is SearchAction.LaunchApp && PowerlineDefaults.ENABLE_WEIGHT_SEARCH) {
            userDataRepository.recordAppLaunch(action.packageName)
        }
        userDataRepository.addToHistory(historyItem(item))
    }

    private fun launchApp(context: Context, action: SearchAction.LaunchApp): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
            ?: action.className?.let { className ->
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(action.packageName, className))
            }
        if (intent == null) {
            Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun openUrl(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun copyText(context: Context, text: String): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Calculation Result", text))
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun shareText(context: Context, text: String): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    // History still uses the V1 shape until Phase 5 moves it onto SearchAction
    private fun historyItem(item: SearchItem): HistoryItem {
        val (id, type, payload) = when (val action = item.action) {
            is SearchAction.LaunchApp -> Triple(action.packageName, ResultType.APP, null)
            is SearchAction.OpenUrl -> Triple(item.id, ResultType.WEB, action.url)
            is SearchAction.CopyText -> Triple(item.id, ResultType.CALC, action.text)
            is SearchAction.ShareText -> Triple(item.id, ResultType.SHARE, action.text)
        }
        return HistoryItem(
            id = id,
            type = type,
            title = item.title,
            subtitle = item.subtitle,
            payload = payload,
            timestamp = System.currentTimeMillis(),
            faviconUrl = item.iconUrl
        )
    }
}
