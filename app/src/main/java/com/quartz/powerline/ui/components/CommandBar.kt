package com.quartz.powerline.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onEnter: () -> Unit,
    onArrowDown: () -> Unit,
    onArrowUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
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
            Text(
                "Search apps, web, math...",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        leadingIcon = {
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { onQueryChange("") },
                enabled = query.isNotEmpty(),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Clear")
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onEnter() }),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        shape = MaterialTheme.shapes.extraLarge
    )
}
