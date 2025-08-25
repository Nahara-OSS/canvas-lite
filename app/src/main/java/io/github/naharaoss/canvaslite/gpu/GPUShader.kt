package io.github.naharaoss.canvaslite.gpu

import android.opengl.GLES20
import android.util.Log

class GPUShader<T : GPUShader.Type>(val type: T, val source: String, val label: String = "") : AutoCloseable {
    val id = GLES20.glCreateShader(type.glEnum)

    init {
        GLES20.glShaderSource(id, source)
        GLES20.glCompileShader(id)

        val status = IntArray(1).also { GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, it, 0) }[0]
        if (status == GLES20.GL_FALSE) {
            val log = GLES20.glGetShaderInfoLog(id)
            GLES20.glDeleteShader(id)
            Log.e("GPUShader", "Shader with label \"$label\" failed to compile: $log")
            throw RuntimeException("Shader with label \"$label\" failed to compile: $log")
        }

        Log.d("GPUShader", "glCompileShader() \"$label\" => $id")
    }

    interface Type {
        val glEnum: Int

        object Vertex : Type { override val glEnum = GLES20.GL_VERTEX_SHADER }
        object Fragment : Type { override val glEnum = GLES20.GL_FRAGMENT_SHADER }
    }

    override fun close() = GLES20.glDeleteShader(id)
}