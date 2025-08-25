package io.github.naharaoss.canvaslite.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.util.Log
import android.view.TextureView
import java.util.concurrent.locks.LockSupport

open class GLTextureView(context: Context) : TextureView(context) {
    private var thread: GLThread? = null
    private var renderer: Renderer? = null

    private class GLThread(
        val surface: SurfaceTexture,
        val renderer: Renderer
    ) : Thread() {
        override fun run() {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                Log.e("GLTextureView", "eglGetDisplay() failed")
                return
            }

            if (!EGL14.eglInitialize(eglDisplay, intArrayOf(0), 0, intArrayOf(0), 0)) {
                Log.e("GLTextureView", "eglInitialize() failed")
                return
            }

            val eglConfig = arrayOfNulls<EGLConfig>(1).also {
                val configCount = intArrayOf(0)
                EGL14.eglChooseConfig(
                    eglDisplay,
                    intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_NONE
                    ),
                    0,
                    it,
                    0,
                    1,
                    configCount,
                    0
                )
            }[0]!!
            val eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
            val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, intArrayOf(EGL14.EGL_NONE), 0)

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            renderer.onSurfaceCreated()

            while (!interrupted()) {
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                renderer.onDrawFrame()
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                LockSupport.park()
            }

            Log.d("GLTextureView", "Releasing surface")
            surface.release()
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
        }
    }

    interface Renderer {
        fun onSurfaceCreated()
        fun onDrawFrame()
    }

    init {
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                val renderer = this@GLTextureView.renderer
                if (renderer == null) throw IllegalStateException("Missing renderer")
                thread = GLThread(surface, renderer).also { it.start() }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                thread?.interrupt()
                return false
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    fun setRenderer(renderer: Renderer) {
        this.renderer = renderer
    }

    fun requestRender() {
        thread?.also { LockSupport.unpark(it) }
    }
}