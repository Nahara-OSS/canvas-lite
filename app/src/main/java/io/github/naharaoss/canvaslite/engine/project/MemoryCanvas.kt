package io.github.naharaoss.canvaslite.engine.project

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.naharaoss.canvaslite.engine.Blending
import java.nio.ByteBuffer

class MemoryCanvas(
    override val canvasSize: Canvas.CanvasSize? = null,
    override val tileSize: Int = 256,
    override var fps: Float = 24f,
    override var background: Color = Color.White,
    override var offset: Offset = Offset.Zero,
    override var zoom: Float = 1f,
    override var rotation: Float = 0f,
    override var currentLayer: Int? = null,
    override var currentFrame: Int = 0
) : Canvas {
    override val layers = mutableListOf<Layer>()

    override fun addLayer(
        insertBefore: Int,
        name: String,
        blending: Blending,
        opacity: Float
    ): Layer {
        val layer = MemoryLayer(this, name, blending, opacity)
        layers.add(insertBefore, layer)
        if (currentLayer == null) currentLayer = layers.indexOf(layer)
        return layer
    }

    override fun removeLayer(layer: Layer) {
        layers.remove(layer)
    }

    private class MemoryLayer(
        val canvas: MemoryCanvas,
        override var name: String,
        override var blending: Blending,
        override var opacity: Float,
        val content: MutableMap<Layer.TileAddress, ByteArray> = mutableMapOf()
    ) : Layer {
        override fun isTilePresent(address: Layer.TileAddress) = content.contains(address)

        override fun loadTile(address: Layer.TileAddress, dst: ByteBuffer): Boolean {
            if (dst.remaining() < canvas.bytesPerTile) throw IllegalArgumentException("The buffer need to read at least ${canvas.bytesPerTile} bytes, but only ${dst.remaining()} bytes remaining")
            return content[address]?.also { dst.put(it) } != null
        }

        override fun storeTile(address: Layer.TileAddress, src: ByteBuffer) {
            if (src.remaining() < canvas.bytesPerTile) throw IllegalArgumentException("The buffer need to write at least ${canvas.bytesPerTile} bytes, but only ${src.remaining()} bytes remaining")
            val tileContent = content.computeIfAbsent(address) { ByteArray(canvas.bytesPerTile) }
            src.get(tileContent)
        }
    }
}