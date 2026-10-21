package com.quartz.powerline.ui.components

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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

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
    focusKey: Any = Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusKey) {
        focusRequester.requestFocus()
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
                        else -> false
                    }
                } else {
                    false
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onEnter() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
