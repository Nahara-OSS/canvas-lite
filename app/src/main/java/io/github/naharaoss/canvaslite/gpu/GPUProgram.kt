package io.github.naharaoss.canvaslite.gpu

import android.opengl.GLES20
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import java.nio.Buffer

class GPUProgram(
    vertexShader: GPUShader<GPUShader.Type.Vertex>,
    fragmentShader: GPUShader<GPUShader.Type.Fragment>,
    val label: String = ""
) : AutoCloseable {
    val id = GLES20.glCreateProgram()

    init {
        GLES20.glAttachShader(id, vertexShader.id)
        GLES20.glAttachShader(id, fragmentShader.id)
        GLES20.glLinkProgram(id)

        val status = IntArray(1).also { GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, it, 0) }[0]
        if (status == GLES20.GL_FALSE) {
            val log = GLES20.glGetProgramInfoLog(id)
            GLES20.glDeleteProgram(id)
            Log.e("GPUShader", "Program with label \"$label\" failed to link: $log")
            throw RuntimeException("Program with label \"$label\" failed to link: $log")
        }

        GLES20.glDetachShader(id, vertexShader.id)
        GLES20.glDetachShader(id, fragmentShader.id)
    }

    interface BindHandle : AutoCloseable {
        val program: GPUProgram

        fun Uniform.setValue1i(x: Int) = GLES20.glUniform1i(location, x)
        fun Uniform.setValue1f(x: Float) = GLES20.glUniform1f(location, x)
        fun Uniform.setValue2f(x: Float, y: Float) = GLES20.glUniform2f(location, x, y)
        fun Uniform.setValue2f(offset: Offset) = setValue2f(offset.x, offset.y)
        fun Uniform.setValue2f(size: Size) = setValue2f(size.width, size.height)
        fun Uniform.setValue3f(x: Float, y: Float, z: Float) = GLES20.glUniform3f(location, x, y, z)
        fun Uniform.setValue4f(x: Float, y: Float, z: Float, w: Float) = GLES20.glUniform4f(location, x, y, z, w)
        fun Uniform.setValue4f(color: Color) = setValue4f(color.red, color.green, color.blue, color.alpha)
        fun Uniform.setValueMatrix4f(matrix: Matrix) = GLES20.glUniformMatrix4fv(location, 1, false, matrix.values, 0)

        fun Attribute.enableArray(): AttribArrayHandle {
            GLES20.glEnableVertexAttribArray(location)

            return object : AttribArrayHandle {
                override val attribute: Attribute get() = this@enableArray
                override fun close() = GLES20.glDisableVertexAttribArray(location)
            }
        }
        fun Attribute.enableArray(block: AttribArrayHandle.() -> Unit) = enableArray().use { it.block() }
        fun AttribArrayHandle.setPointer(
            type: AttribType,
            stride: Int = type.bytes,
            normalize: Boolean = false,
            src: Buffer? = null
        ) = GLES20.glVertexAttribPointer(
            attribute.location,
            type.components,
            type.glEnumDtype,
            normalize,
            stride,
            src
        )

        fun drawArrays(topology: Topology, count: Int, first: Int = 0) = GLES20.glDrawArrays(topology.glEnum, first, count)
    }

    data class Uniform(val location: Int)
    data class Attribute(val location: Int)
    interface AttribArrayHandle : AutoCloseable { val attribute: Attribute }

    enum class AttribType(val glEnumDtype: Int, val components: Int, val bytes: Int) {
        Float(GLES20.GL_FLOAT, 1, 4),
        Float2(GLES20.GL_FLOAT, 2, 8),
        Float3(GLES20.GL_FLOAT, 3, 12),
        Float4(GLES20.GL_FLOAT, 4, 16),
    }

    enum class Topology(val glEnum: Int) {
        TriangleList(GLES20.GL_TRIANGLES),
        TriangleStrip(GLES20.GL_TRIANGLE_STRIP),
        TriangleFan(GLES20.GL_TRIANGLE_FAN),
        LineList(GLES20.GL_LINES),
        LineStrip(GLES20.GL_LINE_STRIP),
        Point(GLES20.GL_POINTS)
    }

    fun uniform(name: String): Uniform? {
        val location = GLES20.glGetUniformLocation(id, name)
        return if (location == -1) null else Uniform(location)
    }

    fun attribute(name: String): Attribute? {
        val location = GLES20.glGetAttribLocation(id, name)
        return if (location == -1) null else Attribute(location)
    }

    fun bind(): BindHandle {
        val oldId = IntArray(1).also { GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, it, 0) }[0]
        GLES20.glUseProgram(id)

        return object : BindHandle {
            override val program: GPUProgram get() = this@GPUProgram
            override fun close() = GLES20.glUseProgram(oldId)
        }
    }

    fun bind(block: BindHandle.() -> Unit): GPUProgram {
        bind().use { it.block() }
        return this
    }

    override fun close() = GLES20.glDeleteProgram(id)
}