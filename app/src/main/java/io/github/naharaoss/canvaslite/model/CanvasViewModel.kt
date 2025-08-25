package io.github.naharaoss.canvaslite.model

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.naharaoss.canvaslite.CanvasApplication
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Layer
import io.github.naharaoss.canvaslite.engine.project.Library
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CanvasViewModel(val library: Library, val libraryId: String, val app: CanvasApplication) : ViewModel() {
    private val _canvas: MutableStateFlow<Canvas?> = MutableStateFlow(null)
    private val _currentLayer: MutableStateFlow<Layer?> = MutableStateFlow(null)
    private val _allLayers = MutableStateFlow(emptyList<Layer>())
    private val _frame = MutableStateFlow(0)

    val canvas = _canvas.asStateFlow()
    val currentLayer = _currentLayer.asStateFlow()
    val allLayers = _allLayers.asStateFlow()
    val frame = _frame.asStateFlow()

    init {
        viewModelScope.launch {
            val canvas = library.loadCanvas(libraryId)
            val storedLayer = canvas.currentLayer
            val layer = when {
                storedLayer == null -> null
                storedLayer >= 0 && storedLayer < canvas.layers.size -> canvas.layers[storedLayer]
                else -> null
            }

            _canvas.update { canvas }
            _currentLayer.update { layer }
            _allLayers.update { canvas.layers.toList() }
            _frame.update { canvas.currentFrame }
        }
    }

    fun selectLayer(layer: Layer) {
        _canvas.value?.also {
            val index = it.layers.indexOf(layer)
            if (index == -1) return
            it.currentLayer = index
            _currentLayer.update { layer }
        }
    }

    fun addLayer(
        insertBefore: Int = _canvas.value?.layers?.size ?: 0,
        name: String = "Layer ${(canvas.value?.layers?.size ?: 0) + 1}",
        blending: Blending = Blending.PremultipliedSourceOver,
        opacity: Float = 1f
    ): Layer? = _canvas.value?.let { canvas ->
        val layer = canvas.addLayer(insertBefore, name, blending, opacity)
        _currentLayer.update { canvas.currentLayer?.let { canvas.layers[it] } }
        _allLayers.update { canvas.layers.toList() }
        layer
    }

    fun removeLayer(layer: Layer) {
        _canvas.value?.also { canvas ->
            canvas.removeLayer(layer)
            _currentLayer.update { canvas.currentLayer?.let { canvas.layers[it] } }
            _allLayers.update { canvas.layers.toList() }
        }
    }

    fun testExport(onFinished: (File) -> Unit) {
        val canvas = _canvas.value
        if (canvas == null) return

        val copyWidth = canvas.canvasSize?.width ?: 1024
        val copyHeight = canvas.canvasSize?.height ?: 1024

        viewModelScope.launch {
            val exportDir = File(app.cacheDir, "exports").also { it.mkdirs() }
            val exportImg = File(exportDir, "${UUID.randomUUID()}.png")

            withContext(Dispatchers.IO) {
                val bitmap = createBitmap(copyWidth, copyHeight)
                canvas.layers[0].copyPixels(bitmap, 0, -copyWidth / 2, -copyHeight / 2, 0, 0, copyWidth, copyHeight, canvas.tileSize)
                FileOutputStream(exportImg).use { bitmap.compress(Bitmap.CompressFormat.PNG, 0, it) }
            }

            onFinished(exportImg)
        }
    }

    companion object {
        val LIBRARY_ID_KEY = object : CreationExtras.Key<String> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!! as CanvasApplication
                val libraryId = this[LIBRARY_ID_KEY]!!
                CanvasViewModel(app.library, libraryId, app)
            }
        }
    }
}