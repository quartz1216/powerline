package com.quartz.powerline.plugins.impl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.Calculator
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel
import java.util.UUID

class CalculatorPlugin : Plugin {
    override val id = "calculator"
    override val name = "Calculator"
    override val description = "Perform quick calculations"
    override val icon = Icons.Default.Calculate

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val calcResult = Calculator.evaluate(query.trim())
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

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Calculator Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No settings available for this plugin.")
        }
    }
}
