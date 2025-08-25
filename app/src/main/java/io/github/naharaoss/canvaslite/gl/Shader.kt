package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20
import android.util.Log

class Shader(val type: Type, val source: String, val label: String = "") : AutoCloseable {
    val id = GLES20.glCreateShader(type.glEnum)

    init {
        GLES20.glShaderSource(id, source)
        GLES20.glCompileShader(id)

        if (IntArray(1).also { GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, it, 0) }[0] == GLES20.GL_FALSE) {
            val log = GLES20.glGetShaderInfoLog(id)
            GLES20.glDeleteShader(id)
            Log.e("Shader", "Shader \"$label\" compile failed: $log")
            throw Exception("Shader \"$label\" compile failed: $log")
        }

        Log.d("Shader", "Compiled \"$label\" $type shader (ID = $id)")
    }

    enum class Type(val glEnum: Int) {
        Vertex(GLES20.GL_VERTEX_SHADER),
        Fragment(GLES20.GL_FRAGMENT_SHADER)
    }

    override fun close() {
        GLES20.glDeleteShader(id)
    }
}

fun VertexShader(source: String) = Shader(type = Shader.Type.Vertex, source = source)
fun FragmentShader(source: String) = Shader(type = Shader.Type.Fragment, source = source)