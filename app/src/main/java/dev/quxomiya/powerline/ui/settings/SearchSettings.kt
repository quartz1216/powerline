package dev.quxomiya.powerline.ui.settings

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.quxomiya.powerline.data.SearchSettingsRepository
import dev.quxomiya.powerline.data.SearchSettingsValidator
import dev.quxomiya.powerline.search.BuiltIn
import dev.quxomiya.powerline.search.SearchEngine
import dev.quxomiya.powerline.search.ShareTarget
import dev.quxomiya.powerline.ui.components.AppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

// Dialog state: nothing open, a new entry, or the entry at `index`
private sealed interface Editing {
    data object None : Editing
    data object New : Editing
    data class Existing(val index: Int) : Editing
}

// Built-in section: switches for the search features that need no setup
@Composable
fun BuiltInSettings() {
    val context = LocalContext.current
    val repository = remember { SearchSettingsRepository.get(context) }
    val disabled by repository.disabledBuiltIns.collectAsState()

    BuiltIn.entries.forEach { builtIn ->
        SwitchRow(
            title = builtIn.title,
            checked = builtIn !in disabled,
            onCheckedChange = { repository.setEnabled(builtIn, it) }
        )
    }
}

// Advanced section: display switches for power users
@Composable
fun AdvancedSettings() {
    val context = LocalContext.current
    val repository = remember { SearchSettingsRepository.get(context) }
    val showPackageName by repository.showPackageName.collectAsState()

    SwitchRow(
        title = "Show package name",
        checked = showPackageName,
        onCheckedChange = repository::setShowPackageName
    )
}

