package io.github.naharaoss.canvaslite.ext

import androidx.compose.ui.graphics.Color

data class HSV(val hue: Float, val saturation: Float, val value: Float, val alpha: Float) {
    val rgb get() = Color.hsv(hue, saturation, value, alpha)
}

val Color.hsv: HSV
    get() {
        val (r, g, b, a) = this
        val (h, s, v) = FloatArray(3).also {
            android.graphics.Color.RGBToHSV(
                (r * 255f).toInt(),
                (g * 255f).toInt(),
                (b * 255f).toInt(),
                it
            )
        }
        return HSV(h, s, v, a)
    }

val Color.isBlack get() = this.red == 0f && this.green == 0f && this.blue == 0f