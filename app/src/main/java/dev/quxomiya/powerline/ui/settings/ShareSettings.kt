package dev.quxomiya.powerline.ui.settings

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.quxomiya.powerline.search.ShareTarget
import dev.quxomiya.powerline.search.shareTargetIcon
import dev.quxomiya.powerline.ui.components.AppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

// Share section: the system share dialog (fixed), then apps reached by alias. Those follow the
// engine chips in the overlay.
@Composable
fun ShareSettings() {
    val context = LocalContext.current
    val repository = remember { SearchSettingsRepository.get(context) }
    val engines by repository.engines.collectAsState()
    val targets by repository.shareTargets.collectAsState()

    var editing by remember { mutableStateOf<Editing>(Editing.None) }

    ShareDialogRow()
    targets.forEachIndexed { index, target ->
        ShareTargetRow(target = target, onClick = { editing = Editing.Existing(index) })
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
            onDelete = index?.let {
                {
                    repository.setShareTargets(targets.filterIndexed { i, _ -> i != index })
                    editing = Editing.None
                }
            },
            onDismiss = { editing = Editing.None }
        )
    }
}


// The "Share text" result: always there, so it can be neither edited nor deleted
@Composable
private fun ShareDialogRow() {
    EntryRow(
        icon = null,
        fallbackIcon = Icons.Default.Share,
        title = "Share dialog  ·  default",
        subtitle = "Android share sheet"
    )
}

@Composable
private fun ShareTargetRow(target: ShareTarget, onClick: () -> Unit) {
    val context = LocalContext.current
    val app = remember(target.packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(target.packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            null
        }
    }
    val icon by produceState<Drawable?>(null, target.packageName, target.className) {
        value = withContext(Dispatchers.IO) { context.shareTargetIcon(target) }
    }
    EntryRow(
        icon = icon,
        fallbackIcon = Icons.Default.Share,
        title = "${target.name}  ·  ${target.alias}",
        subtitle = app ?: "Not installed",
        subtitleIsError = app == null,
        onClickLabel = "Edit ${target.name}",
        onClick = onClick
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
    // null for a new target
    onDelete: (() -> Unit)?,
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
        dismissButton = { DismissButtons(onDelete, onDismiss) }
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
