package io.github.naharaoss.canvaslite.compose.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.naharaoss.canvaslite.R
import io.github.naharaoss.canvaslite.compose.ExpandingToggleButton
import io.github.naharaoss.canvaslite.engine.project.Canvas

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomCanvasDialog(
    initialPreset: Canvas.CanvasPreset,
    onDismissRequest: () -> Unit,
    onCreate: (Canvas.CanvasPreset) -> Unit
) {
    val screenCanvasSize = Canvas.CanvasSize.ScreenSize
    var presetMode by rememberSaveable { mutableStateOf(if (initialPreset.canvasSize != null) PresetMode.Sized else PresetMode.Infinite) }
    var canvasSize by remember { mutableStateOf(initialPreset.canvasSize ?: screenCanvasSize) } // TODO saveable
    var widthAsText by remember(canvasSize) { mutableStateOf(canvasSize.width.toString()) }
    var heightAsText by remember(canvasSize) { mutableStateOf(canvasSize.height.toString()) }
    var fps by rememberSaveable { mutableFloatStateOf(initialPreset.fps) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.secondary,
                        content = { Icon(painterResource(R.drawable.tune_24px), null) }
                    )
                    Text(
                        text = "Custom canvas",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        for (mode in PresetMode.entries) {
                            ExpandingToggleButton(
                                checked = presetMode == mode,
                                onCheckedChange = { presetMode = mode },
                                shapes = when (mode) {
                                    PresetMode.entries.first() -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    PresetMode.entries.last() -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Icon(painterResource(mode.icon), mode.longLabel)
                                Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                                Text(mode.shortLabel)
                            }
                        }
                    }
                    AnimatedContent(
                        modifier = Modifier.fillMaxWidth(),
                        targetState = presetMode
                    ) { presetMode ->
                        when (presetMode) {
                            PresetMode.Sized -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    TextField(
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Width") },
                                        value = widthAsText,
                                        onValueChange = {
                                            widthAsText = it
                                            it.toIntOrNull()?.let { value -> canvasSize = canvasSize.copy(width = value) }
                                        },
                                        isError = widthAsText.toIntOrNull() == null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    TextField(
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Height") },
                                        value = heightAsText,
                                        onValueChange = {
                                            heightAsText = it
                                            it.toIntOrNull()?.let { value -> canvasSize = canvasSize.copy(height = value) }
                                        },
                                        isError = heightAsText.toIntOrNull() == null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                            PresetMode.Infinite -> Text(
                                text = "Create a new canvas without size limitation. This kind of canvas may consumes more memory and storage than usual.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> {}
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onDismissRequest) { Text("Cancel") }
                    TextButton(
                        enabled = when (presetMode) {
                            PresetMode.Sized -> widthAsText.toIntOrNull() != null && heightAsText.toIntOrNull() != null
                            PresetMode.Infinite -> true
                            else -> false
                        },
                        onClick = {
                            when (presetMode) {
                                PresetMode.Sized -> onCreate(Canvas.CanvasPreset(
                                    canvasSize = canvasSize,
                                    tileSize = 256,
                                    background = Color.White,
                                    fps = fps
                                ))
                                PresetMode.Infinite -> onCreate(Canvas.CanvasPreset(
                                    canvasSize = null,
                                    tileSize = 1024,
                                    background = Color.White,
                                    fps = fps
                                ))
                                else -> {}
                            }
                        },
                        content = { Text("Create") }
                    )
                }
            }
        }
    }
}

private enum class PresetMode(val icon: Int, val shortLabel: String, val longLabel: String) {
    Sized(R.drawable.wall_art_24px, "Sized", "Sized canvas"),
    Infinite(R.drawable.all_inclusive_24px, "Infinite", "Infinite canvas"),
}