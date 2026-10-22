package dev.quxomiya.powerline.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay

private const val FOCUS_ATTEMPTS = 5
private const val FOCUS_RETRY_MS = 16L

// Borderless search field used as the header of the overlay panel
@Composable
fun CommandBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onEnter: () -> Unit,
    onArrowDown: () -> Unit,
    onArrowUp: () -> Unit,
    modifier: Modifier = Modifier,
    // Return true to consume the key (e.g. grid navigation while the query is empty)
    onArrowLeft: () -> Boolean = { false },
    onArrowRight: () -> Boolean = { false },
    // Tab / Shift+Tab (backward = true): move between sections
    onTab: (backward: Boolean) -> Unit = {},
    focusKey: Any = Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Every open: take focus, then ask for the soft keyboard (a no-op while a physical one is in use).
    // The overlay window may not have window focus on the first frame, so retry briefly.
    LaunchedEffect(focusKey) {
        repeat(FOCUS_ATTEMPTS) {
            if (runCatching { focusRequester.requestFocus() }.isSuccess) {
                keyboardController?.show()
                return@LaunchedEffect
            }
            delay(FOCUS_RETRY_MS)
        }
    }

    // Keep the cursor at the end when the query is replaced from outside (e.g. a feature chip)
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val value = if (fieldValue.text == query) fieldValue else TextFieldValue(query, TextRange(query.length))

    TextField(
        value = value,
        onValueChange = {
            fieldValue = it
            if (it.text != query) onQueryChange(it.text)
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (it.key) {
                        Key.DirectionDown -> {
                            onArrowDown()
                            true
                        }
                        Key.DirectionUp -> {
                            onArrowUp()
                            true
                        }
                        Key.DirectionLeft -> onArrowLeft()
                        Key.DirectionRight -> onArrowRight()
                        Key.Enter -> {
                            onEnter()
                            true
                        }
                        Key.Tab -> {
                            onTab(it.nativeKeyEvent.isShiftPressed)
                            true
                        }
                        else -> false
                    }
                } else {
                    // Swallow the release of keys acted on at press time. An unhandled Enter release
                    // bubbles up to the overlay's background clickable, which treats it as a click
                    // and closes the overlay (seen after Enter on an engine chip).
                    it.key == Key.Enter || it.key == Key.Tab || it.key == Key.DirectionDown || it.key == Key.DirectionUp
                }
            },
        placeholder = {
            Text("Search apps, web, math...", style = MaterialTheme.typography.titleLarge)
        },
        textStyle = MaterialTheme.typography.titleLarge,
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            // IME_FLAG_FORCE_ASCII: Japanese IMEs open in direct (alphanumeric) input; the user can still switch
            keyboardType = KeyboardType.Ascii,
            autoCorrect = false,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = { onEnter() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
