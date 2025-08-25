package io.github.naharaoss.canvaslite.ext

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

inline fun <reified T> InputStream.readAsJson(): T? = try {
    Json.decodeFromString(reader().readText())
} catch (_: SerializationException) {
    null
}

inline fun <reified T> InputStream?.readAsJson(default: () -> T) = when (this) {
    null -> default()
    else -> try {
        Json.decodeFromString(reader().readText())
    } catch (_: SerializationException) {
        default()
    }
}

inline fun <reified T> File?.readAsJson(): T? = when {
    this?.exists() ?: false -> FileInputStream(this).use { it.readAsJson() }
    else -> null
}

inline fun <reified T> File?.readAsJson(default: () -> T) = when {
    this?.exists() ?: false -> FileInputStream(this).use { it.readAsJson(default) }
    else -> default()
}

inline fun <reified T> OutputStream.writeAsJson(value: T) = writer().apply {
    write(Json.encodeToString(value))
    flush()
}

inline fun <reified T> File.writeAsJson(value: T) = FileOutputStream(this)
    .use { it.writeAsJson(value) }