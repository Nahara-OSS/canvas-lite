package io.github.naharaoss.canvaslite.engine.utils

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

open class TilingViewportAssist() {
    private val _visibleTiles = mutableSetOf<TileId>()

    /**
     * All visible tiles from the last [update] invocation. Initially, there are none.
     */
    val visibleTiles: Set<TileId> = _visibleTiles

    /**
     * @param worldToViewport The transformation matrix that transforms world coordinates to
     * viewport (clip space) coordinates.
     * @param tileSize The dimension of each file.
     * @param minTileX The minimum tile ID along X axis (inclusive).
     * @param minTileY The minimum tile ID along Y axis (inclusive).
     * @param maxTileX The maximum tile ID along X axis (inclusive).
     * @param maxTileY The maximum tile ID along Y axis (inclusive).
     */
    fun update(
        worldToViewport: Matrix,
        tileSize: Size,
        minTileX: Int? = null,
        minTileY: Int? = null,
        maxTileX: Int? = null,
        maxTileY: Int? = null,
    ) {
        val viewportWorldRect = Matrix()
            .apply { setFrom(worldToViewport); invert() }
            .map(Rect(-1f, -1f, 1f, 1f))
        val rawMinTileX = floor(viewportWorldRect.left / tileSize.width).toInt()
        val rawMinTileY = floor(viewportWorldRect.top / tileSize.height).toInt()
        val rawMaxTileX = floor(viewportWorldRect.right / tileSize.width).toInt()
        val rawMaxTileY = floor(viewportWorldRect.bottom / tileSize.height).toInt()
        val minTileX = if (minTileX != null) max(minTileX, rawMinTileX) else rawMinTileX
        val minTileY = if (minTileY != null) max(minTileY, rawMinTileY) else rawMinTileY
        val maxTileX = if (maxTileX != null) min(maxTileX, rawMaxTileX) else rawMaxTileX
        val maxTileY = if (maxTileY != null) min(maxTileY, rawMaxTileY) else rawMaxTileY

        val additions = mutableSetOf<TileId>()
        val removals = _visibleTiles.toMutableSet()

        for (tileY in minTileY..maxTileY) {
            for (tileX in minTileX..maxTileX) {
                val id = TileId(tileX, tileY)
                if (!removals.remove(id)) additions += id
            }
        }

        _visibleTiles -= removals
        _visibleTiles += additions

        removals.forEach { onTileExit(it) }
        additions.forEach { onTileEnter(it) }
    }

    fun clear() {
        _visibleTiles.forEach { onTileExit(it) }
        _visibleTiles.clear()
    }

    open fun onTileEnter(id: TileId) {}
    open fun onTileExit(id: TileId) {}

    data class TileId(val x: Int, val y: Int)
}