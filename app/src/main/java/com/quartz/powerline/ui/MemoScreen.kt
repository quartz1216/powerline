package com.quartz.powerline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoScreen(viewModel: MainViewModel) {
    var memoText by remember { mutableStateOf(viewModel.settingsRepository.getMemo()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memo") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeMemo() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = memoText,
                onValueChange = {
                    memoText = it
                    viewModel.settingsRepository.setMemo(it)
                },
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text("Write your memo here...") }
            )
        }
    }
}
