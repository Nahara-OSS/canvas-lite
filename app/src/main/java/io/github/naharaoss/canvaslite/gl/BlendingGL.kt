package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20
import io.github.naharaoss.canvaslite.engine.Blending

enum class BlendingGL(val op: Int, val srcFactor: Int, val dstFactor: Int) {
    PremultipliedSourceOver(GLES20.GL_FUNC_ADD, GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA),
    StraightSourceOver(GLES20.GL_FUNC_ADD, GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    fun use() {
        GLES20.glBlendEquation(op)
        GLES20.glBlendFunc(srcFactor, dstFactor)
    }
}

val Blending.gl get() = when (this) {
    Blending.PremultipliedSourceOver -> BlendingGL.PremultipliedSourceOver
    Blending.StraightSourceOver -> BlendingGL.StraightSourceOver
    else -> throw Exception()
}