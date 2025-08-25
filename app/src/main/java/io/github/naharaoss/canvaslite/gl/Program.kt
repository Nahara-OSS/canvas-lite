package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20
import android.util.Log
import androidx.compose.ui.graphics.Matrix
import java.nio.Buffer

class Program : AutoCloseable {
    private val shaders: Array<out Shader>
    val id = GLES20.glCreateProgram()
    val label: String

    constructor(vararg shaders: Shader, label: String = "") {
        this.shaders = shaders
        this.label = label
        for (shader in shaders) GLES20.glAttachShader(id, shader.id)
        GLES20.glLinkProgram(id)

        if (IntArray(1).also { GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, it, 0) }[0] == GLES20.GL_FALSE) {
            val log = GLES20.glGetProgramInfoLog(id)
            GLES20.glDeleteProgram(id)
            Log.e("Shader", "Program \"$label\" link failed: $log")
            throw Exception("Program \"$label\" link failed: $log")
        }

        Log.d("Shader", "Linked \"$label\" program (ID = $id)")
    }

    fun use() {
        GLES20.glUseProgram(id)
    }

    fun uniform(name: String): Uniform? {
        val location = GLES20.glGetUniformLocation(id, name)
        return if (location != -1) Uniform(location) else null
    }

    fun attribute(name: String): Attribute? {
        val location = GLES20.glGetAttribLocation(id, name)
        return if (location != -1) Attribute(location) else null
    }

    override fun close() {
        for (shader in shaders) GLES20.glDetachShader(id, shader.id)
        GLES20.glDeleteProgram(id)
    }

    data class Uniform(val location: Int) {
        fun uniform1i(x: Int) = GLES20.glUniform1i(location, x)
        fun uniform1f(x: Float) = GLES20.glUniform1f(location, x)
        fun uniform2f(x: Float, y: Float) = GLES20.glUniform2f(location, x, y)
        fun uniform3f(x: Float, y: Float, z: Float) = GLES20.glUniform3f(location, x, y, z)
        fun uniform4f(x: Float, y: Float, z: Float, w: Float) = GLES20.glUniform4f(location, x, y, z, w)
        fun matrix(mat: Matrix) = GLES20.glUniformMatrix4fv(location, 1, false, mat.values, 0)
    }

    data class Attribute(val location: Int) {
        fun enable(): AutoCloseable {
            GLES20.glEnableVertexAttribArray(location)
            return AutoCloseable { GLES20.glDisableVertexAttribArray(location) }
        }

        fun setPointer(
            type: AttribType,
            stride: Int = type.bytes,
            src: Buffer?
        ) = GLES20.glVertexAttribPointer(
            location,
            type.components,
            type.glEnum,
            false,
            stride,
            src
        )
    }

    enum class AttribType(val components: Int, val glEnum: Int, val bytes: Int) {
        Float32(1, GLES20.GL_FLOAT, 4),
        Float32x2(2, GLES20.GL_FLOAT, 8),
        Float32x3(3, GLES20.GL_FLOAT, 12),
        Float32x4(4, GLES20.GL_FLOAT, 16),
    }
}