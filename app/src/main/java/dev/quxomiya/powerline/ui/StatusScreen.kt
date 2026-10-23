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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.data.SearchSettingsRepository
import dev.quxomiya.powerline.search.BuiltIn
import dev.quxomiya.powerline.services.PowerlineAccessibilityService
import dev.quxomiya.powerline.ui.settings.AdvancedSettings
import dev.quxomiya.powerline.ui.settings.BuiltInSettings
import dev.quxomiya.powerline.ui.settings.ShareSettings
import dev.quxomiya.powerline.ui.settings.WebSearchSettings

// Two panes: Logo (title, icon, status) and Settings. Side by side in landscape; stacked in portrait,
// where the logo collapses as the settings scroll.
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
                // Scrolling the settings up first shrinks the logo into one row; scrolling back
                // down expands it again once the settings are back at their top
                val collapseRange = with(LocalDensity.current) { LogoCollapseRange.toPx() }
                var collapsed by remember { mutableFloatStateOf(0f) }
                val connection = remember(collapseRange) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available.y >= 0) return Offset.Zero
                            val before = collapsed
                            collapsed = (collapsed - available.y).coerceIn(0f, collapseRange)
                            return Offset(0f, before - collapsed)
                        }

                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            if (available.y <= 0) return Offset.Zero
                            val before = collapsed
                            collapsed = (collapsed - available.y).coerceIn(0f, collapseRange)
                            return Offset(0f, before - collapsed)
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxSize().padding(16.dp).nestedScroll(connection)) {
                    CollapsingLogo(isActive, collapsed / collapseRange, Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsPane(accessibilityGranted, batteryUnrestricted, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun rememberAppIcon(): ImageBitmap {
    val context = LocalContext.current
    return remember { context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap() }
}

@Composable
private fun StatusText(isActive: Boolean, textAlign: TextAlign) {
    Text(
        if (isActive) "Press Alt+Space to everything!" else "Powerline is inactive.",
        style = MaterialTheme.typography.titleLarge,
        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        textAlign = textAlign
    )
}

// Landscape: title at the top, icon and status centered in the pane
@Composable
private fun LogoPane(isActive: Boolean, modifier: Modifier = Modifier) {
    val appIcon = rememberAppIcon()

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
                modifier = Modifier.size(LogoIconSize)
            )
            Spacer(modifier = Modifier.height(LogoStatusGap))
            StatusText(isActive, TextAlign.Center)
        }
    }
}

// Portrait: the title, then the logo. At fraction 0 the icon sits above the status, both centered;
// at 1 a small icon has the status beside it. In between, sizes and positions are interpolated.
@Composable
private fun CollapsingLogo(isActive: Boolean, fraction: Float, modifier: Modifier = Modifier) {
    val appIcon = rememberAppIcon()

    Layout(
        content = {
            Text("Powerline", style = MaterialTheme.typography.headlineMedium)
            Image(bitmap = appIcon, contentDescription = null)
            StatusText(isActive, if (fraction < 0.5f) TextAlign.Center else TextAlign.Start)
        },
        modifier = modifier.padding(8.dp)
    ) { measurables, constraints ->
        // First half: the icon moves to the start and the status, still below it, to the column
        // beside it. Second half: the status rises beside the icon. So the two never overlap.
        val sideways = (fraction * 2).coerceAtMost(1f)
        val upwards = (fraction * 2 - 1).coerceIn(0f, 1f)
        val width = constraints.maxWidth
        val gap = 16.dp.roundToPx()
        val iconSize = lerp(LogoIconSize, CollapsedLogoIconSize, fraction).roundToPx()
        val textWidth = width - lerp(0, iconSize + gap, sideways)

        val title = measurables[0].measure(Constraints(maxWidth = width))
        val icon = measurables[1].measure(Constraints.fixed(iconSize, iconSize))
        val text = measurables[2].measure(Constraints(maxWidth = textWidth))

        val iconX = lerp((width - iconSize) / 2, 0, sideways)
        val iconY = title.height + lerp(32.dp, 16.dp, fraction).roundToPx()
        val textX = lerp((width - text.width) / 2, iconSize + gap, sideways)
        val textY = lerp(
            iconY + iconSize + LogoStatusGap.roundToPx(),
            iconY + (iconSize - text.height) / 2,
            upwards
        )
        val height = maxOf(iconY + iconSize, textY + text.height)

        layout(width, height) {
            title.place(0, 0)
            icon.place(iconX, iconY)
            text.place(textX, textY)
        }
    }
}

private val LogoIconSize = 128.dp
private val CollapsedLogoIconSize = 56.dp
private val LogoStatusGap = 24.dp
// Scroll distance over which the portrait logo collapses: about how much shorter it gets
private val LogoCollapseRange = 150.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPane(
    accessibilityGranted: Boolean,
    batteryUnrestricted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val disabledBuiltIns by remember { SearchSettingsRepository.get(context) }.disabledBuiltIns.collectAsState()

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
    
            SettingsSection(title = "Built-in") {
                BuiltInSettings()
            }

            // Hidden while their built-in is off
            if (BuiltIn.WEB !in disabledBuiltIns) {
                SettingsSection(title = "Web search") {
                    WebSearchSettings()
                }
            }

            if (BuiltIn.SHARE !in disabledBuiltIns) {
                SettingsSection(title = "Share") {
                    ShareSettings()
                }
            }

            SettingsSection(title = "Advanced") {
                AdvancedSettings()
            }
    
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
    val expectedComponentName = ComponentName(context, PowerlineAccessibilityService::class.java)
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
