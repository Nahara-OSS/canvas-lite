package io.github.naharaoss.canvaslite.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.naharaoss.canvaslite.engine.project.Library
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FolderViewModel(val library: Library, val folderId: String?) : ViewModel() {
    private val _items: MutableStateFlow<List<Library.Item>?> = MutableStateFlow(null)
    val items = _items.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val items = library.listContent(folderId)
            _items.update { items }
        }
    }
}