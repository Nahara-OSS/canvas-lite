package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.Buffer
import java.nio.ByteBuffer

class Framebuffer : AutoCloseable {
    val id = IntArray(1).also { GLES20.glGenFramebuffers(1, it, 0) }[0]

    fun bind(): AutoCloseable {
        val lastId = IntArray(1).also { GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, it, 0) }[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id)

        return AutoCloseable {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, lastId)
        }
    }

    fun bind(block: Framebuffer.() -> Unit): Framebuffer {
        bind().use { this.block() }
        return this
    }

    fun attach(
        attachment: Attachment,
        target: Texture.BindTarget,
        texture: Texture?,
        mipmapLevel: Int = 0
    ) = GLES20.glFramebufferTexture2D(
        GLES20.GL_FRAMEBUFFER,
        attachment.glEnum,
        target.glEnumTarget,
        texture?.id ?: 0,
        mipmapLevel
    )

    fun readPixels(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        format: Texture.DataFormat = Texture.DataFormat.RGBA_U8,
        dst: Buffer
    ) = GLES20.glReadPixels(x, y, width, height, format.glEnumFmt, format.glEnumDtype, dst)

    fun ensureCompleted() {
        val status = bind().use { GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) }
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

    interface Attachment {
        val glEnum: Int

        data class Color(val index: Int) : Attachment {
            override val glEnum: Int get() = GLES20.GL_COLOR_ATTACHMENT0 + index
        }

        object Depth : Attachment { override val glEnum: Int get() = GLES20.GL_DEPTH_ATTACHMENT }
        object Stencil : Attachment { override val glEnum: Int get() = GLES20.GL_STENCIL_ATTACHMENT }
    }

    override fun close() {
        GLES20.glDeleteFramebuffers(1, intArrayOf(id), 0)
    }
}
