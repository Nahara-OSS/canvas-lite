package io.github.naharaoss.canvaslite.compose

import android.os.Parcel
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.canvaslite.R
import io.github.naharaoss.canvaslite.compose.panel.ColorPicker
import io.github.naharaoss.canvaslite.compose.panel.ColorPickerTab
import io.github.naharaoss.canvaslite.compose.panel.FlipbookPanel
import io.github.naharaoss.canvaslite.compose.panel.LayerBackgroundItem
import io.github.naharaoss.canvaslite.compose.panel.LayerItem
import io.github.naharaoss.canvaslite.compose.panel.LayersPanel

data class OverlayState(
    val colorPicker: Boolean = false,
    val colorPickerTab: ColorPickerTab = ColorPickerTab.Colors,
    val rightMenu: RightMenu = RightMenu.None,
    val bottomPane: BottomPane = BottomPane.None,
) : Parcelable {
    val any get() = colorPicker || rightMenu != RightMenu.None || bottomPane != BottomPane.None

    val asAllClosed get() = copy(
        colorPicker = false,
        rightMenu = RightMenu.None,
        bottomPane = BottomPane.None
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(if (colorPicker) 1 else 0)
        writeInt(colorPickerTab.ordinal)
        writeInt(rightMenu.ordinal)
        writeInt(bottomPane.ordinal)
    }

    companion object CREATOR : Parcelable.Creator<OverlayState> {
        override fun createFromParcel(source: Parcel?) = source?.let { source ->
            val colorPicker = source.readInt() != 0
            val colorPickerTab = ColorPickerTab.entries[source.readInt()]
            val rightMenu = RightMenu.entries[source.readInt()]
            val bottomPane = BottomPane.entries[source.readInt()]
            OverlayState(colorPicker, colorPickerTab, rightMenu, bottomPane)
        }

        override fun newArray(size: Int) = arrayOfNulls<OverlayState?>(size)
    }
}

enum class RightMenu {
    None,
    Hamburger,
    Layers,
}

