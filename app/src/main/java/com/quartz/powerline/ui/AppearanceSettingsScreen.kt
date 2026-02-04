package com.quartz.powerline.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quartz.powerline.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var overlayDimAmount by remember { mutableStateOf(viewModel.settingsRepository.getOverlayDimAmount()) }
    var overlayBlurRadius by remember { mutableStateOf(viewModel.settingsRepository.getOverlayBlurRadius()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Overlay Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Adjust the background appearance of the overlay activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dim Amount Slider
                Text("Dim Amount: ${(overlayDimAmount * 100).toInt()}%")
                Slider(
                    value = overlayDimAmount,
                    onValueChange = { overlayDimAmount = it },
                    onValueChangeFinished = { viewModel.settingsRepository.setOverlayDimAmount(overlayDimAmount) },
                    valueRange = 0f..1f,
                    steps = 19 // 5% increments
                )
                Text(
                    text = "Controls the darkness of the background behind the search interface.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Blur Radius Slider (Android 12+)
                if (Build.VERSION.SDK_INT >= 31) {
                    Text("Background Blur: $overlayBlurRadius px")
                    Slider(
                        value = overlayBlurRadius.toFloat(),
                        onValueChange = { overlayBlurRadius = it.toInt() },
                        onValueChangeFinished = { viewModel.settingsRepository.setOverlayBlurRadius(overlayBlurRadius) },
                        valueRange = 0f..50f,
                        steps = 49 // 1px increments
                    )
                    Text(
                        text = "Blurs the content behind the overlay. Higher values may impact performance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Background Blur",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Not available on this Android version (requires Android 12+).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
