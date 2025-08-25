package io.github.naharaoss.canvaslite.engine.brush

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.engine.PenInput
import kotlin.math.max
import kotlin.math.min

interface Brush {
    // TODO: Brush parameters affected by stylus dynamics
    // The brush parameter function is like this: f(x) = base * curve(x)
    // (keeping it simple as this is meant to be a light sketching app)
    // - "x" is the value obtained from stylus (pressure, tilt, velocity, etc)
    // - "base" is the "slider value" provided by user
    // - The curve(x) function return values ranging from 0.00 to 1.00

    // Stylus sensors:
    // - Pressure
    // - Tilt (also known as "inverted altitude")
    // - Orientation (also known as "azimuth")
    // - Velocity (scale to 1000dp/s until someone asked for more)

    // Common parameters (shared between multiple brush engines):
    // - Size
    // - Opacity
    // - Flow

    // UX considerations
    // - Simplify the parameters UI: Hide the curve if the profile is constant, show otherwise

    /**
     * Create a new live GPU instance of this brush for drawing on OpenGL framebuffer. The instance
     * can only be used inside valid OpenGL context.
     */
    fun createGPUInstance(): GPUBrushInstance

    interface GPUBrushInstance : AutoCloseable {
        val brush: Brush

        /**
         * Calculate the bounding rectangle containing the stroke when drawing for given pen inputs.
         * This will be used to find which tiles will be affected by the brush, thus avoid issuing
         * draw calls on tiles where the stroke doesn't intersect. Typically, the implementation
         * would override this function to inflate the returned rectangle by brush size.
         *
         * @param inputs Stroke data.
         */
        fun calculateBox(inputs: List<PenInput>): Rect? = inputs
            .map { Rect(it.x, it.y, it.x, it.y) }
            .reduceOrNull { a, b -> Rect(
                left = min(a.left, b.left),
                top = min(a.top, b.top),
                right = max(a.right, b.right),
                bottom = max(a.bottom, b.bottom)
            ) }

        /**
         * Consume a list of pen inputs and draw on the current framebuffer using this brush
         * instance. This will be called on each tile that intersected with the stroke's bounding
         * box.
         *
         * @param inputs Stroke data.
         * @param boardToClip The transformation matrix that transforms the coordinates on drawing
         * board to clip space in framebuffer.
         * @param color The color that user have picked for this draw call.
         * @param blending The blending mode that user have picked for this draw call.
         */
        fun draw(
            inputs: List<PenInput>,
            boardToClip: Matrix,
            color: Color,
            blending: Blending
        )
    }
}