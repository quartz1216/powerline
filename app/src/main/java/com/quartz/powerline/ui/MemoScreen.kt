package com.quartz.powerline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quartz.powerline.domain.models.Memo
import com.quartz.powerline.ui.viewmodel.MemoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoScreen(
    viewModel: MemoViewModel,
    onBack: () -> Unit
) {
    val memos by viewModel.memos.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    BackHandler {
        if (isSelectionMode) {
            selectedIds = emptySet()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} selected")
                    } else {
                        Text("Memos")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            selectedIds = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            viewModel.deleteMemos(selectedIds)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (memos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No memos yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(memos, key = { it.id }) { memo ->
                    MemoItem(
                        memo = memo,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedIds.contains(memo.id),
                        onToggleSelection = {
                            val newSet = selectedIds.toMutableSet()
                            if (newSet.contains(memo.id)) {
                                newSet.remove(memo.id)
                            } else {
                                newSet.add(memo.id)
                            }
                            selectedIds = newSet
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedIds = setOf(memo.id)
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoItem(
    memo: Memo,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val dateString = remember(memo.timestamp) { formatter.format(Date(memo.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column {
            Text(
                text = memo.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
