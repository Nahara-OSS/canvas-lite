package io.github.naharaoss.canvaslite.compose.panel

import android.content.Context
import android.opengl.GLES20
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.naharaoss.canvaslite.R
import io.github.naharaoss.canvaslite.compose.ExpandingToggleButton
import io.github.naharaoss.canvaslite.ext.HSV
import io.github.naharaoss.canvaslite.ext.hsv
import io.github.naharaoss.canvaslite.ext.slideTransition
import io.github.naharaoss.canvaslite.gl.Program
import io.github.naharaoss.canvaslite.gl.Shader
import io.github.naharaoss.canvaslite.view.GLTextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    selectedTab: ColorPickerTab,
    pickedColor: Color,
    lastColor: Color,
    palette: List<Color>,
    onSelectTab: (ColorPickerTab) -> Unit,
    onPickColor: (Color) -> Unit,
    onAddToPalette: (Color) -> Unit
) {
    PanelScaffold(
        modifier = modifier,
        actions = {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                ExpandingToggleButton(
                    checked = selectedTab == ColorPickerTab.Colors,
                    onCheckedChange = { onSelectTab(ColorPickerTab.Colors) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Icon(painterResource(R.drawable.colors_24px), "Colors")
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text("Colors")
                }
                ExpandingToggleButton(
                    checked = selectedTab == ColorPickerTab.Palette,
                    onCheckedChange = { onSelectTab(ColorPickerTab.Palette) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Icon(painterResource(R.drawable.palette_24px), "Palette")
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text("Palette")
                }
            }
            IconButton(
                onClick = {},
                content = { Icon(painterResource(R.drawable.expand_content_24px), "Expand") }
            )
        }
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = slideTransition()
        ) { tab ->
            when (tab) {
                ColorPickerTab.Colors -> HsvColorPicker(
                    modifier = Modifier.padding(24.dp, 16.dp),
                    pickedColor = pickedColor,
                    lastColor = lastColor,
                    onPickColor = onPickColor,
                    onAddToPalette = {
                        onAddToPalette(it)
                        onSelectTab(ColorPickerTab.Palette)
                    }
                )
                ColorPickerTab.Palette -> LazyVerticalGrid(
                    modifier = Modifier.height(180.dp),
                    columns = GridCells.Adaptive(36.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(24.dp, 16.dp)
                ) {
                    item {
                        IconButton(onClick = { onAddToPalette(pickedColor) }) {
                            Icon(
                                painter = painterResource(R.drawable.add_24px),
                                contentDescription = "Add current color"
                            )
                        }
                    }
                    item {
                        FilledIconToggleButton(
                            checked = false,
                            onCheckedChange = {}
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.colorize_24px),
                                contentDescription = "Eyedropper"
                            )
                        }
                    }
                    items(palette) { color ->
                        FilledIconToggleButton(
                            checked = color == pickedColor,
                            onCheckedChange = { onPickColor(color) }
                        ) {
                            Box(Modifier.size(24.dp).background(color, CircleShape))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

enum class ColorPickerTab {
    Colors {
        @Composable
        override fun Label() {
            Text("Colors")
        }
    },
    Palette {
        @Composable
        override fun Label() {
            Text("Palette")
        }
    };

    @Composable
    abstract fun Label()
}

@Composable
private fun HsvColorPicker(
    modifier: Modifier = Modifier,
    pickedColor: Color,
    lastColor: Color,
    onPickColor: (Color) -> Unit,
    onAddToPalette: (Color) -> Unit
) {
    var pickerHue by remember(pickedColor) { mutableFloatStateOf(pickedColor.hsv.hue) }
    var pickerSat by remember(pickedColor) { mutableFloatStateOf(pickedColor.hsv.saturation) }
    var pickerVal by remember(pickedColor) { mutableFloatStateOf(pickedColor.hsv.value) }
    var pickedAlpha by remember(pickedColor) { mutableFloatStateOf(pickedColor.hsv.alpha) }
    val livePickerColor = HSV(pickerHue, pickerSat, pickerVal, pickedAlpha).rgb
    val contrastRingColor = listOf(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.inverseOnSurface
    ).maxBy { abs(livePickerColor.luminance() - it.luminance()) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onAddToPalette(pickedColor) }) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = "Add current color to palette"
                )
            }
            FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                Icon(
                    painter = painterResource(R.drawable.colorize_24px),
                    contentDescription = "Eyedropper"
                )
            }
            Row(Modifier
                .weight(1f)
                .height(48.dp)
                .clip(MaterialTheme.shapes.large)) {
                Box(Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(livePickerColor))
                Box(Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(lastColor)
                    .clickable(enabled = true) { onPickColor(lastColor) })
            }
        }
        Box(Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp, max = 256.dp)
            .pointerInput(pickedColor) {
                awaitEachGesture {
                    val pointer = awaitFirstDown()
                    pickerSat = min(max(pointer.position.x / size.width, 0f), 1f)
                    pickerVal = min(max(1f - pointer.position.y / size.height, 0f), 1f)

                    drag(pointer.id) { pointer ->
                        pickerSat = min(max(pointer.position.x / size.width, 0f), 1f)
                        pickerVal = min(max(1f - pointer.position.y / size.height, 0f), 1f)
                    }

                    onPickColor(pickedColor.hsv.copy(
                        hue = pickerHue,
                        saturation = pickerSat,
                        value = pickerVal
                    ).rgb)
                }
            }) {
            AndroidView(
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large),
                factory = { HueSatTextureView(it) },
                update = {
                    it.hue = pickerHue
                    it.requestRender()
                }
            )
            Box(Modifier
                .size(24.dp)
                .align { size, space, layoutDirection -> IntOffset(
                    x = (space.width * pickerSat).toInt() - size.width / 2,
                    y = (space.height * (1f - pickerVal)).toInt() - size.height / 2
                ) }
                .background(livePickerColor, CircleShape)
                .border(4.dp, contrastRingColor, CircleShape))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(painter = painterResource(R.drawable.colors_24px), contentDescription = "Hue")
                Slider(
                    value = pickerHue,
                    onValueChange = { pickerHue = it },
                    onValueChangeFinished = {
                        onPickColor(pickedColor.hsv.copy(
                            hue = pickerHue,
                            saturation = pickerSat,
                            value = pickerVal
                        ).rgb)
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = rainbowBrush, blendMode = BlendMode.SrcAtop)
                        }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(painter = painterResource(R.drawable.opacity_24px), contentDescription = "Opacity")
                Slider(
                    value = pickedAlpha,
                    onValueChange = { pickedAlpha = it },
                    onValueChangeFinished = { onPickColor(pickedColor.copy(alpha = pickedAlpha)) }
                )
            }
        }
    }
}

