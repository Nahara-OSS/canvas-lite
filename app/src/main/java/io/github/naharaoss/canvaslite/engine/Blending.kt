package io.github.naharaoss.canvaslite.engine

import android.opengl.GLES20
import io.github.naharaoss.canvaslite.gpu.GPUBlending

enum class Blending(val gpu: GPUBlending) {
    /**
     * Premultiplied source over blending mode, which is the default. "Source over" means the source
     * image being drawn over the destination surface.
     */
    PremultipliedSourceOver(GPUBlending(
        src = GPUBlending.Factor.One,
        dst = GPUBlending.Factor.OneMinusSrcAlpha
    )),

    /**
     * Source over blending mode but for source with straight alpha instead of premultiplied alpha.
     */
    StraightSourceOver(GPUBlending(
        srcColor = GPUBlending.Factor.SrcAlpha,
        srcAlpha = GPUBlending.Factor.One,
        dstColor = GPUBlending.Factor.OneMinusSrcAlpha,
        dstAlpha = GPUBlending.Factor.OneMinusSrcAlpha
    )),
}