package io.github.naharaoss.canvaslite.model

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import io.github.naharaoss.canvaslite.engine.Preferences
import io.github.naharaoss.canvaslite.ext.readAsJson
import io.github.naharaoss.canvaslite.ext.writeAsJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefFile: File
    private val _preferences: MutableStateFlow<Preferences>
    val preferences: StateFlow<Preferences>

    init {
        val filesRoot = getApplication<Application>().applicationContext.filesDir
        prefFile = File(filesRoot, "preferences.json")

        val savedPref = prefFile.readAsJson { Preferences.createDefaults() }
        _preferences = MutableStateFlow(savedPref)
        preferences = _preferences.asStateFlow()
    }

    fun updatePreference(newPreferences: Preferences) {
        _preferences.update { newPreferences }
        prefFile.writeAsJson(newPreferences)
    }

    fun saveTo(uri: Uri?) {
        if (uri == null) return
        val preferences = _preferences.value
        val resolver = getApplication<Application>().applicationContext.contentResolver
        resolver.openOutputStream(uri)?.use { it.writeAsJson(preferences) }
    }

    fun loadFrom(uri: Uri?) {
        if (uri == null) return
        val resolver = getApplication<Application>().applicationContext.contentResolver
        resolver.openInputStream(uri)
            ?.use { it.readAsJson<Preferences>() }
            ?.also { updatePreference(it) }
    }
}