private val rainbowBrush = Brush.horizontalGradient(
    (0f / 6f) to Color(1f, 0f, 0f, 1f),
    (1f / 6f) to Color(1f, 1f, 0f, 1f),
    (2f / 6f) to Color(0f, 1f, 0f, 1f),
    (3f / 6f) to Color(0f, 1f, 1f, 1f),
    (4f / 6f) to Color(0f, 0f, 1f, 1f),
    (5f / 6f) to Color(1f, 0f, 1f, 1f),
    (6f / 6f) to Color(1f, 0f, 0f, 1f)
)

private class HueSatTextureView(context: Context) : GLTextureView(context) {
    var hue: Float = 0f

    init {
        setRenderer(object : Renderer {
            val vertices = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).apply {
                putFloat(-1f).putFloat(+1f).putFloat(0f).putFloat(1f)
                putFloat(+1f).putFloat(+1f).putFloat(1f).putFloat(1f)
                putFloat(-1f).putFloat(-1f).putFloat(0f).putFloat(0f)
                putFloat(+1f).putFloat(-1f).putFloat(1f).putFloat(0f)
                flip()
            }
            lateinit var program: Program
            var hue: Program.Uniform? = null
            var vertex: Program.Attribute? = null

            override fun onSurfaceCreated() {
                program = Program(
                    Shader(
                        type = Shader.Type.Vertex,
                        source = """
                            precision mediump float;
                            attribute vec4 vertex;
                            varying float sat;
                            varying float val;
                            
                            void main() {
                                gl_Position = vec4(vertex.xy, 0, 1);
                                sat = vertex.z;
                                val = vertex.w;
                            }
                        """.trimIndent()
                    ),
                    Shader(
                        type = Shader.Type.Fragment,
                        source =  """
                            precision mediump float;
                            uniform float hue;
                            varying float sat;
                            varying float val;
                            
                            void main() {
                                float h = hue * 6.0;
                                vec3 c = clamp(vec3(-1.0 + abs(h - 3.0), 2.0 - abs(h - 2.0), 2.0 - abs(h - 4.0)), 0.0, 1.0);
                                gl_FragColor = vec4(mix(vec3(1), c, sat) * val, 1);
                            }
                        """.trimIndent()
                    )
                )
                hue = program.uniform("hue")
                vertex = program.attribute("vertex")
            }

            override fun onDrawFrame() {
                program.use()
                vertex?.enable().use {
                    vertex?.setPointer(type = Program.AttribType.Float32x4, src = vertices)
                    hue?.uniform1f(this@HueSatTextureView.hue /360f)
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                }
            }
        })
    }
}