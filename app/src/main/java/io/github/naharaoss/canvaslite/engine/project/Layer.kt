package io.github.naharaoss.canvaslite.engine.project

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.set
import io.github.naharaoss.canvaslite.engine.Blending
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

interface Layer {
    var name: String
    var blending: Blending
    var opacity: Float

    fun isTilePresent(address: TileAddress): Boolean
    fun loadTile(address: TileAddress, dst: ByteBuffer): Boolean
    fun storeTile(address: TileAddress, src: ByteBuffer)

    fun copyPixels(
        dst: Bitmap,
        frame: Int,
        srcX: Int,
        srcY: Int,
        dstX: Int,
        dstY: Int,
        width: Int,
        height: Int,
        tileSize: Int
    ) {
        val minTileX = floor(srcX / tileSize.toFloat()).toInt()
        val maxTileX = floor((srcX + width) / tileSize.toFloat()).toInt()
        val minTileY = floor(srcY / tileSize.toFloat()).toInt()
        val maxTileY = floor((srcY + height) / tileSize.toFloat()).toInt()

        for (tileY in minTileY..maxTileY) {
            val tileTopLeftY = tileY * tileSize

            for (tileX in minTileX..maxTileX) {
                val address = TileAddress(tileX, tileY, frame)
                if (!isTilePresent(address)) continue

                val tileTopLeftX = tileX * tileSize
                val tileSrcX = max(tileTopLeftX, srcX) - tileTopLeftX
                val tileSrcY = max(tileTopLeftY, srcY) - tileTopLeftY
                val tileDstX = dstX + max(tileTopLeftX, srcX) - srcX
                val tileDstY = dstY + max(tileTopLeftY, srcY) - srcY
                val tileSrcWidth = min(tileSize - tileSrcX, width - tileDstX)
                val tileSrcHeight = min(tileSize - tileSrcY, height - tileDstY)
                val src = ByteBuffer.allocate(tileSize * tileSize * 4).also { loadTile(address, it); it.flip() }.array()

                Log.d("Layer", "Copying pixels from ($tileX; $tileY): Source ($tileSrcX; $tileSrcY) - Size ($tileSrcWidth; $tileSrcHeight)")

                for (y in 0..<tileSrcHeight) {
                    val srcY = tileSrcY + y

                    for (x in 0..<tileSrcWidth) {
                        val srcX = tileSrcX + x
                        val srcAddress = (srcY * tileSize + srcX) * 4
                        val r = (src[srcAddress + 0].toInt() and 0xFF) shl 16
                        val g = (src[srcAddress + 1].toInt() and 0xFF) shl 8
                        val b = (src[srcAddress + 2].toInt() and 0xFF) shl 0
                        val a = (src[srcAddress + 3].toInt() and 0xFF) shl 24
                        dst[tileDstX + x, tileDstY + y] = r or g or b or a
                    }
                }
            }
        }
    }

    @Serializable
    data class TileAddress(val x: Int, val y: Int, val frame: Int = 0)
}