package io.github.naharaoss.canvaslite.view

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.canvaslite.engine.PenInput
import io.github.naharaoss.canvaslite.ext.angle

abstract class MotionInputHandler {
    private var lastTouches = emptyList<Offset>()
    abstract val canvasSize: Size
    abstract val viewSize: Size
    abstract val pan: Offset
    abstract val scale: Float
    abstract val rotation: Float

    protected abstract fun requestUncoalesce(event: MotionEvent)
    protected abstract fun onNavigate(
        pan: Offset = Offset.Zero,
        scale: Float = 1f,
        rotate: Float = 0f
    )
    protected abstract fun onInput(input: PenInput, finished: Boolean = false)

    fun consume(
        event: MotionEvent?,
        touchDrawing: Boolean = false
    ): Boolean {
        if (event == null) return false
        if (event.pointerCount == 0) return false

        var stylusIndex = 0

        if (event.pointerCount > 1 || !touchDrawing) {
            var navigate = true
            val currTouches = mutableListOf<Offset>()

            for (i in 0..<event.pointerCount) {
                val type = event.getToolType(i)
                if (event.action != MotionEvent.ACTION_UP || event.actionIndex != i)
                    currTouches.add(Offset(event.getX(i), event.getY(i)))

                if (type == MotionEvent.TOOL_TYPE_STYLUS) {
                    stylusIndex = i
                    navigate = false
                    break
                }
            }

            if (navigate) {
                if (lastTouches.size != currTouches.size) {
                    lastTouches = currTouches
                    return true
                }

                when (currTouches.size) {
                    1 -> {
                        val delta = currTouches[0] - lastTouches[0]
                        onNavigate(pan = delta)
                    }
                    2 -> {
                        val lastMidpoint = (lastTouches[0] + lastTouches[1]) / 2f
                        val currMidpoint = (currTouches[0] + currTouches[1]) / 2f
                        val lastVec = lastTouches[1] - lastTouches[0]
                        val currVec = currTouches[1] - currTouches[0]
                        onNavigate(
                            pan = currMidpoint - lastMidpoint,
                            scale = currVec.getDistance() / lastVec.getDistance(),
                            rotate = currVec.angle - lastVec.angle
                        )
                    }
                    else -> {}
                }

                lastTouches = currTouches
                return true
            } else {
                lastTouches = currTouches
            }
        }

        val invViewTransform = Matrix()
        invViewTransform.translate(pan.x, pan.y)
        invViewTransform.scale(scale, scale)
        invViewTransform.rotateZ(rotation)
        invViewTransform.invert()

        fun eventToCanvasOffset(pointerIndex: Int): Offset {
            val input = Offset(
                x = event.getX(pointerIndex) - viewSize.width / 2f,
                y = event.getY(pointerIndex) - viewSize.height / 2f
            )
            return invViewTransform.map(input) + Offset(
                x = canvasSize.width / 2f,
                y = canvasSize.height / 2f
            )
        }

        fun eventToPenInput(pointerIndex: Int): PenInput {
            val pos = eventToCanvasOffset(pointerIndex)
            return PenInput(
                x = pos.x,
                y = pos.y,
                pressure = event.getPressure(pointerIndex)
            )
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                requestUncoalesce(event)
                onInput(eventToPenInput(stylusIndex))
            }
            MotionEvent.ACTION_MOVE -> {
                onInput(eventToPenInput(stylusIndex))
            }
            MotionEvent.ACTION_UP -> {
                onInput(eventToPenInput(stylusIndex), finished = true)
            }
        }

        if (lastTouches.isNotEmpty()) lastTouches = emptyList()
        return true
    }
}