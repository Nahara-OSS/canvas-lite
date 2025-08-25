package io.github.naharaoss.canvaslite.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.naharaoss.canvaslite.CanvasApplication
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.engine.project.Library
import kotlinx.coroutines.launch

class LibraryViewModel(val library: Library) : ViewModel() {
    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!! as CanvasApplication
                LibraryViewModel(app.library)
            }
        }
    }

    fun createFolder(parentId: String?, name: String, onFinished: (Library.Item) -> Unit) {
        viewModelScope.launch {
            val item = library.createFolder(parentId, name)
            onFinished(item)
        }
    }

    fun createCanvas(parentId: String?, preset: Canvas.CanvasPreset, onFinished: (Library.Item) -> Unit) {
        viewModelScope.launch {
            val item = library.createCanvas(parentId, preset)
            onFinished(item)
        }
    }
}