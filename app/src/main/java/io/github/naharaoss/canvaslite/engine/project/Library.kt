package io.github.naharaoss.canvaslite.engine.project

import io.github.naharaoss.canvaslite.ext.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * User-managed library.
 */
interface Library {
    suspend fun listContent(folderId: String? = null): List<Item>
    suspend fun createFolder(folderId: String?, name: String): Item
    suspend fun createCanvas(folderId: String?, preset: Canvas.CanvasPreset): Item
    suspend fun loadCanvas(canvasId: String): Canvas
    suspend fun delete(libraryId: String)

    data class Item(
        val libraryId: String,
        val type: ItemType,
        val metadata: ItemMetadata
    )

    enum class ItemType { Folder, Canvas }

    @Serializable
    data class ItemMetadata(
        val name: String,
        @Serializable(with = ZonedDateTimeSerializer::class) val creationTime: ZonedDateTime,
        @Serializable(with = ZonedDateTimeSerializer::class) val lastModified: ZonedDateTime,
        val canvasSize: Canvas.CanvasSize?
    )
}