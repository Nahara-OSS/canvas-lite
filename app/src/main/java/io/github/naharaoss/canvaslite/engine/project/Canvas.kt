package io.github.naharaoss.canvaslite.engine.project

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalResources
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.ext.ColorSerializer
import kotlinx.serialization.Serializable

interface Canvas {
    /**
     * The size of this canvas measured in pixels. The canvas is infinite if the size is not
     * present.
     */
    val canvasSize: CanvasSize?
    val tileSize: Int
    val layers: List<Layer>
    var background: Color
    var fps: Float

    // Editor values
    var offset: Offset
    var zoom: Float
    var rotation: Float
    var currentLayer: Int?
    var currentFrame: Int

    val bytesPerTile get() = tileSize * tileSize * 4

    fun addLayer(
        insertBefore: Int = layers.size,
        name: String = "Layer ${layers.size + 1}",
        blending: Blending = Blending.PremultipliedSourceOver,
        opacity: Float = 1f
    ): Layer

    fun removeLayer(layer: Layer)

    fun putThumbnail(bitmap: Bitmap) {}

    @Serializable
    data class CanvasSize(val width: Int, val height: Int) {
        override fun toString() = "${width}x${height}"

        companion object {
            val ScreenSize: CanvasSize @SuppressLint("ConfigurationScreenWidthHeight") @Composable get() {
                val displayMetrics = LocalResources.current.displayMetrics
                return CanvasSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
            }
        }
    }

    @Serializable
    data class CanvasPreset(
        val canvasSize: CanvasSize?,
        val tileSize: Int,
        @Serializable(with = ColorSerializer::class) val background: Color,
        val fps: Float,
    ) {
        companion object {
            val InfiniteCanvas get() = CanvasPreset(null, 1024, Color.White, 24f)
            val ScreenSize @Composable get() = CanvasPreset(CanvasSize.ScreenSize, 256, Color.White, 24f)
        }
    }
}