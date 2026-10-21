package com.quartz.powerline.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quartz.powerline.services.StatusBarAccessibilityService

// Status / permissions / About. V2 has no settings screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var accessibilityGranted by remember { mutableStateOf(checkAccessibilityPermission(context)) }

    // Re-check when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = checkAccessibilityPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Powerline") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AltSpaceStatusCard(
                isEnabled = accessibilityGranted,
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            StatusSection(title = "How to use") {
                Text(
                    "Press Alt+Space in any app to search. Press again or Esc to close.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            StatusSection(title = "Powerline is no longer a home app") {
                Text(
                    "If your home screen is empty, choose another launcher in system settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                ) {
                    Text("Open home app settings")
                }
            }

            StatusSection(title = "If Alt+Space doesn't work") {
                Text(
                    "On OSs with strict task management such as Xiaomi HyperOS, even accessibility services may be automatically killed. You may need to uncheck 'Pause app activity if unused' in App Info or set battery restrictions to 'No restrictions'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusSection(title = "About") {
                Text("Powerline ${appVersionName(context)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Powerline uses the accessibility service only to detect Alt+Space. It does not read screen content, record keystrokes, show ads, or back up settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AltSpaceStatusCard(
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (isEnabled) "Alt+Space is ready" else "Accessibility service is off",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (!isEnabled) {
                Text(
                    "Enable the Powerline accessibility service to open search with Alt+Space.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onOpenSettings) {
                    Text("Open accessibility settings")
                }
            }
        }
    }
}

@Composable
private fun StatusSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

private fun appVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}

fun checkAccessibilityPermission(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, StatusBarAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponentName) {
            return true
        }
    }
    return false
}
