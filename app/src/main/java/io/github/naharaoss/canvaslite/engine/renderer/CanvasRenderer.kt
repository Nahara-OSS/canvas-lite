package io.github.naharaoss.canvaslite.engine.renderer

import android.opengl.GLES20
import android.util.Log
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Layer
import io.github.naharaoss.canvaslite.engine.utils.TilingViewportAssist
import io.github.naharaoss.canvaslite.gpu.GPUBlending
import io.github.naharaoss.canvaslite.gpu.GPUProgram
import io.github.naharaoss.canvaslite.gpu.GPURenderTarget
import io.github.naharaoss.canvaslite.gpu.GPUShader
import io.github.naharaoss.canvaslite.gpu.GPUTexture
import io.github.naharaoss.canvaslite.gpu.initTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.floor

/**
 * Help render the canvas to framebuffer.
 */
class CanvasRenderer(
    val canvas: Canvas,
    val flipY: Boolean = false
) : AutoCloseable {
    val tileSize = canvas.tileSize

    class TileContent(val size: Int) : AutoCloseable {
        val texture = GPUTexture(GPUTexture.Type.Texture2D).bind {
            minFilter = GPUTexture.Filter.Linear
            magFilter = GPUTexture.Filter.Nearest
            wrapS = GPUTexture.WrapMode.ClampToEdge
            wrapT = GPUTexture.WrapMode.ClampToEdge
            initTexture(size, size)
        }

        val target = GPURenderTarget().bind {
            attach(GPURenderTarget.Attachment.Color(), texture)
            ensureCompleted()
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }

        fun load(layer: Layer, address: Layer.TileAddress) {
            // TODO: Load from canvas to buffer in different thread
            val buf = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder())
            layer.loadTile(address, buf)
            buf.flip()
            texture.bind { initTexture(size, size, data = buf) }
        }

        fun store(layer: Layer, address: Layer.TileAddress) {
            // TODO: Store from buffer to canvas in different thread
            val buf = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder())
            target.bind { readPixels(0, 0, size, size, buf) }
            layer.storeTile(address, buf)
        }

        override fun close() {
            target.close()
            texture.close()
        }
    }

    class TileView(val layerView: LayerView, val x: Int, val y: Int) : AutoCloseable {
        var content: TileContent? = null
        var frame: Int = -1

        fun update(frame: Int) {
            val address = Layer.TileAddress(x, y, frame)
            val present = layerView.layer.isTilePresent(address)

            when {
                present && content == null -> content = TileContent(layerView.renderer.tileSize).also { it.load(layerView.layer, address) }
                !present && content != null -> if (!layerView.renderer.trackDrawingTiles) { content?.close(); content = null }
                this.frame != frame && present -> content?.load(layerView.layer, address)
                else -> {}
            }

            this.frame = frame
        }

        /**
         * Consume the tile view for either drawing or rendering. If the tile view is going to be
         * consumed for drawing, the tile content will be created if this tile view doesn't have
         * content previously. If the tile view is going to be rendered, the [block] will only be
         * invoked if this tile view is holding content.
         *
         * @param forDrawing Whether to consume the tile view for drawing on it.
         * @param block
         */
        fun consume(forDrawing: Boolean, block: (TileContent) -> Unit) {
            val content = this.content
            if (forDrawing && layerView.renderer.trackDrawingTiles) layerView.renderer.drawingTiles.add(this)

            if (content != null) {
                block(content)
            } else if (forDrawing) {
                val content = TileContent(layerView.renderer.tileSize)
                this.content = content
                block(content)
            }
        }

        override fun close() {
            content?.close()
        }
    }

    class LayerView(val renderer: CanvasRenderer, val layer: Layer) : AutoCloseable {
        val tiles = mutableMapOf<TilingViewportAssist.TileId, TileView>()

        fun onTileEnter(id: TilingViewportAssist.TileId) { tiles[id] = TileView(this, id.x, id.y) }
        fun onTileExit(id: TilingViewportAssist.TileId) = tiles.remove(id)?.close()
        fun update(frame: Int) = tiles.forEach { it.value.update(frame) }

        override fun close() {
            for (tileView in tiles.values) tileView.close()
            tiles.clear()
        }
    }

    private var trackDrawingTiles = false
    private val drawingTiles = mutableSetOf<TileView>()
    val layers = mutableMapOf<Layer, LayerView>()
    val assist = object : TilingViewportAssist() {
        override fun onTileEnter(id: TileId) = layers.forEach { it.value.onTileEnter(id) }
        override fun onTileExit(id: TileId) = layers.forEach { it.value.onTileExit(id) }
    }

    /**
     * Update the internal states of this renderer to match with canvas.
     *
     * @param frame The frame number of the renderer.
     * @param worldToViewport The transformation matrix that transforms world coordinates to
     * clip space.
     */
    fun update(frame: Int, worldToViewport: Matrix) {
        val canvasSize = canvas.canvasSize
        val tileSize = canvas.tileSize.toFloat()
        val removals = Collections.newSetFromMap<Layer>(IdentityHashMap())
        removals.addAll(layers.keys)

        for (layer in canvas.layers) {
            if (!removals.remove(layer)) {
                val view = LayerView(this, layer)
                layers[layer] = view
            }
        }

        for (layer in removals) {
            layers[layer]?.close()
            layers.remove(layer)
        }

        assist.update(
            worldToViewport = worldToViewport,
            tileSize = Size(tileSize, tileSize),
            minTileX = canvasSize?.let { floor(-it.width / tileSize).toInt() },
            maxTileX = canvasSize?.let { floor(+it.width / tileSize).toInt() },
            minTileY = canvasSize?.let { floor(-it.height / tileSize).toInt() },
            maxTileY = canvasSize?.let { floor(+it.height / tileSize).toInt() }
        )

        layers.forEach { it.value.update(frame) }
    }

    /**
     * Begin tracking tile views consumed for drawing. When tracking, the tiles will not be cleared
     * in [update].
     */
    fun beginDraw() {
        trackDrawingTiles = true
    }

    /**
     * Finish tracking tile views consumed for drawing and return a list of tiles that are tracked
     * for drawing.
     */
    fun endDraw(): List<TileView> {
        trackDrawingTiles = false
        val list = drawingTiles.toList()
        drawingTiles.clear()
        Log.d("CanvasRenderer", "${list.size} tiles marked for storing to canvas")
        return list
    }

    val tileProgram: GPUProgram
    val tileWorldToViewport: GPUProgram.Uniform?
    val tileWorldPosition: GPUProgram.Uniform?
    val tileWorldSize: GPUProgram.Uniform?
    val tileTexture: GPUProgram.Uniform?
    val tileColorize: GPUProgram.Uniform?
    val tileVertex: GPUProgram.Attribute?

    private val tileVertices = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).apply {
        putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f)
        putFloat(1f).putFloat(0f).putFloat(1f).putFloat(0f)
        putFloat(0f).putFloat(1f).putFloat(0f).putFloat(1f)
        putFloat(1f).putFloat(1f).putFloat(1f).putFloat(1f)
        flip()
    }

    private val backgroundTexture = GPUTexture(GPUTexture.Type.Texture2D).bind {
        minFilter = GPUTexture.Filter.Linear
        magFilter = GPUTexture.Filter.Linear
        initTexture(1, 1, data = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).apply {
            put(0xFF.toByte())
            put(0xFF.toByte())
            put(0xFF.toByte())
            put(0xFF.toByte())
            flip()
        })
    }

    init {
        val vertexShader = GPUShader(GPUShader.Type.Vertex, """
            precision mediump float;
            uniform mat4 worldToViewport;
            uniform vec2 worldPosition;
            uniform vec2 worldSize;
            attribute vec4 vertex;
            varying vec2 uv;
            
            void main() {
                vec2 vertexWorldPos = vertex.xy * worldSize + worldPosition;
                gl_Position = worldToViewport * vec4(vertexWorldPos, 0, 1);
                ${if (flipY) "gl_Position.y = -gl_Position.y;" else "// flipY == false"}
                uv = vertex.zw;
            }
            """.trimIndent())
        val fragmentShader = GPUShader(GPUShader.Type.Fragment, """
            precision mediump float;
            uniform sampler2D texture;
            uniform vec4 colorize;
            varying vec2 uv;
            
            void main() {
                gl_FragColor = texture2D(texture, uv) * colorize;
            }
            """.trimIndent())

        tileProgram = GPUProgram(vertexShader, fragmentShader)
        vertexShader.close()
        fragmentShader.close()

        tileWorldToViewport = tileProgram.uniform("worldToViewport")
        tileWorldPosition = tileProgram.uniform("worldPosition")
        tileWorldSize = tileProgram.uniform("worldSize")
        tileTexture = tileProgram.uniform("texture")
        tileColorize = tileProgram.uniform("colorize")
        tileVertex = tileProgram.attribute("vertex")
    }

    fun renderBackground(worldToViewport: Matrix) {
        val size = canvas.canvasSize
        val color = canvas.background

        if (size != null) {
            val size = Size(size.width.toFloat(), size.height.toFloat())

            tileProgram.bind {
                backgroundTexture.bind(0) {
                    tileVertex?.enableArray()?.use { tileVertex ->
                        tileVertex.setPointer(GPUProgram.AttribType.Float4, src = tileVertices)
                        tileWorldPosition?.setValue2f(size / -2f)
                        tileWorldSize?.setValue2f(size)
                        tileWorldToViewport?.setValueMatrix4f(worldToViewport)
                        tileTexture?.setValue1i(0)
                        tileColorize?.setValue4f(color)
                        drawArrays(GPUProgram.Topology.TriangleStrip, 4)
                    }
                }
            }
        }
    }

    /**
     * Render the content of canvas to current framebuffer. This does not render the background of
     * the canvas.
     */
    fun renderContent(worldToViewport: Matrix) {
        GPUBlending.enable {
            tileProgram.bind {
                tileWorldSize?.setValue2f(tileSize.toFloat(), tileSize.toFloat())
                tileWorldToViewport?.setValueMatrix4f(worldToViewport)
                tileColorize?.setValue4f(Color.White)

                tileVertex?.enableArray()?.use { tileVertex ->
                    tileVertex.setPointer(GPUProgram.AttribType.Float4, src = tileVertices)

                    for (layer in canvas.layers) {
                        val layerView = layers[layer]
                        if (layerView == null) continue

                        layer.blending.gpu.applyBlending()

                        for ((id, tileView) in layerView.tiles) {
                            tileView.consume(false) { content ->
                                content.texture.bind(0) {
                                    val (tileX, tileY) = id
                                    tileWorldPosition?.setValue2f(tileX * tileSize.toFloat(), tileY * tileSize.toFloat())
                                    tileTexture?.setValue1i(0)
                                    drawArrays(GPUProgram.Topology.TriangleStrip, 4)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Render the entire canvas for displaying to current framebuffer.
     *
     * @param worldToViewport Transformation matrix that transforms world space into OpenGL clip
     * space.
     * @param background Background of the drawing board.
     * @param showOverflow Whether to show the tiles overflowing outside the canvas.
     */
    fun renderToScreen(
        worldToViewport: Matrix,
        background: Color = Color(0.5f, 0.5f, 0.5f, 1f),
        showOverflow: Boolean = false,
    ) = when {
        canvas.canvasSize != null && showOverflow -> {
            renderBackground(worldToViewport)
            renderContent(worldToViewport)
        }
        canvas.canvasSize != null && !showOverflow -> {
            GLES20.glEnable(GLES20.GL_STENCIL_TEST)
            GLES20.glClearColor(background.red, background.green, background.blue, background.alpha)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_STENCIL_BUFFER_BIT)

            GLES20.glStencilMask(0xFF)
            GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0b1, 0xFF)
            GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_REPLACE)
            renderBackground(worldToViewport)

            GLES20.glStencilMask(0x00)
            GLES20.glStencilFunc(GLES20.GL_EQUAL, 0b1, 0xFF)
            renderContent(worldToViewport)

            GLES20.glDisable(GLES20.GL_STENCIL_TEST)
        }
        else -> {
            val background = canvas.background
            GLES20.glClearColor(background.red, background.green, background.blue, background.alpha)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            renderContent(worldToViewport)
        }
    }

    override fun close() {
        trackDrawingTiles = false
        drawingTiles.clear()
        assist.clear()
        layers.forEach { it.value.close() }
        layers.clear()

        tileProgram.close()
        backgroundTexture.close()
    }
}