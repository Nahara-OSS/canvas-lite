package io.github.naharaoss.canvaslite.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.engine.PenInput
import io.github.naharaoss.canvaslite.engine.brush.Brush
import io.github.naharaoss.canvaslite.engine.brush.DebugBrush
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Layer
import io.github.naharaoss.canvaslite.engine.renderer.CanvasRenderer
import io.github.naharaoss.canvaslite.gl.getGLError
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates

open class DrawingCanvasView(context: Context) : GLSurfaceView(context) {
    private val viewRenderer = GLSurfaceViewRenderer()

    var eyedropperCallback: (Color) -> Unit = {}
    var transformCallback: (offset: Offset, scale: Float, rotation: Float) -> Unit = { _, _, _ -> }

    private var _canvas: Canvas? = null
    var canvas
        get() = _canvas
        set(value) {
            if (_canvas === value) return
            Log.d("DrawingCanvasView", "Received $value")
            _canvas = value
            queueEvent { viewRenderer.update(canvas = value) }
            requestRender()
        }

    private var _background = Color(0f, 0f, 0f, 1f)
    var background
        get() = _background
        set(value) {
            if (_background == value) return
            _background = value
            queueEvent { viewRenderer.update(background = value) }
            requestRender()
        }

    private var _canvasOffset = Offset(0f, 0f)
    var canvasOffset
        get() = _canvasOffset
        set(value) {
            if (_canvasOffset == value) return
            _canvasOffset = value
            queueEvent { viewRenderer.update(canvasOffset = value) }
            requestRender()
        }

    private var _canvasScale = 1f
    var canvasScale
        get() = _canvasScale
        set(value) {
            if (_canvasScale == value) return
            _canvasScale = value
            queueEvent { viewRenderer.update(canvasScale = value) }
            requestRender()
        }

    private var _canvasRotation = 0f
    var canvasRotation
        get() = _canvasRotation
        set(value) {
            if (_canvasRotation == value) return
            _canvasRotation = value
            queueEvent { viewRenderer.update(canvasRotation = value) }
            requestRender()
        }

    var brushColor = Color.Black
    var touchDrawing = false

    private var _selectedLayer: Layer? = null
    var selectedLayer
        get() = _selectedLayer
        set(value) {
            if (_selectedLayer === value) return
            _selectedLayer = value
            queueEvent { viewRenderer.update() }
            requestRender()
        }

