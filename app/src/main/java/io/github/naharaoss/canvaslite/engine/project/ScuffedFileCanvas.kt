package io.github.naharaoss.canvaslite.engine.project

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.ext.ColorSerializer
import io.github.naharaoss.canvaslite.ext.OffsetSerializer
import io.github.naharaoss.canvaslite.ext.readAsJson
import io.github.naharaoss.canvaslite.ext.writeAsJson
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.io.Serial
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.min

class ScuffedFileCanvas(val root: File) : Canvas {
    private var _metadata: Metadata

    var metadata
        get() = _metadata
        set(value) {
            if (_metadata == value) return
            _metadata = value
            File(root, "metadata.json").writeAsJson(value)
        }

    override val canvasSize: Canvas.CanvasSize? get() = _metadata.canvasSize
    override val tileSize: Int get() = _metadata.tileSize
    override var fps: Float
        get() = _metadata.fps
        set(value) { metadata = metadata.copy(fps = value) }
    override var background: Color
        get() = _metadata.background
        set(value) { metadata = metadata.copy(background = value) }
    override var offset: Offset
        get() = _metadata.offset
        set(value) { metadata = metadata.copy(offset = value) }
    override var zoom: Float
        get() = _metadata.zoom
        set(value) { metadata = metadata.copy(zoom = value) }
    override var rotation: Float
        get() = _metadata.rotation
        set(value) { metadata = metadata.copy(rotation = value) }
    override var currentLayer: Int?
        get() = _metadata.currentLayer
        set(value) { metadata = metadata.copy(currentLayer = value) }
    override var currentFrame: Int
        get() = _metadata.currentFrame
        set(value) { metadata = metadata.copy(currentFrame = value) }

    private val _layers = mutableListOf<FileLayer>()
    override val layers: List<Layer> get() = _layers

    init {
        _metadata = File(root, "metadata.json").readAsJson<Metadata>()!!
        _layers.addAll(metadata.layers.map { FileLayer(this, it, File(root, it)) })
    }

    override fun addLayer(insertBefore: Int, name: String, blending: Blending, opacity: Float): Layer {
        val layerId = "layer-${UUID.randomUUID()}"
        val layerRoot = File(root, layerId)
        layerRoot.mkdirs()

        val layerMeta = FileLayer.Metadata(name, blending, opacity, emptySet())
        File(layerRoot, "metadata.json").writeAsJson(layerMeta)

        val layer = FileLayer(this, layerId, File(root, layerId))
        _layers.add(insertBefore, layer)
        metadata = metadata.copy(layers = _layers.map { it.layerId })
        if (currentLayer == null) currentLayer = _layers.indexOf(layer)
        return layer
    }

    override fun removeLayer(layer: Layer) {
        if (layer !is FileLayer) return
        val currentlySelected = _layers.indexOf(layer) != -1
        _layers.remove(layer)
        metadata = metadata.copy(layers = _layers.map { it.layerId })
        layer.root.deleteRecursively()
        if (currentlySelected) currentLayer = if (layers.isNotEmpty()) layers.lastIndex else null
    }

    override fun putThumbnail(bitmap: Bitmap) {
        FileOutputStream(File(root, "thumbnail.png")).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
        }
    }

    private class FileLayer(
        val canvas: ScuffedFileCanvas,
        val layerId: String,
        val root: File
    ) : Layer {
        private var _metadata: Metadata
        var metadata
            get() = _metadata
            set(value) {
                _metadata = value
                File(root, "metadata.json").writeAsJson(value)
            }

        override var name: String
            get() = _metadata.name
            set(value) { metadata = metadata.copy(name = value) }
        override var blending: Blending
            get() = _metadata.blending
            set(value) { metadata = metadata.copy(blending = value) }
        override var opacity: Float
            get() = _metadata.opacity
            set(value) { metadata = metadata.copy(opacity = value) }

        init { _metadata = File(root, "metadata.json").readAsJson()!! }

        private fun fileFromAddress(address: Layer.TileAddress) = File(root, "frame${address.frame}/tile${address.x}x${address.y}.bin")
        override fun isTilePresent(address: Layer.TileAddress) = _metadata.tiles.contains(address)

        override fun loadTile(address: Layer.TileAddress, dst: ByteBuffer): Boolean {
            val file = fileFromAddress(address)
            if (!file.exists()) return false

            RandomAccessFile(file, "r").use { file ->
                var bytesLeft = canvas.bytesPerTile
                val buffer = ByteArray(4096)

                while (bytesLeft > 0) {
                    val bytesRead = file.read(buffer, 0, min(buffer.size, bytesLeft))
                    dst.put(buffer, 0, bytesRead)
                    bytesLeft -= bytesRead
                }
            }

            return true
        }

        override fun storeTile(address: Layer.TileAddress, src: ByteBuffer) {
            val file = fileFromAddress(address)
            File(file, "..").mkdirs()
            RandomAccessFile(file, "rw").use { file ->
                file.seek(0)
                var bytesLeft = canvas.bytesPerTile
                val buffer = ByteArray(4096)

                while (bytesLeft > 0) {
                    val bytesToWrite = min(buffer.size, bytesLeft)
                    src.get(buffer, 0, bytesToWrite)
                    file.write(buffer, 0, bytesToWrite)
                    bytesLeft -= bytesToWrite
                }
            }

            if (!_metadata.tiles.contains(address)) metadata = metadata.copy(tiles = _metadata.tiles + address)
        }

        @Serializable
        data class Metadata(
            val name: String,
            val blending: Blending,
            val opacity: Float,
            val tiles: Set<Layer.TileAddress>
        )
    }

    companion object {
        fun isInitialized(root: File) = File(root, "metadata.json").exists()

        fun initialize(root: File, preset: Canvas.CanvasPreset) {
            root.mkdirs()
            val metafile = File(root, "metadata.json")
            val metadata = Metadata(
                canvasSize = preset.canvasSize,
                tileSize = preset.tileSize,
                fps = preset.fps,
                background = preset.background,
                layers = emptyList(),
                offset = Offset.Zero,
                zoom = 1f,
                rotation = 0f,
                currentLayer = null,
                currentFrame = 0
            )
            if (metafile.exists()) throw IOException("Cannot initialize in existing project")
            metafile.writeAsJson(metadata)
        }
    }

    @Serializable
    data class Metadata(
        val canvasSize: Canvas.CanvasSize?,
        val tileSize: Int,
        val fps: Float,
        @Serializable(with = ColorSerializer::class) val background: Color,
        val layers: List<String>,
        @Serializable(with = OffsetSerializer::class) val offset: Offset,
        val zoom: Float,
        val rotation: Float,
        val currentLayer: Int?,
        val currentFrame: Int
    )
}