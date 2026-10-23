package dev.quxomiya.powerline.ui.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.quxomiya.powerline.data.SearchSettingsRepository
import dev.quxomiya.powerline.data.SearchSettingsValidator
import dev.quxomiya.powerline.search.SearchEngine
import dev.quxomiya.powerline.search.engineIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

// Web search section: the default engine, then the rest by name. Tapping one opens its dialog,
// which also sets the default and deletes. Aliased ones become the overlay's first chips.
@Composable
fun WebSearchSettings() {
    val context = LocalContext.current
    val repository = remember { SearchSettingsRepository.get(context) }
    val engines by repository.engines.collectAsState()
    val targets by repository.shareTargets.collectAsState()

    var editing by remember { mutableStateOf<Editing>(Editing.None) }
    var confirmReset by remember { mutableStateOf(false) }

    // Indexes into `engines`, whose first entry is the default
    engines.indices.sortedWith(compareBy({ it != 0 }, { engines[it].name.lowercase() })).forEach { index ->
        EngineRow(
            engine = engines[index],
            isDefault = index == 0,
            onClick = { editing = Editing.Existing(index) }
        )
    }
    ButtonRow {
        AddButton("Add engine") { editing = Editing.New }
        TextButton(onClick = { confirmReset = true }) { Text("Restore defaults") }
    }

    if (editing != Editing.None) {
        val index = (editing as? Editing.Existing)?.index
        val others = engines.filterIndexed { i, _ -> i != index }
        EngineEditDialog(
            initial = index?.let { engines.getOrNull(it) },
            isDefault = index == 0,
            otherAliases = others.map { it.alias } + targets.map { it.alias },
            onSave = { engine, makeDefault ->
                repository.setEngines(
                    when {
                        // The default moves to the front
                        makeDefault -> listOf(engine) + others
                        index == null -> engines + engine
                        else -> engines.replaced(index, engine)
                    }
                )
                editing = Editing.None
            },
            // Deleting the default makes the next engine the default
            onDelete = index?.let {
                {
                    repository.setEngines(others)
                    editing = Editing.None
                }
            },
            onDismiss = { editing = Editing.None }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Restore default engines?") },
            text = { Text("Your engine edits are replaced with the built-in list. Share targets are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.resetEngines()
                    confirmReset = false
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}


@Composable
private fun EngineRow(engine: SearchEngine, isDefault: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<Drawable?>(null, engine.url) {
        value = withContext(Dispatchers.IO) { context.engineIcon(engine) }
    }
    EntryRow(
        icon = icon,
        fallbackIcon = Icons.Default.Search,
        title = listOfNotNull(engine.name, engine.alias?.takeIf { it.isNotBlank() }, "default".takeIf { isDefault })
            .joinToString("  ·  "),
        subtitle = engine.url,
        onClickLabel = "Edit ${engine.name}",
        onClick = onClick
    )
}


@Composable
private fun EngineEditDialog(
    initial: SearchEngine?,
    // The default stays checked: another engine has to be made the default instead
    isDefault: Boolean,
    otherAliases: List<String?>,
    onSave: (engine: SearchEngine, makeDefault: Boolean) -> Unit,
    // null for a new engine
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var makeDefault by remember { mutableStateOf(isDefault) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var alias by remember { mutableStateOf(initial?.alias.orEmpty()) }
    var url by remember { mutableStateOf(initial?.url ?: "https://") }
    // Errors appear after the first save attempt, not while the fields are still being filled
    var showErrors by remember { mutableStateOf(false) }

    val nameError = SearchSettingsValidator.name(name)
    val aliasError = SearchSettingsValidator.alias(alias, otherAliases, required = false)
    val urlError = SearchSettingsValidator.url(url)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add engine" else "Edit engine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogField("Name", name, { name = it }, nameError.takeIf { showErrors })
                DialogField("Alias", alias, { alias = it }, aliasError.takeIf { showErrors })
                DialogField("URL", url, { url = it }, urlError.takeIf { showErrors }, singleLine = false)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = makeDefault,
                            enabled = !isDefault,
                            role = Role.Checkbox,
                            onValueChange = { makeDefault = it }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = makeDefault, onCheckedChange = null, enabled = !isDefault)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use as default", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showErrors = true
                if (nameError == null && aliasError == null && urlError == null) {
                    onSave(
                        SearchEngine(
                            id = initial?.id ?: "custom_${UUID.randomUUID()}",
                            name = name.trim(),
                            alias = alias.trim().ifEmpty { null },
                            url = url.trim()
                        ),
                        makeDefault
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { DismissButtons(onDelete, onDismiss) }
    )
}
