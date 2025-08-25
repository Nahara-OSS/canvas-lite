package io.github.naharaoss.canvaslite.gpu

import android.opengl.GLES20
import android.opengl.GLES30
import androidx.compose.ui.graphics.Color

data class GPUBlending(
    val colorOp: Operation = Operation.Add,
    val alphaOp: Operation = Operation.Add,
    val srcColor: Factor = Factor.One,
    val srcAlpha: Factor = Factor.One,
    val dstColor: Factor = Factor.Zero,
    val dstAlpha: Factor = Factor.Zero,
    val constant: Color = Color.Transparent,
) {
    constructor(
        op: Operation = Operation.Add,
        src: Factor = Factor.One,
        dst: Factor = Factor.Zero
    ) : this(op, op, src, src, dst, dst)

    enum class Operation(val glEnum: Int) {
        Add(GLES20.GL_FUNC_ADD),
        Subtract(GLES20.GL_FUNC_SUBTRACT),
        ReverseSubtract(GLES20.GL_FUNC_REVERSE_SUBTRACT),
        Min(GLES30.GL_MIN), // TODO OpenGL ES 3.0
        Max(GLES30.GL_MAX), // TODO OpenGL ES 3.0
    }

    enum class Factor(val glEnum: Int) {
        Zero(GLES20.GL_ZERO),
        One(GLES20.GL_ONE),
        SrcColor(GLES20.GL_SRC_COLOR),
        SrcAlpha(GLES20.GL_SRC_ALPHA),
        OneMinusSrcColor(GLES20.GL_ONE_MINUS_SRC_COLOR),
        OneMinusSrcAlpha(GLES20.GL_ONE_MINUS_SRC_ALPHA),
        DstColor(GLES20.GL_DST_COLOR),
        DstAlpha(GLES20.GL_DST_ALPHA),
        OneMinusDstColor(GLES20.GL_ONE_MINUS_DST_COLOR),
        OneMinusDstAlpha(GLES20.GL_ONE_MINUS_DST_ALPHA),
        ConstColor(GLES20.GL_CONSTANT_COLOR),
        ConstAlpha(GLES20.GL_CONSTANT_ALPHA),
        OneMinusConstColor(GLES20.GL_ONE_MINUS_CONSTANT_COLOR),
        OneMinusConstAlpha(GLES20.GL_ONE_MINUS_CONSTANT_ALPHA),
    }

    fun applyBlending() {
        GLES20.glBlendEquationSeparate(colorOp.glEnum, alphaOp.glEnum)
        GLES20.glBlendFuncSeparate(srcColor.glEnum, dstColor.glEnum, srcAlpha.glEnum, dstAlpha.glEnum)
        GLES20.glBlendColor(constant.red, constant.green, constant.blue, constant.alpha)
    }

    companion object {
        fun enable() = GLES20.glEnable(GLES20.GL_BLEND)
        fun disable() = GLES20.glDisable(GLES20.GL_BLEND)

        fun enable(block: () -> Unit) {
            if (IntArray(1).also { GLES20.glGetIntegerv(GLES20.GL_BLEND, it, 0) }[0] == GLES20.GL_TRUE) {
                block()
            } else {
                enable()
                block()
                disable()
            }
        }
    }
}
