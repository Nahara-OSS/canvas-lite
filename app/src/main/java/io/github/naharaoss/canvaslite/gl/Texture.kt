package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20
import java.nio.ByteBuffer

class Texture : AutoCloseable {
    val id = IntArray(1).also { GLES20.glGenTextures(1, it, 0) }[0]

    fun bind(
        target: BindTarget,
        unit: Int = IntArray(1).also { GLES20.glGetIntegerv(GLES20.GL_ACTIVE_TEXTURE, it, 0) }[0] - GLES20.GL_TEXTURE0
    ): AutoCloseable {
        val lastId = IntArray(1).also { GLES20.glGetIntegerv(target.glEnumParam, it, 0) }[0]
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
        GLES20.glBindTexture(target.glEnumTarget, id)

        return AutoCloseable {
            GLES20.glBindTexture(target.glEnumTarget, lastId)
        }
    }

    fun <T : BindTarget> bind(target: T, block: T.() -> Unit): Texture {
        bind(target).use { target.block() }
        return this
    }

    interface BindTarget {
        val glEnumTarget: Int
        val glEnumParam: Int

        var minFilter: Filter
            get() {
                val v = IntArray(1).also { GLES20.glGetTexParameteriv(glEnumTarget, GLES20.GL_TEXTURE_MIN_FILTER, it, 0) }[0]
                return Filter.entries.find { it.glEnum == v }!!
            }
            set(value) = GLES20.glTexParameteri(glEnumTarget, GLES20.GL_TEXTURE_MIN_FILTER, value.glEnum)
        var magFilter: Filter
            get() {
                val v = IntArray(1).also { GLES20.glGetTexParameteriv(glEnumTarget, GLES20.GL_TEXTURE_MAG_FILTER, it, 0) }[0]
                return Filter.entries.find { it.glEnum == v }!!
            }
            set(value) = GLES20.glTexParameteri(glEnumTarget, GLES20.GL_TEXTURE_MAG_FILTER, value.glEnum)

        data object Texture2D : BindTarget {
            override val glEnumTarget: Int = GLES20.GL_TEXTURE_2D
            override val glEnumParam: Int = GLES20.GL_TEXTURE_BINDING_2D

            fun texImage2D(
                width: Int,
                height: Int,
                mipmapLevel: Int = 0,
                internalFormat: InternalFormat = InternalFormat.RGBA8,
                dataFormat: DataFormat = DataFormat.RGBA_U8,
                data: ByteBuffer? = null
            ) {
                GLES20.glTexImage2D(
                    glEnumTarget,
                    mipmapLevel,
                    internalFormat.glEnum,
                    width,
                    height,
                    0,
                    dataFormat.glEnumFmt,
                    dataFormat.glEnumDtype,
                    data
                )
            }
        }

        data object CubeMap : BindTarget {
            override val glEnumTarget: Int = GLES20.GL_TEXTURE_CUBE_MAP
            override val glEnumParam: Int = GLES20.GL_TEXTURE_BINDING_CUBE_MAP
        }
    }

    enum class InternalFormat(val glEnum: Int) {
        RGBA8(GLES20.GL_RGBA),
    }

    enum class DataFormat(val glEnumFmt: Int, val glEnumDtype: Int) {
        RGBA_U8(GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE),
        RGBA_S8(GLES20.GL_RGBA, GLES20.GL_BYTE)
    }

    enum class Filter(val glEnum: Int) {
        Linear(GLES20.GL_LINEAR),
        Nearest(GLES20.GL_NEAREST)
    }

    override fun close() {
        GLES20.glDeleteTextures(1, intArrayOf(id), 0)
    }
}