enum class BottomPane {
    None,
    Flipbook,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Overlay(
    modifier: Modifier = Modifier,
    state: OverlayState,
    pickedColor: Color,
    lastColor: Color,
    layers: LazyListScope.() -> Unit,
    onStateChange: (OverlayState) -> Unit,
    onNavigateUp: () -> Unit,
    onPickColor: (Color) -> Unit,
    onAddLayer: () -> Unit,
    onPreferences: () -> Unit,
    onExport: () -> Unit
) {
    BackHandler(state.any) { onStateChange(state.asAllClosed) }

    OverlayLayout(
        modifier = modifier,
        leftMenus = {
            IconButton(
                onClick = onNavigateUp,
                content = { Icon(painterResource(R.drawable.arrow_back_24px), "Go back to library") }
            )
        },
        rightMenus = { singlePane ->
            FilledIconToggleButton(
                checked = state.bottomPane == BottomPane.Flipbook,
                onCheckedChange = { onStateChange((if (singlePane) state.asAllClosed else state).copy(bottomPane = if (it) BottomPane.Flipbook else BottomPane.None)) },
                content = { Icon(painterResource(R.drawable.animated_images_24px), "Flipbook") }
            )
            FilledIconToggleButton(
                checked = state.rightMenu == RightMenu.Layers,
                onCheckedChange = { onStateChange((if (singlePane) state.asAllClosed else state).copy(rightMenu = if (it) RightMenu.Layers else RightMenu.None)) },
                content = { Icon(painterResource(R.drawable.layers_24px), "Layers") }
            )
            FilledIconToggleButton(
                checked = state.rightMenu == RightMenu.Hamburger,
                onCheckedChange = { onStateChange((if (singlePane) state.asAllClosed else state).copy(rightMenu = if (it) RightMenu.Hamburger else RightMenu.None)) },
                content = { Icon(painterResource(R.drawable.menu_24px), "Menu") }
            )
        },
        rightMenuPanel = {
            AnimatedContent(
                targetState = state.rightMenu,
                contentAlignment = AbsoluteAlignment.TopRight
            ) { rightMenu ->
                when (rightMenu) {
                    RightMenu.Hamburger -> Column(Modifier.widthIn(max = 300.dp)) {
                        ListItem(
                            headlineContent = { Text("Canvas info") },
                            supportingContent = { Text("View edit time and rename") },
                            leadingContent = { Icon(painterResource(R.drawable.tune_24px), "Edit metadata") }
                        )
                        ListItem(
                            headlineContent = { Text("Export") },
                            supportingContent = { Text("Save as image or video") },
                            leadingContent = { Icon(painterResource(R.drawable.file_export_24px), "Export") },
                            modifier = Modifier.clickable(onClick = onExport)
                        )
                        ListItem(
                            headlineContent = { Text("Open in other app") },
                            supportingContent = { Text("Resume your work in other app") },
                            leadingContent = { Icon(painterResource(R.drawable.open_in_new_24px), "Export") }
                        )
                        ListItem(
                            headlineContent = { Text("Preferences") },
                            supportingContent = { Text("Configure the app") },
                            leadingContent = { Icon(painterResource(R.drawable.settings_24px), "Preferences") },
                            modifier = Modifier.clickable(onClick = onPreferences)
                        )
                    }
                    RightMenu.Layers -> LayersPanel(
                        modifier = Modifier.widthIn(max = 300.dp),
                        onAdd = onAddLayer,
                        content = layers
                    )
                    else -> {}
                }
            }
        },
        toolbar = { singlePane ->
            FilledIconToggleButton(
                checked = state.colorPicker,
                onCheckedChange = { onStateChange((if (singlePane) state.asAllClosed else state).copy(colorPicker = it)) },
                content = { Box(Modifier.size(24.dp).background(pickedColor, CircleShape)) }
            )
            FilledIconToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { Icon(painterResource(R.drawable.brush_24px), null) }
            )
            // TODO quick brush presets
            // there should be Add button here.
        },
        toolbarPanel = { contentAlignment ->
            AnimatedContent(
                targetState = state.colorPicker,
                contentAlignment = contentAlignment
            ) { visible ->
                if (visible) ColorPicker(
                    modifier = Modifier.widthIn(max = 400.dp),
                    selectedTab = state.colorPickerTab,
                    pickedColor = pickedColor,
                    lastColor = lastColor,
                    onSelectTab = { onStateChange(state.copy(colorPickerTab = it)) },
                    palette = listOf(),
                    onAddToPalette = {},
                    onPickColor = onPickColor,
                )
            }
        },
        bottomPanel = {
            AnimatedContent(
                targetState = state.bottomPane,
                contentAlignment = Alignment.BottomCenter
            ) { bottomPane ->
                when (bottomPane) {
                    BottomPane.Flipbook -> BoxWithConstraints {
                        FlipbookPanel(
                            modifier = if (maxWidth >= 600.dp) Modifier.fillMaxWidth(0.8f) else Modifier.fillMaxWidth(1f)
                        )
                    }
                    else -> {}
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverlayLayout(
    modifier: Modifier = Modifier,
    leftMenus: @Composable ((singlePane: Boolean) -> Unit),
    rightMenus: @Composable ((singlePane: Boolean) -> Unit),
    toolbar: @Composable ((singlePane: Boolean) -> Unit),
    toolbarPanel: @Composable ((Alignment) -> Unit),
    rightMenuPanel: @Composable (() -> Unit),
    bottomPanel: @Composable (() -> Unit),
) {
    val toolbarAlignment = BiasAbsoluteAlignment(-1f, 0f)

    BoxWithConstraints(modifier) {
        // On device with limited space, we force popups to open in single pane mode
        // In this mode, there can be at most 1 popup
        // (opening another pane automatically close existing)
        val singlePane = maxWidth < 600.dp || maxHeight < 480.dp

        HorizontalFloatingToolbar(
            modifier = Modifier.align(AbsoluteAlignment.TopLeft),
            expanded = false,
            content = { leftMenus(singlePane) }
        )

        HorizontalFloatingToolbar(
            modifier = Modifier.align(AbsoluteAlignment.TopRight),
            expanded = false,
            content = { rightMenus(singlePane) }
        )

        Column(
            modifier = Modifier.align(AbsoluteAlignment.TopRight),
            horizontalAlignment = AbsoluteAlignment.Right,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HorizontalFloatingToolbar(
                expanded = false,
                content = { rightMenus(singlePane) }
            )
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.extraLarge,
                content = rightMenuPanel
            )
        }

        when {
            maxWidth >= 600.dp -> {
                Row(
                    modifier = Modifier.align(toolbarAlignment),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VerticalFloatingToolbar(expanded = false) {
                        toolbar(singlePane)
                    }
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.extraLarge,
                        content = { toolbarPanel(toolbarAlignment) }
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.extraLarge,
                    content = bottomPanel
                )
            }
            else -> {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        toolbarPanel(Alignment.BottomCenter)
                        bottomPanel()
                    }
                    HorizontalFloatingToolbar(expanded = false) {
                        toolbar(singlePane)
                    }
                }
            }
        }
    }
}