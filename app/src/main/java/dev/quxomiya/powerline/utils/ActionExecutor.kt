package dev.quxomiya.powerline.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.ShareTarget

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
            is SearchAction.ShareText -> shareText(context, action.text, action.target)
        }
        // Only app launches are history (recent row and frecency); see HistoryItem
        val action = item.action
        if (success && action is SearchAction.LaunchApp) {
            userDataRepository.recordAppLaunch(action.packageName, action.className)
        }
    }

    private fun launchApp(context: Context, action: SearchAction.LaunchApp): Boolean {
        // The exact launcher activity: a package can have several launcher icons
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(action.packageName, action.className))
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
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

    // Straight to `target` when given; the share sheet when not, or when the target is gone
    fun shareText(context: Context, text: String, target: ShareTarget? = null): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        if (target != null) {
            try {
                context.startActivity(
                    Intent(sendIntent)
                        .setComponent(ComponentName(target.packageName, target.className))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot share to ${target.name}", Toast.LENGTH_SHORT).show()
            }
        }
        context.startActivity(Intent.createChooser(sendIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