// Web search section: the engines. Aliased ones become the overlay's first chips.
@Composable
fun WebSearchSettings() {
    val context = LocalContext.current
    val repository = remember { SearchSettingsRepository.get(context) }
    val engines by repository.engines.collectAsState()
    val targets by repository.shareTargets.collectAsState()

    var editing by remember { mutableStateOf<Editing>(Editing.None) }
    var confirmReset by remember { mutableStateOf(false) }

    val defaultIndex = engines.indexOfFirst { it.alias.isNullOrBlank() }
    engines.forEachIndexed { index, engine ->
        EngineRow(
            engine = engine,
            isDefault = index == defaultIndex,
            canMoveUp = index > 0,
            canMoveDown = index < engines.lastIndex,
            onClick = { editing = Editing.Existing(index) },
            onMove = { offset -> repository.setEngines(engines.moved(index, offset)) },
            onDelete = { repository.setEngines(engines.filterIndexed { i, _ -> i != index }) }
        )
    }
    ButtonRow {
        AddButton("Add engine") { editing = Editing.New }
        TextButton(onClick = { confirmReset = true }) { Text("Restore defaults") }
    }

    if (editing != Editing.None) {
        val index = (editing as? Editing.Existing)?.index
        EngineEditDialog(
            initial = index?.let { engines.getOrNull(it) },
            otherAliases = engines.filterIndexed { i, _ -> i != index }.map { it.alias } + targets.map { it.alias },
            onSave = { engine ->
                repository.setEngines(if (index == null) engines + engine else engines.replaced(index, engine))
                editing = Editing.None
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

// Share section: apps reached by alias. They follow the engine chips in the overlay.
@Composable
fun ShareSettings() {
    val context = LocalContext.current
    val repository = remember { SearchSettingsRepository.get(context) }
    val engines by repository.engines.collectAsState()
    val targets by repository.shareTargets.collectAsState()

    var editing by remember { mutableStateOf<Editing>(Editing.None) }

    targets.forEachIndexed { index, target ->
        ShareTargetRow(
            target = target,
            onClick = { editing = Editing.Existing(index) },
            onDelete = { repository.setShareTargets(targets.filterIndexed { i, _ -> i != index }) }
        )
    }
    ButtonRow {
        AddButton("Add share target") { editing = Editing.New }
    }

    if (editing != Editing.None) {
        val index = (editing as? Editing.Existing)?.index
        ShareTargetEditDialog(
            initial = index?.let { targets.getOrNull(it) },
            otherAliases = engines.map { it.alias } + targets.filterIndexed { i, _ -> i != index }.map { it.alias },
            onSave = { target ->
                repository.setShareTargets(if (index == null) targets + target else targets.replaced(index, target))
                editing = Editing.None
            },
            onDismiss = { editing = Editing.None }
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        // The row handles the toggle, so TalkBack reads title and state as one item
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
private fun EngineRow(
    engine: SearchEngine,
    isDefault: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit ${engine.name}", onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    !engine.alias.isNullOrBlank() -> "${engine.name}  ·  ${engine.alias}"
                    isDefault -> "${engine.name}  ·  default"
                    else -> engine.name
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                engine.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${engine.name} up")
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${engine.name} down")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete ${engine.name}")
        }
    }
}

@Composable
private fun ShareTargetRow(target: ShareTarget, onClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val app = remember(target.packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(target.packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            null
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit ${target.name}", onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${target.name}  ·  ${target.alias}", style = MaterialTheme.typography.bodyMedium)
            Text(
                app ?: "Not installed",
                style = MaterialTheme.typography.bodySmall,
                color = if (app == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete ${target.name}")
        }
    }
}

@Composable
private fun EngineEditDialog(
    initial: SearchEngine?,
    otherAliases: List<String?>,
    onSave: (SearchEngine) -> Unit,
    onDismiss: () -> Unit
) {
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
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// An app activity that accepts shared text
private data class ShareActivity(
    val packageName: String,
    val className: String,
    val label: String,
    val appLabel: String,
    val icon: Drawable
)

@Composable
private fun ShareTargetEditDialog(
    initial: ShareTarget?,
    otherAliases: List<String?>,
    onSave: (ShareTarget) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activities by remember { mutableStateOf<List<ShareActivity>?>(null) }
    var selected by remember { mutableStateOf(initial?.let { it.packageName to it.className }) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    // The name follows the chosen app until the user types one
    var nameEdited by remember { mutableStateOf(initial != null) }
    var alias by remember { mutableStateOf(initial?.alias.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activities = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    ShareActivity(
                        packageName = info.activityInfo.packageName,
                        className = info.activityInfo.name,
                        label = info.loadLabel(pm).toString(),
                        appLabel = info.activityInfo.applicationInfo.loadLabel(pm).toString(),
                        icon = info.loadIcon(pm)
                    )
                }
                .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
        }
    }

    val nameError = SearchSettingsValidator.name(name)
    val aliasError = SearchSettingsValidator.alias(alias, otherAliases, required = true)
    val appError = if (selected == null) "Choose an app" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add share target" else "Edit share target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogField("Name", name, { name = it; nameEdited = true }, nameError.takeIf { showErrors })
                DialogField("Alias", alias, { alias = it }, aliasError.takeIf { showErrors })
                Text(
                    "App",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (showErrors && appError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                val list = activities
                if (list == null) {
                    Hint("Loading…")
                } else if (list.isEmpty()) {
                    Hint("No app accepts shared text")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(list, key = { "${it.packageName}/${it.className}" }) { activity ->
                            val isSelected = selected == (activity.packageName to activity.className)
                            ShareActivityRow(activity, isSelected) {
                                selected = activity.packageName to activity.className
                                if (!nameEdited) name = activity.appLabel
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showErrors = true
                val (packageName, className) = selected ?: return@TextButton
                if (nameError == null && aliasError == null) {
                    onSave(
                        ShareTarget(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            alias = alias.trim(),
                            packageName = packageName,
                            className = className
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ShareActivityRow(activity: ShareActivity, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        AppIcon(drawable = activity.icon, size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.appLabel, style = MaterialTheme.typography.bodyMedium)
            // Apps with several share activities ("New chat", "Add to note") tell them apart by label
            if (activity.label != activity.appLabel) {
                Text(
                    activity.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DialogField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun <T> List<T>.moved(index: Int, offset: Int): List<T> {
    val target = (index + offset).coerceIn(0, lastIndex)
    return toMutableList().apply { add(target, removeAt(index)) }
}

private fun <T> List<T>.replaced(index: Int, item: T): List<T> = toMutableList().apply { set(index, item) }
