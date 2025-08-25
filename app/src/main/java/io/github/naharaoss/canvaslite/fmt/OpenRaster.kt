package io.github.naharaoss.canvaslite.fmt

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.naharaoss.canvaslite.engine.Blending
import java.io.IOException
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import java.io.ByteArrayOutputStream


/**
 * Specification: https://www.openraster.org/index.html
 */

fun writeSampleOpenRaster(output: OutputStream) = OpenRaster(
    output = output,
    layers = LayerStack(
        image = LayerStack.Image(width = 100, height = 100),
        root = LayerStack.Stack(children = listOf(
            LayerStack.Layer(src = "data/layer2.png", name = "Layer 2"),
            LayerStack.Layer(src = "data/layer1.png", name = "Layer 1"),
        ))
    )
).use { ora ->
    val layer1 = createBitmap(100, 100)
        .also { bitmap ->
            for (y in 0..<100) {
                for (x in 0..<100) {
                    bitmap[x, y] = Color(x / 100f, y / 100f, 0.5f, 1f).toArgb()
                }
            }
        }
        .let { bitmap -> ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 0, it) }.toByteArray() }

    val layer2 = createBitmap(100, 100)
        .also { bitmap ->
            for (y in 0..<100) {
                for (x in 0..<100) {
                    bitmap[x, y] = Color(0f, 0f, 0f, x / 100f).toArgb()
                }
            }
        }
        .let { bitmap -> ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 0, it) }.toByteArray() }

    ora.openEntry("data/layer1.png").use { it.write(layer1) }
    ora.openEntry("data/layer2.png").use { it.write(layer2) }
    ora.openEntry("Thumbnails/thumbnail.png").use { it.write(layer1) }
    ora.openEntry("mergedimage.png").use { it.write(layer1) }
}

class OpenRaster(
    output: OutputStream,
    compression: Int = Deflater.BEST_COMPRESSION,
    layers: LayerStack
) : AutoCloseable {
    private val zip = ZipOutputStream(output)
    private var writingEntry: String? = null

    init {
        zip.apply {
            setLevel(Deflater.NO_COMPRESSION)
            putNextEntry(ZipEntry("mimedata"))
            writer().apply { write("image/openraster") }.flush()
            closeEntry()
            setLevel(compression)

            putNextEntry(ZipEntry("stack.xml"))
            writer().apply { write(layers.xmlCode) }.flush()
            closeEntry()
        }
    }

    fun openEntry(src: String) = object : OutputStream() {
        init {
            if (writingEntry != null) throw IOException("Already writing entry $writingEntry")
            writingEntry = src
            zip.putNextEntry(ZipEntry(src))
        }

        override fun write(b: Int) = zip.write(b)
        override fun write(b: ByteArray?) = zip.write(b)
        override fun write(b: ByteArray?, off: Int, len: Int) = zip.write(b, off, len)
        override fun flush() = zip.flush()

        override fun close() {
            if (writingEntry != src) return
            zip.closeEntry()
            writingEntry = null
        }
    }

    override fun close() = zip.close()
}

data class LayerStack(val image: Image, val root: Stack) {
    data class Image(
        val width: Int,
        val height: Int,
        val resolutionX: Int = 72,
        val resolutionY: Int = 72
    )

    interface StackElement {
        val name: String?
        val opacity: Float
        val visible: Boolean
        val compositeOp: Blending
        val xml: OraXmlNode
    }

    data class Stack(
        override val name: String? = null,
        override val opacity: Float = 1f,
        override val visible: Boolean = true,
        override val compositeOp: Blending = Blending.PremultipliedSourceOver,
        val isolate: Boolean = false,
        val children: List<StackElement> = emptyList()
    ) : StackElement {
        override val xml: OraXmlNode get() = OraXmlNode(
            name = "stack",
            attributes = listOfNotNull(
                name?.let { OraXmlNode.Attribute("name", it) },
                OraXmlNode.Attribute("opacity", opacity),
                OraXmlNode.Attribute("visibility", if (visible) "visible" else "hidden"),
                OraXmlNode.Attribute("composite-op", compositeOp.compositeOpName),
                OraXmlNode.Attribute("isolation", if (isolate) "isolate" else "auto")
            ),
            children = children.map { it.xml }
        )
    }

    data class Layer(
        val src: String,
        override val name: String? = null,
        val x: Int = 0,
        val y: Int = 0,
        override val opacity: Float = 1f,
        override val visible: Boolean = true,
        override val compositeOp: Blending = Blending.PremultipliedSourceOver
    ) : StackElement {
        override val xml: OraXmlNode get() = OraXmlNode(
            name = "layer",
            attributes = listOfNotNull(
                OraXmlNode.Attribute("src", src),
                name?.let { OraXmlNode.Attribute("name", it) },
                OraXmlNode.Attribute("x", x),
                OraXmlNode.Attribute("y", y),
                OraXmlNode.Attribute("opacity", opacity),
                OraXmlNode.Attribute("visibility", if (visible) "visible" else "hidden"),
                OraXmlNode.Attribute("composite-op", compositeOp.compositeOpName)
            )
        )
    }

    val xml: OraXmlNode get() = OraXmlNode(
        name = "image",
        attributes = listOf(
            OraXmlNode.Attribute("version", "0.0.3"),
            OraXmlNode.Attribute("w", image.width),
            OraXmlNode.Attribute("h", image.height),
            OraXmlNode.Attribute("xres", image.resolutionX),
            OraXmlNode.Attribute("yres", image.resolutionY)
        ),
        children = listOf(root.xml)
    )

    val xmlCode get() = "<?xml version='1.0' encoding='UTF-8'?>${xml.code}"
}

data class OraXmlNode(
    val name: String,
    val attributes: List<Attribute> = emptyList(),
    val children: List<OraXmlNode> = emptyList()
) {
    data class Attribute(val name: String, val value: String) {
        constructor(name: String, value: Any) : this(name, "$value")

        val code get() = "$name=\"$value\""
    }

    val code: String get() = if (children.isEmpty()) {
        attributes.joinToString(
            separator = "",
            prefix = "<$name",
            postfix = " />",
            transform = { " ${it.code}" }
        )
    } else {
        val head = attributes.joinToString(separator = "", prefix = name) { " ${it.code}" }
        val content = children.joinToString(separator = "\n") { it.code }
        "<$head>$content</$name>"
    }
}

private val Blending.compositeOpName get() = when (this) {
    Blending.PremultipliedSourceOver -> "svg:src-over"
    else -> throw Exception()
}