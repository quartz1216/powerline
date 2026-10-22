package dev.quxomiya.powerline.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.services.StatusBarAccessibilityService

// Two panes: Logo (title, icon, status) and Settings. Stacked in portrait, side by side in landscape.
@Composable
fun StatusScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var accessibilityGranted by remember { mutableStateOf(checkAccessibilityPermission(context)) }
    var batteryUnrestricted by remember { mutableStateOf(checkBatteryOptimizationIgnored(context)) }

    // Re-check when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted = checkAccessibilityPermission(context)
                batteryUnrestricted = checkBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isActive = accessibilityGranted && batteryUnrestricted

    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (maxWidth > maxHeight) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    LogoPane(isActive, Modifier.weight(1f).fillMaxHeight())
                    Spacer(modifier = Modifier.width(16.dp))
                    SettingsPane(accessibilityGranted, batteryUnrestricted, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    LogoPane(isActive, Modifier.weight(0.45f).fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsPane(accessibilityGranted, batteryUnrestricted, Modifier.weight(0.55f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun LogoPane(isActive: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appIcon = remember {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }

    Box(modifier = modifier.padding(8.dp)) {
        Text(
            "Powerline",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                if (isActive) "Press Alt+Space to everything!" else "Powerline is inactive.",
                style = MaterialTheme.typography.titleLarge,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPane(
    accessibilityGranted: Boolean,
    batteryUnrestricted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = "Permissions") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionChip(
                        label = "Accessibility",
                        isGranted = accessibilityGranted,
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                    PermissionChip(
                        label = "Power option",
                        isGranted = batteryUnrestricted,
                        onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    )
                }
            }
    
            // Search settings will be added here
            SettingsSection(title = "Functions") {}
    
            SettingsSection(title = "If Alt+Space doesn't work") {
                Text(
                    "On OSs with strict task management such as Xiaomi HyperOS, even accessibility services may be automatically killed. You may need to uncheck 'Pause app activity if unused' in App Info or set battery restrictions to 'No restrictions'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    
            SettingsSection(title = "About") {
                Text("Powerline ${appVersionName(context)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Powerline uses the accessibility service only to detect Alt+Space. It does not read screen content, record keystrokes, show ads, or back up settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PowerlineDefaults.PRIVACY_POLICY_URL))) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Privacy policy")
                }
            }
        }
    }
}

@Composable
private fun PermissionChip(
    label: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isGranted) "Granted" else "Not granted",
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

@Composable
private fun SettingsSection(
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

fun checkBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
