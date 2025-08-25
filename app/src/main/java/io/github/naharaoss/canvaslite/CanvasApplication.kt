package io.github.naharaoss.canvaslite

import android.app.Application
import io.github.naharaoss.canvaslite.engine.project.Library
import io.github.naharaoss.canvaslite.engine.project.LibraryImpl
import java.io.File

class CanvasApplication : Application() {
    lateinit var library: Library

    override fun onCreate() {
        super.onCreate()
        library = LibraryImpl(File(filesDir, "library"))
    }
}