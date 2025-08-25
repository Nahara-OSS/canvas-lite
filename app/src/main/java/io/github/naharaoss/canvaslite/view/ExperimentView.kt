package io.github.naharaoss.canvaslite.view

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.engine.PenInput
import io.github.naharaoss.canvaslite.engine.brush.Brush
import io.github.naharaoss.canvaslite.engine.brush.DebugBrush
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Layer
import io.github.naharaoss.canvaslite.engine.project.MemoryCanvas
import io.github.naharaoss.canvaslite.engine.renderer.CanvasRenderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class ExperimentView(context: Context) : GLSurfaceView(context) {
    private val renderer = CanvasViewRenderer(this)
    private var _canvas: Canvas = MemoryCanvas(canvasSize = Canvas.CanvasSize(1080, 2280), tileSize = 256).apply { addLayer() }

    var canvas
        get() = _canvas
        set(value) {
            _canvas = value

            queueEvent {
                renderer.renderer?.update(0, Matrix().apply {
                    scale(2f / width.toFloat(), 2f / height.toFloat())
                    translate(canvasOffset.x, canvasOffset.y)
                    rotateZ(canvasRotation)
                    scale(canvasScale, canvasScale)
                })
            }

            requestRender()
        }

    var touchDrawing = false
    var brushColor = Color.Black
    var canvasOffset = Offset.Zero
    var canvasScale = 1f
    var canvasRotation = 0f

    private class CanvasViewRenderer(val view: ExperimentView) : Renderer {
        var renderer: CanvasRenderer? = null
        lateinit var brush: Brush.GPUBrushInstance

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            renderer = CanvasRenderer(view.canvas, flipY = true).also { renderer ->
                renderer.update(0, Matrix().apply {
                    scale(2f / view.width.toFloat(), 2f / view.height.toFloat())
                    translate(view.canvasOffset.x, view.canvasOffset.y)
                    rotateZ(view.canvasRotation)
                    scale(view.canvasScale, view.canvasScale)
                })
            }
            brush = DebugBrush().createGPUInstance()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glViewport(0, 0, view.width, view.height)
            renderer?.renderToScreen(Matrix().apply {
                scale(2f / view.width.toFloat(), 2f / view.height.toFloat())
                translate(view.canvasOffset.x, view.canvasOffset.y)
                rotateZ(view.canvasRotation)
                scale(view.canvasScale, view.canvasScale)
            })
        }

        fun draw(inputs: List<PenInput>, finished: Boolean) {
            val strokeBounds = brush.calculateBox(inputs)
            val tileSize = view.canvas.tileSize.toFloat()
            if (strokeBounds == null) return

            renderer?.also { renderer ->
                if (view.canvas.layers.isEmpty()) return
                val layer = view.canvas.layers[0] // TODO
                val layerView = renderer.layers[layer]
                if (layerView == null) return

                renderer.beginDraw()
                layerView.tiles.forEach { id, tileView ->
                    val (tileX, tileY) = id
                    val tileRect = Rect(Offset(tileSize * tileX, tileSize * tileY), Size(tileSize, tileSize))
                    if (!tileRect.overlaps(strokeBounds)) return@forEach

                    tileView.consume(true) { content ->
                        content.target.bind {
                            GLES20.glViewport(0, 0, view.canvas.tileSize, view.canvas.tileSize)
                            brush.draw(
                                inputs = inputs,
                                boardToClip = Matrix().apply {
                                    translate(-1f, -1f)
                                    scale(2f / tileSize, 2f / tileSize)
                                    translate(-tileSize * tileX, -tileSize * tileY)
                                },
                                color = view.brushColor,
                                blending = Blending.StraightSourceOver
                            )
                        }
                    }
                }

                if (finished) for (tileView in renderer.endDraw()) {
                    val layer = tileView.layerView.layer
                    val address = Layer.TileAddress(tileView.x, tileView.y, tileView.frame)
                    tileView.consume(false) { content -> content.store(layer, address) }
                }
            }
        }
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private val inputHandler = object : MotionInputHandler() {
        override val canvasSize = Size(0f, 0f)
        override val viewSize: Size get() = Size(width.toFloat(), height.toFloat())
        override val pan: Offset get() = canvasOffset
        override val scale: Float get() = canvasScale
        override val rotation: Float get() = canvasRotation
        private var lastInput: PenInput? = null

        override fun requestUncoalesce(event: MotionEvent) = this@ExperimentView.requestUnbufferedDispatch(event)

        override fun onInput(input: PenInput, finished: Boolean) {
            val lastInput = this.lastInput
            val inputs = lastInput?.let { listOf(it, input) } ?: listOf(input)
            queueEvent { renderer.draw(inputs, finished) }
            this.lastInput = if (finished) null else input
            requestRender()
        }

        override fun onNavigate(pan: Offset, scale: Float, rotate: Float) {
            canvasOffset += pan
            canvasScale *= scale
            canvasRotation += rotate

            val width = width
            val height = height

            queueEvent {
                renderer.renderer?.update(0, Matrix().apply {
                    scale(2f / width.toFloat(), 2f / height.toFloat())
                    translate(canvasOffset.x, canvasOffset.y)
                    rotateZ(canvasRotation)
                    scale(canvasScale, canvasScale)
                })
            }

            requestRender()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = inputHandler.consume(event, touchDrawing)
}