    init {
        setEGLContextClientVersion(2)
        setRenderer(viewRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private class GLSurfaceViewRenderer : Renderer {
        var width by Delegates.notNull<Int>()
        var height by Delegates.notNull<Int>()
        var renderer: CanvasRenderer? = null
        lateinit var brush: Brush.GPUBrushInstance

        // Handle early setup
        var hasContext = false
        var canvas: Canvas? = null
        var background = Color(0f, 0f, 0f, 1f)
        var canvasOffset = Offset(0f, 0f)
        var canvasScale = 1f
        var canvasRotation = 0f

        val worldToViewport get() = Matrix().apply {
            scale(2f / width, 2f / height)
            translate(canvasOffset.x, canvasOffset.y)
            rotateZ(canvasRotation)
            scale(canvasScale, canvasScale)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            hasContext = true
            renderer = canvas?.let { CanvasRenderer(it, true) }
            brush = DebugBrush().createGPUInstance()
            getGLError()?.also { Log.d("DrawingCanvasView", "OpenGL error: $it") }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this.width = width
            this.height = height
            renderer?.update(0, worldToViewport)
        }

        fun update(
            canvas: Canvas? = renderer?.canvas ?: this.canvas,
            background: Color = this.background,
            canvasOffset: Offset = this.canvasOffset,
            canvasScale: Float = this.canvasScale,
            canvasRotation: Float = this.canvasRotation
        ) {
            this.canvas = canvas
            this.background = background
            this.canvasOffset = canvasOffset
            this.canvasScale = canvasScale
            this.canvasRotation = canvasRotation

            if (hasContext) {
                val renderer = this.renderer

                when {
                    renderer == null && canvas != null -> {
                        Log.d("DrawingCanvasView", "  New renderer")
                        this.renderer = CanvasRenderer(canvas, true)
                    }
                    renderer != null && canvas == null -> {
                        Log.d("DrawingCanvasView", "  Closing renderer")
                        renderer.close()
                        this.renderer = null
                    }
                    renderer != null && canvas != null && renderer.canvas !== canvas -> {
                        Log.d("DrawingCanvasView", "  Replacing renderer")
                        renderer.close()
                        this.renderer = CanvasRenderer(canvas, true)
                    }
                    else -> {}
                }
            }

            renderer?.update(0, worldToViewport)
            getGLError()?.also { Log.d("DrawingCanvasView", "OpenGL error: $it") }
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glViewport(0, 0, width, height)
            renderer?.renderToScreen(
                worldToViewport = worldToViewport,
                background = background,
                showOverflow = false
            )
            getGLError()?.also { Log.d("DrawingCanvasView", "OpenGL error: $it") }
        }

        private var usingBrush = false

        fun useBrush(
            inputs: List<PenInput>,
            finished: Boolean,
            layer: Layer,
            brushColor: Color
        ) {
            val strokeBounds = brush.calculateBox(inputs)
            if (strokeBounds == null) return

            renderer?.also { renderer ->
                if (!usingBrush) {
                    renderer.update(0, worldToViewport)
                    usingBrush = true
                }

                val tileSizeI = renderer.canvas.tileSize
                val tileSizeF = tileSizeI.toFloat()
                val tileSizeVec = Size(tileSizeF, tileSizeF)
                val layerView = renderer.layers[layer]
                Log.d("DrawingCanvasView", "Layer ${layer.name} / LayerView $layerView")
                if (layerView == null) return

                renderer.beginDraw()

                layerView.tiles.forEach { id, tileView ->
                    val (tileX, tileY) = id
                    val tileBounds = Rect(Offset(tileX.toFloat() * tileSizeF, tileY.toFloat() * tileSizeF), tileSizeVec)
                    if (!tileBounds.overlaps(strokeBounds)) return@forEach

                    tileView.consume(true) { content ->
                        content.target.bind {
                            GLES20.glViewport(0, 0, tileSizeI, tileSizeI)
                            brush.draw(
                                inputs = inputs,
                                boardToClip = Matrix().apply {
                                    translate(-1f, -1f)
                                    scale(2f / tileSizeF, 2f / tileSizeF)
                                    translate(-tileSizeF * tileX, -tileSizeF * tileY)
                                },
                                color = brushColor,
                                blending = Blending.StraightSourceOver
                            )
                        }
                    }
                }

                if (finished) {
                    usingBrush = false

                    for (tileView in renderer.endDraw()) {
                        val layer = tileView.layerView.layer
                        val address = Layer.TileAddress(tileView.x, tileView.y, tileView.frame)
                        tileView.consume(false) { content -> content.store(layer, address) }
                    }
                }
            }
        }

        fun captureThumbnail(): Bitmap? {
            val canvas = this.canvas
            val renderer = this.renderer
            if (canvas == null || renderer == null) return null

            val viewportWidth = if (canvas.canvasSize != null) canvas.canvasSize!!.width else 1024
            val viewportHeight = if (canvas.canvasSize != null) canvas.canvasSize!!.height else 1024
            val thumbnailWidth = if (viewportWidth > viewportHeight) 1024 else viewportWidth * 1024 / viewportHeight
            val thumbnailHeight = if (viewportWidth > viewportHeight) viewportHeight * 1024 / viewportWidth else 1024

            val dst = ByteBuffer.allocateDirect(thumbnailWidth * thumbnailHeight * 4).order(ByteOrder.nativeOrder())
            renderer.captureAsRgba(
                worldToViewport = if (canvas.canvasSize == null) worldToViewport else Matrix().apply {
                    scale(2f / viewportWidth, -2f / viewportHeight)
                },
                width = thumbnailWidth,
                height = thumbnailHeight,
                dst = dst
            )
            dst.flip()

            val bitmap = createBitmap(thumbnailWidth, thumbnailHeight)

            for (y in 0..<thumbnailHeight) {
                for (x in 0..<thumbnailWidth) {
                    val r = dst.get().toInt() shl 16
                    val g = dst.get().toInt() shl 8
                    val b = dst.get().toInt() shl 0
                    val a = dst.get().toInt() shl 24
                    bitmap[x, y] = r or g or b or a
                }
            }

            return bitmap
        }
    }

    private val inputHandler = object : MotionInputHandler() {
        override val viewSize: Size get() = Size(width.toFloat(), height.toFloat())
        override val canvasSize: Size get() = Size(0f, 0f)
        override val pan: Offset get() = _canvasOffset
        override val scale: Float get() = _canvasScale
        override val rotation: Float get() = _canvasRotation
        private var lastInput: PenInput? = null

        override fun requestUncoalesce(event: MotionEvent) = this@DrawingCanvasView.requestUnbufferedDispatch(event)

        override fun onNavigate(pan: Offset, scale: Float, rotate: Float) {
            // Thread-safe ahh strat
            val canvasOffset = _canvasOffset + pan
            val canvasScale = _canvasScale * scale
            val canvasRotation = _canvasRotation + rotate
            transformCallback(canvasOffset, canvasScale, canvasRotation)

            _canvasOffset = canvasOffset
            _canvasScale = canvasScale
            _canvasRotation = canvasRotation

            queueEvent {
                viewRenderer.update(
                    canvasOffset = canvasOffset,
                    canvasScale = canvasScale,
                    canvasRotation = canvasRotation
                )
            }

            requestRender()
        }

        override fun onInput(input: PenInput, finished: Boolean) {
            val lastInput = this.lastInput
            val inputs = lastInput?.let { listOf(it, input) } ?: listOf(input)
            val layer = _selectedLayer
            if (layer == null) return
            this.lastInput = if (finished) null else input

            queueEvent {
                viewRenderer.useBrush(
                    inputs = inputs,
                    finished = finished,
                    layer = layer,
                    brushColor = brushColor
                )

                if (finished) {
                    viewRenderer.captureThumbnail()?.also {
                        viewRenderer.canvas?.putThumbnail(it)
                    }
                }
            }

            requestRender()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?) = inputHandler.consume(event, touchDrawing)
}