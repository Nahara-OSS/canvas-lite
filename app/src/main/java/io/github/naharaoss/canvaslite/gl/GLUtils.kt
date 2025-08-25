package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20

fun getGLError(): String? {
    val code = GLES20.glGetError()

    return when (code) {
        GLES20.GL_NO_ERROR -> null
        GLES20.GL_INVALID_ENUM -> "Invalid enum"
        GLES20.GL_INVALID_VALUE -> "Invalid value"
        GLES20.GL_INVALID_OPERATION -> "Invalid operation"
        else -> "Unknown OpenGL error: $code"
    }
}