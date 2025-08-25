package io.github.naharaoss.canvaslite.engine.utils

fun copyPixels(
    src: BitmapInfo,
    dst: BitmapInfo,
    srcX: Int,
    srcY: Int,
    dstX: Int,
    dstY: Int,
    width: Int,
    height: Int,
    bytesPerPixel: Int = 4
) {
    if (width == 0 || height == 0) return
    if (srcX < 0 || srcY < 0 || srcX + width > src.width || srcY + height > src.height) throw IllegalArgumentException("Source rect out of bounds")
    if (dstX < 0 || dstY < 0 || dstX + width > dst.width || dstY + height > dst.height) throw IllegalArgumentException("Destination rect out of bounds")

    for (y in 0..<height) {
        System.arraycopy(
            src.data,
            (srcX + src.width * (srcY + y)) * bytesPerPixel,
            dst.data,
            (dstX + dst.width * (dstY + y)) * bytesPerPixel,
            width * bytesPerPixel
        )
    }
}

data class BitmapInfo(val data: ByteArray, val width: Int, val height: Int)