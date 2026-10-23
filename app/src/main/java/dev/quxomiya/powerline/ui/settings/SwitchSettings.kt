package dev.quxomiya.powerline.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.quxomiya.powerline.data.SearchSettingsRepository
import dev.quxomiya.powerline.search.BuiltIn

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
    val forceAsciiInput by repository.forceAsciiInput.collectAsState()

    SwitchRow(
        title = "Show package name",
        checked = showPackageName,
        onCheckedChange = repository::setShowPackageName
    )
    SwitchRow(
        title = "Open keyboard in ASCII input",
        checked = forceAsciiInput,
        onCheckedChange = repository::setForceAsciiInput
    )
}
