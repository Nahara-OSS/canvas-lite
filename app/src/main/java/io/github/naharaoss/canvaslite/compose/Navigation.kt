package io.github.naharaoss.canvaslite.compose

import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import io.github.naharaoss.canvaslite.engine.project.Canvas
import io.github.naharaoss.canvaslite.ext.ColorSerializer
import kotlinx.serialization.json.Json

object CanvasSizeParameter : NavType<Canvas.CanvasSize?>(true) {
    override fun get(bundle: SavedState, key: String) = bundle.getString(key)?.let { parseValue(it) }
    override fun put(bundle: SavedState, key: String, value: Canvas.CanvasSize?) = bundle.putString(key, serializeAsValue(value))
    override fun parseValue(value: String) = Json.decodeFromString<Canvas.CanvasSize?>(value)
    override fun serializeAsValue(value: Canvas.CanvasSize?) = Json.encodeToString(value)
}

object ColorParameter : NavType<Color>(false) {
    override fun get(bundle: SavedState, key: String) = bundle.getString(key)?.let { parseValue(it) }
    override fun put(bundle: SavedState, key: String, value: Color) = bundle.putString(key, serializeAsValue(value))
    override fun parseValue(value: String) = Json.decodeFromString(ColorSerializer, value)
    override fun serializeAsValue(value: Color) = Json.encodeToString(ColorSerializer, value)
}