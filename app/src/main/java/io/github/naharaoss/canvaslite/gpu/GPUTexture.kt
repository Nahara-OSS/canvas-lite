package io.github.naharaoss.canvaslite.gpu

import android.opengl.GLES20
import java.nio.ByteBuffer

class GPUTexture<T : GPUTexture.Type>(
    val type: T,
    val label: String = ""
) : AutoCloseable {
    val id = IntArray(1).also { GLES20.glGenTextures(1, it, 0) }[0]

    interface Type {
        val glEnumType: Int
        val glEnumProp: Int

        object Texture2D : Type {
            override val glEnumType: Int = GLES20.GL_TEXTURE_2D
            override val glEnumProp: Int = GLES20.GL_TEXTURE_BINDING_2D
        }
    }

    enum class Filter(val glEnum: Int) {
        Linear(GLES20.GL_LINEAR),
        Nearest(GLES20.GL_NEAREST),
    }

    enum class WrapMode(val glEnum: Int) {
        Repeat(GLES20.GL_REPEAT),
        Mirrored(GLES20.GL_MIRRORED_REPEAT),
        ClampToEdge(GLES20.GL_CLAMP_TO_EDGE),
    }

    interface BindHandle<T : Type> : AutoCloseable {
        val texture: GPUTexture<T>

        var minFilter: Filter
            get() {
                val id = IntArray(1).also { GLES20.glGetTexParameteriv(texture.type.glEnumType, GLES20.GL_TEXTURE_MIN_FILTER, it, 0) }[0]
                return Filter.entries.find { it.glEnum == id }!!
            }
            set(value) {
                GLES20.glTexParameteri(texture.type.glEnumType, GLES20.GL_TEXTURE_MIN_FILTER, value.glEnum)
            }

        var magFilter: Filter
            get() {
                val id = IntArray(1).also { GLES20.glGetTexParameteriv(texture.type.glEnumType, GLES20.GL_TEXTURE_MAG_FILTER, it, 0) }[0]
                return Filter.entries.find { it.glEnum == id }!!
            }
            set(value) {
                GLES20.glTexParameteri(texture.type.glEnumType, GLES20.GL_TEXTURE_MAG_FILTER, value.glEnum)
            }

        var wrapS: WrapMode
            get() {
                val id = IntArray(1).also { GLES20.glGetTexParameteriv(texture.type.glEnumType, GLES20.GL_TEXTURE_WRAP_S, it, 0) }[0]
                return WrapMode.entries.find { it.glEnum == id }!!
            }
            set(value) {
                GLES20.glTexParameteri(texture.type.glEnumType, GLES20.GL_TEXTURE_WRAP_S, value.glEnum)
            }

        var wrapT: WrapMode
            get() {
                val id = IntArray(1).also { GLES20.glGetTexParameteriv(texture.type.glEnumType, GLES20.GL_TEXTURE_WRAP_T, it, 0) }[0]
                return WrapMode.entries.find { it.glEnum == id }!!
            }
            set(value) {
                GLES20.glTexParameteri(texture.type.glEnumType, GLES20.GL_TEXTURE_WRAP_T, value.glEnum)
            }
    }

    fun bind(textureUnit: Int? = null): BindHandle<T> {
        val oldId = IntArray(1).also { GLES20.glGetIntegerv(type.glEnumProp, it, 0) }[0]
        if (textureUnit != null) GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit)
        GLES20.glBindTexture(type.glEnumType, id)

        return object : BindHandle<T> {
            override val texture: GPUTexture<T> get() = this@GPUTexture
            override fun close() = GLES20.glBindTexture(type.glEnumType, oldId)
        }
    }

    fun bind(textureUnit: Int? = null, block: BindHandle<T>.() -> Unit): GPUTexture<T> {
        bind(textureUnit).use { it.block() }
        return this
    }

    override fun close() = GLES20.glDeleteTextures(1, intArrayOf(id), 0)
}

fun GPUTexture.BindHandle<GPUTexture.Type.Texture2D>.initTexture(
    width: Int,
    height: Int,
    mipmap: Int = 0,
    data: ByteBuffer? = null
) {
    GLES20.glTexImage2D(
        GLES20.GL_TEXTURE_2D,
        mipmap,
        GLES20.GL_RGBA,
        width,
        height,
        0,
        GLES20.GL_RGBA,
        GLES20.GL_UNSIGNED_BYTE,
        data
    )
}