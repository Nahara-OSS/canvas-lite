package io.github.naharaoss.canvaslite.engine.project

import android.graphics.ImageDecoder
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import io.github.naharaoss.canvaslite.ext.readAsJson
import io.github.naharaoss.canvaslite.ext.writeAsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import java.time.ZonedDateTime
import java.util.UUID

class LibraryImpl(val root: File) : Library {
    val rootIndex = File(root, "root.json")
    val contentRoot = File(root, "content")

    init {
        contentRoot.mkdirs()

        if (!rootIndex.exists()) rootIndex.writeAsJson(FolderIndex(
            parentId = null,
            type = Library.ItemType.Folder,
            metadata = Library.ItemMetadata(
                name = "Home",
                creationTime = ZonedDateTime.now(),
                lastModified = ZonedDateTime.now(),
                canvasSize = null
            ),
            children = emptyList()
        ))
    }

    private fun indexFileOf(libraryId: String?) = libraryId?.let { File(contentRoot, "$it.json") } ?: rootIndex
    private fun indexOf(libraryId: String?) = indexFileOf(libraryId).readAsJson<FolderIndex>().also { if (it == null) throw IOException("No such item with library ID $libraryId") }!!
    private fun libraryItemOf(libraryId: String) = indexOf(libraryId).let { Library.Item(libraryId, it.type, it.metadata) }

    override suspend fun listContent(folderId: String?) = withContext(Dispatchers.IO) { indexOf(folderId).children.map { libraryItemOf(it) } }

    override suspend fun createFolder(folderId: String?, name: String): Library.Item = withContext(Dispatchers.IO) {
        val childId = UUID.randomUUID().toString()
        val metadata = Library.ItemMetadata(
            name = name,
            creationTime = ZonedDateTime.now(),
            lastModified = ZonedDateTime.now(),
            canvasSize = null
        )

        val parentIndex = indexOf(folderId)
        indexFileOf(folderId).writeAsJson(parentIndex.copy(children = parentIndex.children + childId))
        indexFileOf(childId).writeAsJson(FolderIndex(
            parentId = folderId,
            type = Library.ItemType.Folder,
            metadata = metadata,
            children = emptyList()
        ))

        Library.Item(
            libraryId = childId,
            type = Library.ItemType.Folder,
            metadata = metadata
        )
    }

    override suspend fun createCanvas(folderId: String?, preset: Canvas.CanvasPreset) = withContext(Dispatchers.IO) {
        val childId = UUID.randomUUID().toString()
        val metadata = Library.ItemMetadata(
            name = "Artwork", // TODO
            creationTime = ZonedDateTime.now(),
            lastModified = ZonedDateTime.now(),
            canvasSize = preset.canvasSize
        )

        val parentIndex = indexOf(folderId)
        indexFileOf(folderId).writeAsJson(parentIndex.copy(children = parentIndex.children + childId))
        indexFileOf(childId).writeAsJson(FolderIndex(
            parentId = folderId,
            type = Library.ItemType.Canvas,
            metadata = metadata,
            children = emptyList()
        ))

        val canvasRoot = File(contentRoot, childId)
        ScuffedFileCanvas.initialize(canvasRoot, preset)
        ScuffedFileCanvas(canvasRoot).addLayer()

        Library.Item(
            libraryId = childId,
            type = Library.ItemType.Canvas,
            metadata = metadata
        )
    }

    override suspend fun loadCanvas(canvasId: String) = withContext(Dispatchers.IO) { ScuffedFileCanvas(File(contentRoot, canvasId)) }

    override suspend fun delete(libraryId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun loadThumbnail(canvasId: String): ImageBitmap? {
        val file = File(contentRoot, "$canvasId/thumbnail.png")
        return if (file.exists()) ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)).asImageBitmap() else null
    }

    @Serializable
    data class FolderIndex(
        val parentId: String?,
        val type: Library.ItemType,
        val metadata: Library.ItemMetadata,
        val children: List<String>
    )
}