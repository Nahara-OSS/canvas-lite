package io.github.naharaoss.canvaslite.gpu

import android.opengl.GLES20
import android.util.Log
import java.nio.Buffer

class GPURenderTarget(val label: String = "") : AutoCloseable {
    val id = IntArray(1).also { GLES20.glGenFramebuffers(1, it, 0) }[0]

    interface BindHandle : AutoCloseable {
        val target: GPURenderTarget

        fun attach(
            attachment: Attachment,
            texture: GPUTexture<GPUTexture.Type.Texture2D>,
            mipmap: Int = 0
        ) = GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            attachment.glEnum,
            texture.type.glEnumType,
            texture.id,
            mipmap
        )

        fun readPixels(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            dst: Buffer
        ) = GLES20.glReadPixels(
            x,
            y,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            dst
        )

        fun ensureCompleted() {
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            val error = when (status) {
                GLES20.GL_FRAMEBUFFER_COMPLETE -> return
                GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "Attachment incomplete"
                GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "Missing attachment"
                GLES20.GL_FRAMEBUFFER_UNSUPPORTED -> "Unsupported attachment combination"
                else -> "Framebuffer incomplete: $status"
            }
            Log.e("Framebuffer", error)
            throw Exception(error)
        }
    }

    interface Attachment {
        val glEnum: Int

        data class Color(val index: Int = 0) : Attachment { override val glEnum = GLES20.GL_COLOR_ATTACHMENT0 + index }
        object Depth : Attachment { override val glEnum = GLES20.GL_DEPTH_ATTACHMENT }
        object Stencil : Attachment { override val glEnum = GLES20.GL_STENCIL_ATTACHMENT }
    }

    fun bind(): BindHandle {
        val oldId = IntArray(1).also { GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, it, 0) }[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id)

        return object : BindHandle {
            override val target: GPURenderTarget get() = this@GPURenderTarget
            override fun close() = GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, oldId)
        }
    }

    fun bind(block: BindHandle.() -> Unit): GPURenderTarget {
        bind().use { it.block() }
        return this
    }

    override fun close() = GLES20.glDeleteFramebuffers(1, intArrayOf(id), 0)
}