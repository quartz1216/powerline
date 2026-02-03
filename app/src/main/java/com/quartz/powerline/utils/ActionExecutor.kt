package com.quartz.powerline.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult

object ActionExecutor {

    fun execute(context: Context, result: SearchResult) {
        when (result.type) {
            ResultType.APP -> {
                val intent = context.packageManager.getLaunchIntentForPackage(result.packageName!!)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
                }
            }
            ResultType.WEB -> {
                val url = if (result.payload!!.startsWith("http")) result.payload else "https://www.google.com/search?q=${Uri.encode(result.payload)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                }
            }
            ResultType.CALC -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Calculation Result", result.payload)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied: ${result.payload}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
