package com.quartz.powerline.plugins.impl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import com.quartz.powerline.domain.Calculator
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import java.util.UUID

class CalculatorPlugin : Plugin {
    override val id = "calculator"
    override val name = "Calculator"
    override val description = "Perform quick calculations"
    override val icon = Icons.Default.Calculate

    override suspend fun search(query: String, context: Context): List<SearchResult> {
        val expression = query.trim()
        if (expression.isBlank()) return emptyList()

        val calcResult = Calculator.evaluate(expression)
        return if (calcResult != null) {
            listOf(
                SearchResult(
                    id = "calc_${UUID.randomUUID()}",
                    pluginId = id,
                    title = calcResult,
                    subtitle = "Calculate",
                    type = ResultType.CALC,
                    payload = calcResult
                )
            )
        } else {
            emptyList()
        }
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Calculation Result", result.payload)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: ${result.payload}", Toast.LENGTH_SHORT).show()
        return true
    }
}
