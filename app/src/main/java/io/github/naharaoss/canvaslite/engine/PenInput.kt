package io.github.naharaoss.canvaslite.engine

import androidx.compose.ui.util.lerp
import kotlin.math.pow
import kotlin.math.sqrt

data class PenInput(
    /**
     * The X position of the pen on drawing board.
     */
    val x: Float,

    /**
     * The Y position of the pen on drawing board.
     */
    val y: Float,

    /**
     * The logical pen pressure, which is how hard user is pressing the pen against the surface.
     */
    val pressure: Float,
) {
    infix fun distanceTo(another: PenInput) = sqrt((another.x - x).pow(2f) + (another.y - y).pow(2f))
}

fun lerp(a: PenInput, b: PenInput, fraction: Float) = PenInput(
    x = lerp(a.x, b.x, fraction),
    y = lerp(a.y, b.y, fraction),
    pressure = lerp(a.pressure, b.pressure, fraction),
)