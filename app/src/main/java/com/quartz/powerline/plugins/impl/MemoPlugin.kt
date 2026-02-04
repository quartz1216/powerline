package com.quartz.powerline.plugins.impl

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.quartz.powerline.data.MemoRepository
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

class MemoPlugin : Plugin {
    override val id = "memo"
    override val name = "Memo"
    override val description = "Save quick notes"

    override fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        return listOf(
            SearchResult(
                id = "memo_save",
                pluginId = id,
                title = "Save to Memo: $query",
                subtitle = "Tap to save",
                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit),
                type = ResultType.MEMO,
                payload = query
            )
        )
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val content = result.payload ?: return false
        val repo = MemoRepository(context)
        repo.addMemo(content)
        Toast.makeText(context, "Memo saved", Toast.LENGTH_SHORT).show()
        return true
    }

    @Composable
    override fun SettingsScreen(viewModel: SettingsViewModel) {
        val context = LocalContext.current
        val repo = MemoRepository(context)

        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                try {
                    val memos = repo.getAllMemos()
                    val json = com.google.gson.Gson().toJson(memos)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    Toast.makeText(context, "Memos exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Memo Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Manage your memos.")
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { exportLauncher.launch("memos_backup.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Memos to JSON")
            }
        }
    }
}
